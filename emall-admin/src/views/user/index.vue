<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { 
  Search, View, User, Phone, Message, Calendar, Plus, Edit, Delete,
  Refresh, CircleCheck, Warning, Avatar, Lock
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'

interface SysUser {
  id: number | null
  username: string
  password?: string
  nickname: string
  phone: string
  email: string
  avatar?: string
  role: number
  status: number
  createTime?: string
}

const userList = ref<SysUser[]>([])
const loading = ref(false)
const searchQuery = ref('')
const roleFilter = ref<number | ''>('')
const statusFilter = ref<number | ''>('')

// === 顶部 Mini Bento 用户指标 ===
const userStats = computed(() => {
  const total = userList.value.length
  const activeBuyers = userList.value.filter(u => u.role === 0 && u.status === 1).length
  const admins = userList.value.filter(u => u.role === 1 || u.role === 2).length
  const banned = userList.value.filter(u => u.status === 0).length
  return { total, activeBuyers, admins, banned }
})

const handleStatCardClick = (type: 'all' | 'buyers' | 'admins' | 'banned') => {
  if (type === 'all') {
    roleFilter.value = ''
    statusFilter.value = ''
  } else if (type === 'buyers') {
    roleFilter.value = 0
    statusFilter.value = 1
  } else if (type === 'admins') {
    roleFilter.value = 1
    statusFilter.value = ''
  } else if (type === 'banned') {
    roleFilter.value = ''
    statusFilter.value = 0
  }
}

// === 详情弹窗状态 ===
const detailVisible = ref(false)
const currentUser = ref<SysUser | null>(null)

// === 新增/修改 表单状态 ===
const formVisible = ref(false)
const dialogType = ref<'add' | 'edit'>('add')
const formRef = ref<FormInstance>()

// 表单数据绑定对象
const userForm = reactive<SysUser>({
  id: null,
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: '',
  role: 0,
  status: 1
})

// 表单校验规则
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 72, message: '密码长度必须为 8 到 72 个字符', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色权限', trigger: 'change' }]
}

const getSafeAvatar = (avatarPath?: string) => {
  if (!avatarPath || avatarPath.includes('default-avatar.png')) {
    return '' 
  }
  return avatarPath
}

// === API 数据拉取 ===
const fetchUsers = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/user/list')
    userList.value = Array.isArray(res) ? res : (res?.records || res?.data || [])
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 模糊搜索与筛选
const filteredUsers = computed(() => {
  return userList.value.filter(u => {
    const query = searchQuery.value.toLowerCase().trim()
    const matchQuery = !query || 
      (u.username && u.username.toLowerCase().includes(query)) ||
      (u.nickname && u.nickname.toLowerCase().includes(query)) ||
      (u.phone && u.phone.includes(query))
    
    let matchRole = true
    if (roleFilter.value !== '') {
      if (roleFilter.value === 1) matchRole = u.role === 1 || u.role === 2
      else matchRole = u.role === roleFilter.value
    }

    const matchStatus = statusFilter.value === '' || u.status === statusFilter.value

    return matchQuery && matchRole && matchStatus
  })
})

const handleResetFilters = () => {
  searchQuery.value = ''
  roleFilter.value = ''
  statusFilter.value = ''
}

// === 业务操作：增删改 ===
const handleAdd = () => {
  dialogType.value = 'add'
  Object.assign(userForm, {
    id: null, username: '', password: '', nickname: '', phone: '', email: '', role: 0, status: 1
  })
  formVisible.value = true
}

const handleEdit = (row: SysUser) => {
  dialogType.value = 'edit'
  Object.assign(userForm, { ...row, password: '' })
  formVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (dialogType.value === 'add') {
          await request.post('/user/add', userForm)
          ElMessage.success('账号创建成功')
        } else {
          await request.put('/user/update', userForm)
          ElMessage.success('用户资料修改成功')
        }
        formVisible.value = false
        fetchUsers() 
      } catch (error: any) {
        ElMessage.error(error.response?.data || '操作失败')
      }
    }
  })
}

