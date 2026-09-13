<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { Delete, ShoppingBag, Lightning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import ClientHeader from '../../components/ClientHeader.vue'
import ClientFooter from '../../components/ClientFooter.vue'

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

const checkedCount = computed(() => {
  return cartStore.items
    .filter(item => item.checked)
    .reduce((sum, item) => sum + item.count, 0)
})

const totalPrice = computed(() => {
  return cartStore.items
    .filter(item => item.checked)
    .reduce((sum, item) => sum + item.price * item.count, 0)
})

// 原价总和，计算优惠省下的钱
const totalSaved = computed(() => {
  let saved = 0
  cartStore.items.filter(item => item.checked).forEach(item => {
    const orig = (item as any).originalPrice || item.price
    if (orig > item.price) {
      saved += (orig - item.price) * item.count
    }
  })
  return saved
})

// 并发同步最新商品价格与库存
const syncLatestPricesAndStock = async () => {
  if (cartStore.items.length === 0) return
  priceLoading.value = true
  let hasChanges = false

  try {
    const updatePromises = cartStore.items.map(async (item) => {
      try {
        const detailPromise = request.get(`/product/detail/${item.id}`)
        const skuPromise = (item as any).skuId ? request.get(`/product/skus/${item.id}`) : Promise.resolve([])
        const [detail, skus]: [any, any] = await Promise.all([detailPromise, skuPromise])

        if (!detail || detail.price === undefined || detail.price === null) return
        let realPrice = Number(detail.price)
        if (isNaN(realPrice)) return

        let originalBasePrice = realPrice
        let realStock = detail.stock ?? 0
        let isFlashActive = false

        if (detail.promoStartTime && detail.promoEndTime && detail.promoPrice !== undefined && detail.promoPrice !== null) {
          const now = Date.now()
          const start = new Date(detail.promoStartTime.replace(/-/g, '/')).getTime()
          const end = new Date(detail.promoEndTime.replace(/-/g, '/')).getTime()
          if (now >= start && now <= end) {
            const promo = Number(detail.promoPrice)
            if (!isNaN(promo)) {
              realPrice = promo
              isFlashActive = true
            }
          }
        }

        if (!isFlashActive && (item as any).skuId && Array.isArray(skus)) {
          const targetSku = skus.find((s: any) => s.id === (item as any).skuId)
          if (targetSku && targetSku.price !== undefined && targetSku.price !== null) {
            const skuPrice = Number(targetSku.price)
            if (!isNaN(skuPrice)) {
              realPrice = skuPrice
              originalBasePrice = skuPrice
              realStock = targetSku.stock ?? 0
            }
          }
        }

        if (item.price !== realPrice || item.stock !== realStock || (item as any).isFlash !== isFlashActive) {
          item.price = realPrice
          item.stock = realStock
          ;(item as any).originalPrice = originalBasePrice
          ;(item as any).isFlash = isFlashActive

          if (item.count > realStock) {
            item.count = realStock > 0 ? realStock : 1
          }
          hasChanges = true
        }
      } catch (err) {
        console.warn(`同步商品 ID ${item.id} 失败`, err)
      }
    })

    await Promise.all(updatePromises)

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
  ElMessageBox.confirm('确定要把这件商品移出购物车吗？', '确认移出', {
    confirmButtonText: '确定移出',
    cancelButtonText: '暂不移出',
    type: 'warning'
  }).then(() => {
    cartStore.items.splice(index, 1)
    cartStore.saveCart()
    ElMessage.success('已移出购物车')
  })
}

const clearCart = () => {
  ElMessageBox.confirm('确定要清空购物车中的所有商品吗？', '警告', {
    confirmButtonText: '彻底清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cartStore.items = []
    cartStore.saveCart()
    ElMessage.success('购物车已清空')
  })
}

const handleCheckout = () => {
  const checkedItems = cartStore.items.filter(item => item.checked)
  if (checkedItems.length === 0) {
    ElMessage.warning('请至少勾选一件商品再结账哦~')
    return
  }
  router.push('/checkout')
}

onMounted(() => {
  syncLatestPricesAndStock()
})
</script>

<template>
  <div class="cart-page-layout">
    <!-- 统一头部 -->
    <ClientHeader />

    <main class="cart-container">
      <!-- 步骤进度指示条 -->
      <div class="checkout-steps-bar">
        <div class="step-node active">
          <div class="step-badge">1</div>
          <div class="step-text">我的购物车</div>
        </div>
        <div class="step-line"></div>
        <div class="step-node">
          <div class="step-badge">2</div>
          <div class="step-text">填写核对订单</div>
        </div>
        <div class="step-line"></div>
        <div class="step-node">
          <div class="step-badge">3</div>
          <div class="step-text">成功提交订单</div>
        </div>
      </div>

      <!-- 购物车主卡片 -->
      <div class="cart-card" v-loading="priceLoading" element-loading-text="正在为您同步最新活动价格与库存...">
        <!-- 空状态展示 -->
        <div v-if="cartStore.items.length === 0" class="empty-cart-view">
          <div class="empty-icon-wrap">
            <el-icon :size="72" color="#cbd5e1"><ShoppingBag /></el-icon>
          </div>
          <h2 class="empty-title">购物车空空如也</h2>
          <p class="empty-desc">赶紧去挑选心仪的自营优质好物，犒劳一下自己吧~</p>
          <el-button type="primary" size="large" round class="go-shopping-btn" @click="router.push('/')">
            去商城逛逛
          </el-button>
        </div>

        <!-- 购物车商品清单 -->
        <div v-else class="cart-table-wrapper">
          <!-- 顶部操作条 -->
          <div class="cart-table-header">
            <div class="col-chk">
              <el-checkbox v-model="isAllSelected" label="全选" />
            </div>
            <div class="col-info">商品信息</div>
            <div class="col-price">单价</div>
            <div class="col-qty">数量</div>
            <div class="col-sub">小计</div>
            <div class="col-op">操作</div>
          </div>

          <!-- 商品行列表 -->
          <div class="cart-items-list">
            <div 
              v-for="(row, idx) in cartStore.items" 
              :key="row.id + '-' + (row.skuId || 0)" 
              class="cart-item-row"
              :class="{ 'is-checked': row.checked }"
            >
              <!-- 勾选框 -->
              <div class="col-chk">
                <el-checkbox v-model="row.checked" @change="handleCheckChange" />
              </div>

              <!-- 商品信息 -->
              <div class="col-info product-media">
                <img :src="row.picUrl" class="item-thumb" @click="router.push(`/product/${row.id}`)" />
                <div class="item-text">
                  <span class="item-title" @click="router.push(`/product/${row.id}`)">{{ row.name }}</span>
                  <div class="item-tags">
                    <span class="item-spec" v-if="row.spec">规格：{{ row.spec }}</span>
                    <span class="tag-flash-pill" v-if="row.isFlash">
                      <el-icon><Lightning /></el-icon> 秒杀特惠
                    </span>
                  </div>
                </div>
              </div>

              <!-- 单价 -->
              <div class="col-price price-cell">
                <span class="real-price">¥{{ row.price }}</span>
                <span class="orig-price" v-if="row.isFlash && row.originalPrice">¥{{ row.originalPrice }}</span>
              </div>

              <!-- 数量步进器 -->
              <div class="col-qty qty-cell">
                <el-input-number 
                  v-model="row.count" 
                  :min="1" 
                  :max="row.stock > 0 ? row.stock : 1" 
                  :disabled="row.stock <= 0"
                  size="small"
                  @change="handleCountChange"
                />
                <span class="stock-warn" v-if="row.stock <= 3 && row.stock > 0">仅剩 {{ row.stock }} 件</span>
                <span class="stock-warn out" v-if="row.stock <= 0">暂无库存</span>
              </div>

              <!-- 小计 -->
              <div class="col-sub subtotal-cell">
                <span class="sub-num">¥{{ (row.price * row.count).toFixed(2) }}</span>
              </div>

              <!-- 操作 -->
              <div class="col-op op-cell">
                <el-button link type="danger" :icon="Delete" @click="handleRemove(idx)">
                  移除
                </el-button>
              </div>
            </div>
          </div>

          <!-- 底部吸底结算控制条 -->
          <div class="cart-summary-bar">
            <div class="summary-left">
              <el-checkbox v-model="isAllSelected" label="全选" />
              <el-button link type="info" @click="clearCart">清空购物车</el-button>
              <span class="checked-summary">
                已选中 <strong class="checked-num">{{ checkedCount }}</strong> 件商品
              </span>
              <span class="saved-summary" v-if="totalSaved > 0">
                已享活动立减：<strong class="saved-num">-¥{{ totalSaved.toFixed(2) }}</strong>
              </span>
            </div>

            <div class="summary-right">
              <div class="total-box">
                <span class="total-label">应付总额：</span>
                <span class="total-curr">¥</span>
                <span class="total-amount">{{ totalPrice.toFixed(2) }}</span>
              </div>
              <el-button 
                type="primary" 
                size="large" 
                class="checkout-btn" 
                :disabled="checkedCount === 0"
                @click="handleCheckout"
              >
                马上结账 ({{ checkedCount }})
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 统一页脚 -->
    <ClientFooter />
  </div>
</template>

<style scoped>
.cart-page-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.cart-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  padding: 30px 0 60px;
  flex: 1;
}

/* 步骤进度条 */
.checkout-steps-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  padding: 24px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  margin-bottom: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
}

