<template>
  <div class="auth-page" @mousemove="handleMouseMove" @mouseleave="resetHero">
    <!-- 动态光影背景 -->
    <div class="auth-bg" aria-hidden="true">
      <div class="aurora aurora-1"></div>
      <div class="aurora aurora-2"></div>
      <div class="aurora aurora-3"></div>
      <div class="light-grid"></div>
      <div class="particles">
        <span v-for="i in 24" :key="i" class="particle"></span>
      </div>
    </div>

    <div class="auth-container">
      <!-- 左侧看板娘立绘 -->
      <div class="auth-left">
        <div
          class="auth-hero"
          ref="heroRef"
          :class="{ 'is-shy': isShy }"
          :style="heroStyle"
          aria-hidden="true"
        >
          <div class="hero-glow"></div>

          <span
            v-for="(st, i) in HERO_STARS"
            :key="i"
            class="hero-star"
            :style="{
              left: st.x + '%',
              top: st.y + '%',
              width: st.s + 'px',
              height: st.s + 'px',
              animationDelay: st.d + 's'
            }"
          ></span>

          <div class="hero-stage">
            <div class="hero-card">
              <span class="hero-rim-halo"><i class="hero-rim-sweep"></i></span>
              <span class="hero-rim"><i class="hero-rim-sweep"></i></span>

              <div class="hero-frame">
                <img class="hero-art" src="/auth/light-hero-login.webp" alt="" draggable="false" />
                <span class="hero-bezel"></span>
                <span class="hero-sheen"></span>
              </div>

              <i class="hero-corner tl"></i>
              <i class="hero-corner tr"></i>
              <i class="hero-corner bl"></i>
              <i class="hero-corner br"></i>
              <span class="hero-base"></span>
            </div>

            <span
              v-for="(m, i) in HERO_MOTES"
              :key="i"
              class="hero-mote"
              :style="{
                left: m.x + '%',
                bottom: m.b + '%',
                width: m.s + 'px',
                height: m.s + 'px',
                animationDelay: m.d + 's',
                animationDuration: m.t + 's'
              }"
            ></span>

            <div class="hero-reflection">
              <div class="hero-mirror">
                <img src="/auth/light-hero-login.webp" alt="" draggable="false" />
              </div>
            </div>
          </div>

          <div class="hero-greeting glass-bubble">
            <span class="greeting-line">{{ isShy ? '我把眼睛闭上啦' : 'Hi，我是 Light' }}</span>
            <span class="greeting-subline">{{ isShy ? '密码不会被我看到 🙈' : '欢迎回来 ✨' }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧玻璃表单 -->
      <div class="auth-right">
        <div class="auth-card glass-panel">
          <div class="card-shine"></div>
          <div class="auth-header">
            <div class="brand-mark">
              <img :src="logoSrc" alt="" aria-hidden="true" draggable="false" />
            </div>
            <h2 class="auth-title">欢迎回来</h2>
            <p class="auth-subtitle">登录以继续探索知识的世界</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="auth-form"
            @submit.prevent="handleLogin"
          >
            <el-form-item prop="identifier">
              <label class="input-label">邮箱或用户名</label>
              <div class="glass-input-wrap">
                <el-input
                  v-model="form.identifier"
                  placeholder="请输入邮箱地址或用户名"
                  size="large"
                  autocomplete="username"
                />
              </div>
            </el-form-item>

            <el-form-item prop="password">
              <label class="input-label">密码</label>
              <div class="glass-input-wrap">
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  show-password
                  autocomplete="current-password"
                  @focus="handlePasswordFocus"
                  @blur="handlePasswordBlur"
                />
              </div>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <router-link to="/contact" class="forgot-link">忘记密码？</router-link>
            </div>

            <el-form-item>
              <button type="submit" class="btn-glow btn-full" :disabled="loading">
                <span class="btn-text">{{ loading ? '登录中...' : '登 录' }}</span>
                <span class="btn-shine"></span>
              </button>
            </el-form-item>
          </el-form>

          <div class="auth-footer">
            <span>还没有账号？</span>
            <router-link to="/register" class="link-glow">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { useThemeStore } from '@/store/theme'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const logoSrc = computed(() =>
  themeStore.isDark() ? '/brand/logo-dark.png' : '/brand/logo-light.png'
)

const formRef = ref(null)
const loading = ref(false)
const rememberMe = ref(true)
const heroRef = ref(null)
const isShy = ref(false)
const parallax = reactive({ x: 0, y: 0, tiltX: 0, tiltY: 0 })

const HERO_STARS = [
  { x: 1, y: 13, s: 16, d: 0 },
  { x: 90, y: 5, s: 11, d: 0.7 },
  { x: 95, y: 46, s: 14, d: 1.4 },
  { x: 4, y: 64, s: 12, d: 2.1 },
  { x: 80, y: 86, s: 10, d: 1.1 },
  { x: 16, y: 90, s: 13, d: 2.6 },
  { x: 46, y: 1, s: 9, d: 1.8 }
]

const HERO_MOTES = [
  { x: 6, b: 18, s: 5, d: 0, t: 5.6 },
  { x: 88, b: 24, s: 4, d: 1.2, t: 6.4 },
  { x: 17, b: 8, s: 6, d: 2.4, t: 5.2 },
  { x: 74, b: 5, s: 4, d: 3.1, t: 6.8 },
  { x: 46, b: 2, s: 5, d: 1.8, t: 7.2 },
  { x: 95, b: 38, s: 3, d: 4, t: 5.9 }
]

const form = reactive({
  identifier: '',
  password: ''
})

const rules = {
  identifier: [
    { required: true, message: '请输入邮箱或用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 个字符', trigger: 'blur' }
  ]
}

const heroStyle = computed(() => ({
  '--px': `${parallax.x}px`,
  '--py': `${parallax.y}px`,
  '--tilt-x': `${parallax.tiltX}deg`,
  '--tilt-y': `${parallax.tiltY}deg`
}))

const clampUnit = (v) => Math.max(-1, Math.min(1, v))

const handleMouseMove = (event) => {
  if (isShy.value) return
  const el = heroRef.value
  if (!el) return

  const rect = el.getBoundingClientRect()
  const nx = clampUnit(((event.clientX - rect.left) / rect.width - 0.5) * 2)
  const ny = clampUnit(((event.clientY - rect.top) / rect.height - 0.5) * 2)

  parallax.x = nx * 18
  parallax.y = ny * 14
  parallax.tiltY = nx * 7
  parallax.tiltX = ny * -5
}

const resetHero = () => {
  if (isShy.value) return
  parallax.x = 0
  parallax.y = 0
  parallax.tiltX = 0
  parallax.tiltY = 0
}

const handlePasswordFocus = () => {
  isShy.value = true
  parallax.x = 0
  parallax.y = 0
  parallax.tiltX = 0
  parallax.tiltY = 0
}

const handlePasswordBlur = () => {
  isShy.value = false
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.identifier, form.password, rememberMe.value)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/home')
  } catch {
    // request interceptor shows the error message
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
@use "sass:math";
@use "@/styles/auth-hero.scss";

.auth-page {
  min-height: 100vh;
  display: flex;
  position: relative;
  overflow: hidden;
  background: var(--color-bg);
}

/* ===== 动态光影背景 ===== */
.auth-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

.aurora {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
  animation: auroraFlow 14s ease-in-out infinite;
}

.aurora-1 {
  width: 60vw;
  height: 60vw;
  top: -20%;
  left: -15%;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.35), transparent 70%);
  animation-delay: 0s;
}

