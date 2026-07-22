import { expect, test } from '@playwright/test'

test('未登录访问订单页时跳转到登录页', async ({ page }) => {
  await page.goto('/orders')

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: 'E-MALL' })).toBeVisible()
})

test('买家可以通过登录页进入商城', async ({ page }) => {
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname

    if (path === '/api/user/login') {
      await route.fulfill({
        json: {
          token: 'client-test-token',
          user: {
            id: 7,
            username: 'buyer',
            nickname: '测试买家',
            role: 0,
            status: 1
          }
        }
      })
      return
    }

    await route.fulfill({ json: [] })
  })

  await page.goto('/login')
  await page.getByPlaceholder('请输入登录账号').fill('buyer')
  await page.getByPlaceholder('请输入登录密码').fill('password123')
  await page.getByRole('button', { name: '开启购物之旅' }).click()

  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByPlaceholder('搜一搜今天买点什么...')).toBeVisible()
  await expect.poll(() => page.evaluate(() => localStorage.getItem('mall-token')))
    .toBe('client-test-token')
})

test('反馈和支付表单不使用已弃用的单选框 API', async ({ page }) => {
  const radioWarnings: string[] = []
  const runtimeErrors: string[] = []
  page.on('pageerror', error => runtimeErrors.push(error.message))
  page.on('console', message => {
    if (message.type() === 'warning' && message.text().includes('[el-radio]')) {
      radioWarnings.push(message.text())
    }
  })

  await page.addInitScript(() => {
    localStorage.setItem('mall-token', 'client-test-token')
    localStorage.setItem('mall_user', JSON.stringify({ id: 7, username: 'buyer', nickname: '测试买家', role: 0 }))
    localStorage.setItem('mall_cart', JSON.stringify([
      { id: 1, skuId: 10, name: '测试商品', price: 99, count: 1, stock: 5, spec: '默认规格', checked: true }
    ]))
  })
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    if (path === '/api/product/detail/1') {
      await route.fulfill({ json: { id: 1, price: 99 } })
    } else if (path === '/api/product/skus/1') {
      await route.fulfill({ json: [{ id: 10, price: 99 }] })
    } else if (path === '/api/address/list') {
      await route.fulfill({
        json: [{ id: 3, receiverName: '测试买家', receiverPhone: '13800000000', province: '辽宁省', city: '沈阳市', region: '浑南区', detailAddress: '测试地址', isDefault: 1 }]
      })
    } else if (path === '/api/order/create') {
      await route.fulfill({ json: { orderId: 1001, totalAmount: 109 } })
    } else {
      await route.fulfill({ json: [] })
    }
  })

  await page.goto('/profile')
  await page.waitForLoadState('networkidle')
  expect(runtimeErrors).toEqual([])
  await expect(page.getByRole('radio', { name: '功能建议' })).toBeVisible()

  await page.goto('/checkout')
  await page.getByRole('button', { name: '提交订单' }).click()
  await expect(page.getByRole('dialog', { name: 'E-MALL 专属收银台' })).toBeVisible()
  await expect(page.getByRole('radio', { name: '支付宝支付' })).toBeVisible()

  expect(radioWarnings).toEqual([])
})
