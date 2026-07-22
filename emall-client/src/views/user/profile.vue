<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Back, EditPen, Message, Phone, Lock, Check, Plus, 
  Notification, ChatLineRound, Promotion, Loading
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  id: userStore.userInfo?.id,
  username: userStore.userInfo?.username,
  nickname: userStore.userInfo?.nickname || '',
  avatar: userStore.userInfo?.avatar || '',
  email: userStore.userInfo?.email || '',
  phone: userStore.userInfo?.phone || '',
  password: '' 
})

const getSafeAvatar = (avatarPath?: string) => {
  if (!avatarPath || avatarPath.includes('default-avatar.png')) return '' 
  if (avatarPath.startsWith('http')) return avatarPath
  return avatarPath
}

const uploadAvatar = async (options: any) => {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('username', form.username) 
  
  try {
    const res: any = await request.post('/file/uploadAvatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.avatar = res.url 
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}

const handleSave = async () => {
  if (!form.nickname) return ElMessage.warning('昵称不能为空')
  
  loading.value = true
  try {
    const updateData: Record<string, unknown> = { ...form }
    if (!updateData.password) {
      delete updateData.password
    }

    await request.put('/user/update', updateData) 
    userStore.setUser({ ...userStore.userInfo, ...updateData })
    
    ElMessage.success('个人资料已更新！')
    form.password = '' 
  } catch (e) {
    ElMessage.error('保存失败，请检查网络')
  } finally {
    loading.value = false
  }
}

const notices = ref<any[]>([])
const myFeedbacks = ref<any[]>([])
const isFbSubmitting = ref(false)

const fbForm = reactive({
  type: '功能建议',
  content: ''
})

const fetchInteractions = async () => {
  try {
    notices.value = await request.get('/interaction/notice/active')
    if (userStore.userInfo) {
      const res: any[] = await request.get('/interaction/feedback/my', { 
        params: { userId: userStore.userInfo.id } 
      })
      // ✨ 核心修复 1：过滤掉实时聊天的消息流，只保留正式的表单反馈！
      myFeedbacks.value = res.filter(item => item.type !== '在线沟通')
    }
  } catch (error) {
    console.error('获取互动数据失败')
  }
}

const handleFeedbackSubmit = async () => {
  if (!fbForm.content.trim()) return ElMessage.warning('建议内容不能为空哦')
  
  isFbSubmitting.value = true
  try {
    await request.post('/interaction/feedback/submit', {
      userId: userStore.userInfo?.id,
      type: fbForm.type,
      content: fbForm.content
    })
    ElMessage.success('感谢您的宝贵建议！我们将持续优化。')
    fbForm.content = ''
    fetchInteractions() 
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    isFbSubmitting.value = false
  }
}

onMounted(() => {
  fetchInteractions()
})
</script>

<template>
  <div class="profile-page">
    <div class="bg-decoration"></div>
    
    <header class="glass-header">
      <div class="header-content">
        <el-button link :icon="Back" @click="router.push('/')" class="back-btn">返回首页</el-button>
        <span class="page-title">账号设置与服务</span>
      </div>
    </header>

    <div class="profile-layout-wrapper">
      
      <el-card class="profile-card" shadow="never">
        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :http-request="uploadAvatar"
            accept="image/*"
          >
            <div class="avatar-container">
              <el-avatar v-if="form.avatar && !form.avatar.includes('default-avatar')" :size="110" :src="getSafeAvatar(form.avatar)" class="user-avatar" />
              <el-avatar v-else :size="110" class="user-avatar default-avatar-bg">
                {{ form.username ? form.username.charAt(0).toUpperCase() : 'U' }}
              </el-avatar>
              <div class="upload-mask">
                <el-icon><Plus /></el-icon>
                <span>修改头像</span>
              </div>
            </div>
          </el-upload>
          <div class="user-tag">尊敬的商城会员</div>
        </div>

        <el-form label-position="top" class="info-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="登录账号 (不可修改)">
                <el-input v-model="form.username" disabled class="custom-input" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="我的昵称">
                <el-input v-model="form.nickname" placeholder="起个好听的名字" :prefix-icon="EditPen" class="custom-input" />
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="安全邮箱">
                <el-input v-model="form.email" placeholder="用于找回密码" :prefix-icon="Message" class="custom-input" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号码">
                <el-input v-model="form.phone" placeholder="联系电话" :prefix-icon="Phone" class="custom-input" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="重置密码 (如无需修改请留空)">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入新密码" :prefix-icon="Lock" class="custom-input" />
          </el-form-item>

          <el-button type="primary" class="save-btn" round :icon="Check" :loading="loading" @click="handleSave">
            确认并保存资料
          </el-button>
        </el-form>
      </el-card>

      <div class="interaction-wrapper">
        <el-card class="interaction-card notice-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon color="#f43f5e" size="18"><Notification /></el-icon> 系统公告与活动
            </div>
          </template>
          <div v-if="notices.length > 0" class="notice-list">
            <div class="notice-item" v-for="n in notices" :key="n.id">
              <h4 class="n-title">{{ n.title }}</h4>
              <p class="n-content">{{ n.content }}</p>
              <span class="n-time">{{ n.createTime?.substring(0, 10) }}发布</span>
            </div>
          </div>
          <el-empty v-else description="暂无最新公告" :image-size="60" />
        </el-card>

        <el-card class="interaction-card fb-card" shadow="never">
          <template #header>
            <div class="card-title">
              <el-icon color="#0ea5e9" size="18"><ChatLineRound /></el-icon> 商城意见箱
            </div>
          </template>
          
          <div class="fb-submit-box">
            <el-radio-group v-model="fbForm.type" size="small" class="fb-radio">
              <el-radio-button value="功能建议">功能建议</el-radio-button>
              <el-radio-button value="购物体验">购物体验</el-radio-button>
              <el-radio-button value="报错反馈">报错反馈</el-radio-button>
            </el-radio-group>
            <el-input 
              v-model="fbForm.content" 
              type="textarea" 
              :rows="3" 
              placeholder="您对 E-MALL 有什么期许或发现了什么 Bug？欢迎告诉我们~" 
              class="cute-textarea mt-10"
            />
            <div class="fb-action">
              <el-button type="primary" size="small" round :icon="Promotion" :loading="isFbSubmitting" @click="handleFeedbackSubmit">
                提交建议
              </el-button>
            </div>
          </div>

          <div class="history-divider"><span>我的历史反馈</span></div>
          <div v-if="myFeedbacks.length > 0" class="fb-list">
            <div class="fb-item" v-for="fb in myFeedbacks" :key="fb.id">
              <div class="fb-header">
                <el-tag size="small" effect="plain" type="info">{{ fb.type }}</el-tag>
                <span class="fb-time">{{ fb.createTime?.substring(5, 16) }}</span>
              </div>
              <div class="fb-question">{{ fb.content }}</div>
              
              <div class="fb-answer" v-if="fb.status === 1 && fb.reply">
                <div class="admin-label">平台回复：</div>
                {{ fb.reply }}
              </div>
              <div class="fb-waiting" v-else>
                <el-icon class="is-loading"><Loading /></el-icon> 感谢提交，我们正在认真阅读中...
              </div>
            </div>
          </div>
          <div v-else class="empty-fb">您还没有提交过建议哦</div>
        </el-card>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* 所有样式原封不动保留 */
.profile-page { min-height: 100vh; background-color: #f0f9ff; padding-top: 100px; padding-bottom: 50px; position: relative; overflow: hidden; }
.bg-decoration { position: absolute; top: -10%; right: -10%; width: 500px; height: 500px; background: radial-gradient(circle, #bae6fd 0%, transparent 70%); filter: blur(60px); z-index: 0; }
.glass-header { position: fixed; top: 0; width: 100%; height: 70px; background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(20px); display: flex; align-items: center; z-index: 100; border-bottom: 1px solid rgba(255, 255, 255, 0.5); }
.header-content { width: 1200px; margin: 0 auto; display: flex; align-items: center; gap: 20px; padding: 0 20px; }
.back-btn { font-size: 15px; color: #64748b; font-weight: bold; }
.page-title { font-weight: bold; color: #0f172a; font-size: 18px; }

.profile-layout-wrapper { display: flex; gap: 25px; width: 1100px; max-width: 95%; margin: 0 auto; position: relative; z-index: 1; align-items: flex-start; }
.profile-card { flex: 1; border-radius: 30px; border: 1px solid rgba(255, 255, 255, 0.6); background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(15px); padding: 30px 20px; box-shadow: 0 20px 60px rgba(2, 132, 199, 0.08); }

.avatar-section { text-align: center; margin-bottom: 40px; }
.avatar-container { position: relative; width: 110px; height: 110px; margin: 0 auto; border-radius: 50%; overflow: hidden; cursor: pointer; border: 4px solid #fff; box-shadow: 0 10px 20px rgba(2, 132, 199, 0.15); transition: transform 0.3s; }
.avatar-container:hover { transform: scale(1.05); }
.upload-mask { position: absolute; inset: 0; background: rgba(0, 0, 0, 0.4); color: white; display: flex; flex-direction: column; justify-content: center; align-items: center; opacity: 0; transition: opacity 0.3s; font-size: 12px; }
.avatar-container:hover .upload-mask { opacity: 1; }
.default-avatar-bg { background: #bae6fd; color: #0284c7; font-weight: bold; font-size: 36px; }
.user-tag { margin-top: 15px; color: #0284c7; font-size: 13px; font-weight: bold; background: #e0f2fe; padding: 4px 12px; border-radius: 20px; display: inline-block; }

.info-form { padding: 0 20px; }
.custom-input :deep(.el-input__wrapper) { border-radius: 12px; background: #f8fafc; box-shadow: none; border: 1px solid #e2e8f0; }
.custom-input :deep(.el-input__wrapper.is-focus) { border-color: #0ea5e9; background: #fff; }
.save-btn { width: 100%; height: 45px; margin-top: 20px; font-weight: bold; font-size: 16px; background: linear-gradient(135deg, #0ea5e9, #0284c7); border: none; box-shadow: 0 10px 20px rgba(14, 165, 233, 0.2); }
.save-btn:hover { transform: translateY(-2px); box-shadow: 0 15px 25px rgba(14, 165, 233, 0.3); }

.interaction-wrapper { width: 420px; display: flex; flex-direction: column; gap: 25px; }
.interaction-card { border-radius: 24px; border: 1px solid rgba(255, 255, 255, 0.6); background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(10px); box-shadow: 0 10px 30px rgba(2, 132, 199, 0.05); }
:deep(.interaction-card .el-card__header) { border-bottom: 1px solid rgba(226, 232, 240, 0.6); padding: 15px 20px; }
.card-title { font-weight: bold; color: #0f172a; font-size: 15px; display: flex; align-items: center; gap: 8px; }

.notice-list { max-height: 200px; overflow-y: auto; padding-right: 5px; }
.notice-list::-webkit-scrollbar { width: 4px; }
.notice-list::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
.notice-item { background: #fff1f2; border: 1px solid #ffe4e6; padding: 12px 15px; border-radius: 12px; margin-bottom: 12px; transition: transform 0.2s; }
.notice-item:hover { transform: translateX(5px); }
.n-title { margin: 0 0 6px 0; color: #e11d48; font-size: 14px; }
.n-content { margin: 0 0 8px 0; font-size: 13px; color: #475569; line-height: 1.5; }
.n-time { font-size: 12px; color: #fb7185; }

.fb-submit-box { margin-bottom: 20px; }
.fb-radio { margin-bottom: 10px; }
.mt-10 { margin-top: 10px; }
.cute-textarea :deep(.el-textarea__inner) { border-radius: 12px; background: #f8fafc; box-shadow: none; border: 1px solid #e2e8f0; padding: 10px; font-size: 13px; }
.cute-textarea :deep(.el-textarea__inner:focus) { border-color: #0ea5e9; background: #fff; }
.fb-action { display: flex; justify-content: flex-end; margin-top: 10px; }

.history-divider { display: flex; align-items: center; text-align: center; color: #94a3b8; font-size: 12px; margin: 15px 0; }
.history-divider::before, .history-divider::after { content: ''; flex: 1; border-bottom: 1px dashed #e2e8f0; }
.history-divider span { padding: 0 10px; }

.fb-list { max-height: 250px; overflow-y: auto; padding-right: 5px; }
.fb-list::-webkit-scrollbar { width: 4px; }
.fb-list::-webkit-scrollbar-thumb { background: #bae6fd; border-radius: 4px; }
.fb-item { padding: 15px 0; border-bottom: 1px dashed #e2e8f0; }
.fb-item:last-child { border-bottom: none; padding-bottom: 0; }
.fb-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.fb-time { font-size: 12px; color: #94a3b8; }
.fb-question { font-size: 13px; color: #1e293b; font-weight: 500; margin-bottom: 10px; line-height: 1.4; background: #f1f5f9; padding: 10px; border-radius: 8px 8px 8px 0; }
.fb-answer { background: #f0fdf4; border: 1px solid #bbf7d0; color: #047857; padding: 10px; border-radius: 8px 8px 0 8px; font-size: 13px; line-height: 1.5; margin-left: 20px; position: relative; }
.admin-label { font-weight: bold; margin-bottom: 4px; font-size: 12px; color: #059669; }
.fb-waiting { color: #d97706; font-size: 12px; display: flex; align-items: center; gap: 5px; margin-left: 20px; }
.empty-fb { text-align: center; color: #94a3b8; font-size: 13px; padding: 20px 0; }
</style>
