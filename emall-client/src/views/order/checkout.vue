<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Location, Plus, Check, Ticket, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'
import ClientHeader from '../../components/ClientHeader.vue'
import ClientFooter from '../../components/ClientFooter.vue'

import wechatQr from './wechat.png'
import alipayQr from './alipay.png'

const router = useRouter()
const route = useRoute() 
const cartStore = useCartStore()
const userStore = useUserStore()

const isBuyNow = computed(() => route.query.type === 'buyNow')

const checkedItems = ref<any[]>([])
const priceLoading = ref(true)

const initItems = () => {
  if (isBuyNow.value) {
    const itemStr = sessionStorage.getItem('buy_now_item')
    checkedItems.value = itemStr ? [JSON.parse(itemStr)] : []
  } else {
    checkedItems.value = JSON.parse(JSON.stringify(cartStore.items.filter(item => item.checked)))
  }
}

const syncLatestPrices = async () => {
  if (checkedItems.value.length === 0) {
    priceLoading.value = false
    return
  }
  priceLoading.value = true
  try {
    const syncTasks = checkedItems.value.map(async (item) => {
      try {
        const detailPromise = request.get(`/product/detail/${item.id}`)
        const skuPromise = item.skuId ? request.get(`/product/skus/${item.id}`) : Promise.resolve([])
        const [detail, skus]: [any, any] = await Promise.all([detailPromise, skuPromise])

        if (!detail || detail.price === undefined || detail.price === null) return
        let realPrice = Number(detail.price)
        if (isNaN(realPrice)) return

        let originalBasePrice = realPrice
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

        if (!isFlashActive && item.skuId && Array.isArray(skus)) {
          const targetSku = skus.find((s: any) => s.id === item.skuId)
          if (targetSku && targetSku.price !== undefined && targetSku.price !== null) {
            const skuPrice = Number(targetSku.price)
            if (!isNaN(skuPrice)) {
              realPrice = skuPrice
              originalBasePrice = skuPrice
            }
          }
        }

        item.price = realPrice
        item.isFlash = isFlashActive
        item.originalPrice = originalBasePrice
      } catch (err) {
        console.warn(`同步商品 ID ${item.id} 价格失败`, err)
      }
    })
    await Promise.all(syncTasks)
  } catch (e) {
    console.error('同步最新价格失败', e)
  } finally {
    priceLoading.value = false
  }
}

const addressList = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)

interface Coupon {
  userCouponId: number
  couponId: number
  name: string
  minAmount: number
  discountAmount: number
  endTime: string
}
const myCoupons = ref<Coupon[]>([])
const selectedUserCouponId = ref<number | null>(null)

const fetchRealAddress = async () => {
  if (!userStore.userInfo) return
  try {
    const res: any = await request.get('/address/list', { params: { userId: userStore.userInfo.id } })
    addressList.value = res
    if (addressList.value.length > 0) {
      const defaultAddr = addressList.value.find(a => a.isDefault)
      selectedAddressId.value = defaultAddr ? defaultAddr.id : addressList.value[0].id
    }
  } catch (error) {
    console.error('获取收货地址失败')
  }
}

const fetchMyCoupons = async () => {
  if (!userStore.userInfo) return
  try {
    const res: any = await request.get('/coupon/myUsable', { params: { userId: userStore.userInfo.id } })
    myCoupons.value = res
  } catch (error) {
    console.error('获取可用优惠券失败')
  }
}

const selectedAddress = computed(() => {
  return addressList.value.find(item => item.id === selectedAddressId.value)
})

const itemsAmount = computed(() => {
  return checkedItems.value.reduce((sum, item) => sum + item.price * item.count, 0)
})

const availableCoupons = computed(() => {
  return myCoupons.value.filter(c => itemsAmount.value >= c.minAmount)
})

const selectedCoupon = computed(() => {
  return myCoupons.value.find(c => c.userCouponId === selectedUserCouponId.value)
})

