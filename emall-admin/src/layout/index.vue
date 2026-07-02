<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAdminStore } from '../stores/admin' 
import { DataLine, User, Goods, List, Setting, Expand, Fold, SwitchButton, ArrowDown, ChatDotRound, Service, Notification, Picture } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const adminStore = useAdminStore() 
const isCollapse = ref(false)

const adminName = computed(() => adminStore.adminInfo.nickname || adminStore.adminInfo.username || '未知管理')
const roleText = computed(() => adminStore.adminInfo.role === 2 ? '超级管理员' : '普通管理员')

const handleLogout = () => { adminStore.clearToken(); router.push('/login') }
</script>

<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo"><img src="https://element-plus.org/images/element-plus-logo.svg" alt="logo" /><span v-show="!isCollapse">E-MALL 管理</span></div>
      <el-menu :default-active="route.path" class="el-menu-vertical" :collapse="isCollapse" router background-color="#001529" text-color="#ffffff" active-text-color="#409EFF">
        <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon><span>数据看板</span></el-menu-item>
        <el-menu-item index="/users" v-if="adminStore.adminInfo.role === 2"><el-icon><User /></el-icon><span>权限与用户管理</span></el-menu-item>
        <el-sub-menu index="product">
          <template #title><el-icon><Goods /></el-icon><span>商品管理</span></template>
          <el-menu-item index="/products">商品列表</el-menu-item>
          <el-menu-item index="/categories">分类管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/orders"><el-icon><List /></el-icon><span>订单管理</span></el-menu-item>
        <el-menu-item index="/comments"><el-icon><ChatDotRound /></el-icon><span>评价审核</span></el-menu-item>
        
        <el-menu-item index="/feedbacks"><el-icon><Service /></el-icon><span>客服中心</span></el-menu-item>
        <el-menu-item index="/ads"><el-icon><Picture /></el-icon><span>轮播管理</span></el-menu-item>
        
        <el-menu-item index="/notices">
          <el-icon><Notification /></el-icon>
          <span>公告管理</span>
        </el-menu-item>

        <el-menu-item index="/settings"><el-icon><Setting /></el-icon><span>系统设置</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse"><component :is="isCollapse ? Expand : Fold" /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '数据看板' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown>
            <span class="user-info"><el-tag :type="adminStore.adminInfo.role === 2 ? 'danger' : 'success'" size="small" effect="dark" style="margin-right:8px">{{ roleText }}</el-tag>{{ adminName }} <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :icon="User" @click="router.push('/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item :icon="SwitchButton" divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container { height: 100vh; }
.aside { background-color: #001529; transition: width 0.3s; overflow-x: hidden; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; gap: 10px; color: white; font-weight: bold; background: #002140; }
.logo img { width: 30px; }
.header { background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.header-left { display: flex; align-items: center; gap: 20px; }
.collapse-btn { font-size: 20px; cursor: pointer; }
.user-info { cursor: pointer; color: #333; font-weight: 500; display: flex; align-items: center; }
.main-content { background-color: #f0f2f5; padding: 20px; }
</style>