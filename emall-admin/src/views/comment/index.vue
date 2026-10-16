<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { 
  ChatDotRound, Delete, Picture, Star, Camera, 
  Warning, Search, Refresh, Goods, Clock
} from '@element-plus/icons-vue'
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
const searchQuery = ref('')
const starFilter = ref<number | ''>('')
const onlyWithPics = ref(false)

// === 顶部 Bento 评价指标 ===
const commentStats = computed(() => {
  const total = comments.value.length
  const fiveStars = comments.value.filter(c => c.star === 5).length
  const withPics = comments.value.filter(c => !!c.pics).length
  const lowStars = comments.value.filter(c => c.star <= 2).length
  const satisfactionRate = total > 0 
    ? Math.round((comments.value.filter(c => c.star >= 4).length / total) * 100) 
    : 100
  return { total, fiveStars, withPics, lowStars, satisfactionRate }
})

const handleStatCardClick = (type: 'all' | '5star' | 'pics' | 'low') => {
  if (type === 'all') {
    starFilter.value = ''
    onlyWithPics.value = false
  } else if (type === '5star') {
    starFilter.value = 5
    onlyWithPics.value = false
  } else if (type === 'pics') {
    starFilter.value = ''
    onlyWithPics.value = true
  } else if (type === 'low') {
    starFilter.value = 2 // 过滤低分
    onlyWithPics.value = false
  }
}

const fetchComments = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/comment/list')
    comments.value = Array.isArray(res) ? res : (res?.data || res?.records || [])
  } catch (e) {
    ElMessage.error('评价数据拉取失败')
  } finally {
    loading.value = false
  }
}

const filteredComments = computed(() => {
  return comments.value.filter(c => {
    const query = searchQuery.value.toLowerCase().trim()
    const matchQuery = !query || 
      (c.nickname && c.nickname.toLowerCase().includes(query)) ||
      (c.productName && c.productName.toLowerCase().includes(query)) ||
      (c.content && c.content.toLowerCase().includes(query))

    let matchStar = true
    if (starFilter.value !== '') {
      if (starFilter.value === 2) matchStar = c.star <= 2
      else matchStar = c.star === starFilter.value
    }

    const matchPics = !onlyWithPics.value || !!c.pics

    return matchQuery && matchStar && matchPics
  })
})

const handleResetFilters = () => {
  searchQuery.value = ''
  starFilter.value = ''
  onlyWithPics.value = false
}

const getSafeUrl = (rawUrl: string) => {
  if (!rawUrl) return ''
  return rawUrl.trim()
}

