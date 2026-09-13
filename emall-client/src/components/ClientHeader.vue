<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Location, ArrowDown, Search, StarFilled, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useCartStore } from '../stores/cart'
import request from '../utils/request'

interface Props {
  modelValue?: string
  showSearch?: boolean
  showNav?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  showSearch: true,
  showNav: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'search', val: string): void
}>()

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const searchKey = ref(props.modelValue || '')

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
  ElMessage.success(`已切换至【${city}】，配送仓储已就近调配！`)
}

// 热门搜索列表
const hotSearchList = ref([
  { id: 1, keyword: 'iPhone 16 Pro' },
  { id: 2, keyword: '极客机械键盘' },
  { id: 3, keyword: '降噪头戴耳机' },
  { id: 4, keyword: '无线快充' }
])

const fetchHotSearches = async () => {
  try {
    const res: any = await request.get('/product/hotSearches')
    if (Array.isArray(res) && res.length > 0) {
      hotSearchList.value = res.slice(0, 5)
    }
  } catch (e) {
    // 保持默认兜底词
  }
}

const executeSearch = (keyword?: string) => {
  const target = keyword !== undefined ? keyword : searchKey.value
  searchKey.value = target
  emit('update:modelValue', target)
  emit('search', target)
  if (router.currentRoute.value.path !== '/') {
    router.push({ path: '/', query: { q: target } })
  }
}

const openCustomerService = () => {
  window.dispatchEvent(new CustomEvent('open-customer-service'))
}

onMounted(() => {
  fetchHotSearches()
})
</script>

<template>
  <div class="emall-header-wrapper">
    <!-- 1. 顶部快捷工具栏 -->
    <div class="emall-topbar">
      <div class="emall-topbar-container">
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
              <span class="link-item reg-link" @click="router.push('/login?mode=register')">免费注册</span>
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
          <el-dropdown trigger="hover">
            <span class="link-item drop-link">客户服务 <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu class="emall-drop-menu">
                <el-dropdown-item @click="router.push('/comments')">我的评价</el-dropdown-item>
                <el-dropdown-item @click="openCustomerService()">在线客服</el-dropdown-item>
                <el-dropdown-item @click="router.push('/profile')">服务与反馈</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="divider">|</span>
          <span class="link-item" @click="router.push('/')">商城首页</span>
        </div>
      </div>
    </div>

    <!-- 2. 品牌主头部与搜索区 -->
    <header class="emall-main-header">
      <div class="emall-header-container">
        <!-- Logo -->
        <div class="emall-logo-box" @click="router.push('/')">
          <div class="emall-brand-title">E-MALL</div>
          <div class="emall-brand-slogan">全球严选 · 品质电商</div>
        </div>

        <!-- 搜索区域 -->
        <div class="emall-search-zone" v-if="showSearch">
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
            <span class="hot-lead"><el-icon><StarFilled /></el-icon> 热门：</span>
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

        <!-- 占位/备用区 -->
        <div v-else class="header-placeholder-slot">
          <slot name="header-middle" />
        </div>

        <!-- 购物车快速入口 -->
        <div class="emall-cart-box" @click="router.push('/cart')">
          <div class="cart-inner">
            <el-icon class="cart-icon"><ShoppingCart /></el-icon>
            <span class="cart-text">我的购物车</span>
            <span class="cart-count-badge">{{ cartStore.totalCount }}</span>
          </div>
        </div>
      </div>
    </header>

    <!-- 城市切换模态框 -->
    <el-dialog v-model="cityDialogVisible" title="选择所在城市" width="460px" class="city-picker-dialog" append-to-body>
      <p class="city-dialog-sub">请选择您当前所在的收货城市，我们将自动为您推荐该地区的本地仓储与最快时效配送：</p>
      <div class="city-grid">
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
      <template #footer>
        <el-button @click="cityDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.emall-header-wrapper {
  width: 100%;
  background: #ffffff;
  border-bottom: 1px solid #f1f5f9;
  position: relative;
  z-index: 100;
}

