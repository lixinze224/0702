<template>
  <el-dialog
    :model-value="modelValue"
    title="构建阶段详情"
    width="600px"
    destroy-on-close
    @close="handleClose"
  >
    <div class="stages-tree">
      <el-tree
        v-if="treeData.length > 0"
        :data="treeData"
        :props="treeProps"
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ node, data }">
          <div class="tree-node">
            <span class="node-name" @click.stop="handleNodeClick(data)">{{ node.label }}</span>
            <el-tag :type="getStatusType(data.status)" size="small">
              {{ data.statusText }}
            </el-tag>
          </div>
        </template>
      </el-tree>
      <el-empty v-else description="暂无阶段数据" />
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>

  <!-- 构建日志抽屉 -->
  <el-drawer
    v-model="showLogDrawer"
    title="构建日志"
    size="65%"
    destroy-on-close
  >
    <template #header>
      <div class="log-drawer-header">
        <el-icon><Document /></el-icon>
        <span>构建日志</span>
      </div>
    </template>
    <div class="log-content">
      <div class="log-toolbar">
        <el-button size="small" @click="copyLogContent">
          <el-icon><DocumentCopy /></el-icon> 复制
        </el-button>
      </div>
      <pre class="log-pre">{{ logContent || '暂无日志' }}</pre>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { usePipelineStore } from '@/stores/pipeline2'
import { ElMessage } from 'element-plus'
import { Document, DocumentCopy } from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: boolean
  jobName: string
  buildNumber: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'close'): void
}>()

const store = usePipelineStore()

const showLogDrawer = ref(false)
const stageData = ref<any>({})
const logContent = ref('')

const treeData = computed(() => {
  if (!stageData.value.stages) return []
  return stageData.value.stages.map((stage: any) => ({
    id: stage.id,
    label: stage.name,
    status: stage.status,
    statusText: getStatusText(stage.status),
    duration: stage.duration,
    children: (stage.steps || []).map((step: any, index: number) => ({
      id: step.id,
      label: `步骤 ${index + 1}`,
      status: stage.status,
      statusText: getStatusText(stage.status),
      duration: stage.duration
    }))
  }))
})

const treeProps = {
  children: 'children',
  label: 'label'
}

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    SUCCESS: 'success',
    FAILURE: 'danger',
    UNSTABLE: 'warning',
    ABORTED: 'info',
    BUILDING: 'primary',
    IN_PROGRESS: 'primary'
  }
  return map[status] || 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    SUCCESS: '构建成功',
    FAILURE: '构建失败',
    UNSTABLE: '构建不稳定',
    ABORTED: '构建被中止',
    QUEUED: '排队中',
    IN_PROGRESS: '构建中',
    NOT_BUILT:'未构建',
    null: '未知'
  }
  return map[status] || status || '未知'
}

function formatDuration(seconds: number): string {
  if (!seconds) return ''
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes}m ${secs}s`
}

async function handleNodeClick(data: any) {
  if (data.id && props.jobName && props.buildNumber) {
    try {
      // 判断是阶段还是步骤
      const isStage = data.children && data.children.length > 0
      let url = ''
      if (isStage) {
        // 阶段日志
        url = `/api/pipeline/stage/log?pipeLineId=${props.jobName}&buildNumber=${props.buildNumber}&stageId=${data.id}&t=${Date.now()}`
      } else {
        // 步骤日志
        url = `/api/pipeline/step/log?pipeLineId=${props.jobName}&buildNumber=${props.buildNumber}&stepId=${data.id}&t=${Date.now()}`
      }
      const res = await fetch(url)
      const result = await res.text();
      
      logContent.value = JSON.parse(result).data || '暂无日志'
      showLogDrawer.value = true
    } catch (error) {
      console.error('获取日志失败:', error)
      logContent.value = '获取日志失败'
      showLogDrawer.value = true
    }
  }
}

async function loadStages() {
  if (props.jobName && props.buildNumber) {
    stageData.value = await store.fetchStages(props.jobName, props.buildNumber)
  }
}

function handleClose() {
  emit('update:modelValue', false)
  emit('close')
}

function copyLogContent() {
  if (logContent.value) {
    navigator.clipboard.writeText(logContent.value).then(() => {
      ElMessage.success('日志已复制')
    })
  }
}

// 监听 props 变化，加载数据
watch([() => props.jobName, () => props.buildNumber], ([newJobName, newBuildNumber]) => {
  if (newJobName && newBuildNumber > 0) {
    loadStages()
  }
}, { immediate: true })
</script>

<style scoped>
.stages-tree {
  max-height: 450px;
  overflow-y: auto;
  padding: 8px;
  background: linear-gradient(135deg, #fafafa 0%, #f5f5f5 100%);
  border-radius: 12px;
}

.stages-tree :deep(.el-tree) {
  background: transparent;
  color: #1a1a1a;
}

.stages-tree :deep(.el-tree-node__content) {
  padding: 8px 12px;
  margin: 4px 0;
  border-radius: 8px;
  transition: all 0.2s;
}

.stages-tree :deep(.el-tree-node__content:hover) {
  background: rgba(22, 119, 255, 0.08);
}

.stages-tree :deep(.el-tree-node__children .el-tree-node__content) {
  background: white;
  border: 1px solid #f0f0f0;
}

.stages-tree :deep(.el-tree-node__children .el-tree-node__content:hover) {
  background: rgba(22, 119, 255, 0.05);
  border-color: #1677ff;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 0;
  width: 100%;
}

.node-name {
  flex: 1;
  font-weight: 500;
  color: #1a1a1a;
  cursor: pointer;
  transition: color 0.2s;
}

.node-name:hover {
  color: #1677ff;
}

.node-duration {
  color: #8c8c8c;
  font-size: 12px;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 4px;
}

.log-content {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0d1117 0%, #161b22 100%);
}

.log-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.log-toolbar .el-button {
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #e4e4e7;
}

.log-toolbar .el-button:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.log-pre {
  flex: 1;
  margin: 0;
  padding: 20px;
  color: #c9d1d9;
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-y: auto;
}

.log-drawer-header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #e4e4e7;
  font-weight: 600;
  font-size: 16px;
}

.log-drawer-header .el-icon {
  color: #60a5fa;
  font-size: 18px;
}

:deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.el-dialog__header) {
  padding: 20px 24px;
  background: linear-gradient(135deg, #fafafa 0%, #ffffff 100%);
  border-bottom: 1px solid #f0f0f0;
  margin: 0;
}

:deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 17px;
  color: #1a1a1a;
}

:deep(.el-dialog__body) {
  padding: 20px 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

:deep(.el-dialog__footer .el-button) {
  border-radius: 8px;
}

:deep(.el-drawer__header) {
  padding: 20px 24px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: #e4e4e7;
  margin: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.el-drawer__body) {
  padding: 0;
}

:deep(.el-empty__description) {
  color: #8c8c8c;
}
</style>