.step-node {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
}

.step-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #f1f5f9;
  color: #64748b;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: bold;
  font-size: 13px;
}

.step-text {
  font-size: 14px;
  font-weight: bold;
}

.step-node.active {
  color: #0284c7;
}

.step-node.active .step-badge {
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
  color: #ffffff;
}

.step-line {
  width: 120px;
  height: 2px;
  background-color: #e2e8f0;
  margin: 0 20px;
}

/* 购物车主卡片 */
.cart-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  overflow: hidden;
}

/* 空状态 */
.empty-cart-view {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon-wrap {
  width: 110px;
  height: 110px;
  border-radius: 50%;
  background-color: #f8fafc;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto 20px;
}

.empty-title {
  margin: 0 0 10px 0;
  font-size: 22px;
  color: #0f172a;
}

.empty-desc {
  margin: 0 0 30px 0;
  font-size: 14px;
  color: #64748b;
}

.go-shopping-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-size: 15px;
  font-weight: bold;
  padding: 0 40px;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.3);
}

/* 购物车列表 */
.cart-table-header {
  display: flex;
  align-items: center;
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 16px 24px;
  font-size: 13px;
  color: #64748b;
  font-weight: bold;
}

.col-chk { width: 80px; }
.col-info { flex: 1; }
.col-price { width: 140px; text-align: center; }
.col-qty { width: 180px; text-align: center; }
.col-sub { width: 150px; text-align: center; }
.col-op { width: 100px; text-align: center; }

