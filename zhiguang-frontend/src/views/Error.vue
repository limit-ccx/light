<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-code">{{ errorCode }}</div>
      <h1 class="error-title">{{ errorTitle }}</h1>
      <p class="error-desc">{{ errorDesc }}</p>
      <div class="error-actions">
        <router-link to="/home" class="btn-primary">返回首页</router-link>
        <button class="btn-secondary" @click="goBack">返回上页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const errorCode = computed(() => route.meta.code || 404)
const errorTitle = computed(() => {
  switch (errorCode.value) {
    case 403: return '禁止访问'
    case 500: return '服务器错误'
    default: return '页面不存在'
  }
})
const errorDesc = computed(() => {
  switch (errorCode.value) {
    case 403: return '抱歉，您没有权限访问此页面'
    case 500: return '抱歉，服务器出了点问题，请稍后重试'
    default: return '抱歉，您访问的页面不存在或已被删除'
  }
})

const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.error-content {
  text-align: center;
  padding: 40px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: var(--color-primary);
  opacity: 0.2;
  line-height: 1;
  margin-bottom: -20px;
}

.error-title {
  font-size: 32px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.error-desc {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.error-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}
</style>