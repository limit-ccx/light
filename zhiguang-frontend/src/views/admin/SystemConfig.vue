<template>
  <div class="admin-system-config">
    <h2 class="page-title">系统配置</h2>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="系统配置" name="configs">
        <el-form v-loading="configLoading" label-width="180px" class="config-form">
          <el-form-item v-for="item in configs" :key="item.key" :label="item.description || item.key">
            <el-input v-model="item.value" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveConfigs">保存配置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="公告管理" name="announcements">
        <div class="filter-bar">
          <el-button type="primary" @click="openAnnouncementDialog()">新增公告</el-button>
        </div>
        <el-table :data="announcements" v-loading="announcementLoading" empty-text="暂无公告">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="160" />
          <el-table-column label="操作" class-name="col-actions" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openAnnouncementDialog(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="deleteAnnouncementItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="announcementPage.page"
            v-model:page-size="announcementPage.size"
            :total="announcementPage.total"
            layout="total, prev, pager, next"
            @current-change="fetchAnnouncements"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="Banner 管理" name="banners">
        <div class="filter-bar">
          <el-button type="primary" @click="openBannerDialog()">新增 Banner</el-button>
        </div>
        <el-table :data="banners" v-loading="bannerLoading" empty-text="暂无 Banner">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="160" />
          <el-table-column prop="imageUrl" label="图片链接" min-width="200" show-overflow-tooltip />
          <el-table-column prop="linkUrl" label="跳转链接" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-switch
                v-model="row.status"
                active-value="ENABLED"
                inactive-value="DISABLED"
                @change="(val) => toggleBannerStatus(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="100" />
          <el-table-column label="操作" class-name="col-actions" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openBannerDialog(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="deleteBannerItem(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="bannerPage.page"
            v-model:page-size="bannerPage.size"
            :total="bannerPage.total"
            layout="total, prev, pager, next"
            @current-change="fetchBanners"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="announcementDialogVisible" class="admin-dialog" title="公告" width="600px">
      <el-form :model="announcementForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="announcementForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="announcementForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="announcementForm.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAnnouncement">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bannerDialogVisible" class="admin-dialog" title="Banner" width="600px">
      <el-form :model="bannerForm" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="bannerForm.title" />
        </el-form-item>
        <el-form-item label="图片链接">
          <el-input v-model="bannerForm.imageUrl" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="bannerForm.linkUrl" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="bannerForm.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBanner">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminMessage as ElMessage } from '@/utils/adminMessage'
import { adminConfirm } from '@/utils/adminConfirm'
import {
  getAllConfigs, batchUpdateConfigs, getAnnouncements, createAnnouncement,
  updateAnnouncement, deleteAnnouncement, getBanners, createBanner, updateBanner,
  updateBannerStatus, deleteBanner
} from '@/api/admin'

const activeTab = ref('configs')

const configs = ref([])
const configLoading = ref(false)

const announcements = ref([])
const announcementLoading = ref(false)
const announcementPage = reactive({ page: 1, size: 10, total: 0 })
const announcementDialogVisible = ref(false)
const announcementForm = reactive({ id: null, title: '', content: '', status: 'ENABLED' })

const banners = ref([])
const bannerLoading = ref(false)
const bannerPage = reactive({ page: 1, size: 10, total: 0 })
const bannerDialogVisible = ref(false)
const bannerForm = reactive({ id: null, title: '', imageUrl: '', linkUrl: '', sortOrder: 0, status: 'ENABLED' })

const fetchConfigs = async () => {
  configLoading.value = true
  try {
    const data = await getAllConfigs()
    configs.value = data || []
  } catch (error) {
    ElMessage.error('获取系统配置失败')
  } finally {
    configLoading.value = false
  }
}

const saveConfigs = async () => {
  try {
    // 将数组转换为后端期望的 key-value Map 格式
    const configMap = {}
    configs.value.forEach(item => {
      configMap[item.key] = item.value
    })
    await batchUpdateConfigs(configMap)
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const fetchAnnouncements = async () => {
  announcementLoading.value = true
  try {
    const data = await getAnnouncements({
      page: announcementPage.page - 1,
      size: announcementPage.size
    })
    announcements.value = data.list || []
    announcementPage.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取公告失败')
  } finally {
    announcementLoading.value = false
  }
}

const openAnnouncementDialog = (row = null) => {
  if (row) {
    Object.assign(announcementForm, row)
  } else {
    announcementForm.id = null
    announcementForm.title = ''
    announcementForm.content = ''
    announcementForm.status = 'ENABLED'
  }
  announcementDialogVisible.value = true
}

const saveAnnouncement = async () => {
  try {
    if (announcementForm.id) {
      await updateAnnouncement(announcementForm.id, announcementForm)
    } else {
      await createAnnouncement(announcementForm)
    }
    ElMessage.success('保存成功')
    announcementDialogVisible.value = false
    fetchAnnouncements()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const deleteAnnouncementItem = async (row) => {
  try {
    await adminConfirm('确定删除该公告吗？', '提示', { type: 'warning' })
    await deleteAnnouncement(row.id)
    ElMessage.success('删除成功')
    fetchAnnouncements()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const fetchBanners = async () => {
  bannerLoading.value = true
  try {
    const data = await getBanners({
      page: bannerPage.page - 1,
      size: bannerPage.size
    })
    banners.value = data.list || []
    bannerPage.total = data.total || 0
  } catch (error) {
    ElMessage.error('获取 Banner 失败')
  } finally {
    bannerLoading.value = false
  }
}

const openBannerDialog = (row = null) => {
  if (row) {
    Object.assign(bannerForm, row)
  } else {
    bannerForm.id = null
    bannerForm.title = ''
    bannerForm.imageUrl = ''
    bannerForm.linkUrl = ''
    bannerForm.sortOrder = 0
    bannerForm.status = 'ENABLED'
  }
  bannerDialogVisible.value = true
}

const saveBanner = async () => {
  try {
    if (bannerForm.id) {
      await updateBanner(bannerForm.id, bannerForm)
    } else {
      await createBanner(bannerForm)
    }
    ElMessage.success('保存成功')
    bannerDialogVisible.value = false
    fetchBanners()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const toggleBannerStatus = async (row, status) => {
  try {
    await updateBannerStatus(row.id, status)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('状态更新失败')
    fetchBanners()
  }
}

const deleteBannerItem = async (row) => {
  try {
    await adminConfirm('确定删除该 Banner 吗？', '提示', { type: 'warning' })
    await deleteBanner(row.id)
    ElMessage.success('删除成功')
    fetchBanners()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  fetchConfigs()
  fetchAnnouncements()
  fetchBanners()
})
</script>

<style lang="scss" scoped>
.page-title {
  margin: 0 0 var(--admin-space-6);
  font-size: var(--admin-text-xl);
  font-weight: var(--admin-font-semibold);
  color: var(--admin-text);
  letter-spacing: -0.01em;
}

.config-form {
  max-width: 640px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
