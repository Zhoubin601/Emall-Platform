<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { 
  Plus, Edit, Delete, Collection, Search, Refresh, 
  FolderOpened, Document, Share, Connection
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Category { 
  id?: number; 
  name: string; 
  parentId?: number;
  level?: number;
  sort?: number;
}

const categoryList = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const catForm = ref<Category>({ name: '', parentId: 0, level: 1 })

// 筛选状态
const searchQuery = ref('')
const levelFilter = ref<'all' | 1 | 2>('all')
const parentFilter = ref<number | ''>('')

// 过滤出所有的一级大类，供下拉菜单使用
const level1Options = computed(() => {
  return categoryList.value.filter(c => c.level === 1)
})

// 顶部 Mini Bento 统计指标
const categoryStats = computed(() => {
  const total = categoryList.value.length
  const level1List = categoryList.value.filter(c => c.level === 1)
  const level2List = categoryList.value.filter(c => c.level === 2)
  const level1Count = level1List.length
  const level2Count = level2List.length
  const coveredParents = new Set(level2List.map(c => c.parentId)).size
  const coverageRate = level1Count > 0 ? Math.round((coveredParents / level1Count) * 100) : 100
  return {
    total,
    level1Count,
    level2Count,
    coverageRate
  }
})

// 计算某个一级分类下属的二级分类数量
const getSubCount = (parentId?: number) => {
  if (!parentId) return 0
  return categoryList.value.filter(c => c.level === 2 && c.parentId === parentId).length
}

// 树形层级排序与过滤后的展示列表
const structuredCategories = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  const hasFilter = q !== '' || levelFilter.value !== 'all' || parentFilter.value !== ''

  if (hasFilter) {
    return categoryList.value.filter(c => {
      const matchName = !q || c.name.toLowerCase().includes(q)
      const matchLevel = levelFilter.value === 'all' || c.level === levelFilter.value
      const matchParent = parentFilter.value === '' || c.parentId === parentFilter.value || c.id === parentFilter.value
      return matchName && matchLevel && matchParent
    })
  }

  // 默认按树形父子结构排列：一级大类后紧跟其二级子类
  const result: Category[] = []
  const l1 = categoryList.value.filter(c => c.level === 1)
  const l2 = categoryList.value.filter(c => c.level === 2)
  const assignedIds = new Set<number>()

  for (const parent of l1) {
    result.push(parent)
    const children = l2.filter(child => child.parentId === parent.id)
    for (const child of children) {
      result.push(child)
      if (child.id) assignedIds.add(child.id)
    }
  }

  // 兜底未匹配父类的二级分类
  for (const orphan of l2) {
    if (orphan.id && !assignedIds.has(orphan.id)) {
      result.push(orphan)
    }
  }

  return result
})

const fetchCategories = async () => {
  loading.value = true
  try {
    const res: any = await request.get<any, Category[]>('/category/list')
    const list = Array.isArray(res) ? res : (res?.data || res?.records || [])
    categoryList.value = list
  } catch (e) {
    ElMessage.error('拉取分类失败')
  } finally {
    loading.value = false
  }
}

// 当层级切换为“一级”时，强制将 parentId 设为 0
const handleLevelChange = (val: any) => {
  if (val === 1) catForm.value.parentId = 0
}

const handleAdd = () => { 
  isEdit.value = false
  catForm.value = { name: '', parentId: 0, level: 1 }
  dialogVisible.value = true 
}

// 快捷在某个一级大类下新增二级子类
const handleAddSub = (parentRow: Category) => {
  isEdit.value = false
  catForm.value = { name: '', parentId: parentRow.id || 0, level: 2 }
  dialogVisible.value = true
}

const handleEdit = (row: Category) => { 
  isEdit.value = true
  catForm.value = { ...row }
  dialogVisible.value = true 
}

