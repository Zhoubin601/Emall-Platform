<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Back, Delete, StarFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const products = ref<any[]>([])

const fetchFavorites = async () => {
  const res = await request.get('/favorite/list', { params: { userId: userStore.userInfo.id } })
  products.value = res
}

const uncollect = async (productId: number) => {
  await request.post('/favorite/toggle', { userId: userStore.userInfo.id, productId })
  fetchFavorites() // 刷新列表
}

onMounted(() => fetchFavorites())
</script>

<template>
  <div class="favorite-page">
    <header class="glass-header">
      <el-button link :icon="Back" @click="router.back()">返回</el-button>
      <span class="title">我的收藏夹</span>
    </header>

    <main class="container">
      <div v-if="products.length === 0" class="empty-state">
        <el-icon size="60" color="#bae6fd"><StarFilled /></el-icon>
        <p>收藏夹空空的，快去逛逛吧~</p>
      </div>

      <el-row :gutter="20">
        <el-col :span="6" v-for="p in products" :key="p.id">
          <el-card class="product-card" shadow="hover">
            <img :src="p.picUrl" class="p-img" @click="router.push(`/product/${p.id}`)" />
            <div class="p-info">
              <h4 class="p-name">{{ p.name }}</h4>
              <div class="p-footer">
                <span class="p-price">¥{{ p.price }}</span>
                <el-button link type="danger" :icon="Delete" @click="uncollect(p.id)">移除</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </main>
  </div>
</template>

<style scoped>
.favorite-page { min-height: 100vh; background: #f0f9ff; padding-top: 80px; }
.glass-header { position: fixed; top: 0; width: 100%; height: 60px; background: rgba(255,255,255,0.7); backdrop-filter: blur(10px); display: flex; align-items: center; padding: 0 50px; z-index: 100; border-bottom: 1px solid #e0f2fe; }
.title { margin-left: 20px; font-weight: bold; color: #0369a1; }
.container { width: 1100px; margin: 0 auto; }
.product-card { border-radius: 20px; border: none; overflow: hidden; margin-bottom: 20px; }
.p-img { width: 100%; height: 180px; object-fit: cover; cursor: pointer; }
.p-info { padding: 15px; }
.p-name { margin: 0 0 10px 0; font-size: 15px; color: #1e293b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.p-footer { display: flex; justify-content: space-between; align-items: center; }
.p-price { color: #f43f5e; font-weight: 900; font-size: 18px; }
.empty-state { text-align: center; padding-top: 150px; color: #94a3b8; }
</style>