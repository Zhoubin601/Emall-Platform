<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue' 
import { 
  Search, ShoppingCart, StarFilled, Ticket, Location, ArrowDown, Right, Service
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

interface Product {
  id: number; name: string; price: number; stock: number;
  picUrl?: string; description: string; status: number;
  categoryId: number; sales: number;      
  promoPrice?: number; promoStartTime?: string; promoEndTime?: string; promoStock?: number;
}

interface Category { id: number; name: string; parentId: number; level: number; }
interface HotSearch { id: number; keyword: string; searchCount: number; }
interface Coupon { id: number; name: string; minAmount: number; discountAmount: number; endTime: string; }
interface Notice { id: number; title: string; content: string; isActive: number; createTime: string; }

const productList = ref<Product[]>([])
const hotSearchList = ref<HotSearch[]>([]) 
const couponList = ref<Coupon[]>([]) 
const bannerList = ref<any[]>([])
const noticeList = ref<Notice[]>([])

const loading = ref(false)
const searchKey = ref('') 
const sortBy = ref('sales')

const allCategories = ref<Category[]>([]) 
const selectedParentId = ref(0)           
const activeCategory = ref(0)             

// 分页状态
const currentPage = ref(1)
const pageSize = ref(12)

// 快报 Tab
const newsTab = ref('hot')

// 城市定位与切换
const currentCity = ref(localStorage.getItem('user_city') || '北京市')
const cityDialogVisible = ref(false)
const hotCities = [
  '北京市', '上海市', '广州市', '深圳市', '杭州市', '成都市',
  '武汉市', '南京市', '西安市', '重庆市', '苏州市', '天津市'
]
const selectCity = (city: string) => {
  currentCity.value = city
  localStorage.setItem('user_city', city)
  cityDialogVisible.value = false
  ElMessage.success(`已成功切换到【${city}】，配送仓储已为您智能就近匹配！`)
}

// 公告弹窗状态
const currentNotice = ref<Notice | null>(null)
const noticeDialogVisible = ref(false)
const allNoticesDialogVisible = ref(false)

const viewNotice = (notice: Notice) => {
  currentNotice.value = notice
  noticeDialogVisible.value = true
}

const openAllNotices = () => {
  allNoticesDialogVisible.value = true
}

// 便民服务引导弹窗
const serviceDialogVisible = ref(false)
const serviceDialogTitle = ref('')
const serviceDialogContent = ref('')

const openServiceGuide = (type: string) => {
  if (type === 'phone') {
    serviceDialogTitle.value = '📱 话费充值与流量直充服务'
    serviceDialogContent.value = 'E-MALL 话费与流量充值中心直连运营商核心通道，支持全国手机号码即时到账。当前正在系统升级与维护中，即将开放自主在线充值，如有紧急充值需求请联系在线客服协助！'
  } else if (type === 'travel') {
    serviceDialogTitle.value = '✈️ 商旅机票与差旅预订通道'
    serviceDialogContent.value = '本专区为企业差旅与高频出行用户提供全球航线特惠直降、专属退改保障服务。系统现已开放企业VIP专属内测，个人通道即将上线！'
  } else if (type === 'hotel') {
    serviceDialogTitle.value = '🏨 酒店住宿精选预订'
    serviceDialogContent.value = '汇集全球优质星级酒店及精选民宿，支持到店付款与信用住免押金权益，相关通道即将接入！'
  } else if (type === 'b2b') {
    serviceDialogTitle.value = '🏢 企业采购与批量采购通道'
    serviceDialogContent.value = '面向企事业单位提供阳光采购、大额团购定制、专票直开及账期服务。如有批量团购需求，请直接点击客服进行人工对接！'
  } else if (type === 'trace') {
    serviceDialogTitle.value = '🛡️ 全球正品溯源保障体系'
    serviceDialogContent.value = 'E-MALL 所有自营商品均经过海关检验与一物一码区块链溯源，消费者收到商品后可刮开防伪涂层或扫描包装二维码查看全链路验真报告。'
  }
  serviceDialogVisible.value = true
}

// 全局唤起客服抽屉
const openCustomerService = () => {
  window.dispatchEvent(new CustomEvent('open-customer-service'))
}

// 秒杀倒计时
const countdownStatusText = ref('本场倒计时：')
const countdownHours = ref('00')
const countdownMinutes = ref('00')
const countdownSeconds = ref('00')
let countdownTimer: any = null

const userStore = useUserStore()
const router = useRouter()
const cartStore = useCartStore()

// 频道栏动态提取前 4 个一级分类
const navChannelCategories = computed(() => {
  return allCategories.value.filter(c => c.level === 1).slice(0, 4)
})

// 多级分类数据关联
const level1Categories = computed(() => [
  { id: 0, name: '全部商品分类', parentId: 0, level: 1, subTags: '数码 / 办公 / 服饰 / 居家' },
  ...allCategories.value.filter(c => c.level === 1).map(c => ({
    ...c,
    subTags: c.name.includes('数码') ? '手机 / 耳机 / 智能数码' :
             c.name.includes('电脑') ? '笔记本 / 外设 / 显示器' :
             c.name.includes('服饰') ? '男装 / 女装 / 潮流运动' :
             c.name.includes('日用') || c.name.includes('家') ? '个护 / 居家 / 冲饮美食' : '品质生活 / 热销优选'
  }))
])

const level2Categories = computed(() => {
  if (selectedParentId.value === 0) return []
  return allCategories.value.filter(c => c.level === 2 && c.parentId === selectedParentId.value)
})

const fetchDynamicCategories = async () => { 
  try { 
    allCategories.value = await request.get<any, Category[]>('/category/list') 
  } catch (e) {} 
}

const fetchHotSearches = async () => { 
  try { 
    hotSearchList.value = await request.get<any, HotSearch[]>('/product/hotSearches') 
  } catch (e) {} 
}

const fetchCoupons = async () => { 
  try { 
    couponList.value = await request.get<any, Coupon[]>('/coupon/list') 
  } catch (e) {} 
}

const fetchAds = async () => {
  try { 
    bannerList.value = await request.get('/ad/active') 
  } catch (e) {
    console.error('拉取轮播图失败')
  }
}

// ✨ 真实拉取系统激活公告
const fetchNotices = async () => {
  try {
    const res = await request.get<any, Notice[]>('/interaction/notice/active')
    noticeList.value = res || []
  } catch (e) {
    console.error('拉取公告失败')
  }
}

const fetchProducts = async () => {
  loading.value = true
  try {
    const params = {
      keyword: searchKey.value,
      categoryId: activeCategory.value === 0 ? null : activeCategory.value,
      sortBy: sortBy.value === 'promo' ? '' : sortBy.value 
    }
    const res = await request.get<any, Product[]>('/product/list', { params })
    productList.value = res || []
  } catch (error) { 
    ElMessage.error('获取商品失败') 
  } finally { 
    loading.value = false 
  }
}

// 检查秒杀是否进行中
const isFlashSaleActive = (product: Product) => {
  if (!product.promoStartTime || !product.promoEndTime || !product.promoPrice) return false
  const now = new Date().getTime()
  const start = new Date(product.promoStartTime.replace(/-/g, '/')).getTime()
  const end = new Date(product.promoEndTime.replace(/-/g, '/')).getTime()
  return now >= start && now <= end
}

// 检查秒杀是否未开始（预告）
const isFlashSaleUpcoming = (product: Product) => {
  if (!product.promoStartTime || !product.promoEndTime || !product.promoPrice) return false
  const now = new Date().getTime()
  const start = new Date(product.promoStartTime.replace(/-/g, '/')).getTime()
  return now < start
}

// 筛选与排序
const filteredProducts = computed(() => {
  let list = [...productList.value]
  if (sortBy.value === 'promo') {
    list = list.filter(p => isFlashSaleActive(p) || (p.promoPrice && p.promoPrice > 0))
  } else if (sortBy.value === 'price_asc') {
    list.sort((a, b) => a.price - b.price)
  } else if (sortBy.value === 'price_desc') {
    list.sort((a, b) => b.price - a.price)
  }
  return list
})

// ✨ 真实秒杀精选产品计算（严格只筛选处于进行中或预告中的真实秒杀商品）
const seckillProducts = computed(() => {
  return productList.value.filter(p => isFlashSaleActive(p) || isFlashSaleUpcoming(p))
})

// ✨ 真实秒杀抢购进度计算（精准基于秒杀已售量与秒杀限量库存）
const calculateProgress = (product: Product) => {
  const promoSold = (product as any).promoSold || 0
  const promoLimit = (product as any).promoStock || product.stock || 0
  if (promoLimit <= 0 || promoSold <= 0) return 0
  return Math.min(100, Math.round((promoSold / promoLimit) * 100))
}

// 分页切片计算
const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredProducts.value.slice(start, start + pageSize.value)
})

