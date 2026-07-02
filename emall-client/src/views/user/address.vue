<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Back, Plus, Edit, Delete, Location, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const addressList = ref<any[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  userId: userStore.userInfo?.id,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  region: '',
  detailAddress: '',
  isDefault: 0
})

const fetchAddress = async () => {
  const res = await request.get('/address/list', { params: { userId: userStore.userInfo.id } })
  addressList.value = res
}

const openAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, receiverName: '', receiverPhone: '', province: '', city: '', region: '', detailAddress: '', isDefault: 0 })
  dialogVisible.value = true
}

const openEdit = (item: any) => {
  isEdit.value = true
  Object.assign(form, item)
  dialogVisible.value = true
}

const handleSave = async () => {
  if (isEdit.value) {
    await request.put('/address/update', form)
  } else {
    await request.post('/address/add', form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  fetchAddress()
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除这个地址吗？', '警告', { type: 'warning' }).then(async () => {
    await request.delete(`/address/delete/${id}`)
    ElMessage.success('删除成功')
    fetchAddress()
  })
}

const handleSetDefault = async (id: number) => {
  await request.put(`/address/setDefault/${userStore.userInfo.id}/${id}`)
  ElMessage.success('已设为默认地址')
  fetchAddress()
}

onMounted(() => fetchAddress())
</script>

<template>
  <div class="address-page">
    <header class="glass-header">
      <el-button link :icon="Back" @click="router.push('/')">返回首页</el-button>
      <span class="title">收货地址管理</span>
      <el-button type="primary" round :icon="Plus" @click="openAdd">新增地址</el-button>
    </header>

    <main class="container">
      <el-row :gutter="20">
        <el-col :span="12" v-for="item in addressList" :key="item.id">
          <el-card class="address-card" :class="{ 'is-default': item.isDefault }" shadow="hover">
            <div class="card-header">
              <span class="name">{{ item.receiverName }}</span>
              <span class="phone">{{ item.receiverPhone }}</span>
              <el-tag v-if="item.isDefault" size="small" effect="dark" class="tag">默认</el-tag>
            </div>
            <p class="detail">{{ item.province }} {{ item.city }} {{ item.region }} {{ item.detailAddress }}</p>
            <div class="card-actions">
              <el-button link :icon="Check" v-if="!item.isDefault" @click="handleSetDefault(item.id)">设为默认</el-button>
              <el-button link :icon="Edit" @click="openEdit(item)">编辑</el-button>
              <el-button link :icon="Delete" type="danger" @click="handleDelete(item.id)">删除</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </main>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '修改地址' : '新增地址'" width="500px" class="cute-dialog">
      <el-form :model="form" label-position="top">
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="收货人"><el-input v-model="form.receiverName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.receiverPhone" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="10">
          <el-col :span="8"><el-form-item label="省"><el-input v-model="form.province" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="市"><el-input v-model="form.city" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="区"><el-input v-model="form.region" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="详细地址"><el-input type="textarea" v-model="form.detailAddress" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.address-page { min-height: 100vh; background: #f0f9ff; padding: 80px 50px; }
.glass-header { position: fixed; top: 0; left: 0; right: 0; height: 60px; background: rgba(255,255,255,0.7); backdrop-filter: blur(10px); display: flex; align-items: center; justify-content: space-between; padding: 0 50px; z-index: 100; border-bottom: 1px solid #e0f2fe; }
.title { font-weight: bold; color: #0369a1; font-size: 18px; }
.container { max-width: 1000px; margin: 0 auto; }
.address-card { border-radius: 20px; margin-bottom: 20px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255,255,255,0.8); }
.is-default { border-color: #38bdf8; background: #f0f9ff; }
.card-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.name { font-weight: 900; color: #1e293b; font-size: 16px; }
.phone { color: #64748b; font-size: 14px; }
.detail { font-size: 13px; color: #475569; line-height: 1.6; min-height: 40px; }
.card-actions { margin-top: 15px; border-top: 1px dashed #e2e8f0; padding-top: 10px; display: flex; justify-content: flex-end; gap: 10px; }
.tag { background-color: #38bdf8; border: none; }
</style>