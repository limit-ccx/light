<template>
  <div class="sign-in-page">
    <Header />

    <main class="main-content">
      <div class="container">
        <div class="sign-in-header">
          <h1 class="page-title">每日签到</h1>
          <p class="page-subtitle">坚持签到，领取积分与经验奖励</p>
        </div>

        <div class="sign-in-grid">
          <!-- 左侧：签到日历 -->
          <div class="calendar-section">
            <SignInCalendar
              ref="calendarRef"
              :initial-year="currentYear"
              :initial-month="currentMonth"
              @month-change="handleMonthChange"
              @day-click="handleDayClick"
            />
          </div>

          <!-- 右侧：用户状态与操作 -->
          <div class="status-section">
            <!-- 用户信息卡片 -->
            <div class="status-card user-card">
              <div class="user-info">
                <div class="user-avatar">{{ userInitial }}</div>
                <div class="user-meta">
                  <div class="user-name">{{ userName }}</div>
                  <div class="user-level">Lv.{{ levelInfo.level }}</div>
                </div>
              </div>
              <div class="exp-info">
                <div class="exp-text">{{ levelInfo.experience }} / {{ levelInfo.nextLevelExperience }} XP</div>
                <div class="exp-bar">
                  <div class="exp-progress" :style="{ width: levelProgress + '%' }"></div>
                </div>
              </div>
            </div>

            <!-- 积分卡片 -->
            <div class="status-card points-card">
              <div class="status-icon">💎</div>
              <div class="status-content">
                <div class="status-value">{{ pointInfo.points }}</div>
                <div class="status-label">当前积分</div>
              </div>
            </div>

            <!-- 签到按钮 -->
            <div class="status-card action-card">
              <button
                class="btn-sign-in-large"
                :class="{ signed: signInStatus.signedInToday, loading: signing }"
                :disabled="signInStatus.signedInToday || signing"
                @click="handleSignIn"
              >
                <span class="btn-icon">{{ signInStatus.signedInToday ? '✓' : '📅' }}</span>
                <span class="btn-text">
                  {{ signInStatus.signedInToday ? '今日已签到' : (signing ? '签到中...' : '立即签到') }}
                </span>
              </button>
              <div class="action-tip">
                连续签到 <strong>{{ signInStatus.continuousDays || 0 }}</strong> 天
                <span v-if="signInStatus.supplementRemaining > 0" class="supplement-tip">
                  · 剩余补签 {{ signInStatus.supplementRemaining }} 次
                </span>
              </div>
            </div>

            <!-- 奖励规则 -->
            <div class="status-card rule-card">
              <h3 class="rule-title">奖励规则</h3>
              <ul class="rule-list">
                <li>每日签到可获得基础积分和经验</li>
                <li>连续签到天数越多，奖励越高</li>
                <li>连续签到达到 7 天可获得额外成就奖励</li>
                <li>每月最多可补签 3 次，仅限本月内日期</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </main>

    <SignInRewardModal
      v-model="rewardModalVisible"
      :result="rewardResult"
      @close="handleRewardClose"
    />

    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '@/store/auth'
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import SignInCalendar from '@/components/SignInCalendar.vue'
import SignInRewardModal from '@/components/SignInRewardModal.vue'
import {
  getLevel,
  getPoints,
  getSignInCalendar,
  getSignInStatus,
  signIn,
  supplementSignIn
} from '@/api/growth'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const calendarRef = ref(null)

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)

const signing = ref(false)
const rewardModalVisible = ref(false)
const rewardResult = ref({})

const levelInfo = reactive({
  level: 1,
  experience: 0,
  nextLevelExperience: 100
})

const pointInfo = reactive({
  points: 0,
  totalPoints: 0
})

const signInStatus = reactive({
  signedInToday: false,
  continuousDays: 0,
  supplementRemaining: 0,
  supplementUsed: 0,
  supplementMax: 3
})

const userName = computed(() => authStore.user?.username || '用户')
const userInitial = computed(() => userName.value.charAt(0).toUpperCase())

const levelProgress = computed(() => {
  if (levelInfo.nextLevelExperience <= 0) return 0
  const progress = (levelInfo.experience / levelInfo.nextLevelExperience) * 100
  return Math.min(100, Math.max(0, progress))
})

const loadLevel = async () => {
  try {
    const data = await getLevel()
    Object.assign(levelInfo, {
      level: data.level || 1,
      experience: data.experience || 0,
      nextLevelExperience: data.nextLevelExperience || 100
    })
  } catch (error) {
    console.error('加载等级失败:', error)
  }
}

const loadPoints = async () => {
  try {
    const data = await getPoints()
    Object.assign(pointInfo, {
      points: data.points || 0,
      totalPoints: data.totalPoints || 0
    })
  } catch (error) {
    console.error('加载积分失败:', error)
  }
}

const loadSignInStatus = async () => {
  try {
    const data = await getSignInStatus()
    Object.assign(signInStatus, {
      signedInToday: data.signedInToday || false,
      continuousDays: data.continuousDays || 0,
      supplementRemaining: data.supplementRemaining || 0,
      supplementUsed: data.supplementUsed || 0,
      supplementMax: data.supplementMax || 3
    })
  } catch (error) {
    console.error('加载签到状态失败:', error)
  }
}

