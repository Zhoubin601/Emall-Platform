import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

type ApiClient = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete'> & {
  get<T = any, R = T, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
  post<T = any, R = T, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
  put<T = any, R = T, D = any>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
  delete<T = any, R = T, D = any>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
}

// 1. 创建 axios 实例
const request = axios.create({
  // ✨ 核心修复：改成 '/api'，让 Vite 的代理 (proxy) 正式生效！
  // 这样就能彻底避免浏览器直连 8080 产生的跨域拦截和 OPTIONS 请求失败问题。
  baseURL: '/api', 
  timeout: 5000 
}) as ApiClient

// 2. 请求拦截器 (可以在这里统一带上 Token)
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('mall-token')
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
      localStorage.removeItem('mall-token')
      localStorage.removeItem('mall_user')
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
