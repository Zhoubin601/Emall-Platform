<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { 
  Plus, 
  EditPen, 
  Delete, 
  Position, 
  Bell, 
  Check, 
  Hide, 
  TrendCharts, 
  Search, 
  Refresh 
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Notice {
  id?: number
  title: string
  content: string
  isActive: number
  createTime?: string
}

const noticeList = ref<Notice[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isSubmitting = ref(false)
const isEdit = ref(false)
const searchQuery = ref('')
const statusFilter = ref<'all' | 'active' | 'inactive'>('all')

const form = reactive<Notice>({
  title: '',
  content: '',
  isActive: 1
})

const fetchNotices = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/interaction/notice/list')
    noticeList.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

// 4 Mini Bento 统计指标
const totalNoticesCount = computed(() => noticeList.value.length)
const activeNoticesCount = computed(() => noticeList.value.filter(n => n.isActive === 1).length)
const inactiveNoticesCount = computed(() => noticeList.value.filter(n => n.isActive === 0).length)
const coverageRate = computed(() => {
  if (totalNoticesCount.value === 0) return '100%'
  return Math.round((activeNoticesCount.value / totalNoticesCount.value) * 100) + '%'
})

// 过滤后的列表
const filteredNoticeList = computed(() => {
  let list = noticeList.value
  if (statusFilter.value === 'active') {
    list = list.filter(n => n.isActive === 1)
  } else if (statusFilter.value === 'inactive') {
    list = list.filter(n => n.isActive === 0)
  }
  if (searchQuery.value.trim()) {
    const kw = searchQuery.value.trim().toLowerCase()
    list = list.filter(n => 
      (n.title && n.title.toLowerCase().includes(kw)) ||
      (n.content && n.content.toLowerCase().includes(kw))
    )
  }
  return list
})

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: undefined, title: '', content: '', isActive: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: Notice) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要永久删除这条商城公告吗？删除后前台首页跑马灯将不再轮播。', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/interaction/notice/delete/${id}`)
    ElMessage.success('公告已成功删除！')
    fetchNotices()
  }).catch(() => {})
}

const handleStatusChange = async (row: Notice) => {
  try {
    await request.put('/interaction/notice/update', { id: row.id, isActive: row.isActive })
    ElMessage.success(row.isActive === 1 ? '公告已上线发布至前台' : '公告已下线隐藏')
  } catch (error) {
    row.isActive = row.isActive === 1 ? 0 : 1 // 还原状态
    ElMessage.error('状态切换失败')
  }
}