// 换页事件
const handlePageChange = (page: number) => {
  currentPage.value = page
  scrollToProductSection()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  scrollToProductSection()
}

const scrollToProductSection = () => {
  const el = document.getElementById('emall-product-section')
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

const executeSearch = async (keyword?: string) => {
  if (keyword) searchKey.value = keyword 
  currentPage.value = 1
  if (searchKey.value.trim()) {
    request.post(`/product/searchRecord?keyword=${encodeURIComponent(searchKey.value)}`).then(() => fetchHotSearches())
  }
  fetchProducts()
}

const handleClaimCoupon = async (couponId: number) => {
  if (!userStore.userInfo) { 
    ElMessage.warning('请先登录后再来抢神券哦！')
    return router.push('/login') 
  }
  try {
    const res = await request.post(`/coupon/claim?userId=${userStore.userInfo.id}&couponId=${couponId}`)
    if ((res as unknown as string).includes('成功')) {
      ElMessage.success('🎉 ' + res)
    } else {
      ElMessage.warning('⚠️ ' + res)
    }
  } catch (error) { 
    ElMessage.error('领取失败，请检查网络') 
  }
}

const handleLevel1Click = (id: number) => { 
  selectedParentId.value = id
  activeCategory.value = id
  currentPage.value = 1
  fetchProducts() 
  scrollToProductSection()
}

const handleLevel2Click = (id: number) => { 
  activeCategory.value = id
  currentPage.value = 1
  fetchProducts() 
  scrollToProductSection()
}

const goToDetail = (id: number) => router.push(`/product/${id}`)

const handleQuickAdd = async (product: Product) => {
  if (product.promoStartTime && product.promoEndTime) {
    const now = new Date().getTime()
    const start = new Date(product.promoStartTime.replace(/-/g, '/')).getTime()
    if (now < start) {
      return ElMessage.warning('秒杀活动还未开始哦，请去详情页蹲点吧！')
    }
  }

  try {
    const skus: any[] = await request.get(`/product/skus/${product.id}`)
    if (!skus || skus.length === 0) {
      return ElMessage.warning('该商品商家还在配置规格中，暂不可售！')
    }

    const defaultSku = skus[0]
    if (defaultSku.stock <= 0) {
      return ElMessage.warning(`该商品默认规格 [${defaultSku.specName}] 已售罄！`)
    }

    let finalPrice = defaultSku.price
    if (isFlashSaleActive(product) && product.promoPrice) {
      finalPrice = product.promoPrice
    }

    const itemToAdd = {
      ...product,
      price: finalPrice,
      stock: defaultSku.stock,
      picUrl: defaultSku.picUrl || product.picUrl
    }

    cartStore.addToCart(itemToAdd, 1, defaultSku.specName, defaultSku.id)
    ElMessage.success(`已将 1 件 "${defaultSku.specName}" 加入购物车！`)
  } catch (error) {
    ElMessage.error('加购失败，请稍后重试')
  }
}

const handleBannerClick = (url: string) => {
  if (url) {
    if (url.startsWith('http')) {
      window.open(url, '_blank')
    } else {
      router.push(url)
    }
  }
}

// ✨ 真实倒计时时钟：基于商品真实活动时间进行动态计算
const startCountdown = () => {
  const updateTick = () => {
    const now = new Date().getTime()
    let targetTime: number | null = null

    // 1. 查找正在进行的秒杀中最近的结束时间
    const activeEnds = productList.value
      .filter(p => isFlashSaleActive(p))
      .map(p => new Date(p.promoEndTime!.replace(/-/g, '/')).getTime())

    if (activeEnds.length > 0) {
      targetTime = Math.min(...activeEnds)
      countdownStatusText.value = '本场距结束：'
    } else {
      // 2. 查找即将开始的秒杀中最近的开始时间
      const upcomingStarts = productList.value
        .filter(p => isFlashSaleUpcoming(p))
        .map(p => new Date(p.promoStartTime!.replace(/-/g, '/')).getTime())
      
      if (upcomingStarts.length > 0) {
        targetTime = Math.min(...upcomingStarts)
        countdownStatusText.value = '下场距开抢：'
      } else {
        // 3. 兜底为今日特惠场次倒计时（至今日 24:00）
        const midnight = new Date()
        midnight.setHours(23, 59, 59, 999)
        targetTime = midnight.getTime()
        countdownStatusText.value = '今日特惠剩：'
      }
    }

    const diff = Math.max(0, Math.floor((targetTime - now) / 1000))
    const h = Math.floor(diff / 3600)
    const m = Math.floor((diff % 3600) / 60)
    const s = diff % 60
    countdownHours.value = h.toString().padStart(2, '0')
    countdownMinutes.value = m.toString().padStart(2, '0')
    countdownSeconds.value = s.toString().padStart(2, '0')
  }

  updateTick()
  countdownTimer = setInterval(updateTick, 1000)
}

onMounted(() => {
  fetchDynamicCategories()
  fetchHotSearches()
  fetchCoupons() 
  fetchAds()
  fetchNotices()
  fetchProducts()
  startCountdown()
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<template>
  <div class="emall-layout">
    <!-- 1. 顶部快捷导航栏 (Top Shortcut Bar) -->
    <div class="emall-topbar">
      <div class="emall-container topbar-content">
        <div class="topbar-left">
          <span class="location-item">
            <el-icon class="loc-icon"><Location /></el-icon>
            <span>{{ currentCity }}</span>
            <span class="loc-switch" @click="cityDialogVisible = true">[切换]</span>
          </span>
        </div>
        <div class="topbar-right">
          <div class="topbar-user">
            <template v-if="userStore.userInfo">
              <span class="user-greeting">你好，{{ userStore.userInfo.nickname || userStore.userInfo.username }}</span>
              <el-tag size="small" type="primary" effect="plain" class="vip-tag">VIP尊享会员</el-tag>
              <span class="divider">|</span>
              <span class="link-item logout-link" @click="userStore.logout(); router.push('/login')">退出</span>
            </template>
            <template v-else>
              <span class="link-item active-link" @click="router.push('/login')">你好，请登录</span>
              <span class="link-item reg-link" @click="router.push('/register')">免费注册</span>
            </template>
          </div>
          <span class="divider">|</span>
          <span class="link-item" @click="router.push('/orders')">我的订单</span>
          <span class="divider">|</span>
          <el-dropdown trigger="hover">
            <span class="link-item drop-link">我的商城 <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu class="emall-drop-menu">
                <el-dropdown-item @click="router.push('/profile')">个人资料</el-dropdown-item>
                <el-dropdown-item @click="router.push('/orders')">全部订单</el-dropdown-item>
                <el-dropdown-item @click="router.push('/favorites')">我的收藏</el-dropdown-item>
                <el-dropdown-item @click="router.push('/address')">收货地址</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="divider">|</span>
          <span class="link-item" @click="openServiceGuide('b2b')">企业采购</span>
          <span class="divider">|</span>
          <el-dropdown trigger="hover">
            <span class="link-item drop-link">客户服务 <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu class="emall-drop-menu">
                <el-dropdown-item @click="router.push('/comments')">我的评价</el-dropdown-item>
                <el-dropdown-item @click="openAllNotices()">平台公告 & 帮助</el-dropdown-item>
                <el-dropdown-item @click="openServiceGuide('trace')">正品与售后</el-dropdown-item>
                <el-dropdown-item @click="openCustomerService()">在线客服</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="divider">|</span>
          <span class="link-item" @click="openAllNotices()">网站导航</span>
        </div>
      </div>
    </div>

    <!-- 2. 主头部与搜索区 (Main Header & Search Bar) -->
    <header class="emall-main-header">
      <div class="emall-container header-inner">
        <!-- E-MALL 品牌 Logo -->
        <div class="emall-logo-box" @click="router.push('/')">
          <div class="emall-brand-title">E-MALL</div>
          <div class="emall-brand-slogan">全球严选 · 品质电商</div>
        </div>

        <!-- 搜索框 -->
        <div class="emall-search-zone">
          <div class="emall-search-form">
            <input 
              v-model="searchKey" 
              type="text" 
              class="emall-search-input" 
              placeholder="搜一搜商品名称、型号、分类或关键词..." 
              @keyup.enter="executeSearch()"
            />
            <button class="emall-search-btn" @click="executeSearch()">
              <el-icon :size="16"><Search /></el-icon>
              <span>搜索</span>
            </button>
          </div>
          <!-- 热门搜索推荐词 -->
          <div class="emall-hot-words">
            <span class="hot-lead"><el-icon><StarFilled /></el-icon> 热门搜索：</span>
            <span 
              v-for="(item, idx) in hotSearchList" 
              :key="item.id" 
              class="hot-tag" 
              :class="{ 'first-hot': idx === 0 }"
              @click="executeSearch(item.keyword)"
            >
              {{ item.keyword }}
            </span>
          </div>
        </div>

        <!-- 我的购物车按钮 -->
        <div class="emall-cart-box" @click="router.push('/cart')">
          <div class="cart-inner">
            <el-icon class="cart-icon"><ShoppingCart /></el-icon>
            <span class="cart-text">我的购物车</span>
            <span class="cart-count-badge">{{ cartStore.totalCount }}</span>
          </div>
        </div>
      </div>
    </header>

    <!-- 3. 频道主导航条 (Channel Bar) -->
    <nav class="emall-channel-bar">
      <div class="emall-container channel-inner">
        <div class="category-header-tab" @click="handleLevel1Click(0)">
          <span class="cat-tab-icon">☰</span>
          <span>全部商品分类</span>
        </div>
        <ul class="channel-nav-list">
          <li class="channel-item" :class="{ active: sortBy === 'sales' && activeCategory === 0 }" @click="sortBy = 'sales'; activeCategory = 0; selectedParentId = 0; currentPage = 1; fetchProducts()">首页精选</li>
          <li class="channel-item highlight-item" :class="{ active: sortBy === 'promo' }" @click="sortBy = 'promo'; currentPage = 1; fetchProducts()">⚡ 限时秒杀</li>
          <li class="channel-item" @click="scrollToProductSection()">领券中心</li>
          <li class="channel-item" :class="{ active: sortBy === 'new' }" @click="sortBy = 'new'; currentPage = 1; fetchProducts()">新品首发</li>
          <!-- 动态频道分类列表 -->
          <li 
            v-for="cat in navChannelCategories" 
            :key="'nav-cat-' + cat.id" 
            class="channel-item" 
            :class="{ active: activeCategory === cat.id }"
            @click="handleLevel1Click(cat.id)"
          >
            {{ cat.name }}
          </li>
          <li class="channel-item" @click="ElMessage.info('尊享VIP会员：全场自营专属特权，折上再享95折！')">VIP会员</li>
        </ul>
      </div>
    </nav>

    <!-- 4. 首屏黄金三栏网格 (Hero 3-Column Grid) -->
    <div class="emall-container hero-wrapper">
      <!-- 左栏：多级分类导航菜单 -->
      <aside class="hero-left-menu">
        <ul class="emall-cat-tree">
          <li 
            v-for="cat in level1Categories" 
            :key="cat.id" 
            class="cat-tree-item"
            :class="{ active: selectedParentId === cat.id }"
            @click="handleLevel1Click(cat.id)"
          >
            <div class="cat-main-row">
              <span class="cat-name">{{ cat.name }}</span>
              <el-icon class="cat-arrow"><Right /></el-icon>
            </div>
            <div class="cat-sub-text">{{ cat.subTags }}</div>
          </li>
        </ul>
      </aside>

      <!-- 中栏：主轮播大图与配套推广 -->
      <section class="hero-center-carousel">
        <el-carousel v-if="bannerList.length > 0" height="420px" class="emall-carousel" arrow="hover" trigger="click">
          <el-carousel-item v-for="banner in bannerList" :key="banner.id" @click="handleBannerClick(banner.linkUrl)">
            <img :src="banner.picUrl" class="carousel-img" :title="banner.title" loading="lazy" />
          </el-carousel-item>
        </el-carousel>
        <div v-else class="empty-carousel-box">
          <div class="loading-text">正在为您准备精彩热卖活动...</div>
        </div>

        <!-- 轮播图下方二级子分类快速筛选 -->
        <div class="sub-cat-chips" v-if="level2Categories.length > 0">
          <span class="sub-chip-label">热门推荐：</span>
          <span 
            v-for="sub in level2Categories" 
            :key="sub.id" 
            class="sub-chip-pill"
            :class="{ active: activeCategory === sub.id }"
            @click="handleLevel2Click(sub.id)"
          >
            {{ sub.name }}
          </span>
        </div>
      </section>

      <!-- 右栏：用户服务与便民矩阵 -->
      <aside class="hero-right-panel">
        <!-- 用户身份卡 -->
        <div class="emall-user-card">
          <div class="user-avatar-row">
            <el-avatar :size="50" :src="userStore.userInfo?.avatar?.includes('default-avatar.png') ? '' : userStore.userInfo?.avatar" class="emall-user-avatar">
              {{ userStore.userInfo?.username?.charAt(0).toUpperCase() || 'U' }}
            </el-avatar>
            <div class="user-greeting-box">
              <div class="greet-title">Hi~ {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '欢迎来到 E-MALL' }}</div>
              <div class="greet-sub" v-if="userStore.userInfo">VIP尊享会员 · 畅享专属特权</div>
              <div class="greet-sub" v-else>注册立享新人专属 888元 神券包</div>
            </div>
          </div>
          <div class="user-btn-row" v-if="!userStore.userInfo">
            <el-button type="primary" size="small" class="emall-primary-btn" @click="router.push('/login')">登录</el-button>
            <el-button size="small" class="emall-plain-btn" @click="router.push('/register')">注册</el-button>
            <el-button size="small" class="emall-welfare-btn" @click="router.push('/login')">新人福利</el-button>
          </div>
          <div class="user-stats-row" v-else>
            <div class="stat-col" @click="router.push('/orders')">
              <div class="stat-num">📦</div>
              <div class="stat-lbl">我的订单</div>
            </div>
            <div class="stat-col" @click="router.push('/favorites')">
              <div class="stat-num">⭐</div>
              <div class="stat-lbl">收藏夹</div>
            </div>
            <div class="stat-col" @click="router.push('/cart')">
              <div class="stat-num">{{ cartStore.totalCount }}</div>
              <div class="stat-lbl">购物车</div>
            </div>
          </div>
        </div>

        <!-- 商城快报与真实系统公告切换 -->
        <div class="emall-news-box">
          <div class="news-tab-header">
            <span class="tab-title" :class="{ active: newsTab === 'hot' }" @click="newsTab = 'hot'">最新快报</span>
            <span class="tab-title" :class="{ active: newsTab === 'promo' }" @click="newsTab = 'promo'">平台公告</span>
            <span class="more-link" @click="openAllNotices()">更多 ❯</span>
          </div>
          <ul class="news-list" v-if="newsTab === 'hot'">
            <template v-if="noticeList.length > 0">
              <li 
                v-for="notice in noticeList.slice(0, 3)" 
                :key="'hot-n-' + notice.id" 
                class="news-clickable-item"
                @click="viewNotice(notice)"
              >
                <span class="tag-accent">HOT</span>
                <span class="news-text-ellipsis">{{ notice.title }}</span>
              </li>
            </template>
            <template v-else>
              <li><span class="tag-accent">HOT</span>【首发】全系新品限时直降上线</li>
              <li><span class="tag-blue">速运</span>【配送】全国主要城市支持官方自营次日达</li>
              <li><span class="tag-accent">特惠</span>【数码】极客周大牌装备立减热卖中</li>
            </template>
          </ul>
          <ul class="news-list" v-else>
            <template v-if="noticeList.length > 0">
              <li 
                v-for="notice in noticeList.slice(0, 3)" 
                :key="'promo-n-' + notice.id" 
                class="news-clickable-item"
                @click="viewNotice(notice)"
              >
                <span class="tag-blue">公告</span>
                <span class="news-text-ellipsis">{{ notice.title }}</span>
              </li>
            </template>
            <template v-else>
              <li><span class="tag-blue">通知</span>暂无系统公告发布</li>
            </template>
          </ul>
        </div>

        <!-- 便民服务 6 宫格 -->
        <div class="emall-service-matrix">
          <div class="service-cell" @click="openServiceGuide('phone')">
            <div class="service-icon">📱</div>
            <div class="service-text">话费充值</div>
          </div>
          <div class="service-cell" @click="openServiceGuide('travel')">
            <div class="service-icon">✈️</div>
            <div class="service-text">机票商旅</div>
          </div>
          <div class="service-cell" @click="openServiceGuide('hotel')">
            <div class="service-icon">🏨</div>
            <div class="service-text">酒店住宿</div>
          </div>
          <div class="service-cell" @click="scrollToProductSection()">
            <div class="service-icon">🎁</div>
            <div class="service-text">礼品卡券</div>
          </div>
          <div class="service-cell" @click="openServiceGuide('b2b')">
            <div class="service-icon">🏢</div>
            <div class="service-text">企业采购</div>
          </div>
          <div class="service-cell" @click="openServiceGuide('trace')">
            <div class="service-icon">🛡️</div>
            <div class="service-text">正品溯源</div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 5. 限时秒杀专区 (Flash Sale Zone) -->
    <div class="emall-container emall-seckill-section" v-if="seckillProducts.length > 0">
      <div class="seckill-header">
        <div class="seckill-title-box">
          <span class="seckill-logo-text">⚡ 限时秒杀</span>
          <span class="seckill-sub-title">FLASH DEALS · 超值特惠</span>
        </div>
        <div class="seckill-countdown-box">
          <span class="countdown-lead">{{ countdownStatusText }}</span>
          <span class="time-block">{{ countdownHours }}</span>
          <span class="colon">:</span>
          <span class="time-block">{{ countdownMinutes }}</span>
          <span class="colon">:</span>
          <span class="time-block">{{ countdownSeconds }}</span>
        </div>
      </div>
      <div class="seckill-body">
        <div class="seckill-grid">
          <div 
            v-for="item in seckillProducts" 
            :key="'seckill-' + item.id" 
            class="seckill-card"
            @click="goToDetail(item.id)"
          >
            <div class="seckill-img-wrap">
              <img :src="item.picUrl" :alt="item.name" class="seckill-img" loading="lazy" />
              <span class="seckill-badge">{{ isFlashSaleActive(item) ? '秒杀中' : (isFlashSaleUpcoming(item) ? '即将开抢' : '特惠价') }}</span>
            </div>
            <div class="seckill-info">
              <div class="seckill-name">{{ item.name }}</div>
              <div class="seckill-price-row">
                <div class="price-now">
                  <span class="currency">¥</span>
                  <span class="num">{{ isFlashSaleActive(item) ? item.promoPrice : (item.promoPrice || item.price) }}</span>
                </div>
                <div class="price-origin" v-if="isFlashSaleActive(item) || item.promoPrice">¥{{ item.price }}</div>
              </div>
              <div class="seckill-progress">
                <div class="prog-bar"><div class="prog-fill" :style="{ width: calculateProgress(item) + '%' }"></div></div>
                <span class="prog-text">
                  <template v-if="calculateProgress(item) > 0">已抢 {{ calculateProgress(item) }}%</template>
                  <template v-else-if="item.promoStock">已抢 0% · 限量{{ item.promoStock }}件</template>
                  <template v-else>已抢 0% · 热抢中</template>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. 领券中心专区 (Coupon Center) -->
    <div class="emall-container emall-coupon-zone" v-if="couponList.length > 0">
      <div class="zone-title">
        <el-icon color="#0284c7" size="22"><Ticket /></el-icon>
        <span class="main-txt">领券中心</span>
        <span class="sub-txt">全场通用神券 · 先领券再下单折上折</span>
      </div>
      <div class="coupon-grid">
        <div class="emall-coupon-item" v-for="coupon in couponList" :key="coupon.id">
          <div class="coupon-left">
            <div class="c-val"><span class="sign">¥</span><span class="big">{{ coupon.discountAmount }}</span></div>
            <div class="c-rule">满 {{ coupon.minAmount }} 元可用</div>
          </div>
          <div class="coupon-mid">
            <div class="c-title">{{ coupon.name }}</div>
            <div class="c-scope">全品类官方自营通用</div>
          </div>
          <div class="coupon-right" @click="handleClaimCoupon(coupon.id)">
            <span class="btn-claim">立即领取</span>
          </div>
          <div class="tooth-top"></div>
          <div class="tooth-bottom"></div>
        </div>
      </div>
    </div>

    <!-- 7. 为您推荐商品流与专业分页 (Product Feeds & Pagination) -->
    <section class="emall-container emall-product-feed-section" id="emall-product-section">
      <!-- 频道筛选 Tab 条 -->
      <div class="feed-filter-bar">
        <div class="filter-tab-group">
          <div 
            class="filter-tab" 
            :class="{ active: sortBy === 'sales' }" 
            @click="sortBy = 'sales'; currentPage = 1; fetchProducts()"
          >
            <el-icon><StarFilled /></el-icon>
            <span>为您推荐</span>
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: sortBy === 'new' }" 
            @click="sortBy = 'new'; currentPage = 1; fetchProducts()"
          >
            <span>✨ 新品上市</span>
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: sortBy === 'promo' }" 
            @click="sortBy = 'promo'; currentPage = 1; fetchProducts()"
          >
            <span>⚡ 特惠降价</span>
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: sortBy === 'price_asc' }" 
            @click="sortBy = 'price_asc'; currentPage = 1; fetchProducts()"
          >
            <span>价格从低到高 ↑</span>
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: sortBy === 'price_desc' }" 
            @click="sortBy = 'price_desc'; currentPage = 1; fetchProducts()"
          >
            <span>价格从高到低 ↓</span>
          </div>
        </div>
        <div class="feed-stat">共 <span class="highlight-count">{{ filteredProducts.length }}</span> 件严选好物</div>
      </div>

      <!-- 骨架屏加载态 -->
      <div class="emall-products-grid" v-if="loading">
        <div class="emall-product-card skeleton-box" v-for="i in 8" :key="'skel-' + i">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="height: 220px; width: 100%;" />
              <div style="padding: 12px;">
                <el-skeleton-item variant="h3" style="width: 80%; height: 18px; margin-bottom: 8px;" />
                <el-skeleton-item variant="text" style="width: 100%; height: 14px; margin-bottom: 12px;" />
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <el-skeleton-item variant="text" style="width: 40%; height: 22px;" />
                  <el-skeleton-item variant="circle" style="width: 28px; height: 28px;" />
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <!-- 真实商品卡片网格 -->
      <div class="emall-products-grid" v-else>
        <div 
          v-for="product in pagedProducts" 
          :key="product.id" 
          class="emall-product-card"
          @click="goToDetail(product.id)"
        >
          <div class="img-wrapper">
            <img v-if="product.picUrl" :src="product.picUrl" :alt="product.name" class="p-img" loading="lazy" />
            <div v-else class="no-p-img">暂无商品图</div>
            <span class="tag-ziying">官方自营</span>
            <span class="tag-flash" v-if="isFlashSaleActive(product)">限时特惠</span>
          </div>
          <div class="p-info">
            <div class="p-price-row">
              <span class="p-currency">¥</span>
              <span class="p-integer">{{ isFlashSaleActive(product) ? product.promoPrice : Math.floor(product.price) }}</span>
              <span class="p-decimal" v-if="!isFlashSaleActive(product)">.{{ (product.price % 1).toFixed(2).substring(2) }}</span>
              <span class="p-orig" v-if="isFlashSaleActive(product) && product.promoPrice">¥{{ product.price }}</span>
            </div>
            <div class="p-title" :title="product.name">
              <span class="title-tag-blue">自营</span>
              {{ product.name }}
            </div>
            <div class="p-desc">{{ product.description }}</div>
            <div class="p-badges">
              <span class="badge-ship">官方速运</span>
              <span class="badge-coupon" v-if="couponList.length > 0">满减券</span>
            </div>
            <div class="p-footer">
              <span class="p-comment-count">{{ product.sales > 0 ? (product.sales + '+ 人已买') : '官方优选' }}</span>
              <button class="add-cart-btn" @click.stop="handleQuickAdd(product)" title="加入购物车">
                <el-icon><ShoppingCart /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 空数据提示 -->
      <div v-if="filteredProducts.length === 0 && !loading" class="empty-products-box">
        <div class="empty-icon">🛒</div>
        <div class="empty-tip">{{ sortBy === 'promo' ? '当前暂无秒杀活动商品' : '没有找到匹配的商品，换个搜索词试试吧~' }}</div>
        <el-button type="primary" size="small" @click="searchKey = ''; activeCategory = 0; selectedParentId = 0; sortBy = 'sales'; fetchProducts()">查看全部商品</el-button>
      </div>

      <!-- 8. 专业级 Element Plus 分页控制器 (Pagination) -->
      <div class="emall-pagination-wrapper" v-if="filteredProducts.length > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 36, 48]"
          :total="filteredProducts.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <!-- 9. 品质服务保障底栏与多列页脚 (Footer) -->
    <footer class="emall-footer">
      <!-- 四大核心品质保障 -->
      <div class="footer-guarantee-bar">
        <div class="emall-container guarantee-inner">
          <div class="guarantee-item">
            <div class="g-circle">品</div>
            <div class="g-text">
              <div class="g-title">品类齐全</div>
              <div class="g-sub">全球严选 · 轻松购物</div>
            </div>
          </div>
          <div class="guarantee-item">
            <div class="g-circle">快</div>
            <div class="g-text">
              <div class="g-title">多仓直发</div>
              <div class="g-sub">官方速运 · 极速送达</div>
            </div>
          </div>
          <div class="guarantee-item">
            <div class="g-circle">好</div>
            <div class="g-text">
              <div class="g-title">正品行货</div>
              <div class="g-sub">官方自营 · 精致服务</div>
            </div>
          </div>
          <div class="guarantee-item">
            <div class="g-circle">省</div>
            <div class="g-text">
              <div class="g-title">天天特惠</div>
              <div class="g-sub">大额神券 · 畅选无忧</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 多列服务导航 -->
      <div class="emall-container footer-links-zone">
        <div class="link-col">
          <div class="col-title">购物指南</div>
          <div class="col-link">购物流程</div>
          <div class="col-link">会员介绍</div>
          <div class="col-link">生活旅行</div>
          <div class="col-link">常见问题</div>
        </div>
        <div class="link-col">
          <div class="col-title">配送方式</div>
          <div class="col-link">上门自提</div>
          <div class="col-link">极速限时达</div>
          <div class="col-link">配送服务查询</div>
          <div class="col-link">配送费收取标准</div>
        </div>
        <div class="link-col">
          <div class="col-title">支付方式</div>
          <div class="col-link">货到付款</div>
          <div class="col-link">在线支付</div>
          <div class="col-link">分期付款</div>
          <div class="col-link">企业转账</div>
        </div>
        <div class="link-col">
          <div class="col-title">售后服务</div>
          <div class="col-link">售后政策</div>
          <div class="col-link">价格保护</div>
          <div class="col-link">退款说明</div>
          <div class="col-link">返修/退换货</div>
        </div>
        <div class="link-col">
          <div class="col-title">特色服务</div>
          <div class="col-link">积分商城</div>
          <div class="col-link">DIY 装机</div>
          <div class="col-link">延保服务</div>
          <div class="col-link">E-MALL 通信</div>
        </div>
      </div>

      <!-- 版权与备案 -->
      <div class="footer-copyright">
        <div class="emall-container copy-inner">
          <p>© 2026 E-MALL 全球品质电商平台 版权所有 | 沪ICP备12345678号 | 公网安备31010002000088号</p>
          <p>客户服务热线：400-800-8800 | 消费者维权热线：12315</p>
        </div>
      </div>
    </footer>

    <!-- 10. 移动端底部快捷导航栏 (Mobile Tabbar) -->
    <nav class="mobile-tabbar">
      <div class="tab-item active" @click="router.push('/')">
        <span class="tab-icon">🏠</span>
        <span class="tab-label">首页</span>
      </div>
      <div class="tab-item" @click="router.push('/cart')">
        <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0" class="tab-badge">
          <span class="tab-icon">🛒</span>
        </el-badge>
        <span class="tab-label">购物车</span>
      </div>
      <div class="tab-item" @click="router.push('/orders')">
        <span class="tab-icon">📦</span>
        <span class="tab-label">订单</span>
      </div>
      <div class="tab-item" @click="router.push(userStore.userInfo ? '/profile' : '/login')">
        <span class="tab-icon">👤</span>
        <span class="tab-label">{{ userStore.userInfo ? '我的' : '登录' }}</span>
      </div>
    </nav>

    <!-- 11. 城市切换对话框 (City Switcher Dialog) -->
    <el-dialog v-model="cityDialogVisible" title="切换当前配送城市" width="460px" destroy-on-close class="emall-custom-dialog">
      <div class="city-dialog-body">
        <div class="current-city-tip">
          当前定位城市：<span class="city-highlight">{{ currentCity }}</span>
        </div>
        <div class="city-section-title">热门城市推荐</div>
        <div class="city-tags-grid">
          <span 
            v-for="city in hotCities" 
            :key="city" 
            class="city-chip" 
            :class="{ active: currentCity === city }"
            @click="selectCity(city)"
          >
            {{ city }}
          </span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cityDialogVisible = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 12. 系统公告详情对话框 (Notice Detail Dialog) -->
    <el-dialog v-model="noticeDialogVisible" :title="currentNotice?.title || '系统公告详情'" width="560px" destroy-on-close class="emall-custom-dialog">
      <div class="notice-detail-box" v-if="currentNotice">
        <div class="notice-meta-bar">
          <span class="notice-badge">平台官方</span>
          <span class="notice-time">发布时间：{{ currentNotice.createTime ? currentNotice.createTime.replace('T', ' ').substring(0, 19) : '刚刚' }}</span>
        </div>
        <div class="notice-article-content">
          {{ currentNotice.content }}
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="noticeDialogVisible = false">我已了解</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 13. 全部公告列表对话框 (All Notices Dialog) -->
    <el-dialog v-model="allNoticesDialogVisible" title="E-MALL 平台公告与帮助中心" width="650px" destroy-on-close class="emall-custom-dialog">
      <div class="all-notices-container">
        <div v-if="noticeList.length === 0" class="empty-notices">
          暂无官方公告发布
        </div>
        <div 
          v-for="n in noticeList" 
          :key="'list-n-' + n.id" 
          class="notice-list-card"
          @click="viewNotice(n); allNoticesDialogVisible = false"
        >
          <div class="nl-header">
            <span class="nl-title">{{ n.title }}</span>
            <span class="nl-time">{{ n.createTime ? n.createTime.substring(0, 10) : '' }}</span>
          </div>
          <div class="nl-summary">{{ n.content }}</div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="allNoticesDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 14. 便民服务与专属人工通道对话框 (Service Guide Dialog) -->
    <el-dialog v-model="serviceDialogVisible" :title="serviceDialogTitle" width="520px" destroy-on-close class="emall-custom-dialog">
      <div class="service-dialog-content">
        <div class="service-intro-box">
          <p class="service-p">{{ serviceDialogContent }}</p>
        </div>
        <div class="service-channel-box">
          <div class="sc-title">如需进一步业务办理或人工协助：</div>
          <div class="sc-action-row">
            <el-button type="primary" class="sc-btn" @click="openCustomerService(); serviceDialogVisible = false">
              <el-icon><Service /></el-icon>
              <span>联系在线人工客服</span>
            </el-button>
            <el-button class="sc-plain-btn" @click="serviceDialogVisible = false">
              我知道了
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ================= E-MALL 经典蓝调设计变量 ================= */
.emall-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  color: #334155;
}

