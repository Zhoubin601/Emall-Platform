<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, EditPen, Delete, Position } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Notice {
  id?: number;
  title: string;
  content: string;
  isActive: number;
  createTime?: string;
}

const noticeList = ref<Notice[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isSubmitting = ref(false)
const isEdit = ref(false)

const form = reactive<Notice>({
  title: '',
  content: '',
  isActive: 1
})

const fetchNotices = async () => {
  loading.value = true
  try {
    noticeList.value = await request.get('/interaction/notice/list')
  } catch (error) {
    ElMessage.error('获取公告列表失败')
  } finally {
    loading.value = false
  }
}

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
  ElMessageBox.confirm('确定要永久删除这条公告吗？', '删除提示', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    await request.delete(`/interaction/notice/delete/${id}`)
    ElMessage.success('公告已删除！')
    fetchNotices()
  }).catch(() => {})
}

const handleStatusChange = async (row: Notice) => {
  try {
    await request.put('/interaction/notice/update', { id: row.id, isActive: row.isActive })
    ElMessage.success(row.isActive === 1 ? '公告已上线展示' : '公告已下线隐藏')
  } catch (error) {
    row.isActive = row.isActive === 1 ? 0 : 1 // 还原状态
    ElMessage.error('状态切换失败')
  }
}

const handleSubmit = async () => {
  if (!form.title || !form.content) return ElMessage.warning('公告标题和内容不能为空')
  
  isSubmitting.value = true
  try {
    if (isEdit.value) {
      await request.put('/interaction/notice/update', form)
    } else {
      await request.post('/interaction/notice/add', form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '发布成功')
    dialogVisible.value = false
    fetchNotices()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => fetchNotices())
</script>

<template>
  <div class="notice-container">
    <el-card class="glass-card" shadow="hover">
      <div class="toolbar">
        <div class="header-title">📢 全局活动与公告管理</div>
        <el-button type="primary" :icon="Plus" class="cute-btn" round @click="handleAdd">
          发布新公告
        </el-button>
      </div>

      <el-table :data="noticeList" style="width: 100%" v-loading="loading" stripe class="custom-table">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        
        <el-table-column label="公告详情" min-width="350">
          <template #default="{ row }">
            <div class="notice-info">
              <h4 class="n-title">{{ row.title }}</h4>
              <p class="n-desc" :title="row.content">{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="发布时间" width="180" align="center" />
        
        <el-table-column label="前台展示状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch 
              v-model="row.isActive" 
              :active-value="1" 
              :inactive-value="0" 
              active-color="#10b981" 
              @change="handleStatusChange(row)" 
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" :icon="EditPen" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" :icon="Delete" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑公告内容' : '发布全新公告'" 
      width="550px" 
      class="glass-dialog"
      destroy-on-close
    >
      <el-form :model="form" label-position="top" class="rich-form">
        <el-form-item label="公告标题" required>
          <el-input v-model="form.title" placeholder="例如：双十一狂欢开启！" />
        </el-form-item>
        
        <el-form-item label="详细内容" required>
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="6" 
            placeholder="请输入想要告知买家的详细内容..." 
            resize="none"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" round>取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="isSubmitting" :icon="Position" round>
          {{ isEdit ? '保存更新' : '立即发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.notice-container { height: 100%; }
.glass-card { border-radius: 12px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.85); box-shadow: 0 4px 16px rgba(186, 230, 253, 0.2); }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header-title { font-weight: bold; font-size: 18px; color: #0f172a; }
.cute-btn { background: linear-gradient(135deg, #0ea5e9, #0284c7); border: none; box-shadow: 0 4px 10px rgba(14, 165, 233, 0.2); }

:deep(.custom-table th.el-table__cell) { background-color: #f0f9ff; color: #0369a1; border-bottom: none; }

.notice-info { display: flex; flex-direction: column; gap: 5px; }
.n-title { margin: 0; font-size: 15px; font-weight: bold; color: #1e293b; }
.n-desc { margin: 0; font-size: 13px; color: #64748b; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

:deep(.glass-dialog) { border-radius: 16px; background: rgba(255, 255, 255, 0.95); border: 1px solid rgba(186, 230, 253, 0.6); }
:deep(.glass-dialog .el-dialog__header) { border-bottom: 1px solid #e0f2fe; padding: 20px; font-weight: bold; color: #0369a1; }
.rich-form :deep(.el-form-item__label) { font-weight: bold; color: #475569; }
</style>