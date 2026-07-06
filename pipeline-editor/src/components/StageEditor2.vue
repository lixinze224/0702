<template>
  <div class="stage-editor">
    <div class="editor-header">
      <h3>{{ isEdit ? '编辑 Stage' : '添加 Stage' }}</h3>
      <el-button type="text" @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <el-form :model="form" label-width="100px" class="editor-form">
      <el-form-item label="Stage 名称">
        <el-input v-model="form.name" placeholder="例如：构建、测试、部署" />
      </el-form-item>

      <el-form-item label="执行类型">
        <el-radio-group v-model="form.type">
          <el-radio-button label="sequential">串行</el-radio-button>
          <el-radio-button label="parallel">并行</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="执行条件">
        <el-switch v-model="hasWhen" active-text="启用条件" />
      </el-form-item>

      <template v-if="hasWhen && form.when">
        <el-form-item label="分支匹配">
          <el-input v-model="form.when!.branch" placeholder="例如：main, release/*" />
        </el-form-item>
        <el-form-item label="表达式">
          <el-input v-model="form.when!.expression" placeholder="例如：env.DEPLOY == 'true'" />
        </el-form-item>
      </template>

      <!-- 分支列表 -->
      <div class="branches-section">
        <div class="section-header">
          <span class="section-title">{{ form.type === 'parallel' ? '并行分支' : '执行步骤' }}</span>
          <el-button type="primary" size="small" @click="addBranch">
            <el-icon><Plus /></el-icon> {{ form.type === 'parallel' ? '添加分支' : '添加步骤组' }}
          </el-button>
        </div>

        <div v-for="(branch, branchIndex) in form.branches" :key="branch.id" class="branch-card">
          <div class="branch-header">
            <el-input v-model="branch.name" placeholder="分支/步骤组名称" size="small" style="width: 200px" />
            <el-button type="danger" size="small" text @click="removeBranch(branchIndex)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>

          <!-- 步骤列表 -->
          <div class="steps-list">
            <div v-for="(step, stepIndex) in branch.steps" :key="step.id" class="step-item">
              <div class="step-info">
                <el-tag size="small" :type="getStepTypeColor(step.type)">{{ step.type }}</el-tag>
                <span class="step-name">{{ step.name }}</span>
              </div>
              <div class="step-actions">
                <el-button type="primary" size="small" text @click="editStep(branchIndex, stepIndex)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button type="danger" size="small" text @click="removeStep(branchIndex, stepIndex)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <el-button type="primary" size="small" text @click="openStepDialog(branchIndex)">
              <el-icon><Plus /></el-icon> 添加步骤
            </el-button>
          </div>
        </div>
      </div>
    </el-form>

    <div class="editor-footer">
      <el-button @click="$emit('close')">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>

    <!-- 步骤编辑对话框 -->
    <el-dialog v-model="stepDialogVisible" :title="isEditStep ? '编辑步骤' : '添加步骤'" width="500px">
      <el-form :model="stepForm" label-width="100px">
        <el-form-item label="步骤类型">
          <el-select v-model="stepForm.type" placeholder="选择步骤类型" style="width: 100%">
            <el-option v-for="type in stepTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="步骤名称">
          <el-input v-model="stepForm.name" placeholder="步骤显示名称" />
        </el-form-item>

        <!-- 根据类型显示不同配置 -->
        <template v-if="stepForm.type === 'sh'">
          <el-form-item label="Shell 命令">
            <el-input v-model="stepForm.config.command" type="textarea" :rows="3" placeholder="输入 Shell 命令" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'echo'">
          <el-form-item label="打印内容">
            <el-input v-model="stepForm.config.message" placeholder="要打印的日志内容" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'git'">
          <el-form-item label="仓库地址">
            <el-input v-model="stepForm.config.url" placeholder="Git 仓库 URL" />
          </el-form-item>
          <el-form-item label="分支">
            <el-input v-model="stepForm.config.branch" placeholder="默认 main" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'input'">
          <el-form-item label="提示信息">
            <el-input v-model="stepForm.config.message" placeholder="等待用户输入的提示信息" />
          </el-form-item>
          <el-form-item label="审批人">
            <el-input v-model="stepForm.config.approver" placeholder="允许提交的用户，多个用逗号分隔" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'sleep'">
          <el-form-item label="等待时间(秒)">
            <el-input-number v-model="stepForm.config.time" :min="1" :max="3600" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'timeout'">
          <el-form-item label="超时时间">
            <el-input-number v-model="stepForm.config.time" :min="1" :max="1440" />
          </el-form-item>
          <el-form-item label="时间单位">
            <el-select v-model="stepForm.config.unit">
              <el-option label="秒" value="SECONDS" />
              <el-option label="分钟" value="MINUTES" />
              <el-option label="小时" value="HOURS" />
            </el-select>
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'retry'">
          <el-form-item label="重试次数">
            <el-input-number v-model="stepForm.config.count" :min="1" :max="10" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'archiveArtifacts'">
          <el-form-item label="文件路径">
            <el-input v-model="stepForm.config.artifacts" placeholder="例如：dist/**, *.jar" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'junit'">
          <el-form-item label="测试结果路径">
            <el-input v-model="stepForm.config.testResults" placeholder="例如：reports/**/*.xml" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'cleanWs'">
          <el-alert type="info" :closable="false">此步骤将清理当前工作区</el-alert>
        </template>

        <template v-if="stepForm.type === 'custom'">
          <el-form-item label="自定义代码">
            <el-input v-model="stepForm.config.code" type="textarea" :rows="5" placeholder="输入自定义 Jenkins Pipeline 代码" />
          </el-form-item>
        </template>

        <!-- 脚本类 -->
        <template v-if="stepForm.type === 'bat'">
          <el-form-item label="Batch 命令">
            <el-input v-model="stepForm.config.command" type="textarea" :rows="3" placeholder="Windows Batch 命令" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'powershell'">
          <el-form-item label="PowerShell 命令">
            <el-input v-model="stepForm.config.command" type="textarea" :rows="3" placeholder="PowerShell 命令" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'python'">
          <el-form-item label="Python 脚本">
            <el-input v-model="stepForm.config.script" type="textarea" :rows="3" placeholder="Python 脚本内容" />
          </el-form-item>
        </template>

        <!-- Docker -->
        <template v-if="stepForm.type === 'dockerBuildAndPush'">
          <el-form-item label="镜像名称">
            <el-input v-model="stepForm.config.image" placeholder="例如：myregistry/myapp:tag" />
          </el-form-item>
          <el-form-item label="构建上下文">
            <el-input v-model="stepForm.config.context" placeholder="构建上下文路径，默认 ." />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'dockerPull'">
          <el-form-item label="镜像名称">
            <el-input v-model="stepForm.config.image" placeholder="例如：nginx:latest" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'dockerRun'">
          <el-form-item label="镜像名称">
            <el-input v-model="stepForm.config.image" placeholder="例如：nginx:latest" />
          </el-form-item>
          <el-form-item label="运行命令">
            <el-input v-model="stepForm.config.command" placeholder="容器启动命令" />
          </el-form-item>
        </template>

        <!-- Kubernetes -->
        <template v-if="stepForm.type === 'kubectl'">
          <el-form-item label="Kubectl 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：apply -f deployment.yaml" />
          </el-form-item>
        </template>

        <!-- Maven/Gradle/NPM/Ant -->
        <template v-if="stepForm.type === 'maven'">
          <el-form-item label="Maven 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：clean package" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'jar'">
          <el-form-item label="Maven 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：clean package" />
          </el-form-item>
         <el-form-item label="超时时间(分钟)">
            <el-input-number v-model="stepForm.config.timeout" :min="1" :max="120" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'gradle'">
          <el-form-item label="Gradle 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：build" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'npm'">
          <el-form-item label="NPM 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：install 或 run build" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'ant'">
          <el-form-item label="Ant 目标">
            <el-input v-model="stepForm.config.target" placeholder="例如：build" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'node'">
          <el-form-item label="NPM 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：install 或 run build" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'cpp'">
          <el-form-item label="C++ 构建命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：cmake .. && make" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'go'">
          <el-form-item label="Go 构建命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：go build 或 go run" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'qt'">
          <el-form-item label="Qt 构建命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：qmake && make" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'net'">
          <el-form-item label="dotnet 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：dotnet build 或 dotnet run" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'php'">
          <el-form-item label="Composer 命令">
            <el-input v-model="stepForm.config.command" placeholder="例如：composer install 或 composer update" />
          </el-form-item>
        </template>

        <!-- SVN -->
        <template v-if="stepForm.type === 'svn'">
          <el-form-item label="SVN 仓库地址">
            <el-input v-model="stepForm.config.url" placeholder="SVN 仓库 URL" />
          </el-form-item>
        </template>

        <!-- 测试相关 -->
        <template v-if="stepForm.type === 'coverage'">
          <el-form-item label="覆盖率报告路径">
            <el-input v-model="stepForm.config.reportPath" placeholder="例如：coverage/**/coverage.xml" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'findbugs'">
          <el-form-item label="匹配模式">
            <el-input v-model="stepForm.config.pattern" placeholder="例如：**/findbugs.xml" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'checkstyle'">
          <el-form-item label="匹配模式">
            <el-input v-model="stepForm.config.pattern" placeholder="例如：**/checkstyle-result.xml" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'sonar'">
          <el-form-item label="项目名称">
            <el-input v-model="stepForm.config.projectName" placeholder="Sonar 项目名称" />
          </el-form-item>
          <el-form-item label="项目Key">
            <el-input v-model="stepForm.config.projectKey" placeholder="Sonar 项目Key" />
          </el-form-item>
          <el-form-item label="源码路径">
            <el-input v-model="stepForm.config.sources" placeholder="源码路径" />
          </el-form-item>
          <el-form-item label="字节码路径">
            <el-input v-model="stepForm.config.binaryPath" placeholder="Java 字节码路径" />
          </el-form-item>
          <el-form-item label="Sonar 路径">
            <el-input v-model="stepForm.config.cliPath" placeholder="sonar-scanner 路径" />
          </el-form-item>
          <el-form-item label="服务器地址">
            <el-input v-model="stepForm.config.serverUrl" placeholder="SonarQube 服务器地址" />
          </el-form-item>
          <el-form-item label="登录令牌">
            <el-input v-model="stepForm.config.token" placeholder="SonarQube 登录令牌" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'pytest'">
          <el-form-item label="PyTest 选项">
            <el-input v-model="stepForm.config.options" placeholder="例如：-v --cov=src" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'nose'">
          <el-form-item label="Nose 选项">
            <el-input v-model="stepForm.config.options" placeholder="额外的 nosetests 选项" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'apiTest'">
          <el-form-item label="集合路径">
            <el-input v-model="stepForm.config.collectionPath" placeholder="Postman 集合 JSON 文件路径" />
          </el-form-item>
          <el-form-item label="环境">
            <el-input v-model="stepForm.config.environment" placeholder="Postman 环境文件路径" />
          </el-form-item>
          <el-form-item label="报告名称">
            <el-input v-model="stepForm.config.reportName" placeholder="测试报告文件名" />
          </el-form-item>
        </template>

        <!-- 部署相关 -->
        <template v-if="stepForm.type === 'deploy'">
          <el-form-item label="适配器">
            <el-input v-model="stepForm.config.adapter" placeholder="例如：tomcat9" />
          </el-form-item>
          <el-form-item label="WAR 文件路径">
            <el-input v-model="stepForm.config.warFile" placeholder="例如：target/*.war" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'scpDeploy'">
          <el-form-item label="远程IP">
            <el-input v-model="stepForm.config.nodeName" placeholder="远程主机 IP" />
          </el-form-item>
          <el-form-item label="传输文件">
            <el-input v-model="stepForm.config.transferPath" placeholder="要传输的文件路径" />
          </el-form-item>
          <el-form-item label="远程命令">
            <el-input v-model="stepForm.config.remoteCommand" placeholder="传输后执行的命令" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'sshCommand'">
          <el-form-item label="远程配置">
            <el-input v-model="stepForm.config.remote" type="textarea" :rows="2" placeholder="远程主机配置 JSON" />
          </el-form-item>
          <el-form-item label="命令">
            <el-input v-model="stepForm.config.command" placeholder="要执行的远程命令" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'sshScript'">
          <el-form-item label="远程配置">
            <el-input v-model="stepForm.config.remote" type="textarea" :rows="2" placeholder="远程主机配置 JSON" />
          </el-form-item>
          <el-form-item label="脚本路径">
            <el-input v-model="stepForm.config.script" placeholder="远程服务器上的脚本路径" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'ansiblePlaybook'">
          <el-form-item label="Playbook 路径">
            <el-input v-model="stepForm.config.playbook" placeholder="例如：deploy.yml" />
          </el-form-item>
          <el-form-item label="Inventory">
            <el-input v-model="stepForm.config.inventory" placeholder="例如：hosts" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'ansibleAdHoc'">
          <el-form-item label="远程配置">
            <el-input v-model="stepForm.config.remote" type="textarea" :rows="2" placeholder="远程主机配置" />
          </el-form-item>
          <el-form-item label="模块">
            <el-input v-model="stepForm.config.module" placeholder="例如：shell, copy, command" />
          </el-form-item>
          <el-form-item label="参数">
            <el-input v-model="stepForm.config.args" placeholder="模块参数" />
          </el-form-item>
        </template>

        <!-- S3 -->
        <template v-if="stepForm.type === 'archiveAWS S3'">
          <el-form-item label="S3 桶名">
            <el-input v-model="stepForm.config.bucket" placeholder="例如：my-bucket" />
          </el-form-item>
          <el-form-item label="文件路径">
            <el-input v-model="stepForm.config.file" placeholder="例如：build/*.jar" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'copyArtifacts'">
          <el-form-item label="项目名称">
            <el-input v-model="stepForm.config.project" placeholder="源项目名称" />
          </el-form-item>
          <el-form-item label="选择器">
            <el-input v-model="stepForm.config.selector" placeholder="例如：lastSuccessful()" />
          </el-form-item>
          <el-form-item label="目标目录">
            <el-input v-model="stepForm.config.target" placeholder="目标目录路径" />
          </el-form-item>
        </template>

        <!-- 文件操作 -->
        <template v-if="stepForm.type === 'writeFile'">
          <el-form-item label="文件路径">
            <el-input v-model="stepForm.config.file" placeholder="要写入的文件路径" />
          </el-form-item>
          <el-form-item label="文件内容">
            <el-input v-model="stepForm.config.text" type="textarea" :rows="4" placeholder="文件内容" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'readFile'">
          <el-form-item label="文件路径">
            <el-input v-model="stepForm.config.file" placeholder="要读取的文件路径" />
          </el-form-item>
        </template>

        <!-- 流程控制 -->
        <template v-if="stepForm.type === 'waitUntil'">
          <el-form-item label="条件">
            <el-input v-model="stepForm.config.condition" type="textarea" :rows="2" placeholder="等待条件表达式" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'catchError'">
          <el-form-item label="执行块">
            <el-input v-model="stepForm.config.block" type="textarea" :rows="2" placeholder="要捕获错误的代码块" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'error'">
          <el-form-item label="错误消息">
            <el-input v-model="stepForm.config.message" placeholder="错误消息" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'unstable'">
          <el-form-item label="消息">
            <el-input v-model="stepForm.config.message" placeholder="不稳定原因" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'lock'">
          <el-form-item label="锁定资源">
            <el-input v-model="stepForm.config.resource" placeholder="资源名称" />
          </el-form-item>
          <el-form-item label="资源繁忙时跳过">
            <el-switch v-model="stepForm.config.skipIfLocked" />
          </el-form-item>
        </template>

        <!-- AWS -->
        <template v-if="stepForm.type === 'withAWS'">
          <el-form-item label="区域">
            <el-input v-model="stepForm.config.region" placeholder="例如：us-east-1" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'withDockerRegistry'">
          <el-form-item label="注册表配置">
            <el-input v-model="stepForm.config.registry" placeholder="Docker 注册表配置" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'chdir'">
          <el-form-item label="目录路径">
            <el-input v-model="stepForm.config.path" placeholder="Windows 要切换的目录" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'tool'">
          <el-form-item label="工具名称">
            <el-input v-model="stepForm.config.name" placeholder="在系统配置中定义的工具名称" />
          </el-form-item>
        </template>

        <!-- 通知 -->
        <template v-if="stepForm.type === 'mail'">
          <el-form-item label="收件人">
            <el-input v-model="stepForm.config.to" placeholder="例如：dev@example.com" />
          </el-form-item>
          <el-form-item label="主题">
            <el-input v-model="stepForm.config.subject" placeholder="邮件主题" />
          </el-form-item>
          <el-form-item label="正文">
            <el-input v-model="stepForm.config.body" type="textarea" :rows="3" placeholder="邮件正文" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'slackSend'">
          <el-form-item label="频道">
            <el-input v-model="stepForm.config.channel" placeholder="例如：#builds" />
          </el-form-item>
          <el-form-item label="消息">
            <el-input v-model="stepForm.config.message" placeholder="要发送的消息" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'dingtalk'">
          <el-form-item label="消息内容">
            <el-input v-model="stepForm.config.message" type="textarea" :rows="3" placeholder="钉钉消息内容" />
          </el-form-item>
        </template>

        <template v-if="stepForm.type === 'wechat'">
          <el-form-item label="消息内容">
            <el-input v-model="stepForm.config.message" type="textarea" :rows="3" placeholder="企业微信消息内容" />
          </el-form-item>
        </template>

        <!-- build -->
        <template v-if="stepForm.type === 'build'">
          <!-- <el-form-item label="子流水线名称">
            <el-input v-model="stepForm.name" placeholder="子流水线显示名称" />
          </el-form-item> -->
          <el-form-item label="Job 名称">
            <el-input v-model="stepForm.config.job" placeholder="要触发的 Jenkins 任务名称" />
          </el-form-item>
        </template>

        <!-- parallel -->
        <template v-if="stepForm.type === 'parallel'">
          <el-form-item label="并行块">
            <el-input v-model="stepForm.config.block" type="textarea" :rows="3" placeholder="并行执行的代码块" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="stepDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveStep">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import { ElMessage } from 'element-plus'