const handleDelete = (id: number) => {
  const subCount = getSubCount(id)
  const confirmMsg = subCount > 0 
    ? `该一级大类下包含 ${subCount} 个二级子类，确定要删除吗？` 
    : '确定要删除该商品分类吗？'
  ElMessageBox.confirm(confirmMsg, '删除确认', { 
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning' 
  }).then(async () => {
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
    ElMessage.success(isEdit.value ? '分类更新成功' : '新分类创建成功')
    dialogVisible.value = false
    fetchCategories()
  } catch (e) { 
    ElMessage.error('保存失败') 
  }
}

const getParentName = (parentId?: number) => {
  if (!parentId) return '-'
  const parent = categoryList.value.find(c => c.id === parentId)
  return parent ? parent.name : '-'
}

const resetFilters = () => {
  searchQuery.value = ''
  levelFilter.value = 'all'
  parentFilter.value = ''
}

onMounted(() => fetchCategories())
</script>

<template>
  <div class="category-container">
    <!-- 1. 顶部类目资产 Mini Bento 指标卡 -->
    <div class="category-stats-grid">
      <div 
        class="stat-bento-card" 
        :class="{ active: levelFilter === 'all' }"
        @click="levelFilter = 'all'"
      >
        <div class="stat-icon-wrap blue"><el-icon><Collection /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">全站类目节点总数</div>
          <div class="stat-val">{{ categoryStats.total }} <span class="unit">个</span></div>
        </div>
        <span class="stat-tag">全景树</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: levelFilter === 1 }"
        @click="levelFilter = levelFilter === 1 ? 'all' : 1"
      >
        <div class="stat-icon-wrap indigo"><el-icon><FolderOpened /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">一级核心大类</div>
          <div class="stat-val">{{ categoryStats.level1Count }} <span class="unit">大类</span></div>
        </div>
        <span class="stat-tag tag-indigo">频道导航</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: levelFilter === 2 }"
        @click="levelFilter = levelFilter === 2 ? 'all' : 2"
      >
        <div class="stat-icon-wrap green"><el-icon><Share /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">二级垂直子类</div>
          <div class="stat-val">{{ categoryStats.level2Count }} <span class="unit">子类</span></div>
        </div>
        <span class="stat-tag tag-green">精细运营</span>
      </div>

      <div class="stat-bento-card" @click="resetFilters">
        <div class="stat-icon-wrap amber"><el-icon><Connection /></el-icon></div>
        <div class="stat-meta">
          <div class="stat-label">大类子级覆盖率</div>
          <div class="stat-val">{{ categoryStats.coverageRate }} <span class="unit">%</span></div>
        </div>
        <span class="stat-tag tag-amber">层级完整</span>
      </div>
    </div>

    <!-- 2. 主表格与工具栏 -->
    <el-card class="main-category-card" shadow="never">
      <div class="toolbar-header">
        <div class="filter-controls">
          <el-input
            v-model="searchQuery"
            placeholder="搜索分类名称..."
            clearable
            :prefix-icon="Search"
            class="search-input"
          />
          <el-select v-model="levelFilter" placeholder="层级筛选" class="level-select">
            <el-option label="全部层级" value="all" />
            <el-option label="仅看一级大类" :value="1" />
            <el-option label="仅看二级子类" :value="2" />
          </el-select>
          <el-select v-model="parentFilter" placeholder="按所属大类过滤" clearable class="parent-select">
            <el-option
              v-for="item in level1Options"
              :key="item.id"
              :label="item.name"
              :value="item.id!"
            />
          </el-select>
          <el-button 
            v-if="searchQuery || levelFilter !== 'all' || parentFilter !== ''"
            link 
            type="primary" 
            :icon="Refresh" 
            @click="resetFilters"
          >
            重置筛选
          </el-button>
        </div>

        <div class="action-controls">
          <el-button type="primary" :icon="Plus" class="btn-create-cat" @click="handleAdd">
            新增商品分类
          </el-button>
        </div>
      </div>

      <el-table 
        :data="structuredCategories" 
        style="width: 100%" 
        v-loading="loading" 
        class="custom-admin-table"
        :row-class-name="({ row }: { row: any }) => row.level === 1 ? 'level-1-row' : 'level-2-row'"
      >
        <el-table-column prop="id" label="ID" width="85" align="center">
          <template #default="{ row }">
            <span class="row-id">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <!-- 分类名称（含树形缩进结构） -->
        <el-table-column prop="name" label="分类层级结构与名称" min-width="260">
          <template #default="{ row }">
            <div class="cat-name-cell" :class="{ 'is-child': row.level === 2 }">
              <template v-if="row.level === 2">
                <span class="tree-branch">└─</span>
                <el-icon class="child-icon"><Document /></el-icon>
                <span class="child-name">{{ row.name }}</span>
              </template>
              <template v-else>
                <span class="folder-badge"><el-icon><FolderOpened /></el-icon></span>
                <strong class="parent-name">{{ row.name }}</strong>
                <span class="sub-count-pill" v-if="getSubCount(row.id) > 0">
                  {{ getSubCount(row.id) }} 个子类
                </span>
              </template>
            </div>
          </template>
        </el-table-column>
        
        <!-- 级别标签 -->
        <el-table-column prop="level" label="层级类型" width="140" align="center">
          <template #default="{ row }">
            <span class="level-capsule" :class="row.level === 1 ? 'lv-1' : 'lv-2'">
              <span class="lv-dot"></span>
              {{ row.level === 1 ? '一级大类' : '二级子类' }}
            </span>
          </template>
        </el-table-column>

        <!-- 所属大类 -->
        <el-table-column label="上级归属大类" width="180" align="center">
          <template #default="{ row }">
            <span v-if="row.level === 2" class="parent-ref-pill">
              {{ getParentName(row.parentId) }}
            </span>
            <span v-else class="root-node-text">根节点 (Root)</span>
          </template>
        </el-table-column>
        
        <!-- 操作列 -->
        <el-table-column label="管理操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <div class="cat-actions">
              <el-button 
                v-if="row.level === 1" 
                type="success" 
                plain 
                size="small" 
                :icon="Plus" 
                class="action-pill"
                @click="handleAddSub(row)"
              >
                加子类
              </el-button>
              <el-button 
                type="primary" 
                plain 
                size="small" 
                :icon="Edit" 
                class="action-pill"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button 
                type="danger" 
                link 
                size="small" 
                :icon="Delete" 
                @click="handleDelete(row.id)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建 / 编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑商品分类信息' : '创建商品分类节点'" 
      width="460px" 
      class="glass-dialog"
      destroy-on-close
    >
      <el-form :model="catForm" label-width="95px" class="cat-modal-form">
        <el-form-item label="分类名称" required>
          <el-input v-model="catForm.name" placeholder="输入分类名称，如：旗舰手机 / 智能穿戴" clearable />
        </el-form-item>
        
        <el-form-item label="分类级别">
          <el-radio-group v-model="catForm.level" @change="handleLevelChange">
            <el-radio :value="1" border>一级大类</el-radio>
            <el-radio :value="2" border>二级子类</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-collapse-transition>
          <el-form-item label="关联至大类" v-if="catForm.level === 2" required>
            <el-select v-model="catForm.parentId" placeholder="请选择所属的一级大类" style="width: 100%;">
              <el-option 
                v-for="item in level1Options" 
                :key="item.id" 
                :label="item.name" 
                :value="item.id!" 
              />
            </el-select>
          </el-form-item>
        </el-collapse-transition>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-container {
  min-height: 100%;
}