const getStarDesc = (star: number) => {
  if (star >= 5) return '极佳推荐'
  if (star >= 4) return '满意好评'
  if (star === 3) return '中规中矩'
  return '有待改善'
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要彻底删除这条违规评价吗？此操作不可逆！', '违规内容清理', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
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
    <!-- 1. 顶部 Mini Bento 评价分析看板 -->
    <div class="comment-stats-grid">
      <div 
        class="stat-bento-card" 
        :class="{ active: starFilter === '' && !onlyWithPics }"
        @click="handleStatCardClick('all')"
      >
        <div class="stat-icon-wrap blue"><el-icon><ChatDotRound /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">全平台累计评价</div>
          <div class="stat-val">{{ commentStats.total }} <span class="unit">条</span></div>
        </div>
        <span class="stat-tag">全景</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: starFilter === 5 }"
        @click="handleStatCardClick('5star')"
      >
        <div class="stat-icon-wrap amber"><el-icon><Star /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">五星满分好评</div>
          <div class="stat-val">{{ commentStats.fiveStars }} <span class="unit">条</span></div>
        </div>
        <span class="stat-tag tag-green">满意度 {{ commentStats.satisfactionRate }}%</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: onlyWithPics }"
        @click="handleStatCardClick('pics')"
      >
        <div class="stat-icon-wrap purple"><el-icon><Camera /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">带图晒单分享</div>
          <div class="stat-val">{{ commentStats.withPics }} <span class="unit">条</span></div>
        </div>
        <span class="stat-tag tag-purple">图文并茂</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: starFilter === 2 }"
        @click="handleStatCardClick('low')"
      >
        <div class="stat-icon-wrap red"><el-icon><Warning /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">差评关注 (≤2星)</div>
          <div class="stat-val" :class="{ 'text-danger': commentStats.lowStars > 0 }">
            {{ commentStats.lowStars }} <span class="unit">条</span>
          </div>
        </div>
        <span class="stat-tag tag-red" v-if="commentStats.lowStars > 0">需复盘</span>
        <span class="stat-tag tag-green" v-else>零差评</span>
      </div>
    </div>

    <!-- 2. 主卡片与操作工具栏 -->
    <el-card class="main-comment-card" shadow="never">
      <div class="toolbar-header">
        <div class="filter-controls">
          <el-input 
            v-model="searchQuery" 
            placeholder="搜索买家、商品名或评价内容..." 
            class="search-input" 
            clearable 
            :prefix-icon="Search"
          />
          <el-select v-model="starFilter" placeholder="评分级别" clearable class="filter-select">
            <el-option label="全部评分" :value="''" />
            <el-option label="★★★★★ 5星好评" :value="5" />
            <el-option label="★★★★☆ 4星满意" :value="4" />
            <el-option label="★★★☆☆ 3星中评" :value="3" />
            <el-option label="≤ 2星 差评关注" :value="2" />
          </el-select>
          <el-checkbox v-model="onlyWithPics" label="仅看图文晒单" class="pic-checkbox" />
          <el-button 
            v-if="searchQuery || starFilter !== '' || onlyWithPics" 
            link 
            type="primary" 
            :icon="Refresh" 
            @click="handleResetFilters"
          >
            重置
          </el-button>
        </div>

        <div class="action-controls">
          <span class="count-tip">共筛选出 <strong>{{ filteredComments.length }}</strong> 条评价</span>
        </div>
      </div>

      <el-table :data="filteredComments" v-loading="loading" class="custom-admin-table" style="width: 100%">
        <!-- ID -->
        <el-table-column prop="id" label="ID" width="75" align="center">
          <template #default="{ row }">
            <span class="row-id">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <!-- 评价买家 -->
        <el-table-column label="评价买家" width="150">
          <template #default="{ row }">
            <div class="buyer-cell">
              <el-avatar :size="32" class="buyer-avatar">
                {{ row.nickname.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="buyer-name" :title="row.nickname">{{ row.nickname }}</div>
            </div>
          </template>
        </el-table-column>

        <!-- 评价商品 -->
        <el-table-column label="关联商品" min-width="200">
          <template #default="{ row }">
            <div class="product-cell-box">
              <span class="p-tag"><el-icon><Goods /></el-icon> 官方自营</span>
              <div class="p-name" :title="row.productName">
                {{ row.productName || '商品快照读取中...' }}
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 星级评分 -->
        <el-table-column label="体验评分" width="160" align="center">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-rate v-model="row.star" disabled text-color="#f59e0b" />
              <span class="rate-desc" :class="'star-' + row.star">
                {{ getStarDesc(row.star) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- 评价心得正文 -->
        <el-table-column prop="content" label="买家真实心声" min-width="260">
          <template #default="{ row }">
            <div class="content-bubble">
              "{{ row.content }}"
            </div>
          </template>
        </el-table-column>

        <!-- 买家晒图 -->
        <el-table-column label="实物晒单" width="180">
          <template #default="{ row }">
            <div class="pics-box" v-if="row.pics && row.pics.trim()">
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
            <span v-else class="no-pic-text">未配图纯文本</span>
          </template>
        </el-table-column>

        <!-- 评价时间 -->
        <el-table-column label="发表时间" width="170" align="center">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Clock /></el-icon> {{ row.createTime }}
            </div>
          </template>
        </el-table-column>

        <!-- 审核操作 -->
        <el-table-column label="审核管理" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              type="danger" 
              plain 
              size="small" 
              :icon="Delete" 
              class="del-btn"
              @click="handleDelete(row.id)"
            >
              删除违规
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.comment-admin-container {
  min-height: 100%;
}

/* 1. Mini Bento 评价指标网格 */
.comment-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-bento-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  position: relative;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.stat-bento-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.stat-bento-card.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.12);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-right: 14px;
  flex-shrink: 0;
}

.stat-icon-wrap.blue { background: #eff6ff; color: #2563eb; }
.stat-icon-wrap.amber { background: #fef3c7; color: #d97706; }
.stat-icon-wrap.purple { background: #faf5ff; color: #7c3aed; }
.stat-icon-wrap.red { background: #fef2f2; color: #dc2626; }

.stat-meta {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-bottom: 4px;
}

.stat-val {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.stat-val .unit {
  font-size: 12px;
  font-weight: normal;
  color: #94a3b8;
  margin-left: 2px;
}

.stat-val.text-danger {
  color: #ef4444;
}

.stat-tag {
  position: absolute;
  top: 14px;
  right: 14px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
}

.stat-tag.tag-green { background: #dcfce7; color: #15803d; }
.stat-tag.tag-purple { background: #f3e8ff; color: #6b21a8; }
.stat-tag.tag-red { background: #fee2e2; color: #b91c1c; }

/* 2. 主表格卡片与工具栏 */
.main-comment-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.toolbar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 18px;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input { width: 260px; }
.filter-select { width: 170px; }
.pic-checkbox { margin-left: 4px; }

.action-controls {
  display: flex;
  align-items: center;
}

.count-tip {
  font-size: 13px;
  color: #64748b;
}

.count-tip strong {
  color: #0f172a;
}

/* 3. 表格自定义样式 */
:deep(.custom-admin-table th.el-table__cell) {
  background-color: #f8fafc;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 0;
}

.row-id {
  font-family: ui-monospace, SFMono-Regular, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}

.buyer-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.buyer-avatar {
  background: #e0f2fe;
  color: #0284c7;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
}

.buyer-name {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-cell-box {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.p-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #0284c7;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
}

.p-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rate-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.rate-desc {
  font-size: 11px;
  font-weight: 600;
}

.rate-desc.star-5 { color: #16a34a; }
.rate-desc.star-4 { color: #0284c7; }
.rate-desc.star-3 { color: #d97706; }
.rate-desc.star-2, .rate-desc.star-1 { color: #dc2626; }

.content-bubble {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: #334155;
  line-height: 1.5;
}

.pics-box {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.c-img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  cursor: zoom-in;
  border: 1px solid #e2e8f0;
  transition: transform 0.2s ease;
}

.c-img:hover {
  transform: scale(1.08);
}

.img-error {
  width: 100%;
  height: 100%;
  background: #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #cbd5e1;
}

.no-pic-text {
  color: #94a3b8;
  font-size: 12px;
}

.time-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.del-btn {
  border-radius: 6px;
  font-weight: 500;
}

@media (max-width: 1200px) {
  .comment-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