const couponDiscount = computed(() => {
  if (!selectedCoupon.value) return 0
  return selectedCoupon.value.discountAmount
})

const shippingFee = computed(() => {
  return itemsAmount.value >= 99 ? 0 : 10
})

const finalAmount = computed(() => {
  const result = itemsAmount.value + shippingFee.value - couponDiscount.value
  return result > 0 ? result : 0
})

const isSubmitting = ref(false)
const showPayDialog = ref(false)
const currentOrderId = ref<number | null>(null)
const serverTotalAmount = ref<number | null>(null)
const idempotencyKey = ref(crypto.randomUUID())
const payMethod = ref('alipay')

const handlePlaceOrder = async () => {
  if (!userStore.userInfo) return router.push('/login')
  if (!selectedAddress.value) return ElMessage.warning('请选择收货地址')
  if (checkedItems.value.length === 0) return ElMessage.warning('没有结算商品')

  isSubmitting.value = true
  try {
    const orderData = {
      userCouponId: selectedUserCouponId.value, 
      items: checkedItems.value.map(item => ({
        skuId: item.skuId,
        productCount: item.count
      }))
    }

    const result: any = await request.post('/order/create', orderData, {
      headers: { 'Idempotency-Key': idempotencyKey.value }
    })
    currentOrderId.value = Number(result.orderId)
    serverTotalAmount.value = Number(result.totalAmount)
    idempotencyKey.value = crypto.randomUUID()
    
    if (!isBuyNow.value) {
      cartStore.items = cartStore.items.filter(item => !item.checked)
      localStorage.setItem('mall_cart', JSON.stringify(cartStore.items))
    } else {
      sessionStorage.removeItem('buy_now_item')
    }
    showPayDialog.value = true
  } catch (error: any) {
    ElMessage.error(error.response?.data || '下单失败')
  } finally {
    isSubmitting.value = false
  }
}

const handlePaid = async () => {
  if (!currentOrderId.value) return
  try {
    await request.put(`/order/status/${currentOrderId.value}/1`)
    ElMessage.success('🎉 支付成功，顺丰仓配已开始为您打包拣货！')
    showPayDialog.value = false
    router.push('/orders')
  } catch (error) { 
    ElMessage.error('支付状态同步异常，请检查网络') 
  }
}

const handleUnpaid = () => {
  ElMessage.warning('订单已生成，请在 30 分钟内完成支付哦')
  showPayDialog.value = false
  router.push('/orders')
}

onMounted(async () => {
  initItems()
  if (checkedItems.value.length === 0) {
    ElMessage.warning('请先选择需要结算的商品')
    router.push('/cart')
    return
  }
  fetchRealAddress() 
  fetchMyCoupons()
  await syncLatestPrices() 
})
</script>