import type { PipelineStage, PipelineStep } from '@/types/pipeline'
import { Close, Plus, Delete, Edit } from '@element-plus/icons-vue'

const props = defineProps<{
  stage?: PipelineStage | null
}>()

const emit = defineEmits<{
  (e: 'save', stage: PipelineStage): void
  (e: 'close'): void
}>()

const isEdit = computed(() => !!props.stage)
const hasWhen = ref(false)

const stepTypes = [
  // 构建
  { value: 'sh', label: 'Shell 命令' },
  { value: 'bat', label: 'Windows Batch' },
  { value: 'powershell', label: 'PowerShell' },
  { value: 'python', label: 'Python 脚本' },
  { value: 'dockerBuildAndPush', label: 'Docker 构建推送' },
  { value: 'dockerPull', label: 'Docker 拉取' },
  { value: 'dockerRun', label: 'Docker 运行' },
  { value: 'kubectl', label: 'Kubectl 命令' },
  { value: 'maven', label: 'Maven 构建' },
  { value: 'jar', label: '构建jar' },
  { value: 'gradle', label: 'Gradle 构建' },
  { value: 'npm', label: 'NPM 构建' },
  { value: 'ant', label: 'Ant 构建' },
  { value: 'node', label: 'Node编译' },
  { value: 'cpp', label: 'C++多平台构建' },
  { value: 'go', label: 'Go多平台构建' },
  { value: 'qt', label: 'Qt多平台构建' },
  { value: 'net', label: 'Net编译' },
  { value: 'php', label: 'PHP编译' },
  // 版本控制
  { value: 'git', label: 'Git 拉取' },
  { value: 'checkout', label: '代码检出 (SCM)' },
  { value: 'svn', label: 'SVN 拉取' },
  // 测试
  { value: 'junit', label: 'JUnit 测试结果' },
  { value: 'coverage', label: '覆盖率报告' },
  { value: 'findbugs', label: 'FindBugs 静态分析' },
  { value: 'checkstyle', label: 'Checkstyle 检查' },
  { value: 'sonar', label: 'SonarQube 扫描' },
  { value: 'pytest', label: 'PyTest 测试' },
  { value: 'nose', label: 'Nose 测试' },
  { value: 'apiTest', label: '接口测试' },
  // 部署
  { value: 'deploy', label: '通用部署' },
  { value: 'scpDeploy', label: '远程传输文件部署' },
  { value: 'sshCommand', label: 'SSH 远程命令' },
  { value: 'sshScript', label: 'SSH 远程脚本' },
  { value: 'ansiblePlaybook', label: 'Ansible Playbook' },
  { value: 'ansibleAdHoc', label: 'Ansible 临时命令' },
  // 制品与文件
  { value: 'archiveArtifacts', label: '归档制品' },
  { value: 'archiveAWS S3', label: '归档到 S3' },
  { value: 'copyArtifacts', label: '复制制品' },
  { value: 'stash', label: '暂存文件' },
  { value: 'unstash', label: '恢复暂存' },
  { value: 'cleanWs', label: '清理工作区' },
  { value: 'deleteDir', label: '删除目录' },
  { value: 'writeFile', label: '写入文件' },
  { value: 'readFile', label: '读取文件' },
  // 流程控制
  { value: 'input', label: '人工审批' },
  { value: 'timeout', label: '超时控制' },
  { value: 'retry', label: '重试' },
  { value: 'sleep', label: '等待' },
  { value: 'waitUntil', label: '等待条件' },
  { value: 'catchError', label: '捕获错误' },
  { value: 'error', label: '抛出错误' },
  { value: 'unstable', label: '标记不稳定' },
  { value: 'lock', label: '锁定资源' },
  { value: 'milestone', label: '里程碑' },
  // 环境与凭据
  { value: 'withCredentials', label: '使用凭据' },
  { value: 'withEnv', label: '使用环境变量' },
  { value: 'withAWS', label: 'AWS 凭据' },
  { value: 'withDockerRegistry', label: 'Docker 注册表' },
  { value: 'dir', label: '切换目录' },
  { value: 'chdir', label: '切换目录 (Windows)' },
  { value: 'tool', label: '调用工具' },
  // 通知
  { value: 'echo', label: '打印消息' },
  { value: 'mail', label: '发送邮件' },
  { value: 'slackSend', label: 'Slack 通知' },
  { value: 'dingtalk', label: '钉钉通知' },
  { value: 'wechat', label: '企业微信通知' },
  // 其他
  { value: 'build', label: '触发构建' },
  { value: 'script', label: '脚本块' },
  { value: 'parallel', label: '并行块' },
  { value: 'structuredClone', label: '结构化克隆' },
  { value: 'custom', label: '自定义代码' },
]

