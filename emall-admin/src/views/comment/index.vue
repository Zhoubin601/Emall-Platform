<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ChatDotRound, Delete, Picture } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Comment {
  id: number
  nickname: string
  productName?: string
  star: number
  content: string
  pics?: string
  createTime: string
}

const comments = ref<Comment[]>([])
const loading = ref(false)

const fetchComments = async () => {
  loading.value = true
  try {
    const res = await request.get<any, Comment[]>('/comment/list')
    comments.value = res
  } catch (e) {
    ElMessage.error('评价数据拉取失败')
  } finally {
    loading.value = false
  }
}

const getSafeUrl = (rawUrl: string) => {
  if (!rawUrl) return ''
  if (rawUrl.startsWith('http')) return rawUrl
  const fileName = rawUrl.substring(rawUrl.lastIndexOf('/') + 1)
  return `/uploads/${encodeURIComponent(fileName)}`
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要彻底删除这条评价吗？', '违规内容清理', {
    confirmButtonText: '确定删除',
    type: 'error'
  }).then(async () => {
    try {
      await request.delete(`/comment/${id}`)
      ElMessage.success('评价已从数据库彻底移除')
      fetchComments()
    } catch (e) {
      ElMessage.error('删除操作失败')
    }
  }).catch(() => {})
}

onMounted(() => fetchComments())
</script>

<template>
  <div class="comment-admin-container">
    <el-card class="glass-card" shadow="never">
      <div class="header-title">
        <el-icon class="title-icon"><ChatDotRound /></el-icon>
        <span>评价审核中心</span>
      </div>

      <el-table :data="comments" v-loading="loading" stripe class="custom-table">
        <el-table-column label="评价用户" width="130">
          <template #default="{ row }">
            <strong>{{ row.nickname }}</strong>
          </template>
        </el-table-column>

        <el-table-column label="评价商品" width="240">
          <template #default="{ row }">
            <div class="product-cell-box">
              <el-tag size="small" effect="plain" class="p-tag">🎁 购入商品</el-tag>
              <div class="p-name" :title="row.productName">
                {{ row.productName || '商品信息读取中...' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="评分" width="150" align="center">
          <template #default="{ row }">
            <el-rate v-model="row.star" disabled />
          </template>
        </el-table-column>

        <el-table-column prop="content" label="评价心得" min-width="200" show-overflow-tooltip />

        <el-table-column label="买家晒图" width="220">
          <template #default="{ row }">
            <div class="pics-box" v-if="row.pics">
              <el-image 
                v-for="(img, index) in row.pics.split(',')" 
                :key="index" 
                :src="getSafeUrl(img)" 
                :preview-src-list="row.pics.split(',').map(getSafeUrl)"
                class="c-img" 
                fit="cover" 
                hide-on-click-modal
                preview-teleported
              >
                <template #error>
                  <div class="img-error"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
            </div>
            <span v-else class="no-pic-text">未上传图片</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="评价时间" width="180" align="center" />

        <el-table-column label="审核操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row.id)">删除违规</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.comment-admin-container { height: 100%; }
.glass-card { border-radius: 12px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(10px); }
.header-title { font-size: 18px; font-weight: bold; color: #0369a1; margin-bottom: 25px; display: flex; align-items: center; gap: 10px; }
.title-icon { font-size: 24px; color: #38bdf8; }

/* ✨ 商品名称专用样式 */
.product-cell-box { display: flex; flex-direction: column; gap: 6px; }
.p-tag { width: fit-content; border-color: #bae6fd; color: #0284c7; border-radius: 6px; }
.p-name { font-weight: bold; color: #1e293b; font-size: 13px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.pics-box { display: flex; gap: 8px; flex-wrap: wrap; }
.c-img { width: 45px; height: 45px; border-radius: 8px; cursor: zoom-in; border: 2px solid #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.img-error { width: 100%; height: 100%; background: #f1f5f9; display: flex; align-items: center; justify-content: center; color: #cbd5e1; }
.no-pic-text { color: #cbd5e1; font-size: 12px; font-style: italic; }

:deep(.custom-table th.el-table__cell) { background-color: #f0f9ff; color: #0369a1; border-bottom: none; }
</style>