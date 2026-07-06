import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router/index'

const instance = axios.create({
  baseURL: '/',
  timeout: 100000
})

// 请求拦截 - 添加 token
instance.interceptors.request.use(config => {
  const token = 'your-token-here'
  config.headers = config.headers || {}
  config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截 - 401 处理
instance.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default instance