<template>
  <div class="checkout-page-layout">
    <ClientHeader />

    <main class="checkout-container" v-loading="priceLoading" element-loading-text="正在为您核对最新商品价格与优惠抵扣...">
      <!-- 步骤进度指示条 -->
      <div class="checkout-steps-bar">
        <div class="step-node done">
          <div class="step-badge"><el-icon><Check /></el-icon></div>
          <div class="step-text">1. 我的购物车</div>
        </div>
        <div class="step-line active"></div>
        <div class="step-node active">
          <div class="step-badge">2</div>
          <div class="step-text">2. 填写核对订单</div>
        </div>
        <div class="step-line"></div>
        <div class="step-node">
          <div class="step-badge">3</div>
          <div class="step-text">3. 成功提交订单</div>
        </div>
      </div>

      <div class="checkout-main-grid">
        <!-- 左侧：主要信息输入栏 -->
        <div class="checkout-content-left">
          <!-- 1. 收货人地址选择 -->
          <div class="panel-card address-panel">
            <div class="panel-header">
              <div class="ph-title">
                <el-icon color="#f43f5e"><Location /></el-icon>
                <span>收货地址</span>
              </div>
              <el-button type="primary" link :icon="Plus" @click="router.push('/address')">
                管理 / 新增收货地址
              </el-button>
            </div>

            <div class="address-cards-grid">
              <div 
                v-for="addr in addressList" 
                :key="addr.id" 
                class="address-box-card"
                :class="{ active: selectedAddressId === addr.id }"
                @click="selectedAddressId = addr.id"
              >
                <div class="addr-top">
                  <span class="receiver-name">{{ addr.receiverName }}</span>
                  <span class="receiver-phone">{{ addr.receiverPhone }}</span>
                  <span class="badge-default" v-if="addr.isDefault">默认</span>
                </div>
                <div class="addr-detail">
                  {{ addr.province }} {{ addr.city }} {{ addr.region || '' }} {{ addr.detailAddress }}
                </div>
                <el-icon v-if="selectedAddressId === addr.id" class="addr-checked"><Check /></el-icon>
              </div>

              <div class="address-box-card add-new-card" @click="router.push('/address')">
                <el-icon :size="24" color="#0ea5e9"><Plus /></el-icon>
                <span>使用新收货地址</span>
              </div>
            </div>
          </div>

          <!-- 2. 商品清单核对 -->
          <div class="panel-card products-panel">
            <div class="panel-header">
              <div class="ph-title">
                <span>商品清单</span>
                <span class="count-hint">共 {{ checkedItems.reduce((s, i) => s + i.count, 0) }} 件</span>
              </div>
              <el-button link type="info" @click="router.push('/cart')">返回购物车修改</el-button>
            </div>

            <div class="products-list-view">
              <div v-for="item in checkedItems" :key="item.id + '-' + item.skuId" class="order-product-row">
                <img :src="item.picUrl" class="p-thumb" />
                <div class="p-detail">
                  <span class="p-name">{{ item.name }}</span>
                  <div class="p-tags">
                    <span class="p-spec" v-if="item.spec">规格：{{ item.spec }}</span>
                    <span class="p-flash" v-if="item.isFlash">⚡ 限时特惠</span>
                  </div>
                </div>
                <div class="p-price-qty">
                  <span class="p-price">¥{{ item.price }}</span>
                  <span class="p-qty">x {{ item.count }}</span>
                </div>
                <div class="p-subtotal">
                  <span>¥{{ (item.price * item.count).toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 3. 优惠券选择 -->
          <div class="panel-card coupon-panel">
            <div class="panel-header">
              <div class="ph-title">
                <el-icon color="#f43f5e"><Ticket /></el-icon>
                <span>专属神券抵扣</span>
              </div>
              <span class="coupon-avail-hint" v-if="availableCoupons.length > 0">
                有 {{ availableCoupons.length }} 张可用优惠券
              </span>
              <span class="coupon-avail-hint" v-else>暂无满足门槛的可用券</span>
            </div>

            <div class="coupon-cards-grid" v-if="availableCoupons.length > 0">
              <div 
                class="coupon-item-box"
                :class="{ active: selectedUserCouponId === null }"
                @click="selectedUserCouponId = null"
              >
                <div class="cp-discount">不使用</div>
                <div class="cp-info">
                  <div class="cp-name">不使用任何优惠券</div>
                </div>
              </div>

              <div 
                v-for="c in availableCoupons" 
                :key="c.userCouponId" 
                class="coupon-item-box has-val"
                :class="{ active: selectedUserCouponId === c.userCouponId }"
                @click="selectedUserCouponId = c.userCouponId"
              >
                <div class="cp-discount">
                  <span class="cp-curr">¥</span>
                  <span class="cp-num">{{ c.discountAmount }}</span>
                </div>
                <div class="cp-info">
                  <div class="cp-name">{{ c.name }}</div>
                  <div class="cp-condition">满 ¥{{ c.minAmount }} 可用</div>
                </div>
                <el-icon v-if="selectedUserCouponId === c.userCouponId" class="cp-checked"><Check /></el-icon>
              </div>
            </div>
          </div>

          <!-- 4. 支付方式选择 -->
          <div class="panel-card payment-panel">
            <div class="panel-header">
              <div class="ph-title">
                <el-icon color="#0ea5e9"><Wallet /></el-icon>
                <span>支付方式</span>
              </div>
            </div>

            <div class="pay-methods-grid">
              <div 
                class="pay-method-card"
                :class="{ active: payMethod === 'alipay' }"
                @click="payMethod = 'alipay'"
              >
                <div class="pm-brand alipay">支</div>
                <div class="pm-text">
                  <div class="pm-name">支付宝支付</div>
                  <div class="pm-sub">推荐使用支付宝扫码或免密支付</div>
                </div>
                <el-icon v-if="payMethod === 'alipay'" class="pm-checked"><Check /></el-icon>
              </div>

              <div 
                class="pay-method-card"
                :class="{ active: payMethod === 'wechat' }"
                @click="payMethod = 'wechat'"
              >
                <div class="pm-brand wechat">微</div>
                <div class="pm-text">
                  <div class="pm-name">微信支付</div>
                  <div class="pm-sub">使用微信扫一扫极速完成付款</div>
                </div>
                <el-icon v-if="payMethod === 'wechat'" class="pm-checked"><Check /></el-icon>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：结算汇总小计卡片 (吸顶) -->
        <div class="checkout-summary-column">
          <div class="summary-card">
            <h3 class="summary-title">费用明细</h3>

            <div class="fee-row">
              <span class="fee-label">商品总额</span>
              <span class="fee-val">¥{{ itemsAmount.toFixed(2) }}</span>
            </div>

            <div class="fee-row">
              <span class="fee-label">运费 (顺丰速运)</span>
              <span class="fee-val" :class="{ 'free-ship': shippingFee === 0 }">
                {{ shippingFee === 0 ? '满99包邮 ¥0.00' : `¥${shippingFee.toFixed(2)}` }}
              </span>
            </div>

            <div class="fee-row" v-if="couponDiscount > 0">
              <span class="fee-label">优惠券立减</span>
              <span class="fee-val discount">-¥{{ couponDiscount.toFixed(2) }}</span>
            </div>

            <div class="fee-divider"></div>

            <div class="summary-total-row">
              <span class="st-label">实付应结：</span>
              <div class="st-price-wrap">
                <span class="st-curr">¥</span>
                <span class="st-num">{{ finalAmount.toFixed(2) }}</span>
              </div>
            </div>

            <div class="summary-shipping-info" v-if="selectedAddress">
              <div class="ship-to">寄送至：{{ selectedAddress.province }} {{ selectedAddress.city }} {{ selectedAddress.region || '' }} {{ selectedAddress.detailAddress }}</div>
              <div class="ship-receiver">收件人：{{ selectedAddress.receiverName }} ({{ selectedAddress.receiverPhone }})</div>
            </div>

            <el-button 
              type="danger" 
              size="large" 
              class="place-order-btn" 
              :loading="isSubmitting"
              @click="handlePlaceOrder"
            >
              提交订单并付款
            </el-button>

            <div class="summary-trust-tips">
              <span>🔒 SSL加密安全付款</span> · <span>顺丰直发</span> · <span>正品发票</span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 支付弹窗 -->
    <el-dialog 
      v-model="showPayDialog" 
      :title="`收银台 · ${payMethod === 'alipay' ? '支付宝' : '微信'}支付`" 
      width="440px" 
      :close-on-click-modal="false" 
      :show-close="false" 
      class="cute-pay-dialog"
      append-to-body
    >
      <div class="pay-modal-content">
        <div class="pay-amount-box">
          <span class="pa-label">扫码支付金额：</span>
          <span class="pa-curr">¥</span>
          <span class="pa-num">{{ (serverTotalAmount ?? finalAmount).toFixed(2) }}</span>
        </div>

        <div class="qr-box">
          <img :src="payMethod === 'alipay' ? alipayQr : wechatQr" class="qr-img" />
          <div class="qr-tips">请使用手机{{ payMethod === 'alipay' ? '支付宝' : '微信' }}扫一扫完成支付</div>
        </div>

        <div class="pay-dialog-actions">
          <el-button type="success" size="large" class="btn-paid" @click="handlePaid">
            已完成支付
          </el-button>
          <el-button type="info" size="large" link @click="handleUnpaid">
            稍后在“我的订单”中支付
          </el-button>
        </div>
      </div>
    </el-dialog>

    <ClientFooter />
  </div>
</template>

<style scoped>
.checkout-page-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.checkout-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  padding: 30px 0 60px;
  flex: 1;
}

/* 步骤指示条 */
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

.step-node.done .step-badge {
  background-color: #0ea5e9;
  color: #ffffff;
}

.step-node.active {
  color: #0284c7;
}

.step-node.active .step-badge {
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
  color: #ffffff;
}

.step-line {
  width: 100px;
  height: 2px;
  background-color: #e2e8f0;
  margin: 0 16px;
}

.step-line.active {
  background-color: #0284c7;
}

/* 左右分栏 */
.checkout-main-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
  align-items: start;
}

