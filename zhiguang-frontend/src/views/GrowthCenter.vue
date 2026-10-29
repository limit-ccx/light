<template>
  <div class="growth-page">
    <Header />

    <main class="main-content">
      <div class="container">
        <div class="growth-header">
          <h1 class="page-title">成长中心</h1>
          <p class="page-subtitle">坚持创作与互动，解锁更多成就</p>
        </div>

        <div class="growth-grid">
          <!-- 等级卡片 -->
          <div class="growth-card level-card">
            <div class="card-header">
              <span class="card-icon">🏆</span>
              <span class="card-title">当前等级</span>
            </div>
            <div class="level-display">
              <div class="level-badge">Lv.{{ levelInfo.level }}</div>
              <div class="level-meta">
                <div class="exp-text">
                  {{ levelInfo.experience }} / {{ levelInfo.nextLevelExperience }} XP
                </div>
                <div class="exp-bar">
                  <div
                    class="exp-progress"
                    :style="{ width: levelProgress + '%' }"
                  ></div>
                </div>
              </div>
            </div>
          </div>

          <!-- 积分卡片 -->
          <div class="growth-card points-card">
            <div class="card-header">
              <span class="card-icon">💎</span>
              <span class="card-title">积分余额</span>
            </div>
            <div class="points-display">
              <span class="points-value">{{ pointInfo.points }}</span>
              <span class="points-label">累计获得 {{ pointInfo.totalPoints }}</span>
            </div>
          </div>

          <!-- 签到卡片 -->
          <div class="growth-card sign-in-card">
            <div class="card-header">
              <span class="card-icon">📅</span>
              <span class="card-title">每日签到</span>
            </div>
            <div class="sign-in-body">
              <div class="continuous-days">
                <span class="days-value">{{ signInStatus.continuousDays }}</span>
                <span class="days-label">连续签到天数</span>
              </div>
              <button
                class="btn-sign-in"
                :class="{ signed: signInStatus.signedInToday, loading: signing }"
                :disabled="signInStatus.signedInToday || signing"
                @click="handleSignIn"
              >
                {{ signInStatus.signedInToday ? '今日已签到' : (signing ? '签到中...' : '立即签到') }}
              </button>
              <button class="btn-sign-in secondary" @click="goToSignInPage">
                查看签到日历
              </button>
            </div>
          </div>
        </div>

        <!-- 成就列表 -->
        <div class="growth-section">
          <div class="section-header">
            <h2 class="section-title">
              <span class="section-icon">🎯</span>
              我的成就
            </h2>
            <span class="section-subtitle">
              已解锁 {{ achievementInfo.obtainedCount }} / {{ achievementInfo.achievements.length }} 个
            </span>
          </div>
          <div class="achievement-list" v-loading="loadingAchievements">
            <div
              v-for="item in achievementInfo.achievements"
              :key="item.id"
              class="achievement-item"
              :class="{ obtained: item.obtained }"
            >
              <div class="achievement-icon">
                {{ achievementIcon(item.code) }}
              </div>
              <div class="achievement-info">
                <div class="achievement-name">
                  {{ item.name }}
                  <span v-if="item.obtained" class="obtained-badge">已解锁</span>
                  <span v-else class="locked-badge">未解锁</span>
                </div>
                <div class="achievement-desc">{{ item.description }}</div>
                <div class="achievement-condition">{{ item.requiredCondition }}</div>
              </div>
              <div class="achievement-reward">
                <span class="reward-value">+{{ item.rewardPoints }}</span>
                <span class="reward-label">积分</span>
              </div>
            </div>
            <div v-if="!loadingAchievements && achievementInfo.achievements.length === 0" class="empty-state">
              <p>暂无成就数据</p>
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
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const loadingAchievements = ref(false)
const signing = ref(false)
const router = useRouter()

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
  continuousDays: 0
})

const achievementInfo = reactive({
  achievements: [],
  obtainedCount: 0
})

const levelProgress = computed(() => {
  if (levelInfo.nextLevelExperience <= 0) return 0
  const progress = (levelInfo.experience / levelInfo.nextLevelExperience) * 100
  return Math.min(100, Math.max(0, progress))
})

const achievementIcon = (code) => {
  const icons = {
    FIRST_POST: '✍️',
    COMMENT_MASTER: '💬',
    COLLECTION_EXPERT: '🔖',
    LIKES_100: '❤️',
    VIEWS_1000: '👁️',
    SIGN_IN_7_DAYS: '🔥'
  }
  return icons[code] || '🏅'
}

