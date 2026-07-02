<script setup lang="ts">
import { ref, onMounted, computed } from 'vue' 
import { Search, ShoppingCart, User, TopRight, StarFilled, Ticket } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useRouter } from 'vue-router'
import { useCartStore } from '../../stores/cart'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

interface Product {
  id: number; name: string; price: number; stock: number;
  picUrl?: string; description: string; status: number;
  categoryId: number; sales: number;      
  promoPrice?: number; promoStartTime?: string; promoEndTime?: string; // ✨ 新增秒杀字段
}

interface Category { id: number; name: string; parentId: number; level: number; }
interface HotSearch { id: number; keyword: string; searchCount: number; }
interface Coupon { id: number; name: string; minAmount: number; discountAmount: number; endTime: string; }

const productList = ref<Product[]>([])
const hotSearchList = ref<HotSearch[]>([]) 
const couponList = ref<Coupon[]>([]) 
const bannerList = ref<any[]>([]) // ✨ 存放后端拉取的动态轮播图

const loading = ref(false)
const searchKey = ref('') 
const sortBy = ref('sales') // 新增 'promo' 状态 

const allCategories = ref<Category[]>([]) 
const selectedParentId = ref(0)           
const activeCategory = ref(0)             

const userStore = useUserStore()
const router = useRouter()
const cartStore = useCartStore()

const level1Categories = computed(() => [{ id: 0, name: '✨ 全部精选', parentId: 0, level: 1 }, ...allCategories.value.filter(c => c.level === 1)])
const level2Categories = computed(() => selectedParentId.value === 0 ? [] : allCategories.value.filter(c => c.level === 2 && c.parentId === selectedParentId.value))

const fetchDynamicCategories = async () => { try { allCategories.value = await request.get<any, Category[]>('/category/list') } catch (e) {} }
const fetchHotSearches = async () => { try { hotSearchList.value = await request.get<any, HotSearch[]>('/product/hotSearches') } catch (e) {} }
const fetchCoupons = async () => { try { couponList.value = await request.get<any, Coupon[]>('/coupon/list') } catch (e) {} }

// ✨ 新增：拉取展示中的广告轮播图
const fetchAds = async () => {
  try { 
    bannerList.value = await request.get('/ad/active') 
  } catch (e) {
    console.error('拉取轮播图失败')
  }
}

const fetchProducts = async () => {
  loading.value = true
  try {
    const params = {
      keyword: searchKey.value,
      categoryId: activeCategory.value === 0 ? null : activeCategory.value,
      // 如果选了促销，向后端请求时暂不排序，让前端来过滤即可
      sortBy: sortBy.value === 'promo' ? '' : sortBy.value 
    }
    productList.value = await request.get<any, Product[]>('/product/list', { params })
  } catch (error) { ElMessage.error('获取商品失败') } 
  finally { loading.value = false }
}

// ✨ 核心逻辑 1：判断该商品是否正在秒杀活动中
const isFlashSaleActive = (product: Product) => {
  if (!product.promoStartTime || !product.promoEndTime || !product.promoPrice) return false
  const now = new Date().getTime()
  const start = new Date(product.promoStartTime.replace(/-/g, '/')).getTime()
  const end = new Date(product.promoEndTime.replace(/-/g, '/')).getTime()
  return now >= start && now <= end
}

// ✨ 核心逻辑 2：动态计算需要展示的商品列表
const displayProducts = computed(() => {
  if (sortBy.value === 'promo') {
    // 选了“限时促销”，只展示正在秒杀的商品
    return productList.value.filter(p => isFlashSaleActive(p))
  }
  return productList.value
})

const executeSearch = async (keyword?: string) => {
  if (keyword) searchKey.value = keyword 
  if (searchKey.value.trim()) request.post(`/product/searchRecord?keyword=${encodeURIComponent(searchKey.value)}`).then(() => fetchHotSearches())
  fetchProducts()
}

const handleClaimCoupon = async (couponId: number) => {
  if (!userStore.userInfo) { ElMessage.warning('请先登录后再来抢神券哦！'); return router.push('/login') }
  try {
    const res = await request.post(`/coupon/claim?userId=${userStore.userInfo.id}&couponId=${couponId}`)
    if ((res as unknown as string).includes('成功')) ElMessage.success('🎉 ' + res)
    else ElMessage.warning('⚠️ ' + res)
  } catch (error) { ElMessage.error('领取失败，请检查网络') }
}

const handleLevel1Click = (id: number) => { selectedParentId.value = id; activeCategory.value = id; fetchProducts() }
const handleLevel2Click = (id: number) => { activeCategory.value = id; fetchProducts() }
const goToDetail = (id: number) => router.push(`/product/${id}`)

