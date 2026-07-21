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