/* 面板通用卡片 */
.panel-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
  padding: 24px;
  margin-bottom: 20px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.ph-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
}

.count-hint {
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
}

/* 地址网格 */
.address-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.address-box-card {
  padding: 16px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #ffffff;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.address-box-card:hover {
  border-color: #0284c7;
}

.address-box-card.active {
  border-color: #0284c7;
  background-color: #f0f9ff;
  box-shadow: 0 0 0 1px #0284c7;
}

.addr-top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.receiver-name {
  font-weight: bold;
  font-size: 14px;
  color: #1e293b;
}

.receiver-phone {
  font-size: 13px;
  color: #64748b;
}

.badge-default {
  background-color: #0284c7;
  color: #ffffff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
}

.addr-detail {
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
}

.addr-checked {
  position: absolute;
  bottom: 0;
  right: 0;
  background-color: #0284c7;
  color: #ffffff;
  font-size: 11px;
  padding: 2px;
  border-top-left-radius: 8px;
}

.add-new-card {
  border-style: dashed;
  justify-content: center;
  align-items: center;
  color: #0ea5e9;
  font-size: 13px;
  font-weight: bold;
  min-height: 90px;
}

.add-new-card:hover {
  background-color: #f0f9ff;
  border-color: #0ea5e9;
}

/* 商品清单 */
.products-list-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-product-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 10px;
}