function createDefaultStep(): PipelineStep {
  return {
    id: uuidv4(),
    type: 'sh',
    name: '新步骤',
    config: {}
  }
}

function createDefaultBranch() {
  return {
    id: uuidv4(),
    name: '默认分支',
    steps: [createDefaultStep()]
  }
}

const form = reactive<PipelineStage>({
  id: uuidv4(),
  name: '',
  type: 'sequential',
  branches: [createDefaultBranch()],
  when: undefined
})

const stepDialogVisible = ref(false)
const isEditStep = ref(false)
const currentBranchIndex = ref(0)
const currentStepIndex = ref(-1)

const stepForm = reactive<PipelineStep>({
  id: uuidv4(),
  type: 'sh',
  name: '',
  config: {}
})

watch(hasWhen, (newVal) => {
  if (newVal && !form.when) {
    form.when = { branch: '', expression: '' }
  }
})

watch(() => props.stage, (newVal) => {
  if (newVal) {
    Object.assign(form, JSON.parse(JSON.stringify(newVal)))
    hasWhen.value = !!newVal.when
  } else {
    form.id = uuidv4()
    form.name = ''
    form.type = 'sequential'
    form.branches = [createDefaultBranch()]
    form.when = undefined
    hasWhen.value = false
  }
}, { immediate: true })

