<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Back, Location, Plus, Check, Ticket, Lightning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'

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
  priceLoading.value = true
  try {
    for (let item of checkedItems.value) {
      const detail: any = await request.get(`/product/detail/${item.id}`)
      let realPrice = detail.price
      let originalBasePrice = detail.price 
      let isFlashActive = false
      
      if (detail.promoStartTime && detail.promoEndTime && detail.promoPrice) {
        const now = new Date().getTime()
        const start = new Date(detail.promoStartTime.replace(/-/g, '/')).getTime()
        const end = new Date(detail.promoEndTime.replace(/-/g, '/')).getTime()
        if (now >= start && now <= end) {
          realPrice = detail.promoPrice
          isFlashActive = true
        }
      }
      
      if (!isFlashActive && item.skuId) {
        const skus: any[] = await request.get(`/product/skus/${item.id}`)
        const targetSku = skus.find(s => s.id === item.skuId)
        if (targetSku) {
          realPrice = targetSku.price
          originalBasePrice = targetSku.price
        }
      }

      item.price = realPrice
      item.isFlash = isFlashActive
      item.originalPrice = originalBasePrice
    }
  } catch (e) {
    console.error('同步最新价格失败', e)
  } finally {
    priceLoading.value = false
  }
}

const addressList = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)

interface Coupon {
  userCouponId: number; couponId: number; name: string;
  minAmount: number; discountAmount: number; endTime: string;
}
const myCoupons = ref<Coupon[]>([])
const selectedUserCouponId = ref<number | null>(null)

const fetchRealAddress = async () => {
  if (!userStore.userInfo) return
  try {
    const res = await request.get('/address/list', { params: { userId: userStore.userInfo.id } })
    addressList.value = res
    if (addressList.value.length > 0) {
      const defaultAddr = addressList.value.find(a => a.isDefault === 1)
      selectedAddressId.value = defaultAddr ? defaultAddr.id : addressList.value[0].id
    }
  } catch (error) {}
}

const fetchMyCoupons = async () => {
  if (!userStore.userInfo) return
  try {
    const res = await request.get<any, Coupon[]>('/coupon/myUsable', { params: { userId: userStore.userInfo.id } })
    myCoupons.value = res || []
  } catch (error) {}
}

const selectedAddress = computed(() => addressList.value.find(a => a.id === selectedAddressId.value))
const shippingFee = ref(10)

const productTotal = computed(() => {
  return checkedItems.value.reduce((sum, item) => sum + item.price * item.count, 0)
})

const eligibleCoupons = computed(() => {
  return myCoupons.value.filter(c => c.minAmount <= productTotal.value)
})

watch(eligibleCoupons, (newVal) => {
  if (newVal.length > 0 && !selectedUserCouponId.value) {
    const bestCoupon = newVal.reduce((prev, curr) => (prev.discountAmount > curr.discountAmount) ? prev : curr)
    selectedUserCouponId.value = bestCoupon.userCouponId
  } else if (newVal.length === 0) {
    selectedUserCouponId.value = null
  }
}, { immediate: true })

const currentDiscount = computed(() => {
  if (!selectedUserCouponId.value) return 0
  const coupon = eligibleCoupons.value.find(c => c.userCouponId === selectedUserCouponId.value)
  return coupon ? coupon.discountAmount : 0
})

const totalPrice = computed(() => {
  const finalPrice = productTotal.value + shippingFee.value - currentDiscount.value
  return Math.max(finalPrice, 0)
})

const isSubmitting = ref(false)
const showPayDialog = ref(false)
const currentOrderId = ref<number | null>(null)
const payMethod = ref('alipay')

