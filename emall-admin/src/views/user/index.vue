<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, View, User, Phone, Message, Calendar, Plus, Edit, Delete } from '@element-plus/icons-vue'
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

// ✨ 核心修复：安全头像解析函数
// 如果没有头像，或者是系统默认的那个不存在的路径，直接返回空
// 这样 el-avatar 就不会去发起无用的网络请求报 404，而是直接显示文字兜底
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
    const res = await request.get<any, SysUser[]>('/user/list')
    userList.value = res
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 模糊搜索
const filteredUsers = computed(() => {
  if (!searchQuery.value) return userList.value
  const query = searchQuery.value.toLowerCase()
  return userList.value.filter(u => 
    (u.username && u.username.toLowerCase().includes(query)) ||
    (u.nickname && u.nickname.toLowerCase().includes(query)) ||
    (u.phone && u.phone.includes(query))
  )
})

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
          // ✨ 这里发起的 /user/add 请求，现在后端已经有对应接口了！
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
  ElMessageBox.confirm(`确定要永久删除账号 [${row.username}] 吗？`, '危险操作', {
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
    ElMessage.success(`用户状态已更新`)
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

const getRoleTagType = (role: number) => {
  if (role === 2) return 'danger'
  if (role === 1) return 'warning'
  return 'info'
}

onMounted(() => fetchUsers())
</script>

<template>
  <div class="user-container">
    <el-card shadow="never" class="glass-card">
      <div class="header-title">
        <el-icon><User /></el-icon> 用户管理中心
      </div>

      <div class="toolbar">
        <el-input
          v-model="searchQuery"
          placeholder="输入 账号 / 昵称 / 手机号 进行搜索..."
          class="search-input"
          clearable
          :prefix-icon="Search"
        />
        <div class="toolbar-right">
          <span class="user-count" style="margin-right: 20px;">共检索到 <strong>{{ filteredUsers.length }}</strong> 名用户</span>
          <el-button type="primary" :icon="Plus" round @click="handleAdd">新增用户 / 管理员</el-button>
        </div>
      </div>

      <el-table :data="filteredUsers" style="width: 100%" v-loading="loading" stripe class="custom-table">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        
        <el-table-column label="用户信息" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="40" :src="getSafeAvatar(row.avatar)" class="avatar">
                {{ row.username.charAt(0).toUpperCase() }}
              </el-avatar>
              <div class="user-info">
                <div class="username">{{ row.username }}</div>
                <div class="nickname">{{ row.nickname || '未设置昵称' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="角色权限" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)" effect="light" round>
              {{ getRoleText(row.role) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch 
              v-model="row.status" 
              :active-value="1" 
              :inactive-value="0" 
              active-color="#10b981"
              inactive-color="#f43f5e"
              :disabled="row.role === 2" 
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" width="200" align="center" />

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="showDetails(row)">详情</el-button>
            <el-button type="warning" link :icon="Edit" @click="handleEdit(row)" :disabled="row.role === 2">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)" :disabled="row.role === 2">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="dialogType === 'add' ? '✨ 新增账号' : '✏️ 修改资料'" width="500px" class="cute-dialog" destroy-on-close>
      <el-form :model="userForm" :rules="rules" ref="formRef" label-width="90px" class="user-form">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="userForm.username" :disabled="dialogType === 'edit'" placeholder="用于登录的唯一账号" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="userForm.password" type="password" show-password placeholder="请设置初始登录密码" />
        </el-form-item>
        <el-form-item label="用户昵称" prop="nickname"><el-input v-model="userForm.nickname" placeholder="前台显示的昵称" /></el-form-item>
        <el-form-item label="手机号码" prop="phone"><el-input v-model="userForm.phone" placeholder="选填" /></el-form-item>
        <el-form-item label="电子邮箱" prop="email"><el-input v-model="userForm.email" placeholder="选填" /></el-form-item>
        <el-form-item label="角色分配" prop="role">
          <el-radio-group v-model="userForm.role">
            <el-radio :label="0" border>🛍️ 普通买家</el-radio>
            <el-radio :label="1" border>🛡️ 普通管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="formVisible = false">手滑了</el-button>
        <el-button type="primary" round class="submit-btn" @click="submitForm">{{ dialogType === 'add' ? '确认创建' : '保存修改' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="用户详细资料" width="450px" class="cute-dialog" destroy-on-close>
      <div v-if="currentUser" class="detail-container">
        <div class="detail-header">
          <el-avatar :size="80" :src="getSafeAvatar(currentUser.avatar)">
            {{ currentUser.username.charAt(0).toUpperCase() }}
          </el-avatar>
          <h3>{{ currentUser.nickname || currentUser.username }}</h3>
          <el-tag :type="getRoleTagType(currentUser.role)" effect="dark" round size="small">{{ getRoleText(currentUser.role) }}</el-tag>
        </div>
        <el-descriptions :column="1" border class="detail-desc">
          <el-descriptions-item><template #label><el-icon><User /></el-icon> 登录账号</template>{{ currentUser.username }}</el-descriptions-item>
          <el-descriptions-item><template #label><el-icon><Phone /></el-icon> 手机号码</template>{{ currentUser.phone || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item><template #label><el-icon><Message /></el-icon> 电子邮箱</template>{{ currentUser.email || '未绑定' }}</el-descriptions-item>
          <el-descriptions-item><template #label><el-icon><Calendar /></el-icon> 注册时间</template>{{ currentUser.createTime }}</el-descriptions-item>
          <el-descriptions-item><template #label>账号状态</template>
            <span :style="{ color: currentUser.status === 1 ? '#10b981' : '#f43f5e', fontWeight: 'bold' }">
              {{ currentUser.status === 1 ? '正常启用' : '已被封禁' }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button round type="primary" class="submit-btn" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-container { height: 100%; }
.glass-card { border-radius: 12px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(10px); padding-bottom: 20px; }
.header-title { font-size: 18px; font-weight: bold; color: #0369a1; margin-bottom: 25px; display: flex; align-items: center; gap: 8px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.search-input { width: 350px; }
:deep(.search-input .el-input__wrapper) { border-radius: 20px; }
.user-count { font-size: 14px; color: #64748b; }
.user-count strong { color: #0284c7; font-size: 16px; }

:deep(.custom-table th.el-table__cell) { background-color: #f0f9ff; color: #0369a1; border-bottom: none; }
.user-cell { display: flex; align-items: center; gap: 15px; }
.avatar { background: #bae6fd; color: #0284c7; font-weight: bold; font-size: 18px; }
.user-info { display: flex; flex-direction: column; }
.username { font-weight: bold; color: #0f172a; }
.nickname { font-size: 12px; color: #94a3b8; }

:deep(.cute-dialog) { border-radius: 16px; overflow: hidden; }
:deep(.cute-dialog .el-dialog__header) { background: #f0f9ff; margin-right: 0; border-bottom: 1px solid #e0f2fe; padding: 20px; font-weight: bold; color: #0369a1; }
.submit-btn { background: #0ea5e9; border: none; font-weight: bold; padding: 0 25px; }
.submit-btn:hover { background: #0284c7; box-shadow: 0 4px 12px rgba(2, 132, 199, 0.3); }

.user-form { padding: 10px 20px; }
:deep(.el-radio.is-bordered.is-checked) { border-color: #0ea5e9; background: #f0f9ff; }
:deep(.el-radio__input.is-checked .el-radio__inner) { border-color: #0ea5e9; background: #0ea5e9; }

.detail-container { padding: 10px 0; }
.detail-header { display: flex; flex-direction: column; align-items: center; margin-bottom: 30px; }
.detail-header h3 { margin: 15px 0 10px 0; color: #0f172a; }
.detail-desc { border-radius: 8px; overflow: hidden; }
</style>
