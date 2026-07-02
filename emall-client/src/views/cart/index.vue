<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { Back, Delete, ShoppingBag, Lightning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const router = useRouter()
const cartStore = useCartStore()

const priceLoading = ref(false)

const isAllSelected = computed({
  get: () => cartStore.items.length > 0 && cartStore.items.every(item => item.checked),
  set: (val) => {
    cartStore.items.forEach(item => item.checked = val)
    cartStore.saveCart()
  }
})

const totalPrice = computed(() => {
  return cartStore.items
    .filter(item => item.checked)
    .reduce((sum, item) => sum + item.price * item.count, 0)
})

// ✨ 核心重构：购物车每次打开时，强制向后端同步最新的价格、库存和秒杀状态
const syncLatestPricesAndStock = async () => {
  if (cartStore.items.length === 0) return
  priceLoading.value = true
  let hasChanges = false

  try {
    for (let item of cartStore.items) {
      const detail: any = await request.get(`/product/detail/${item.id}`)
      let realPrice = detail.price
      let originalBasePrice = detail.price
      let realStock = detail.stock
      let isFlashActive = false
      
      // 1. 检查是否正在秒杀活动中
      if (detail.promoStartTime && detail.promoEndTime && detail.promoPrice) {
        const now = new Date().getTime()
        const start = new Date(detail.promoStartTime.replace(/-/g, '/')).getTime()
        const end = new Date(detail.promoEndTime.replace(/-/g, '/')).getTime()
        if (now >= start && now <= end) {
          realPrice = detail.promoPrice
          isFlashActive = true
        }
      }
      
      // 2. 如果不在秒杀中，则检查 SKU 最新规格价格
      if (!isFlashActive && (item as any).skuId) {
        const skus: any[] = await request.get(`/product/skus/${item.id}`)
        const targetSku = skus.find(s => s.id === (item as any).skuId)
        if (targetSku) {
          realPrice = targetSku.price
          originalBasePrice = targetSku.price
          realStock = targetSku.stock
        }
      }

      // 3. 对比缓存，如果价格、库存、秒杀状态发生变化，自动覆写缓存！
      if (item.price !== realPrice || item.stock !== realStock || (item as any).isFlash !== isFlashActive) {
        item.price = realPrice
        item.stock = realStock
        ;(item as any).originalPrice = originalBasePrice
        ;(item as any).isFlash = isFlashActive
        
        // 防呆：如果最新库存小于用户加入购物车时的数量，自动把数量降下来
        if (item.count > realStock) {
          item.count = realStock > 0 ? realStock : 1
        }
        hasChanges = true
      }
    }

    // 如果发现数据有变动，立刻保存回 localStorage
    if (hasChanges) {
      cartStore.saveCart()
    }
  } catch (error) {
    console.error('同步购物车最新价格与库存失败', error)
  } finally {
    priceLoading.value = false
  }
}

const handleCountChange = () => {
  cartStore.saveCart()
}

const handleCheckChange = () => {
  cartStore.saveCart()
}

const handleRemove = (index: number) => {
  ElMessageBox.confirm('确定要把这件宝贝移出购物车吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cartStore.items.splice(index, 1)
    cartStore.saveCart()
    ElMessage.success('移除成功')
  })
}

const handleCheckout = () => {
  const checkedItems = cartStore.items.filter(item => item.checked)
  if (checkedItems.length === 0) {
    ElMessage.warning('请至少勾选一件商品再结算哦~')
    return
  }
  router.push('/checkout')
}

onMounted(() => {
  syncLatestPricesAndStock() // ✨ 页面挂载时静默刷新数据
})
</script>

