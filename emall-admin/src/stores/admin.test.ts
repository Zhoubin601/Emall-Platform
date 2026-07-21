import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAdminStore } from './admin'

describe('admin store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('persists and clears an admin session', () => {
    const store = useAdminStore()
    const admin = { id: 1, username: 'admin', role: 1 }

    store.setAdminLogin('admin-token', admin)
    expect(localStorage.getItem('admin-token')).toBe('admin-token')
    expect(JSON.parse(localStorage.getItem('admin-info') || '{}')).toEqual(admin)

    store.clearToken()
    expect(store.token).toBe('')
    expect(store.adminInfo).toEqual({})
  })

  it('recovers from malformed persisted profile data', () => {
    localStorage.setItem('admin-info', '{broken')

    expect(useAdminStore().adminInfo).toEqual({})
    expect(localStorage.getItem('admin-info')).toBeNull()
  })
})
