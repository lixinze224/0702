<template>
  <div class="pipeline-list-page">
    <!-- Header -->
    <header class="header">
      <div class="header-left">
        <div class="logo">
          <el-icon :size="20"><Connection /></el-icon>
          <span>Flow 流水线编排</span>
        </div>
      </div>
    <!-- 主体区域 -->
      <div class="header-right">
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建流水线
        </el-button>
        <el-button @click="goToConfig">
          <el-icon><Setting /></el-icon>
          配置管理
        </el-button>
        <el-button @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出系统
        </el-button>
      </div>
    </header>

    <!-- 主体区域 -->
    <main class="main-container">
      <!-- 空状态 -->
      <div v-if="store.pipelines.length === 0" class="empty-state">
        <el-icon :size="64" color="#d9d9d9"><Box /></el-icon>
        <h3>暂无流水线</h3>
        <p>点击新建按钮创建第一个流水线</p>
      </div>

      <!-- 流水线卡片网格 -->
      <div v-else class="pipeline-grid">
        <div
          v-for="pipeline in store.pipelines"
          :key="pipeline.id"
          class="pipeline-card"
          :class="{ selected: selectedPipelineId === pipeline.id }"
          @click="enterEditor(pipeline.id)"
        >
          <div class="card-header-row">
            <div class="card-icon">
              <el-icon :size="18"><Connection /></el-icon>
            </div>
            <h3 class="card-title">{{ pipeline.name }}</h3>
            <div class="card-date">{{ formatDate(pipeline.createdAt) }}</div>
          </div>
          <div class="card-body">
            <p class="card-desc">{{ pipeline.description || '暂无描述' }}</p>
          </div>
          <div class="card-footer">
            <div class="card-stats">
              <span class="stat-item">
                <el-icon><Box /></el-icon>
                {{ pipeline.stageNum }} 阶段
              </span>
            </div>
            <div class="card-actions">
              <el-button
                type="primary"
                size="small"
                text
                @click.stop="openRenameDialog(pipeline)"
                title="重命名"
              >
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button
                type="danger"
                size="small"
                text
                @click.stop="deletePipeline(pipeline.id)"
                title="删除"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 新建流水线对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建流水线" width="520px">
      <el-form :model="newPipelineForm" label-width="100px">
        <el-form-item label="名称" required>
          <el-input v-model="newPipelineForm.name" placeholder="流水线名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newPipelineForm.description" type="textarea" :rows="3" placeholder="流水线描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createPipeline">创建</el-button>
      </template>
    </el-dialog>

    <!-- 重命名对话框 -->
    <el-dialog v-model="showRenameDialog" title="重命名流水线" width="520px">
      <el-form :model="renameForm" label-width="100px">
        <el-form-item label="新名称" required>
          <el-input v-model="renameForm.newName" placeholder="请输入新名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenameDialog = false">取消</el-button>
        <el-button type="primary" @click="renamePipeline">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, Plus, Setting, Delete, Box, Edit, SwitchButton } from '@element-plus/icons-vue'
import axios from '@/utils/axios'
import { v4 as uuidv4 } from 'uuid'
import { usePipelineStore } from '@/stores/pipeline2'
import type { Pipeline } from '@/types/pipeline'

const router = useRouter()
const store = usePipelineStore()

onMounted(() => {
  store.fetchPipelines()
})

const showCreateDialog = ref(false)
const selectedPipelineId = ref<string | null>(null)
const newPipelineForm = reactive({
  name: '',
  description: ''
})

const showRenameDialog = ref(false)
const renameForm = reactive({
  oldName: '',
  newName: ''
})

function openCreateDialog() {
  showCreateDialog.value = true
}

