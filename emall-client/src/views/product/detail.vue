<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ShoppingCart, Back, Medal, Star, StarFilled, Warning, Clock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'

interface Sku {
  id: number; productId: number; specName: string;
  price: number; stock: number; picUrl?: string;
}

interface Comment {
  id: number; nickname: string; avatar: string; 
  star: number; content: string; pics: string; createTime: string
}

const userStore = useUserStore()
const cartStore = useCartStore()
const route = useRoute()
const router = useRouter()

const productId = route.params.id
const loading = ref(true)
const isCollected = ref(false)
const product = ref<any>({})
const buyCount = ref(1)

// SKU 规格相关状态
const skus = ref<Sku[]>([])
const selectedSku = ref<Sku | null>(null)

const activeTab = ref('details') 
const comments = ref<Comment[]>([]) 

// ================= ✨ 核心新增：秒杀倒计时逻辑引擎 =================
const flashSaleStatus = ref(0) // 0: 无活动/已结束, 1: 未开始(预告), 2: 进行中
const countdownStr = ref('')
let timer: any = null

const calculateFlashSale = () => {
  if (!product.value.promoStartTime || !product.value.promoEndTime) {
    flashSaleStatus.value = 0
    return
  }
  const now = new Date().getTime()
  // 兼容不同浏览器的日期解析
  const start = new Date(product.value.promoStartTime.replace(/-/g, '/')).getTime()
  const end = new Date(product.value.promoEndTime.replace(/-/g, '/')).getTime()

  if (now < start) {
    flashSaleStatus.value = 1
    updateTimeText(start - now)
  } else if (now >= start && now <= end) {
    flashSaleStatus.value = 2
    updateTimeText(end - now)
  } else {
    flashSaleStatus.value = 0
  }
}

