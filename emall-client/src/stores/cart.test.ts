import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCartStore } from './cart'

const { warning } = vi.hoisted(() => ({ warning: vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage: { warning } }))

const product = {
  id: 10,
  name: '测试商品',
  price: 99,
  picUrl: '/uploads/common/product.png',
  stock: 3
}

describe('cart store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('merges the same SKU and persists the cart', () => {
    const store = useCartStore()

    store.addToCart(product, 1, '黑色', 101)
    store.addToCart(product, 2, '黑色', 101)

    expect(store.items).toHaveLength(1)
    expect(store.items[0].count).toBe(3)
    expect(store.totalCount).toBe(3)
    expect(JSON.parse(localStorage.getItem('mall_cart') || '[]')).toEqual(store.items)
  })

  it('keeps different SKUs as separate cart lines', () => {
    const store = useCartStore()

    store.addToCart(product, 1, '黑色', 101)
    store.addToCart(product, 1, '白色', 102)

    expect(store.items.map(item => item.skuId)).toEqual([101, 102])
  })

  it('rejects an initial quantity above stock', () => {
    const store = useCartStore()

    store.addToCart(product, 4, '黑色', 101)

    expect(store.items).toHaveLength(0)
    expect(warning).toHaveBeenCalledOnce()
  })

  it('recovers from malformed persisted data', () => {
    localStorage.setItem('mall_cart', '{broken')
    setActivePinia(createPinia())

    expect(useCartStore().items).toEqual([])
    expect(localStorage.getItem('mall_cart')).toBeNull()
  })
})
