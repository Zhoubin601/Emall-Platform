<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Plus, Edit, Delete, Check, Location, Phone, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import UserCenterLayout from '../../components/UserCenterLayout.vue'

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
  if (!userStore.userInfo) return
  const res: any = await request.get('/address/list', { params: { userId: userStore.userInfo.id } })
  addressList.value = res || []
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
  if (!form.receiverName || !form.receiverPhone || !form.province || !form.city || !form.detailAddress) {
    return ElMessage.warning('请填写完整的收件人及地址信息')
  }

  if (isEdit.value) {
    await request.put('/address/update', form)
  } else {
    await request.post('/address/add', form)
  }
  ElMessage.success('收货地址保存成功！')
  dialogVisible.value = false
  fetchAddress()
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除这个收货地址吗？', '删除确认', { 
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning' 
  }).then(async () => {
    await request.delete(`/address/delete/${id}`)
    ElMessage.success('地址已成功删除')
    fetchAddress()
  })
}

const handleSetDefault = async (id: number) => {
  await request.put(`/address/setDefault/${userStore.userInfo.id}/${id}`)
  ElMessage.success('已将该地址设为默认收货地址')
  fetchAddress()
}

onMounted(() => fetchAddress())
</script>

<template>
  <UserCenterLayout activeMenu="address" pageTitle="收货地址管理">
    <div class="address-view-wrap">
      <!-- 标题操作栏 -->
      <div class="section-title-bar">
        <div>
          <h2>收货地址管理</h2>
          <span class="sub-hint">管理您的常用收货地址，下单时可智能自动推荐与极速填充</span>
        </div>
        <el-button type="primary" round :icon="Plus" class="add-addr-btn" @click="openAdd">
          新增收货地址
        </el-button>
      </div>

      <!-- 地址列表网格 -->
      <div class="address-cards-grid">
        <div 
          v-for="item in addressList" 
          :key="item.id" 
          class="addr-card-item"
          :class="{ 'is-default': item.isDefault }"
        >
          <div class="card-top-row">
            <div class="user-block">
              <span class="user-name">{{ item.receiverName }}</span>
              <span class="user-phone">{{ item.receiverPhone }}</span>
            </div>
            <span class="tag-default-badge" v-if="item.isDefault">默认地址</span>
          </div>

          <div class="card-location-row">
            <el-icon color="#0ea5e9" class="loc-icon"><Location /></el-icon>
            <div class="loc-text">
              <span class="area-text">{{ item.province }} {{ item.city }} {{ item.region }}</span>
              <span class="detail-text">{{ item.detailAddress }}</span>
            </div>
          </div>

          <div class="card-action-footer">
            <el-button 
              v-if="!item.isDefault" 
              link 
              type="primary" 
              size="small" 
              :icon="Check" 
              @click="handleSetDefault(item.id)"
            >
              设为默认
            </el-button>
            <span v-else class="empty-holder"></span>

            <div class="action-btns-right">
              <el-button link size="small" :icon="Edit" @click="openEdit(item)">编辑</el-button>
              <el-button link size="small" type="danger" :icon="Delete" @click="handleDelete(item.id)">删除</el-button>
            </div>
          </div>
        </div>

        <!-- 添加新地址空白磁贴 -->
        <div class="addr-card-item add-tile-card" @click="openAdd">
          <div class="add-icon-circle">
            <el-icon :size="28" color="#0ea5e9"><Plus /></el-icon>
          </div>
          <span class="add-tile-text">新增收货地址</span>
        </div>
      </div>
    </div>

    <!-- 地址编辑模态框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '修改收货地址' : '新增收货地址'" 
      width="520px" 
      class="cute-addr-dialog"
      append-to-body
    >
      <el-form label-position="top" class="addr-modal-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="收件人姓名" required>
              <el-input v-model="form.receiverName" placeholder="如：张三">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" required>
              <el-input v-model="form.receiverPhone" placeholder="11位有效手机号码">
                <template #prefix><el-icon><Phone /></el-icon></template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="省份" required>
              <el-input v-model="form.province" placeholder="如：北京市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市" required>
              <el-input v-model="form.city" placeholder="如：北京市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区/县">
              <el-input v-model="form.region" placeholder="如：海淀区" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细街道地址" required>
          <el-input 
            v-model="form.detailAddress" 
            type="textarea" 
            :rows="3" 
            placeholder="详细到门牌号、楼层、园区或单元..." 
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.isDefault" :true-label="1" :false-label="0" label="设为默认收货地址" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" class="modal-save-btn" @click="handleSave">保存地址</el-button>
      </template>
    </el-dialog>
  </UserCenterLayout>
</template>

<style scoped>
.address-view-wrap {
  width: 100%;
}

.section-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.add-addr-btn {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  border: none;
  font-weight: bold;
}

/* 网格卡片 */
.address-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.addr-card-item {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 140px;
  position: relative;
  transition: all 0.2s;
}

.addr-card-item:hover {
  border-color: #0ea5e9;
  box-shadow: 0 6px 18px rgba(14, 165, 233, 0.08);
}

.addr-card-item.is-default {
  border-color: #0284c7;
  background: #f0f9ff;
}

.card-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.user-name {
  font-size: 15px;
  font-weight: bold;
  color: #1e293b;
  margin-right: 10px;
}

.user-phone {
  font-size: 13px;
  color: #64748b;
}

.tag-default-badge {
  background: #0284c7;
  color: #ffffff;
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: bold;
}

.card-location-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;
}

.loc-icon {
  margin-top: 2px;
}

.loc-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.area-text {
  font-size: 13px;
  color: #334155;
  font-weight: 500;
}

.detail-text {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.card-action-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px dashed #e2e8f0;
  padding-top: 10px;
}

.empty-holder {
  width: 1px;
}

.action-btns-right {
  display: flex;
  gap: 8px;
}

/* 新增磁贴 */
.add-tile-card {
  border: 2px dashed #cbd5e1;
  background: #f8fafc;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.add-tile-card:hover {
  border-color: #0ea5e9;
  background-color: #f0f9ff;
}

.add-icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #e0f2fe;
  display: flex;
  justify-content: center;
  align-items: center;
}

.add-tile-text {
  font-size: 14px;
  font-weight: bold;
  color: #0284c7;
}

.modal-save-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-weight: bold;
}
</style>
