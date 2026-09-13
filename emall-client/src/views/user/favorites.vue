<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Delete, StarFilled, ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import { useCartStore } from '../../stores/cart'
import UserCenterLayout from '../../components/UserCenterLayout.vue'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const products = ref<any[]>([])
const loading = ref(false)

const fetchFavorites = async () => {
  if (!userStore.userInfo) return
  loading.value = true
  try {
    const res: any = await request.get('/favorite/list', { params: { userId: userStore.userInfo.id } })
    products.value = res || []
  } finally {
    loading.value = false
  }
}

const uncollect = async (productId: number) => {
  try {
    await request.post('/favorite/toggle', { userId: userStore.userInfo.id, productId })
    ElMessage.success('已从收藏夹移出')
    fetchFavorites()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleQuickAdd = async (product: any) => {
  try {
    const skus: any = await request.get(`/product/skus/${product.id}`)
    const defaultSku = Array.isArray(skus) && skus.length > 0 ? skus[0] : null
    
    cartStore.addToCart({
      ...product,
      price: defaultSku ? defaultSku.price : product.price,
      stock: defaultSku ? defaultSku.stock : product.stock
    }, 1, defaultSku?.specName || '标准规格', defaultSku?.id || 0)
    
    ElMessage.success(`已将「${product.name}」加入购物车！`)
  } catch (e) {
    ElMessage.error('加入购物车失败')
  }
}

onMounted(() => fetchFavorites())
</script>

<template>
  <UserCenterLayout activeMenu="favorites" pageTitle="我的收藏夹">
    <div class="favorites-view-wrap" v-loading="loading">
      <div class="section-title-bar">
        <div>
          <h2>我的收藏夹</h2>
          <span class="sub-hint">您关注并收藏的优质好物，价格变动第一时间提醒</span>
        </div>
        <span class="fav-count-pill" v-if="products.length > 0">共收藏 {{ products.length }} 件商品</span>
      </div>

      <!-- 空状态 -->
      <div v-if="products.length === 0 && !loading" class="empty-fav-view">
        <div class="empty-icon-circle">
          <el-icon :size="56" color="#cbd5e1"><StarFilled /></el-icon>
        </div>
        <h3>您的收藏夹还是空的</h3>
        <p>把心动的自营好物收藏起来，随时掌握降价动态~</p>
        <el-button type="primary" round class="go-shop-btn" @click="router.push('/')">
          去商城探索好物
        </el-button>
      </div>

      <!-- 商品卡片瀑布网格 -->
      <div class="fav-products-grid" v-else>
        <div v-for="p in products" :key="p.id" class="fav-item-card">
          <div class="fav-img-wrap" @click="router.push(`/product/${p.id}`)">
            <img :src="p.picUrl" class="fav-prod-img" loading="lazy" />
            <span class="tag-ziying">官方自营</span>
          </div>

          <div class="fav-info-body">
            <h4 class="fav-title" :title="p.name" @click="router.push(`/product/${p.id}`)">
              {{ p.name }}
            </h4>
            
            <div class="fav-price-row">
              <span class="curr">¥</span>
              <span class="price-num">{{ p.price }}</span>
            </div>

            <div class="fav-card-actions">
              <el-button 
                type="primary" 
                size="small" 
                class="add-cart-btn" 
                :icon="ShoppingCart"
                @click="handleQuickAdd(p)"
              >
                加购物车
              </el-button>
              <el-button 
                link 
                type="info" 
                size="small" 
                :icon="Delete"
                @click="uncollect(p.id)"
              >
                移除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </UserCenterLayout>
</template>

<style scoped>
.favorites-view-wrap {
  width: 100%;
}

.section-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.section-title-bar h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: #0f172a;
}

.sub-hint {
  font-size: 13px;
  color: #64748b;
}

.fav-count-pill {
  font-size: 13px;
  color: #0284c7;
  font-weight: bold;
}

/* 空状态 */
.empty-fav-view {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon-circle {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: #f8fafc;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto 16px;
}

.empty-fav-view h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
}

.empty-fav-view p {
  margin: 0 0 20px;
  font-size: 13px;
  color: #64748b;
}

.go-shop-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-weight: bold;
}

/* 商品卡片网格 */
.fav-products-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.fav-item-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.2s, box-shadow 0.2s;
}

.fav-item-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.fav-img-wrap {
  width: 100%;
  height: 200px;
  position: relative;
  cursor: pointer;
  background: #f8fafc;
}

.fav-prod-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.tag-ziying {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #0ea5e9;
  color: #ffffff;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 4px;
}

.fav-info-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fav-title {
  margin: 0;
  font-size: 14px;
  font-weight: bold;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}

.fav-title:hover {
  color: #0284c7;
}

.fav-price-row {
  display: flex;
  align-items: baseline;
}

.curr {
  font-size: 14px;
  font-weight: bold;
  color: #f43f5e;
}

.price-num {
  font-size: 22px;
  font-weight: 900;
  color: #f43f5e;
}

.fav-card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
  padding-top: 10px;
  border-top: 1px dashed #f1f5f9;
}

.add-cart-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  border-radius: 14px;
  font-weight: bold;
}
</style>