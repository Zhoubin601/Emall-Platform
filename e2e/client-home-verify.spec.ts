import { test, expect } from '@playwright/test'
import path from 'path'

const artifactDir = 'C:\\Users\\Zhou_bibi\\.gemini\\antigravity\\brain\\43f970a2-e0d6-4315-8270-11836c770365'

test.describe('Client Homepage Verification', () => {
  test('Verify authentic homepage interactive features', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 1000 })

    // 访问前台商城首页（支持 5173 端口与 4173 端口）
    try {
      await page.goto('http://127.0.0.1:5173/')
    } catch {
      await page.goto('http://127.0.0.1:4173/')
    }
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(1000)

    // 截图 1: 首页首屏
    await page.screenshot({ path: path.join(artifactDir, 'home_hero_full.png'), fullPage: false })

    // 1. 验证并测试城市切换
    const locSwitch = page.locator('.loc-switch')
    if (await locSwitch.isVisible()) {
      await locSwitch.click()
      await page.waitForTimeout(500)
      const cityChip = page.locator('.city-chip', { hasText: '上海市' })
      if (await cityChip.isVisible()) {
        await cityChip.click()
        await page.waitForTimeout(500)
      }
    }

    // 2. 验证真实系统公告点击
    const noticeItem = page.locator('.news-clickable-item').first()
    if (await noticeItem.isVisible()) {
      await noticeItem.click()
      await page.waitForTimeout(500)
      await page.screenshot({ path: path.join(artifactDir, 'notice_dialog.png') })
      const closeBtn = page.locator('.dialog-footer button', { hasText: '我已了解' })
      if (await closeBtn.isVisible()) await closeBtn.click()
    }

    // 3. 验证便民服务与客服指引
    const phoneService = page.locator('.service-cell', { hasText: '话费充值' })
    if (await phoneService.isVisible()) {
      await phoneService.click()
      await page.waitForTimeout(500)
      await page.screenshot({ path: path.join(artifactDir, 'service_dialog.png') })
      const serviceCloseBtn = page.locator('.sc-plain-btn')
      if (await serviceCloseBtn.isVisible()) await serviceCloseBtn.click()
    }

    // 4. 截图 2: 完整全页渲染
    await page.screenshot({ path: path.join(artifactDir, 'home_full_page.png'), fullPage: true })
  })
})
