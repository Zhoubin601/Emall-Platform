import { test, expect } from '@playwright/test'
import path from 'path'

import os from 'os'
import fs from 'fs'

const artifactDir = process.env.ARTIFACT_DIR || path.join(os.tmpdir(), 'emall-artifacts')
if (!fs.existsSync(artifactDir)) {
  fs.mkdirSync(artifactDir, { recursive: true })
}

test('E-MALL Portal Layout & Pagination Verification', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto('/')
  await page.waitForTimeout(2000)

  // 1. Verify Topbar & Header Search
  await expect(page.locator('.emall-topbar')).toBeVisible()
  await expect(page.locator('.emall-brand-title')).toHaveText('E-MALL')
  await expect(page.locator('.emall-search-input')).toBeVisible()
  await expect(page.locator('.emall-cart-box')).toBeVisible()

  // Capture Hero Section
  await page.screenshot({ path: path.join(artifactDir, 'screenshot_emall_hero.png') })

  // 2. Scroll and verify Seckill section & Coupon center
  const seckillSection = page.locator('.emall-seckill-section')
  await seckillSection.scrollIntoViewIfNeeded()
  await page.waitForTimeout(1000)
  await expect(page.locator('.seckill-logo-text')).toHaveText('⚡ 限时秒杀')
  await page.screenshot({ path: path.join(artifactDir, 'screenshot_emall_seckill.png') })

  // 3. Scroll to Product Feed and verify Pagination
  const productSection = page.locator('#emall-product-section')
  await productSection.scrollIntoViewIfNeeded()
  await page.waitForTimeout(1000)
  
  const productCards = page.locator('.emall-product-card')
  const count = await productCards.count()
  console.log(`Page 1 shows ${count} products (matching pageSize 12)`)
  expect(count).toBeGreaterThan(0)
  expect(count).toBeLessThanOrEqual(12)

  // Capture Product Feed Page 1
  await page.screenshot({ path: path.join(artifactDir, 'screenshot_emall_products_page1.png') })

  // 4. Click pagination next page (Page 2)
  const page2Btn = page.locator('.el-pagination .el-pager li').filter({ hasText: '2' })
  if (await page2Btn.isVisible()) {
    await page2Btn.click()
    await page.waitForTimeout(1000)
    const countPage2 = await page.locator('.emall-product-card').count()
    console.log(`Page 2 shows ${countPage2} products`)
    expect(countPage2).toBeGreaterThan(0)
    await page.screenshot({ path: path.join(artifactDir, 'screenshot_emall_products_page2.png') })
  }

  // 5. Scroll to Footer
  const footer = page.locator('.emall-footer')
  await footer.scrollIntoViewIfNeeded()
  await page.waitForTimeout(1000)
  await expect(page.locator('.footer-guarantee-bar')).toBeVisible()
  await page.screenshot({ path: path.join(artifactDir, 'screenshot_emall_footer.png') })

  // 6. Capture full page
  await page.evaluate(() => window.scrollTo(0, 0))
  await page.waitForTimeout(1000)
  await page.screenshot({ path: path.join(artifactDir, 'screenshot_emall_fullpage.png'), fullPage: true })
})