const handleDelete = (row: SysUser) => {
  ElMessageBox.confirm(`确定要永久删除账号 [${row.username}] 吗？此操作不可逆！`, '危险操作', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await request.delete(`/user/${row.id}`)
      ElMessage.success('账号已彻底删除')
      fetchUsers()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleStatusChange = async (row: SysUser) => {
  try {
    await request.put('/user/update', { id: row.id, status: row.status })
    ElMessage.success(row.status === 1 ? '账号已启用' : '账号已被封禁')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态更新失败')
  }
}

const showDetails = (row: SysUser) => {
  currentUser.value = row
  detailVisible.value = true
}

const getRoleText = (role: number) => {
  if (role === 2) return '超级管理员'
  if (role === 1) return '普通管理员'
  return '普通买家'
}

onMounted(() => fetchUsers())
</script>

<template>
  <div class="user-container">
    <!-- 1. 顶部 Mini Bento 用户画像指标 -->
    <div class="user-stats-grid">
      <div 
        class="stat-bento-card" 
        :class="{ active: roleFilter === '' && statusFilter === '' }"
        @click="handleStatCardClick('all')"
      >
        <div class="stat-icon-wrap blue"><el-icon><User /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">全域注册账号</div>
          <div class="stat-val">{{ userStats.total }} <span class="unit">人</span></div>
        </div>
        <span class="stat-tag">全景</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: roleFilter === 0 && statusFilter === 1 }"
        @click="handleStatCardClick('buyers')"
      >
        <div class="stat-icon-wrap green"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">正常流通买家</div>
          <div class="stat-val">{{ userStats.activeBuyers }} <span class="unit">人</span></div>
        </div>
        <span class="stat-tag tag-green">活跃良好</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: roleFilter === 1 && statusFilter === '' }"
        @click="handleStatCardClick('admins')"
      >
        <div class="stat-icon-wrap purple"><el-icon><Avatar /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">系统管理人员</div>
          <div class="stat-val">{{ userStats.admins }} <span class="unit">人</span></div>
        </div>
        <span class="stat-tag tag-purple">管理权限</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: statusFilter === 0 }"
        @click="handleStatCardClick('banned')"
      >
        <div class="stat-icon-wrap red"><el-icon><Warning /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">封禁受限账号</div>
          <div class="stat-val" :class="{ 'text-danger': userStats.banned > 0 }">
            {{ userStats.banned }} <span class="unit">人</span>
          </div>
        </div>
        <span class="stat-tag tag-red" v-if="userStats.banned > 0">需复核</span>
        <span class="stat-tag tag-green" v-else>零违规</span>
      </div>
    </div>

    <!-- 2. 主卡片与操作工具栏 -->
    <el-card shadow="never" class="main-user-card">
      <div class="toolbar-header">
        <div class="filter-controls">
          <el-input
            v-model="searchQuery"
            placeholder="搜索账号、昵称、手机号..."
            class="search-input"
            clearable
            :prefix-icon="Search"
          />
          <el-select v-model="roleFilter" placeholder="全部角色" clearable class="filter-select">
            <el-option label="全部角色" :value="''" />
            <el-option label="普通买家" :value="0" />
            <el-option label="管理人员 (超管/普管)" :value="1" />
          </el-select>
          <el-select v-model="statusFilter" placeholder="全部状态" clearable class="filter-select-sm">
            <el-option label="全部状态" :value="''" />
            <el-option label="正常启用" :value="1" />
            <el-option label="封禁受限" :value="0" />
          </el-select>
          <el-button 
            v-if="searchQuery || roleFilter !== '' || statusFilter !== ''"
            link 
            type="primary" 
            :icon="Refresh" 
            @click="handleResetFilters"
          >
            重置
          </el-button>
        </div>

        <div class="action-controls">
          <span class="user-count-tip">检索到 <strong>{{ filteredUsers.length }}</strong> 名用户</span>
          <el-button type="primary" :icon="Plus" class="btn-create-user" @click="handleAdd">
            新增用户 / 管理员
          </el-button>
        </div>
      </div>

      <!-- 用户列表表格 -->
      <el-table :data="filteredUsers" style="width: 100%" v-loading="loading" class="custom-admin-table">
        <el-table-column prop="id" label="ID" width="75" align="center">
          <template #default="{ row }">
            <span class="row-id">#{{ row.id }}</span>
          </template>
        </el-table-column>
        
        <!-- 用户身份 (头像 + 账号 + 昵称) -->
        <el-table-column label="用户身份" min-width="210">
          <template #default="{ row }">
            <div class="user-profile-cell">
              <el-avatar :size="42" :src="getSafeAvatar(row.avatar)" class="avatar-pill">
                {{ row.username.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-meta">
                <div class="username-row">
                  <span class="username-text">{{ row.username }}</span>
                  <el-icon v-if="row.role === 2" class="crown-icon" title="超级管理员"><Lock /></el-icon>
                </div>
                <div class="nickname-text">{{ row.nickname || '未设定昵称' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 联系方式 -->
        <el-table-column label="通讯方式" min-width="190">
          <template #default="{ row }">
            <div class="contact-cell">
              <div class="contact-item" v-if="row.phone">
                <el-icon><Phone /></el-icon> {{ row.phone }}
              </div>
              <div class="contact-item" v-if="row.email">
                <el-icon><Message /></el-icon> {{ row.email }}
              </div>
              <span v-if="!row.phone && !row.email" class="empty-contact">未绑定联系方式</span>
            </div>
          </template>
        </el-table-column>
        
        <!-- 角色权限 -->
        <el-table-column label="角色权限" width="130" align="center">
          <template #default="{ row }">
            <span class="role-capsule" :class="'role-' + row.role">
              {{ getRoleText(row.role) }}
            </span>
          </template>
        </el-table-column>

        <!-- 账号状态 -->
        <el-table-column label="账号状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch 
              v-model="row.status" 
              :active-value="1" 
              :inactive-value="0" 
              active-color="#10b981"
              inactive-color="#ef4444"
              :disabled="row.role === 2" 
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>

        <!-- 注册时间 -->
        <el-table-column label="注册时间" width="180" align="center">
          <template #default="{ row }">
            <div class="time-cell">
              <el-icon><Calendar /></el-icon> {{ row.createTime || '-' }}
            </div>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button type="primary" plain size="small" :icon="View" @click="showDetails(row)">
                详情
              </el-button>
              <el-button 
                type="warning" 
                plain 
                size="small" 
                :icon="Edit" 
                @click="handleEdit(row)" 
                :disabled="row.role === 2"
              >
                编辑
              </el-button>
              <el-button 
                type="danger" 
                link 
                size="small" 
                :icon="Delete" 
                @click="handleDelete(row)" 
                :disabled="row.role === 2"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增 / 修改用户弹窗 -->
    <el-dialog 
      v-model="formVisible" 
      :title="dialogType === 'add' ? '新增平台用户 / 管理员' : '修改用户核心资料'" 
      width="500px" 
      class="glass-dialog" 
      destroy-on-close
    >
      <el-form :model="userForm" :rules="rules" ref="formRef" label-width="90px" class="user-modal-form">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="userForm.username" :disabled="dialogType === 'edit'" placeholder="请输入系统唯一登录账号" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="userForm.password" type="password" show-password placeholder="长度需在 8 - 72 个字符之间" />
        </el-form-item>
        <el-form-item label="用户昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="前台商城展示昵称" />
        </el-form-item>
        <el-form-item label="手机号码" prop="phone">
          <el-input v-model="userForm.phone" placeholder="选填，用于接收履约通知" />
        </el-form-item>
        <el-form-item label="电子邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="选填，用于找回密码" />
        </el-form-item>
        <el-form-item label="分配角色" prop="role">
          <el-radio-group v-model="userForm.role">
            <el-radio :value="0" border>普通买家</el-radio>
            <el-radio :value="1" border>普通管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">{{ dialogType === 'add' ? '确认创建' : '保存修改' }}</el-button>
      </template>
    </el-dialog>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户核心档案详情" width="460px" class="glass-dialog" destroy-on-close>
      <div v-if="currentUser" class="detail-container">
        <div class="detail-header">
          <el-avatar :size="72" :src="getSafeAvatar(currentUser.avatar)" class="avatar-large">
            {{ currentUser.username.charAt(0).toUpperCase() }}
          </el-avatar>
          <h3 class="detail-name">{{ currentUser.nickname || currentUser.username }}</h3>
          <span class="role-capsule" :class="'role-' + currentUser.role">
            {{ getRoleText(currentUser.role) }}
          </span>
        </div>
        <el-descriptions :column="1" border size="small" class="detail-desc">
          <el-descriptions-item label="登录账号">{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item label="手机号码">{{ currentUser.phone || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item label="电子邮箱">{{ currentUser.email || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ currentUser.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账号状态">
            <span :class="currentUser.status === 1 ? 'status-active-text' : 'status-banned-text'">
              {{ currentUser.status === 1 ? '● 正常使用中' : '● 已被封禁停用' }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">完成查看</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-container {
  min-height: 100%;
}

/* 1. Mini Bento 指标网格 */
.user-stats-grid {
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
.stat-icon-wrap.green { background: #ecfdf5; color: #059669; }
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

/* 2. 主卡片与操作工具栏 */
.main-user-card {
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
  gap: 10px;
  flex-wrap: wrap;
}

.search-input { width: 260px; }
.filter-select { width: 170px; }
.filter-select-sm { width: 120px; }

.action-controls {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-count-tip {
  font-size: 13px;
  color: #64748b;
}

.user-count-tip strong {
  color: #0f172a;
}

.btn-create-user {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border: none;
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
  transition: all 0.2s ease;
}

.btn-create-user:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}

/* 3. 表格内细节元素 */
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

.user-profile-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-pill {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.username-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.username-text {
  font-weight: 700;
  color: #0f172a;
  font-size: 14px;
}

.crown-icon {
  color: #eab308;
  font-size: 14px;
}

.nickname-text {
  font-size: 12px;
  color: #64748b;
}

/* 通讯方式 */
.contact-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #475569;
}

.empty-contact {
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

/* 角色权限胶囊 */
.role-capsule {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}

.role-2 {
  background: #fdf2f8;
  color: #be185d;
  border: 1px solid #fbcfe8;
}

.role-1 {
  background: #f0f9ff;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.role-0 {
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.time-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: #64748b;
}

.actions-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 4. 弹窗样式 */
:deep(.glass-dialog) {
  border-radius: 16px;
}

:deep(.glass-dialog .el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 18px 24px;
  font-weight: 700;
  color: #0f172a;
}

:deep(.glass-dialog .el-dialog__body) {
  padding: 20px 24px;
}

:deep(.glass-dialog .el-dialog__footer) {
  border-top: 1px solid #f1f5f9;
  padding: 14px 24px;
}

.user-modal-form {
  padding: 6px 0;
}

.detail-container {
  padding: 6px 0;
}

.detail-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
}

.avatar-large {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  color: #ffffff;
  font-size: 28px;
  font-weight: 700;
}

.detail-name {
  margin: 4px 0 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.detail-desc {
  border-radius: 8px;
  overflow: hidden;
}

.status-active-text {
  color: #15803d;
  font-weight: 600;
}

.status-banned-text {
  color: #b91c1c;
  font-weight: 600;
}

@media (max-width: 1200px) {
  .user-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
