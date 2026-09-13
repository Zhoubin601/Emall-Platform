<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Edit, Goods, Ticket, Delete, ChatLineSquare } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserCenterLayout from '../../components/UserCenterLayout.vue'

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
  return rawUrl || ''
}

const goToEdit = (c: any) => {
  router.push({
    path: '/order/comment',
    query: { commentId: c.id, productId: c.productId, orderId: c.orderId }
  })
}

const handleDelete = (c: any) => {
  ElMessageBox.confirm('确定要删除这条评价吗？删除后将无法恢复。', '删除评价确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await request.delete(`/comment/${c.id}`)
      ElMessage.success('评价已成功删除')
      fetchMyComments()
    } catch (error) {
      ElMessage.error('删除评价失败，请重试')
    }
  }).catch(() => {})
}

onMounted(() => fetchMyComments())
</script>

<template>
  <UserCenterLayout activeMenu="comments" pageTitle="我的评价中心">
    <div class="my-comments-view-wrap" v-loading="loading">
      <div class="section-title-bar">
        <div>
          <h2>我的评价与买家秀</h2>
          <span class="sub-hint">回看您的真实使用体验与分享，支持随时重新编辑与管理</span>
        </div>
        <span class="comment-count-pill" v-if="comments.length > 0">共发表 {{ comments.length }} 条评价</span>
      </div>

      <!-- 空状态 -->
      <div v-if="comments.length === 0 && !loading" class="empty-comments-view">
        <div class="empty-icon-circle">
          <el-icon :size="56" color="#cbd5e1"><ChatLineSquare /></el-icon>
        </div>
        <h3>您还没有发表过商品评价</h3>
        <p>购买商品并确认收货后，即可发表专属好评与买家秀照片~</p>
        <el-button type="primary" round class="go-orders-btn" @click="router.push('/orders')">
          前往我的订单
        </el-button>
      </div>

      <!-- 评价列表 -->
      <div class="comments-card-list" v-else>
        <div v-for="c in comments" :key="c.id" class="user-comment-card">
          <!-- 关联商品条 -->
          <div class="comment-meta-bar">
            <div class="meta-left">
              <span class="meta-tag prod-tag">
                <el-icon><Goods /></el-icon>
                <span>{{ c.productName || '自营严选商品' }}</span>
              </span>
              <span class="meta-tag sn-tag" v-if="c.orderSn">
                <el-icon><Ticket /></el-icon>
                <span>单号：{{ c.orderSn }}</span>
              </span>
            </div>
            <div class="meta-actions">
              <el-button link type="primary" size="small" :icon="Edit" @click="goToEdit(c)">修改评价</el-button>
              <el-button link type="danger" size="small" :icon="Delete" @click="handleDelete(c)">删除评价</el-button>
            </div>
          </div>

          <!-- 评分 -->
          <div class="comment-rating-row">
            <el-rate :model-value="c.star" disabled text-color="#ff9900" size="small" />
            <span class="rating-text">{{ c.star === 5 ? '⭐⭐⭐⭐⭐ 非常满意' : c.star >= 4 ? '⭐⭐⭐⭐ 满意' : '一般' }}</span>
            <span class="comment-time">{{ c.createTime || '近期已评' }}</span>
          </div>

          <!-- 评价内容 -->
          <div class="comment-text-content">
            {{ c.content }}
          </div>

          <!-- 买家秀图片网格 -->
          <div class="comment-photos-grid" v-if="c.pics">
            <el-image 
              v-for="img in c.pics.split(',').filter(Boolean)" 
              :key="img" 
              :src="getSafeUrl(img)" 
              :preview-src-list="c.pics.split(',').filter(Boolean).map(getSafeUrl)"
              fit="cover"
              class="photo-thumb"
            />
          </div>
        </div>
      </div>
    </div>
  </UserCenterLayout>
</template>

<style scoped>
.my-comments-view-wrap {
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

.comment-count-pill {
  font-size: 13px;
  color: #0284c7;
  font-weight: bold;
}

/* 空状态 */
.empty-comments-view {
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

.empty-comments-view h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
}

.empty-comments-view p {
  margin: 0 0 20px;
  font-size: 13px;
  color: #64748b;
}

.go-orders-btn {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
  font-weight: bold;
}

/* 评价卡片列表 */
.comments-card-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.user-comment-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: box-shadow 0.2s;
}

.user-comment-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.comment-meta-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 6px;
}

.prod-tag {
  background-color: #f0f9ff;
  color: #0284c7;
  font-weight: bold;
}

.sn-tag {
  background-color: #f8fafc;
  color: #64748b;
}

.comment-rating-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-text {
  font-size: 12px;
  font-weight: bold;
  color: #f43f5e;
}

.comment-time {
  font-size: 12px;
  color: #94a3b8;
  margin-left: auto;
}

.comment-text-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
}

.comment-photos-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.photo-thumb {
  width: 90px;
  height: 90px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
}
</style>
