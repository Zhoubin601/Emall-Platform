import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 从本地缓存读取，防止刷新掉登录状态
  const userInfo = ref<any>(JSON.parse(localStorage.getItem('mall_user') || 'null'))

  const setUser = (user: any) => {
    userInfo.value = user
    localStorage.setItem('mall_user', JSON.stringify(user))
  }

  const logout = () => {
    userInfo.value = null
    localStorage.removeItem('mall_user')
  }

  return { userInfo, setUser, logout }
})