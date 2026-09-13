<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Location, Star, ChatLineSquare, ShoppingBag, ArrowRight } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import ClientHeader from './ClientHeader.vue'
import ClientFooter from './ClientFooter.vue'

interface Props {
  activeMenu?: string
  pageTitle?: string
}

const props = withDefaults(defineProps<Props>(), {
  activeMenu: '',
  pageTitle: ''
})

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuItems = [
  { key: 'profile', title: '个人资料与安全', path: '/profile', icon: User },
  { key: 'address', title: '收货地址管理', path: '/address', icon: Location },
  { key: 'favorites', title: '我的收藏夹', path: '/favorites', icon: Star },
  { key: 'comments', title: '我的评价中心', path: '/comments', icon: ChatLineSquare },
  { key: 'orders', title: '全部订单记录', path: '/orders', icon: ShoppingBag }
]

const currentActive = computed(() => {
  if (props.activeMenu) return props.activeMenu
  const p = route.path
  if (p.includes('/address')) return 'address'
  if (p.includes('/favorites')) return 'favorites'
  if (p.includes('/comments')) return 'comments'
  if (p.includes('/orders')) return 'orders'
  return 'profile'
})

const safeAvatar = computed(() => {
  const av = userStore.userInfo?.avatar
  if (!av || av.includes('default-avatar.png')) return ''
  return av
})
</script>

<template>
  <div class="user-center-layout">
    <ClientHeader />

    <main class="user-center-container">
      <!-- 面包屑 -->
      <nav class="breadcrumb-bar">
        <span class="crumb-link" @click="router.push('/')">首页</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-link" @click="router.push('/profile')">个人中心</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">{{ pageTitle || '管理面板' }}</span>
      </nav>

      <div class="user-center-grid">
        <!-- 左侧：用户专属导航卡片 -->
        <aside class="uc-sidebar-card">
          <!-- 用户微名片 -->
          <div class="uc-profile-card">
            <div class="avatar-wrap">
              <el-avatar v-if="safeAvatar" :size="64" :src="safeAvatar" class="uc-avatar" />
              <el-avatar v-else :size="64" class="uc-avatar fallback-avatar">
                {{ userStore.userInfo?.nickname?.substring(0, 1) || userStore.userInfo?.username?.substring(0, 1) || 'U' }}
              </el-avatar>
            </div>
            <div class="uc-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || 'E-MALL 会员' }}</div>
            <div class="uc-vip-badge">
              <span>💎 VIP 尊享会员</span>
            </div>
          </div>

          <!-- 导航菜单 -->
          <ul class="uc-nav-menu">
            <li 
              v-for="item in menuItems" 
              :key="item.key" 
              class="uc-menu-item"
              :class="{ active: currentActive === item.key }"
              @click="router.push(item.path)"
            >
              <div class="menu-item-left">
                <el-icon :size="18" class="menu-icon"><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </div>
              <el-icon class="menu-arrow"><ArrowRight /></el-icon>
            </li>
          </ul>
        </aside>

        <!-- 右侧：内容区卡片 -->
        <section class="uc-content-card">
          <slot />
        </section>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<style scoped>
.user-center-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.user-center-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  padding: 24px 0 60px;
  flex: 1;
}

/* 面包屑 */
.breadcrumb-bar {
  padding: 0 0 16px;
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

/* 布局网格 */
.user-center-grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
  align-items: start;
}

/* 侧边栏 */
.uc-sidebar-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.uc-profile-card {
  padding: 28px 20px;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
  display: flex;
  flex-direction: column;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
}

.avatar-wrap {
  margin-bottom: 12px;
}

.uc-avatar {
  border: 3px solid #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.fallback-avatar {
  background: linear-gradient(135deg, #0284c7, #38bdf8);
  color: #ffffff;
  font-size: 24px;
  font-weight: bold;
}

.uc-name {
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
  margin-bottom: 6px;
}

.uc-vip-badge {
  background: #f0f9ff;
  color: #0284c7;
  font-size: 11px;
  font-weight: bold;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid #bae6fd;
}

/* 菜单 */
.uc-nav-menu {
  list-style: none;
  padding: 12px;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.uc-menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 10px;
  color: #475569;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.menu-arrow {
  color: #cbd5e1;
  font-size: 12px;
  transition: transform 0.2s;
}

.uc-menu-item:hover {
  background-color: #f8fafc;
  color: #0284c7;
}

.uc-menu-item:hover .menu-arrow {
  color: #0284c7;
  transform: translateX(2px);
}

.uc-menu-item.active {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #ffffff;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.25);
}

.uc-menu-item.active .menu-arrow {
  color: #ffffff;
}

/* 内容卡片 */
.uc-content-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
  padding: 30px;
  min-height: 520px;
}
</style>