const updateTimeText = (diff: number) => {
  const h = Math.floor(diff / (1000 * 60 * 60))
  const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const s = Math.floor((diff % (1000 * 60)) / 1000)
  countdownStr.value = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
// =================================================================

// ✨ 价格显示逻辑重构：提取原价，并根据秒杀状态覆盖当前价
const basePrice = computed(() => selectedSku.value ? selectedSku.value.price : '?.??')
const currentPrice = computed(() => {
  // 如果正在秒杀中，强行使用秒杀特价
  if (flashSaleStatus.value === 2 && product.value.promoPrice) {
    return product.value.promoPrice
  }
  return basePrice.value
})

const currentStock = computed(() => selectedSku.value ? selectedSku.value.stock : 0)
const currentPic = computed(() => (selectedSku.value && selectedSku.value.picUrl) ? selectedSku.value.picUrl : product.value.picUrl)
const currentSpecName = computed(() => selectedSku.value ? selectedSku.value.specName : '')
const currentSkuId = computed(() => selectedSku.value ? selectedSku.value.id : 0)

// 判断商品当前是否可以正常售卖（必须配置了规格，且选中了规格）
const isSellable = computed(() => skus.value.length > 0 && selectedSku.value !== null)

const fetchDetail = async () => {
  try {
    const res = await request.get(`/product/detail/${productId}`)
    product.value = res
    // ✨ 拉取到数据后立刻计算秒杀状态并开启定时器
    calculateFlashSale()
    if (product.value.promoStartTime) {
      timer = setInterval(calculateFlashSale, 1000)
    }
  } finally {
    loading.value = false
  }
}

const fetchSkus = async () => {
  try {
    const res = await request.get<any, Sku[]>(`/product/skus/${productId}`)
    skus.value = res
    if (res.length > 0) {
      selectedSku.value = res[0] // 默认选中第一个规格
    }
  } catch (error) {
    console.error('获取规格失败')
  }
}

const fetchComments = async () => {
  try {
    const res = await request.get<any, Comment[]>(`/comment/list/${productId}`)
    comments.value = res
  } catch (error) {}
}

const checkFavoriteStatus = async () => {
  if (!userStore.userInfo) return
  const res = await request.get('/favorite/status', {
    params: { userId: userStore.userInfo.id, productId: productId }
  })
  isCollected.value = res
}

const handleToggleFavorite = async () => {
  if (!userStore.userInfo) {
    ElMessage.warning('请先登录后再收藏')
    return router.push('/login')
  }
  const res = await request.post('/favorite/toggle', { userId: userStore.userInfo.id, productId: product.value.id })
  isCollected.value = (res === 'collected')
  ElMessage.success(isCollected.value ? '已加入收藏夹' : '已从收藏夹移除')
}

// 加入购物车
const handleAddToCart = () => {
  if (!isSellable.value) return ElMessage.warning('该商品正在配置规格，暂不可售！')
  if (flashSaleStatus.value === 1) return ElMessage.warning('秒杀活动还未开始哦，请稍等！') // ✨ 拦截未开始的秒杀
  if (currentStock.value <= 0) return ElMessage.warning('该规格已售罄！')
  
  const itemToAdd = {
    ...product.value,
    price: currentPrice.value,
    stock: currentStock.value,
    picUrl: currentPic.value
  }
  
  cartStore.addToCart(itemToAdd, buyCount.value, currentSpecName.value, currentSkuId.value)
  ElMessage.success(`成功将 ${buyCount.value} 件 "${currentSpecName.value}" 加入购物车！`)
}

// 立即购买
const handleBuyNow = () => {
  if (!userStore.userInfo) {
    ElMessage.warning('请先登录后再购买')
    return router.push('/login')
  }
  if (!isSellable.value) return ElMessage.warning('该商品正在配置规格，暂不可售！')
  if (flashSaleStatus.value === 1) return ElMessage.warning('秒杀活动还未开始哦，请稍等！') // ✨ 拦截未开始的秒杀
  if (currentStock.value <= 0) return ElMessage.warning('该规格已售罄！')
  
  const buyNowItem = {
    id: product.value.id,
    skuId: currentSkuId.value,
    name: product.value.name,
    price: currentPrice.value, // ✨ 传递当前的真实价格（秒杀价或原价）
    picUrl: currentPic.value,
    stock: currentStock.value,
    count: buyCount.value,
    spec: currentSpecName.value,
    checked: true 
  }
  
  sessionStorage.setItem('buy_now_item', JSON.stringify(buyNowItem))
  router.push({ path: '/checkout', query: { type: 'buyNow' } })
}

// 监听库存变化，防止购买数量超过当前规格的库存
watch(currentStock, (newStock) => {
  if (buyCount.value > newStock && newStock > 0) {
    buyCount.value = newStock
  }
})

onMounted(async () => {
  await fetchDetail()
  await fetchSkus() // 先拉商品，再拉规格
  checkFavoriteStatus()
  fetchComments()
})
</script>

<template>
  <div class="detail-layout">
    <header class="glass-header">
      <div class="header-content">
        <div class="header-left">
          <el-button link :icon="Back" @click="router.back()" class="back-btn">返回商城</el-button>
          <span class="brand-text">E-MALL 严选</span>
        </div>
        <div class="header-right">
          <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0">
            <el-button circle class="icon-btn" :icon="ShoppingCart" @click="router.push('/cart')" />
          </el-badge>
        </div>
      </div>
    </header>

    <main class="main-container" v-loading="loading">
      <el-card class="glass-card hero-card" v-if="product.id">
        <div class="product-content">
          <div class="image-showcase">
            <img v-if="currentPic" :src="currentPic" class="main-img" />
            <div v-else class="no-img">暂无图片</div>
          </div>

          <div class="info-showcase">
            <h1 class="title">{{ product.name }}</h1>
            <p class="desc">{{ product.description }}</p>
            
            <div class="flash-banner" v-if="flashSaleStatus !== 0" :class="{ 'is-active': flashSaleStatus === 2 }">
              <div class="fb-left">
                <el-icon size="24"><Clock /></el-icon>
                <span>{{ flashSaleStatus === 2 ? '限时秒杀中' : '秒杀活动预告' }}</span>
              </div>
              <div class="fb-right">
                <span class="fb-label">{{ flashSaleStatus === 2 ? '距结束仅剩' : '距开始还剩' }}</span>
                <span class="fb-time">{{ countdownStr }}</span>
              </div>
            </div>

            <div class="price-box" :class="{ 'flash-price-box': flashSaleStatus === 2 }">
              <div class="price-main">
                <span class="currency">¥</span>
                <span class="price-num">{{ currentPrice }}</span>
                <span class="original-price" v-if="flashSaleStatus === 2">原价: ¥{{ basePrice }}</span>
              </div>
              <div class="price-tags">
                <el-tag type="danger" effect="dark" size="small">正品保障</el-tag>
                <el-tag type="warning" effect="plain" size="small">送积分</el-tag>
              </div>
            </div>

            <div class="meta-info">
              <div class="meta-item">
                <span class="label">发货：</span>
                <span class="value">
                  <el-icon color="#10b981"><Medal /></el-icon> 现货 · 顺丰速运 · 免运费
                </span>
              </div>
              <div class="meta-item">
                <span class="label">库存：</span>
                <span class="value" :style="{ color: isSellable && currentStock > 0 ? '#334155' : '#ef4444' }">
                  <template v-if="!isSellable">待商家配置规格</template>
                  <template v-else-if="currentStock > 0">有货 (剩余 {{ currentStock }} 件)</template>
                  <template v-else>已售罄，正在补货中...</template>
                </span>
              </div>
            </div>

            <div class="spec-selector">
              <span class="label">选规格：</span>
              <div class="spec-list" v-if="skus.length > 0">
                <el-tag
                  v-for="sku in skus"
                  :key="sku.id"
                  :type="selectedSku?.id === sku.id ? 'primary' : 'info'"
                  :effect="selectedSku?.id === sku.id ? 'dark' : 'plain'"
                  class="spec-tag"
                  @click="selectedSku = sku"
                >
                  {{ sku.specName }}
                </el-tag>
              </div>
              <div v-else class="empty-spec-alert">
                <el-icon><Warning /></el-icon> 该商品暂未配置售卖规格，暂时无法购买
              </div>
            </div>

            <div class="action-box">
              <div class="count-selector">
                <span class="label">选数量：</span>
                <el-input-number v-model="buyCount" :min="1" :max="currentStock > 0 ? currentStock : 1" :disabled="!isSellable || currentStock <= 0 || flashSaleStatus === 1" class="custom-input-number" />
              </div>
              <div class="buttons">
                <el-button type="primary" size="large" class="buy-btn" round @click="handleBuyNow" :disabled="!isSellable || currentStock <= 0 || flashSaleStatus === 1">
                  {{ flashSaleStatus === 1 ? '活动即将开始' : '立即抢购' }}
                </el-button>
                <el-button type="warning" size="large" :icon="ShoppingCart" class="cart-btn" round @click="handleAddToCart" :disabled="!isSellable || currentStock <= 0 || flashSaleStatus === 1">
                  加入购物车
                </el-button>
                <el-button 
                  class="favorite-btn"
                  :class="{ 'is-active': isCollected }"
                  circle 
                  size="large"
                  @click="handleToggleFavorite"
                >
                  <el-icon :size="20">
                    <component :is="isCollected ? StarFilled : Star" />
                  </el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="glass-card detail-tabs-card" v-if="product.id">
        <el-tabs v-model="activeTab" class="custom-tabs">
          <el-tab-pane label="图文详情" name="details">
            <div class="rich-content">
              <h3>产品特色</h3>
              <p>严选材质，匠心工艺。以下是该商品的详细展示：</p>
              <img v-if="product.picUrl" :src="product.picUrl" class="detail-long-img" />
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`用户评价 (${comments.length})`" name="reviews">
            <div v-if="comments.length > 0" class="comment-list">
              <div v-for="item in comments" :key="item.id" class="comment-item">
                <div class="comment-user">
                  <el-avatar :size="45" :src="item.avatar" />
                  <div class="user-meta">
                    <div class="u-name-row">
                      <span class="u-name">{{ item.nickname }}</span>
                      <el-rate v-model="item.star" disabled />
                    </div>
                    <span class="u-time">{{ item.createTime }}</span>
                  </div>
                </div>
                <p class="u-content">{{ item.content }}</p>
                <div class="u-pics" v-if="item.pics">
                  <el-image 
                    v-for="(img, index) in item.pics.split(',')" 
                    :key="index"
                    :src="img" 
                    :preview-src-list="item.pics.split(',')"
                    class="review-img"
                    fit="cover"
                  />
                </div>
              </div>
            </div>
            <el-empty v-else :image-size="200" description="暂无评价，快来抢占第一条热评吧！" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.detail-layout { min-height: 100vh; background-color: #f8fafc; padding-top: 100px; padding-bottom: 50px; }
.glass-header { position: fixed; top: 0; left: 0; right: 0; height: 70px; background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(16px); border-bottom: 1px solid rgba(226, 232, 240, 0.8); z-index: 1000; display: flex; justify-content: center; }
.header-content { width: 1200px; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.header-left { display: flex; align-items: center; }
.back-btn { font-size: 15px; color: #475569; margin-right: 20px; font-weight: bold; }
.brand-text { font-size: 18px; font-weight: bold; color: #0f172a; border-left: 2px solid #e2e8f0; padding-left: 20px; }

.main-container { width: 1200px; max-width: 95%; margin: 0 auto; }
.glass-card { border-radius: 24px; border: none; background: white; box-shadow: 0 10px 30px rgba(0,0,0,0.03); }

.hero-card { margin-bottom: 25px; }
.product-content { display: flex; gap: 50px; padding: 30px; }
.image-showcase { flex: 0 0 450px; height: 450px; border-radius: 20px; overflow: hidden; background: #f1f5f9; display: flex; align-items: center; justify-content: center; }
.main-img { width: 100%; height: 100%; object-fit: cover; transition: all 0.3s ease; }
.no-img { color: #94a3b8; font-size: 14px; }

.info-showcase { flex: 1; display: flex; flex-direction: column; padding-top: 10px; }
.title { font-size: 32px; color: #0f172a; margin: 0 0 15px 0; font-weight: 800; }
.desc { font-size: 15px; color: #64748b; line-height: 1.6; margin-bottom: 25px; }

/* ✨ 新增：秒杀横幅专属样式 */
.flash-banner { display: flex; justify-content: space-between; align-items: center; padding: 12px 20px; background: linear-gradient(135deg, #10b981, #059669); color: white; border-radius: 16px 16px 0 0; margin-bottom: -10px; z-index: 1; position: relative; }
.flash-banner.is-active { background: linear-gradient(135deg, #f43f5e, #e11d48); }
.fb-left { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: bold; letter-spacing: 1px; }
.fb-right { display: flex; align-items: center; gap: 10px; }
.fb-label { font-size: 13px; opacity: 0.9; }
.fb-time { font-size: 18px; font-weight: 900; background: rgba(0,0,0,0.2); padding: 4px 10px; border-radius: 8px; letter-spacing: 1px; font-family: monospace; }

.price-box { background: #fff1f2; padding: 25px; border-radius: 16px; display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border: 1px solid #ffe4e6; position: relative; z-index: 2; }
.flash-price-box { border-radius: 0 0 16px 16px; border-top: none; }
.price-main { color: #e11d48; display: flex; align-items: baseline; }
.currency { font-size: 24px; font-weight: bold; margin-right: 5px; }
.price-num { font-size: 44px; font-weight: 900; transition: all 0.3s ease; }
/* ✨ 新增：秒杀原价划线样式 */
.original-price { font-size: 14px; color: #94a3b8; text-decoration: line-through; margin-left: 15px; font-weight: normal; }

.meta-info { margin-bottom: 20px; }
.meta-item { margin-bottom: 12px; font-size: 14px; display: flex; align-items: center; }
.label { color: #94a3b8; width: 65px; font-weight: bold; }
.value { color: #334155; font-weight: 500; display: flex; align-items: center; gap: 5px; transition: color 0.3s; }

.spec-selector { margin-bottom: 25px; display: flex; align-items: flex-start; border-bottom: 1px dashed #cbd5e1; padding-bottom: 25px; min-height: 32px; }
.spec-list { display: flex; gap: 12px; flex-wrap: wrap; flex: 1; }
.spec-tag { cursor: pointer; padding: 8px 18px; border-radius: 8px; font-size: 14px; font-weight: bold; transition: all 0.2s; border: 2px solid transparent; }
.spec-tag:hover { transform: translateY(-2px); }
.spec-tag.el-tag--info { background: #f8fafc; border-color: #e2e8f0; color: #475569; }
.spec-tag.el-tag--primary { background: #0284c7; color: white; box-shadow: 0 4px 12px rgba(2, 132, 199, 0.3); }

.empty-spec-alert { flex: 1; color: #f43f5e; font-size: 13px; font-weight: bold; display: flex; align-items: center; gap: 5px; background: #fff1f2; padding: 8px 15px; border-radius: 6px; }

.action-box { margin-top: auto; }
.count-selector { margin-bottom: 30px; display: flex; align-items: center; }
.buttons { display: flex; gap: 15px; }
.buy-btn { flex: 1; background: #e11d48; border: none; font-weight: bold; }
.buy-btn.is-disabled { background: #fda4af; }
.cart-btn { flex: 1; background: #fbbf24; border: none; color: #78350f; font-weight: bold; }
.cart-btn.is-disabled { background: #fde68a; color: #d4d4d8; }

.favorite-btn { width: 50px; height: 50px; border: 2px solid #f1f5f9; color: #94a3b8; transition: all 0.3s; }
.favorite-btn.is-active { background: #fff1f2; border-color: #fb7185; color: #f43f5e; animation: heartBeat 0.3s linear; }

@keyframes heartBeat {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.comment-list { padding: 10px 0; }
.comment-item { padding: 25px 0; border-bottom: 1px solid #f1f5f9; }
.comment-user { display: flex; gap: 15px; align-items: center; margin-bottom: 15px; }
.u-name-row { display: flex; align-items: center; gap: 12px; }
.u-name { font-weight: bold; color: #1e293b; }
.u-time { font-size: 12px; color: #94a3b8; margin-top: 4px; display: block; }
.u-content { font-size: 15px; color: #475569; line-height: 1.6; margin-bottom: 15px; }
.u-pics { display: flex; gap: 10px; flex-wrap: wrap; }
.review-img { width: 100px; height: 100px; border-radius: 8px; cursor: zoom-in; }
.write-comment-entry { text-align: center; margin-top: 40px; }

.detail-tabs-card { padding: 20px 40px 50px; }
.rich-content h3 { color: #0f172a; margin-bottom: 15px; border-left: 4px solid #0284c7; padding-left: 10px; }
.detail-long-img { width: 100%; border-radius: 16px; margin-top: 30px; }
</style>