function addBranch() {
  form.branches.push(createDefaultBranch())
}

function removeBranch(index: number) {
  if (form.branches.length > 1) {
    form.branches.splice(index, 1)
  }
}

function openStepDialog(branchIndex: number) {
  currentBranchIndex.value = branchIndex
  currentStepIndex.value = -1
  isEditStep.value = false
  Object.assign(stepForm, createDefaultStep())
  stepDialogVisible.value = true
}

function editStep(branchIndex: number, stepIndex: number) {
  currentBranchIndex.value = branchIndex
  currentStepIndex.value = stepIndex
  isEditStep.value = true
  const step = form.branches[branchIndex].steps[stepIndex]
  Object.assign(stepForm, JSON.parse(JSON.stringify(step)))
  stepDialogVisible.value = true
}

function saveStep() {
  if (isEditStep.value && currentStepIndex.value >= 0) {
    form.branches[currentBranchIndex.value].steps[currentStepIndex.value] = JSON.parse(JSON.stringify(stepForm))
  } else {
    form.branches[currentBranchIndex.value].steps.push(JSON.parse(JSON.stringify(stepForm)))
  }
  stepDialogVisible.value = false
}

function removeStep(branchIndex: number, stepIndex: number) {
  form.branches[branchIndex].steps.splice(stepIndex, 1)
}

