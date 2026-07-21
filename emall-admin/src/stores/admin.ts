import { defineStore } from 'pinia'
import { ref } from 'vue'
import { readStoredJson } from '../utils/storage'

export const useAdminStore = defineStore('admin', () => {
  // 1. 读取本地存储，防止刷新掉线
  const token = ref(localStorage.getItem('admin-token') || '')
  // ✨ 新增：存储管理员的实体信息 (包含 role)
  const adminInfo = ref<any>(readStoredJson('admin-info', {}))

  // 2. 登录成功时，签发 Token 并保存信息
  const setAdminLogin = (newToken: string, info: any) => {
    token.value = newToken
    adminInfo.value = info
    localStorage.setItem('admin-token', newToken)
    localStorage.setItem('admin-info', JSON.stringify(info))
  }

  // 3. 退出登录时，彻底清空保险箱
  const clearToken = () => {
    token.value = ''
    adminInfo.value = {}
    localStorage.removeItem('admin-token')
    localStorage.removeItem('admin-info')
  }

  return { token, adminInfo, setAdminLogin, clearToken }
})
