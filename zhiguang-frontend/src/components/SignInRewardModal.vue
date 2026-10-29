<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="360px"
    :show-close="false"
    :close-on-click-modal="false"
    center
    class="sign-in-reward-modal"
  >
    <div class="reward-content">
      <div class="reward-icon">
        <span v-if="isSupplement">🔄</span>
        <span v-else>🎉</span>
      </div>
      <div class="reward-title">
        {{ isSupplement ? '补签成功' : '签到成功' }}
      </div>
      <div class="reward-date">
        {{ formatDate(signDate) }}
      </div>

      <div class="reward-stats">
        <div class="reward-stat">
          <div class="reward-value points">+{{ rewardPoints }}</div>
          <div class="reward-label">积分</div>
        </div>
        <div class="reward-stat">
          <div class="reward-value experience">+{{ rewardExperience }}</div>
          <div class="reward-label">经验</div>
        </div>
        <div class="reward-stat" v-if="continuousDays > 0">
          <div class="reward-value days">{{ continuousDays }}</div>
          <div class="reward-label">连续天数</div>
        </div>
      </div>

      <div class="reward-total">
        <span>当前积分: <strong>{{ totalPoints }}</strong></span>
        <span>当前经验: <strong>{{ totalExperience }}</strong></span>
      </div>

      <div v-if="!isSupplement && continuousDays >= 7" class="reward-bonus">
        🔥 连续签到 {{ continuousDays }} 天，获得额外奖励！
      </div>
    </div>

    <template #footer>
      <div class="reward-footer">
        <el-button type="primary" round @click="handleClose">知道了</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  result: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'close'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const isSupplement = computed(() => props.result?.supplement || false)
const title = computed(() => isSupplement.value ? '补签成功' : '签到成功')
const signDate = computed(() => props.result?.signDate)
const rewardPoints = computed(() => props.result?.rewardPoints || 0)
const rewardExperience = computed(() => props.result?.rewardExperience || 0)
const continuousDays = computed(() => props.result?.continuousDays || 0)
const totalPoints = computed(() => props.result?.totalPoints || 0)
const totalExperience = computed(() => props.result?.totalExperience || 0)

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

function handleClose() {
  visible.value = false
  emit('close')
}
</script>

<style lang="scss" scoped>
.reward-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 0;
}

.reward-icon {
  font-size: 56px;
  margin-bottom: 12px;
  animation: bounce 0.6s ease;
}

.reward-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text, #202124);
  margin-bottom: 6px;
}

.reward-date {
  font-size: 14px;
  color: var(--color-text-secondary, #5f6368);
  margin-bottom: 24px;
}

.reward-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  width: 100%;
  justify-content: center;
}

.reward-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 72px;
  padding: 12px 16px;
  border-radius: 12px;
  background: var(--color-bg-soft, #f8f9fa);
}

.reward-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;

  &.points {
    color: #f59e0b;
  }

  &.experience {
    color: var(--color-primary, #1a73e8);
  }

  &.days {
    color: #34a853;
  }
}

.reward-label {
  font-size: 12px;
  color: var(--color-text-secondary, #5f6368);
}

.reward-total {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--color-text-secondary, #5f6368);
  margin-bottom: 12px;

  strong {
    color: var(--color-text, #202124);
    font-weight: 600;
  }
}

.reward-bonus {
  padding: 10px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(245,158,11,0.15), rgba(245,158,11,0.05));
  color: #d97706;
  font-size: 13px;
  font-weight: 500;
}

.reward-footer {
  width: 100%;
}

@keyframes bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}
</style>
