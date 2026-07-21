import axios from 'axios'
import { ElMessage } from 'element-plus'

// 1. 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 5000 // 请求超时时间
})

// 2. 请求拦截器 (可以在这里统一带上 Token)
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('admin-token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 3. 响应拦截器 (统一处理后端的报错)
request.interceptors.response.use(
  response => {
    // 直接返回数据主体，剥离掉 axios 自带的 status 等外层壳子
    return response.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin-token')
      localStorage.removeItem('admin-info')
      if (window.location.pathname !== '/login') {
        ElMessage.warning('登录已失效，请重新登录')
        window.location.assign('/login')
      }
    } else {
      ElMessage.error(error.response?.data?.message || error.message || '网络请求失败，请检查后端是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
