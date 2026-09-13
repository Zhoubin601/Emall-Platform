<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type UploadUserFile } from 'element-plus'
import { Plus, Picture, Goods, Medal } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import ClientHeader from '../../components/ClientHeader.vue'
import ClientFooter from '../../components/ClientFooter.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const productName = ref('加载中...')
const productPic = ref('')
const orderSn = ref('加载中...')
const isEdit = ref(false)
const commentId = route.query.commentId 

const isAnonymous = ref(false)

const form = reactive({
  id: null as number | null,
  productId: route.query.productId as any,
  orderId: route.query.orderId as any,
  star: 5,
  content: ''
})

const quickTags = [
  '顺丰神速', '包装精美', '做工精良', '性价比高', '自营正品', '颜值极高'
]

const addQuickTag = (tag: string) => {
  if (form.content.includes(tag)) return
  form.content = form.content ? `${form.content}，${tag}` : tag
}

const ratingMoodText = computed(() => {
  const map: Record<number, string> = {
    1: '非常不满意，有待改进',
    2: '不满意，体验一般',
    3: '基本满意，符合预期',
    4: '满意，品质很不错',
    5: '非常满意，物超所值，强烈推荐！'
  }
  return map[form.star] || '请进行满意度打分'
})

// Element Plus 文件上传绑定
const fileList = ref<UploadUserFile[]>([])

