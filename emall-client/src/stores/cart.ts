import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

export interface CartItem {
  id: number
  skuId: number // ✨ 新增：SKU唯一标识，区分同一商品的不同规格
  name: string
  price: number
  picUrl?: string
  count: number 
  stock: number 
  spec: string 
  checked: boolean 
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>(JSON.parse(localStorage.getItem('mall_cart') || '[]'))

  const saveCart = () => {
    localStorage.setItem('mall_cart', JSON.stringify(items.value))
  }

  // ✨ 核心修改：接收 skuId
  const addToCart = (product: any, count: number = 1, spec: string = '默认规格', skuId: number = 0) => {
    // 查找购物车中是否已有 [同商品 + 同规格] 的记录
    const existItem = items.value.find(item => item.id === product.id && item.skuId === skuId)
    
    if (existItem) {
      if (existItem.count + count > existItem.stock) {
        ElMessage.warning('超出该规格最大库存限制！')
        return
      }
      existItem.count += count
    } else {
      items.value.push({
        id: product.id,
        skuId: skuId, // 记录 SKU
        name: product.name,
        price: product.price, // 这里的 price 已经是该规格的专属价格了
        picUrl: product.picUrl,
        stock: product.stock,
        count: count,
        spec: spec,
        checked: true 
      })
    }
    saveCart()
  }

  const totalCount = computed(() => {
    return items.value
      .filter(item => item.checked)
      .reduce((sum, item) => sum + item.count, 0)
  })

  return { items, addToCart, totalCount, saveCart }
})