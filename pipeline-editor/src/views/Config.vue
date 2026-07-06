<template>
  <div class="config-page">
    <header class="header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="logo">
          <el-icon :size="20"><Connection /></el-icon>
          <span>配置管理</span>
        </div>
      </div>
    </header>

    <main class="main-container">
      <div class="toolbar">
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增配置
        </el-button>
      </div>

      <!-- 配置列表 -->
      <div class="config-table">
        <el-table :data="configList" border>
          <el-table-column prop="jenkinsIp" label="IP" min-width="180" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="token" label="Token" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="240" align="center">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="openEditDialog(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button type="danger" size="small" text @click="deleteConfig(row.id)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="configList.length === 0" description="暂无配置" />
      </div>
    </main>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="isEdit ? '编辑配置' : '新增配置'"
      width="520px"
      destroy-on-close
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="IP" prop="jenkinsIp">
          <el-input v-model="formData.jenkinsIp" placeholder="Jenkins 服务器 IP 地址" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="formData.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item label="Token" prop="token">
          <el-input v-model="formData.token" type="password" placeholder="API Token" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Connection, ArrowLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import axios from '@/utils/axios'

const router = useRouter()

interface JenkinsConfig {
  id?: string
  jenkinsIp: string
  username: string
  password: string
  token: string
}

const configList = ref<JenkinsConfig[]>([])
const showDialog = ref(false)
const isEdit = ref(false)
const editingId = ref('')
const formRef = ref()

const formData = reactive<Omit<JenkinsConfig, 'id'>>({
  jenkinsIp: '',
  username: '',
  password: '',
  token: ''
})

const rules = {
  jenkinsIp: [{ required: true, message: '请输入 IP 地址', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  token: [{ required: true, message: '请输入 Token', trigger: 'blur' }]
}

onMounted(() => {
  fetchConfigs()
})

async function fetchConfigs() {
  try {
    const res = await axios.get('/api/jenkins-config')
    const data = Array.isArray(res.data) ? res.data : ((res.data as any)?.data || [])
    configList.value = data
  } catch (error) {
    console.error('获取配置列表失败:', error)
  }
}

function goBack() {
  router.push('/pipelines')
}

function openCreateDialog() {
  isEdit.value = false
  Object.assign(formData, { jenkinsIp: '', username: '', password: '', token: '' })
  showDialog.value = true
}

function openEditDialog(row: JenkinsConfig) {
  isEdit.value = true
  editingId.value = row.id || ''
  Object.assign(formData, {
    id: editingId.value,
    jenkinsIp: row.jenkinsIp,
    username: row.username,
    password: row.password,
    token: row.token
  })
  showDialog.value = true
}

async function saveConfig() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  try {
    if(isEdit.value){
      await axios.put(`/api/jenkins-config/{formData.id}`, formData)
    }else{
      await axios.post('/api/jenkins-config', formData)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
    showDialog.value = false
    fetchConfigs()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

async function deleteConfig(id: string) {
  try {
    await ElMessageBox.confirm('确定要删除该配置吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`/api/jenkins-config/${id}`)
    ElMessage.success('删除成功')
    fetchConfigs()
  } catch (error) {
    if ((error as any)?.type !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}
</script>

<style scoped>
.config-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

.header {
  height: 56px;
  background: white;
  border-bottom: 1px solid #d9d9d9;
  display: flex;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-size: 18px;
  font-weight: 600;
  color: #1677ff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.toolbar {
  margin-bottom: 16px;
}

.config-table {
  background: white;
  border-radius: 8px;
  padding: 16px;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>