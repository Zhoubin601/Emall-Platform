import { expect, test } from '@playwright/test'

const dashboardData = {
  metrics: {
    totalUsers: 12,
    totalOrders: 34,
    totalSales: 5678,
    todaySales: 99
  },
  trend: {
    dates: ['07-16', '07-17', '07-18', '07-19', '07-20', '07-21', '07-22'],
    sales: [10, 20, 30, 25, 40, 60, 80]
  },
  statusData: [
    { name: '待付款', value: 2 },
    { name: '待发货', value: 5 },
    { name: '已完成', value: 27 }
  ],
  rank: {
    names: ['测试商品'],
    sales: [10]
  }
}

test('未登录访问看板时跳转到登录页', async ({ page }) => {
  await page.goto('/dashboard')

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: 'E-MALL 管理后台' })).toBeVisible()
})

test('管理员可以登录并看到看板数据', async ({ page }) => {
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname

    if (path === '/api/user/adminLogin') {
      await route.fulfill({
        json: {
          token: 'admin-test-token',
          user: {
            id: 1,
            username: 'admin',
            nickname: '测试管理员',
            role: 1,
            status: 1
          }
        }
      })
      return
    }

    if (path === '/api/dashboard/data') {
      await route.fulfill({ json: dashboardData })
      return
    }

    await route.fulfill({ json: [] })
  })

  await page.goto('/login')
  await page.getByPlaceholder('管理员账号').fill('admin')
  await page.getByPlaceholder('管理员密码').fill('password123')
  await page.getByRole('button', { name: '安全登录' }).click()

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByText('总买家数')).toBeVisible()
  await expect(page.locator('.metric-info .value').first()).toHaveText('12')
  await expect.poll(() => page.evaluate(() => localStorage.getItem('admin-token')))
    .toBe('admin-test-token')
})

test('用户和分类表单不使用已弃用的单选框 API', async ({ page }) => {
  const radioWarnings: string[] = []
  page.on('console', message => {
    if (message.type() === 'warning' && message.text().includes('[el-radio]')) {
      radioWarnings.push(message.text())
    }
  })

  await page.addInitScript(() => {
    localStorage.setItem('admin-token', 'admin-test-token')
    localStorage.setItem('admin-info', JSON.stringify({ id: 1, username: 'admin', role: 2 }))
  })
  await page.route('**/api/**', route => route.fulfill({ json: [] }))

  await page.goto('/users')
  await page.getByRole('button', { name: '新增用户 / 管理员' }).click()
  await expect(page.getByRole('radio', { name: '普通买家' })).toBeVisible()

  await page.goto('/categories')
  await page.getByRole('button', { name: '新增分类' }).click()
  await expect(page.getByRole('radio', { name: '一级大类' })).toBeVisible()

  expect(radioWarnings).toEqual([])
})

test('普通管理员不能直接打开用户管理页', async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('admin-token', 'regular-admin-token')
    localStorage.setItem('admin-info', JSON.stringify({ id: 2, username: 'operator', role: 1 }))
  })
  await page.route('**/api/**', route => route.fulfill({ json: [] }))

  await page.goto('/users')

  await expect(page).toHaveURL(/\/dashboard$/)
})
