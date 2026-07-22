import { describe, expect, it } from 'vitest'
import { resolveAdminRedirect } from './index'

describe('admin route guard', () => {
  it('redirects anonymous users to login', () => {
    expect(resolveAdminRedirect('/dashboard', '')).toBe('/login')
  })

  it('allows authenticated navigation', () => {
    expect(resolveAdminRedirect('/orders', 'token')).toBeUndefined()
  })

  it('redirects authenticated users away from login', () => {
    expect(resolveAdminRedirect('/login', 'token')).toBe('/')
  })

  it('redirects regular administrators away from user management', () => {
    expect(resolveAdminRedirect('/users', 'token', 1)).toBe('/dashboard')
  })

  it('allows super administrators to manage users', () => {
    expect(resolveAdminRedirect('/users', 'token', 2)).toBeUndefined()
  })
})
