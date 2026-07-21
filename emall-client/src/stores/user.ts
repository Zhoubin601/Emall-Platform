import { defineStore } from 'pinia'
import { ref } from 'vue'
import { readStoredJson } from '../utils/storage'

export const useUserStore = defineStore('user', () => {
  // 从本地缓存读取，防止刷新掉登录状态
  const storedToken = localStorage.getItem('mall-token') || ''
  const token = ref(storedToken)
  const userInfo = ref<any>(storedToken ? readStoredJson('mall_user', null) : null)
  if (!storedToken) localStorage.removeItem('mall_user')

  const setLogin = (newToken: string, user: any) => {
    token.value = newToken
    userInfo.value = user
    localStorage.setItem('mall-token', newToken)
    localStorage.setItem('mall_user', JSON.stringify(user))
  }

  const setUser = (user: any) => {
    userInfo.value = user
    localStorage.setItem('mall_user', JSON.stringify(user))
  }

  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('mall-token')
    localStorage.removeItem('mall_user')
  }

  return { userInfo, token, setLogin, setUser, logout }
})
