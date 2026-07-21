<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Plus, Picture, Operation, Clock, Download, Upload } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Category { id: number; name: string }

interface Product {
  id?: number; name: string; categoryId?: number; price: number;
  stock: number; picUrl?: string; description: string;
  status: number; sales?: number; createTime?: string;
  promoPrice?: number; promoStartTime?: string; promoEndTime?: string; 
}

interface Sku { id?: number; productId: number; specName: string; price: number; stock: number; picUrl?: string; }

const productList = ref<Product[]>([])
const categoryList = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)

const searchQuery = ref('')
const searchCategory = ref<number | ''>('')

const productForm = reactive<Product>({
  name: '', categoryId: undefined, price: 0, stock: 0, picUrl: '', description: '', status: 1
})

const fetchProducts = async () => {
  loading.value = true
  try { productList.value = await request.get<any, Product[]>('/product/list') } 
  finally { loading.value = false }
}

const fetchCategories = async () => {
  try { categoryList.value = await request.get<any, Category[]>('/category/list') } 
  catch (error) { categoryList.value = [{ id: 1, name: '数码3C' }, { id: 2, name: '服装服饰' }, { id: 3, name: '家居日用' }] }
}

const filteredProducts = computed(() => {
  return productList.value.filter(p => {
    const matchName = p.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchCat = searchCategory.value === '' || p.categoryId === searchCategory.value
    return matchName && matchCat
  })
})

const handleAdd = () => {
  isEdit.value = false
  Object.assign(productForm, { id: undefined, name: '', categoryId: undefined, price: 0, stock: 0, picUrl: '', description: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: Product) => {
  isEdit.value = true
  Object.assign(productForm, row)
  dialogVisible.value = true
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要彻底删除该商品吗？此操作不可逆！', '高危操作', { type: 'error' }).then(async () => {
    await request.delete(`/product/delete/${id}`)
    ElMessage.success('商品已彻底删除！')
    fetchProducts()
  }).catch(() => {})
}

const handleStatusChange = async (row: Product) => {
  try {
    await request.put('/product/update', { id: row.id, status: row.status })
    ElMessage.success(row.status === 1 ? '商品已上架' : '商品已下架')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态切换失败')
  }
}

const handleSubmit = async () => {
  if (!productForm.name || !productForm.categoryId) return ElMessage.warning('必填项不能为空')
  submitLoading.value = true
  try {
    if (isEdit.value) await request.put('/product/update', productForm)
    else await request.post('/product/add', productForm)
    ElMessage.success('操作成功！')
    dialogVisible.value = false
    fetchProducts()
  } finally { submitLoading.value = false }
}

const getCategoryName = (cId: number) => {
  const cat = categoryList.value.find(c => c.id === cId)
  return cat ? cat.name : '未分类'
}

// ================= ✨ 数据导入导出模块 =================
const handleExport = async () => {
  try {
    ElMessage.info('正在生成商品报表，请稍候...')
    const res: any = await request.get('/product/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `E-MALL商品数据_${new Date().getTime()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('导出成功！')
  } catch (error) {
    ElMessage.error('导出失败，请检查网络或数据异常')
  }
}

const handleImport = async (options: any) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res: any = await request.post('/product/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(res || '批量导入成功！')
    fetchProducts() 
  } catch (error) {
    ElMessage.error('导入失败，请检查 Excel 格式是否正确')
  }
}

// ================= ✨ 强化版：规格管理模块 =================
const skuDrawerVisible = ref(false)
const activeProduct = ref<Product | null>(null)
const skuList = ref<Sku[]>([])
const skuLoading = ref(false)
const skuDialogVisible = ref(false)
const isSkuEdit = ref(false)
const skuForm = reactive<Sku>({ productId: 0, specName: '', price: 0, stock: 0, picUrl: '' })

const openSkuDrawer = async (row: Product) => { activeProduct.value = row; skuDrawerVisible.value = true; fetchSkus(row.id!) }
const fetchSkus = async (pId: number) => { skuLoading.value = true; try { skuList.value = await request.get<any, Sku[]>(`/sku/list/${pId}`) } finally { skuLoading.value = false } }
const handleAddSku = () => { isSkuEdit.value = false; Object.assign(skuForm, { id: undefined, productId: activeProduct.value?.id, specName: '', price: activeProduct.value?.price || 0, stock: 0, picUrl: activeProduct.value?.picUrl || '' }); skuDialogVisible.value = true }
const handleEditSku = (row: Sku) => { isSkuEdit.value = true; Object.assign(skuForm, row); skuDialogVisible.value = true }

