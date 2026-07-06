<template>
  <div class="log-viewer">
    <div class="log-header">
      <div class="log-title">
        <el-icon><Document /></el-icon>
        <span>运行日志</span>
        <el-tag v-if="run" :type="getStatusType(run.status)" size="small">
          {{ getStatusText(run.status) }}
        </el-tag>
      </div>
      <div class="log-actions">
        <el-button type="primary" size="small" text @click="refreshLogs">
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-button type="primary" size="small" text @click="downloadLogs">
          <el-icon><Download /></el-icon> 下载
        </el-button>
        <el-button type="danger" size="small" text @click="$emit('close')">
          <el-icon><Close /></el-icon> 关闭
        </el-button>
      </div>
    </div>

    <!-- 运行概览 -->
    <div v-if="run" class="run-overview">
      <el-descriptions :column="3" size="small" border>
        <el-descriptions-item label="构建号">#{{ run.buildNumber }}</el-descriptions-item>
        <el-descriptions-item label="触发人">{{ run.triggeredBy }}</el-descriptions-item>
        <el-descriptions-item label="分支">{{ run.branch || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatTime(run.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ run.endTime ? formatTime(run.endTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="持续时间">{{ formatDuration(run.duration) }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <!-- Stage 日志切换 -->
    <div v-if="run && run.stages.length > 0" class="stage-tabs">
      <el-tabs v-model="activeStageIndex" type="card">
        <el-tab-pane
          v-for="(stage, index) in run.stages"
          :key="stage.stageId"
          :label="getStageName(stage.stageId)"
          :name="index"
        >
          <template #label>
            <span class="tab-label">
              <el-icon v-if="stage.status === 'running'" class="is-loading"><Loading /></el-icon>
              <el-icon v-else-if="stage.status === 'success'" color="#52c41a"><CircleCheck /></el-icon>
              <el-icon v-else-if="stage.status === 'failed'" color="#ff4d4f"><CircleClose /></el-icon>
              <el-icon v-else-if="stage.status === 'skipped'" color="#d9d9d9"><Remove /></el-icon>
              {{ getStageName(stage.stageId) }}
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 日志内容 -->
    <div class="log-content" ref="logContentRef">
      <div v-if="loadingLogs" class="log-loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在加载日志...</span>
      </div>
      <div v-else-if="!currentLogs.length" class="log-empty">
        <el-empty description="暂无日志" />
      </div>
      <div v-else class="log-lines">
        <div
          v-for="(log, index) in currentLogs"
          :key="index"
          class="log-line"
          :class="getLogLineClass(log)"
        >
          <span class="log-index">{{ index + 1 }}</span>
          <span class="log-text">{{ log }}</span>
        </div>
        <div v-if="isRunning" class="log-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在运行中...</span>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="log-footer">
      <el-checkbox v-model="autoScroll">自动滚动到底部</el-checkbox>
      <el-button v-if="isRunning" type="danger" size="small" @click="abortRun">
        <el-icon><CircleClose /></el-icon> 终止构建
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { Document, Refresh, Download, Close, Loading, CircleCheck, CircleClose, Remove } from '@element-plus/icons-vue'
import type { PipelineRun } from '@/types/pipeline'
import { usePipelineStore } from '@/stores/pipeline2'
import axios from '@/utils/axios'

const props = defineProps<{
  run: PipelineRun | null
}>()

defineEmits<{
  (e: 'close'): void
}>()

const store = usePipelineStore()
const logContentRef = ref<HTMLElement>()
const activeStageIndex = ref(0)
const autoScroll = ref(true)
const apiLogs = ref<string[]>([])
const loadingLogs = ref(false)

const isRunning = computed(() => props.run?.status === 'running')

const currentLogs = computed(() => {
  if (apiLogs.value.length > 0) return apiLogs.value
  if (!props.run || !props.run.stages[activeStageIndex.value]) return []
  return props.run.stages[activeStageIndex.value].logs
})

async function fetchLogsFromApi() {
  if (!props.run?.pipelineId || !props.run?.buildNumber) return

  loadingLogs.value = true
  try {
    const res = await axios.get('/api/pipeline/build/log', {
      params: {
        pipeLineId: props.run.pipelineId,
        buildNumber: props.run.buildNumber
      }
    })
    console.log('构建日志 API 返回:', res.data)

    // 根据后端返回格式解析日志
    let logs: string[] = []
    if (res.data) {
      if (Array.isArray(res.data)) {
        logs = res.data
      } else if (typeof res.data === 'string') {
        logs = res.data.split('\n')
      } else if (res.data.data) {
        logs = Array.isArray(res.data.data) ? res.data.data : res.data.data.split('\n')
      }
    }
    apiLogs.value = logs
  } catch (error) {
    console.error('获取构建日志失败:', error)
    apiLogs.value = ['获取日志失败，请重试']
  } finally {
    loadingLogs.value = false
  }
}

// 监听 run prop 变化，重新获取日志
watch(() => props.run, (newRun) => {
  if (newRun?.pipelineId && newRun?.buildNumber) {
    apiLogs.value = []
    fetchLogsFromApi()
  }
}, { immediate: true })

function getStageName(stageId: string): string {
  const stage = store.currentPipeline?.stages.find(s => s.id === stageId)
  return stage?.name || stageId
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    success: 'success',
    failed: 'danger',
    running: 'primary',
    aborted: 'warning',
    unstable: 'warning',
    skipped: 'info',
    pending: 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    success: '成功',
    failed: '失败',
    running: '运行中',
    aborted: '已中止',
    unstable: '不稳定',
    skipped: '已跳过',
    pending: '构建中'
  }
  return map[status] || status
}

function getLogLineClass(log: string): string {
  if (log.includes('ERROR') || log.includes('FAILED')) return 'error'
  if (log.includes('WARN')) return 'warning'
  if (log.includes('SUCCESS') || log.includes('completed')) return 'success'
  return ''
}

function formatTime(timeStr: string): string {
  return new Date(timeStr).toLocaleString('zh-CN')
}

function formatDuration(ms?: number): string {
  if (!ms) return '-'
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  if (minutes < 60) return `${minutes}分${remainingSeconds}秒`
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return `${hours}时${remainingMinutes}分${remainingSeconds}秒`
}

function refreshLogs() {
  apiLogs.value = []
  fetchLogsFromApi()
}

async function downloadLogs() {
  if (!props.run?.pipelineId || !props.run?.buildNumber) return

  try {
    const response = await axios.get('/api/pipeline/build/log/download', {
      params: {
        pipeLineId: props.run.pipelineId,
        buildNumber: props.run.buildNumber
      },
      responseType: 'blob'
    })
    const blob = new Blob([response.data], { type: 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `build-${props.run.buildNumber}-logs.txt`
    a.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载日志失败:', error)
  }
}

function abortRun() {
  if (!props.run) return
  props.run.status = 'aborted'
  props.run.endTime = new Date().toISOString()
}

watch(() => currentLogs.value.length, () => {
  if (autoScroll.value) {
    nextTick(() => {
      if (logContentRef.value) {
        logContentRef.value.scrollTop = logContentRef.value.scrollHeight
      }
    })
  }
})
</script>

<style scoped>
.log-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #e4e4e7;
  border-radius: 12px;
  overflow: hidden;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #252538 0%, #1f1f30 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.log-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #f4f4f5;
}

.log-title .el-icon {
  color: #60a5fa;
  font-size: 18px;
}

.log-actions {
  display: flex;
  gap: 8px;
}

.log-actions .el-button {
  border-radius: 8px;
  transition: all 0.2s;
}

.log-actions .el-button:hover {
  background: rgba(255, 255, 255, 0.1);
}

.run-overview {
  padding: 16px 20px;
  background: linear-gradient(135deg, #1f1f30 0%, #252538 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.stage-tabs {
  background: linear-gradient(135deg, #252538 0%, #1f1f30 100%);
}

.stage-tabs :deep(.el-tabs__header) {
  margin: 0;
  background: transparent;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.stage-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.stage-tabs :deep(.el-tabs__item) {
  color: #a1a1aa;
  padding: 0 20px;
  height: 42px;
  line-height: 42px;
  border: none;
  transition: all 0.2s;
}

.stage-tabs :deep(.el-tabs__item:hover) {
  color: #e4e4e7;
  background: rgba(255, 255, 255, 0.05);
}

.stage-tabs :deep(.el-tabs__item.is-active) {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.1);
  border-bottom: 2px solid #60a5fa;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.log-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.7;
  background: #0d1117;
}

.log-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.log-loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  height: 200px;
  color: #60a5fa;
  font-size: 14px;
}

.log-empty .el-empty {
  padding: 30px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
}

.log-lines {
  display: flex;
  flex-direction: column;
}

.log-line {
  display: flex;
  gap: 16px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.15s;
}

.log-line:hover {
  background: rgba(255, 255, 255, 0.05);
}

.log-index {
  color: #484f58;
  min-width: 50px;
  text-align: right;
  user-select: none;
  font-size: 12px;
}

.log-text {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-word;
}

.log-line.error .log-text {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  border-left: 2px solid #ff6b6b;
}

.log-line.warning .log-text {
  color: #ffd93d;
  background: rgba(255, 217, 61, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  border-left: 2px solid #ffd93d;
}

.log-line.success .log-text {
  color: #6bcb77;
  background: rgba(107, 203, 119, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  border-left: 2px solid #6bcb77;
}

.log-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 0;
  color: #60a5fa;
  font-size: 13px;
}

.log-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: linear-gradient(135deg, #252538 0%, #1f1f30 100%);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.log-footer .el-checkbox__label {
  color: #a1a1aa;
}

:deep(.el-descriptions__body) {
  background: transparent;
}

:deep(.el-descriptions__label) {
  background: rgba(255, 255, 255, 0.04) !important;
  color: #a1a1aa !important;
  border-color: rgba(255, 255, 255, 0.06) !important;
  font-size: 13px;
}

:deep(.el-descriptions__content) {
  background: transparent !important;
  color: #e4e4e7 !important;
  border-color: rgba(255, 255, 255, 0.06) !important;
  font-size: 13px;
}

:deep(.el-tag) {
  border-radius: 6px;
}
</style>