<template>
  <div class="cart-layout">
    <header class="glass-header">
      <div class="header-content">
        <el-button link :icon="Back" @click="router.push('/')" class="back-btn">继续闲逛</el-button>
        <span class="brand-text">我的购物车</span>
      </div>
    </header>

    <main class="main-container">
      <el-card class="glass-card" shadow="never" v-loading="priceLoading" element-loading-text="正在为您同步最新活动价格...">
        
        <div v-if="cartStore.items.length === 0" class="empty-state">
          <el-icon class="empty-icon"><ShoppingBag /></el-icon>
          <h2>购物车竟然是空的</h2>
          <p>再忙，也要记得买点什么犒劳自己~</p>
          <el-button type="primary" round size="large" class="cute-btn" @click="router.push('/')">
            去商城逛逛
          </el-button>
        </div>

        <div v-else>
          <el-table :data="cartStore.items" style="width: 100%" class="custom-table">
            <el-table-column width="55" align="center">
              <template #header>
                <el-checkbox v-model="isAllSelected" />
              </template>
              <template #default="{ row }">
                <el-checkbox v-model="row.checked" @change="handleCheckChange" />
              </template>
            </el-table-column>

            <el-table-column label="商品信息" min-width="250">
              <template #default="{ row }">
                <div class="product-info-cell">
                  <img :src="row.picUrl" class="cell-img" />
                  <div class="info-text">
                    <span class="cell-name">{{ row.name }}</span>
                    <div style="display: flex; gap: 8px; align-items: center; margin-top: 2px;">
                      <span class="cell-spec" v-if="row.spec">{{ row.spec }}</span>
                      <el-tag v-if="row.isFlash" type="danger" effect="dark" size="small" style="height: 20px; padding: 0 5px; border-radius: 4px;">
                        <el-icon style="margin-right: 2px;"><Lightning /></el-icon>秒杀中
                      </el-tag>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column label="单价" width="120" align="center">
              <template #default="{ row }">
                <div style="display: flex; flex-direction: column; align-items: center; justify-content: center;">
                  <span class="cell-price" :class="{ 'flash-price': row.isFlash }">¥ {{ row.price }}</span>
                  <span class="cell-old-price" v-if="row.isFlash">¥{{ row.originalPrice }}</span>
                </div>
              </template>
            </el-table-column>
            
            <el-table-column label="数量" width="180" align="center">
              <template #default="{ row }">
                <el-input-number 
                  v-model="row.count" 
                  :min="1" 
                  :max="row.stock > 0 ? row.stock : 1" 
                  :disabled="row.stock <= 0"
                  size="small"
                  @change="handleCountChange"
                />
              </template>
            </el-table-column>
            
            <el-table-column label="小计" width="120" align="center">
              <template #default="{ row }">
                <span class="cell-subtotal">¥ {{ (row.price * row.count).toFixed(2) }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link :icon="Delete" @click="handleRemove($index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="checkout-bar">
            <div class="total-info">
              <el-checkbox v-model="isAllSelected" label="全选" size="large" />
              <span>已选 <strong style="color: #0284c7;">{{ cartStore.totalCount }}</strong> 件</span>
              <div class="total-price-box">
                合计：<span class="total-currency">¥</span><span class="total-price">{{ totalPrice.toFixed(2) }}</span>
              </div>
            </div>
            <el-button type="primary" size="large" round class="checkout-btn" @click="handleCheckout">
              马上结账
            </el-button>
          </div>
        </div>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.cart-layout { min-height: 100vh; background-color: #f0f8ff; padding-top: 100px; padding-bottom: 50px; }

/* 导航栏毛玻璃 */
.glass-header { position: fixed; top: 0; left: 0; right: 0; height: 70px; background: rgba(255, 255, 255, 0.75); backdrop-filter: blur(16px); border-bottom: 1px solid rgba(186, 230, 253, 0.4); z-index: 1000; display: flex; justify-content: center; }
.header-content { width: 1200px; display: flex; align-items: center; padding: 0 20px; }
.back-btn { font-size: 15px; color: #0284c7; margin-right: 20px; font-weight: bold; }
.brand-text { font-size: 20px; font-weight: 900; color: #0ea5e9; border-left: 2px solid #bae6fd; padding-left: 20px; }
.info-text { display: flex; flex-direction: column; gap: 4px; justify-content: center; }
.cell-spec { font-size: 12px; color: #94a3b8; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; width: fit-content; }
.main-container { width: 1000px; max-width: 95%; margin: 0 auto; }

/* 核心卡片毛玻璃 */
.glass-card { border-radius: 24px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(10px); box-shadow: 0 10px 30px rgba(125, 211, 252, 0.15); padding: 20px; }

.empty-state { text-align: center; padding: 80px 0; }
.empty-icon { font-size: 80px; color: #bae6fd; margin-bottom: 20px; }
.empty-state h2 { color: #0f172a; margin-bottom: 10px; }
.empty-state p { color: #64748b; margin-bottom: 30px; }
.cute-btn { background-color: #38bdf8; border: none; font-weight: bold; padding: 0 30px; }
.cute-btn:hover { background-color: #0ea5e9; }

/* 表格定制 */
:deep(.el-table th.el-table__cell) { background-color: #f0f9ff !important; color: #0369a1; border-bottom: none; }
:deep(.el-table td.el-table__cell) { border-bottom: 1px dashed #e2e8f0; }
:deep(.el-table::before) { display: none; }

.product-info-cell { display: flex; align-items: center; gap: 15px; }
.cell-img { width: 60px; height: 60px; border-radius: 12px; object-fit: cover; }
.cell-img-placeholder { width: 60px; height: 60px; border-radius: 12px; background: #f1f5f9; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #94a3b8; }

.cell-name { font-weight: bold; color: #1e293b; }

/* ✨ 价格样式优化 */
.cell-price { color: #64748b; transition: color 0.3s; }
.flash-price { color: #e11d48; font-weight: 900; }
.cell-old-price { font-size: 12px; color: #94a3b8; text-decoration: line-through; margin-top: 2px; }

.cell-subtotal { color: #f43f5e; font-weight: bold; font-size: 16px; }

/* 底部结算栏 */
.checkout-bar { display: flex; justify-content: space-between; align-items: center; margin-top: 30px; padding: 20px 30px; background: linear-gradient(135deg, #f0f9ff, #e0f2fe); border-radius: 20px; }
.total-info { display: flex; align-items: center; gap: 30px; color: #475569; }
.total-price-box { display: flex; align-items: baseline; color: #0f172a; font-weight: bold; }
.total-currency { color: #f43f5e; font-size: 20px; margin-left: 10px; margin-right: 2px; }
.total-price { color: #f43f5e; font-size: 32px; }
.checkout-btn { background: #0284c7; border: none; font-size: 16px; padding: 0 40px; }
.checkout-btn:hover { background: #0369a1; transform: translateY(-2px); box-shadow: 0 5px 15px rgba(2, 132, 199, 0.3); transition: all 0.3s; }
</style>