const handlePlaceOrder = async () => {
  if (!userStore.userInfo) return router.push('/login')
  if (!selectedAddress.value) return ElMessage.warning('请选择收货地址')
  if (checkedItems.value.length === 0) return ElMessage.warning('没有结算商品')

  isSubmitting.value = true
  try {
    const orderData = {
      userId: userStore.userInfo.id,
      totalAmount: totalPrice.value,
      userCouponId: selectedUserCouponId.value, 
      status: 0, 
      productId: checkedItems.value[0].id,
      items: checkedItems.value.map(item => ({
        productId: item.id,
        skuId: item.skuId, // ✨ 核心修复：必须把具体规格 ID 传给后端精准扣库存！
        productName: item.spec ? `${item.name} (${item.spec})` : item.name, 
        productPrice: item.price,
        productCount: item.count
      }))
    }

    const resId = await request.post('/order/create', orderData)
    currentOrderId.value = resId as unknown as number
    
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

// ✨ 核心替换：支付完成后的核弹级动作
const handlePaid = async () => {
  if (!currentOrderId.value) return
  
  try {
    // 1. 同步订单状态为“已支付” (status = 1)
    await request.put(`/order/status/${currentOrderId.value}/1`)
    
    ElMessage.success('支付成功！')
    showPayDialog.value = false
    router.push('/orders')
  } catch (error) { 
    ElMessage.error('支付状态同步异常，请检查网络') 
  }
}

const handleUnpaid = () => {
  ElMessage.warning('订单已生成，请在规定时间内完成支付哦')
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
  <div class="checkout-page">
    <header class="glass-header">
      <div class="header-content">
        <el-button link :icon="Back" @click="router.back()" class="back-btn">返回</el-button>
        <span class="page-title">确认订单</span>
      </div>
    </header>

    <main class="main-container" v-loading="priceLoading" element-loading-text="正在为您核对最新优惠价格...">
      <section class="section-card glass-card">
        <div class="section-header">
          <span class="section-title"><el-icon><Location /></el-icon> 选择收货地址</span>
          <el-button type="primary" link :icon="Plus" @click="router.push('/address')">新增地址</el-button>
        </div>

        <div class="address-grid">
          <div v-for="addr in addressList" :key="addr.id" class="address-item" :class="{ active: selectedAddressId === addr.id }" @click="selectedAddressId = addr.id">
            <div class="user-info">
              <span class="name">{{ addr.receiverName }}</span>
              <span class="phone">{{ addr.receiverPhone }}</span>
              <el-tag v-if="addr.isDefault" size="small" effect="plain" class="default-tag">默认</el-tag>
            </div>
            <div class="detail">{{ addr.province }} {{ addr.city }} {{ addr.region }} {{ addr.detailAddress }}</div>
            <div class="check-icon" v-if="selectedAddressId === addr.id"><el-icon><Check /></el-icon></div>
          </div>
          <div class="address-item manage-card" @click="router.push('/address')">
            <div class="manage-content"><el-icon size="24"><Plus /></el-icon><p>管理 / 添加</p></div>
          </div>
        </div>
      </section>

      <section class="section-card glass-card mt-20">
        <div class="section-header"><span class="section-title">商品清单</span></div>
        <div class="product-list">
          <div v-for="item in checkedItems" :key="item.id + (item.spec || '')" class="product-item">
            <img :src="item.picUrl" class="p-img" />
            <div class="p-info">
              <div class="p-name">{{ item.name }} <span v-if="item.spec" style="color:#94a3b8; font-size:12px; margin-left:5px">({{ item.spec }})</span></div>
              
              <div class="p-price-box">
                <el-tag v-if="item.isFlash" type="danger" effect="dark" size="small" class="flash-tag">
                  <el-icon><Lightning /></el-icon> 秒杀特惠
                </el-tag>
                <span class="current-price">¥ {{ item.price.toFixed(2) }}</span>
                <span class="old-price" v-if="item.isFlash">¥{{ item.originalPrice }}</span>
                <span class="count-multiplier">x {{ item.count }}</span>
              </div>

            </div>
            <div class="p-subtotal">¥ {{ (item.price * item.count).toFixed(2) }}</div>
          </div>
        </div>
      </section>

      <section class="section-card glass-card mt-20 summary-section">
        <div class="coupon-box">
          <div class="coupon-header">
            <span class="c-title"><el-icon color="#f43f5e"><Ticket /></el-icon> 店铺优惠券</span>
            <span class="c-tips" v-if="myCoupons.length > 0">已为您自动推荐最优搭配</span>
          </div>
          <el-select v-model="selectedUserCouponId" class="coupon-selector" placeholder="无可用优惠券" clearable :disabled="eligibleCoupons.length === 0">
            <el-option v-for="c in eligibleCoupons" :key="c.userCouponId" :label="`${c.name} (满 ${c.minAmount} 减 ${c.discountAmount})`" :value="c.userCouponId">
              <span style="float: left">{{ c.name }}</span>
              <span style="float: right; color: #f43f5e; font-weight: bold; font-size: 13px">- ¥ {{ c.discountAmount }}</span>
            </el-option>
          </el-select>
        </div>

        <div class="price-detail">
          <div class="price-row"><span>商品总额</span><span>¥ {{ productTotal.toFixed(2) }}</span></div>
          <div class="price-row"><span>运费合计</span><span>+ ¥ {{ shippingFee.toFixed(2) }}</span></div>
          <div class="price-row highlight-discount" v-if="currentDiscount > 0"><span>优惠券抵扣</span><span>- ¥ {{ currentDiscount.toFixed(2) }}</span></div>
          <div class="price-row total-row"><span>实付款</span><span class="final-price">¥ {{ totalPrice.toFixed(2) }}</span></div>
        </div>
        
        <div class="action-bar">
          <div class="selected-addr-desc" v-if="selectedAddress">
            寄送至：{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.detailAddress }} （{{ selectedAddress.receiverName }} 收）
          </div>
          <el-button type="primary" size="large" round class="submit-btn" :loading="isSubmitting" @click="handlePlaceOrder">提交订单</el-button>
        </div>
      </section>
    </main>

    <el-dialog v-model="showPayDialog" title="E-MALL 专属收银台" width="400px" center :close-on-click-modal="false" :show-close="false" class="cute-pay-dialog">
      <div class="pay-container">
        <div class="pay-amount">需支付：<span class="num">¥ {{ totalPrice.toFixed(2) }}</span></div>
        <el-radio-group v-model="payMethod" class="pay-method-group">
          <el-radio label="alipay" size="large" border><span style="color:#0284c7; font-weight:bold;">支付宝支付</span></el-radio>
          <el-radio label="wechat" size="large" border><span style="color:#10b981; font-weight:bold;">微信支付</span></el-radio>
        </el-radio-group>
        <div class="qr-box">
          <img v-if="payMethod === 'alipay'" :src="alipayQr" class="qr-code" alt="支付宝二维码" />
          <img v-if="payMethod === 'wechat'" :src="wechatQr" class="qr-code" alt="微信二维码" />
          <p class="scan-tip">请使用手机 {{ payMethod === 'alipay' ? '支付宝' : '微信' }} 扫码支付</p>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button round class="cancel-pay-btn" @click="handleUnpaid">我再想想</el-button>
          <el-button round type="primary" class="confirm-pay-btn" @click="handlePaid">支付完成</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 核心布局与基础样式 */
.checkout-page { min-height: 100vh; background-color: #f0f9ff; padding-top: 80px; padding-bottom: 50px; }
.glass-header { position: fixed; top: 0; left: 0; right: 0; height: 65px; background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(12px); border-bottom: 1px solid #e0f2fe; z-index: 100; display: flex; justify-content: center; }
.header-content { width: 1000px; display: flex; align-items: center; padding: 0 20px; }
.page-title { margin-left: 20px; font-size: 18px; font-weight: bold; color: #0369a1; }
.main-container { width: 1000px; margin: 0 auto; }
.mt-20 { margin-top: 20px; }
.glass-card { background: rgba(255, 255, 255, 0.8); backdrop-filter: blur(8px); border-radius: 20px; border: 1px solid rgba(186, 230, 253, 0.5); padding: 25px; box-shadow: 0 10px 25px rgba(2, 132, 199, 0.05); }

/* 地址模块 */
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.section-title { font-size: 16px; font-weight: bold; color: #1e293b; display: flex; align-items: center; gap: 8px; }
.address-grid { display: flex; flex-direction: row; gap: 20px; overflow-x: auto; padding: 10px 5px 20px 5px; scroll-behavior: smooth; -webkit-overflow-scrolling: touch; }
.address-grid::-webkit-scrollbar { height: 6px; }
.address-grid::-webkit-scrollbar-thumb { background: #bae6fd; border-radius: 10px; }
.address-grid::-webkit-scrollbar-track { background: transparent; }
.address-item { flex-shrink: 0; width: 320px; position: relative; border: 2px solid #f1f5f9; border-radius: 16px; padding: 18px; cursor: pointer; transition: all 0.3s; background: white; }
.address-item.active { border-color: #38bdf8; background: #f0f9ff; transform: translateY(-5px); box-shadow: 0 10px 20px rgba(14, 165, 233, 0.1); }
.user-info { margin-bottom: 8px; display: flex; align-items: center; gap: 10px; }
.name { font-weight: bold; color: #0f172a; }
.phone { color: #64748b; font-size: 13px; }
.detail { font-size: 13px; color: #475569; line-height: 1.4; }
.check-icon { position: absolute; top: -10px; right: -10px; background: #38bdf8; color: white; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 10px rgba(56, 189, 248, 0.4); }
.manage-card { display: flex; align-items: center; justify-content: center; color: #0ea5e9; border: 2px dashed #bae6fd; background: rgba(240, 249, 255, 0.5); }
.manage-card:hover { border-color: #38bdf8; background: #e0f2fe; }

/* 订单明细模块 */
.product-item { display: flex; align-items: center; padding: 15px 0; border-bottom: 1px dashed #e2e8f0; }
.p-img { width: 70px; height: 70px; border-radius: 10px; object-fit: cover; margin-right: 20px; border: 1px solid #f1f5f9; }
.p-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 8px; }
.p-name { font-weight: 700; color: #1e293b; font-size: 15px; }

/* ✨ 价格校对与展示专属样式 */
.p-price-box { display: flex; align-items: center; gap: 8px; }
.current-price { color: #e11d48; font-weight: 800; font-size: 15px; }
.old-price { color: #94a3b8; text-decoration: line-through; font-size: 12px; }
.flash-tag { margin-right: 5px; }
.count-multiplier { color: #64748b; font-size: 13px; margin-left: 5px; }

.p-subtotal { font-weight: 900; color: #0f172a; font-size: 18px; }

/* 优惠券与结算汇总 */
.summary-section { padding-top: 30px; }
.coupon-box { background: #fff1f2; border: 1px solid #ffe4e6; border-radius: 12px; padding: 20px; margin-bottom: 25px; display: flex; justify-content: space-between; align-items: center; }
.coupon-header { display: flex; flex-direction: column; gap: 5px; }
.c-title { font-weight: bold; color: #1e293b; display: flex; align-items: center; gap: 6px; }
.c-tips { font-size: 12px; color: #fb7185; }
.coupon-selector { width: 320px; }
:deep(.coupon-selector .el-input__wrapper) { border-radius: 8px; box-shadow: 0 0 0 1px #fecdd3 inset; }
:deep(.coupon-selector .el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px #f43f5e inset; }

.price-detail { border-bottom: 1px solid #f1f5f9; padding-bottom: 15px; margin-bottom: 20px; }
.price-row { display: flex; justify-content: space-between; margin-bottom: 10px; color: #64748b; font-size: 14px; }
.highlight-discount { color: #f43f5e; font-weight: bold; }
.total-row { margin-top: 15px; color: #0f172a; align-items: center; }
.final-price { color: #f43f5e; font-size: 28px; font-weight: 900; }

.action-bar { display: flex; justify-content: space-between; align-items: center; }
.selected-addr-desc { font-size: 13px; color: #94a3b8; max-width: 60%; }
.submit-btn { padding: 0 40px; font-size: 16px; font-weight: bold; background: linear-gradient(135deg, #0ea5e9, #0284c7); border: none; box-shadow: 0 8px 20px rgba(14, 165, 233, 0.2); height: 45px; }
.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 12px 25px rgba(14, 165, 233, 0.3); }

/* 收银台样式 */
:deep(.cute-pay-dialog) { border-radius: 20px; overflow: hidden; }
:deep(.cute-pay-dialog .el-dialog__header) { background: #f0f9ff; padding: 20px; margin-right: 0; border-bottom: 1px solid #e0f2fe; font-weight: bold; color: #0369a1; }
.pay-container { text-align: center; padding: 10px 0; }
.pay-amount { font-size: 16px; color: #475569; margin-bottom: 25px; }
.pay-amount .num { color: #f43f5e; font-size: 32px; font-weight: 900; margin-left: 5px; }
.pay-method-group { margin-bottom: 25px; display: flex; justify-content: center; gap: 10px; width: 100%; }
.qr-box { background: #f8fafc; padding: 25px; border-radius: 16px; border: 2px dashed #cbd5e1; display: inline-block; }
.qr-code { width: 180px; height: 180px; object-fit: contain; border-radius: 10px; }
.scan-tip { color: #64748b; font-size: 14px; margin-top: 15px; font-weight: bold; }
.dialog-footer { display: flex; justify-content: center; gap: 20px; width: 100%; }
.cancel-pay-btn { background: #f1f5f9; color: #64748b; border: none; }
.confirm-pay-btn { background: #0ea5e9; border: none; padding: 0 30px; font-weight: bold; }
</style>