.cart-items-list {
  display: flex;
  flex-direction: column;
}

.cart-item-row {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
  transition: background-color 0.2s;
}

.cart-item-row:hover {
  background-color: #fcfdfe;
}

.cart-item-row.is-checked {
  background-color: #fffbfa;
}

.product-media {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-thumb {
  width: 76px;
  height: 76px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: transform 0.2s;
}

.item-thumb:hover {
  transform: scale(1.05);
}

.item-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-title {
  font-size: 14px;
  font-weight: bold;
  color: #1e293b;
  cursor: pointer;
  line-height: 1.4;
  transition: color 0.2s;
}

.item-title:hover {
  color: #f43f5e;
}

.item-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.item-spec {
  font-size: 12px;
  color: #64748b;
  background-color: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.tag-flash-pill {
  font-size: 11px;
  color: #e11d48;
  background-color: #ffe4e6;
  border: 1px solid #fecdd3;
  padding: 1px 6px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 2px;
  font-weight: bold;
}

.price-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.real-price {
  font-size: 15px;
  font-weight: bold;
  color: #1e293b;
}

.orig-price {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
}

.qty-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stock-warn {
  font-size: 11px;
  color: #f43f5e;
}

.stock-warn.out {
  color: #94a3b8;
}

.subtotal-cell {
  font-size: 18px;
  font-weight: 900;
  color: #f43f5e;
}

/* 底部结算汇总条 */
.cart-summary-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
  position: sticky;
  bottom: 0;
  z-index: 10;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.04);
}

.summary-left {
  display: flex;
  align-items: center;
  gap: 24px;
  font-size: 13px;
  color: #64748b;
}

.checked-num {
  color: #f43f5e;
  font-size: 15px;
}

.saved-summary {
  color: #0ea5e9;
  background: #f0f9ff;
  padding: 4px 10px;
  border-radius: 6px;
}

.saved-num {
  font-weight: bold;
}

.summary-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-box {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 13px;
  color: #64748b;
}

.total-curr {
  font-size: 18px;
  font-weight: bold;
  color: #f43f5e;
  margin-left: 4px;
}

.total-amount {
  font-size: 30px;
  font-weight: 900;
  color: #f43f5e;
  line-height: 1;
}

.checkout-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-size: 16px;
  font-weight: bold;
  height: 48px;
  padding: 0 36px;
  border-radius: 24px;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.3);
  transition: all 0.2s;
}

.checkout-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(2, 132, 199, 0.4);
}
</style>