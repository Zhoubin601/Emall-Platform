<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type UploadUserFile } from 'element-plus'
import { Plus, Back, Goods, Ticket } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const productName = ref('加载中...')
const orderSn = ref('加载中...')
const isEdit = ref(false)
const commentId = route.query.commentId 

const form = reactive({
  id: null as number | null,
  productId: route.query.productId as any,
  orderId: route.query.orderId as any,
  star: 5,
  content: ''
})

// ✨ 绝杀方案核心 1：用 Element Plus 的双向绑定直接接管文件列表
const fileList = ref<UploadUserFile[]>([])

const uploadData = computed(() => {
  return {
    nickname: userStore.userInfo?.username || '未知账号', 
    productName: productName.value === '加载中...' ? '评价商品' : productName.value
  }
})

// 安全回显函数：确保中文和空格能正确显示
const getSafeUrl = (rawUrl: string) => {
  if (!rawUrl) return ''
  const fileName = rawUrl.substring(rawUrl.lastIndexOf('/') + 1)
  return `/uploads/${encodeURIComponent(fileName)}`
}

onMounted(async () => {
  if (commentId) {
    isEdit.value = true
    try {
      const oldData = await request.get<any, any>(`/comment/detail/${commentId}`)
      form.id = oldData.id
      form.star = oldData.star
      form.content = oldData.content
      form.productId = oldData.productId
      form.orderId = oldData.orderId
      
      // ✨ 绝杀方案核心 2：如果是修改评价，将数据库的图片装载进双向绑定列表
      if (oldData.pics) {
        fileList.value = oldData.pics.split(',').map((url: string) => ({
          name: '已上传图片',
          url: getSafeUrl(url), 
          rawUrl: url // 悄悄把真实的数据库原始路径存起来
        }))
      }
    } catch (e) { ElMessage.error('原评价加载失败') }
  }

  if (form.productId) {
    request.get<any, any>(`/product/detail/${form.productId}`).then(res => {
      productName.value = res.name
    }).catch(() => productName.value = '未知商品')
  }

  if (form.orderId) {
    request.get<any, any>(`/order/detail/${form.orderId}`).then(res => {
      orderSn.value = res.orderSn
    }).catch(() => orderSn.value = '未知单号')
  }
})

// ✨ 绝杀方案核心 3：提交时现场榨取 URL
const submitComment = async () => {
  if (!form.content.trim()) return ElMessage.warning('请填写心得')
  
  // 遍历当前的真实图片列表
  const finalPics = fileList.value.map(file => {
    // A. 如果是刚刚新上传的图片，底层对象里会有 response
    if (file.response) {
      const res = file.response as any
      // 暴力提取：无论后端包了多少层，都能拿出来
      let extractedUrl = res.url || res.data?.url || res.data || res
      if (typeof extractedUrl === 'string') extractedUrl = extractedUrl.replace(/"/g, '') // 防御性清理多余引号
      return extractedUrl
    }
    // B. 如果是之前已经上传过的图片（修改评价模式），直接交出刚才存好的原始路径
    return (file as any).rawUrl || file.url
  }).filter(url => url && typeof url === 'string') // 过滤掉一切异常空值

  const submitData = {
    ...form,
    userId: userStore.userInfo.id,
    nickname: userStore.userInfo.username,
    avatar: userStore.userInfo.avatar,
    // 只要有链接，一定能转成字符串传给后端！
    pics: finalPics.length > 0 ? finalPics.join(',') : null
  }
  
  console.log("【终极探针】即将提交入库的评价数据：", submitData)

  try {
    const apiUrl = isEdit.value ? '/comment/update' : '/comment/add'
    await request.post(apiUrl, submitData)
    
    ElMessage.success(isEdit.value ? '修改成功！' : '发布成功！')
    router.push(isEdit.value ? '/comments' : '/orders')
  } catch (error) {
    ElMessage.error('提交失败，请检查网络')
  }
}
</script>

<template>
  <div class="comment-post-page">
    <header class="glass-header">
      <div class="header-content">
        <el-button link :icon="Back" @click="router.back()">返回</el-button>
        <span class="title">{{ isEdit ? '修改评价' : '发表评价' }}</span>
      </div>
    </header>

    <main class="container">
      <el-card class="comment-card glass-card">
        <div class="info-banner">
          <div class="info-row">
            <el-icon><Goods /></el-icon>
            <span class="label">评价商品：</span>
            <span class="value highlight">{{ productName }}</span>
          </div>
          <div class="info-row">
            <el-icon><Ticket /></el-icon>
            <span class="label">订单编号：</span>
            <span class="value">{{ orderSn }}</span>
          </div>
        </div>

        <div class="section">
          <p class="section-label">满意度评分</p>
          <el-rate v-model="form.star" size="large" show-text />
        </div>

        <div class="section">
          <p class="section-label">分享心得</p>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="宝贝好用吗？快来写下你的真实感受吧..."
            maxlength="200"
            show-word-limit
            class="cute-textarea"
          />
        </div>

        <div class="section">
          <p class="section-label">晒图分享</p>
          <el-upload
            v-model:file-list="fileList"
            action="/api/file/upload" 
            list-type="picture-card"
            :data="uploadData"
            :limit="3"
            class="cute-upload"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </div>

        <div class="footer">
          <el-button type="primary" class="submit-btn" round @click="submitComment">
            {{ isEdit ? '确认修改' : '发布评价' }}
          </el-button>
        </div>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.comment-post-page { min-height: 100vh; background: #f0f9ff; padding-top: 80px; }
.glass-header { position: fixed; top: 0; width: 100%; height: 60px; background: rgba(255,255,255,0.7); backdrop-filter: blur(10px); z-index: 100; display: flex; justify-content: center; border-bottom: 1px solid #e0f2fe; }
.header-content { width: 800px; display: flex; align-items: center; padding: 0 20px; }
.title { margin-left: 20px; font-weight: bold; color: #0369a1; }
.container { width: 800px; max-width: 95%; margin: 0 auto; }
.glass-card { border-radius: 24px; border: none; background: rgba(255, 255, 255, 0.85); box-shadow: 0 12px 40px rgba(186, 230, 253, 0.4); padding: 20px; }
.info-banner { background: rgba(224, 242, 254, 0.5); padding: 20px; border-radius: 18px; margin-bottom: 30px; border: 1px solid rgba(186, 230, 253, 0.5); }
.info-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; color: #475569; font-size: 14px; }
.info-row:last-child { margin-bottom: 0; }
.label { font-weight: bold; color: #64748b; }
.value { color: #1e293b; }
.value.highlight { color: #0369a1; font-weight: 800; }
.section { margin-bottom: 25px; }
.section-label { font-size: 15px; font-weight: bold; color: #334155; margin-bottom: 12px; }
:deep(.cute-textarea .el-textarea__inner) { border-radius: 16px; background: #f8fafc; border: 1px solid #e2e8f0; padding: 15px; }
:deep(.cute-upload .el-upload--picture-card) { background: #f1f5f9; border-radius: 16px; border: 2px dashed #cbd5e1; }
.footer { text-align: center; margin-top: 30px; }
.submit-btn { width: 260px; height: 48px; font-weight: bold; background: linear-gradient(135deg, #0ea5e9, #0284c7); border: none; box-shadow: 0 8px 20px rgba(14, 165, 233, 0.3); }
</style>