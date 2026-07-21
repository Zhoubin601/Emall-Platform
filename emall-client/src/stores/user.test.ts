import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useUserStore } from './user'

describe('user store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('persists and clears a login session', () => {
    const store = useUserStore()
    const user = { id: 7, username: 'buyer', nickname: '买家' }

    store.setLogin('signed-token', user)
    expect(localStorage.getItem('mall-token')).toBe('signed-token')
    expect(JSON.parse(localStorage.getItem('mall_user') || '{}')).toEqual(user)

    store.logout()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('mall-token')).toBeNull()
  })

  it('drops malformed user data without crashing startup', () => {
    localStorage.setItem('mall-token', 'signed-token')
    localStorage.setItem('mall_user', '{broken')

    const store = useUserStore()

    expect(store.userInfo).toBeNull()
    expect(localStorage.getItem('mall_user')).toBeNull()
  })
})
