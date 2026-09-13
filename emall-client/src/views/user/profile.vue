<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { 
  EditPen, Message, Phone, Lock, Check, Plus, 
  Promotion, User
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'
import request from '../../utils/request'
import UserCenterLayout from '../../components/UserCenterLayout.vue'

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

const safeAvatar = computed(() => {
  if (!form.avatar || form.avatar.includes('default-avatar.png')) return '' 
  return form.avatar
})

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
    
    ElMessage.success('🎉 个人资料更新成功！')
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
    notices.value = await request.get('/interaction/notice/active') || []
    if (userStore.userInfo) {
      const res: any[] = await request.get('/interaction/feedback/my', { 
        params: { userId: userStore.userInfo.id } 
      }) || []
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
    ElMessage.success('感谢您的宝贵建议！小E将认真研读并回复您')
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
  <UserCenterLayout activeMenu="profile" pageTitle="个人资料与安全">
    <div class="profile-view-wrap">
      <!-- 页面标题 -->
      <div class="section-title-bar">
        <h2>个人资料与账号安全</h2>
        <span class="sub-hint">管理您的身份信息、联系方式与专属安全设置</span>
      </div>

      <el-row :gutter="30">
        <!-- 左栏：基本资料 -->
        <el-col :span="14">
          <div class="form-card-inner">
            <h3 class="card-inner-title">基本信息设置</h3>

            <div class="avatar-upload-row">
              <div class="avatar-container">
                <el-avatar v-if="safeAvatar" :size="76" :src="safeAvatar" class="edit-avatar" />
                <el-avatar v-else :size="76" class="edit-avatar default-av">
                  {{ form.nickname?.substring(0, 1) || form.username?.substring(0, 1) || 'U' }}
                </el-avatar>
              </div>

              <div class="avatar-action">
                <el-upload
                  action=""
                  :http-request="uploadAvatar"
                  :show-file-list="false"
                  accept="image/*"
                >
                  <el-button size="small" type="primary" plain round :icon="Plus">更换自定义头像</el-button>
                </el-upload>
                <div class="avatar-tips">支持 JPG、PNG 格式，建议尺寸 200x200 像素</div>
              </div>
            </div>

            <el-form label-position="top" class="custom-el-form">
              <el-form-item label="登录账号 (系统唯一标识)">
                <el-input v-model="form.username" disabled class="disabled-input">
                  <template #prefix><el-icon><User /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item label="用户昵称 (个性化显示名称)" required>
                <el-input v-model="form.nickname" placeholder="请输入您的昵称">
                  <template #prefix><el-icon><EditPen /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item label="绑定邮箱 (用于找回密码及验证码通知)">
                <el-input v-model="form.email" placeholder="example@emall.com">
                  <template #prefix><el-icon><Message /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item label="联系电话">
                <el-input v-model="form.phone" placeholder="请输入手机号码">
                  <template #prefix><el-icon><Phone /></el-icon></template>
                </el-input>
              </el-form-item>

              <el-form-item label="重置登录密码 (若无需修改请留空)">
                <el-input v-model="form.password" type="password" show-password placeholder="输入 8 位以上新密码">
                  <template #prefix><el-icon><Lock /></el-icon></template>
                </el-input>
              </el-form-item>

              <div class="form-btn-row">
                <el-button 
                  type="primary" 
                  size="large" 
                  class="save-btn" 
                  :loading="loading" 
                  :icon="Check" 
                  @click="handleSave"
                >
                  保存修改资料
                </el-button>
              </div>
            </el-form>
          </div>
        </el-col>

        <!-- 右栏：服务建议与平台公告 -->
        <el-col :span="10">
          <div class="feedback-card-inner">
            <h3 class="card-inner-title">
              <el-icon color="#0ea5e9"><Promotion /></el-icon>
              <span>意见与功能反馈</span>
            </h3>
            <p class="fb-desc">我们非常重视您的每条声音，采纳后可获赠 VIP 尊享优惠券！</p>

            <el-radio-group v-model="fbForm.type" size="small" class="fb-type-radio">
              <el-radio-button label="功能建议" />
              <el-radio-button label="体验优化" />
              <el-radio-button label="商品需求" />
            </el-radio-group>

            <el-input
              v-model="fbForm.content"
              type="textarea"
              :rows="4"
              placeholder="请描述您的建议或体验中遇到的问题..."
              maxlength="200"
              show-word-limit
              class="fb-textarea"
            />

            <el-button 
              type="primary" 
              class="fb-submit-btn" 
              :loading="isFbSubmitting" 
              @click="handleFeedbackSubmit"
            >
              提交反馈建议
            </el-button>

            <!-- 历史反馈回复列表 -->
            <div class="my-feedbacks-history" v-if="myFeedbacks.length > 0">
              <h4 class="history-title">历史建议与回复</h4>
              <div v-for="item in myFeedbacks" :key="item.id" class="fb-history-item">
                <div class="fb-h-top">
                  <el-tag size="small" type="info">{{ item.type }}</el-tag>
                  <span class="fb-h-time">{{ item.createTime?.substring(0, 10) }}</span>
                </div>
                <div class="fb-h-content">{{ item.content }}</div>
                <div class="fb-h-reply" v-if="item.reply">
                  <span class="reply-label">官方管家回复：</span>{{ item.reply }}
                </div>
                <div class="fb-h-reply wait" v-else>
                  <span class="reply-label">状态：</span>运营管家正在研读中...
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </UserCenterLayout>
</template>

<style scoped>
.profile-view-wrap {
  width: 100%;
}

.section-title-bar {
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

.card-inner-title {
  margin: 0 0 20px;
  font-size: 16px;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 头像上传行 */
.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.edit-avatar {
  border: 2px solid #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.default-av {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #ffffff;
  font-size: 28px;
  font-weight: bold;
}

.avatar-action {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.avatar-tips {
  font-size: 11px;
  color: #94a3b8;
}

.custom-el-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: bold;
  color: #475569;
  padding-bottom: 4px;
}

.custom-el-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  padding: 4px 12px;
}

.disabled-input :deep(.el-input__wrapper) {
  background-color: #f8fafc;
}

.form-btn-row {
  margin-top: 24px;
}

.save-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-weight: bold;
  border-radius: 20px;
  padding: 0 36px;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.3);
}

/* 右栏反馈 */
.feedback-card-inner {
  background: #f8fafc;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 20px;
}

.fb-desc {
  margin: 0 0 16px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.fb-type-radio {
  margin-bottom: 12px;
}

.fb-textarea {
  margin-bottom: 12px;
}

.fb-submit-btn {
  width: 100%;
  border-radius: 8px;
  font-weight: bold;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
}

.my-feedbacks-history {
  margin-top: 24px;
  border-top: 1px dashed #cbd5e1;
  padding-top: 16px;
}

.history-title {
  margin: 0 0 12px;
  font-size: 13px;
  color: #475569;
}

.fb-history-item {
  background: #ffffff;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid #e2e8f0;
}

.fb-h-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.fb-h-time {
  font-size: 11px;
  color: #94a3b8;
}

.fb-h-content {
  font-size: 12px;
  color: #334155;
  line-height: 1.5;
}

.fb-h-reply {
  margin-top: 8px;
  padding: 8px;
  background: #f0fdf4;
  border-radius: 6px;
  font-size: 11px;
  color: #166534;
}

.fb-h-reply.wait {
  background: #f8fafc;
  color: #64748b;
}

.reply-label {
  font-weight: bold;
}
</style>