.aurora-2 {
  width: 50vw;
  height: 50vw;
  bottom: -15%;
  right: -10%;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.35), transparent 70%);
  animation-delay: -4s;
}

.aurora-3 {
  width: 40vw;
  height: 40vw;
  top: 35%;
  left: 35%;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.28), transparent 70%);
  animation-delay: -8s;
}

.light-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse at 50% 50%, black 0%, transparent 70%);
}

.particles {
  position: absolute;
  inset: 0;
}

.particle {
  position: absolute;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.6);
  animation: particleFloat 8s ease-in-out infinite;

  @for $i from 1 through 24 {
    &:nth-child(#{$i}) {
      left: math.random(100) * 1%;
      top: math.random(100) * 1%;
      animation-delay: math.random(8000) * -1ms;
      animation-duration: 6s + math.random(6);
      opacity: 0.3 + math.div(math.random(50), 100);
    }
  }
}

/* ===== 玻璃面板通用 ===== */
.glass-panel {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.72) 0%, rgba(255, 255, 255, 0.42) 100%);
  backdrop-filter: blur(32px) saturate(180%);
  -webkit-backdrop-filter: blur(32px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow:
    inset 1px 1px rgba(255, 255, 255, 0.62),
    inset -1px -1px rgba(255, 255, 255, 0.16),
    0 24px 60px rgba(0, 0, 0, 0.08);
  border-radius: var(--radius-xl);
  position: relative;
  overflow: hidden;

  @include glass-rim(0.62, 0.3);
}

.glass-bubble {
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow:
    inset 1px 1px rgba(255, 255, 255, 0.7),
    0 12px 40px rgba(0, 0, 0, 0.1);

  @include glass-rim(0.7, 0.28);
}

.card-shine {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    120deg,
    transparent 30%,
    rgba(255, 255, 255, 0.35) 45%,
    transparent 60%
  );
  animation: cardShine 6s ease-in-out infinite;
  pointer-events: none;
}

/* ===== 布局 ===== */
.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  min-height: 100vh;
  display: flex;
}

.auth-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  position: relative;
}

.auth-right {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
}

/* ===== 表单卡片 ===== */
.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 44px 40px;
}