.p-thumb {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #e2e8f0;
}

.p-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.p-name {
  font-size: 13px;
  font-weight: bold;
  color: #1e293b;
  line-height: 1.4;
}

.p-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.p-spec {
  font-size: 11px;
  color: #64748b;
  background-color: #e2e8f0;
  padding: 1px 6px;
  border-radius: 4px;
}

.p-flash {
  font-size: 11px;
  color: #e11d48;
  font-weight: bold;
}

.p-price-qty {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  width: 90px;
}

.p-price {
  font-size: 14px;
  color: #1e293b;
  font-weight: bold;
}

.p-qty {
  font-size: 12px;
  color: #94a3b8;
}

.p-subtotal {
  width: 100px;
  text-align: right;
  font-size: 15px;
  font-weight: 900;
  color: #f43f5e;
}

/* 优惠券网格 */
.coupon-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.coupon-item-box {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  background-color: #fcfdfe;
}

.coupon-item-box:hover {
  border-color: #0284c7;
}

.coupon-item-box.active {
  border-color: #0284c7;
  background-color: #f0f9ff;
}

.cp-discount {
  background-color: #f0f9ff;
  color: #0284c7;
  padding: 8px 12px;
  border-radius: 6px;
  font-weight: 900;
  font-size: 16px;
  display: flex;
  align-items: baseline;
}

