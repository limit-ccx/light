<template>
  <InfoLayout
    eyebrow="Feedback"
    title="建议反馈"
    description="你的每一条建议，都会让纸域空间变得更好。"
    :particles="false"
  >
    <template #meta>
      <span class="meta-chip">Bug 反馈</span>
      <span class="meta-chip">功能建议</span>
      <span class="meta-chip">内容投诉</span>
    </template>

    <div class="ln-panel ln-panel-pad feedback-panel">
      <el-form :model="form" label-position="top" class="feedback-form">
        <el-form-item label="反馈类型">
          <el-radio-group v-model="form.type">
            <el-radio label="bug">Bug 反馈</el-radio>
            <el-radio label="feature">功能建议</el-radio>
            <el-radio label="content">内容投诉</el-radio>
            <el-radio label="other">其他</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请简要描述你的反馈" />
        </el-form-item>

        <el-form-item label="详细描述">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="请尽可能详细地描述问题或建议，必要时可附上复现步骤"
          />
        </el-form-item>

        <el-form-item label="联系邮箱（选填）">
          <el-input v-model="form.email" placeholder="方便我们回复你" />
        </el-form-item>

        <el-form-item class="form-submit">
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交反馈
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </InfoLayout>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import InfoLayout from '@/components/content/InfoLayout.vue'
import { submitFeedback } from '@/api/feedback'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  type: 'bug',
  title: '',
  content: '',
  email: ''
})

const submitting = ref(false)

const handleSubmit = async () => {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning('请填写标题和详细描述')
    return
  }
  if (!authStore.isLoggedIn) {
    ElMessage.warning('登录后才能提交反馈')
    router.push({ name: 'Login', query: { redirect: '/feedback' } })
    return
  }

  submitting.value = true
  try {
    await submitFeedback({
      type: form.type,
      title: form.title.trim(),
      content: form.content.trim(),
      contact: form.email.trim()
    })
    ElMessage.success('反馈已提交，感谢你的建议')
    form.title = ''
    form.content = ''
    form.email = ''
  } catch {
    ElMessage.error('提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.meta-chip {
  padding: 4px 11px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-3);
  font-size: 12px;
}

.feedback-panel {
  width: 100%;
  max-width: 680px;
  margin: 0 auto;
}

.feedback-form {
  :deep(.el-form-item__label) {
    color: var(--content-ink);
    font-weight: 500;
    padding-bottom: 6px;
  }

  :deep(.el-input__wrapper),
  :deep(.el-textarea__inner) {
    background: var(--content-surface-tint);
    box-shadow: 0 0 0 1px var(--content-chip-line) inset;
    border-radius: 12px;
    padding: 6px 14px;
  }

  :deep(.el-input__wrapper.is-focus),
  :deep(.el-textarea__inner:focus) {
    box-shadow: 0 0 0 1px var(--content-line-strong) inset;
  }

  :deep(.el-radio__label) {
    color: var(--content-ink-2);
  }

  :deep(.el-form-item) {
    margin-bottom: 22px;
  }
}

.form-submit {
  margin-bottom: 0 !important;

  :deep(.el-button) {
    min-width: 132px;
    height: 42px;
    border-radius: 12px;
    border: none;
    background: linear-gradient(120deg, #1a73e8, #5b6cf5 60%, #8b5cf6);
    box-shadow: 0 10px 24px rgba(47, 99, 235, 0.28);
  }
}
</style>