/* 1. 顶部 Bento 指标卡 */
.category-stats-grid {
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
.stat-icon-wrap.indigo { background: #eef2ff; color: #4f46e5; }
.stat-icon-wrap.green { background: #ecfdf5; color: #059669; }
.stat-icon-wrap.amber { background: #fffbeb; color: #d97706; }

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

.stat-tag.tag-indigo { background: #e0e7ff; color: #4338ca; }
.stat-tag.tag-green { background: #dcfce7; color: #15803d; }
.stat-tag.tag-amber { background: #fef3c7; color: #b45309; }

/* 2. 主表格与工具栏 */
.main-category-card {
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
  gap: 12px;
  flex-wrap: wrap;
}

.search-input { width: 240px; }
.level-select { width: 140px; }
.parent-select { width: 170px; }

.btn-create-cat {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border: none;
  font-weight: 600;
  border-radius: 8px;
  padding: 8px 18px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

/* 3. 表格树形视觉 */
:deep(.custom-admin-table th.el-table__cell) {
  background-color: #f8fafc;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 0;
}

:deep(.custom-admin-table .level-1-row) {
  background-color: #ffffff;
}

:deep(.custom-admin-table .level-2-row) {
  background-color: #fafbfc;
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

.cat-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cat-name-cell.is-child {
  padding-left: 28px;
}

.folder-badge {
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: #eff6ff;
  color: #2563eb;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}

.parent-name {
  font-size: 14px;
  color: #0f172a;
  font-weight: 700;
}

.sub-count-pill {
  font-size: 11px;
  color: #475569;
  background: #f1f5f9;
  padding: 1px 8px;
  border-radius: 12px;
  font-weight: 500;
}

.tree-branch {
  color: #94a3b8;
  font-family: monospace;
  font-weight: 700;
  font-size: 14px;
}

.child-icon {
  color: #10b981;
  font-size: 15px;
}

.child-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

/* 层级胶囊 */
.level-capsule {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}

.lv-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.level-capsule.lv-1 {
  background: #eff6ff;
  color: #1d4ed8;
}
.level-capsule.lv-1 .lv-dot { background: #2563eb; }

.level-capsule.lv-2 {
  background: #ecfdf5;
  color: #047857;
}
.level-capsule.lv-2 .lv-dot { background: #10b981; }

.parent-ref-pill {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 6px;
}

.root-node-text {
  font-size: 12px;
  color: #94a3b8;
  font-family: ui-monospace, SFMono-Regular, monospace;
}

.cat-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-pill {
  border-radius: 6px;
  font-weight: 500;
}

/* 弹窗样式 */
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

@media (max-width: 1200px) {
  .category-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