.emall-container {
  width: 1200px;
  max-width: 96%;
  margin: 0 auto;
}

/* ================= 1. 顶部快捷导航栏 ================= */
.emall-topbar {
  background-color: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
  height: 32px;
  line-height: 32px;
  font-size: 12px;
  color: #64748b;
}
.topbar-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.location-item {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #475569;
}
.loc-icon {
  color: #0284c7;
  font-size: 14px;
}
.loc-switch {
  color: #0284c7;
  cursor: pointer;
  margin-left: 2px;
}
.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.topbar-user {
  display: flex;
  align-items: center;
  gap: 6px;
}
.vip-tag {
  height: 18px;
  line-height: 16px;
  padding: 0 6px;
  font-size: 10px;
  font-weight: bold;
  background: #e0f2fe;
  color: #0284c7;
  border-color: #bae6fd;
}
.link-item {
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}
.link-item:hover, .active-link {
  color: #0284c7;
}
.reg-link {
  color: #0284c7;
  margin-left: 4px;
  font-weight: 500;
}
.divider {
  color: #cbd5e1;
  font-size: 10px;
}
.drop-link {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ================= 2. 主头部与搜索区 ================= */
.emall-main-header {
  background: #fff;
  padding: 22px 0 16px 0;
  border-bottom: 1px solid #f1f5f9;
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 25px;
}
.emall-logo-box {
  cursor: pointer;
  flex-shrink: 0;
}
.emall-brand-title {
  font-size: 32px;
  font-weight: 900;
  background: linear-gradient(to right, #0284c7, #38bdf8);
  -webkit-background-clip: text;
  color: transparent;
  letter-spacing: 1.5px;
  line-height: 1;
}
.emall-brand-slogan {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  letter-spacing: 1px;
}
.emall-search-zone {
  flex: 1;
  max-width: 580px;
}
.emall-search-form {
  display: flex;
  height: 42px;
  border: 2px solid #0284c7;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.08);
}
.emall-search-input {
  flex: 1;
  border: none;
  outline: none;
  padding: 0 16px;
  font-size: 14px;
  color: #334155;
}
.emall-search-btn {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  border: none;
  padding: 0 25px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: opacity 0.2s;
}
.emall-search-btn:hover {
  opacity: 0.92;
}
.emall-hot-words {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
  flex-wrap: wrap;
}
.hot-lead {
  color: #0284c7;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 2px;
}
.hot-tag {
  cursor: pointer;
  transition: color 0.2s;
}
.hot-tag:hover {
  color: #0284c7;
}
.first-hot {
  color: #0284c7;
  font-weight: bold;
}
.emall-cart-box {
  flex-shrink: 0;
  cursor: pointer;
}
.cart-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 18px;
  border: 1px solid #0284c7;
  background: #f0f9ff;
  border-radius: 20px;
  color: #0284c7;
  font-size: 13px;
  font-weight: bold;
  transition: all 0.2s;
}
.cart-inner:hover {
  background: #e0f2fe;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.15);
}
.cart-icon {
  font-size: 18px;
}
.cart-count-badge {
  background: #f43f5e;
  color: #fff;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 7px;
  border-radius: 10px;
}

/* ================= 3. 频道主导航条 ================= */
.emall-channel-bar {
  background: #fff;
  border-bottom: 2px solid #0284c7;
}
.channel-inner {
  display: flex;
  align-items: center;
  height: 42px;
}
.category-header-tab {
  width: 200px;
  height: 100%;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #fff;
  font-size: 15px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 16px;
  box-sizing: border-box;
  cursor: pointer;
}
.channel-nav-list {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0 20px;
  gap: 25px;
}
.channel-item {
  font-size: 15px;
  font-weight: bold;
  color: #334155;
  cursor: pointer;
  transition: color 0.2s;
}
.channel-item:hover, .channel-item.active {
  color: #0284c7;
}
.channel-item.highlight-item {
  color: #f43f5e;
}

/* ================= 4. 首屏黄金三栏网格 ================= */
.hero-wrapper {
  display: flex;
  gap: 15px;
  margin-top: 15px;
  height: 420px;
}
/* 左栏分类树 */
.hero-left-menu {
  width: 200px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  overflow-y: auto;
  border: 1px solid #e2e8f0;
}
.emall-cat-tree {
  list-style: none;
  margin: 0;
  padding: 8px 0;
}
.cat-tree-item {
  padding: 10px 14px;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: all 0.2s;
}
.cat-tree-item:hover, .cat-tree-item.active {
  background: #f0f9ff;
  border-left-color: #0284c7;
}
.cat-main-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cat-name {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}
.cat-tree-item.active .cat-name {
  color: #0284c7;
  font-weight: bold;
}
.cat-arrow {
  font-size: 12px;
  color: #94a3b8;
}
.cat-sub-text {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 中栏主轮播 */
.hero-center-carousel {
  flex: 1;
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
  border: 1px solid #e2e8f0;
}
.carousel-img {
  width: 100%;
  height: 420px;
  object-fit: cover;
  cursor: pointer;
}
.empty-carousel-box {
  height: 420px;
  background: #f0f9ff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0284c7;
}
.sub-cat-chips {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(6px);
  padding: 8px 15px;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 10;
}
.sub-chip-label {
  color: #fff;
  font-size: 12px;
  font-weight: bold;
}
.sub-chip-pill {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.sub-chip-pill:hover, .sub-chip-pill.active {
  background: #0284c7;
  color: #fff;
}

/* 右栏用户卡片 & 便民服务 */
.hero-right-panel {
  width: 250px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.emall-user-card {
  background: #fff;
  border-radius: 6px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  border: 1px solid #e2e8f0;
}
.user-avatar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.emall-user-avatar {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  font-weight: bold;
}
.user-greeting-box {
  flex: 1;
  overflow: hidden;
}
.greet-title {
  font-size: 13px;
  font-weight: bold;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.greet-sub {
  font-size: 11px;
  color: #0284c7;
  margin-top: 3px;
}
.user-btn-row {
  display: flex;
  gap: 6px;
  margin-top: 12px;
}
.emall-primary-btn {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
  flex: 1;
}
.emall-plain-btn {
  flex: 1;
}
.emall-welfare-btn {
  background: #f0f9ff;
  color: #0284c7;
  border-color: #bae6fd;
  flex: 1.2;
}
.user-stats-row {
  display: flex;
  justify-content: space-around;
  margin-top: 12px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 8px;
}
.stat-col {
  text-align: center;
  cursor: pointer;
}
.stat-num {
  font-size: 15px;
  font-weight: bold;
  color: #0284c7;
}
.stat-lbl {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

.emall-news-box {
  background: #fff;
  border-radius: 6px;
  padding: 10px 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  border: 1px solid #e2e8f0;
}
.news-tab-header {
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 6px;
  font-size: 12px;
}
.tab-title {
  cursor: pointer;
  color: #64748b;
  font-weight: 500;
}
.tab-title.active {
  color: #0284c7;
  font-weight: bold;
  border-bottom: 2px solid #0284c7;
  padding-bottom: 4px;
}
.more-link {
  margin-left: auto;
  color: #94a3b8;
  font-size: 11px;
  cursor: pointer;
}
.news-list {
  list-style: none;
  margin: 6px 0 0 0;
  padding: 0;
  font-size: 12px;
}
.news-list li {
  line-height: 22px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #475569;
}
.tag-accent {
  background: #ffe4e6;
  color: #f43f5e;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 2px;
  margin-right: 4px;
  font-weight: 500;
}
.tag-blue {
  background: #e0f2fe;
  color: #0284c7;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 2px;
  margin-right: 4px;
  font-weight: 500;
}

.emall-service-matrix {
  background: #fff;
  border-radius: 6px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 8px 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  border: 1px solid #e2e8f0;
}
.service-cell {
  text-align: center;
  padding: 6px 2px;
  cursor: pointer;
  transition: all 0.2s;
}
.service-cell:hover {
  background: #f0f9ff;
}
.service-icon {
  font-size: 18px;
}
.service-text {
  font-size: 11px;
  color: #64748b;
  margin-top: 2px;
}

/* ================= 5. 限时秒杀专区 ================= */
.emall-seckill-section {
  margin-top: 20px;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
  overflow: hidden;
  border: 1px solid #e2e8f0;
}
.seckill-header {
  background: linear-gradient(90deg, #0284c7 0%, #0369a1 100%);
  color: #fff;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}
.seckill-title-box {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.seckill-logo-text {
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 1px;
}
.seckill-sub-title {
  font-size: 12px;
  opacity: 0.9;
}
.seckill-countdown-box {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: bold;
}
.time-block {
  background: #0f172a;
  color: #fff;
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 3px;
  margin: 0 3px;
}
.colon {
  color: #fff;
}
.seckill-body {
  padding: 15px;
}
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.seckill-card {
  border: 1px solid #f1f5f9;
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.3s;
}
.seckill-card:hover {
  border-color: #0284c7;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.12);
}
.seckill-img-wrap {
  width: 100%;
  height: 140px;
  position: relative;
  overflow: hidden;
  border-radius: 4px;
}
.seckill-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.seckill-card:hover .seckill-img {
  transform: scale(1.06);
}
.seckill-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  background: #f43f5e;
  color: #fff;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 2px;
  font-weight: 500;
}
.seckill-name {
  font-size: 12px;
  color: #334155;
  margin-top: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.seckill-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 4px;
}
.price-now {
  color: #f43f5e;
  font-size: 16px;
  font-weight: 900;
}
.price-origin {
  color: #94a3b8;
  font-size: 11px;
  text-decoration: line-through;
}
.seckill-progress {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}
.prog-bar {
  flex: 1;
  height: 6px;
  background: #e0f2fe;
  border-radius: 3px;
  overflow: hidden;
}
.prog-fill {
  height: 100%;
  background: linear-gradient(90deg, #38bdf8, #0284c7);
}
.prog-text {
  font-size: 10px;
  color: #94a3b8;
}

/* ================= 6. 领券中心专区 ================= */
.emall-coupon-zone {
  margin-top: 20px;
  background: #fff;
  border-radius: 6px;
  padding: 15px 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
  border: 1px solid #e2e8f0;
}
.zone-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.main-txt {
  font-size: 18px;
  font-weight: bold;
  color: #334155;
}
.sub-txt {
  font-size: 12px;
  color: #94a3b8;
}
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}
.emall-coupon-item {
  display: flex;
  height: 72px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px dashed #7dd3fc;
  border-radius: 6px;
  position: relative;
  overflow: hidden;
}
.coupon-left {
  width: 110px;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.c-val .sign { font-size: 14px; }
.c-val .big { font-size: 26px; font-weight: 900; }
.c-rule { font-size: 10px; opacity: 0.9; }
.coupon-mid {
  flex: 1;
  padding: 12px 15px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.c-title { font-size: 14px; font-weight: bold; color: #1e293b; }
.c-scope { font-size: 11px; color: #64748b; margin-top: 4px; }
.coupon-right {
  width: 90px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-left: 1px dashed #bae6fd;
  cursor: pointer;
  transition: background 0.2s;
}
.coupon-right:hover {
  background: #bae6fd;
}
.btn-claim {
  font-size: 12px;
  font-weight: bold;
  color: #0284c7;
}

/* ================= 7. 为您推荐商品流 ================= */
.emall-product-feed-section {
  margin-top: 20px;
}
.feed-filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 6px;
  padding: 10px 15px;
  margin-bottom: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  border: 1px solid #e2e8f0;
}
.filter-tab-group {
  display: flex;
  gap: 15px;
}
.filter-tab {
  font-size: 14px;
  font-weight: bold;
  color: #64748b;
  padding: 6px 14px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
}
.filter-tab:hover, .filter-tab.active {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
}
.feed-stat {
  font-size: 13px;
  color: #94a3b8;
}
.highlight-count {
  color: #0284c7;
  font-weight: bold;
}

.emall-products-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}
.emall-product-card {
  background: #fff;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}
.emall-product-card:hover {
  border-color: #0284c7;
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(2, 132, 199, 0.1);
}
.img-wrapper {
  width: 100%;
  height: 220px;
  background: #f8fafc;
  position: relative;
  overflow: hidden;
}
.p-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}
.emall-product-card:hover .p-img {
  transform: scale(1.05);
}
.no-p-img {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}
.tag-ziying {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
  font-weight: bold;
}
.tag-flash {
  position: absolute;
  top: 8px;
  right: 8px;
  background: #f43f5e;
  color: #fff;
  font-size: 10px;
  padding: 2px 5px;
  border-radius: 3px;
}

.p-info {
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  flex: 1;
}
.p-price-row {
  display: flex;
  align-items: baseline;
  color: #f43f5e;
}
.p-currency { font-size: 13px; font-weight: bold; margin-right: 1px; }
.p-integer { font-size: 22px; font-weight: 900; }
.p-decimal { font-size: 13px; font-weight: bold; }
.p-orig { font-size: 11px; color: #94a3b8; text-decoration: line-through; margin-left: 6px; }

.p-title {
  font-size: 14px;
  line-height: 20px;
  color: #334155;
  margin-top: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 40px;
  transition: color 0.2s;
}
.emall-product-card:hover .p-title {
  color: #0284c7;
}
.title-tag-blue {
  background: #e0f2fe;
  color: #0284c7;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 2px;
  margin-right: 4px;
  font-weight: 500;
}

.p-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.p-badges {
  display: flex;
  gap: 5px;
  margin-top: 6px;
}
.badge-ship {
  border: 1px solid #0284c7;
  color: #0284c7;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 2px;
}
.badge-coupon {
  background: #e0f2fe;
  color: #0284c7;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 2px;
}

.p-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}
.p-comment-count {
  font-size: 11px;
  color: #94a3b8;
}
.add-cart-btn {
  background: #f0f9ff;
  border: 1px solid #0284c7;
  color: #0284c7;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.add-cart-btn:hover {
  background: #0284c7;
  color: #fff;
}

/* 空商品提示 */
.empty-products-box {
  background: #fff;
  border-radius: 6px;
  padding: 40px;
  text-align: center;
  border: 1px solid #e2e8f0;
}
.empty-icon { font-size: 48px; margin-bottom: 10px; }
.empty-tip { font-size: 14px; color: #94a3b8; margin-bottom: 15px; }

/* ================= 8. 专业级分页条 ================= */
.emall-pagination-wrapper {
  display: flex;
  justify-content: center;
  margin: 30px 0 40px 0;
  background: #fff;
  padding: 16px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
  border: 1px solid #e2e8f0;
}
:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #0284c7 !important;
  color: #fff;
}
:deep(.el-pagination.is-background .el-pager li:hover) {
  color: #0284c7;
}

/* ================= 9. 品质保障与多列页脚 ================= */
.emall-footer {
  background: #f1f5f9;
  margin-top: 40px;
}
.footer-guarantee-bar {
  background: #fff;
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 28px 0;
}
.guarantee-inner {
  display: flex;
  justify-content: space-around;
}
.guarantee-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.g-circle {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 2px solid #0284c7;
  color: #0284c7;
  font-size: 22px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
}
.g-title {
  font-size: 16px;
  font-weight: bold;
  color: #334155;
}
.g-sub {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.footer-links-zone {
  display: flex;
  justify-content: space-around;
  padding: 35px 0;
}
.link-col .col-title {
  font-size: 14px;
  font-weight: bold;
  color: #475569;
  margin-bottom: 12px;
}
.link-col .col-link {
  font-size: 12px;
  color: #64748b;
  line-height: 24px;
  cursor: pointer;
  transition: color 0.2s;
}
.link-col .col-link:hover {
  color: #0284c7;
}

.footer-copyright {
  background: #e2e8f0;
  padding: 16px 0;
  text-align: center;
  font-size: 12px;
  color: #64748b;
}
.copy-inner p {
  margin: 4px 0;
}

/* ================= 10. 移动端自适应与 Tabbar ================= */
.mobile-tabbar {
  display: none;
}

@media (max-width: 992px) {
  .hero-wrapper {
    flex-direction: column;
    height: auto;
  }
  .hero-left-menu, .hero-right-panel {
    width: 100%;
  }
  .seckill-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .emall-products-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .coupon-grid {
    grid-template-columns: 1fr;
  }
  .guarantee-inner {
    flex-wrap: wrap;
    gap: 15px;
  }
}

@media (max-width: 768px) {
  .emall-topbar, .channel-nav-list, .footer-links-zone {
    display: none;
  }
  .header-inner {
    flex-direction: column;
    gap: 10px;
  }
  .emall-search-zone {
    width: 100%;
  }
  .seckill-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .emall-products-grid {
    grid-template-columns: 1fr;
  }
  .mobile-tabbar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 55px;
    background: #fff;
    border-top: 1px solid #eee;
    display: flex;
    justify-content: space-around;
    align-items: center;
    z-index: 1000;
  }
  .tab-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    cursor: pointer;
    color: #64748b;
    font-size: 11px;
  }
  .tab-item.active {
    color: #0284c7;
    font-weight: bold;
  }
  .tab-icon {
    font-size: 20px;
  }
}

/* ================= 11. 新增功能弹窗与快报样式 ================= */
.news-clickable-item {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: transform 0.2s, color 0.2s;
}
.news-clickable-item:hover {
  color: #0284c7;
  transform: translateX(3px);
}
.news-text-ellipsis {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 城市切换弹窗 */
.city-dialog-body {
  padding: 10px 0;
}
.current-city-tip {
  font-size: 14px;
  color: #475569;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px dashed #e2e8f0;
}
.city-highlight {
  color: #0284c7;
  font-weight: bold;
  font-size: 16px;
}
.city-section-title {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
  font-weight: 500;
}
.city-tags-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.city-chip {
  padding: 6px 16px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #334155;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.city-chip:hover {
  background: #e0f2fe;
  color: #0284c7;
}
.city-chip.active {
  background: #0284c7;
  color: #fff;
  font-weight: bold;
}

/* 公告详情弹窗 */
.notice-detail-box {
  padding: 5px 0;
}
.notice-meta-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}
.notice-badge {
  background: #e0f2fe;
  color: #0284c7;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
}
.notice-time {
  font-size: 12px;
  color: #94a3b8;
}
.notice-article-content {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
  white-space: pre-wrap;
}

/* 公告列表弹窗 */
.all-notices-container {
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.empty-notices {
  text-align: center;
  padding: 30px 0;
  color: #94a3b8;
  font-size: 14px;
}
.notice-list-card {
  padding: 14px 16px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;
}
.notice-list-card:hover {
  border-color: #0284c7;
  background: #f0f9ff;
  transform: translateY(-1px);
}
.nl-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.nl-title {
  font-weight: bold;
  color: #0f172a;
  font-size: 14px;
}
.nl-time {
  font-size: 12px;
  color: #94a3b8;
}
.nl-summary {
  font-size: 13px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 便民服务弹窗 */
.service-dialog-content {
  padding: 5px 0;
}
.service-intro-box {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}
.service-p {
  margin: 0;
  font-size: 14px;
  color: #0369a1;
  line-height: 1.7;
}
.service-channel-box {
  border-top: 1px solid #f1f5f9;
  padding-top: 15px;
}
.sc-title {
  font-size: 13px;
  color: #475569;
  margin-bottom: 12px;
  font-weight: 500;
}
.sc-action-row {
  display: flex;
  gap: 12px;
}
.sc-btn {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
}
</style>
