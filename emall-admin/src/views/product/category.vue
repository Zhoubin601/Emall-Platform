<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus, Edit, Delete, Collection } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Category { 
  id?: number; 
  name: string; 
  parentId?: number;
  level?: number;
}

const categoryList = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const catForm = ref<Category>({ name: '', parentId: 0, level: 1 })

// ✨ 核心功能 1：过滤出所有的一级大类，供下拉菜单使用
const level1Options = computed(() => {
  return categoryList.value.filter(c => c.level === 1)
})

const fetchCategories = async () => {
  loading.value = true
  try {
    const res = await request.get<any, Category[]>('/category/list')
    categoryList.value = res
  } catch (e) {
    ElMessage.error('拉取分类失败')
  } finally {
    loading.value = false
  }
}

// ✨ 核心功能 2：当层级切换为“一级”时，强制将 parentId 设为 0
const handleLevelChange = (val: any) => {
  if (val === 1) catForm.value.parentId = 0
}

const handleAdd = () => { 
  isEdit.value = false; 
  catForm.value = { name: '', parentId: 0, level: 1 }; 
  dialogVisible.value = true 
}

const handleEdit = (row: Category) => { 
  isEdit.value = true; 
  catForm.value = { ...row }; 
  dialogVisible.value = true 
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除该分类吗？', '警告', { type: 'warning' }).then(async () => {
    await request.delete(`/category/delete/${id}`)
    ElMessage.success('分类已删除')
    fetchCategories()
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!catForm.value.name) return ElMessage.warning('分类名称不能为空')
  if (catForm.value.level === 2 && !catForm.value.parentId) {
    return ElMessage.warning('请选择该子类所属的大类')
  }
  try {
    if (isEdit.value) await request.put('/category/update', catForm.value)
    else await request.post('/category/add', catForm.value)
    ElMessage.success('操作成功')
    dialogVisible.value = false
    fetchCategories()
  } catch (e) { ElMessage.error('保存失败') }
}

const getParentName = (parentId: number) => {
  const parent = categoryList.value.find(c => c.id === parentId)
  return parent ? parent.name : '-'
}

onMounted(() => fetchCategories())
</script>

<template>
  <div class="category-container">
    <el-card class="glass-card" shadow="never">
      <div class="header">
        <div class="title"><el-icon><Collection /></el-icon> 分类与层级设置</div>
        <el-button type="primary" :icon="Plus" round @click="handleAdd">新增分类</el-button>
      </div>

      <el-table :data="categoryList" style="width: 100%" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="分类名称">
          <template #default="{ row }"><strong>{{ row.name }}</strong></template>
        </el-table-column>
        
        <el-table-column prop="level" label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.level === 1 ? 'primary' : 'success'" effect="light">
              {{ row.level === 1 ? '一级大类' : '二级子类' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="所属大类" width="150" align="center">
          <template #default="{ row }">
            <span style="color: #64748b; font-size: 13px;">{{ row.level === 2 ? getParentName(row.parentId) : '自身是大类' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类信息' : '创建新分类'" width="450px" class="glass-dialog">
      <el-form :model="catForm" label-width="100px" style="padding-right: 20px;">
        <el-form-item label="分类名称" required>
          <el-input v-model="catForm.name" placeholder="输入名称，如：智能手机" />
        </el-form-item>
        
        <el-form-item label="分类级别">
          <el-radio-group v-model="catForm.level" @change="handleLevelChange">
            <el-radio :label="1" border>一级大类</el-radio>
            <el-radio :label="2" border>二级子类</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-collapse-transition>
          <el-form-item label="关联至大类" v-if="catForm.level === 2" required>
            <el-select v-model="catForm.parentId" placeholder="请选择所属大类" style="width: 100%;">
              <el-option 
                v-for="item in level1Options" 
                :key="item.id" 
                :label="item.name" 
                :value="item.id" 
              />
            </el-select>
          </el-form-item>
        </el-collapse-transition>
      </el-form>
      <template #footer>
        <el-button round @click="dialogVisible = false">取消</el-button>
        <el-button round type="primary" @click="handleSubmit" style="padding: 0 30px;">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.glass-card { border-radius: 12px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.85); }
.header { display: flex; justify-content: space-between; margin-bottom: 20px; align-items: center; }
.title { font-size: 18px; font-weight: bold; color: #0369a1; display: flex; align-items: center; gap: 8px; }
:deep(.glass-dialog) { border-radius: 16px; overflow: hidden; }
:deep(.glass-dialog .el-dialog__header) { background: #f0f9ff; border-bottom: 1px solid #e0f2fe; margin-right: 0; padding: 20px; }
:deep(.el-radio.is-bordered) { margin-right: 10px; }
</style>