.cp-curr { font-size: 12px; }
.cp-num { font-size: 20px; }

.cp-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cp-name {
  font-size: 13px;
  font-weight: bold;
  color: #1e293b;
}

.cp-condition {
  font-size: 11px;
  color: #94a3b8;
}

.cp-checked {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #0284c7;
  color: #ffffff;
  font-size: 10px;
  padding: 2px;
  border-bottom-left-radius: 6px;
}

.coupon-avail-hint {
  font-size: 12px;
  color: #f43f5e;
  font-weight: bold;
}

/* 支付方式 */
.pay-methods-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.pay-method-card {
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.pay-method-card:hover {
  border-color: #0ea5e9;
}

.pay-method-card.active {
  border-color: #0ea5e9;
  background-color: #f0f9ff;
  box-shadow: 0 0 0 1px #0ea5e9;
}

.pm-brand {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #ffffff;
  font-size: 18px;
  font-weight: 900;
  display: flex;
  justify-content: center;
  align-items: center;
}

.pm-brand.alipay { background-color: #1677ff; }
.pm-brand.wechat { background-color: #07c160; }

.pm-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pm-name {
  font-size: 14px;
  font-weight: bold;
  color: #1e293b;
}

.pm-sub {
  font-size: 11px;
  color: #94a3b8;
}

.pm-checked {
  position: absolute;
  top: 0;
  right: 0;
  background-color: #0ea5e9;
  color: #ffffff;
  font-size: 11px;
  padding: 2px;
  border-bottom-left-radius: 8px;
}

/* 右侧结算汇总卡 */
.checkout-summary-column {
  position: sticky;
  top: 20px;
}

.summary-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  padding: 24px;
}

.summary-title {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #0f172a;
}

.fee-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  color: #64748b;
}

.fee-val {
  font-weight: bold;
  color: #1e293b;
}

.fee-val.free-ship {
  color: #0ea5e9;
}

.fee-val.discount {
  color: #f43f5e;
}

.fee-divider {
  height: 1px;
  background-color: #e2e8f0;
  margin: 16px 0;
}

.summary-total-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}

.st-label {
  font-size: 15px;
  font-weight: bold;
  color: #0f172a;
}

.st-price-wrap {
  display: flex;
  align-items: baseline;
}

.st-curr {
  font-size: 18px;
  font-weight: bold;
  color: #f43f5e;
}

.st-num {
  font-size: 32px;
  font-weight: 900;
  color: #f43f5e;
  line-height: 1;
}

.summary-shipping-info {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 20px;
  line-height: 1.5;
}

.place-order-btn {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-size: 16px;
  font-weight: bold;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.3);
  transition: all 0.2s;
}

.place-order-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(2, 132, 199, 0.4);
}

.summary-trust-tips {
  margin-top: 14px;
  text-align: center;
  font-size: 11px;
  color: #94a3b8;
}

/* 支付弹窗 */
.pay-modal-content {
  text-align: center;
  padding: 10px 0;
}

.pay-amount-box {
  display: flex;
  justify-content: center;
  align-items: baseline;
  margin-bottom: 20px;
}

.pa-label {
  font-size: 14px;
  color: #64748b;
}

.pa-curr {
  font-size: 18px;
  color: #f43f5e;
  font-weight: bold;
  margin-left: 6px;
}

.pa-num {
  font-size: 32px;
  font-weight: 900;
  color: #f43f5e;
}

.qr-box {
  margin: 0 auto 24px;
}

.qr-img {
  width: 200px;
  height: 200px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.qr-tips {
  margin-top: 10px;
  font-size: 13px;
  color: #64748b;
}

.pay-dialog-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-paid {
  width: 100%;
  border-radius: 20px;
  font-weight: bold;
}
</style>
