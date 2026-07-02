import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/home/index.vue')
  },
  // ✨ 新增商品详情页路由
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../views/product/detail.vue')
  },// ✨ 新增购物车路由
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/cart/index.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue')
  },
  
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/user/profile.vue')
  },
  {
  path: '/checkout',
  name: 'Checkout',
  component: () => import('../views/order/checkout.vue')
  },
  {
  path: '/address',
  name: 'Address',
  component: () => import('../views/user/address.vue')
  },
  {
  path: '/favorites',
  name: 'Favorites',
  component: () => import('../views/user/favorites.vue')
},
{
    path: '/order/comment',
    name: 'OrderComment',
    component: () => import('../views/order/comment.vue'),
    meta: { 
      title: '发表评价',
      requiresAuth: true // 评价功能必须登录后才能访问
    }
  },

  // ✨ 顺便确认一下您的订单列表路由是否配置正确
  {
    path: '/orders',
    name: 'OrderList',
    component: () => import('../views/order/list.vue'),
    meta: { 
      title: '我的订单',
      requiresAuth: true 
    }
  },
  {
    path: '/comments',
    name: 'MyComments',
    component: () => import('../views/user/MyComments.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router