<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  ShoppingCart, Star, StarFilled, Lightning, 
  Check, Service, Medal, Van, CircleCheck, Picture
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'
import ClientHeader from '../../components/ClientHeader.vue'
import ClientFooter from '../../components/ClientFooter.vue'

interface Sku {
  id: number
  productId: number
  specName: string
  price: number
  stock: number
  picUrl?: string
}

interface Comment {
  id: number
  nickname: string
  avatar: string
  star: number
  content: string
  pics: string
  createTime: string
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

// 图片相册
const activeImageIndex = ref(0)
const imageList = computed(() => {
  const list: string[] = []
  if (selectedSku.value?.picUrl) list.push(selectedSku.value.picUrl)
  if (product.value.picUrl && !list.includes(product.value.picUrl)) list.push(product.value.picUrl)
  if (Array.isArray(product.value.subImages)) {
    product.value.subImages.forEach((img: string) => {
      if (img && !list.includes(img)) list.push(img)
    })
  }
  return list.length > 0 ? list : ['https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80']
})

const currentPic = computed(() => imageList.value[activeImageIndex.value] || product.value.picUrl)

const activeTab = ref('details') 
const comments = ref<Comment[]>([]) 

// 图片预览大图弹窗
const previewDialogVisible = ref(false)
const previewImageSrc = ref('')
const handlePreviewImage = (src: string) => {
  previewImageSrc.value = src
  previewDialogVisible.value = true
}

// 秒杀倒计时引擎
const flashSaleStatus = ref(0) // 0: 无/已结束, 1: 预告, 2: 进行中
const countdownStr = ref('')
let timer: any = null

const calculateFlashSale = () => {
  if (!product.value.promoStartTime || !product.value.promoEndTime) {
    flashSaleStatus.value = 0
    return
  }
  const now = new Date().getTime()
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

// 价格与库存计算
const basePrice = computed(() => selectedSku.value ? selectedSku.value.price : product.value.price || 0)
const currentPrice = computed(() => {
  if (flashSaleStatus.value === 2 && product.value.promoPrice) {
    return product.value.promoPrice
  }
  return basePrice.value
})

const currentStock = computed(() => selectedSku.value ? selectedSku.value.stock : product.value.stock || 0)
const currentSpecName = computed(() => selectedSku.value ? selectedSku.value.specName : '默认规格')
const currentSkuId = computed(() => selectedSku.value ? selectedSku.value.id : 0)

const isSellable = computed(() => skus.value.length > 0 && selectedSku.value !== null)

const fetchDetail = async () => {
  try {
    const res = await request.get(`/product/detail/${productId}`)
    product.value = res
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
      selectedSku.value = res[0]
    }
  } catch (error) {
    console.error('获取规格失败')
  }
}

const selectSku = (sku: Sku) => {
  selectedSku.value = sku
  if (sku.picUrl) {
    const foundIdx = imageList.value.indexOf(sku.picUrl)
    if (foundIdx !== -1) {
      activeImageIndex.value = foundIdx
    }
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
  try {
    const res = await request.get('/favorite/status', {
      params: { userId: userStore.userInfo.id, productId: productId }
    })
    isCollected.value = (res === true || res === 'collected')
  } catch (e) {}
}

const handleToggleFavorite = async () => {
  if (!userStore.userInfo) {
    ElMessage.warning('请先登录后再收藏')
    return router.push('/login')
  }
  try {
    const res = await request.post('/favorite/toggle', { userId: userStore.userInfo.id, productId: product.value.id })
    isCollected.value = (res === 'collected')
    ElMessage.success(isCollected.value ? '已成功加入收藏夹 ❤️' : '已从收藏夹移除')
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

// 加入购物车
const handleAddToCart = () => {
  if (!isSellable.value) return ElMessage.warning('该商品正在配置规格，暂不可售！')
  if (flashSaleStatus.value === 1) return ElMessage.warning('秒杀活动还未开始哦，请稍候！')
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
  if (flashSaleStatus.value === 1) return ElMessage.warning('秒杀活动还未开始哦，请稍候！')
  if (currentStock.value <= 0) return ElMessage.warning('该规格已售罄！')
  
  const buyNowItem = {
    id: product.value.id,
    skuId: currentSkuId.value,
    name: product.value.name,
    price: currentPrice.value,
    picUrl: currentPic.value,
    stock: currentStock.value,
    count: buyCount.value,
    spec: currentSpecName.value,
    checked: true 
  }
  
  sessionStorage.setItem('buy_now_item', JSON.stringify(buyNowItem))
  router.push({ path: '/checkout', query: { type: 'buyNow' } })
}

watch(currentStock, (newStock) => {
  if (buyCount.value > newStock && newStock > 0) {
    buyCount.value = newStock
  }
})

onMounted(async () => {
  await fetchDetail()
  await fetchSkus()
  checkFavoriteStatus()
  fetchComments()
})
</script>

<template>
  <div class="product-detail-layout">
    <!-- 统一头部 -->
    <ClientHeader />

    <main class="detail-container" v-loading="loading">
      <!-- 1. 面包屑导航 -->
      <nav class="breadcrumb-bar">
        <span class="crumb-link" @click="router.push('/')">首页</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-link" @click="router.push('/')">官方优选</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">{{ product.name || '商品详情' }}</span>
      </nav>

      <!-- 2. 主商品展示卡片 -->
      <div class="product-main-card" v-if="product.id">
        <!-- 左侧：相册展区 -->
        <div class="gallery-column">
          <!-- 主大图 -->
          <div class="main-image-box">
            <img :src="currentPic" class="main-image" :alt="product.name" />
            <span class="tag-ziying">官方自营</span>
            <span class="tag-flash" v-if="flashSaleStatus === 2">限时秒杀</span>
            <div class="zoom-hint">
              <el-icon><Picture /></el-icon>
              <span @click="handlePreviewImage(currentPic)">查看原图</span>
            </div>
          </div>

          <!-- 缩略图条 -->
          <div class="thumbnail-strip" v-if="imageList.length > 1">
            <div 
              v-for="(img, idx) in imageList" 
              :key="idx" 
              class="thumb-item" 
              :class="{ active: activeImageIndex === idx }"
              @mouseenter="activeImageIndex = idx"
            >
              <img :src="img" class="thumb-img" />
            </div>
          </div>

          <!-- 正品保障标签条 -->
          <div class="gallery-promise-bar">
            <span class="promise-item"><el-icon color="#0ea5e9"><CircleCheck /></el-icon> 假一赔十</span>
            <span class="promise-item"><el-icon color="#0ea5e9"><Van /></el-icon> 顺丰包邮</span>
            <span class="promise-item"><el-icon color="#0ea5e9"><Medal /></el-icon> 7天无理由</span>
            <span class="promise-item"><el-icon color="#0ea5e9"><Service /></el-icon> 专属管家</span>
          </div>
        </div>

        <!-- 右侧：商品购买与规格选择面板 -->
        <div class="spec-column">
          <!-- 标题与副标题 -->
          <div class="title-zone">
            <div class="product-title-row">
              <span class="title-tag">自营严选</span>
              <h1 class="product-title">{{ product.name }}</h1>
            </div>
            <p class="product-desc">{{ product.description }}</p>
          </div>

          <!-- 秒杀倒计时条 (进行中或预告) -->
          <div class="flash-sale-bar" v-if="flashSaleStatus !== 0" :class="{ 'is-ongoing': flashSaleStatus === 2 }">
            <div class="fs-left">
              <el-icon :size="20" class="fs-icon"><Lightning /></el-icon>
              <span class="fs-title">{{ flashSaleStatus === 2 ? '⚡ E-MALL 限时秒杀特惠' : '⏰ 秒杀活动即将开抢' }}</span>
            </div>
            <div class="fs-right">
              <span class="fs-label">{{ flashSaleStatus === 2 ? '本场距结束：' : '下场距开抢：' }}</span>
              <span class="fs-time">{{ countdownStr }}</span>
            </div>
          </div>

          <!-- 价格与活动区块 -->
          <div class="pricing-card" :class="{ 'flash-pricing': flashSaleStatus === 2 }">
            <div class="price-row">
              <div class="price-main">
                <span class="price-label">活动特惠价</span>
                <span class="currency">¥</span>
                <span class="price-num">{{ currentPrice }}</span>
                <span class="orig-price" v-if="flashSaleStatus === 2 || (product.promoPrice && currentPrice < basePrice)">
                  参考原价 ¥{{ basePrice }}
                </span>
              </div>
              <div class="sales-stats">
                <div class="stat-item">
                  <span class="stat-num">{{ product.sales || 0 }}+</span>
                  <span class="stat-lbl">累计已售</span>
                </div>
                <span class="stat-divider"></span>
                <div class="stat-item">
                  <span class="stat-num">{{ comments.length }}</span>
                  <span class="stat-lbl">用户评价</span>
                </div>
              </div>
            </div>

            <!-- 优惠促销标签 -->
            <div class="coupon-line">
              <span class="line-label">优惠支持</span>
              <div class="coupon-tags">
                <span class="coupon-pill">满100减20</span>
                <span class="coupon-pill">满300减50</span>
                <span class="coupon-tip">可在首页领券中心一键领取抵扣</span>
              </div>
            </div>
          </div>

          <!-- 规格选择矩阵 -->
          <div class="spec-matrix-zone">
            <div class="spec-row">
              <span class="row-label">规格型号</span>
              <div class="sku-buttons">
                <div 
                  v-for="sku in skus" 
                  :key="sku.id" 
                  class="sku-btn"
                  :class="{ 
                    active: selectedSku?.id === sku.id,
                    'is-soldout': sku.stock <= 0
                  }"
                  @click="sku.stock > 0 && selectSku(sku)"
                >
                  <img v-if="sku.picUrl" :src="sku.picUrl" class="sku-mini-img" />
                  <span class="sku-name">{{ sku.specName }}</span>
                  <span class="sku-price">¥{{ sku.price }}</span>
                  <el-icon v-if="selectedSku?.id === sku.id" class="sku-checked-icon"><Check /></el-icon>
                </div>
              </div>
            </div>

            <!-- 购买数量 -->
            <div class="quantity-row">
              <span class="row-label">购买数量</span>
              <div class="qty-stepper">
                <el-input-number 
                  v-model="buyCount" 
                  :min="1" 
                  :max="currentStock > 0 ? currentStock : 1" 
                  :disabled="currentStock <= 0"
                  size="default"
                />
                <span class="stock-info">
                  库存还剩 <strong :class="{ 'low-stock': currentStock <= 5 }">{{ currentStock }}</strong> 件
                </span>
              </div>
            </div>

            <!-- 配送保障说明 -->
            <div class="deliver-row">
              <span class="row-label">配送服务</span>
              <span class="deliver-text">
                由 <strong>E-MALL 官方旗舰仓</strong> 极速发货，并提供售后保障。18:00 前完成支付，顺丰预计次日达！
              </span>
            </div>
          </div>

          <!-- 底部操作按钮群 -->
          <div class="action-buttons-row">
            <el-button 
              type="danger" 
              size="large" 
              class="btn-add-cart" 
              :icon="ShoppingCart"
              :disabled="currentStock <= 0 || !isSellable"
              @click="handleAddToCart"
            >
              加入购物车
            </el-button>

            <el-button 
              type="primary" 
              size="large" 
              class="btn-buy-now" 
              :icon="Lightning"
              :disabled="currentStock <= 0 || !isSellable"
              @click="handleBuyNow"
            >
              {{ currentStock > 0 ? '立即购买' : '该规格已售罄' }}
            </el-button>

            <button 
              class="fav-btn" 
              :class="{ 'is-collected': isCollected }"
              @click="handleToggleFavorite"
              title="收藏商品"
            >
              <el-icon :size="20"><component :is="isCollected ? StarFilled : Star" /></el-icon>
              <span>{{ isCollected ? '已收藏' : '收藏' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 3. 商品详情与买家秀评价 Tab 栏 -->
      <div class="product-bottom-section">
        <el-tabs v-model="activeTab" class="product-tabs" type="border-card">
          <!-- Tab 1: 图文详情 -->
          <el-tab-pane label="商品图文详情" name="details">
            <div class="tab-content details-tab">
              <div class="details-text-lead">
                <h3>商品核心亮点</h3>
                <p>{{ product.description }}</p>
              </div>
              <div class="details-html" v-if="product.detailHtml" v-html="product.detailHtml"></div>
              <div class="details-gallery" v-if="imageList.length > 0">
                <img v-for="(img, idx) in imageList" :key="idx" :src="img" class="content-full-img" loading="lazy" />
              </div>
            </div>
          </el-tab-pane>

          <!-- Tab 2: 规格参数 -->
          <el-tab-pane label="规格与包装" name="specs">
            <div class="tab-content specs-tab">
              <table class="specs-table">
                <tbody>
                  <tr>
                    <td class="td-key">商品名称</td>
                    <td class="td-val">{{ product.name }}</td>
                  </tr>
                  <tr>
                    <td class="td-key">商品编号</td>
                    <td class="td-val">EMALL-PROD-{{ product.id }}</td>
                  </tr>
                  <tr>
                    <td class="td-key">可选规格</td>
                    <td class="td-val">{{ skus.map(s => s.specName).join('、') || '标准默认规格' }}</td>
                  </tr>
                  <tr>
                    <td class="td-key">服务承诺</td>
                    <td class="td-val">全国联保、7天无理由退换、假一赔十</td>
                  </tr>
                  <tr>
                    <td class="td-key">包装清单</td>
                    <td class="td-val">商品正品包装盒 x1、商品主机/实物 x1、说明书与三包凭证 x1</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </el-tab-pane>

          <!-- Tab 3: 买家评价与买家秀 -->
          <el-tab-pane :label="`买家口碑评价 (${comments.length})`" name="comments">
            <div class="tab-content comments-tab">
              <!-- 评分概览 -->
              <div class="comments-summary-banner">
                <div class="score-box">
                  <span class="score-num">99<small>%</small></span>
                  <span class="score-lbl">好评率</span>
                </div>
                <div class="score-tags">
                  <span class="tag-chip active">全部 ({{ comments.length }})</span>
                  <span class="tag-chip">好评 ({{ comments.filter(c => c.star >= 4).length }})</span>
                  <span class="tag-chip">有图买家秀 ({{ comments.filter(c => !!c.pics).length }})</span>
                  <span class="tag-chip">顺丰神速 ({{ Math.max(1, comments.length) }})</span>
                </div>
              </div>

              <!-- 评价列表 -->
              <div class="comments-list">
                <div v-if="comments.length === 0" class="empty-comments">
                  <p>该商品暂无买家评价，抢先下单并留下您的使用心得吧！</p>
                </div>

                <div v-for="c in comments" :key="c.id" class="comment-item-card">
                  <div class="comment-author">
                    <el-avatar :size="42" class="c-avatar" style="background:#0ea5e9;">
                      {{ c.nickname?.substring(0,1) || '买' }}
                    </el-avatar>
                    <div class="author-info">
                      <span class="author-name">{{ c.nickname || 'E-MALL 认证买家' }}</span>
                      <span class="author-vip">VIP 尊享买家</span>
                    </div>
                  </div>
                  <div class="comment-body">
                    <div class="c-meta">
                      <el-rate :model-value="c.star" disabled text-color="#ff9900" size="small" />
                      <span class="c-date">{{ c.createTime || '近期已购' }}</span>
                    </div>
                    <p class="c-text">{{ c.content }}</p>
                    
                    <!-- 买家秀晒图 -->
                    <div class="c-pics-grid" v-if="c.pics">
                      <img 
                        v-for="(imgUrl, pIdx) in c.pics.split(',').filter(Boolean)" 
                        :key="pIdx" 
                        :src="imgUrl" 
                        class="c-pic-thumb"
                        @click="handlePreviewImage(imgUrl)"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- Tab 4: 售后与服务 -->
          <el-tab-pane label="售后服务与保障" name="service">
            <div class="tab-content service-tab">
              <div class="service-block">
                <h4>🛡️ 厂家服务</h4>
                <p>本平台销售并发货的商品，由 E-MALL 提供正品发票和相应的售后服务。请您放心购买！</p>
              </div>
              <div class="service-block">
                <h4>📦 7天无理由退换货保障</h4>
                <p>商品签收之日起7日内，在保证商品完好、包装齐全且不影响二次销售的前提下，支持无理由退货。顺丰上门取件，极速退款直达账户。</p>
              </div>
              <div class="service-block">
                <h4>⚡ 价格保护政策</h4>
                <p>自签收之日起 7 天内，若本站同一商品规格出现降价，您可申请退还差价！</p>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </main>

    <!-- 原图预览弹窗 -->
    <el-dialog v-model="previewDialogVisible" title="高清原图查看" width="600px" append-to-body>
      <div style="text-align: center;">
        <img :src="previewImageSrc" style="max-width: 100%; max-height: 500px; border-radius: 8px;" />
      </div>
    </el-dialog>

    <!-- 统一页脚 -->
    <ClientFooter />
  </div>
</template>

<style scoped>
.product-detail-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.detail-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  padding-bottom: 50px;
  flex: 1;
}

/* 面包屑 */
.breadcrumb-bar {
  padding: 16px 0;
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.crumb-link {
  cursor: pointer;
  transition: color 0.2s;
}

.crumb-link:hover {
  color: #0284c7;
}

.crumb-sep {
  color: #cbd5e1;
}

.crumb-current {
  color: #0f172a;
  font-weight: bold;
}

/* 主商品展示卡片 */
.product-main-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 30px;
  display: grid;
  grid-template-columns: 460px 1fr;
  gap: 40px;
  margin-bottom: 30px;
}

/* 左侧相册展区 */
.gallery-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.main-image-box {
  width: 460px;
  height: 460px;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.main-image:hover {
  transform: scale(1.04);
}

.tag-ziying {
  position: absolute;
  top: 14px;
  left: 14px;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #ffffff;
  font-size: 12px;
  font-weight: bold;
  padding: 3px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
}

.tag-flash {
  position: absolute;
  top: 14px;
  right: 14px;
  background: linear-gradient(135deg, #f43f5e, #e11d48);
  color: #ffffff;
  font-size: 12px;
  font-weight: bold;
  padding: 3px 8px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(244, 63, 94, 0.3);
}

.zoom-hint {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  color: #ffffff;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.thumbnail-strip {
  display: flex;
  gap: 12px;
}

.thumb-item {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  background: #f1f5f9;
  transition: all 0.2s;
}

.thumb-item:hover, .thumb-item.active {
  border-color: #0284c7;
  transform: translateY(-2px);
}

.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-promise-bar {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  background-color: #f8fafc;
  border-radius: 10px;
  font-size: 12px;
  color: #475569;
}

.promise-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 右侧规格面板 */
.spec-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.product-title-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}

.title-tag {
  flex-shrink: 0;
  background-color: #0ea5e9;
  color: #ffffff;
  font-size: 12px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 4px;
}

.product-title {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
  color: #0f172a;
  line-height: 1.4;
}

.product-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

/* 秒杀倒计时条 */
.flash-sale-bar {
  background: linear-gradient(135deg, #f43f5e, #fb7185);
  color: #ffffff;
  padding: 10px 16px;
  border-radius: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.fs-left {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: bold;
  font-size: 14px;
}

.fs-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.fs-time {
  background-color: #ffffff;
  color: #e11d48;
  font-weight: 900;
  padding: 2px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
}

/* 价格卡片 */
.pricing-card {
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 20px;
}

.flash-pricing {
  background: linear-gradient(135deg, #fff1f2 0%, #ffffff 100%);
  border-color: #fecdd3;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-main {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price-label {
  font-size: 12px;
  color: #f43f5e;
  font-weight: bold;
  margin-right: 4px;
}

.currency {
  color: #f43f5e;
  font-size: 20px;
  font-weight: bold;
}

.price-num {
  color: #f43f5e;
  font-size: 34px;
  font-weight: 900;
  line-height: 1;
}

.orig-price {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: line-through;
  margin-left: 10px;
}

.sales-stats {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-num {
  font-size: 14px;
  font-weight: bold;
  color: #1e293b;
}

.stat-lbl {
  font-size: 11px;
  color: #94a3b8;
}

.stat-divider {
  width: 1px;
  height: 24px;
  background-color: #e2e8f0;
}

.coupon-line {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.line-label {
  font-size: 12px;
  color: #64748b;
  flex-shrink: 0;
}

.coupon-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.coupon-pill {
  background-color: #ffe4e6;
  color: #e11d48;
  border: 1px dashed #f43f5e;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 4px;
}

.coupon-tip {
  font-size: 12px;
  color: #94a3b8;
}

/* 规格矩阵 */
.spec-matrix-zone {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.spec-row, .quantity-row, .deliver-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.row-label {
  width: 60px;
  flex-shrink: 0;
  font-size: 13px;
  color: #64748b;
}

.sku-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sku-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background-color: #ffffff;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  font-size: 13px;
  color: #334155;
}

.sku-btn:hover {
  border-color: #0284c7;
}

.sku-btn.active {
  border-color: #0284c7;
  background-color: #f0f9ff;
  color: #0284c7;
  font-weight: bold;
}

.sku-mini-img {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  object-fit: cover;
}

.sku-price {
  font-size: 12px;
  color: #94a3b8;
}

.sku-btn.active .sku-price {
  color: #0284c7;
}

.sku-checked-icon {
  position: absolute;
  bottom: 0;
  right: 0;
  background-color: #0284c7;
  color: #ffffff;
  font-size: 10px;
  padding: 1px;
  border-top-left-radius: 6px;
}

.sku-btn.is-soldout {
  opacity: 0.5;
  cursor: not-allowed;
  text-decoration: line-through;
}

.qty-stepper {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stock-info {
  font-size: 13px;
  color: #64748b;
}

.low-stock {
  color: #f43f5e;
}

.deliver-text {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

/* 按钮组 */
.action-buttons-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 10px;
}

.btn-add-cart {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  color: #0284c7;
  font-weight: bold;
  height: 48px;
  padding: 0 32px;
  border-radius: 24px;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.12);
  transition: all 0.2s;
}

.btn-add-cart:hover {
  background: #e0f2fe;
  border-color: #0284c7;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(2, 132, 199, 0.2);
}

.btn-buy-now {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-weight: bold;
  height: 48px;
  padding: 0 36px;
  border-radius: 24px;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.3);
  transition: all 0.2s;
}

.btn-buy-now:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(2, 132, 199, 0.4);
}

.fav-btn {
  height: 48px;
  padding: 0 20px;
  border-radius: 24px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.fav-btn:hover, .fav-btn.is-faved {
  color: #0284c7;
  border-color: #0284c7;
  background-color: #f0f9ff;
}

/* 4大板块详细卡片 */
.product-detail-tabs-card {
  background: #ffffff;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
}

:deep(.el-tabs--border-card) {
  border: none;
  background: #ffffff;
}

:deep(.el-tabs--border-card > .el-tabs__header) {
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item) {
  font-size: 15px;
  font-weight: bold;
  color: #64748b;
  height: 54px;
  line-height: 54px;
  padding: 0 30px;
}

:deep(.el-tabs--border-card > .el-tabs__header .el-tabs__item.is-active) {
  color: #0284c7;
  background-color: #ffffff;
  border-right-color: #e2e8f0;
  border-left-color: #e2e8f0;
}

.tab-content {
  padding: 30px;
}

/* 图文详情 */
.details-text-lead {
  margin-bottom: 20px;
}

.details-text-lead h3 {
  margin-top: 0;
  color: #0f172a;
}

.details-text-lead p {
  color: #475569;
  line-height: 1.6;
}

.content-full-img {
  width: 100%;
  border-radius: 12px;
  margin-bottom: 16px;
  display: block;
}

/* 规格表格 */
.specs-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.specs-table td {
  padding: 14px 20px;
  border: 1px solid #e2e8f0;
}

.td-key {
  width: 180px;
  background-color: #f8fafc;
  color: #64748b;
  font-weight: bold;
}

.td-val {
  color: #1e293b;
}

/* 买家评价 */
.comments-summary-banner {
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  border-radius: 12px;
  padding: 20px 30px;
  display: flex;
  align-items: center;
  gap: 40px;
  margin-bottom: 30px;
}

.score-box {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.score-num {
  font-size: 40px;
  font-weight: 900;
  color: #f43f5e;
  line-height: 1;
}

.score-lbl {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

.score-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-chip {
  padding: 6px 14px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-chip:hover {
  border-color: #f43f5e;
  color: #f43f5e;
}

.tag-chip.active {
  background-color: #f43f5e;
  border-color: #f43f5e;
  color: #ffffff;
  font-weight: bold;
}

.comment-item-card {
  border-bottom: 1px solid #f1f5f9;
  padding: 24px 0;
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 20px;
}

.comment-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  font-size: 13px;
  font-weight: bold;
  color: #1e293b;
}

.author-vip {
  font-size: 11px;
  color: #0ea5e9;
  background-color: #f0f9ff;
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
}

.comment-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.c-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.c-date {
  font-size: 12px;
  color: #94a3b8;
}

.c-text {
  margin: 0;
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

.c-pics-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.c-pic-thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: transform 0.2s;
}

.c-pic-thumb:hover {
  transform: scale(1.05);
}

.empty-comments {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
  font-size: 14px;
}

/* 售后服务 */
.service-block {
  margin-bottom: 24px;
}

.service-block h4 {
  margin: 0 0 8px 0;
  font-size: 15px;
  color: #0f172a;
}

.service-block p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}
</style>
