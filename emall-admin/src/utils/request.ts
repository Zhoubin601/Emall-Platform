import axios from 'axios'
import { ElMessage } from 'element-plus'

// 1. 创建 axios 实例
const request = axios.create({
  // 这里填入你 Spring Boot 后端的地址
  baseURL: 'http://localhost:8080/api',
  timeout: 5000 // 请求超时时间
})

// 2. 请求拦截器 (可以在这里统一带上 Token)
request.interceptors.request.use(
  config => {
    // 比如：config.headers['Authorization'] = 'Bearer ' + localStorage.getItem('token')
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
    ElMessage.error(error.message || '网络请求失败，请检查后端是否启动')
    return Promise.reject(error)
  }
)

export default request