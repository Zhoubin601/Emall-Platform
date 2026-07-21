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
})