const loadCalendar = async (year, month) => {
  try {
    const data = await getSignInCalendar({ year, month })
    if (calendarRef.value) {
      calendarRef.value.updateCalendar(data)
    }
  } catch (error) {
    console.error('加载日历失败:', error)
    ElMessage.error('加载日历失败')
  }
}

const handleMonthChange = ({ year, month }) => {
  currentYear.value = year
  currentMonth.value = month
  loadCalendar(year, month)
}

const handleDayClick = async (day) => {
  if (!day.date) return

  if (day.canSign) {
    await handleSignIn()
    return
  }

  if (day.canSupplement) {
    if (signInStatus.supplementRemaining <= 0) {
      ElMessage.warning('本月补签次数已用完')
      return
    }

    try {
      await ElMessageBox.confirm(
        `确认消耗 1 次补签机会补签 ${formatDate(day.date)} 吗？\n本月剩余补签次数：${signInStatus.supplementRemaining} 次`,
        '确认补签',
        {
          confirmButtonText: '确认补签',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      await doSupplementSignIn(formatDateForApi(day.date))
    } catch (error) {
      if (error !== 'cancel') {
        console.error('补签失败:', error)
      }
    }
  }
}

const handleSignIn = async () => {
  if (signInStatus.signedInToday || signing.value) return
  signing.value = true
  try {
    const result = await signIn()
    rewardResult.value = result
    rewardModalVisible.value = true
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '签到失败')
  } finally {
    signing.value = false
  }
}

const doSupplementSignIn = async (date) => {
  signing.value = true
  try {
    const result = await supplementSignIn(date)
    rewardResult.value = result
    rewardModalVisible.value = true
    await refreshAll()
    ElMessage.success('补签成功')
  } catch (error) {
    ElMessage.error(error.message || '补签失败')
  } finally {
    signing.value = false
  }
}

const handleRewardClose = () => {
  rewardModalVisible.value = false
}

const refreshAll = async () => {
  await Promise.all([
    loadLevel(),
    loadPoints(),
    loadSignInStatus(),
    loadCalendar(currentYear.value, currentMonth.value)
  ])
}

function formatDate(date) {
  const d = new Date(date)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function formatDateForApi(date) {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

onMounted(() => {
  refreshAll()
})
</script>

<style lang="scss" scoped>
@use "@/styles/tech-button-mixins.scss" as *;
.sign-in-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 40px 0;
}

.sign-in-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--color-text), var(--color-primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
}

.sign-in-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 24px;
}

.calendar-section,
.status-section {
  min-width: 0;
}

.status-card {
  background: var(--color-card, #fff);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.08));
}

.user-card {
  .user-info {
    display: flex;
    align-items: center;
    gap: 14px;
    margin-bottom: 16px;
  }

  .user-avatar {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
    color: white;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    font-weight: 600;
  }

  .user-meta {
    flex: 1;
  }

  .user-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text);
    margin-bottom: 4px;
  }

  .user-level {
    font-size: 13px;
    color: var(--color-text-secondary);
  }

  .exp-info {
    .exp-text {
      font-size: 13px;
      color: var(--color-text-secondary);
      margin-bottom: 8px;
      display: flex;
      justify-content: space-between;
    }

    .exp-bar {
      height: 8px;
      background: var(--color-bg-soft);
      border-radius: 999px;
      overflow: hidden;
    }

    .exp-progress {
      height: 100%;
      background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
      border-radius: 999px;
      transition: width 0.4s ease;
    }
  }
}

.points-card {
  display: flex;
  align-items: center;
  gap: 16px;

  .status-icon {
    font-size: 32px;
  }

  .status-value {
    font-size: 28px;
    font-weight: 700;
    color: var(--color-text);
    line-height: 1;
  }

  .status-label {
    font-size: 13px;
    color: var(--color-text-secondary);
  }
}

.action-card {
  text-align: center;

  .btn-sign-in-large {
    @include tech-button(52px, 32px, 999px);
    width: 100%;
    font-size: 16px;
    margin-bottom: 12px;

    &.signed {
      opacity: 0.9;
      filter: saturate(0.9);
    }

    &.loading {
      opacity: 0.8;
    }
  }

  .btn-icon {
    font-size: 20px;
  }

  .action-tip {
    font-size: 13px;
    color: var(--color-text-secondary);

    strong {
      color: var(--color-primary);
      font-weight: 600;
    }
  }

  .supplement-tip {
    color: #f59e0b;
  }
}

.rule-card {
  .rule-title {
    font-size: 15px;
    font-weight: 600;
    color: var(--color-text);
    margin-bottom: 12px;
  }

  .rule-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      font-size: 13px;
      color: var(--color-text-secondary);
      padding: 6px 0;
      padding-left: 16px;
      position: relative;

      &::before {
        content: '•';
        position: absolute;
        left: 0;
        color: var(--color-primary);
        font-weight: 700;
      }
    }
  }
}

@media (max-width: 992px) {
  .sign-in-grid {
    grid-template-columns: 1fr;
  }

  .status-section {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;

    .status-card {
      margin-bottom: 0;
    }

    .user-card,
    .action-card,
    .rule-card {
      grid-column: span 2;
    }
  }
}

@media (max-width: 576px) {
  .status-section {
    grid-template-columns: 1fr;

    .user-card,
    .action-card,
    .rule-card {
      grid-column: span 1;
    }
  }

  .page-title {
    font-size: 26px;
  }
}
</style>
