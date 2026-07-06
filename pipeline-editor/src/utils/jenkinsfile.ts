import type { Pipeline, PipelineStage, PipelineStep } from '@/types/pipeline'

export function pipelineToJenkinsfile(pipeline: Pipeline): string {
  const lines: string[] = []

  lines.push('pipeline {')

  // Agent
  lines.push(`  agent ${agentToString(pipeline.agent)}`)

  // Environment
  if (pipeline.environment.length > 0) {
    lines.push('')
    lines.push('  environment {')
    for (const env of pipeline.environment) {
      lines.push(`    ${env.key} = '${env.value}'`)
    }
    lines.push('  }')
  }

  // Stages
  lines.push('')
  lines.push('  stages {')
  for (const stage of pipeline.stages) {
    const stageLines = stageToJenkinsfile(stage, 2)
    lines.push(...stageLines)

    // 检查是否有 build 步骤，如果有则生成独立的 stage
    for (const branch of stage.branches) {
      for (const step of branch.steps) {
        if (step.type === 'build') {
          const buildName = step.name || step.config.job || '调用流水线'
          lines.push(`    stage('${buildName}') {`)
          lines.push(`      steps {`)
          lines.push(`        build job: '${step.config.job || ''}', wait: true, propagate: true`)
          lines.push(`      }`)
          lines.push(`    }`)
        }
      }
    }
  }
  lines.push('  }')

  // Post
  if (Object.keys(pipeline.post).length > 0) {
    lines.push('')
    lines.push('  post {')
    for (const [key, steps] of Object.entries(pipeline.post)) {
      if (steps && steps.length > 0) {
        lines.push(`    ${key} {`)
        for (const step of steps) {
          lines.push(...stepToJenkinsfile(step, 3))
        }
        lines.push('    }')
      }
    }
    lines.push('  }')
  }

  lines.push('}')

  return lines.join('\n')
}

function wrapString(str: string): string {
  if (str.includes('\n')) {
    return `'''${str}'''`
  }
  return `'${str}'`
}

function agentToString(agent: Pipeline['agent']): string {
  switch (agent.type) {
    case 'any':
      return 'any'
    case 'none':
      return 'none'
    case 'label':
      return `{ label '${agent.label}' }`
    case 'docker':
      return `{ docker { image '${agent.dockerImage}' } }`
    case 'kubernetes':
      return `{ kubernetes { yaml '''\n${agent.kubernetesYaml}\n''' } }`
    default:
      return 'any'
  }
}

function stageToJenkinsfile(stage: PipelineStage, indent: number): string[] {
  const lines: string[] = []
  const prefix = '  '.repeat(indent)

  lines.push(`${prefix}stage('${stage.name}') {`)

  if (stage.when) {
    if (stage.when.branch) {
      lines.push(`${prefix}  when {`)
      lines.push(`${prefix}    branch '${stage.when.branch}'`)
      lines.push(`${prefix}  }`)
    }
    if (stage.when.expression) {
      lines.push(`${prefix}  when {`)
      lines.push(`${prefix}    expression { ${stage.when.expression} }`)
      lines.push(`${prefix}  }`)
    }
  }

  if (stage.type === 'parallel') {
    lines.push(`${prefix}  parallel {`)
    for (const branch of stage.branches) {
      lines.push(`${prefix}    stage('${branch.name}') {`)
      lines.push(`${prefix}      steps {`)
      for (const step of branch.steps) {
        lines.push(...stepToJenkinsfile(step, indent + 3))
      }
      lines.push(`${prefix}      }`)
      lines.push(`${prefix}    }`)
    }
    lines.push(`${prefix}  }`)
  } else {
    const branch = stage.branches[0]
    if (branch) {
      const steps: PipelineStep[] = branch.steps
      const inputStep = steps.find(step => step.type === 'input')
      const mailStep = steps.find(step => step.type === 'mail')
      const scpStep = steps.find(step => step.type === 'scpDeploy')
      // build 步骤在 pipelineToJenkinsfile 中单独处理为独立 stage
      const otherSteps = steps.filter(step => step.type !== 'input' && step.type !== 'mail' && step.type !== 'scpDeploy' && step.type !== 'build')

      // scpDeploy environment 块
      if (scpStep) {
        const scpCfg = scpStep.config
        lines.push(`${prefix}environment {`)
        lines.push(`${prefix}  REMOTE_IP = "${scpCfg.nodeName || ''}"`)
        lines.push(`${prefix}  REMOTE_COMMAND = "${scpCfg.remoteCommand || ''}"`)
        lines.push(`${prefix}  transferFile = "${scpCfg.transferPath || ''}"`)
        lines.push(`${prefix}}`)
      }

      // input 和 steps 同级层级，不嵌套在 steps {} 块内
      if (inputStep) {
        lines.push(`${prefix}input {`)
        lines.push(`${prefix}  message "\${CHECKPOINT_MSG}"`)
        lines.push(`${prefix}  ok "yes"`)
        lines.push(`${prefix}  submitter "\${CHECKPOINT_SUBMITTER}"`)
        lines.push(`${prefix}}`)
      }

      // steps 块
      lines.push(`${prefix}steps {`)
      for (const step of otherSteps) {
        lines.push(...stepToJenkinsfile(step, indent + 1))
      }
      if (inputStep) {
        lines.push(`${prefix}  echo "审核成功，继续执行"`)
      }
      // post 邮件变量定义（放在 script 块内）
      if (mailStep) {
        const mailCfg = mailStep.config
        lines.push(`${prefix}  script {`)
        lines.push(`${prefix}    def PROJECT_NAME = '${mailCfg.projectName || ''}'`)
        lines.push(`${prefix}    def USER_EMAILS = '${mailCfg.to || ''}'`)
        lines.push(`${prefix}    def FROM_USER = '${mailCfg.from || ''}'`)
        lines.push(`${prefix}    def CHANGES = '${mailCfg.changes || ''}'`)
        lines.push(`${prefix}    def BUILD_NUMBER = '${mailCfg.number || '1'}'`)
        lines.push(`${prefix}    def BUILD_STATUS = '${mailCfg.status || '1'}'`)
        lines.push(`${prefix}    def BUILD_URL = '${mailCfg.url || '1'}'`)
        lines.push(`${prefix}  }`)
        lines.push(`${prefix}  echo "1"`)
      }
      // scpDeploy
      if (scpStep) {
        lines.push(`${prefix}  sh 'touch a.txt'`)
        lines.push(`${prefix}  sshPublisher(`)
        lines.push(`${prefix}    publishers: [`)
        lines.push(`${prefix}      sshPublisherDesc(`)
        lines.push(`${prefix}        configName: "\${REMOTE_IP}",`)
        lines.push(`${prefix}        transfers: [`)
        lines.push(`${prefix}          sshTransfer(`)
        lines.push(`${prefix}            sourceFiles: "\${transferFile}",`)
        lines.push(`${prefix}            removePrefix: "",`)
        lines.push(`${prefix}            remoteDirectory: "",`)
        lines.push(`${prefix}            execCommand: "\${REMOTE_COMMAND}"`)
        lines.push(`${prefix}          )`)
        lines.push(`${prefix}        ]`)
        lines.push(`${prefix}      )`)
        lines.push(`${prefix}    ],usePromotionTimestamp: false, useWorkspaceInPromotion: false, verbose: true`)
        lines.push(`${prefix}  )`)
      }
      lines.push(`${prefix}}`)

      // post 块（如果存在 mail 步骤）
      if (mailStep) {
        lines.push(`${prefix}post {`)
        lines.push(`${prefix}  always {`)
        lines.push(...stepToJenkinsfile(mailStep, indent + 2))
        lines.push(`${prefix}  }`)
        lines.push(`${prefix}}`)
      }
    }
  }

  lines.push(`${prefix}}`)
  return lines
}

