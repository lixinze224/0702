<template>
  <div class="pipeline-canvas" ref="canvasRef">
    <!-- 运行进度指示器 -->
    <div v-if="store.isRunning || currentRun" class="run-progress-overlay">
      <div class="run-progress-header">
        <div class="run-progress-info">
          <el-icon class="is-loading" color="#1677ff"><Loading /></el-icon>
          <span v-if="store.isRunning">正在运行 #{{ currentRun?.buildNumber || '' }}...</span>
          <span v-else>运行 #{{ currentRun?.buildNumber || '' }} {{ getRunStatusText(currentRun?.status || '') }}</span>
        </div>
        <div class="run-progress-actions" v-if="!store.isRunning">
          <el-button size="small" text @click="viewLogs">
            <el-icon><Document /></el-icon> 查看日志
          </el-button>
        </div>
      </div>
      <div class="run-progress-stages">
        <div
          v-for="stage in runStages"
          :key="stage.stageId"
          class="progress-stage"
          :class="getStageProgressClass(stage)"
        >
          <div class="progress-stage-indicator">
            <el-icon v-if="stage.status === 'running'" class="is-loading"><Loading /></el-icon>
            <el-icon v-else-if="stage.status === 'success'" color="#52c41a"><CircleCheck /></el-icon>
            <el-icon v-else-if="stage.status === 'failed'" color="#ff4d4f"><CircleClose /></el-icon>
            <el-icon v-else-if="stage.status === 'skipped'" color="#d9d9d9"><Remove /></el-icon>
            <el-icon v-else color="#8c8c8c"><Clock /></el-icon>
          </div>
          <div class="progress-stage-name">{{ getStageName(stage.stageId) }}</div>
          <div class="progress-stage-time" v-if="stage.startTime">
            {{ formatDuration(stage.duration) }}
          </div>
        </div>
      </div>
      <!-- 进度条 -->
      <div class="run-progress-bar">
        <div class="run-progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
    </div>

    <!-- Canvas Toolbar -->
    <div class="canvas-toolbar">
      <el-button size="small" text title="添加阶段" @click="addNewStage">
        <el-icon><Plus /></el-icon>
      </el-button>
      <el-button size="small" text title="自动布局" @click="autoLayout">
        <el-icon><MagicStick /></el-icon>
      </el-button>
      <div class="toolbar-divider"></div>
      <el-button size="small" text title="放大" @click="zoomIn">
        <el-icon><ZoomIn /></el-icon>
      </el-button>
      <el-button size="small" text title="缩小" @click="zoomOut">
        <el-icon><ZoomOut /></el-icon>
      </el-button>
      <el-button size="small" text title="适应屏幕" @click="fitView">
        <el-icon><FullScreen /></el-icon>
      </el-button>
    </div>

    <!-- Canvas Content -->
    <div class="canvas-content" :style="{ transform: `scale(${zoom})` }">
      <!-- Pipeline Stages -->
      <div class="stages-container">
        <div
          v-for="(stage, stageIndex) in stages"
          :key="stage.id"
          class="stage-container"
        >
          <!-- Stage Header -->
          <div class="stage-header" :class="getStageStatusClass(stage.id)" @click="emit('select-stage', stage.id)">
            <div class="stage-title-row">
              <span class="stage-name" @click.stop="handleEditStageName(stage)">{{ stage.name }}</span>
              <div class="stage-status-indicator" :class="getStageStatus(stage.id)"></div>
            </div>
            <div class="stage-actions">
              <el-button size="small" text @click.stop="handleEditStageName(stage)" title="编辑阶段名称">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button size="small" text @click.stop="addTaskToStage(stage.id)" title="添加任务">
                <el-icon><Plus /></el-icon>
              </el-button>
              <el-button size="small" text @click.stop="deleteStageConfirm(stage.id)" title="删除阶段">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <div v-if="stageIndex < stages.length - 1" class="stage-arrow">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>

          <!-- Stage Body -->
          <div class="stage-body">
            <!-- Parallel Group -->
            <div v-if="stage.type === 'parallel'" class="parallel-group">
              <div class="parallel-label">并行</div>
              <div class="parallel-tasks">
                <div
                  v-for="branch in stage.branches"
                  :key="branch.id"
                  class="task-node"
                  :class="{ selected: selectedTaskId === branch.id }"
                  @click="selectTask(branch.id)"
                >
                  <div class="task-header">
                    <div class="task-icon" :class="getTaskIconClass(branch)">
                      <el-icon><component :is="getTaskIcon(branch)" /></el-icon>
                    </div>
                    <span class="task-title">{{ branch.name }}</span>
                    <div class="task-status" :class="getTaskStatusClass(branch)"></div>
                  </div>
                  <div class="task-config">
                    <div v-for="(step, idx) in branch.steps.slice(0, 2)" :key="idx" class="task-config-item">
                      <el-icon><Clock /></el-icon>
                      <span>{{ step.name }}</span>
                    </div>
                    <div v-if="branch.steps.length > 2" class="task-config-more">
                      +{{ branch.steps.length - 2 }} 个步骤
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Sequential Tasks -->
            <template v-else>
              <div
                v-for="branch in stage.branches"
                :key="branch.id"
              >
                <div
                  v-for="(step, sIdx) in branch.steps"
                  :key="`${stage.id}-${branch.id}-${sIdx}`"
                  class="task-node"
                  :class="{ selected: selectedTaskId === step.id }"
                  @click="selectTask(step.id)"
                >
                  <div class="task-header">
                    <div class="task-icon" :class="getStepIconClass(step.type)">
                      <el-icon><component :is="getStepIcon(step.type)" /></el-icon>
                    </div>
                    <span class="task-title">{{ step.name }}</span>
                    <div class="task-status" :class="getStepStatusClass(step.id)"></div>
                    <el-button size="small" text class="delete-step-btn" @click.stop="deleteStepConfirm(stage.id, branch.id, step.id)" title="删除步骤">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                  <div class="task-config">
                    <div v-for="(value, idx) in getStepConfig(step)" :key="idx" class="task-config-item">
                      <el-icon><Document /></el-icon>
                      <span>{{ value }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <!-- Add Task Button -->
            <div class="add-task-btn" @click="addTaskToStage(stage.id)">
              <el-icon><Plus /></el-icon>
              添加任务
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="stages.length === 0" class="empty-canvas">
          <el-icon :size="48"><Folder /></el-icon>
          <p>暂无阶段，点击上方"+"添加阶段</p>
        </div>
      </div>
    </div>

    <!-- Add Task Dialog -->
    <el-dialog v-model="showAddTaskDialog" title="添加任务" width="380px">
      <div class="task-template-grid">
        <div class="template-category">
          <div class="category-title">代码源</div>
          <div class="template-card" @click="createTask('git')">
            <el-icon :size="24"><Download /></el-icon>
            <div class="template-info">
              <div class="template-name">拉取代码</div>
              <div class="template-desc">从git拉代码</div>
            </div>
          </div>
        </div>

        <div class="template-category">
          <div class="category-title">编译</div>
          <div class="template-card" @click="createTask('jar')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">构建jar</div>
              <div class="template-desc">将程序打成jar包</div>
            </div>
          </div>
          <!-- <div class="template-card" @click="createTask('cpp')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">C++多平台构建</div>
              <div class="template-desc">使用多平台构建C++</div>
            </div>
          </div> -->
          <div class="template-card" @click="createTask('go')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">Go多平台构建</div>
              <div class="template-desc">使用多平台构建Go</div>
            </div>
          </div>
          <div class="template-card" @click="createTask('qt')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">Qt多平台构建</div>
              <div class="template-desc">使用多平台构建qt类型</div>
            </div>
          </div>
          <div class="template-card" @click="createTask('javaImage')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">JAVA生成镜像(Arm)</div>
              <div class="template-desc">将jar包打成镜像(Arm版本)</div>
            </div>
          </div>
          <div class="template-card" @click="createTask('node')">
            <el-icon :size="24"><Cpu /></el-icon>
            <div class="template-info">
              <div class="template-name">Node编译</div>
              <div class="template-desc">Node编译</div>
            </div>
          </div>
          <div class="template-card" @click="createTask('net')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">Net编译</div>
              <div class="template-desc">.NET编译</div>
            </div>
          </div>
          <!-- <div class="template-card" @click="createTask('python')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">Python编译</div>
              <div class="template-desc">Python编译</div>
            </div>
          </div> -->
          <div class="template-card" @click="createTask('php')">
            <el-icon :size="24"><Box /></el-icon>
            <div class="template-info">
              <div class="template-name">PHP编译</div>
              <div class="template-desc">PHP编译</div>
            </div>
          </div>
        </div>

        <div class="template-category">
          <div class="category-title">扫描</div>
          <div class="template-card" @click="createTask('code')">
            <el-icon :size="24"><Filter /></el-icon>
            <div class="template-info">
              <div class="template-name">代码扫描</div>
              <div class="template-desc">扫描当前代码</div>
            </div>
          </div>
        </div>

        <div class="template-category">
          <div class="category-title">推送</div>
          <div class="template-card" @click="createTask('deploy')">
            <el-icon :size="24"><Upload /></el-icon>
            <div class="template-info">
              <div class="template-name">推送镜像到制品库（部署）</div>
              <div class="template-desc">推送镜像到制品库</div>
            </div>
          </div>
          <div class="template-card" @click="createTask('release')">
            <el-icon :size="24"><Upload /></el-icon>
            <div class="template-info">
              <div class="template-name">推送镜像到制品库（发版）</div>
              <div class="template-desc">推送镜像到制品库</div>
            </div>
          </div>
        </div>

        <div class="template-category">
          <div class="category-title">测试</div>
          <div class="template-card" @click="createTask('api')">
            <el-icon :size="24"><Clock /></el-icon>
            <div class="template-info">
              <div class="template-name">接口测试</div>
              <div class="template-desc">postman接口测试</div>
            </div>
          </div>
          <div class="template-card" @click="createTask('email')">
            <el-icon :size="24"><Message /></el-icon>
            <div class="template-info">
              <div class="template-name">发送邮件</div>
              <div class="template-desc">发送邮件</div>
            </div>
          </div>
        </div>

        <div class="template-category">
          <div class="category-title">审核</div>
          <div class="template-card" @click="createTask('approve')">
            <el-icon :size="24"><User /></el-icon>
            <div class="template-info">
              <div class="template-name">审批卡点</div>
              <div class="template-desc">进行人为卡点审批</div>
            </div>
          </div>
        </div>

        <div class="template-category">
          <div class="category-title">部署</div>
          <div class="template-card" @click="createTask('cloud')">
            <el-icon :size="24"><Monitor /></el-icon>
            <div class="template-info">
              <div class="template-name">云应用开发环境部署</div>
              <div class="template-desc">云应用开发环境部署</div>
            </div>
          </div>
          <div class="template-card" @click="createTask('scp')">
            <el-icon :size="24"><Upload /></el-icon>
            <div class="template-info">
              <div class="template-name">远程传输文件部署</div>
              <div class="template-desc">远程传输文件部署</div>
            </div>
          </div>
        </div>

        <div class="template-category">
          <div class="category-title">子流水线</div>
          <div class="template-card" @click="createTask('build')">
            <el-icon :size="24"><Connection /></el-icon>
            <div class="template-info">
              <div class="template-name">子流水线</div>
              <div class="template-desc">触发其他Jenkins任务</div>
            </div>
          </div>
        </div>

        <!-- <div class="template-category">
          <div class="category-title">流程控制</div>
          <div class="template-card" @click="createTask('echo')">
            <el-icon :size="24"><ChatLineRound /></el-icon>
            <div class="template-info">
              <div class="template-name">打印日志</div>
              <div class="template-desc">输出文本到控制台</div>
            </div>
          </div>
        </div> -->
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import type { PipelineStage, PipelineStep, PipelineRun, StageStatus } from '@/types/pipeline'
import { usePipelineStore } from '@/stores/pipeline2'
import {
  Plus,
  MagicStick,
  ZoomIn,
  ZoomOut,
  FullScreen,
  ArrowRight,
  Delete,
  Clock,
  Document,
  Folder,
  Box,
  Cpu,
  Monitor,
  User,
  Filter,
  Loading,
  CircleCheck,
  CircleClose,
  Remove,
  Download,
  Upload,
  Message,
  ChatLineRound,
  Edit,
  Connection
} from "@element-plus/icons-vue"

const props = defineProps<{
  run?: PipelineRun | null
}>()

const emit = defineEmits<{
  (e: 'select-stage', stageId: string): void
  (e: 'select-step', stepId: string, step: { type: string; name: string; config: Record<string, any> }): void
  (e: 'view-logs'): void
  (e: 'edit-stage', stage: PipelineStage): void
}>()

const store = usePipelineStore()

// 当前运行的 Run
const currentRun = computed(() => {
  if (!props.run) {
    // 如果没有传入 run，检查是否有正在运行的 run
    const runs = store.pipelineRuns
    return runs.find(r => r.status === 'running') || null
  }
  return props.run
})

// 运行时的阶段状态
const runStages = computed(() => {
  if (!currentRun.value) return []
  return currentRun.value.stages
})

// 计算进度百分比
const progressPercent = computed(() => {
  if (!currentRun.value) return 0
  const stages = currentRun.value.stages
  if (stages.length === 0) return 0

  const completedCount = stages.filter(s =>
    ['success', 'failed', 'skipped', 'aborted', 'unstable'].includes(s.status)
  ).length

  return Math.round((completedCount / stages.length) * 100)
})

function getRunStatusText(status: string): string {
  const map: Record<string, string> = {
    success: '执行成功',
    failed: '执行失败',
    running: '执行中',
    aborted: '已中止',
    unstable: '不稳定',
    skipped: '已跳过',
    pending: '构建中'
  }
  return map[status] || status
}

function getStageProgressClass(stage: { status: StageStatus; startTime?: string; endTime?: string }): string {
  const classes: string[] = []
  if (stage.status === 'running') classes.push('is-running')
  if (stage.status === 'success') classes.push('is-success')
  if (stage.status === 'failed') classes.push('is-failed')
  if (stage.status === 'skipped') classes.push('is-skipped')
  return classes.join(' ')
}

function getStageName(stageId: string): string {
  const stage = store.currentPipeline?.stages.find(s => s.id === stageId)
  return stage?.name || stageId
}

function formatDuration(ms?: number): string {
  if (!ms) return ''
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}m ${remainingSeconds}s`
}

function viewLogs() {
  emit('view-logs')
}

const canvasRef = ref<HTMLElement>()
const zoom = ref(1)
const selectedTaskId = ref<string | null>(null)
const showAddTaskDialog = ref(false)
const currentStageIdForTask = ref<string | null>(null)

const stages = computed(() => store.currentPipeline?.stages || [])

function addNewStage() {
  const newStage: PipelineStage = {
    id: uuidv4(),
    name: `阶段 ${stages.value.length + 1}`,
    type: 'sequential',
    branches: [{
      id: uuidv4(),
      name: '默认分支',
      steps: []
    }]
  }
  store.addStage(newStage)
}

function autoLayout() {
  // Placeholder for auto layout
}

function zoomIn() {
  zoom.value = Math.min(zoom.value + 0.1, 2)
}

function zoomOut() {
  zoom.value = Math.max(zoom.value - 0.1, 0.5)
}

function fitView() {
  zoom.value = 1
}

function handleEditStageName(stage: PipelineStage) {
  emit('edit-stage', stage)
}

function addTaskToStage(stageId: string) {
  currentStageIdForTask.value = stageId
  showAddTaskDialog.value = true
}

function deleteStageConfirm(stageId: string) {
  store.removeStage(stageId)
}

function deleteStepConfirm(stageId: string, branchId: string, stepId: string) {
  store.removeStep(stageId, branchId, stepId)
  if (selectedTaskId.value === stepId) {
    selectedTaskId.value = ''
  }
}

function selectTask(id: string) {
  selectedTaskId.value = id
  // 找到对应的 step 并触发 select-step 事件
  for (const stage of stages.value) {
    for (const branch of stage.branches) {
      const step = branch.steps.find(s => s.id === id)
      if (step) {
        emit('select-step', id, { type: step.type, name: step.name, config: step.config })
        return
      }
    }
  }
}

function createTask(type: string) {
  if (!currentStageIdForTask.value) return

  const stage = stages.value.find(s => s.id === currentStageIdForTask.value)
  if (!stage) return

  const stepTypes: Record<string, { type: PipelineStep['type']; name: string }> = {
    git: { type: 'git', name: '拉取代码' },
    jar: { type: 'jar', name: '构建jar' },
    cpp: { type: 'cpp', name: 'C++多平台构建' },
    go: { type: 'go', name: 'Go多平台构建' },
    qt: { type: 'qt', name: 'Qt多平台构建' },
    javaImage: { type: 'javaImage', name: 'JAVA生成镜像(Arm)' },
    node: { type: 'node', name: 'Node编译' },
    net: { type: 'net', name: 'Net编译' },
    python: { type: 'python', name: 'Python编译' },
    php: { type: 'php', name: 'PHP编译' },
    code: { type: 'sonar', name: '代码扫描' },
    deploy: { type: 'pushImageDeploy', name: '推送镜像到制品库（部署）' },
    release: { type: 'pushImageRelease', name: '推送镜像到制品库（发版）' },
    api: { type: 'apiTest', name: '接口测试' },
    email: { type: 'mail', name: '发送邮件' },
    approve: { type: 'input', name: '审批卡点' },
    cloud: { type: 'cloudDeploy', name: '云应用开发环境部署' },
    scp: { type: 'scpDeploy', name: '远程传输文件部署' },
    build: { type: 'build', name: '子流水线' },
    echo: { type: 'echo', name: '打印日志' }
  }

  const config = stepTypes[type] || { type: 'sh', name: '新任务' }

  const newStep: PipelineStep = {
    id: uuidv4(),
    type: config.type,
    name: config.name,
    config: getDefaultConfig(type)
  }

  // Add to stage - create new branches array to trigger reactivity
  const newBranches = JSON.parse(JSON.stringify(stage.branches))
  if (newBranches.length > 0) {
    newBranches[0].steps.push(newStep)
    store.updateStage(stage.id, { branches: newBranches })
  }

  showAddTaskDialog.value = false
  selectedTaskId.value = newStep.id
}

function getDefaultConfig(type: string): Record<string, any> {
  const configs: Record<string, Record<string, any>> = {
    git: { branch: 'main', credentialsId: '' },
    jar: { command: 'mvn clean package' },
    cpp: { command: 'cmake .. && make' },
    go: { command: 'go build' },
    qt: { command: 'qmake && make' },
    javaImage: { dockerfileContent: 'FROM openjdk:8', imageName: '' },
    node: { command: 'npm install && npm run build' },
    net: { command: 'dotnet build' },
    python: { command: 'python setup.py build' },
    php: { command: 'composer install' },
    code: { command: 'sonar-scanner', projectKey: '', projectName: '', sources: '', binaryPath: '' },
    deploy: { imageName: 'myapp:latest', registryUrl: '', filePath: '', imageTag: '', authToken: '', repositoryName: '', imageLabels: '', timeout: 60 },
    release: { imageName: 'myapp:latest', registryUrl: '', filePath: '', imageTag: '', authToken: '', repositoryName: '', imageLabels: '', timeout: 60 },
    api: { command: 'newman run', collectionPath: '', environment: '', reportName: '' },
    email: { projectName: '', from: '', to: '', subject: '', changes: '' },
    approve: { approver: '', message: '' },
    cloud: { appManageUrl: '', username: '', password: '', deployAppName: '', namespace: '', productFlagName: '', exposePortList: '', cpuNum: '', memory: '', codeRepositoryId2Display: '', exposePort: '' },
    scp: { nodeName: '', remoteCommand: '', transferPath: '' },
    build: { job: '' },
    echo: { message: 'Hello World' }
  }
  return configs[type] || {}
}

function getTaskIconClass(branch: { steps: PipelineStep[] }): string {
  const firstStep = branch.steps[0]
  if (!firstStep) return 'script'

  const iconMap: Record<string, string> = {
    sh: 'script',
    git: 'git',
    echo: 'echo',
    input: 'approve',
    archiveArtifacts: 'deploy',
    junit: 'test',
    cleanWs: 'clean',
    custom: 'script'
  }
  return iconMap[firstStep.type] || 'script'
}

function getTaskIcon(branch: { steps: PipelineStep[] }): string {
  const firstStep = branch.steps[0]
  if (!firstStep) return 'Tools'

  const iconMap: Record<string, string> = {
    sh: 'Tools',
    git: 'Document',
    echo: 'ChatLineSquare',
    input: 'User',
    archiveArtifacts: 'Upload',
    junit: 'Clock',
    cleanWs: 'Delete',
    custom: 'Tools'
  }
  return iconMap[firstStep.type] || 'Tools'
}

function getStepIconClass(type: string): string {
  const iconMap: Record<string, string> = {
    sh: 'script',
    git: 'git',
    echo: 'echo',
    input: 'approve',
    archiveArtifacts: 'deploy',
    junit: 'test',
    cleanWs: 'clean',
    custom: 'script',
    node: 'node',
    jar: 'maven',
    cpp: 'script',
    go: 'script',
    qt: 'script',
    net: 'script',
    python: 'script',
    php: 'script',
    javaImage: 'dockerBuildAndPush',
    build: 'build'
  }
  return iconMap[type] || 'script'
}

function getStepIcon(type: string): string {
  const iconMap: Record<string, string> = {
    sh: 'Tools',
    git: 'Document',
    echo: 'ChatLineSquare',
    input: 'User',
    archiveArtifacts: 'Upload',
    junit: 'Clock',
    cleanWs: 'Delete',
    custom: 'Tools',
    build: 'Connection'
  }
  return iconMap[type] || 'Tools'
}

function getStepConfig(step: PipelineStep): any[] {
  return Object.values(step.config || {}).slice(0, 2)
}

function getTaskStatusClass(branch: { id: string }): string {
  if (!props.run) return ''
  // For parallel stages, branch id matches stageId
  const stage = props.run.stages.find(s => s.stageId === branch.id)
  if (!stage) return ''
  return stage.status
}

function getStepStatusClass(stepId: string): string {
  if (!props.run) return ''
  // For sequential stages, we need to find the step through stage
  const pipeline = store.currentPipeline
  if (!pipeline) return ''

  for (const runStage of props.run.stages) {
    const stage = pipeline.stages.find(s => s.id === runStage.stageId)
    if (!stage) continue
    for (const branch of stage.branches) {
      const step = branch.steps.find(s => s.id === stepId)
      if (step) {
        return runStage.status
      }
    }
  }
  return ''
}

function getStageStatus(stageId: string): string {
  if (!props.run) return ''
  const runStage = props.run.stages.find(s => s.stageId === stageId)
  return runStage?.status || ''
}

function getStageStatusClass(stageId: string): string {
  const status = getStageStatus(stageId)
  const statusClassMap: Record<string, string> = {
    success: 'stage-success',
    failed: 'stage-failed',
    running: 'stage-running',
    skipped: 'stage-skipped'
  }
  return statusClassMap[status] || ''
}
</script>

<style scoped>
.pipeline-canvas {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  position: relative;
  overflow: auto;
  background-image: radial-gradient(circle, #d1d5db 1px, transparent 1px);
  background-size: 22px 22px;
}

/* Running progress indicator */
.run-progress-overlay {
  position: sticky;
  top: 0;
  left: 0;
  right: 0;
  background: #ffffff;
  border-bottom: 1px solid #e4e7ed;
  padding: 16px 24px;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-radius: 0 0 12px 12px;
}

.run-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.run-progress-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.run-progress-actions {
  display: flex;
  gap: 10px;
}

.run-progress-stages {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.progress-stage {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: #f5f5f5;
  border-radius: 6px;
  font-size: 13px;
  transition: all 0.3s;
  border: 1px solid #e8e8e8;
}

.progress-stage:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.progress-stage.is-running {
  background: #e6f4ff;
  border-color: #91caff;
}

.progress-stage.is-success {
  background: #f6ffed;
  border-color: #b7eb8f;
}

.progress-stage.is-failed {
  background: #fff2f0;
  border-color: #ffccc7;
}

.progress-stage.is-skipped {
  background: #f5f5f5;
  border-color: #d9d9d9;
  opacity: 0.7;
}

.progress-stage-indicator {
  display: flex;
  align-items: center;
}

.progress-stage-name {
  font-weight: 500;
}

.progress-stage-time {
  color: #8c8c8c;
  font-size: 12px;
  padding: 2px 8px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 4px;
}

.run-progress-bar {
  margin-top: 14px;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.run-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1677ff 0%, #52c41a 100%);
  border-radius: 3px;
  transition: width 0.5s ease;
  box-shadow: 0 0 8px rgba(22, 119, 255, 0.3);
}

.canvas-toolbar {
  position: sticky;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #ffffff 0%, #f5f5f5 100%);
  border-radius: 12px;
  padding: 10px 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 6px;
  z-index: 50;
  border: 1px solid rgba(0, 0, 0, 0.06);
  margin: 20px auto;
  width: fit-content;
}

.canvas-toolbar :deep(.el-button) {
  border-radius: 8px;
  transition: all 0.2s;
}

.canvas-toolbar :deep(.el-button:hover) {
  background: rgba(22, 119, 255, 0.08);
}

.toolbar-divider {
  width: 1px;
  background: linear-gradient(to bottom, transparent, #d9d9d9, transparent);
  margin: 0 10px;
}

.canvas-content {
  padding: 24px 48px 48px;
  min-height: calc(100% - 80px);
}

.stages-container {
  display: inline-flex;
  flex-direction: row;
  vertical-align: top;
  gap: 0;
  position: relative;
}

/* Stage Container */
.stage-container {
  display: inline-flex;
  flex-direction: column;
  position: relative;
  margin-right: 48px;
}

.stage-header {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  border: 2px solid #1677ff;
  border-radius: 12px 12px 0 0;
  padding: 14px 20px;
  font-weight: 600;
  font-size: 14px;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 280px;
  position: relative;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.25);
}

.stage-header::after {
  content: '';
  position: absolute;
  right: -45px;
  top: 50%;
  width: 44px;
  height: 3px;
  background: linear-gradient(90deg, #4096ff, #d9d9d9);
}

.stage-container:last-child .stage-header::after {
  display: none;
}

.stage-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stage-name {
  cursor: pointer;
  transition: color 0.2s;
  color: #ffffff;
}

.stage-name:hover {
  color: #bae0ff;
}

.stage-status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #bae0ff;
  box-shadow: 0 0 6px rgba(255, 255, 255, 0.5);
}

.stage-status-indicator.success { background: linear-gradient(135deg, #b7eb8f, #52c41a); box-shadow: 0 0 8px rgba(82, 196, 26, 0.6); }
.stage-status-indicator.failed { background: linear-gradient(135deg, #ff7875, #ff4d4f); box-shadow: 0 0 8px rgba(255, 77, 79, 0.6); }
.stage-status-indicator.running { background: linear-gradient(135deg, #91caff, #1677ff); animation: pulse 1.5s infinite; box-shadow: 0 0 8px rgba(22, 119, 255, 0.6); }
.stage-status-indicator.skipped { background: #d9d9d9; box-shadow: 0 0 6px rgba(0, 0, 0, 0.2); }

/* Stage status header colors */
.stage-header.stage-success {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
  border-color: #b7eb8f;
  color: #389e0d;
  box-shadow: 0 4px 16px rgba(82, 196, 26, 0.15);
}

.stage-header.stage-failed {
  background: linear-gradient(135deg, #fff2f0 0%, #ffccc7 100%);
  border-color: #ffccc7;
  color: #cf1322;
  box-shadow: 0 4px 16px rgba(255, 77, 79, 0.15);
}

.stage-header.stage-running {
  background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
  border-color: #91caff;
  color: #1677ff;
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.15);
}

.stage-header.stage-skipped {
  background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%);
  border-color: #d9d9d9;
  color: #8c8c8c;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.stage-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.stage-header:hover .stage-actions {
  opacity: 1;
}

.stage-arrow {
  position: absolute;
  right: -50px;
  top: 55%;
  transform: translateY(-50%);
  color: #4096ff;
  z-index: 10;
  font-size: 18px;
}

.stage-body {
  background: #ffffff;
  border: 2px solid #d9d9d9;
  border-top: none;
  border-radius: 0 0 12px 12px;
  padding: 16px;
  min-width: 280px;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}

/* Task Node */
.task-node {
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.task-node:last-child {
  margin-bottom: 0;
}

.task-node:hover {
  border-color: #1677ff;
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.12);
  transform: translateY(-3px);
}

.task-node.selected {
  border-color: #1677ff;
  box-shadow: 0 0 0 3px rgba(22, 119, 255, 0.15), 0 8px 24px rgba(22, 119, 255, 0.1);
  background: #ffffff;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.delete-step-btn {
  margin-left: auto;
  opacity: 0;
  transition: opacity 0.2s;
}

.task-node:hover .delete-step-btn {
  opacity: 1;
}

.task-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.task-icon.git { background: #fff7e6; color: #fa8c16; }
.task-icon.maven { background: #e6f7ff; color: #1890ff; }
.task-icon.npm { background: #f9f0ff; color: #722ed1; }
.task-icon.dockerBuildAndPush { background: #e6f7ff; color: #1890ff; }
.task-icon.junit { background: #fff7e6; color: #fa8c16; }
.task-icon.sonar { background: #f9f0ff; color: #722ed1; }
.task-icon.sshCommand { background: #f5f5f5; color: #595959; }
.task-icon.kubectl { background: #e6f7ff; color: #08979c; }
.task-icon.echo { background: #f5f5f5; color: #595959; }
.task-icon.sh { background: #f5f5f5; color: #595959; }
.task-icon.build { background: #e6f7ff; color: #1890ff; }
.task-icon.deploy { background: #f6ffed; color: #52c41a; }
.task-icon.test { background: #fff7e6; color: #fa8c16; }
.task-icon.scan { background: #f9f0ff; color: #722ed1; }
.task-icon.approve { background: #fff2f0; color: #ff4d4f; }
.task-icon.script { background: #f5f5f5; color: #595959; }
.task-icon.clean { background: #f5f5f5; color: #595959; }

.task-title {
  font-weight: 600;
  font-size: 14px;
  flex: 1;
  color: #1a1a1a;
}

.task-status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d9d9d9;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.1);
}

.task-status.success { background: linear-gradient(135deg, #52c41a, #73d13d); box-shadow: 0 0 8px rgba(82, 196, 26, 0.4); }
.task-status.running { background: linear-gradient(135deg, #1677ff, #4096ff); animation: pulse 1.5s infinite; box-shadow: 0 0 8px rgba(22, 119, 255, 0.4); }
.task-status.failed { background: linear-gradient(135deg, #ff4d4f, #ff7875); box-shadow: 0 0 8px rgba(255, 77, 79, 0.4); }
.task-status.skipped { background: #d9d9d9; }

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.9); }
}

.task-config {
  font-size: 12px;
  color: #8c8c8c;
  padding-left: 46px;
  line-height: 1.5;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 8px;
  padding: 6px 10px;
  margin-top: 6px;
}

.task-config-item {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 260px;
  padding: 2px 0;
  border-bottom: 1px dashed #e8e8e8;
}
.task-config-item:last-child {
  border-bottom: none;
  margin-bottom: -2px;
}

.task-config-more {
  margin-top: 6px;
  color: #1677ff;
  font-weight: 500;
}

/* Add Task Button */
.add-task-btn {
  border: 2px dashed #d9d9d9;
  border-radius: 10px;
  padding: 10px;
  text-align: center;
  color: #8c8c8c;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  margin-top: 8px;
}

.add-task-btn:hover {
  border-color: #1677ff;
  color: #1677ff;
  background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.1);
}

/* Parallel Group */
.parallel-group {
  border: 2px dashed #b7eb8f;
  border-radius: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fcfffa 0%, #f6ffed 100%);
  position: relative;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.08);
}

.parallel-label {
  position: absolute;
  top: -12px;
  left: 12px;
  background: linear-gradient(135deg, #52c41a, #73d13d);
  padding: 4px 12px;
  font-size: 11px;
  color: white;
  font-weight: 600;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
}

.parallel-tasks {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Empty Canvas */
.empty-canvas {
  text-align: center;
  padding: 80px 24px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.empty-canvas p {
  margin-top: 16px;
  color: #8c8c8c;
}

/* Task Template Grid */
.task-template-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 480px;
  overflow-y: auto;
  padding-right: 4px;
}

.task-template-grid::-webkit-scrollbar {
  width: 6px;
}

.task-template-grid::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 3px;
}

.task-template-grid::-webkit-scrollbar-thumb {
  background: #d9d9d9;
  border-radius: 3px;
}

.task-template-grid::-webkit-scrollbar-thumb:hover {
  background: #bfbfbf;
}

.template-category {
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  padding: 14px 16px;
  background: linear-gradient(180deg, #ffffff 0%, #fafbfc 100%);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.category-title {
  font-size: 12px;
  color: #999999;
  margin-bottom: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.template-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  background: #ffffff;
  gap: 14px;
  margin-bottom: 8px;
}

.template-card:last-child {
  margin-bottom: 0;
}

.template-card:hover {
  border-color: #1677ff;
  background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
  transform: translateX(6px);
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.15);
}

.template-card:hover .template-name {
  color: #1677ff;
}

.template-card:hover .el-icon {
  transform: scale(1.1);
}

.template-card .el-icon {
  color: #1677ff;
  font-size: 24px;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.template-info {
  flex: 1;
  min-width: 0;
}

.template-name {
  font-weight: 500;
  font-size: 14px;
  color: #333333;
  transition: color 0.2s ease;
}

.template-desc {
  font-size: 12px;
  color: #999999;
  margin-top: 3px;
  transition: color 0.2s ease;
}

.template-card:hover .template-desc {
  color: #666666;
}
</style>