import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Pipeline, PipelineStage, PipelineStep, PipelineRun, StageStatus } from '@/types/pipeline'
import { v4 as uuidv4 } from 'uuid'
import { pipelineToJenkinsfile } from '@/utils/jenkinsfile'
import { generateMockLogs } from '@/utils/layout'
import axios from '@/utils/axios'

// const pipeline3: Pipeline = {
//   "id": uuidv4(),
//   "name": "流水线3",
//   "description": "流水线3",
//   "agent": {
//     "type": "any"
//   },
//   "environment": [],
//   "stages": [
//     {
//       "id": uuidv4(),
//       "name": "代码拉取",
//       "type": "sequential",
//       "branches": [
//         {
//           "id": uuidv4(),
//           "name": "默认分支",
//           "steps": [
//             {
//               "id": uuidv4(),
//               "type": "git",
//               "name": "新步骤",
//               "config": {
//                 "url": "github.com/memorychen",
//                 "branch": "main"
//               }
//             }
//           ]
//         }
//       ]
//     },
//     {
//       "id": uuidv4(),
//       "name": "构建",
//       "type": "sequential",
//       "branches": [
//         {
//           "id": uuidv4(),
//           "name": "默认分支",
//           "steps": [
//             {
//               "id": uuidv4(),
//               "type": "sh",
//               "name": "Maven 构建",
//               "config": {
//                 "command": "mvn clean package",
//                 "timeout": 30
//               }
//             },
//             {
//               "id": uuidv4(),
//               "type": "sh",
//               "name": "NPM 构建",
//               "config": {
//                 "command": "npm install && npm run build",
//                 "timeout": 30
//               }
//             },
//             {
//               "id": uuidv4(),
//               "type": "sh",
//               "name": "Docker 构建",
//               "config": {
//                 "command": "docker build -t app .",
//                 "timeout": 30
//               }
//             }
//           ]
//         }
//       ]
//     },
//     {
//       "id": uuidv4(),
//       "name": "测试",
//       "type": "sequential",
//       "branches": [
//         {
//           "id": uuidv4(),
//           "name": "默认分支",
//           "steps": [
//             {
//               "id": uuidv4(),
//               "type": "junit",
//               "name": "单元测试",
//               "config": {
//                 "testResults": "**/target/surefire-reports/*.xml"
//               }
//             },
//             {
//               "id": uuidv4(),
//               "type": "sh",
//               "name": "代码扫描",
//               "config": {
//                 "command": "sonar-scanner"
//               }
//             }
//           ]
//         }
//       ]
//     },
//     {
//       "id": uuidv4(),
//       "name": "部署",
//       "type": "sequential",
//       "branches": [
//         {
//           "id": uuidv4(),
//           "name": "默认分支",
//           "steps": [
//             {
//               "id": uuidv4(),
//               "type": "sh",
//               "name": "主机部署",
//               "config": {
//                 "host": "",
//                 "script": "./deploy.sh"
//               }
//             },
//             {
//               "id": uuidv4(),
//               "type": "sh",
//               "name": "K8s 部署",
//               "config": {
//                 "command": "kubectl apply -f deployment.yaml"
//               }
//             }
//           ]
//         }
//       ]
//     }
//   ],
//   "post": {},
//   createdAt: new Date().toISOString(),
//   updatedAt: new Date().toISOString()
// }
export const usePipelineStore = defineStore('pipeline', () => {
  // State
  const pipelines = ref<Pipeline[]>([])
  const currentPipelineId = ref<string>('')
  const runs = ref<PipelineRun[]>([])
  const isRunning = ref(false)
  const currentRunId = ref<string | null>(null)

  // 初始化时从后端获取流水线数据
  async function fetchPipelines() {
    try {
      const res = await axios.get('/api/pipeline/pipelines')
      console.log('API 返回数据:', res.data)
      const data = Array.isArray(res.data) ? res.data : ((res.data as any)?.data || [])
      pipelines.value = data.map((item: { id: string; name: string; description?: string; stageNum?: number }) => ({
        id: item.id,
        name: item.name,
        description: item.description || '',
        agent: { type: 'any' },
        environment: [],
        stages: [],
        post: {},
        createdAt: '',
        updatedAt: '',
        stageNum: item.stageNum
      }))
      console.log('映射后的 pipelines:', pipelines.value)
      // 注意：不再自动加载第一个流水线的内容，由各页面按需调用 fetchPipelineContent
    } catch (error) {
      console.error('获取流水线列表失败:', error)
    }
  }

  // 根据流水线名称获取详细内容
  async function fetchPipelineContent(id: string) {
    try {
      const res = await axios.get(`/api/pipeline/pipeline/content`,{
        params: {
          pipeLineId :id,
        }
      })
      console.log('流水线内容:', res.data)
      const contentData = (res.data as any)?.data || res.data
      console.log('contentData.json 原始内容:', contentData?.json)

           // 将后端返回的步骤格式转换为前端格式
      function convertStepFromBackend(backendStep: any): PipelineStep {
        const config: Record<string, any> = {}
        if (backendStep.arguments) {
          // 兼容数组格式 [{key, value}] 和对象格式 {isLiteral, value} (如 python 类型)
          if (Array.isArray(backendStep.arguments)) {
            for (const arg of backendStep.arguments) {
              config[arg.key] = arg.value?.isLiteral ? arg.value.value : arg.value
            }
          } else if (backendStep.arguments.isLiteral !== undefined && backendStep.arguments.value !== undefined) {
            // { isLiteral, value } 格式，直接提取 value
            config.script = backendStep.arguments.value
          } else {
            // 普通对象格式 {key: value}
            Object.entries(backendStep.arguments).forEach(([key, value]: [string, any]) => {
              config[key] = value?.isLiteral ? value.value : value
            })
          }
        }

        // 自动识别 jar 类型：如果 sh 命令以 mvn 开头，则识别为 jar 类型
        let stepType = backendStep.name as PipelineStep['type']
        let stepName = backendStep.name
        const cmd = config.script || config.command || ''
        if (stepType === 'sh' && cmd.trim().startsWith('mvn')) {
          stepType = 'jar'
          stepName = '构建jar'
        }

        // 自动识别 node 类型：如果 sh 命令以 npm 开头，则识别为 node 类型
        if (stepType === 'sh' && cmd.trim().startsWith('npm')) {
          stepType = 'node'
          stepName = 'Node编译'
        }

        // 自动识别 cpp 类型：如果 sh 命令包含 cmake，则识别为 cpp 类型
        if (stepType === 'sh' && cmd.includes('cmake')) {
          stepType = 'cpp'
          stepName = 'C++多平台构建'
        }

        // 自动识别 go 类型：如果 sh 命令包含 go build 或 go run，则识别为 go 类型
        if (stepType === 'sh' && (cmd.includes('go build') || cmd.includes('go run'))) {
          stepType = 'go'
          stepName = 'Go多平台构建'
        }

        // 自动识别 qt 类型：如果 sh 命令包含 qmake，则识别为 qt 类型
        if (stepType === 'sh' && cmd.includes('qmake')) {
          stepType = 'qt'
          stepName = 'Qt多平台构建'
        }

        // 自动识别 net 类型：如果 sh 命令包含 dotnet，则识别为 net 类型
        if (stepType === 'sh' && cmd.includes('dotnet')) {
          stepType = 'net'
          stepName = 'Net编译'
        }

        // 自动识别 php 类型：如果 sh 命令以 composer 开头，则识别为 php 类型
        if (stepType === 'sh' && cmd.trim().startsWith('composer')) {
          stepType = 'php'
          stepName = 'PHP编译'
        }

        // 步骤名称中文映射
        const nameMap: Record<string, string> = {
          'git': '拉取代码',
          'checkout': '代码检出',
          'sh': 'Shell 命令',
          'bat': 'Windows Batch',
          'echo': '打印消息',
          'input': '人工审批',
          'sleep': '等待',
          'timeout': '超时控制',
          'retry': '重试',
          'junit': 'JUnit 测试',
          'archiveArtifacts': '归档制品',
          'cleanWs': '清理工作区',
          'build': '触发构建',
          'script': '脚本块',
          'apiTest': '接口测试',
          'scpDeploy': '远程传输文件部署',
          'node': 'Node编译',
          'cpp': 'C++多平台构建',
          'go': 'Go多平台构建',
          'qt': 'Qt多平台构建',
          'net': 'Net编译',
          'python': 'Python编译',
          'php': 'PHP编译',
          'pushImageDeploy': '推送镜像到制品库（部署）',
          'pushImageRelease': '推送镜像到制品库（发版）',
          'cloudDeploy': '云应用开发环境部署'
        }
        if (nameMap[stepType]) {
          stepName = nameMap[stepType]
        }

        return {
          id: uuidv4(),
          type: stepType,
          name: stepName,
          config
        }
      }

      // 将后端返回的分支格式转换为前端格式
      function convertBranchFromBackend(backendBranch: any, postEmailext?: any) {
        const rawSteps = backendBranch.steps || []
        const steps: PipelineStep[] = []
        let skipNextScript = false // 标志：是否跳过下一个 script 步骤

        for (let i = 0; i < rawSteps.length; i++) {
          const step = rawSteps[i]

          // 检查是否需要跳过当前的 script 步骤（docker rmi）
          if (skipNextScript && step.name === 'script') {
            skipNextScript = false
            continue
          }

          // 检测连续的 script 步骤：包含 docker build 的 + docker rmi
          if (step.name === 'script' && step.arguments) {
            const scriptBlock = step.arguments.find((a: any) => a.key === 'scriptBlock')?.value?.value || ''

            // 检查是否是 javaImage 构建步骤（包含 IMG_NAME 或 docker build）
            if (typeof scriptBlock === 'string' && scriptBlock.includes('IMG_NAME') && scriptBlock.includes('docker build')) {
              // 检查下一个 step 是否是清理脚本（docker rmi）
              const nextStep = rawSteps[i + 1]
              if (nextStep?.name === 'script') {
                const nextScriptArg = nextStep.arguments?.find((a: any) => a.key === 'scriptBlock')
                const nextScriptBlock = nextScriptArg?.value?.value || ''
                if (nextScriptBlock.includes('docker rmi')) {
                  // 合并为 javaImage 步骤
                  steps.push({
                    id: uuidv4(),
                    type: 'javaImage',
                    name: 'JAVA生成镜像(Arm)',
                    config: { scriptBlock }
                  })
                  skipNextScript = true // 标记下一个 script 步骤需要跳过
                  continue //防止执行到下面的普通步骤转换
                }
              }
            }

            // 检查是否是推送镜像到制品库步骤（包含 curl push 命令）
            const hasPushCommand = scriptBlock.includes('product/jenkinsClient/push') || (scriptBlock.includes('curl') && scriptBlock.includes('productFlag'))
            // 检查是否包含清理命令（可能在同一个 script 块内，也可能在下一个 step）
            const containsRmCommand = scriptBlock.includes('rm -f') ||
              (rawSteps[i + 1]?.name === 'script' && rawSteps[i + 1]?.arguments?.find((a: any) => a.key === 'scriptBlock')?.value?.value?.includes('rm -f'))

            if (hasPushCommand && containsRmCommand) {
              // 合并为推送镜像到制品库步骤
              const config: Record<string, any> = {}

              // 从 scriptBlock 解析推送相关变量
              const extractDef = (varName: string): string => {
                const match = scriptBlock.match(new RegExp(`def\\s+${varName}\\s*=\\s*"([^"]*)"`))
                return match ? match[1] : ''
              }
              const extractForm = (key: string): string => {
                const match = scriptBlock.match(new RegExp(`--form\\s+'${key}="([^"]*)"`))
                return match ? match[1] : ''
              }

              config.serviceAlias = extractDef('productFlag').replace('.toLowerCase()', '')
              config.jenkinsName = extractDef('jenkinsName')
              config.jenkinsNumber = extractDef('jenkinsNumber')
              config.productRepoUrl = extractForm('PRODUCT_REPO_URL') || extractForm('productRepoUrl')
              config.userToken = extractForm('USER_TOKEN') || extractForm('userToken')
              config.productRepoName = extractForm('productRepositoryName')
              config.productFlagName = extractForm('productFlagName')
              config.cpu = extractForm('cpu')
              config.memory = extractForm('memory')
              config.framework = extractForm('framework')
              config.exposePortList = extractForm('exposePortBoList')

              const productStatusMatch = scriptBlock.match(/productStatus="([^"]*)"/)
              if (productStatusMatch) {
                config.productStatus = productStatusMatch[1]
              }

              const safeCodeMatch = scriptBlock.match(/safeCode="([^"]*)"/)
              if (safeCodeMatch) {
                config.safeCode = parseInt(safeCodeMatch[1]) || 0
              }

              const filePathMatch = scriptBlock.match(/--form\s+'file=@([^'"]*)\.tar'/)
              if (filePathMatch) {
                config.filePath = filePathMatch[1]
              }

              const appBoMatch = scriptBlock.match(/uploadProductApplicationBo="(\\{.*\\})"/)
              if (appBoMatch) {
                try {
                  const appBoStr = appBoMatch[1].replace(/\\"/g, '"')
                  const appBo = JSON.parse(appBoStr)
                  config.appId = appBo.appId || ''
                  config.appCode = appBo.appCode || ''
                  config.appName = appBo.appName || ''
                  config.appVersionId = appBo.appVersionId || ''
                  config.appVersionName = appBo.appVersionName || ''
                  config.tripartiteName = appBo.tripartiteName || ''
                } catch (e) {
                  console.log('解析 uploadProductApplicationBo 失败', e)
                }
              }

              const isDeploy = config.productStatus === 'product_status_dev'
              steps.push({
                id: uuidv4(),
                type: isDeploy ? 'pushImageDeploy' : 'pushImageRelease',
                name: isDeploy ? '推送镜像到制品库（部署）' : '推送镜像到制品库（发版）',
                config
              })
              i = scriptBlock.includes('rm -f') ? 1 : 2
              continue
            }

            // 检查是否是云应用开发环境部署步骤（包含 auth/login 和 PIPELINE_STATUS）
            if (scriptBlock.includes('getAuthToken') && scriptBlock.includes('auth/login')) {
              // 收集所有连续的 cloudDeploy script 块
              let cloudDeployScripts = [scriptBlock]
              let j = i + 1
              while (j < rawSteps.length) {
                const nextStep = rawSteps[j]
                if (nextStep.name === 'script' && nextStep.arguments) {
                  const nextScriptBlock = nextStep.arguments.find((a: any) => a.key === 'scriptBlock')?.value?.value || ''
                  if (nextScriptBlock.includes('PIPELINE_STATUS=true')) {
                    cloudDeployScripts.push(nextScriptBlock)
                    j++
                    break
                  }
                  cloudDeployScripts.push(nextScriptBlock)
                  j++
                } else {
                  break
                }
              }

              // 解析配置
              const config: Record<string, any> = {}
              const allScriptContent = cloudDeployScripts.join('\n')

              // 解析 username/password
              const credsMatch = allScriptContent.match(/--data-raw\s+'\{[^{]*"username"\s*:\s*"([^"]*)"\s*,\s*"password"\s*:\s*"([^"]*)"\}/)
              if (credsMatch) {
                config.username = credsMatch[1]
                config.password = credsMatch[2]
              }

              // 解析 appManageUrl
              const appManageUrlMatch = allScriptContent.match(/curl\s+\$\{APP_MANAGE_URL\}/)
              if (appManageUrlMatch) {
                config.appManageUrl = '${APP_MANAGE_URL}'
              }

              // 解析 namespace (从 recycle 或 envBinding)
              const namespaceMatch = allScriptContent.match(/envs\/([^/]+)\/recycle|envBinding.*?"name"\s*:\s*"([^"]+)"/)
              if (namespaceMatch) {
                config.namespace = namespaceMatch[1] || namespaceMatch[2]
              }

              // 解析 deployAppName
              const deployAppNameMatch = allScriptContent.match(/applications\/\$\{env\.DEPLOY_APP_NAME\}/)
              if (deployAppNameMatch) {
                config.deployAppName = '${DEPLOY_APP_NAME}'
              }

              // 解析 productFlagName
              const productFlagMatch = allScriptContent.match(/"alias"\s*:\s*"([^"]+)"/)
              if (productFlagMatch) {
                config.productFlagName = productFlagMatch[1]
              }

              // 解析 exposePortList
              const exposePortMatch = allScriptContent.match(/portList\s*=\s*new groovy\.json\.JsonSlurper\(\)\.parseText\("([^"]+)"\)/)
              if (exposePortMatch) {
                config.exposePortList = exposePortMatch[1]
              }

              // 解析 cpu/memory
              const cpuMatch = allScriptContent.match(/"cpu"\s*:\s*"([^"]+)"/)
              if (cpuMatch) {
                config.cpuNum = cpuMatch[1]
              }
              const memMatch = allScriptContent.match(/"memory"\s*:\s*"([^"]+)M"/)
              if (memMatch) {
                config.memory = memMatch[1]
              }

              steps.push({
                id: uuidv4(),
                type: 'cloudDeploy',
                name: '云应用开发环境部署',
                config
              })
              i = j - 1
              continue
            }

            // 检查是否是 sonar 代码扫描步骤（包含 SONAR_PROJECT_NAME 或 SONAR_PROJECT_KEY）
            if (scriptBlock.includes('SONAR_PROJECT_NAME') || scriptBlock.includes('SONAR_PROJECT_KEY')) {
              const config: Record<string, any> = {}

              // 从 scriptBlock 解析 Sonar 相关变量
              const sonarVarMap: Record<string, string> = {
                'SONAR_PROJECT_NAME': 'projectName',
                'SONAR_PROJECT_KEY': 'projectKey',
                'SONAR_CODE_SOURCES_PATH': 'sources',
                'SONAR_JAVA_BINARIES_PATH': 'binaryPath',
                'SONAR_PATH': 'cliPath',
                'SONAR_HOST_URL': 'serverUrl',
                'SONAR_LOGIN_TOKEN': 'token'
              }
              for (const [varName, configKey] of Object.entries(sonarVarMap)) {
                const match = scriptBlock.match(new RegExp(`def\\s+${varName}\\s*=\\s*['"]([^'"]*)['"]`))
                if (match) {
                  config[configKey] = match[1]
                }
              }

              steps.push({
                id: uuidv4(),
                type: 'sonar',
                name: '代码扫描',
                config
              })
              continue
            }

            // 检查是否是 apiTest 接口测试步骤（包含 POSTMAN_COLLECTION）
            if (scriptBlock.includes('POSTMAN_COLLECTION')) {
              const config: Record<string, any> = {}

              // 从 scriptBlock 解析 Postman 相关变量
              const postmanVarMap: Record<string, string> = {
                'POSTMAN_COLLECTION': 'collectionPath',
                'POSTMAN_ENV': 'environment',
                'TEST_REPORT': 'reportName'
              }
              for (const [varName, configKey] of Object.entries(postmanVarMap)) {
                const match = scriptBlock.match(new RegExp(`def\\s+${varName}\\s*=\s*['"]([^'"]*)['"]`))
                if (match) {
                  config[configKey] = match[1]
                }
              }

              steps.push({
                id: uuidv4(),
                type: 'apiTest',
                name: '接口测试',
                config
              })

              // 检查下一个 step 是否是 sh (newman)，如果是则跳过
              const nextStep = rawSteps[i + 1]
              if (nextStep?.name === 'sh') {
                i += 1 // 跳过 newman sh步骤
              }
              continue
            }

            // 检查是否是 mail 步骤（包含 PROJECT_NAME 等变量定义）+ echo
            if (scriptBlock.includes('PROJECT_NAME') && !scriptBlock.includes('docker')) {
              const nextStep = rawSteps[i + 1]
              if (nextStep?.name === 'echo') {
                // 合并为 mail 步骤
                const config: Record<string, any> = {}

                // 从 scriptBlock 解析变量
                const varMap: Record<string, string> = {
                  'PROJECT_NAME': 'projectName',
                  'USER_EMAILS': 'to',
                  'FROM_USER': 'from',
                  'CHANGES': 'changes',
                  'BUILD_NUMBER': 'number',
                  'BUILD_STATUS': 'status',
                  'BUILD_URL': 'url'
                }
                for (const [varName, configKey] of Object.entries(varMap)) {
                  const match = scriptBlock.match(new RegExp(`def\\s+${varName}\\s*=\\s*['"]([^'"]*)['"]`))
                  if (match) {
                    config[configKey] = match[1]
                  }
                }

                // 从 emailext 解析 body（如果 postEmailext 存在）
                if (postEmailext) {
                  const bodyArg = postEmailext.arguments?.find((a: any) => a.key === 'body')
                  if (bodyArg?.value?.value) {
                    config.body = bodyArg.value.value
                  }
                }

                steps.push({
                  id: uuidv4(),
                  type: 'mail',
                  name: '发送邮件',
                  config
                })
                i = i + 1 // 设置为要跳过的索引(for循环会自动+1)，而不是 i+=2(会跳过更多)
                continue // 防止执行到下面的普通步骤转换
              }
            }
          }

          // 普通步骤转换
          steps.push(convertStepFromBackend(step))
        }

        return {
          id: uuidv4(),
          name: backendBranch.name || 'default',
          steps
        }
      }

      // 将后端返回的阶段格式转换为前端格式
      function convertStageFromBackend(backendStage: any) {
        // 提取 post.conditions 中的 emailext 步骤（用于 mail 合并）
        let postEmailext: any = null
        if (backendStage.post?.conditions) {
          for (const cond of backendStage.post.conditions) {
            if (cond.branch?.steps) {
              for (const step of cond.branch.steps) {
                if (step.name === 'emailext') {
                  postEmailext = step
                  break
                }
              }
            }
          }
        }

        const branches = (backendStage.branches || []).map((branch: any) => convertBranchFromBackend(branch, postEmailext))

        // 处理 input 审批步骤：如果 stage.input 存在，添加一个 input 类型的步骤
        if (backendStage.input) {
          const inputConfig: Record<string, any> = {}
          if (backendStage.input.message) {
            inputConfig.message = backendStage.input.message.value || backendStage.input.message
          }
          if (backendStage.input.submitter) {
            inputConfig.approver = backendStage.input.submitter.value || backendStage.input.submitter
          }

          // 在第一个分支的 steps开头添加 input 步骤
          if (branches.length > 0 && branches[0].steps.length > 0) {
            branches[0].steps.unshift({
              id: uuidv4(),
              type: 'input',
              name: '审批卡点',
              config: inputConfig
            })
          }

          // 移除紧随其后的 echo "审核成功，继续执行" 步骤（这是 input 审批通过后的自动输出，不需要单独显示）
          if (branches.length > 0) {
            branches[0].steps = branches[0].steps.filter((step: PipelineStep) => {
              if (step.type === 'echo' && step.name === '打印消息') {
                const msg = step.config?.message || ''
                // 检查是否是 "审核成功，继续执行" 或类似的审批通过提示
                if (msg.includes('审核成功') || msg === '1') {
                  return false
                }
              }
              return true
            })
          }
        }

        // 处理 scpDeploy 远程传输文件部署步骤
        // 如果 stage.environment 中定义了 REMOTE_IP、REMOTE_COMMAND、transferFile，则合并 sh + sshPublisher 为一个步骤
        const envArray = backendStage.environment || []
        const envMap: Record<string, string> = {}
        for (const env of envArray) {
          if (env.key && env.value) {
            envMap[env.key] = env.value.isLiteral ? env.value.value : env.value
          }
        }

        if (envMap.REMOTE_IP && envMap.REMOTE_COMMAND && envMap.transferFile) {
          // 在第一个分支中查找 sh + sshPublisher 组合并合并
          // 注意：sh 和 sshPublisher 之间可能有其他步骤（如 mail），所以需要向后查找 sshPublisher
          if (branches.length > 0) {
            const branch = branches[0]
            const newSteps: PipelineStep[] = []
            let i = 0
            while (i < branch.steps.length) {
              const step = branch.steps[i]
              // 检测 sh步骤后面是否有 sshPublisher
              if (step.type === 'sh') {
                let sshPublisherIndex = -1
                for (let j = i + 1; j < branch.steps.length; j++) {
                  if (branch.steps[j].name === 'sshPublisher') {
                    sshPublisherIndex = j
                    break
                  }
                }
                if (sshPublisherIndex !== -1) {
                  // 合并为 scpDeploy 步骤
                  newSteps.push({
                    id: uuidv4(),
                    type: 'scpDeploy',
                    name: '远程传输文件部署',
                    config: {
                      nodeName: envMap.REMOTE_IP,
                      remoteCommand: envMap.REMOTE_COMMAND,
                      transferPath: envMap.transferFile
                    }
                  })
                  i = sshPublisherIndex + 1 // 跳过 sh 和 sshPublisher 及其之间的步骤
                  continue
                }
              }
              newSteps.push(step)
              i++
            }
            branches[0].steps = newSteps
          }
        }

        return {
          id: uuidv4(),
          name: backendStage.name,
          type: backendStage.type || 'sequential',
          branches
        }
      }

      if (contentData?.json) {
        // 解析 json 字符串为对象
        const pipelineData = JSON.parse(contentData.json)

        // 更新当前流水线数据（注意：id类型可能不一致，需要转换后比较）
        const idToFind = String(id)
        const index = pipelines.value.findIndex(p => String(p.id) === idToFind)
        if (index !== -1) {
          const rawPipeline = pipelineData.pipeline || pipelineData
          let convertedStages = (rawPipeline.stages || []).map(convertStageFromBackend)

          // 合并子流水线 stage：如果一个 stage 只有 build 步骤，将其合并到前一个 stage
          const mergedStages: typeof convertedStages = []
          for (let i = 0; i < convertedStages.length; i++) {
            const stage = convertedStages[i]
            const hasOnlyBuild = stage.branches.length === 1 &&
              stage.branches[0].steps.length === 1 &&
              stage.branches[0].steps[0].type === 'build'

            if (hasOnlyBuild && mergedStages.length > 0) {
              // 将 build 步骤合并到前一个 stage
              const prevStage = mergedStages[mergedStages.length - 1]
              const buildStep = stage.branches[0].steps[0]
              // 更新 build 步骤的名称为 stage 名称
              buildStep.name = stage.name
              prevStage.branches[0].steps.push(buildStep)
            } else {
              mergedStages.push(stage)
            }
          }

          pipelines.value[index] = {
            ...pipelines.value[index],
            agent: rawPipeline.agent || { type: 'any' },
            stages: mergedStages,
            post: rawPipeline.post || {}
          }
        }

        // 存储 jenkinsfile 和 yaml 供预览
        const targetPipeline = pipelines.value.find(p => String(p.id) === idToFind)
        if (targetPipeline) {
          const ext = targetPipeline as any
          ext.jenkinsfileContent = contentData.jenkinsfile || ''
          ext.yamlContent = contentData.yaml || ''
        }
      }

      // 流水线内容获取完成后，获取执行记录
      fetchRuns(id)
    } catch (error) {
      console.error('获取流水线内容失败:', error)
    }
  }

  fetchPipelines()
  // Getters
  const currentPipeline = computed(() =>
    pipelines.value.find(p => String(p.id) === String(currentPipelineId.value)) || null
  )

  const jenkinsfile = computed(() => {
    if (!currentPipeline.value) return ''
    return pipelineToJenkinsfile(currentPipeline.value)
  })

  const pipelineRuns = computed(() => {
    console.log('pipelineRuns 计算中:', { currentPipelineId: currentPipelineId.value, runsLength: runs.value.length })
    const filtered = runs.value.filter(r => String(r.pipelineId) === String(currentPipelineId.value))
      .sort((a, b) => b.buildNumber - a.buildNumber)
    console.log('过滤后的 runs:', filtered.length)
    return filtered
  })

  // 自动刷新状态
  const autoRefreshIntervalId = ref<number | null>(null)
  const autoRefreshCount = ref(0)
  const MAX_REFRESH_COUNT = 60

  // 启动自动刷新
  function startAutoRefresh(pipelineId: string) {
    // 清除之前的定时器
    stopAutoRefresh()

    // 设置定时器，每 2 秒刷新一次
    autoRefreshIntervalId.value = window.setInterval(() => {
      autoRefreshCount.value++
      console.log(`自动刷新 #${autoRefreshCount.value}`)

      // 刷新执行记录
      fetchRuns(pipelineId)

      // 检查是否所有 runs 都已完成（不是 running 状态）
      const pipelineRunsList = runs.value.filter(r => String(r.pipelineId) === String(pipelineId))
      const allCompleted = pipelineRunsList.every(r => r.status !== 'running')

      console.log('当前 runs状态:', pipelineRunsList.map(r => ({ buildNumber: r.buildNumber, status: r.status })))

      // 停止条件：全部完成 或 超过最大次数
      if (allCompleted || autoRefreshCount.value >= MAX_REFRESH_COUNT) {
        console.log('停止自动刷新:', { allCompleted, autoRefreshCount: autoRefreshCount.value })
        stopAutoRefresh()
      }
    }, 2000)
  }

  // 停止自动刷新
  function stopAutoRefresh() {
    if (autoRefreshIntervalId.value !== null) {
      clearInterval(autoRefreshIntervalId.value)
      autoRefreshIntervalId.value = null
    }
    autoRefreshCount.value = 0
  }

  function setCurrentPipeline(id: string) {
    currentPipelineId.value = id
    // 只有已保存到后端的流水线（有jenkinsfileContent）才获取内容
    const pipeline = pipelines.value.find(p => p.id === id)
    if (pipeline?.jenkinsfileContent) {
      fetchPipelineContent(id)
      fetchRuns(id)
    }
  }

  function createPipeline(pipeline: Partial<Pipeline>) {
    const newPipeline: Pipeline = {
      id: uuidv4(),
      name: pipeline.name || '新流水线',
      description: pipeline.description || '',
      agent: pipeline.agent || { type: 'any' },
      environment: pipeline.environment || [],
      stages: pipeline.stages || [],
      post: pipeline.post || {},
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    pipelines.value.push(newPipeline)
    currentPipelineId.value = newPipeline.id
    return newPipeline
  }

  function updatePipeline(id: string, updates: Partial<Pipeline>) {
    console.log(updates)
    const index = pipelines.value.findIndex(p => p.id === id)
    if (index !== -1) {
      pipelines.value[index] = {
        ...pipelines.value[index],
        ...updates,
        updatedAt: new Date().toISOString()
      }
    }
  }

  // 保存流水线到后端
  async function savePipeline(jobName: string, jenkinsfile: string) {
    try {
      const res = await axios.post(`/api/pipeline/pipeline?jobName=${encodeURIComponent(jobName)}`, jenkinsfile, {
        headers: {
          'Content-Type': 'text/plain'
        }
      })
      console.log('保存流水线成功:', res.data)
      return res.data
    } catch (error) {
      console.error('保存流水线失败:', error)
      throw error
    }
  }

  async function deletePipeline(id: string) {
    pipelines.value = pipelines.value.filter(p => p.id !== id)
    if (currentPipelineId.value === id && pipelines.value.length > 0) {
      currentPipelineId.value = pipelines.value[0].id
    }
    await axios.delete(`api/pipeline/delete?pipeLineId=${id}`,{
        headers: {
          'Content-Type': 'text/plain'
        }
      })
  }

  function addStage(stage: PipelineStage) {
    if (!currentPipeline.value) return
    currentPipeline.value.stages.push(stage)
    console.log(stage)
    currentPipeline.value.updatedAt = new Date().toISOString()
  }

  function updateStage(stageId: string, updates: Partial<PipelineStage>) {
    console.log(stageId)
    console.log(updates)
    if (!currentPipeline.value) return
    const index = currentPipeline.value.stages.findIndex(s => s.id === stageId)
    if (index !== -1) {
      currentPipeline.value.stages[index] = {
        ...currentPipeline.value.stages[index],
        ...updates
      }
      currentPipeline.value.updatedAt = new Date().toISOString()
    }
  }

  function removeStage(stageId: string) {
    if (!currentPipeline.value || !stageId) return
    currentPipeline.value.stages = currentPipeline.value.stages.filter(s => s.id !== stageId)
    currentPipeline.value.updatedAt = new Date().toISOString()
  }

  function removeStep(stageId: string, branchId: string, stepId: string) {
    if (!currentPipeline.value) return
    const stage = currentPipeline.value.stages.find(s => s.id === stageId)
    if (!stage) return
    const branch = stage.branches.find(b => b.id === branchId)
    if (!branch) return
    branch.steps = branch.steps.filter(s => s.id !== stepId)
    currentPipeline.value.updatedAt = new Date().toISOString()
  }

  function moveStage(stageId: string, direction: 'up' | 'down') {
    if (!currentPipeline.value) return
    const stages = currentPipeline.value.stages
    const index = stages.findIndex(s => s.id === stageId)
    if (index === -1) return

    if (direction === 'up' && index > 0) {
      [stages[index], stages[index - 1]] = [stages[index - 1], stages[index]]
    } else if (direction === 'down' && index < stages.length - 1) {
      [stages[index], stages[index + 1]] = [stages[index + 1], stages[index]]
    }
    currentPipeline.value.updatedAt = new Date().toISOString()
  }

  // 模拟运行流水线
  async function runPipeline() {
    if (!currentPipeline.value || isRunning.value) return

    isRunning.value = true
    const pipeline = currentPipeline.value
    const run: PipelineRun = {
      id: uuidv4(),
      pipelineId: pipeline.id,
      buildNumber: runs.value.filter(r => r.pipelineId === pipeline.id).length + 1,
      status: 'running',
      startTime: new Date().toISOString(),
      triggeredBy: 'admin',
      branch: 'main',
      stages: pipeline.stages.map(s => ({
        stageId: s.id,
        status: 'pending' as StageStatus,
        logs: []
      }))
    }
    console.log(run)
    runs.value.push(run)
    currentRunId.value = run.id

    // 模拟执行
    for (let i = 0; i < pipeline.stages.length; i++) {
      const stage = pipeline.stages[i]
      const runStage = run.stages[i]

      runStage.status = 'running'
      runStage.startTime = new Date().toISOString()

      // 模拟执行时间
      await new Promise(resolve => setTimeout(resolve, 1500 + Math.random() * 2000))

      // 随机成功或失败（90%成功率）
      const isSuccess = Math.random() > 0.1
      runStage.status = isSuccess ? 'success' : 'failed'
      runStage.endTime = new Date().toISOString()

      // 生成日志
      const branch = stage.branches[0]
      if (branch) {
        for (const step of branch.steps) {
          runStage.logs.push(...generateMockLogs(stage.name, step.name, runStage.status))
        }
      }

      // 如果失败，后续 stage 标记为 skipped
      if (!isSuccess) {
        run.status = 'failed'
        for (let j = i + 1; j < run.stages.length; j++) {
          run.stages[j].status = 'skipped'
        }
        break
      }
    }

    if (run.status === 'running') {
      run.status = 'success'
    }

    run.endTime = new Date().toISOString()
    run.duration = new Date(run.endTime).getTime() - new Date(run.startTime).getTime()

    isRunning.value = false
    currentRunId.value = null

    return run
  }

  function getRunById(runId: string) {
    return runs.value.find(r => r.id === runId) || null
  }

  // 从 Jenkins API 获取构建日志记录
  async function fetchRuns(pipeLineId: string) {
    try {
      console.log('fetchRuns 调用:', pipeLineId)
      const res = await axios.get('/api/pipeline/builds', {
        params: {
          pipeLineId: pipeLineId,
        }
      })
      console.log('builds API 响应:', res.data)
      const builds = Array.isArray(res.data) ? res.data : ((res.data as any)?.data || [])
      

      // 将 Jenkins 构建记录转换为 PipelineRun 格式
      const newRuns: PipelineRun[] = builds.map((build: any) => ({
        id: `${pipeLineId}-${build.number}`,
        pipelineId: String(pipeLineId),
        buildNumber: build.number,
        status: mapJenkinsStatus(build.result),
        startTime: build.timestamp,
        endTime: build.timestamp,
        duration: parseDuration(build.duration),
        triggeredBy: build.builder || 'unknown',
        branch: build.pipeline?.split('-').slice(-1)[0] || 'main',
        stages: []
      }))

      // 更新 runs 数组
      runs.value = newRuns
      return newRuns
    } catch (error) {
      console.error('获取构建记录失败:', error)
      return []
    }
  }

  // 从 Jenkins API 获取构建阶段详情
  async function fetchStages(jobName: string, buildNumber: number) {
    try {
      const res = await axios.get('/api/pipeline/stages', {
        params: {
          pipeLineId:jobName,
          buildNumber:buildNumber,
          t: Date.now()
        }
      })
      return (res.data as any).data || {}
    } catch (error) {
      console.error('获取构建阶段失败:', error)
      return {}
    }
  }

  // 将 Jenkins 状态映射为 Pipeline 状态
  function mapJenkinsStatus(result: string): StageStatus {
    const map: Record<string, StageStatus> = {
      'SUCCESS': 'success',
      'FAILURE': 'failed',
      'UNSTABLE': 'unstable',
      'ABORTED': 'aborted',
      'BUILDING': 'running',
      'RETRY': 'running'
    }
    return map[result] || 'pending'
  }

  // 解析持续时间字符串 (如 "5m 30s") 为毫秒
  function parseDuration(duration: string): number {
    if (!duration) return 0
    const match = duration.match(/(\d+)m\s*(\d+)s/)
    if (match) {
      return (parseInt(match[1]) * 60 + parseInt(match[2])) * 1000
    }
    // 尝试直接解析为数字（秒）
    const num = parseInt(duration)
    return isNaN(num) ? 0 : num * 1000
  }

  return {
    pipelines,
    currentPipelineId,
    currentPipeline,
    jenkinsfile,
    runs,
    isRunning,
    currentRunId,
    pipelineRuns,
    setCurrentPipeline,
    createPipeline,
    updatePipeline,
    deletePipeline,
    addStage,
    updateStage,
    removeStage,
    removeStep,
    moveStage,
    runPipeline,
    savePipeline,
    getRunById,
    fetchPipelines,
    fetchPipelineContent,
    fetchRuns,
    fetchStages,
    startAutoRefresh,
    stopAutoRefresh
  }
})