/* 顶部快捷条 */
.emall-topbar {
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  height: 34px;
  line-height: 34px;
  font-size: 12px;
  color: #64748b;
}

.emall-topbar-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topbar-left .location-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.loc-icon {
  color: #f43f5e;
}

.loc-switch {
  color: #0ea5e9;
  cursor: pointer;
  margin-left: 2px;
}

.loc-switch:hover {
  text-decoration: underline;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-greeting {
  font-weight: bold;
  color: #0f172a;
}

.vip-tag {
  height: 18px;
  padding: 0 6px;
  font-size: 10px;
  border-radius: 4px;
}

.divider {
  color: #cbd5e1;
}

.link-item {
  cursor: pointer;
  color: #64748b;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 2px;
}

.link-item:hover {
  color: #0284c7;
}

.active-link {
  color: #0284c7;
  font-weight: bold;
}

.reg-link {
  color: #0ea5e9;
}

.logout-link {
  color: #94a3b8;
}

.drop-link {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* 主头部 */
.emall-main-header {
  padding: 18px 0;
  background-color: #ffffff;
}

.emall-header-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
}

/* Logo */
.emall-logo-box {
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.emall-brand-title {
  font-size: 30px;
  font-weight: 900;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #0284c7 0%, #38bdf8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1.1;
}

.emall-brand-slogan {
  font-size: 11px;
  color: #94a3b8;
  letter-spacing: 2px;
  margin-top: 2px;
}

/* 搜索区 */
.emall-search-zone {
  flex: 1;
  max-width: 620px;
}

.emall-search-form {
  display: flex;
  align-items: center;
  border: 2px solid #0284c7;
  border-radius: 24px;
  overflow: hidden;
  background-color: #ffffff;
  transition: box-shadow 0.2s;
}

.emall-search-form:focus-within {
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.emall-search-input {
  flex: 1;
  height: 38px;
  padding: 0 16px;
  border: none;
  outline: none;
  font-size: 13px;
  color: #1e293b;
}

.emall-search-input::placeholder {
  color: #94a3b8;
}

.emall-search-btn {
  height: 38px;
  padding: 0 24px;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  color: #ffffff;
  font-size: 14px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.emall-search-btn:hover {
  opacity: 0.9;
}

.emall-hot-words {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
  padding-left: 10px;
}

.hot-lead {
  display: flex;
  align-items: center;
  gap: 2px;
  color: #94a3b8;
}

.hot-tag {
  cursor: pointer;
  transition: color 0.2s;
}

.hot-tag:hover {
  color: #0284c7;
}

.hot-tag.first-hot {
  color: #0284c7;
  font-weight: bold;
}

.header-placeholder-slot {
  flex: 1;
  display: flex;
  align-items: center;
}

/* 购物车入口按钮 */
.emall-cart-box {
  flex-shrink: 0;
  cursor: pointer;
}

.cart-inner {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 20px;
  border: 1px solid #bae6fd;
  background-color: #f0f9ff;
  border-radius: 20px;
  color: #0284c7;
  font-size: 13px;
  font-weight: bold;
  transition: all 0.2s;
}

.cart-inner:hover {
  background-color: #e0f2fe;
  border-color: #0284c7;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.15);
}

.cart-icon {
  font-size: 18px;
}

.cart-count-badge {
  background-color: #f43f5e;
  color: #ffffff;
  font-size: 11px;
  font-weight: bold;
  padding: 1px 7px;
  border-radius: 10px;
}

/* 城市弹窗 */
.city-dialog-sub {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.city-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.city-chip {
  padding: 8px;
  text-align: center;
  font-size: 13px;
  color: #334155;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.city-chip:hover {
  color: #f43f5e;
  border-color: #f43f5e;
  background-color: #fff1f2;
}

.city-chip.active {
  color: #ffffff;
  background-color: #f43f5e;
  border-color: #f43f5e;
  font-weight: bold;
}
</style>
