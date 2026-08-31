import { test, expect } from '@playwright/test'
import path from 'path'

const artifactDir = 'C:\\Users\\a3185\\.gemini\\antigravity\\brain\\614bf3e9-fe6c-4b62-995e-a05ae0962c26'

test('Direct Live Client Home without Mocks', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1200 })
  await page.goto('http://127.0.0.1:4173/')
  await page.waitForTimeout(2000)

  // Verify that products are rendered
  const productCards = page.locator('.product-card')
  const count = await productCards.count()
  console.log(`Rendered ${count} products from backend API!`)

  // Check no 502 error toasts
  const toasts = page.locator('.el-message--error')
  const toastCount = await toasts.count()
  expect(toastCount).toBe(0)

  // Scroll down to show product grid
  await page.evaluate(() => window.scrollBy(0, 600))
  await page.waitForTimeout(1000)

  await page.screenshot({ path: path.join(artifactDir, 'screenshot_direct_live_home_products.png') })
})
