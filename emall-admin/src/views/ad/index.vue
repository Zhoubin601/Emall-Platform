<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { 
  Search, 
  Plus, 
  UploadFilled, 
  Picture, 
  VideoPlay, 
  VideoPause, 
  TrendCharts, 
  EditPen, 
  Delete,
  Link,
  Refresh
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Ad {
  id?: number
  title: string
  picUrl: string
  linkUrl: string
  sort: number
  status: number
}

const adList = ref<Ad[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const searchQuery = ref('')
const statusFilter = ref<'all' | 'active' | 'inactive'>('all')

const form = reactive<Ad>({
  title: '', 
  picUrl: '', 
  linkUrl: '', 
  sort: 1, 
  status: 1
})

const fetchAds = async () => {
  loading.value = true
  try { 
    const res: any = await request.get('/ad/list', { params: { title: searchQuery.value } })
    adList.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    ElMessage.error('获取广告轮播列表失败')
  } finally { 
    loading.value = false 
  }
}

// 4 Mini Bento 统计指标
const totalAdsCount = computed(() => adList.value.length)
const activeAdsCount = computed(() => adList.value.filter(a => a.status === 1).length)
const inactiveAdsCount = computed(() => adList.value.filter(a => a.status === 0).length)
const utilizationRate = computed(() => {
  if (totalAdsCount.value === 0) return '100%'
  return Math.round((activeAdsCount.value / totalAdsCount.value) * 100) + '%'
})

// 过滤列表
const filteredAdList = computed(() => {
  let list = adList.value
  if (statusFilter.value === 'active') {
    list = list.filter(a => a.status === 1)
  } else if (statusFilter.value === 'inactive') {
    list = list.filter(a => a.status === 0)
  }
  if (searchQuery.value.trim()) {
    const kw = searchQuery.value.trim().toLowerCase()
    list = list.filter(a => 
      (a.title && a.title.toLowerCase().includes(kw)) ||
      (a.linkUrl && a.linkUrl.toLowerCase().includes(kw))
    )
  }
  return list
})

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { 
    id: undefined, 
    title: '', 
    picUrl: '', 
    linkUrl: '', 
    sort: adList.value.length + 1, 
    status: 1 
  })
  dialogVisible.value = true
}

const handleEdit = (row: Ad) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要彻底删除该轮播图广告吗？删除后前台首页将立即停止轮播。', '删除确认', { 
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning' 
  }).then(async () => {
    await request.delete(`/ad/delete/${id}`)
    ElMessage.success('广告海报删除成功')
    fetchAds()
  }).catch(() => {})
}

const handleStatusChange = async (row: Ad) => {
  try {
    await request.put('/ad/update', { id: row.id, status: row.status })
    ElMessage.success(row.status === 1 ? '广告已上线投放' : '广告已暂停下线')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
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
    ElMessage.success('海报本地上传解析成功！')
  } catch (e) {
    ElMessage.error('上传失败，请检查后端网络')
  }
}

