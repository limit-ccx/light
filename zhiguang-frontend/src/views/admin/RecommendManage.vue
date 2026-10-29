<template>
  <div class="admin-recommend-manage">
    <h2 class="page-title">内容推荐</h2>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>新增推荐</span>
        </div>
      </template>
      <el-form :model="form" inline>
        <el-form-item label="推荐类型">
          <el-select v-model="form.targetType" popper-class="admin-select-popper" placeholder="选择类型" style="width: 160px">
            <el-option label="文章" value="ARTICLE" />
            <el-option label="专栏" value="COLUMN" />
            <el-option label="合集" value="COLLECTION" />
            <el-option label="用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID">
          <el-input-number v-model="form.targetId" :min="1" placeholder="目标ID" style="width: 160px" />
        </el-form-item>
        <el-form-item label="展示位置">
          <el-input v-model="form.position" placeholder="如 HOME" style="width: 160px" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleAdd">新增推荐</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <span>已推荐内容</span>
      </template>
      <el-table :data="featuredList" v-loading="loading" empty-text="暂无推荐内容">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="targetType" label="类型" width="120" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column prop="position" label="位置" width="140" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="createdAt" label="推荐时间" min-width="160" />
        <el-table-column label="操作" class-name="col-actions" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleRemove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchFeatured"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { adminConfirm } from '@/utils/adminConfirm'
import { getFeaturedContent, addFeaturedContent, removeFeaturedContent } from '@/api/admin'

const loading = ref(false)
const featuredList = ref([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const form = reactive({
  targetType: 'ARTICLE',
  targetId: undefined,
  position: 'HOME',
  sortOrder: 0
})

const fetchFeatured = async () => {
  loading.value = true
  try {
    const data = await getFeaturedContent({
      page: pagination.page - 1,
      size: pagination.size
    })
    featuredList.value = data.list || []
    pagination.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取推荐内容失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = async () => {
  if (!form.targetId || !form.position) return ElMessage.warning('请填写完整信息')
  try {
    await addFeaturedContent({
      targetType: form.targetType,
      targetId: form.targetId,
      position: form.position,
      sortOrder: form.sortOrder
    })
    ElMessage.success('新增推荐成功')
    form.targetId = undefined
    form.sortOrder = 0
    fetchFeatured()
  } catch (error) {
    ElMessage.error('新增推荐失败')
  }
}

const handleRemove = async (row) => {
  try {
    await adminConfirm('确定移除该推荐内容吗？', '提示', { type: 'warning' })
    await removeFeaturedContent(row.id)
    ElMessage.success('移除成功')
    fetchFeatured()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('移除失败')
  }
}

onMounted(fetchFeatured)
</script>

<style lang="scss" scoped>
.page-title {
  margin: 0 0 var(--admin-space-6);
  font-size: var(--admin-text-xl);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  letter-spacing: -0.01em;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-card {
  margin-top: 20px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
