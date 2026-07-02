<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// ✨ 引入了 Delete 图标和弹窗组件
import { Back, Edit, Calendar, Goods, Ticket, Delete } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const comments = ref<any[]>([])
const loading = ref(false)

const fetchMyComments = async () => {
  if (!userStore.userInfo) return router.push('/login')
  loading.value = true
  try {
    const res = await request.get<any, any[]>('/comment/my', { 
      params: { userId: userStore.userInfo.id } 
    })
    comments.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

const getSafeUrl = (rawUrl: string) => {
  if (!rawUrl) return ''
  const fileName = rawUrl.substring(rawUrl.lastIndexOf('/') + 1)
  return `/uploads/${encodeURIComponent(fileName)}`
}

const goToEdit = (c: any) => {
  router.push({
    path: '/order/comment',
    query: { commentId: c.id, productId: c.productId, orderId: c.orderId }
  })
}

// ✨ 新增：删除评价的业务逻辑
const handleDelete = (c: any) => {
  ElMessageBox.confirm('确定要删除这条评价吗？删除后将无法恢复。', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await request.delete(`/comment/${c.id}`)
      ElMessage.success('评价已成功删除')
      fetchMyComments() // 重新获取评价列表
    } catch (error) {
      ElMessage.error('删除评价失败，请重试')
    }
  }).catch(() => {})
}

onMounted(() => fetchMyComments())
</script>

<template>
  <div class="my-comments-page">
    <header class="glass-header">
      <div class="header-content">
        <el-button link :icon="Back" @click="router.push('/')">返回首页</el-button>
        <span class="page-title">评价管理中心</span>
      </div>
    </header>

    <main class="container">
      <div v-loading="loading">
        <el-empty v-if="comments.length === 0 && !loading" description="您还没有发表过评价哦~" />

        <el-card v-for="c in comments" :key="c.id" class="comment-card glass-card">
          <div class="meta-info">
            <div class="tag product-name">
              <el-icon><Goods /></el-icon>
              <span>{{ c.productName || '精选好物' }}</span>
            </div>
            <div class="tag order-sn">
              <el-icon><Ticket /></el-icon>
              <span>单号：{{ c.orderSn || '读取中...' }}</span>
            </div>
          </div>

          <div class="card-header">
            <el-rate v-model="c.star" disabled show-score />
            <div class="action-group">
              <el-button type="primary" link :icon="Edit" @click="goToEdit(c)">修改评价</el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDelete(c)">删除评价</el-button>
            </div>
          </div>

          <div class="comment-body">
            <p class="text-content">{{ c.content }}</p>
            
            <div class="image-gallery" v-if="c.pics">
              <el-image 
                v-for="img in c.pics.split(',')" 
                :key="img" 
                :src="getSafeUrl(img)" 
                :preview-src-list="c.pics.split(',').map(url => getSafeUrl(url))"
                class="comment-img" 
                fit="cover"
              >
                <template #error>
                  <div class="error-tip">图片加载失败</div>
                </template>
              </el-image>
            </div>
          </div>

          <div class="card-footer">
            <div class="time-box">
              <el-icon><Calendar /></el-icon>
              <span>最后更新：{{ c.createTime }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </main>
  </div>
</template>

<style scoped>
.my-comments-page { min-height: 100vh; background: #f0f9ff; padding-top: 80px; padding-bottom: 40px; }
.glass-header { position: fixed; top: 0; width: 100%; height: 60px; background: rgba(255,255,255,0.7); backdrop-filter: blur(10px); z-index: 100; display: flex; justify-content: center; border-bottom: 1px solid #e0f2fe; }
.header-content { width: 800px; display: flex; align-items: center; padding: 0 20px; }
.page-title { margin-left: 20px; font-weight: bold; color: #0369a1; }
.container { width: 800px; max-width: 95%; margin: 0 auto; }
.glass-card { border-radius: 20px; border: none; background: rgba(255,255,255,0.85); margin-bottom: 25px; box-shadow: 0 10px 30px rgba(186, 230, 253, 0.3); padding: 15px; }

.meta-info { display: flex; gap: 15px; margin-bottom: 15px; }
.tag { display: flex; align-items: center; gap: 6px; padding: 5px 12px; border-radius: 10px; font-size: 13px; font-weight: bold; }
.product-name { background: #e0f2fe; color: #0369a1; }
.order-sn { background: #f1f5f9; color: #64748b; }

.card-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px dashed #e2e8f0; padding-bottom: 10px; margin-bottom: 15px; }
/* ✨ 新增操作组的样式，让修改和删除按钮有一些间隔 */
.action-group { display: flex; gap: 15px; }

.text-content { color: #475569; line-height: 1.6; margin-bottom: 15px; font-size: 15px; }

.image-gallery { display: flex; gap: 10px; margin-bottom: 10px; }
.comment-img { width: 100px; height: 100px; border-radius: 12px; border: 2px solid #fff; cursor: zoom-in; }

.card-footer { display: flex; justify-content: flex-end; color: #94a3b8; font-size: 12px; margin-top: 10px; }
.time-box { display: flex; align-items: center; gap: 5px; background: #f8fafc; padding: 5px 15px; border-radius: 20px; }

.error-tip { background: #f1f5f9; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 11px; color: #94a3b8; }
</style>