async function createPipeline() {
  if (!newPipelineForm.name.trim()) {
    ElMessage.warning('请输入流水线名称')
    return
  }
  try {
    // // 调用后端API创建新流水线，获取返回的流水线ID
    // const res = await axios.post('/api/pipeline/pipeline', {
    //   name: newPipelineForm.name.trim(),
    //   description: newPipelineForm.description.trim()
    // })
    // console.log('创建流水线返回:', res.data)

    // // 从接口返回数据中获取流水线ID
    // const newPipelineId = res.data?.data?.id

    // 创建新流水线对象（用于跳转到详情页后显示）
    const newPipeline: Pipeline = {
      id: uuidv4(),
      name: newPipelineForm.name.trim(),
      description: newPipelineForm.description.trim(),
      agent: { type: 'any' },
      environment: [],
      stages: [],
      post: {},
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }

    // 添加到 store 的 pipelines 数组
    store.pipelines.push(newPipeline)
    // 设置为当前流水线
    store.setCurrentPipeline(newPipeline.id)

    showCreateDialog.value = false
    // 清空表单
    newPipelineForm.name = ''
    newPipelineForm.description = ''

    // 跳转到流水线详情页面，带上新流水线名称
    router.push(`/pipelineDetail/${newPipeline.id}?name=${encodeURIComponent(newPipeline.name)}`)
  } catch (error) {
    console.error('创建流水线失败:', error)
    ElMessage.error('创建流水线失败')
  }
}

function goToConfig() {
  router.push('/config')
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出系统吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 清除本地存储的登录信息
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    // 跳转到登录页
    router.push('/login')
  } catch {
    // 用户取消操作
  }
}

function enterEditor(pipelineId: string) {
  selectedPipelineId.value = pipelineId
  store.setCurrentPipeline(pipelineId)
  router.push(`/pipelineDetail/${pipelineId}`)
}

function openRenameDialog(pipeline: any) {
  renameForm.oldName = pipeline.name
  renameForm.newName = pipeline.name
  showRenameDialog.value = true
}

async function renamePipeline() {
  if (!renameForm.newName.trim()) {
    ElMessage.warning('请输入新名称')
    return
  }
  try {
    await axios.post('/api/pipeline/rename', {
      oldName: renameForm.oldName,
      newName: renameForm.newName
    })
    ElMessage.success('重命名成功')
    showRenameDialog.value = false
    store.fetchPipelines()
  } catch (error) {
    ElMessage.error('重命名失败')
  }
}

async function deletePipeline(pipelineId: string) {
  try {
    await ElMessageBox.confirm('确定要删除该流水线吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    store.deletePipeline(pipelineId)
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.pipeline-list-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.header {
  height: 64px;
  background: white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #1677ff;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.5px;
}

.logo .el-icon {
  background: linear-gradient(135deg, #1677ff 0%, #4096ff 100%);
  color: white;
  border-radius: 8px;
  padding: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.main-container {
  flex: 1;
  overflow-y: auto;
  padding: 32px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.empty-state .el-icon {
  background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%);
  border-radius: 50%;
  padding: 24px;
}

.empty-state h3 {
  margin: 24px 0 12px;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.empty-state p {
  font-size: 14px;
  color: #8c8c8c;
  margin: 0;
}

.pipeline-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.pipeline-card {
  background: white;
  border-radius: 12px;
  padding: 0;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  height: 160px;
}

.pipeline-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 28px rgba(22, 119, 255, 0.15);
  border: 2px solid #1677ff;
}

.pipeline-card.selected {
  border: 2px solid #1677ff;
  box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.15), 0 8px 28px rgba(22, 119, 255, 0.15);
  transform: translateY(-4px);
  position: relative;
}

.pipeline-card.selected::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 12px;
  border: 3px solid #1677ff;
  pointer-events: none;
  animation: borderPulse 2s ease-in-out infinite;
}

@keyframes borderPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.pipeline-card.selected::after {
  content: '';
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  background: #1677ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #e6f4ff 0%, #bae0ff 100%);
  border-radius: 6px;
  color: #1677ff;
  flex-shrink: 0;
}

.card-header-row {
  display: flex;
  align-items: center;
  padding: 12px 16px 8px;
  gap: 10px;
}

.card-body {
  padding: 0 16px 8px;
  flex: 1;
  overflow: hidden;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.card-desc {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 28px;
}

.card-date {
  font-size: 11px;
  color: #9ca3af;
  flex-shrink: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 16px;
  border-top: 1px solid #f0f0f0;
}

.card-stats {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.stat-item .el-icon {
  color: #1677ff;
  font-size: 14px;
}

.card-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.pipeline-card:hover .card-actions {
  opacity: 1;
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>