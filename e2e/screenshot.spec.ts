import { test } from '@playwright/test'
import path from 'path'

const artifactDir = 'C:\\Users\\a3185\\.gemini\\antigravity\\brain\\614bf3e9-fe6c-4b62-995e-a05ae0962c26'

test.describe('Visual Screenshots Test', () => {
  test('Capture Client Desktop, Mobile, Cart, and Order Screenshots', async ({ page }) => {
    // Mock Client APIs
    await page.route('**/api/**', async route => {
      const url = new URL(route.request().url())
      const pathname = url.pathname

      if (pathname === '/api/category/list') {
        return route.fulfill({
          json: [
            { id: 1, name: '📱 智能数码', parentId: 0, level: 1 },
            { id: 2, name: '👕 潮流服饰', parentId: 0, level: 1 },
            { id: 3, name: '🥤 美味零食', parentId: 0, level: 1 },
            { id: 4, name: '🏠 家居家电', parentId: 0, level: 1 }
          ]
        })
      }

      if (pathname === '/api/ad/active') {
        return route.fulfill({
          json: [
            {
              id: 1,
              title: '新品首发特惠',
              picUrl: 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=1200&q=80',
              linkUrl: '/'
            }
          ]
        })
      }

      if (pathname === '/api/coupon/list') {
        return route.fulfill({
          json: [
            { id: 1, name: '新人专享神券', minAmount: 100, discountAmount: 20, endTime: '2026-12-31' },
            { id: 2, name: '满减大促特权', minAmount: 300, discountAmount: 50, endTime: '2026-12-31' }
          ]
        })
      }

      if (pathname === '/api/product/hotSearches') {
        return route.fulfill({
          json: [
            { id: 1, keyword: 'iPhone 16 Pro', searchCount: 999 },
            { id: 2, keyword: '华为 Mate 70', searchCount: 888 },
            { id: 3, keyword: '降噪耳机', searchCount: 666 }
          ]
        })
      }

      if (pathname === '/api/product/list') {
        return route.fulfill({
          json: [
            {
              id: 101,
              name: 'iPhone 16 Pro Max 钛金属极光灰',
              description: '全新 A18 Pro 芯片，超视网膜 XDR 屏幕，全天候电池续航',
              price: 8999,
              promoPrice: 8499,
              promoStartTime: '2026-01-01 00:00:00',
              promoEndTime: '2026-12-31 23:59:59',
              stock: 50,
              sales: 320,
              picUrl: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80'
            },
            {
              id: 102,
              name: '极简降噪无线头戴式耳机',
              description: 'Hi-Res 高清音频认证，主动混合降噪，40小时长续航',
              price: 1299,
              stock: 120,
              sales: 580,
              picUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80'
            },
            {
              id: 103,
              name: '智能光电机械键盘 87键 RGB',
              description: '客制化线性轴体，全键无冲，PBT热升华键帽',
              price: 499,
              stock: 80,
              sales: 210,
              picUrl: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80'
            },
            {
              id: 104,
              name: '商务极简极速无线充电器',
              description: '15W 磁吸快充，低温不发烫，多重安全防护',
              price: 199,
              stock: 200,
              sales: 450,
              picUrl: 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80'
            }
          ]
        })
      }

      if (pathname === '/api/product/detail/101') {
        return route.fulfill({
          json: {
            id: 101,
            name: 'iPhone 16 Pro Max 钛金属极光灰',
            description: '全新 A18 Pro 芯片，超视网膜 XDR 屏幕',
            price: 8999,
            promoPrice: 8499,
            promoStartTime: '2026-01-01 00:00:00',
            promoEndTime: '2026-12-31 23:59:59',
            stock: 50,
            sales: 320,
            picUrl: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80'
          }
        })
      }

      if (pathname === '/api/product/detail/102') {
        return route.fulfill({
          json: {
            id: 102,
            name: '极简降噪无线头戴式耳机',
            description: 'Hi-Res 高清音频认证',
            price: 1299,
            stock: 120,
            sales: 580,
            picUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80'
          }
        })
      }

      if (pathname === '/api/product/skus/101') {
        return route.fulfill({
          json: [
            { id: 1, productId: 101, specName: '256GB / 极光灰', price: 8999, stock: 50 }
          ]
        })
      }

      if (pathname === '/api/product/skus/102') {
        return route.fulfill({
          json: [
            { id: 2, productId: 102, specName: '曜石黑 / 旗舰版', price: 1299, stock: 120 }
          ]
        })
      }

      if (pathname === '/api/order/my') {
        return route.fulfill({
          json: [
            {
              id: 1,
              orderSn: 'EMALL202608310001',
              totalAmount: '8499.00',
              status: 2,
              createTime: '2026-08-31 10:30:00'
            },
            {
              id: 2,
              orderSn: 'EMALL202608310002',
              totalAmount: '1299.00',
              status: 1,
              createTime: '2026-08-31 12:15:00'
            },
            {
              id: 3,
              orderSn: 'EMALL202608310003',
              totalAmount: '499.00',
              status: 3,
              commentStatus: 0,
              createTime: '2026-08-30 16:40:00'
            }
          ]
        })
      }

      return route.fulfill({ json: [] })
    })

    // Setup local login state
    await page.addInitScript(() => {
      localStorage.setItem('mall-token', 'client-mock-token')
      localStorage.setItem('mall_user', JSON.stringify({
        id: 7,
        username: 'vip_buyer',
        nickname: 'VIP星选官',
        avatar: '',
        role: 0
      }))
      localStorage.setItem('mall-user', JSON.stringify({
        id: 7,
        username: 'vip_buyer',
        nickname: 'VIP星选官',
        avatar: '',
        role: 0
      }))
      localStorage.setItem('mall_cart', JSON.stringify([
        {
          id: 101,
          skuId: 1,
          name: 'iPhone 16 Pro Max 钛金属极光灰',
          spec: '256GB / 极光灰',
          price: 8499,
          originalPrice: 8999,
          isFlash: true,
          count: 1,
          stock: 50,
          checked: true,
          picUrl: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80'
        },
        {
          id: 102,
          skuId: 2,
          name: '极简降噪无线头戴式耳机',
          spec: '曜石黑 / 旗舰版',
          price: 1299,
          originalPrice: 1299,
          isFlash: false,
          count: 1,
          stock: 120,
          checked: true,
          picUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80'
        }
      ]))
    })

    // 1. Desktop Home
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('http://127.0.0.1:4173/')
    await page.waitForTimeout(1000)
    await page.screenshot({ path: path.join(artifactDir, 'screenshot_client_desktop_home.png'), fullPage: false })

    // 2. Mobile Home
    await page.setViewportSize({ width: 375, height: 812 })
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(artifactDir, 'screenshot_client_mobile_home.png'), fullPage: false })

    // 3. Desktop Cart
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('http://127.0.0.1:4173/cart')
    await page.waitForTimeout(1000)
    await page.screenshot({ path: path.join(artifactDir, 'screenshot_client_cart.png'), fullPage: false })

    // 4. Order List
    await page.goto('http://127.0.0.1:4173/orders')
    await page.waitForTimeout(1000)
    await page.screenshot({ path: path.join(artifactDir, 'screenshot_client_orders.png'), fullPage: false })
  })

  test('Capture Admin Dashboard Screenshot with Mock Data', async ({ page }) => {
    await page.route('**/api/**', async route => {
      const url = new URL(route.request().url())
      const pathname = url.pathname

      if (pathname === '/api/dashboard/data') {
        return route.fulfill({
          json: {
            metrics: {
              totalUsers: 12800,
              totalOrders: 3450,
              totalSales: 588690.0,
              todaySales: 18450.0
            },
            trend: {
              dates: ['08-25', '08-26', '08-27', '08-28', '08-29', '08-30', '08-31'],
              sales: [42000, 58000, 61000, 78000, 89000, 112000, 148690]
            },
            statusData: [
              { name: '待支付', value: 120 },
              { name: '已支付/待发货', value: 450 },
              { name: '已发货/运输中', value: 780 },
              { name: '已完成', value: 2100 }
            ],
            rank: {
              names: ['iPhone 16 Pro', '降噪耳机', '机械键盘', '无线充电器', '智能手表'],
              sales: [980, 750, 620, 510, 430]
            }
          }
        })
      }

      return route.fulfill({ json: [] })
    })

    await page.addInitScript(() => {
      localStorage.setItem('admin-token', 'admin-mock-token')
      localStorage.setItem('admin-info', JSON.stringify({
        id: 1,
        username: 'superadmin',
        nickname: '系统超级管理员',
        avatar: '',
        role: 1
      }))
    })

    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('http://127.0.0.1:4174/dashboard')
    await page.waitForTimeout(2000)
    await page.screenshot({ path: path.join(artifactDir, 'screenshot_admin_dashboard.png'), fullPage: false })
  })
})