const handleQuickAdd = async (product: Product) => {
  // 1. 拦截未开始的秒杀活动，防止提前抢购
  if (product.promoStartTime && product.promoEndTime) {
    const now = new Date().getTime()
    const start = new Date(product.promoStartTime.replace(/-/g, '/')).getTime()
    if (now < start) {
      return ElMessage.warning('秒杀活动还未开始哦，请去详情页蹲点吧！')
    }
  }

  try {
    // 2. 异步静默拉取该商品的最新的规格列表
    const skus: any[] = await request.get(`/product/skus/${product.id}`)
    if (!skus || skus.length === 0) {
      return ElMessage.warning('该商品商家还在配置规格中，暂不可售！')
    }

    // 3. 快捷加购默认选中第一个规格
    const defaultSku = skus[0]
    if (defaultSku.stock <= 0) {
      return ElMessage.warning(`该商品默认规格 [${defaultSku.specName}] 已售罄！`)
    }

    // 4. 动态计算真实单价 (优先使用秒杀特价)
    let finalPrice = defaultSku.price
    if (isFlashSaleActive(product)) {
      finalPrice = product.promoPrice!
    }

    // 5. 组装无懈可击的数据体，送入 Pinia 购物车
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

// ✨ 轮播图点击跳转逻辑
const handleBannerClick = (url: string) => {
  if (url) {
    if (url.startsWith('http')) {
      window.open(url, '_blank')
    } else {
      router.push(url)
    }
  }
}

onMounted(() => {
  fetchDynamicCategories()
  fetchHotSearches()
  fetchCoupons() 
  fetchAds() // ✨ 挂载时拉取广告
  fetchProducts()
})
</script>

<template>
  <div class="mall-layout">
    <header class="glass-header">
      <div class="header-content">
        <div class="logo" @click="router.push('/')" style="cursor:pointer"><span class="brand-text">E-MALL</span></div>
        <div class="search-wrapper">
          <el-input v-model="searchKey" placeholder="搜一搜今天买点什么..." class="cute-input" :prefix-icon="Search" clearable @keyup.enter="executeSearch()">
            <template #append><el-button :icon="Search" @click="executeSearch()" class="search-btn">搜索</el-button></template>
          </el-input>
          <div class="hot-search-tags" v-if="hotSearchList.length > 0">
            <span class="hot-label"><el-icon><StarFilled /></el-icon> 热门搜索:</span>
            <span v-for="item in hotSearchList" :key="item.id" class="hot-word" @click="executeSearch(item.keyword)">{{ item.keyword }}</span>
          </div>
        </div>
        <div class="nav-actions">
          <el-badge :value="cartStore.totalCount" class="item" :hidden="cartStore.totalCount === 0">
            <el-button circle class="icon-btn" :icon="ShoppingCart" @click="router.push('/cart')" />
          </el-badge>
          <div class="user-entry" style="margin-left: 15px;">
            <el-dropdown v-if="userStore.userInfo" trigger="click">
              <div class="avatar-wrapper">
                <el-avatar :size="40" :src="userStore.userInfo.avatar?.includes('default-avatar.png') ? '' : userStore.userInfo.avatar">
                  {{ userStore.userInfo.username?.charAt(0).toUpperCase() || 'U' }}
                </el-avatar>
                <span class="user-name">{{ userStore.userInfo.nickname || userStore.userInfo.username }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/favorites')">我的收藏</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/comments')">我的评价</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/address')">地址管理</el-dropdown-item>
                  <el-dropdown-item divided @click="userStore.logout(); router.push('/login')" style="color: #f43f5e;">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-else circle class="icon-btn" :icon="User" @click="router.push('/login')" />
          </div>
        </div>
      </div>
    </header>

    <main class="main-container">
      
      <div class="hero-section">
        <div class="hero-sidebar glass-card">
          <ul class="vertical-menu">
            <li 
              v-for="cat in level1Categories" 
              :key="cat.id"
              :class="{ active: selectedParentId === cat.id }" 
              @click="handleLevel1Click(cat.id)"
            >
              {{ cat.name }}
            </li>
          </ul>
        </div>

        <div class="hero-banner">
          <el-carousel v-if="bannerList.length > 0" height="400px" class="cute-carousel" motion-blur>
            <el-carousel-item v-for="banner in bannerList" :key="banner.id" @click="handleBannerClick(banner.linkUrl)">
              <img :src="banner.picUrl" class="banner-img" style="cursor: pointer" :title="banner.title" />
            </el-carousel-item>
          </el-carousel>
          <el-carousel v-else height="400px" class="cute-carousel">
            <el-carousel-item>
              <div class="empty-banner">
                正在为您准备精彩活动...
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>
      </div>

      <transition name="el-zoom-in-top">
        <div class="sub-nav-container" v-if="level2Categories.length > 0">
          <div class="category-nav sub-nav">
            <div 
              v-for="sub in level2Categories" 
              :key="sub.id" 
              class="category-pill mini" 
              :class="{ active: activeCategory === sub.id }" 
              @click="handleLevel2Click(sub.id)"
            >
              {{ sub.name }}
            </div>
          </div>
        </div>
      </transition>

      <div class="coupon-section" v-if="couponList.length > 0">
        <div class="section-title"><el-icon color="#f43f5e" size="24"><Ticket /></el-icon> 领券中心<span class="title-sub">先领券，再购物，尽享折上折！</span></div>
        <div class="coupon-list">
          <div class="coupon-card" v-for="coupon in couponList" :key="coupon.id">
            <div class="c-left"><span class="c-currency">¥</span><span class="c-amount">{{ coupon.discountAmount }}</span></div>
            <div class="c-middle"><div class="c-name">{{ coupon.name }}</div><div class="c-condition">满 {{ coupon.minAmount }} 元可用</div></div>
            <div class="c-right" @click="handleClaimCoupon(coupon.id)"><div class="vertical-text">立即领取</div></div>
            <div class="hole top-hole"></div><div class="hole bottom-hole"></div>
          </div>
        </div>
      </div>

      <div class="product-section">
        <div class="filter-bar">
          <div class="sort-tabs">
            <span :class="{ active: sortBy === 'sales' }" @click="sortBy = 'sales'; fetchProducts()"><el-icon><StarFilled /></el-icon> 热门商品</span>
            <span :class="{ active: sortBy === 'new' }" @click="sortBy = 'new'; fetchProducts()">✨ 新品推荐</span>
            <span :class="{ active: sortBy === 'promo' }" @click="sortBy = 'promo'; fetchProducts()">🔥 限时促销</span>
          </div>
          <div class="result-count">共 {{ displayProducts.length }} 件宝贝</div>
        </div>

        <div v-if="loading" class="loading-state">正在努力搬运商品中...</div>
        
        <el-row :gutter="25" class="product-grid" v-else>
          <el-col :xs="12" :sm="8" :md="6" :lg="6" v-for="product in displayProducts" :key="product.id">
            <el-card class="product-card" shadow="hover" :body-style="{ padding: '0px' }" @click="goToDetail(product.id)">
              <div class="img-placeholder">
                <img v-if="product.picUrl" :src="product.picUrl" class="product-img" />
                <div v-else class="no-img">暂无图片</div>
                <div v-if="isFlashSaleActive(product)" class="promo-tag">超值特惠</div>
              </div>
              <div class="product-info">
                <h3 class="product-name">{{ product.name }}</h3>
                <p class="product-desc" :title="product.description">{{ product.description }}</p>
                <div class="price-row">
                  <div class="price-display">
                    <span class="price">¥ {{ isFlashSaleActive(product) ? product.promoPrice : product.price }}</span>
                    <span class="original-price" v-if="isFlashSaleActive(product)">¥{{ product.price }}</span>
                  </div>
                  <el-button circle class="icon-btn-cart" :icon="ShoppingCart" @click.stop="handleQuickAdd(product)" />
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <div v-if="displayProducts.length === 0 && !loading" class="empty-category">
          <span style="font-size: 40px;">🛒</span>
          <p>{{ sortBy === 'promo' ? '当前没有正在进行中的秒杀活动哦~' : '没有找到相关宝贝哦，换个关键词试试吧~' }}</p>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* ================= 原有基础与顶部导航样式 ================= */
.mall-layout { min-height: 100vh; background-color: #f8fafc; padding-top: 80px; }
.glass-header { position: fixed; top: 0; left: 0; right: 0; background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(16px); border-bottom: 1px solid rgba(226, 232, 240, 0.8); z-index: 1000; display: flex; justify-content: center; padding: 15px 0; }
.header-content { width: 1200px; max-width: 95%; display: flex; align-items: flex-start; justify-content: space-between; }
.brand-text { font-size: 26px; font-weight: 900; background: linear-gradient(to right, #0284c7, #38bdf8); -webkit-background-clip: text; color: transparent; letter-spacing: 2px; line-height: 40px; }

.search-wrapper { width: 45%; display: flex; flex-direction: column; gap: 8px; }
:deep(.cute-input .el-input__wrapper) { border-radius: 25px 0 0 25px; background-color: #f1f5f9; box-shadow: none; padding: 0 20px; transition: all 0.3s; }
:deep(.cute-input .el-input-group__append) { border-radius: 0 25px 25px 0; background: #0ea5e9; color: white; border: none; box-shadow: none; padding: 0 25px; font-weight: bold; cursor: pointer; }
:deep(.cute-input .el-input-group__append:hover) { background: #0284c7; }

.hot-search-tags { display: flex; gap: 10px; font-size: 12px; align-items: center; padding-left: 10px; flex-wrap: wrap; }
.hot-label { color: #ef4444; font-weight: bold; display: flex; align-items: center; gap: 2px; }
.hot-word { color: #64748b; cursor: pointer; transition: color 0.2s; }
.hot-word:hover { color: #0ea5e9; text-decoration: underline; }

.nav-actions { display: flex; align-items: center; height: 40px; }
.icon-btn { border: none; background: #f1f5f9; color: #475569; font-size: 18px; }
.icon-btn:hover { background: #e0f2fe; color: #0284c7; }
.user-entry { display: flex; align-items: center; cursor: pointer; height: 100%; }
.avatar-wrapper { display: flex; align-items: center; gap: 8px; outline: none; }
.user-name { font-size: 14px; color: #475569; font-weight: bold; }

.main-container { width: 1200px; max-width: 95%; margin: 0 auto; padding-bottom: 60px; }

/* ================= ✨ 新增/修改的首屏布局样式 ================= */

/* Hero 区域：左侧 220px，右侧自适应 */
.hero-section {
  display: flex;
  gap: 20px;
  margin-top: 20px;
  height: 400px; /* 强制与轮播图等高 */
}

/* 左侧分类侧边栏 */
.hero-sidebar {
  width: 220px;
  flex-shrink: 0;
  border-radius: 20px;
  overflow-y: auto;
  padding: 15px 0;
  /* 隐藏滚动条但保留滚动功能 */
  scrollbar-width: none; 
}
.hero-sidebar::-webkit-scrollbar {
  display: none; 
}

/* 毛玻璃卡片通用类 */
.glass-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 20px 40px rgba(0,0,0,0.04);
}

/* 垂直菜单样式 */
.vertical-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.vertical-menu li {
  padding: 12px 25px;
  font-size: 15px;
  color: #475569;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  position: relative;
}

.vertical-menu li:hover {
  color: #0ea5e9;
  background: rgba(224, 242, 254, 0.5);
  font-weight: bold;
}

.vertical-menu li.active {
  background: linear-gradient(90deg, rgba(14, 165, 233, 0.1) 0%, transparent 100%);
  color: #0369a1;
  font-weight: bold;
  border-left: 4px solid #0ea5e9;
}

/* 右侧轮播图容器 */
.hero-banner {
  flex: 1;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
  min-width: 0; /* 防止 flex 撑破父容器 */
}

.banner-img { width: 100%; height: 100%; object-fit: cover; }
.empty-banner {
  height: 100%; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  background: #f1f5f9; 
  color: #94a3b8; 
  font-size: 16px;
}

/* 二级分类容器 (吸附在首屏下方) */
.sub-nav-container {
  margin-top: 20px;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  border-radius: 16px;
  border: 1px dashed rgba(186, 230, 253, 0.8);
}
.category-nav { display: flex; gap: 15px; overflow-x: auto; padding-bottom: 5px; }
.category-nav::-webkit-scrollbar { display: none; }
.category-pill { padding: 10px 24px; border-radius: 30px; background: rgba(224, 242, 254, 0.6); backdrop-filter: blur(8px); color: #0369a1; font-weight: bold; font-size: 14px; cursor: pointer; transition: all 0.3s; border: 1px solid rgba(186, 230, 253, 0.5); white-space: nowrap; }
.category-pill.active { background: #0ea5e9; color: white; border-color: #0ea5e9; box-shadow: 0 8px 20px rgba(14, 165, 233, 0.3); }
.category-pill.mini { padding: 6px 18px; font-size: 12px; background: rgba(241, 245, 249, 0.6); border-style: dashed; }
.category-pill.mini.active { background: #38bdf8; border-style: solid; }


/* ================= 领券中心 & 商品列表原有样式 ================= */
.coupon-section { margin-top: 35px; }
.section-title { font-size: 22px; font-weight: 900; color: #0f172a; margin-bottom: 20px; display: flex; align-items: center; gap: 10px; }
.title-sub { font-size: 13px; font-weight: normal; color: #94a3b8; margin-left: 10px; }
.coupon-list { display: flex; gap: 20px; overflow-x: auto; padding-bottom: 15px; }
.coupon-list::-webkit-scrollbar { height: 6px; }
.coupon-list::-webkit-scrollbar-thumb { background: #fecdd3; border-radius: 10px; }
.coupon-card { flex-shrink: 0; width: 300px; height: 100px; display: flex; background: linear-gradient(135deg, #fff1f2, #ffe4e6); border-radius: 12px; position: relative; border: 1px solid #fecdd3; box-shadow: 0 8px 15px rgba(244, 63, 94, 0.08); overflow: hidden; transition: transform 0.3s; }
.coupon-card:hover { transform: translateY(-5px); box-shadow: 0 12px 20px rgba(244, 63, 94, 0.15); }
.c-left { width: 90px; display: flex; align-items: center; justify-content: center; color: #e11d48; font-weight: 900; }
.c-currency { font-size: 16px; margin-right: 2px; margin-top: 10px; }
.c-amount { font-size: 38px; }
.c-middle { flex: 1; padding: 0 15px; display: flex; flex-direction: column; justify-content: center; border-right: 2px dashed #fda4af; }
.c-name { font-weight: bold; color: #1e293b; font-size: 16px; margin-bottom: 5px; }
.c-condition { font-size: 12px; color: #9f1239; background: rgba(253, 164, 175, 0.3); padding: 2px 8px; border-radius: 10px; display: inline-block; align-self: flex-start; }
.c-right { width: 75px; display: flex; align-items: center; justify-content: center; cursor: pointer; background: #f43f5e; color: white; transition: background 0.3s; }
.c-right:hover { background: #e11d48; }
.vertical-text { width: 20px; font-size: 15px; font-weight: bold; line-height: 1.2; letter-spacing: 2px; text-align: center; }
.hole { position: absolute; right: 67px; width: 16px; height: 16px; background: #f8fafc; border-radius: 50%; z-index: 10; border: 1px solid #fecdd3; }
.top-hole { top: -9px; border-bottom-color: transparent; border-left-color: transparent; border-right-color: transparent; }
.bottom-hole { bottom: -9px; border-top-color: transparent; border-left-color: transparent; border-right-color: transparent; }

.product-section { margin-top: 35px; }
.filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; padding: 0 5px; border-top: 1px solid #f1f5f9; padding-top: 20px; }
.sort-tabs { display: flex; gap: 30px; }
.sort-tabs span { font-size: 15px; color: #64748b; cursor: pointer; position: relative; transition: all 0.3s; display: flex; align-items: center; gap: 4px; }
.sort-tabs span.active { color: #0369a1; font-weight: bold; }
.sort-tabs span.active::after { content: ""; position: absolute; bottom: -8px; left: 20%; width: 60%; height: 3px; background: #0ea5e9; border-radius: 10px; }
.result-count { font-size: 13px; color: #94a3b8; }

.product-card { border-radius: 20px; border: none; cursor: pointer; transition: all 0.4s; background: white; overflow: hidden; margin-bottom: 25px; position: relative; }
.product-card:hover { transform: translateY(-8px); box-shadow: 0 15px 30px rgba(14, 165, 233, 0.15); }
.img-placeholder { height: 220px; overflow: hidden; background: #f8fafc; position: relative; }
.product-img { width: 100%; height: 100%; object-fit: cover; }
.promo-tag { position: absolute; top: 10px; left: -30px; background: #ef4444; color: white; padding: 4px 30px; font-size: 12px; font-weight: bold; transform: rotate(-45deg); box-shadow: 0 4px 10px rgba(239, 68, 68, 0.3); }

.product-info { padding: 20px; }
.product-name { margin: 0 0 10px 0; font-size: 17px; color: #1e293b; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.product-desc { margin: 0 0 15px 0; font-size: 13px; color: #64748b; line-height: 1.5; height: 40px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.price-row { display: flex; justify-content: space-between; align-items: center; }
.price-display { display: flex; align-items: baseline; }
.price { font-size: 22px; font-weight: 900; color: #f43f5e; }

/* ✨ 新增划线价样式 */
.original-price { font-size: 12px; color: #94a3b8; text-decoration: line-through; margin-left: 6px; }

.icon-btn-cart { border: none; background: #f1f5f9; color: #0ea5e9; transition: all 0.3s; }
.icon-btn-cart:hover { background: #0ea5e9; color: white; }
.loading-state, .empty-category { text-align: center; padding: 60px 0; color: #94a3b8; font-size: 16px; width: 100%; }
</style>