const handleSubmit = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    return ElMessage.warning('公告标题和正文内容不能为空')
  }
  
  isSubmitting.value = true
  try {
    if (isEdit.value) {
      await request.put('/interaction/notice/update', form)
    } else {
      await request.post('/interaction/notice/add', form)
    }
    ElMessage.success(isEdit.value ? '公告更新成功' : '全新公告发布成功')
    dialogVisible.value = false
    fetchNotices()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(fetchNotices)
</script>

<template>
  <div class="notice-manage-wrapper">
    <!-- 4 Mini Bento 统计指标卡 -->
    <div class="bento-grid">
      <div class="bento-card bento-total">
        <div class="bento-icon-box"><el-icon><Bell /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ totalNoticesCount }}</div>
          <div class="bento-label">全站公告总数</div>
        </div>
      </div>

      <div class="bento-card bento-active">
        <div class="bento-icon-box"><el-icon><Check /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ activeNoticesCount }}</div>
          <div class="bento-label">前台生效展示中</div>
        </div>
      </div>

      <div class="bento-card bento-inactive">
        <div class="bento-icon-box"><el-icon><Hide /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ inactiveNoticesCount }}</div>
          <div class="bento-label">草稿 / 已隐藏</div>
        </div>
      </div>

      <div class="bento-card bento-rate">
        <div class="bento-icon-box"><el-icon><TrendCharts /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ coverageRate }}</div>
          <div class="bento-label">在线公告活跃率</div>
        </div>
      </div>
    </div>

    <!-- 主表格卡片 -->
    <el-card class="glass-card main-table-card" shadow="never">
      <!-- 过滤工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-radio-group v-model="statusFilter" size="default">
            <el-radio-button label="all">全部 ({{ totalNoticesCount }})</el-radio-button>
            <el-radio-button label="active">展示中 ({{ activeNoticesCount }})</el-radio-button>
            <el-radio-button label="inactive">已隐藏 ({{ inactiveNoticesCount }})</el-radio-button>
          </el-radio-group>

          <el-input 
            v-model="searchQuery" 
            placeholder="搜索公告标题或内容关键词..." 
            style="width: 280px" 
            clearable 
            :prefix-icon="Search"
          />
        </div>

        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="fetchNotices" size="default" plain>刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd" size="default" class="btn-create-notice">
            发布新公告
          </el-button>
        </div>
      </div>

      <!-- 表格内容 -->
      <el-table :data="filteredNoticeList" style="width: 100%" v-loading="loading" stripe class="custom-admin-table">
        <el-table-column prop="id" label="ID" width="80" align="center">
          <template #default="{ row }">
            <span class="notice-id-badge">#{{ row.id }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="公告标题与核心摘要" min-width="360">
          <template #default="{ row }">
            <div class="notice-info-cell">
              <div class="notice-header-row">
                <span class="broadcast-tag">📢 商城广播</span>
                <strong class="n-title">{{ row.title }}</strong>
              </div>
              <p class="n-desc" :title="row.content">{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="发布时间" width="180" align="center">
          <template #default="{ row }">
            <span class="time-col">{{ row.createTime || '-' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="前台展示状态" width="140" align="center">
          <template #default="{ row }">
            <div class="status-toggle-cell">
              <el-switch 
                v-model="row.isActive" 
                :active-value="1" 
                :inactive-value="0" 
                active-color="#10b981" 
                @change="handleStatusChange(row)" 
              />
              <span class="status-state-text" :class="row.isActive === 1 ? 'is-active' : 'is-inactive'">
                {{ row.isActive === 1 ? '展示中' : '已隐藏' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="EditPen" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发布/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑商城公告' : '发布全新商城公告'" 
      width="580px" 
      class="custom-notice-dialog"
      destroy-on-close
    >
      <el-form :model="form" label-position="top">
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" placeholder="例如：🎉 欢迎来到 E-MALL 全球品质严选商城！" />
        </el-form-item>
        
        <el-form-item label="详细正文内容" required>
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="5" 
            placeholder="请输入想要告知全站买家的活动公告或时效升级详情..." 
            resize="none"
          />
        </el-form-item>

        <el-form-item label="前台展示设置">
          <el-radio-group v-model="form.isActive">
            <el-radio :label="1">立即上线在前台顶部轮播广播</el-radio>
            <el-radio :label="0">暂存草稿 (暂不上线)</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" round>取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="isSubmitting" :icon="Position" round>
            {{ isEdit ? '保存更新' : '立即发布' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.notice-manage-wrapper { 
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0;
}

/* 4 Mini Bento 统计卡 */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.bento-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: transform 0.2s, box-shadow 0.2s;
}

.bento-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
}

.bento-icon-box {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.bento-total .bento-icon-box { background: #e0f2fe; color: #0284c7; }
.bento-active .bento-icon-box { background: #dcfce7; color: #16a34a; }
.bento-inactive .bento-icon-box { background: #fee2e2; color: #ef4444; }
.bento-rate .bento-icon-box { background: #fef3c7; color: #d97706; }

.bento-content {
  display: flex;
  flex-direction: column;
}

.bento-num {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.bento-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

/* 表格卡片 */
.main-table-card {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-create-notice {
  font-weight: 600;
  border-radius: 8px;
}

.notice-id-badge {
  font-family: monospace;
  font-weight: 700;
  color: #64748b;
}

.notice-info-cell { 
  display: flex; 
  flex-direction: column; 
  gap: 6px; 
}

.notice-header-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.broadcast-tag {
  font-size: 11px;
  font-weight: 700;
  color: #0284c7;
  background: #e0f2fe;
  padding: 1px 6px;
  border-radius: 6px;
}

.n-title { 
  font-size: 14px; 
  color: #0f172a; 
}

.n-desc { 
  margin: 0; 
  font-size: 13px; 
  color: #64748b; 
  line-height: 1.5; 
  display: -webkit-box; 
  -webkit-line-clamp: 2; 
  -webkit-box-orient: vertical; 
  overflow: hidden; 
}

.time-col {
  font-size: 12px;
  color: #64748b;
}

.status-toggle-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.status-state-text {
  font-size: 11px;
  font-weight: 600;
}

.status-state-text.is-active {
  color: #16a34a;
}

.status-state-text.is-inactive {
  color: #94a3b8;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>