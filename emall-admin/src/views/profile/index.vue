<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useAdminStore } from '../../stores/admin'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Plus, EditPen, Message, Phone, Lock, Check } from '@element-plus/icons-vue'

const adminStore = useAdminStore()
const loading = ref(false)

// ✨ 表单数据绑定 (从 Pinia Store 中初始化)
const form = reactive({
  id: adminStore.adminInfo?.id,
  username: adminStore.adminInfo?.username,
  nickname: adminStore.adminInfo?.nickname || '',
  email: adminStore.adminInfo?.email || '',
  phone: adminStore.adminInfo?.phone || '',
  avatar: adminStore.adminInfo?.avatar || '',
  password: '' // 密码留空代表不修改
})

// ✨ 安全头像解析 (复用之前的兜底逻辑)
const getSafeAvatar = (avatarPath?: string) => {
  if (!avatarPath || avatarPath.includes('default-avatar.png')) return '' 
  if (avatarPath.startsWith('http')) return avatarPath
  const fileName = avatarPath.substring(avatarPath.lastIndexOf('/') + 1)
  return `/uploads/${encodeURIComponent(fileName)}`
}

// ✨ 自定义头像上传逻辑
const uploadAvatar = async (options: any) => {
  const formData = new FormData()
  formData.append('file', options.file)
  formData.append('username', form.username) // 传给后端命名用
  
  try {
    const res: any = await request.post('/file/uploadAvatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.avatar = res.url // 拿到后端返回的路径
    ElMessage.success('头像上传成功')
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
}

// ✨ 保存资料并同步全局状态
const handleSave = async () => {
  loading.value = true
  try {
    const updateData = { ...form }
    // 如果密码框没填，就从提交数据里删掉，防止把密码改为空
    if (!updateData.password) {
      delete updateData.password
    }

    await request.put('/user/update', updateData) // 复用后端的 update 接口
    
    // ✨ 核心：同步更新 Pinia 和 LocalStorage，让页面右上角的头像立刻刷新
    const updatedInfo = { ...adminStore.adminInfo, ...updateData }
    adminStore.setAdminLogin(adminStore.token, updatedInfo)
    
    ElMessage.success('个人资料更新成功！')
    form.password = '' // 保存后清空密码框
  } catch (e) {
    ElMessage.error('更新失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="profile-container">
    <el-card class="glass-card" shadow="never">
      <div class="profile-layout">
        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :http-request="uploadAvatar"
            accept="image/jpeg,image/png,image/webp"
          >
            <div class="avatar-wrapper">
              <el-avatar v-if="form.avatar && !form.avatar.includes('default-avatar')" :size="120" :src="getSafeAvatar(form.avatar)" class="user-avatar" />
              <el-avatar v-else :size="120" class="user-avatar default-avatar-bg">
                {{ form.username.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="avatar-hover-mask">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
          </el-upload>
          <div class="user-role-tag">
            <el-tag :type="adminStore.adminInfo.role === 2 ? 'danger' : 'warning'" effect="dark" round>
              {{ adminStore.adminInfo.role === 2 ? '超级管理员' : '普通管理员' }}
            </el-tag>
          </div>
        </div>

        <div class="form-section">
          <h2 class="section-title">个人信息设置</h2>
          <el-form label-position="top" class="custom-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="登录账号 (不可修改)">
                  <el-input v-model="form.username" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="显示昵称">
                  <el-input v-model="form.nickname" :prefix-icon="EditPen" placeholder="设置你的专属昵称" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="电子邮箱">
                  <el-input v-model="form.email" :prefix-icon="Message" placeholder="绑定安全邮箱" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话">
                  <el-input v-model="form.phone" :prefix-icon="Phone" placeholder="手机号码" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="修改密码 (留空则不修改)">
              <el-input v-model="form.password" type="password" :prefix-icon="Lock" show-password placeholder="若需修改，请在此输入新密码" />
            </el-form-item>

            <div class="action-row">
              <el-button type="primary" class="save-btn" round :icon="Check" :loading="loading" @click="handleSave">
                保存所有修改
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.profile-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 20px;
}

/* 玻璃卡片 */
.glass-card {
  width: 800px;
  border-radius: 24px;
  border: 1px solid rgba(186, 230, 253, 0.5);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  padding: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.05);
}

.profile-layout {
  display: flex;
  gap: 50px;
}

/* 左侧头像区 */
.avatar-section {
  width: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 20px;
}

.avatar-wrapper {
  position: relative;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 4px solid #e0f2fe;
  box-shadow: 0 10px 25px rgba(2, 132, 199, 0.15);
  transition: all 0.3s;
}

.avatar-wrapper:hover {
  transform: scale(1.05);
  border-color: #bae6fd;
}

.default-avatar-bg {
  background: #bae6fd;
  color: #0284c7;
  font-weight: bold;
  font-size: 40px;
}

.avatar-hover-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 13px;
  font-weight: bold;
}

.avatar-wrapper:hover .avatar-hover-mask {
  opacity: 1;
}

.upload-icon {
  font-size: 24px;
  margin-bottom: 5px;
}

.user-role-tag {
  margin-top: 20px;
}

/* 右侧表单区 */
.form-section {
  flex: 1;
}

.section-title {
  color: #0f172a;
  font-size: 20px;
  margin-bottom: 25px;
  margin-top: 0;
}

.custom-form :deep(.el-form-item__label) {
  font-weight: bold;
  color: #475569;
}

.custom-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  background-color: #f8fafc;
  box-shadow: none;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.custom-form :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  border-color: #0ea5e9;
  box-shadow: 0 0 0 2px rgba(14, 165, 233, 0.1);
}

.action-row {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
}

.save-btn {
  padding: 0 30px;
  height: 40px;
  font-weight: bold;
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
  box-shadow: 0 8px 15px rgba(14, 165, 233, 0.2);
}
.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px rgba(14, 165, 233, 0.3);
}
</style>