// 💥 强化：保存规格后触发全量同步
const submitSkuForm = async () => { 
  if (!skuForm.specName) return ElMessage.warning('请填写规格名称')
  if (!skuForm.picUrl && activeProduct.value?.picUrl) skuForm.picUrl = activeProduct.value.picUrl
  try { 
    if (isSkuEdit.value) await request.put('/sku/update', skuForm)
    else await request.post('/sku/add', skuForm)
    
    await request.get('/product/sync-inventory') // 强制重算总库存并清理缓存
    
    ElMessage.success('保存成功！外层总库存已自动对齐')
    skuDialogVisible.value = false
    fetchSkus(activeProduct.value!.id!)
    fetchProducts() // 刷新外层列表，保证数字绝对一致
  } catch (e) {} 
}

// ================= 秒杀设置模块 =================
const promoDialogVisible = ref(false)
const promoForm = reactive({ id: 0, promoPrice: 0, timeRange: [] as string[] })

const openPromoDialog = (row: Product) => {
  promoForm.id = row.id!
  promoForm.promoPrice = row.promoPrice || row.price
  promoForm.timeRange = (row.promoStartTime && row.promoEndTime) ? [row.promoStartTime, row.promoEndTime] : []
  promoDialogVisible.value = true
}

const submitPromoForm = async () => {
  if (!promoForm.timeRange || promoForm.timeRange.length !== 2) {
    return ElMessage.warning('请选择完整的秒杀起止时间')
  }
  try {
    submitLoading.value = true
    await request.put('/product/update', {
      id: promoForm.id,
      promoPrice: promoForm.promoPrice,
      promoStartTime: promoForm.timeRange[0],
      promoEndTime: promoForm.timeRange[1]
    })
    ElMessage.success('🎉 秒杀活动设置成功！')
    promoDialogVisible.value = false
    fetchProducts()
  } finally { submitLoading.value = false }
}

const clearPromo = async () => {
  try {
    await request.put('/product/update', { id: promoForm.id, promoPrice: null, promoStartTime: null, promoEndTime: null })
    ElMessage.success('已取消该商品的秒杀活动')
    promoDialogVisible.value = false
    fetchProducts()
  } catch (e) {}
}

onMounted(() => { fetchCategories(); fetchProducts() })
</script>