function getStepTypeColor(type: string): string {
  const colorMap: Record<string, string> = {
    // 构建
    sh: 'primary',
    bat: 'primary',
    powershell: 'primary',
    python: 'success',
    dockerBuildAndPush: 'success',
    dockerPull: 'success',
    dockerRun: 'success',
    kubectl: 'primary',
    maven: 'primary',
    jar: 'primary',
    gradle: 'primary',
    npm: 'primary',
    ant: 'primary',
    node: 'success',
    cpp: 'primary',
    go: 'primary',
    qt: 'primary',
    net: 'primary',
    php: 'primary',
    // 版本控制
    git: 'success',
    checkout: 'success',
    svn: 'success',
    // 测试
    junit: 'success',
    coverage: 'success',
    findbugs: 'warning',
    checkstyle: 'warning',
    sonar: 'warning',
    pytest: 'success',
    nose: 'success',
    apiTest: 'success',
    // 部署
    deploy: 'danger',
    scpDeploy: 'danger',
    sshCommand: 'primary',
    sshScript: 'primary',
    ansiblePlaybook: 'primary',
    ansibleAdHoc: 'primary',
    // 制品与文件
    archiveArtifacts: 'success',
    'archiveAWS S3': 'success',
    copyArtifacts: 'success',
    stash: 'info',
    unstash: 'info',
    cleanWs: 'info',
    deleteDir: 'info',
    writeFile: 'primary',
    readFile: 'primary',
    // 流程控制
    input: 'warning',
    timeout: 'warning',
    retry: 'danger',
    sleep: 'info',
    waitUntil: 'warning',
    catchError: 'warning',
    error: 'danger',
    unstable: 'warning',
    lock: 'info',
    milestone: 'info',
    // 环境与凭据
    withCredentials: 'warning',
    withEnv: 'warning',
    withAWS: 'warning',
    withDockerRegistry: 'warning',
    dir: 'info',
    chdir: 'info',
    tool: 'info',
    // 通知
    echo: 'info',
    mail: 'primary',
    slackSend: 'success',
    dingtalk: 'success',
    wechat: 'success',
    // 其他
    build: 'primary',
    script: 'primary',
    parallel: 'primary',
    structuredClone: 'info',
    custom: 'primary'
  }
  return colorMap[type] || 'info'
}

function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入 Stage 名称')
    return
  }

  if (!hasWhen.value) {
    form.when = undefined
  }

  emit('save', JSON.parse(JSON.stringify(form)))
}
</script>

<style scoped>
.stage-editor {
  padding: 20px;
  background: white;
  border-radius: 8px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e4e7ed;
}

.editor-header h3 {
  margin: 0;
  font-size: 18px;
}

.editor-form {
  max-height: 60vh;
  overflow-y: auto;
}

.branches-section {
  margin-top: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.section-title {
  font-weight: bold;
  font-size: 14px;
}

.branch-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 15px;
  margin-bottom: 15px;
  background: #fafafa;
}

.branch-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.steps-list {
  padding-left: 10px;
}

.step-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 8px;
}

.step-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.step-name {
  font-size: 13px;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
}
</style>