const uploadData = computed(() => {
  return {
    nickname: userStore.userInfo?.username || '买家', 
    productName: productName.value === '加载中...' ? '商品' : productName.value
  }
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('mall-token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const getSafeUrl = (rawUrl: string) => {
  return rawUrl || ''
}

onMounted(async () => {
  if (commentId) {
    isEdit.value = true
    try {
      const oldData: any = await request.get(`/comment/detail/${commentId}`)
      form.id = oldData.id
      form.star = oldData.star
      form.content = oldData.content
      form.productId = oldData.productId
      form.orderId = oldData.orderId
      
      if (oldData.pics) {
        fileList.value = oldData.pics.split(',').filter(Boolean).map((url: string) => ({
          name: '已上传买家秀',
          url: getSafeUrl(url), 
          rawUrl: url
        }))
      }
    } catch (e) { ElMessage.error('原评价信息加载失败') }
  }

  if (form.productId) {
    request.get<any, any>(`/product/detail/${form.productId}`).then(res => {
      productName.value = res.name
      productPic.value = res.picUrl
    }).catch(() => productName.value = '自营严选商品')
  }

  if (form.orderId) {
    request.get<any, any>(`/order/detail/${form.orderId}`).then(res => {
      orderSn.value = res.orderSn
    }).catch(() => orderSn.value = 'EMALL订单')
  }
})

const isSubmitting = ref(false)

const submitComment = async () => {
  if (!form.content.trim()) return ElMessage.warning('请填写至少一句您的真实使用心得哦')
  
  isSubmitting.value = true

  const finalPics = fileList.value.map(file => {
    if (file.response) {
      const res = file.response as any
      let extractedUrl = res.url || res.data?.url || res.data || res
      if (typeof extractedUrl === 'string') extractedUrl = extractedUrl.replace(/"/g, '')
      return extractedUrl
    }
    return (file as any).rawUrl || file.url
  }).filter(url => url && typeof url === 'string')

  const submitData = {
    id: form.id,
    userId: userStore.userInfo?.id,
    productId: Number(form.productId),
    orderId: Number(form.orderId),
    star: form.star,
    content: form.content,
    pics: finalPics.join(',')
  }

  try {
    if (isEdit.value) {
      await request.put('/comment/update', submitData)
      ElMessage.success('🎉 评价已成功修改！')
    } else {
      await request.post('/comment/add', submitData)
      ElMessage.success('🎉 评价发表成功！感谢您对 E-MALL 的支持')
    }
    router.push('/orders')
  } catch (e) {
    ElMessage.error('提交失败，请检查网络')
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="comment-page-layout">
    <ClientHeader />

    <main class="comment-container">
      <!-- 面包屑导航 -->
      <nav class="breadcrumb-bar">
        <span class="crumb-link" @click="router.push('/')">首页</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-link" @click="router.push('/orders')">我的订单</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">{{ isEdit ? '修改评价' : '发表评价晒单' }}</span>
      </nav>

      <div class="comment-main-card">
        <!-- 左栏：商品信息摘要 -->
        <div class="product-summary-col">
          <div class="product-cover-box">
            <img v-if="productPic" :src="productPic" class="prod-img" />
            <div v-else class="no-img"><el-icon :size="48"><Goods /></el-icon></div>
          </div>
          <h3 class="prod-title">{{ productName }}</h3>
          <div class="order-info-tag">
            <span>关联单号：{{ orderSn }}</span>
          </div>
          <div class="trust-badge-line">
            <el-icon color="#0ea5e9"><Medal /></el-icon>
            <span>真实买家订单 认证评价</span>
          </div>
        </div>

        <!-- 右栏：评价主表单 -->
        <div class="comment-form-col">
          <div class="form-section-title">
            <span>商品综合评分</span>
          </div>

          <!-- 星级打分与情绪反馈 -->
          <div class="rating-bar">
            <el-rate v-model="form.star" size="large" allow-half={false} />
            <span class="mood-text">{{ ratingMoodText }}</span>
          </div>

          <!-- 快速标签 -->
          <div class="quick-tags-box">
            <span class="tags-lead">快速评价：</span>
            <span 
              v-for="tag in quickTags" 
              :key="tag" 
              class="quick-tag-chip"
              @click="addQuickTag(tag)"
            >
              + {{ tag }}
            </span>
          </div>

          <!-- 心得文本输入 -->
          <div class="textarea-box">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="宝贝满足您的期待吗？说说它的做工、材质、使用感受等，分享给其他挑剔的小伙伴吧~"
              class="custom-textarea"
            />
          </div>

          <!-- 买家秀图片上传 -->
          <div class="upload-section">
            <div class="upload-title">
              <el-icon><Picture /></el-icon>
              <span>买家秀晒图 (最多上传 5 张实拍图)</span>
            </div>

            <el-upload
              v-model:file-list="fileList"
              action="/api/file/upload"
              list-type="picture-card"
              :headers="uploadHeaders"
              :data="uploadData"
              :limit="5"
              accept="image/*"
              class="cute-uploader"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </div>

          <!-- 底部提交栏 -->
          <div class="submit-action-bar">
            <el-checkbox v-model="isAnonymous" label="匿名评价 (隐藏您的真实用户名)" />
            <el-button 
              type="primary" 
              size="large" 
              class="btn-submit"
              :loading="isSubmitting"
              @click="submitComment"
            >
              {{ isEdit ? '确认修改评价' : '立即发布评价晒单' }}
            </el-button>
          </div>
        </div>
      </div>
    </main>

    <ClientFooter />
  </div>
</template>

<style scoped>
.comment-page-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.comment-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  padding: 24px 0 60px;
  flex: 1;
}

/* 面包屑 */
.breadcrumb-bar {
  padding: 0 0 16px;
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.crumb-link {
  cursor: pointer;
  transition: color 0.2s;
}

.crumb-link:hover {
  color: #0284c7;
}

.crumb-sep {
  color: #cbd5e1;
}

.crumb-current {
  color: #0f172a;
  font-weight: bold;
}

/* 主卡片 */
.comment-main-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  display: grid;
  grid-template-columns: 320px 1fr;
  overflow: hidden;
}

/* 左侧商品卡 */
.product-summary-col {
  background-color: #fcfdfe;
  border-right: 1px solid #f1f5f9;
  padding: 36px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.product-cover-box {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  margin-bottom: 20px;
}

.prod-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-img {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #cbd5e1;
}

.prod-title {
  margin: 0 0 12px;
  font-size: 16px;
  color: #0f172a;
  line-height: 1.5;
}

.order-info-tag {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 12px;
  border-radius: 6px;
  margin-bottom: 16px;
}

.trust-badge-line {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #0ea5e9;
}

/* 右侧表单 */
.comment-form-col {
  padding: 36px 40px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-section-title {
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
}

.rating-bar {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mood-text {
  font-size: 14px;
  font-weight: bold;
  color: #f43f5e;
}

/* 快速标签 */
.quick-tags-box {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.tags-lead {
  font-size: 13px;
  color: #64748b;
}

.quick-tag-chip {
  font-size: 12px;
  color: #475569;
  background-color: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 4px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-tag-chip:hover {
  background-color: #ffe4e6;
  border-color: #f43f5e;
  color: #e11d48;
}

.custom-textarea :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 14px;
  font-size: 14px;
  border-color: #cbd5e1;
}

.custom-textarea :deep(.el-textarea__inner:focus) {
  border-color: #0284c7;
}

/* 上传 */
.upload-section {
  margin-top: 6px;
}

.upload-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: bold;
  color: #334155;
  margin-bottom: 12px;
}

.submit-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

.btn-submit {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-weight: bold;
  padding: 0 36px;
  border-radius: 20px;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.3);
  transition: all 0.2s;
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(2, 132, 199, 0.4);
}
</style>
