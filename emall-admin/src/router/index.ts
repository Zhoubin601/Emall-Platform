import { createRouter, createWebHistory } from 'vue-router'
import { useAdminStore } from '../stores/admin'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/index.vue'), meta: { title: '数据看板' } },
      { path: 'users', name: 'Users', component: () => import('../views/user/index.vue'), meta: { title: '用户管理' } },
      { path: 'products', name: 'Products', component: () => import('../views/product/index.vue'), meta: { title: '商品列表' } },
      { path: 'categories', component: () => import('../views/product/category.vue'), meta: { title: '分类管理' } },
      { path: 'orders', name: 'Orders', component: () => import('../views/order/index.vue'), meta: { title: '订单管理' } },
      { path: 'comments', name: 'Comments', component: () => import('../views/comment/index.vue'), meta: { title: '评价审核' } },
      { path: 'feedbacks', name: 'Feedbacks', component: () => import('../views/feedback/index.vue'), meta: { title: '客服中心' } },
      // ✨ 补齐：系统公告路由
      { path: 'notices', name: 'Notices', component: () => import('../views/notice/index.vue'), meta: { title: '公告管理' } },
      // ✨ 核心修复：轮播图(广告)管理路由
      { path: 'ads', name: 'Ads', component: () => import('../views/ad/index.vue'), meta: { title: '轮播管理' } },
      { path: 'profile', name: 'Profile', component: () => import('../views/profile/index.vue'), meta: { title: '个人中心' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, _from, next) => {
  const adminStore = useAdminStore()
  if (to.path !== '/login' && !adminStore.token) next('/login')
  else if (to.path === '/login' && adminStore.token) next('/')
  else next()
})

export default router
