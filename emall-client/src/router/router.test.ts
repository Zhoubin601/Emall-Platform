import { describe, expect, it } from 'vitest'
import { resolveClientRedirect } from './index'

describe('client route guard', () => {
  it('redirects anonymous users away from protected routes', () => {
    expect(resolveClientRedirect('OrderList', true, '')).toBe('/login')
  })

  it('lets authenticated users enter protected routes', () => {
    expect(resolveClientRedirect('OrderList', true, 'token')).toBeUndefined()
  })

  it('redirects authenticated users away from login', () => {
    expect(resolveClientRedirect('Login', false, 'token')).toBe('/')
  })
})