.auth-header {
  margin-bottom: 36px;
  text-align: center;
}

.brand-mark {
  width: 60px;
  height: 60px;
  margin: 0 auto 18px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  box-shadow: 0 10px 26px rgba(99, 102, 241, 0.28);

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.auth-title {
  font-size: 30px;
  font-weight: 700;
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--color-text), var(--color-ai-purple));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-subtitle {
  font-size: 15px;
  color: var(--color-text-secondary);
}

/* ===== 玻璃输入框 ===== */
.auth-form {
  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-form-item__error) {
    padding-top: 4px;
  }
}

.input-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  padding-left: 4px;
}

.glass-input-wrap {
  position: relative;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: inset 0 1px 2px rgba(255, 255, 255, 0.6), 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all var(--transition-fast);

  &:hover {
    border-color: rgba(168, 85, 247, 0.45);
    box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.1), inset 0 1px 2px rgba(255, 255, 255, 0.7);
  }

  &:has(.is-focus) {
    border-color: rgba(168, 85, 247, 0.6);
    box-shadow: 0 0 0 4px rgba(168, 85, 247, 0.12), 0 0 20px rgba(168, 85, 247, 0.15);
  }

  :deep(.el-input__wrapper) {
    padding: 6px 16px;
    background: transparent;
    box-shadow: none;
    border-radius: var(--radius-md);

    .el-input__inner {
      color: var(--color-text);
      font-size: 14.5px;
      height: 42px;

      &::placeholder {
        color: var(--color-text-muted);
      }
    }
  }
}

.form-options {
  @include flex-between;
  margin-bottom: 28px;
  font-size: 14px;

  :deep(.el-checkbox__label) {
    color: var(--color-text-secondary);
  }

  :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
    color: var(--color-ai-purple);
  }
}

.forgot-link {
  font-size: 14px;
  color: var(--color-ai-purple);
  font-weight: 500;

  &:hover {
    color: var(--color-ai-pink);
    text-decoration: underline;
  }
}

/* ===== 发光按钮 ===== */
/* 主按钮外观统一由 styles/tech-buttons.scss 提供（.btn-glow） */

/* ===== 页脚 ===== */
.auth-footer {
  text-align: center;
  font-size: 14px;
  color: var(--color-text-muted);
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.link-glow {
  color: var(--color-ai-purple);
  font-weight: 600;
  margin-left: 4px;
  position: relative;

  &:hover {
    color: var(--color-ai-pink);
    text-decoration: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 0;
    height: 2px;
    background: linear-gradient(90deg, var(--color-ai-purple), var(--color-ai-pink));
    transition: width var(--transition-normal);
  }

  &:hover::after {
    width: 100%;
  }
}

/* ===== 动画 ===== */
@keyframes auroraFlow {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(5%, -5%) scale(1.05); }
  66% { transform: translate(-3%, 3%) scale(0.98); }
}

@keyframes particleFloat {
  0%, 100% { transform: translateY(0) scale(1); opacity: 0.4; }
  50% { transform: translateY(-20px) scale(1.2); opacity: 0.8; }
}

@keyframes cardShine {
  0%, 100% { transform: translateX(-100%) rotate(25deg); }
  50% { transform: translateX(100%) rotate(25deg); }
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .auth-left {
    display: none;
  }

  .auth-right {
    width: 100%;
    min-height: 100vh;
    padding: 40px 24px;
  }

  .auth-card {
    max-width: 400px;
    padding: 36px 28px;
  }
}

@media (max-width: 480px) {
  .auth-title {
    font-size: 26px;
  }

  .auth-card {
    padding: 28px 22px;
  }
}

/* 减少动效偏好：保留立绘与视差关闭 */
/* ===== 暗色模式适配 ===== */
[data-theme="dark"] {
  .glass-panel {
    background:
      linear-gradient(135deg, rgba(25, 30, 42, 0.72) 0%, rgba(15, 20, 28, 0.52) 100%);
    border-color: rgba(255, 255, 255, 0.12);
    box-shadow:
      inset 1px 1px rgba(255, 255, 255, 0.12),
      inset -1px -1px rgba(140, 170, 220, 0.1),
      0 24px 60px rgba(0, 0, 0, 0.35);
  }

  .glass-bubble {
    background: rgba(25, 30, 42, 0.8);
    border-color: rgba(255, 255, 255, 0.12);
  }

  .glass-input-wrap {
    background: rgba(20, 25, 35, 0.6);
    border-color: rgba(255, 255, 255, 0.12);

    &:hover {
      border-color: rgba(168, 85, 247, 0.55);
    }

    &:has(.is-focus) {
      box-shadow: 0 0 0 4px rgba(168, 85, 247, 0.18), 0 0 24px rgba(168, 85, 247, 0.2);
    }
  }

  .auth-footer {
    border-top-color: rgba(255, 255, 255, 0.08);
  }

}
</style>