const loadLevel = async () => {
  try {
    const data = await request({ url: '/api/growth/level', method: 'get' })
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
    const data = await request({ url: '/api/growth/points', method: 'get' })
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
    const data = await request({ url: '/api/growth/sign-in/status', method: 'get' })
    Object.assign(signInStatus, {
      signedInToday: data.signedInToday || false,
      continuousDays: data.continuousDays || 0
    })
  } catch (error) {
    console.error('加载签到状态失败:', error)
  }
}

const loadAchievements = async () => {
  loadingAchievements.value = true
  try {
    const data = await request({ url: '/api/growth/achievements', method: 'get' })
    achievementInfo.achievements = data.achievements || []
    achievementInfo.obtainedCount = data.obtainedCount || 0
  } catch (error) {
    console.error('加载成就失败:', error)
    achievementInfo.achievements = []
    achievementInfo.obtainedCount = 0
  } finally {
    loadingAchievements.value = false
  }
}

const handleSignIn = async () => {
  if (signInStatus.signedInToday || signing.value) return
  signing.value = true
  try {
    const result = await request({ url: '/api/growth/sign-in', method: 'post' })
    ElMessage.success(`签到成功，连续 ${result.continuousDays} 天，获得 ${result.rewardPoints} 积分`)
    await Promise.all([loadSignInStatus(), loadPoints(), loadLevel(), loadAchievements()])
  } catch (error) {
    ElMessage.error(error.message || '签到失败')
  } finally {
    signing.value = false
  }
}

const goToSignInPage = () => {
  router.push('/sign-in')
}

onMounted(() => {
  loadLevel()
  loadPoints()
  loadSignInStatus()
  loadAchievements()
})
</script>

<style lang="scss" scoped>
@use "@/styles/tech-button-mixins.scss" as *;
.growth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.main-content {
  flex: 1;
  padding: 40px 0;
}

.growth-header {
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

.growth-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 40px;
}

.growth-card {
  @include card-static;
  padding: 24px;
  transition: all var(--transition-normal);

  &:hover {
    transform: translateY(-4px);
    box-shadow: var(--shadow-lg);
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.card-icon {
  font-size: 22px;
}

.card-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.level-display {
  display: flex;
  align-items: center;
  gap: 20px;
}

.level-badge {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: white;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-glow);
}

.level-meta {
  flex: 1;
  min-width: 0;
}

.exp-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 10px;
}

.exp-bar {
  height: 10px;
  background: var(--color-bg-soft);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.exp-progress {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary), var(--color-secondary));
  border-radius: var(--radius-full);
  transition: width 0.6s ease;
}

.points-display {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.points-value {
  font-size: 42px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
}

.points-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.sign-in-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.continuous-days {
  display: flex;
  flex-direction: column;
}

.days-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1;
}

.days-label {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.btn-sign-in {
  @include tech-button(42px, 24px, 999px);
  font-size: 14px;
  flex-shrink: 0;

  &.signed {
    opacity: 0.8;
    filter: saturate(0.85);
  }

  &.loading {
    opacity: 0.7;
  }

  &.secondary {
    @include tech-ghost(42px, 22px, 999px);
    font-size: 14px;
  }
}

.growth-section {
  @include card-static;
  padding: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
}

.section-icon {
  font-size: 22px;
}

.section-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
}

.achievement-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.achievement-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: var(--radius-md);
  background: var(--color-bg-soft);
  border: 1px solid transparent;
  transition: all var(--transition-fast);
  opacity: 0.7;

  &.obtained {
    opacity: 1;
    background: var(--color-google-blue-light);
    border-color: rgba(26, 115, 232, 0.15);
  }

  &:hover {
    transform: translateX(4px);
  }
}

.achievement-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: var(--color-card);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.achievement-info {
  flex: 1;
  min-width: 0;
}

.achievement-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.obtained-badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-secondary);
  color: white;
  font-size: 11px;
  font-weight: 500;
}

.locked-badge {
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--color-bg);
  color: var(--color-text-muted);
  font-size: 11px;
  font-weight: 500;
}

.achievement-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.achievement-condition {
  font-size: 12px;
  color: var(--color-text-muted);
}

.achievement-reward {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.reward-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}

.reward-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-secondary);
}

@media (max-width: 992px) {
  .growth-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-title {
    font-size: 26px;
  }

  .level-display {
    flex-direction: column;
    align-items: flex-start;
  }

  .sign-in-body {
    flex-direction: column;
    align-items: flex-start;
  }

  .achievement-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .achievement-reward {
    flex-direction: row;
    align-items: center;
    gap: 4px;
  }
}
</style>
