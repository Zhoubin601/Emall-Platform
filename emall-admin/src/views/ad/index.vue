<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Plus, UploadFilled } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Ad {
  id?: number; title: string; picUrl: string; linkUrl: string; sort: number; status: number;
}

const adList = ref<Ad[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const searchQuery = ref('')

const form = reactive<Ad>({
  title: '', picUrl: '', linkUrl: '', sort: 0, status: 1
})

const fetchAds = async () => {
  loading.value = true
  try { adList.value = await request.get('/ad/list', { params: { title: searchQuery.value } }) } 
  finally { loading.value = false }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: undefined, title: '', picUrl: '', linkUrl: '', sort: 0, status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: Ad) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除这张轮播图吗？', '提示', { type: 'warning' }).then(async () => {
    await request.delete(`/ad/delete/${id}`)
    ElMessage.success('删除成功')
    fetchAds()
  })
}

const handleStatusChange = async (row: Ad) => {
  await request.put('/ad/update', { id: row.id, status: row.status })
  ElMessage.success('状态已同步')
}

// 图片上传引擎
const uploadImage = async (options: any) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res: any = await request.post('/file/uploadCommon', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.picUrl = res.url || res.data?.url || res.data || res
    ElMessage.success('海报本地上传成功！')
  } catch (e) {
    ElMessage.error('上传失败，请检查后端服务是否正常')
  }
}

const submitForm = async () => {
  if (!form.picUrl) return ElMessage.warning('海报图片链接不能为空')
  try {
    if (isEdit.value) await request.put('/ad/update', form)
    else await request.post('/ad/add', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchAds()
  } catch (e) {}
}

onMounted(fetchAds)
</script>

<template>
  <div class="ad-container">
    <el-card class="glass-card">
      <div class="toolbar">
        <el-input v-model="searchQuery" placeholder="搜索广告标题..." style="width: 250px" clearable @keyup.enter="fetchAds">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" :icon="Plus" @click="handleAdd" round>新增轮播图</el-button>
      </div>

      <el-table :data="adList" v-loading="loading" stripe>
        <el-table-column label="预览图" width="200">
          <template #default="{ row }">
            <el-image :src="row.picUrl" fit="cover" class="ad-preview" :preview-src-list="[row.picUrl]" />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column prop="linkUrl" label="跳转链接" min-width="150">
          <template #default="{ row }"><span class="link-text">{{ row.linkUrl || '未设置' }}</span></template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑广告' : '新增广告'" width="550px">
      <el-form label-width="80px">
        <el-form-item label="广告标题"><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        
        <el-form-item label="海报图片" required>
          <div style="display: flex; flex-direction: column; gap: 10px; width: 100%;">
            <el-input v-model="form.picUrl" placeholder="可直接粘贴网络图片链接，或使用下方云朵上传" clearable />
            <el-upload 
              action="#" 
              class="ad-uploader" 
              :show-file-list="false" 
              :http-request="uploadImage"
              accept="image/*"
            >
              <div class="uploader-content">
                <img v-if="form.picUrl" :src="form.picUrl" class="upload-img" />
                <el-icon v-else class="uploader-icon"><UploadFilled /></el-icon>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="跳转链接"><el-input v-model="form.linkUrl" placeholder="例如: /product/1" /></el-form-item>
        <el-form-item label="排序值"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 20px; }
.ad-preview { width: 150px; height: 60px; border-radius: 8px; border: 1px solid #eee; }

/* ✨ 核心修复：利用深度选择器强制让原生上传块宽度 100% 并显示为块级元素 */
:deep(.ad-uploader .el-upload) { width: 100%; display: block; }

/* ✨ 核心修复：将样式赋给内部的结构盒子 */
.uploader-content { 
  border: 1px dashed #d9d9d9; 
  border-radius: 8px; 
  cursor: pointer; 
  position: relative; 
  overflow: hidden; 
  width: 100%; 
  height: 160px; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  background: #fafafa; 
  transition: border-color 0.3s; 
}
.uploader-content:hover { border-color: #0ea5e9; }

.upload-img { width: 100%; height: 100%; object-fit: cover; }
.uploader-icon { font-size: 28px; color: #8c939d; }
.link-text { font-size: 12px; color: #94a3b8; word-break: break-all; }
</style>