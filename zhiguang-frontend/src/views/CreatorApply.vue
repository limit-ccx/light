<template>
  <div class="creator-apply-page">
    <Header />

    <main class="main-content">
      <div class="container">
        <div class="page-header">
          <h1 class="page-title">创作者认证</h1>
          <p class="page-subtitle">提交资料与作品，通过审核后即可获得认证标识</p>
        </div>

        <div class="apply-layout">
          <div class="apply-card">
            <h2 class="card-title">填写认证申请</h2>
            <div class="apply-form">
              <div class="form-group">
                <label class="form-label">认证方向 <span class="required">*</span></label>
                <el-select
                  v-model="form.direction"
                  placeholder="请选择认证方向"
                  class="direction-select"
                  size="large"
                >
                  <el-option
                    v-for="item in directions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </div>

              <div class="form-group">
                <label class="form-label">个人简介</label>
                <textarea
                  v-model="form.bio"
                  class="textarea-field"
                  rows="5"
                  placeholder="介绍一下你的专业背景、擅长领域..."
                ></textarea>
              </div>

              <div class="form-group">
                <label class="form-label">作品链接 <span class="required">*</span></label>
                <div class="work-links">
                  <div v-for="(work, index) in form.works" :key="index" class="work-link-item">
                    <input
                      v-model="form.works[index]"
                      type="text"
                      class="input-field"
                      placeholder="https://..."
                    />
                    <button class="btn-icon btn-danger-ghost" @click="removeWork(index)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M18 6L6 18M6 6l12 12"/>
                      </svg>
                    </button>
                  </div>
                  <button class="btn-add-work" @click="addWork" :disabled="form.works.length >= 10">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 5v14M5 12h14"/>
                    </svg>
                    添加作品链接
                  </button>
                </div>
              </div>

              <div class="form-actions">
                <button class="btn-primary" @click="submitApply" :disabled="submitting">
                  {{ submitting ? '提交中...' : '提交申请' }}
                </button>
              </div>
            </div>
          </div>

          <div class="status-card" v-if="myApplication">
            <h2 class="card-title">申请状态</h2>
            <div class="status-body">
              <div class="status-row">
                <span class="status-label">认证方向</span>
                <span class="status-value">{{ myApplication.direction }}</span>
              </div>
              <div class="status-row">
                <span class="status-label">当前状态</span>
                <span class="status-badge" :class="statusClass">{{ statusText }}</span>
              </div>
              <div class="status-row" v-if="myApplication.reviewReason">
                <span class="status-label">审核说明</span>
                <span class="status-value">{{ myApplication.reviewReason }}</span>
              </div>
              <div class="status-row">
                <span class="status-label">提交时间</span>
                <span class="status-value">{{ formatTime(myApplication.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import { applyCreator, getMyCreatorApplication } from '@/api/creator'
import { ElMessage } from 'element-plus'

const directions = [
  'Java 工程师',
  '产品经理',
  '测试工程师',
  'AI 开发者',
  '前端工程师',
  '后端工程师'
]

const form = reactive({
  direction: '',
  bio: '',
  works: ['']
})

const myApplication = ref(null)
const submitting = ref(false)

const statusText = computed(() => {
  const map = {
    PENDING: '审核中',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[myApplication.value?.status] || myApplication.value?.status
})

const statusClass = computed(() => {
  const map = {
    PENDING: 'pending',
    APPROVED: 'approved',
    REJECTED: 'rejected'
  }
  return map[myApplication.value?.status] || ''
})

const addWork = () => {
  if (form.works.length < 10) {
    form.works.push('')
  }
}

const removeWork = (index) => {
  form.works.splice(index, 1)
  if (form.works.length === 0) {
    form.works.push('')
  }
}

const submitApply = async () => {
  if (!form.direction) {
    ElMessage.warning('请选择认证方向')
    return
  }
  const validWorks = form.works.filter(w => w.trim())
  if (validWorks.length === 0) {
    ElMessage.warning('请至少填写一个作品链接')
    return
  }

  submitting.value = true
  try {
    const response = await applyCreator({
      direction: form.direction,
      bio: form.bio,
      works: validWorks
    })
    ElMessage.success(response?.message || '提交成功')
    myApplication.value = response?.data || response
    resetForm()
  } catch (error) {
    console.error('提交认证申请失败:', error)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.direction = ''
  form.bio = ''
  form.works = ['']
}

const loadMyApplication = async () => {
  try {
    const response = await getMyCreatorApplication()
    myApplication.value = response
  } catch (error) {
    console.error('加载申请状态失败:', error)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  loadMyApplication()
})
</script>

<style lang="scss" scoped>
.creator-apply-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 40px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
}

.apply-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
  align-items: start;
}

.apply-card,
.status-card {
  @include card-static;
  padding: 28px;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--color-text);

  .required {
    color: var(--color-error);
    margin-left: 2px;
  }
}

.direction-select {
  width: 100%;
}

.work-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.work-link-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.btn-add-work {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: transparent;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);

  &:hover:not(:disabled) {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.btn-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  background: transparent;
  border: none;
}

.btn-danger-ghost {
  color: var(--color-text-secondary);

  &:hover {
    background: var(--color-google-red-light);
    color: var(--color-google-red);
  }
}

.status-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
}

.status-label {
  color: var(--color-text-secondary);
}

.status-value {
  color: var(--color-text);
  font-weight: 500;
}

.status-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  font-size: 13px;
  font-weight: 500;

  &.pending {
    background: var(--color-google-yellow-light);
    color: #b06000;
  }

  &.approved {
    background: var(--color-google-green-light);
    color: var(--color-google-green);
  }

  &.rejected {
    background: var(--color-google-red-light);
    color: var(--color-google-red);
  }
}

@media (max-width: 900px) {
  .apply-layout {
    grid-template-columns: 1fr;
  }
}
</style>