const submitForm = async () => {
  if (!form.title) return ElMessage.warning('广告标题不能为空')
  if (!form.picUrl) return ElMessage.warning('海报图片链接不能为空')
  try {
    if (isEdit.value) await request.put('/ad/update', form)
    else await request.post('/ad/add', form)
    ElMessage.success(isEdit.value ? '广告信息更新成功' : '新广告创建成功')
    dialogVisible.value = false
    fetchAds()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(fetchAds)
</script>

<template>
  <div class="ad-manage-wrapper">
    <!-- 4 Mini Bento 统计指标卡 -->
    <div class="bento-grid">
      <div class="bento-card bento-total">
        <div class="bento-icon-box"><el-icon><Picture /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ totalAdsCount }}</div>
          <div class="bento-label">轮播广告位总数</div>
        </div>
      </div>

      <div class="bento-card bento-active">
        <div class="bento-icon-box"><el-icon><VideoPlay /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ activeAdsCount }}</div>
          <div class="bento-label">线上生效投放中</div>
        </div>
      </div>

      <div class="bento-card bento-inactive">
        <div class="bento-icon-box"><el-icon><VideoPause /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ inactiveAdsCount }}</div>
          <div class="bento-label">已暂停 / 已下架</div>
        </div>
      </div>

      <div class="bento-card bento-rate">
        <div class="bento-icon-box"><el-icon><TrendCharts /></el-icon></div>
        <div class="bento-content">
          <div class="bento-num">{{ utilizationRate }}</div>
          <div class="bento-label">广告位投放率</div>
        </div>
      </div>
    </div>

    <!-- 主表格卡片 -->
    <el-card class="glass-card main-table-card" shadow="never">
      <!-- 过滤工具栏 -->
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-radio-group v-model="statusFilter" size="default">
            <el-radio-button label="all">全部 ({{ totalAdsCount }})</el-radio-button>
            <el-radio-button label="active">投放中 ({{ activeAdsCount }})</el-radio-button>
            <el-radio-button label="inactive">已下线 ({{ inactiveAdsCount }})</el-radio-button>
          </el-radio-group>

          <el-input 
            v-model="searchQuery" 
            placeholder="搜索广告标题或跳转链接..." 
            style="width: 280px" 
            clearable 
            :prefix-icon="Search"
          />
        </div>

        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="fetchAds" size="default" plain>刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd" size="default" class="btn-create-ad">
            新增轮播图
          </el-button>
        </div>
      </div>

      <!-- 轮播图列表表格 -->
      <el-table :data="filteredAdList" v-loading="loading" stripe class="custom-admin-table" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center">
          <template #default="{ row }">
            <span class="ad-id-badge">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column label="海报预览 (16:9)" width="220">
          <template #default="{ row }">
            <div class="ad-preview-wrapper">
              <el-image 
                :src="row.picUrl" 
                fit="cover" 
                class="ad-preview-img" 
                :preview-src-list="[row.picUrl]" 
                preview-teleported
              >
                <template #error>
                  <div class="image-fallback">
                    <el-icon><Picture /></el-icon>
                    <span>无海报</span>
                  </div>
                </template>
              </el-image>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="广告主标题与文案" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <strong class="ad-main-title">{{ row.title }}</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="linkUrl" label="点击跳转路径" min-width="180">
          <template #default="{ row }">
            <div class="link-cell">
              <el-icon color="#0284c7"><Link /></el-icon>
              <span class="link-text">{{ row.linkUrl || '未设置 (默认留在首页)' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="权重排序" width="110" align="center">
          <template #default="{ row }">
            <span class="sort-badge">第 {{ row.sort }} 帧</span>
          </template>
        </el-table-column>

        <el-table-column label="生效投放状态" width="130" align="center">
          <template #default="{ row }">
            <div class="status-toggle-cell">
              <el-switch 
                v-model="row.status" 
                :active-value="1" 
                :inactive-value="0" 
                @change="handleStatusChange(row)" 
              />
              <span class="status-state-text" :class="row.status === 1 ? 'is-active' : 'is-inactive'">
                {{ row.status === 1 ? '投放中' : '已暂停' }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="管理操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="EditPen" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 弹窗表单 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑广告海报配置' : '发布全新轮播图广告'" 
      width="580px"
      class="custom-ad-dialog"
      destroy-on-close
    >
      <el-form label-position="top">
        <el-form-item label="广告主标题 / 活动标语" required>
          <el-input v-model="form.title" placeholder="例如：新品首发·钛金属极简美学" />
        </el-form-item>
        
        <el-form-item label="海报大图链接 (推荐 1400x560 宽屏比例)" required>
          <div class="upload-wrapper-box">
            <el-input 
              v-model="form.picUrl" 
              placeholder="请输入网络图片 URL，或点击下方虚线框上传本地图片" 
              clearable 
            />
            <el-upload 
              action="#" 
              class="ad-uploader" 
              :show-file-list="false" 
              :http-request="uploadImage"
              accept="image/*"
            >
              <div class="uploader-content">
                <img v-if="form.picUrl" :src="form.picUrl" class="upload-img" />
                <div v-else class="uploader-placeholder">
                  <el-icon class="uploader-icon"><UploadFilled /></el-icon>
                  <span class="upload-tip">点击或拖拽本地图片上传</span>
                </div>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="14">
            <el-form-item label="点击跳转路径 (商品详情/分类页)">
              <el-input v-model="form.linkUrl" placeholder="例如: /product/101 或 /" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="展示排序序号">
              <el-input-number v-model="form.sort" :min="1" :max="99" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="初始启用状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">立即上线投放</el-radio>
            <el-radio :label="0">仅存为草稿 (暂不上线)</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false" round>取消</el-button>
          <el-button type="primary" @click="submitForm" round>确定保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ad-manage-wrapper {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
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
.bento-inactive .bento-icon-box { background: #f1f5f9; color: #64748b; }
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

.btn-create-ad {
  font-weight: 600;
  border-radius: 8px;
}

.ad-id-badge {
  font-family: monospace;
  font-weight: 700;
  color: #64748b;
}

.ad-preview-wrapper {
  width: 180px;
  height: 75px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.ad-preview-img {
  width: 100%;
  height: 100%;
  display: block;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #94a3b8;
  gap: 4px;
}

.ad-main-title {
  font-size: 14px;
  color: #0f172a;
}

.link-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.link-text {
  font-size: 13px;
  color: #0284c7;
  font-family: monospace;
  background: #f0f9ff;
  padding: 2px 8px;
  border-radius: 6px;
}

.sort-badge {
  font-size: 12px;
  font-weight: 700;
  color: #3b82f6;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  padding: 3px 8px;
  border-radius: 8px;
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

/* 弹窗上传框 */
.upload-wrapper-box {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

:deep(.ad-uploader .el-upload) { 
  width: 100%; 
  display: block; 
}

.uploader-content { 
  border: 2px dashed #cbd5e1; 
  border-radius: 12px; 
  cursor: pointer; 
  position: relative; 
  overflow: hidden; 
  width: 100%; 
  height: 160px; 
  display: flex; 
  align-items: center; 
  justify-content: center; 
  background: #f8fafc; 
  transition: all 0.2s; 
}

.uploader-content:hover { 
  border-color: #0284c7; 
  background: #f0f9ff;
}

.upload-img { 
  width: 100%; 
  height: 100%; 
  object-fit: cover; 
}

.uploader-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.uploader-icon { 
  font-size: 32px; 
  color: #64748b; 
}

.upload-tip {
  font-size: 13px;
  color: #64748b;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>