<template>
  <div class="product-container">
    <el-card class="glass-card" shadow="hover">
      <div class="toolbar">
        <div class="filter-group">
          <el-input v-model="searchQuery" placeholder="搜索商品名称..." class="search-input" clearable :prefix-icon="Search" />
          <el-select v-model="searchCategory" placeholder="全部分类" clearable class="filter-select">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </div>
        
        <div style="display: flex; gap: 10px;">
          <el-button type="success" plain :icon="Download" @click="handleExport" round>
            导出 Excel
          </el-button>
          
          <el-upload
            action="#"
            :show-file-list="false"
            :http-request="handleImport"
            accept=".xlsx, .xls"
          >
            <el-button type="warning" plain :icon="Upload" round>导入商品</el-button>
          </el-upload>

          <el-button type="primary" :icon="Plus" class="cute-btn" @click="handleAdd" round>发布新商品</el-button>
        </div>
      </div>

      <el-table :data="filteredProducts" style="width: 100%" v-loading="loading" stripe class="custom-table">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column label="商品信息" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image :src="row.picUrl" class="p-img" fit="cover"><template #error><div class="img-error"><el-icon><Picture /></el-icon></div></template></el-image>
              <div class="p-info">
                <div class="p-name">{{ row.name }}</div>
                <el-tag size="small" effect="plain" type="info">{{ getCategoryName(row.categoryId) }}</el-tag>
                <el-tag v-if="row.promoStartTime" size="small" type="danger" effect="dark" style="margin-top: 4px;">限时秒杀中</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="基础单价" width="100" align="center">
          <template #default="{ row }"><span class="price-text">¥ {{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="基础库存" width="90" align="center">
          <template #default="{ row }"><el-tag :type="row.stock < 10 ? 'danger' : 'success'" effect="light" round>{{ row.stock }}</el-tag></template>
        </el-table-column>
        <el-table-column label="上架状态" width="100" align="center">
          <template #default="{ row }"><el-switch v-model="row.status" :active-value="1" :inactive-value="0" active-color="#10b981" @change="handleStatusChange(row)" /></template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }"> 
            <el-button type="danger" link size="small" :icon="Clock" @click="openPromoDialog(row)">秒杀</el-button>
            <el-button type="success" link size="small" :icon="Operation" @click="openSkuDrawer(row)">规格/补货</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" link size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="promoDialogVisible" title="🔥 设置限时秒杀活动" width="500px" class="glass-dialog">
      <el-form label-width="100px" class="rich-form">
        <el-form-item label="秒杀特价" required>
          <el-input-number v-model="promoForm.promoPrice" :min="0.01" :precision="2" style="width: 100%;" />
          <div style="font-size: 12px; color: #94a3b8; line-height: 1.2; margin-top: 5px;">
            设置后，活动期间该商品所有规格均强制覆盖为此特价。
          </div>
        </el-form-item>
        <el-form-item label="起止时间" required>
          <el-date-picker
            v-model="promoForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearPromo" type="danger" plain round style="float: left;">取消此活动</el-button>
        <el-button @click="promoDialogVisible = false" round>取消</el-button>
        <el-button type="primary" @click="submitPromoForm" :loading="submitLoading" round>保存秒杀</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="skuDrawerVisible" :title="`📦 [ ${activeProduct?.name} ] 规格管理`" size="55%" class="cute-drawer">
      <div class="drawer-toolbar"><el-button type="primary" :icon="Plus" round @click="handleAddSku">添加新规格</el-button></div>
      <el-table :data="skuList" v-loading="skuLoading" stripe border size="small" class="sku-table">
        <el-table-column prop="specName" label="规格名称" min-width="160" />
        <el-table-column prop="price" label="价格" width="110" align="center" />
        <el-table-column prop="stock" label="库存" width="100" align="center" />
        <el-table-column label="操作" width="140" align="center"><template #default="{ row }"><el-button type="primary" link size="small" @click="handleEditSku(row)">改价/补货</el-button></template></el-table-column>
      </el-table>
    </el-drawer>
    
    <el-dialog v-model="skuDialogVisible" title="管理规格" width="450px" class="glass-dialog">
      <el-form :model="skuForm" label-width="90px">
        <el-form-item label="规格名称"><el-input v-model="skuForm.specName" /></el-form-item>
        <el-form-item label="专属单价"><el-input-number v-model="skuForm.price" style="width: 100%;" /></el-form-item>
        <el-form-item label="补货库存"><el-input-number v-model="skuForm.stock" style="width: 100%;" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="submitSkuForm" type="primary">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="商品信息" width="550px" class="glass-dialog">
      <el-form :model="productForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="productForm.name"/></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="productForm.price"/></el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="productForm.stock" disabled />
          <div style="font-size: 12px; color: #94a3b8; line-height: 1.2; margin-top: 5px;">
            系统已开启库存强一致性联动。总库存由 [规格/补货] 自动求和计算，禁止手动修改。
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="handleSubmit" type="primary">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-container { height: 100%; }
.glass-card { border-radius: 12px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(10px); box-shadow: 0 4px 16px rgba(186, 230, 253, 0.2); }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 20px; }
.filter-group { display: flex; gap: 15px; }
.search-input { width: 280px; }
.filter-select { width: 150px; }
.cute-btn { background-color: #0ea5e9; border: none; font-weight: bold; }
:deep(.custom-table th.el-table__cell) { background-color: #f0f9ff; color: #0369a1; border-bottom: none; }
.price-text { color: #f43f5e; font-weight: 900; font-size: 16px; }
.product-cell { display: flex; align-items: center; gap: 15px; }
.p-img { width: 55px; height: 55px; border-radius: 10px; border: 1px solid #e2e8f0; }
.p-info { display: flex; flex-direction: column; align-items: flex-start; gap: 5px; }
.p-name { font-weight: bold; color: #1e293b; line-height: 1.3; }
:deep(.glass-dialog) { border-radius: 16px; background: rgba(255, 255, 255, 0.95); backdrop-filter: blur(15px); border: 1px solid rgba(186, 230, 253, 0.6); }
:deep(.glass-dialog .el-dialog__header) { border-bottom: 1px solid #e0f2fe; padding: 20px; font-weight: bold; color: #0369a1; }
</style>