function stepToJenkinsfile(step: PipelineStep, indent: number): string[] {
  const prefix = '  '.repeat(indent)
  const cfg = step.config

  const lines: string[] = []

  // 如果启用了执行前睡眠
  if (cfg.enableSleep && cfg.sleepTime) {
    lines.push(`${prefix}sleep ${cfg.sleepTime}`)
  }

  switch (step.type) {
    case 'sh':
      lines.push(`${prefix}sh '${cfg.command || ''}'`)
      break
    case 'bat':
      lines.push(`${prefix}bat '${cfg.command || ''}'`)
      break
    case 'powershell':
      lines.push(`${prefix}powershell '${cfg.command || ''}'`)
      break
    case 'python':
      lines.push(`${prefix}python '${cfg.script || ''}'`)
      break
    case 'dockerBuildAndPush':
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  docker.buildAndPush('${cfg.image || ''}', '${cfg.context || '.'}')`)
      lines.push(`${prefix}}`)
      break
    case 'dockerPull':
      lines.push(`${prefix}docker.pull('${cfg.image || ''}')`)
      break
    case 'dockerRun':
      lines.push(`${prefix}docker.run('${cfg.image || ''}', '${cfg.command || ''}')`)
      break
    case 'kubectl':
      lines.push(`${prefix}sh 'kubectl ${cfg.command || ''}'`)
      break
    case 'maven':
      lines.push(`${prefix}sh 'mvn ${cfg.command || 'clean package'}'`)
      break
    case 'gradle':
      lines.push(`${prefix}sh 'gradle ${cfg.command || 'build'}'`)
      break
    case 'npm':
      lines.push(`${prefix}sh 'npm ${cfg.command || 'install'}'`)
      break
    case 'ant':
      lines.push(`${prefix}ant ${cfg.target || 'build'}`)
      break
    case 'git':
      lines.push(`${prefix}git branch: '${cfg.branch || 'main'}', url: '${cfg.url || ''}'${cfg.credentialsId ? `, credentialsId: '${cfg.credentialsId}'` : ''}`)
      break
    case 'checkout':
      lines.push(`${prefix}checkout scm`)
      break
    case 'svn':
      lines.push(`${prefix}svn '${cfg.url || ''}'`)
      break
    case 'junit':
      lines.push(`${prefix}junit '${cfg.testResults || ''}'`)
      break
    case 'coverage':
      lines.push(`${prefix}coverage '${cfg.reportPath || ''}'`)
      break
    case 'findbugs':
      lines.push(`${prefix}findbugs '${cfg.pattern || ''}'`)
      break
    case 'checkstyle':
      lines.push(`${prefix}checkstyle '${cfg.pattern || ''}'`)
      break
    case 'sonar':
      lines.push(`${prefix}script {`)
      lines.push(`${prefix} def SONAR_PROJECT_NAME = '${cfg.projectName || ''}'`)
      lines.push(`${prefix} def SONAR_PROJECT_KEY = '${cfg.projectKey || ''}'`)
      lines.push(`${prefix} def SONAR_CODE_SOURCES_PATH = '${cfg.sources || ''}'`)
      lines.push(`${prefix} def SONAR_JAVA_BINARIES_PATH = '${cfg.binaryPath || ''}'`)
      lines.push(`${prefix} def SONAR_PATH = '${cfg.cliPath || ''}'`)
      lines.push(`${prefix} def SONAR_HOST_URL = '${cfg.serverUrl || ''}'`)
      lines.push(`${prefix} def SONAR_LOGIN_TOKEN = '${cfg.token || ''}'`)
      lines.push(`${prefix}sh """\${SONAR_PATH} -Dsonar.projectName=\${SONAR_PROJECT_NAME} -Dsonar.projectKey=\${SONAR_PROJECT_KEY} -Dsonar.sources=\${SONAR_CODE_SOURCES_PATH} -Dsonar.java.binaries=\${SONAR_JAVA_BINARIES_PATH} -Dsonar.host.url=http://\${SONAR_HOST_URL} -Dsonar.login=\${SONAR_LOGIN_TOKEN} """`)
      lines.push(`${prefix}}`)
      break
    case 'pytest':
      lines.push(`${prefix}sh 'pytest ${cfg.options || ''}'`)
      break
    case 'nose':
      lines.push(`${prefix}sh 'nosetests ${cfg.options || ''}'`)
      break
    case 'deploy':
      lines.push(`${prefix}deploy adapters: [${cfg.adapter || ''}], warFile: '${cfg.warFile || ''}'`)
      break
    case 'sshCommand':
      lines.push(`${prefix}sshCommand remote: ${cfg.remote || ''}, command: '${cfg.command || ''}'`)
      break
    case 'sshScript':
      lines.push(`${prefix}sshScript remote: ${cfg.remote || ''}, script: '${cfg.script || ''}'`)
      break
    case 'ansiblePlaybook':
      lines.push(`${prefix}ansiblePlaybook playbook: '${cfg.playbook || ''}', inventory: '${cfg.inventory || ''}'`)
      break
    case 'ansibleAdHoc':
      lines.push(`${prefix}ansibleAdHoc remote: ${cfg.remote || ''}, module: '${cfg.module || ''}', args: '${cfg.args || ''}'`)
      break
    case 'archiveArtifacts':
      lines.push(`${prefix}archiveArtifacts '${cfg.artifacts || ''}'`)
      break
    case 'archiveAWS S3':
      lines.push(`${prefix}s3Upload bucket: '${cfg.bucket || ''}', file: '${cfg.file || ''}'`)
      break
    case 'copyArtifacts':
      lines.push(`${prefix}copyArtifacts projectName: '${cfg.project || ''}', selector: ${cfg.selector || 'lastSuccessful()'}, target: '${cfg.target || ''}'`)
      break
    case 'stash':
      lines.push(`${prefix}stash name: '${cfg.name || ''}', includes: '${cfg.includes || ''}'`)
      break
    case 'unstash':
      lines.push(`${prefix}unstash '${cfg.name || ''}'`)
      break
    case 'cleanWs':
      lines.push(`${prefix}cleanWs()`)
      break
    case 'deleteDir':
      lines.push(`${prefix}deleteDir()`)
      break
    case 'writeFile':
      lines.push(`${prefix}writeFile file: '${cfg.file || ''}', text: ${wrapString(cfg.text || '')}`)
      break
    case 'readFile':
      lines.push(`${prefix}readFile '${cfg.file || ''}'`)
      break
    case 'input':
      lines.push(`${prefix} def CHECKPOINT_MSG = '${cfg.message || ''}'`)
      lines.push(`${prefix} def CHECKPOINT_SUBMITTER = '${cfg.approver || ''}'`)
      lines.push(`${prefix}input {`)
      lines.push(`${prefix}  message "\${CHECKPOINT_MSG}"`)
      lines.push(`${prefix}  ok "yes"`)
      lines.push(`${prefix}  submitter "\${CHECKPOINT_SUBMITTER}"`)
      lines.push(`${prefix}}`)
      break
    case 'timeout':
      lines.push(`${prefix}timeout(time: ${cfg.time || 60}, unit: '${cfg.unit || 'MINUTES'}') {`)
      lines.push(`${prefix}  ${cfg.step || 'echo "Timeout block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'retry':
      lines.push(`${prefix}retry(${cfg.count || 3}) {`)
      lines.push(`${prefix}  ${cfg.step || 'echo "Retry block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'sleep':
      lines.push(`${prefix}sleep ${cfg.time || 10}`)
      break
    case 'waitUntil':
      lines.push(`${prefix}waitUntil {`)
      lines.push(`${prefix}  ${cfg.condition || 'echo "Waiting..."'}`)
      lines.push(`${prefix}}`)
      break
    case 'catchError':
      lines.push(`${prefix}catchError {`)
      lines.push(`${prefix}  ${cfg.block || 'echo "Error caught"'}`)
      lines.push(`${prefix}}`)
      break
    case 'error':
      lines.push(`${prefix}error '${cfg.message || 'Build failed'}'`)
      break
    case 'unstable':
      lines.push(`${prefix}unstable '${cfg.message || 'Build marked unstable'}'`)
      break
    case 'lock':
      lines.push(`${prefix}lock(resource: '${cfg.resource || ''}', skipIfLocked: ${cfg.skipIfLocked || false}) {`)
      lines.push(`${prefix}  ${cfg.block || 'echo "Locked"'}`)
      lines.push(`${prefix}}`)
      break
    case 'milestone':
      lines.push(`${prefix}milestone()`)
      break
    case 'withCredentials':
      lines.push(`${prefix}withCredentials([${cfg.credentials || ''}]) {`)
      lines.push(`${prefix}  ${cfg.step || 'echo "Credentials block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'withEnv':
      lines.push(`${prefix}withEnv(['${cfg.env || ''}']) {`)
      lines.push(`${prefix}  ${cfg.step || 'echo "Env block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'withAWS':
      lines.push(`${prefix}withAWS(${cfg.region ? `region: '${cfg.region}'` : ''}) {`)
      lines.push(`${prefix}  ${cfg.step || 'echo "AWS block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'withDockerRegistry':
      lines.push(`${prefix}withDockerRegistry(${cfg.registry || ''}) {`)
      lines.push(`${prefix}  ${cfg.step || 'echo "Docker registry block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'dir':
      lines.push(`${prefix}dir('${cfg.path || ''}') {`)
      lines.push(`${prefix}  ${cfg.step || 'echo "Dir block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'chdir':
      lines.push(`${prefix}chdir '${cfg.path || ''}'`)
      break
    case 'tool':
      lines.push(`${prefix}tool '${cfg.name || ''}'`)
      break
    case 'echo':
      lines.push(`${prefix}echo ${wrapString(cfg.message || '')}`)
      break
    case 'mail':
      lines.push(`${prefix} emailext(`)
      lines.push(`${prefix}   attachLog: true,`)
      lines.push(`${prefix}   subject: "构建通知:\${PROJECT_NAME}-BUILD#\${BUILD_NUMBER}-\${BUILD_STATUS}",`)
      lines.push(`${prefix}   to: "\${USER_EMAILS}",`)
      lines.push(`${prefix}   body: '''<!DOCTYPE html>
      <html>
      <meta charset="UTF-8">
      <body>
      <table>
        <li>构建项目：\${PROJECT_NAME}</li>
        <li>构建结果：\${BUILD_STATUS}</li>
        <li>构建编号：\${BUILD_NUMBER}</li>
        <li>发件用户：\${FROM_USER}</li>
        <li>变更概要：\${CHANGES}</li>
        <li>构建地址：\${BUILD_URL}</li>
        <li>构建日志：\${BUILD_URL}console</li>
      </table>
      </body>
      </html>'''`)
      lines.push(`${prefix} )`)
      break
    case 'slackSend':
      lines.push(`${prefix}slackSend channel: '${cfg.channel || ''}', message: ${wrapString(cfg.message || '')}`)
      break
    case 'dingtalk':
      lines.push(`${prefix}dingtalk ${wrapString(cfg.message || '')}`)
      break
    case 'wechat':
      lines.push(`${prefix}wechat ${wrapString(cfg.message || '')}`)
      break
    case 'build':
      lines.push(`${prefix}build job: '${cfg.job || ''}', wait: true, propagate: true`)
      break
    case 'script':
      // 检测 scriptBlock 是否包含 javaImage 构建内容（不含 cleanup）
      const scriptBlock = cfg.scriptBlock || cfg.script || ''
      if (scriptBlock.includes('IMG_NAME') && scriptBlock.includes('docker build') && !scriptBlock.includes('docker rmi')) {
        // 从 scriptBlock 中解析 imageName 和 dockerfileContent
        const imgNameMatch = scriptBlock.match(/def\s+IMG_NAME\s*=\s*['"]([^'"]*)['"]/)
        // 解析 Dockerfile 内容 - 匹配 sh '''xxx'''格式
        const dockerfileMatch = scriptBlock.match(/sh\s+['"][^'"]*<<DOCKERFILE_EOF['"]\s*\n\s*sh\s+['"]([\s\S]*?)['"]/)
        const imageName = imgNameMatch ? imgNameMatch[1] : ''
        const dockerfileContent = dockerfileMatch ? dockerfileMatch[1] : ''

        lines.push(`${prefix}script {`)
        lines.push(`${prefix}  def IMG_NAME = '${imageName}'`)
        lines.push(`${prefix}  sh '\''cat > Dockerfile <<DOCKERFILE_EOF'\''`)
        lines.push(`${prefix}  sh '\''${dockerfileContent || 'FROM openjdk:8'}'\''`)
        lines.push(`${prefix}  sh '\''DOCKERFILE_EOF'\''`)
        lines.push(`${prefix}  sh '\''docker build -t \${IMG_NAME} -f Dockerfile .'\''`)
        lines.push(`${prefix}  def imgFullId = sh(returnStdout: true, script: 'docker images --filter=reference="${imageName}" -q --no-trunc | head -n 1').trim()`)
        lines.push(`${prefix}  def imgShortId = imgFullId.replaceFirst('sha256:','').substring(0,12)`)
        lines.push(`${prefix}  echo '\${imgShortId}'`)
        lines.push(`${prefix}  env.IMG_SHORT_Id=imgShortId`)
        lines.push(`${prefix}  sh 'docker save -o \${IMG_NAME}_\${imgShortId}.tar \${IMG_NAME}'`)
        lines.push(`${prefix}  env.IMG_WITH_ID='\${IMG_NAME}_\${imgShortId}'`)
        lines.push(`${prefix}}`)
      } else if (scriptBlock.includes('docker rmi')) {
        // cleanup 脚本，不生成内容（会在下一个 script步骤中单独处理）
        // 或者直接输出 cleanup 命令
        lines.push(`${prefix}script {`)
        lines.push(`${prefix}  ${scriptBlock.trim()}`)
        lines.push(`${prefix}}`)
      } else {
        // 普通 script块
        lines.push(`${prefix}script {`)
        // 处理多行脚本内容，每行缩进
        const scriptLines = scriptBlock.split('\n')
        for (const line of scriptLines) {
          lines.push(`${prefix}  ${line}`)
        }
        lines.push(`${prefix}}`)
      }
      break
    case 'parallel':
      lines.push(`${prefix}parallel {`)
      lines.push(`${prefix}  ${cfg.block || 'echo "Parallel block"'}`)
      lines.push(`${prefix}}`)
      break
    case 'structuredClone':
      lines.push(`${prefix}structuredClone()`)
      break
    case 'jar':
      lines.push(`${prefix}sh 'mvn ${cfg.command || 'clean package'}'`)
      break
    case 'cpp':
      lines.push(`${prefix}sh '${cfg.command || 'cmake .. && make'}'`)
      break
    case 'go':
      lines.push(`${prefix}sh 'go build ${cfg.command || ''}'`)
      break
    case 'qt':
      lines.push(`${prefix}sh '${cfg.command || 'qmake && make'}'`)
      break
    case 'javaImage':
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  def IMG_NAME = '${cfg.imageName || ''}'`)
      lines.push(`${prefix}  sh '\''cat > Dockerfile <<DOCKERFILE_EOF'\''`)
      lines.push(`${prefix}  sh '\''${cfg.dockerfileContent || 'FROM openjdk:8'}'\''`)
      lines.push(`${prefix}  sh '\''DOCKERFILE_EOF'\''`)
      lines.push(`${prefix}  sh '\''docker build -t \${IMG_NAME} -f Dockerfile .'\''`)
      lines.push(`${prefix}  def imgFullId = sh(returnStdout: true, script: 'docker images --filter=reference="${cfg.imageName}" -q --no-trunc | head -n 1').trim()`)
      lines.push(`${prefix}  def imgShortId = imgFullId.replaceFirst('sha256:','').substring(0,12)`)
      lines.push(`${prefix}  echo '\${imgShortId}'`)
      lines.push(`${prefix}  env.IMG_SHORT_Id=imgShortId`)
      lines.push(`${prefix}  sh 'docker save -o \${IMG_NAME}_\${imgShortId}.tar \${IMG_NAME}'`)
      lines.push(`${prefix}  env.IMG_WITH_ID='\${IMG_NAME}_\${imgShortId}'`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  sh 'docker rmi \${env.IMG_SHORT_Id}'`)
      lines.push(`${prefix}}`)
      break
    case 'node':
      lines.push(`${prefix}sh 'npm ${cfg.command || 'install && npm run build'}'`)
      break
    case 'net':
      lines.push(`${prefix}sh 'dotnet ${cfg.command || 'build'}'`)
      break
    case 'python':
      lines.push(`${prefix}sh 'python ${cfg.command || 'setup.py build'}'`)
      break
    case 'php':
      lines.push(`${prefix}sh 'composer ${cfg.command || 'install'}'`)
      break
    case 'pushImageDeploy':
    case 'pushImageRelease':
      lines.push(`${prefix}script {`)
      // 构建 uploadProductApplicationBo JSON
      const appBo = {
        appId: cfg.appId || '${APP_ID}',
        appCode: cfg.appCode || '${APP_CODE}',
        appName: cfg.appName || '${APP_NAME}',
        appVersionId: cfg.appVersionId || '${APP_VERSION_ID}',
        appVersionName: cfg.appVersionName || '${VERSION_NAME}',
        tripartiteName: cfg.tripartiteName || ''
      }
      const uploadProductApplicationBo = JSON.stringify(appBo).replace(/"/g, '\\"')
      lines.push(`${prefix}  def productFlag = "${cfg.serviceAlias || '${SERVICE_ALIAS}'}".toLowerCase()`)
      lines.push(`${prefix}  def pushResponse = sh(returnStdout:true, script: """`)
      lines.push(`${prefix}curl -X POST ${cfg.productRepoUrl || '${PRODUCT_REPO_URL}'}/product/jenkinsClient/push/${cfg.userToken || '${USER_TOKEN}'} \\\\`)
      lines.push(`${prefix}--form 'file=@${cfg.filePath || '${env.IMG_WITH_ID}'}.tar' \\\\`)
      lines.push(`${prefix}--form 'productFlag="\${productFlag}"' \\\\`)
      lines.push(`${prefix}--form 'productRepositoryName="${cfg.productRepoName || '${PRODUCT_REPO_NAME}'}"' \\\\`)
      lines.push(`${prefix}--form 'productFlagName="${cfg.productFlagName || '${PRODUCT_FLAG_NAME}'}"' \\\\`)
      lines.push(`${prefix}--form 'productVersion="snapshotshot"' \\\\`)
      lines.push(`${prefix}--form 'productPublishVersion="temp"' \\\\`)
      lines.push(`${prefix}--form 'productStatus="${cfg.productStatus || 'product_status_dev'}"' \\\\`)
      lines.push(`${prefix}--form 'jenkinsName="${cfg.jenkinsName || '${JOB_NAME}'}"' \\\\`)
      lines.push(`${prefix}--form 'jenkinsNumber="${cfg.jenkinsNumber || '${BUILD_NUMBER}'}"' \\\\`)
      lines.push(`${prefix}--form 'gitlabUri="${cfg.gitlabUri || '${CODE_REPOSITORY_ID2_DISPLAY_URL}'}.git"' \\\\`)
      lines.push(`${prefix}--form 'gitlabBranch="${cfg.gitlabBranch || '${CURBRANCH}'}"' \\\\`)
      lines.push(`${prefix}--form 'safeCode="${cfg.safeCode ?? 0}"' \\\\`)
      lines.push(`${prefix}--form 'coverUpload="true"' \\\\`)
      lines.push(`${prefix}--form 'cpu="${cfg.cpu || '${CPU_NUM}'}"' \\\\`)
      lines.push(`${prefix}--form 'memory="${cfg.memory || '${MEMORY}'}"' \\\\`)
      lines.push(`${prefix}--form 'framework="${cfg.framework || '${ARCH}'}"' \\\\`)
      lines.push(`${prefix}--form 'exposePortBoList=${cfg.exposePortList || '${EXPOSE_PORT}'}' \\\\`)
      lines.push(`${prefix}--form 'uploadProductApplicationBo="${uploadProductApplicationBo}"'`)
      lines.push(`${prefix}""").trim()`)
      lines.push(`${prefix}  echo "\${pushResponse}"`)
      lines.push(`${prefix}  def jsonSlurper = new groovy.json.JsonSlurper()`)
      lines.push(`${prefix}  def statusCode = jsonSlurper.parseText(pushResponse).code?.toString()?.trim()`)
      lines.push(`${prefix}  if ('200'!="\${statusCode}") {`)
      lines.push(`${prefix}      def msgResponse = jsonSlurper.parseText(pushResponse).msg?.toString()?.trim()`)
      lines.push(`${prefix}      error "\${msgResponse}"`)
      lines.push(`${prefix}  } else {`)
      lines.push(`${prefix}      def responseCreateName = jsonSlurper.parseText(pushResponse).data?.dockerDownloadFileVo?.dockerPullUrl?.toString()?.trim()`)
      lines.push(`${prefix}      env.IMG_URL = responseCreateName`)
      lines.push(`${prefix}  }`)
      lines.push(`${prefix}  sh "rm -f \${env.IMG_WITH_ID}.tar"`)
      lines.push(`${prefix}}`)
      break
    case 'apiTest':
      lines.push(`${prefix}script {`)
      lines.push(`${prefix} def POSTMAN_COLLECTION = '${cfg.collectionPath || ''}'`)
      lines.push(`${prefix} def POSTMAN_ENV = '${cfg.environment || ''}'`)
      lines.push(`${prefix} def TEST_REPORT = '${cfg.reportName || ''}'`)
      lines.push(`${prefix}}`)
      //lines.push(`${prefix}sh 'newman run -Dcommand=${cfg.command || ''} -DcollectionPath=${cfg.collectionPath || ''} -Denvironment=${cfg.environment || ''} -DreportName=${cfg.reportName || ''}'`)
      lines.push(`${prefix}sh '''#!/bin/bash
        sleep 3
        cd \$WORKSPACE
        newman run \${POSTMAN_COLLECTION} -e \${POSTMAN_ENV} -k -r cli,html,json,junit --reporter-html-export \${TEST_REPORT}
        exit 0
        '''`)
      break
    case 'cloudDeploy':
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  // 1. 登录获取鉴权token`)
      lines.push(`${prefix}  def getAuthToken = sh(returnStdout:true, script: """`)
      lines.push(`${prefix}  curl ${cfg.appManageUrl || '\${APP_MANAGE_URL}'}/api/v1/auth/login -X POST \\\\`)
      lines.push(`${prefix}  -H 'Accept: application/json' \\\\`)
      lines.push(`${prefix}  -H 'Content-Type: application/json' \\\\`)
      lines.push(`${prefix}  --data-raw '{"username":"${cfg.username || 'admin'}","password":"${cfg.password || ''}"}'`)
      lines.push(`${prefix}  """).trim()`)
      lines.push(`${prefix}  def jsonSlurper = new groovy.json.JsonSlurper()`)
      lines.push(`${prefix}  def accessToken = jsonSlurper.parseText(getAuthToken).accessToken?.toString()?.trim()`)
      lines.push(`${prefix}  def refreshToken = jsonSlurper.parseText(getAuthToken).refreshToken?.toString()?.trim()`)
      lines.push(`${prefix}  env.ACCESS_TOKEN = "Bearer \${accessToken}"`)
      lines.push(`${prefix}  env.REFRESH_TOKEN = refreshToken`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  // recycle 重置环境实例`)
      lines.push(`${prefix}  def recycleResp = sh(returnStdout:true, script: """`)
      lines.push(`${prefix}  curl ${cfg.appManageUrl || '\${APP_MANAGE_URL}'}/api/v1/applications/\${env.DEPLOY_APP_NAME}/envs/${cfg.namespace || 'k8s-workbenches-dev'}/recycle -X POST \\\\`)
      lines.push(`${prefix}  -H "Accept: application/json" \\\\`)
      lines.push(`${prefix}  -H "Authorization: \${ACCESS_TOKEN}" \\\\`)
      lines.push(`${prefix}  -H "X-Refresh-Token: \${REFRESH_TOKEN}" \\\\`)
      lines.push(`${prefix}  --data-raw '{}'`)
      lines.push(`${prefix}  """).trim()`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  // 处理端口列表`)
      lines.push(`${prefix}  def CODE_REPOSITORY_ID2_DISPLAY = "${cfg.codeRepositoryId2Display || '\${CODE_REPOSITORY_ID2_DISPLAY}'}".toLowerCase()`)
      lines.push(`${prefix}  def portList = new groovy.json.JsonSlurper().parseText("${cfg.exposePort || '\${EXPOSE_PORT}'}")`)
      lines.push(`${prefix}  portList.each { item ->`)
      lines.push(`${prefix}    item.expose=true`)
      lines.push(`${prefix}    item.port=item.port.toInteger()`)
      lines.push(`${prefix}    echo "item - \${item}"`)
      lines.push(`${prefix}  }`)
      lines.push(`${prefix}  portList= groovy.json.JsonOutput.toJson(portList)`)
      lines.push(`${prefix}  // 创建/更新应用配置`)
      lines.push(`${prefix}  def postAppCreation = sh(returnStdout:true, script: """`)
      lines.push(`${prefix}  curl ${cfg.appManageUrl || '\${APP_MANAGE_URL}'}/capi/plan.sakura.io/v1/app -X POST \\\\`)
      lines.push(`${prefix}  -H 'Accept: application/json' \\\\`)
      lines.push(`${prefix}  -H 'Content-Type: application/json' \\\\`)
      lines.push(`${prefix}  -H "Authorization: \${ACCESS_TOKEN}" \\\\`)
      lines.push(`${prefix}  -H "X-Refresh-Token: \${REFRESH_TOKEN}" \\\\`)
      lines.push(`${prefix}  --data-raw '{\\"planType\\":\\"CreateOrUpdate\\",\\"name\\":\\"\${env.DEPLOY_APP_NAME}\\",\\"alias\\":\\"${cfg.productFlagName || '${PRODUCT_FLAG_NAME}'}\\",\\"description\\":\\"\${env.DEPLOY_APP_NAME} desc\\",\\"project\\":\\"default\\",\\"envBinding\\":[{\\"name\\":\\"${cfg.namespace || 'k8s-workbenches-dev'}\\"}],\\"components\\":[{\\"name\\":\\"main\\",\\"mainComp\\":true,\\"alias\\":\\"\${CODE_REPOSITORY_ID2_DISPLAY}-alias\\",\\"description\\":\\"desc\\",\\"componentType\\":\\"webservice\\",\\"ComponentRawMsg\\":{\\"memory\\":\\"${cfg.memory || '${MEMORY}'}M\\",\\"cpu\\":\\"${cfg.cpuNum || '${CPU_NUM}'}\\",\\"exposeType\\":\\"NodePort\\",\\"ports\\":\${portList},\\"image\\":\\"\${env.IMG_URL}\\",\\"imagePullPolicy\\":\\"Always\\"}}]}'`)
      lines.push(`${prefix}  """).trim()`)
      lines.push(`${prefix}  echo "\${postAppCreation}"`)
      lines.push(`${prefix}  def responseCreateName = new groovy.json.JsonSlurper().parseText(postAppCreation).appName?.toString()?.trim()`)
      lines.push(`${prefix}  echo "\${responseCreateName}"`)
      lines.push(`${prefix}  if (responseCreateName!="\${env.DEPLOY_APP_NAME}"){`)
      lines.push(`${prefix}    error("返回失败")`)
      lines.push(`${prefix}  }`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  // 获取部署工作流名称`)
      lines.push(`${prefix}  def getWorkflow =sh(returnStdout:true, script: """`)
      lines.push(`${prefix}  curl ${cfg.appManageUrl || '\${APP_MANAGE_URL}'}/api/v1/applications/\${env.DEPLOY_APP_NAME}/envs -X GET \\\\`)
      lines.push(`${prefix}  -H 'Accept: application/json' \\\\`)
      lines.push(`${prefix}  -H 'Content-Type: application/json' \\\\`)
      lines.push(`${prefix}  -H "Authorization: \${ACCESS_TOKEN}" \\\\`)
      lines.push(`${prefix}  -H "X-Refresh-Token: \${REFRESH_TOKEN}"`)
      lines.push(`${prefix}  """).trim()`)
      lines.push(`${prefix}  def jsonSlurper = new groovy.json.JsonSlurper()`)
      lines.push(`${prefix}  def responseWorkflow = jsonSlurper.parseText(getWorkflow)`)
      lines.push(`${prefix}  echo "\${getWorkflow}"`)
      lines.push(`${prefix}  if (responseWorkflow ) {`)
      lines.push(`${prefix}    env.WORKFLOW_NAME=responseWorkflow?.envBindings[0]?.workflow?.name.toString()?.trim()`)
      lines.push(`${prefix}    echo "工作流获取成功"`)
      lines.push(`${prefix}  }else{`)
      lines.push(`${prefix}    error("获取工作流失败")`)
      lines.push(`${prefix}  }`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  echo "\${env.WORKFLOW_NAME}"`)
      lines.push(`${prefix}  echo "开始部署"`)
      lines.push(`${prefix}  // 触发部署`)
      lines.push(`${prefix}  def deployResponse = sh(returnStdout:true, script: """`)
      lines.push(`${prefix}  curl ${cfg.appManageUrl || '\${APP_MANAGE_URL}'}/api/v1/applications/\${env.DEPLOY_APP_NAME}/deploy -X POST \\\\`)
      lines.push(`${prefix}  -H 'Accept: application/json' \\\\`)
      lines.push(`${prefix}  -H 'Content-Type: application/json' \\\\`)
      lines.push(`${prefix}  -H "Authorization: \${ACCESS_TOKEN}" \\\\`)
      lines.push(`${prefix}  -H "X-Refresh-Token: \${REFRESH_TOKEN}" \\\\`)
      lines.push(`${prefix}  --data-raw '{\\"appName\\":\\"\${env.DEPLOY_APP_NAME}\\",\\"workflowName\\":\\"\${env.WORKFLOW_NAME}\\",\\"triggerType\\":\\"web\\",\\"force\\":true}'`)
      lines.push(`${prefix}  """).trim()`)
      lines.push(`${prefix}  def jsonSlurper = new groovy.json.JsonSlurper()`)
      lines.push(`${prefix}  if (deployResponse) {`)
      lines.push(`${prefix}    echo "\${deployResponse}"`)
      lines.push(`${prefix}    def workflowRecord = jsonSlurper.parseText(deployResponse)?.record?.name`)
      lines.push(`${prefix}    env.WORKFLOW_RECORD = workflowRecord`)
      lines.push(`${prefix}  }else{`)
      lines.push(`${prefix}    error("部署失败")`)
      lines.push(`${prefix}  }`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  // 封装查询部署状态方法`)
      lines.push(`${prefix}  def queryDeployStatus = {`)
      lines.push(`${prefix}    def appStatusResponse = sh(returnStdout:true, script:"""`)
      lines.push(`${prefix}    curl ${cfg.appManageUrl || '\${APP_MANAGE_URL}'}/api/v1/applications/\${env.DEPLOY_APP_NAME}/workflows/\${env.WORKFLOW_NAME}/records/\${env.WORKFLOW_RECORD} -X GET \\\\`)
      lines.push(`${prefix}    -H 'Accept: application/json' \\\\`)
      lines.push(`${prefix}    -H 'Content-Type: application/json' \\\\`)
      lines.push(`${prefix}    -H "Authorization: \${ACCESS_TOKEN}" \\\\`)
      lines.push(`${prefix}    -H "X-Refresh-Token: \${REFRESH_TOKEN}"`)
      lines.push(`${prefix}    """).trim()`)
      lines.push(`${prefix}    echo "\${appStatusResponse}"`)
      lines.push(`${prefix}    def jsonSlurper = new groovy.json.JsonSlurper()`)
      lines.push(`${prefix}    def deployStatusInfo = jsonSlurper.parseText(appStatusResponse)`)
      lines.push(`${prefix}    return deployStatusInfo`)
      lines.push(`${prefix}  }`)
      lines.push(`${prefix}  // 循环轮询部署状态，最多5次，间隔20s`)
      lines.push(`${prefix}  for(int i=0;i<5;i++){`)
      lines.push(`${prefix}    sleep 20`)
      lines.push(`${prefix}    def workflowInfo = queryDeployStatus()`)
      lines.push(`${prefix}    def deployStatus = workflowInfo?.status.toString()?.trim()`)
      lines.push(`${prefix}    if(deployStatus=='succeeded'){`)
      lines.push(`${prefix}      env.deploy_STATUS=true`)
      lines.push(`${prefix}      echo "\${workflowInfo}"`)
      lines.push(`${prefix}      break`)
      lines.push(`${prefix}    }else if(deployStatus=='terminated' || deployStatus=='failed'){`)
      lines.push(`${prefix}      echo "\${workflowInfo}"`)
      lines.push(`${prefix}      error("部署错误")`)
      lines.push(`${prefix}    }else if(deployStatus=='executing'||deployStatus=='initializing'){`)
      lines.push(`${prefix}      if(i==4){`)
      lines.push(`${prefix}        echo "\${workflowInfo}"`)
      lines.push(`${prefix}        error("部署超时")`)
      lines.push(`${prefix}      }`)
      lines.push(`${prefix}      continue`)
      lines.push(`${prefix}    }else{`)
      lines.push(`${prefix}      echo "\${workflowInfo}"`)
      lines.push(`${prefix}      error("部署错误")`)
      lines.push(`${prefix}    }`)
      lines.push(`${prefix}  }`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  // 查询应用访问端点`)
      lines.push(`${prefix}  def CODE_REPOSITORY_ID2_DISPLAY = "${cfg.codeRepositoryId2Display || '\${CODE_REPOSITORY_ID2_DISPLAY}'}".toLowerCase()`)
      lines.push(`${prefix}  def appEndpointResponse = sh(returnStdout:true, script: """`)
      lines.push(`${prefix}  curl --location --globoff '${cfg.appManageUrl || '\${APP_MANAGE_URL}'}/api/v1/query?velql=service-endpoints-view(appNs%3D${cfg.namespace || 'k8s-workbenches-dev'}%2CappName%3D\${env.DEPLOY_APP_NAME}).status' \\\\`)
      lines.push(`${prefix}  -H 'Accept: application/json' \\\\`)
      lines.push(`${prefix}  -H 'Content-Type: application/json' \\\\`)
      lines.push(`${prefix}  -H "Authorization: \${ACCESS_TOKEN}" \\\\`)
      lines.push(`${prefix}  -H "X-Refresh-Token: \${REFRESH_TOKEN}"`)
      lines.push(`${prefix}  """).trim()`)
      lines.push(`${prefix}  echo "\${appEndpointResponse}"`)
      lines.push(`${prefix}  def jsonSlurper = new groovy.json.JsonSlurper()`)
      lines.push(`${prefix}  def endpointInfo = jsonSlurper.parseText(appEndpointResponse)?.endpoints`)
      lines.push(`${prefix}  def endpointHost = endpointInfo[0]?.endpoint?.host.toString()?.trim()`)
      lines.push(`${prefix}  def endpointPort = endpointInfo[0]?.endpoint?.port.toString()?.trim()`)
      lines.push(`${prefix}  env.endpointUrl= "\${endpointHost}:\${endpointPort}"`)
      lines.push(`${prefix}}`)
      lines.push(`${prefix}script {`)
      lines.push(`${prefix}  env.PIPELINE_STATUS=true`)
      lines.push(`${prefix}}`)
      break
    case 'scpDeploy':
      break
    case 'custom':
      lines.push(`${prefix}${cfg.code || ''}`)
      break
    default:
      lines.push(`${prefix}// Unknown step type: ${step.type}`)
  }

  return lines
}

export function jenkinsfileToPipeline(jenkinsfile: string, pipelineName: string): Partial<Pipeline> {
  const pipeline: Partial<Pipeline> = {
    name: pipelineName,
    description: 'Imported from Jenkinsfile',
    agent: { type: 'any' },
    environment: [],
    stages: [],
    post: {},
  }

  const agentMatch = jenkinsfile.match(/agent\s+(\w+)/)
  if (agentMatch) {
    pipeline.agent = { type: agentMatch[1] as any }
  }

  const envMatch = jenkinsfile.match(/environment\s*\{([^}]*)\}/s)
  if (envMatch) {
    const envLines = envMatch[1].split('\n')
    for (const line of envLines) {
      const match = line.match(/(\w+)\s*=\s*['"]([^'"]*)['"]/)
      if (match) {
        pipeline.environment?.push({ key: match[1], value: match[2] })
      }
    }
  }

  const stagesMatch = jenkinsfile.match(/stages\s*\{([\s\S]*)\}\s*(?:post|\})/)
  if (stagesMatch) {
    const stageMatches = stagesMatch[1].matchAll(/stage\s*\(['"]([^'"]*)['"]\)\s*\{([^}]*)\}/g)
    for (const match of stageMatches) {
      const stage: PipelineStage = {
        id: `stage_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
        name: match[1],
        type: 'sequential',
        branches: [{
          id: `branch_${Date.now()}`,
          name: match[1],
          steps: []
        }]
      }
      pipeline.stages?.push(stage)
    }
  }

  return pipeline
}
