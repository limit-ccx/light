<template>
  <InfoLayout
    eyebrow="Avatar Gallery"
    title="头像商城"
    description="用签到积分兑换限定头像，兑换后永久拥有，可随时切换。"
    :stats="heroStats"
  >
    <template #actions>
      <router-link to="/sign-in" class="hero-action">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="5" width="18" height="16" rx="3"></rect>
          <path d="M8 3v4M16 3v4M3 11h18"></path>
          <path d="m9 16 2 2 4-4"></path>
        </svg>
        去签到赚积分
      </router-link>
      <router-link to="/profile" class="hero-action ghost">返回个人中心</router-link>
    </template>

    <!-- 积分概览 -->
    <section class="balance-card">
      <div class="balance-avatar">
        <img :src="authStore.user?.avatar || defaultAvatar" alt="当前头像" />
        <span class="balance-avatar-ring"></span>
      </div>
      <div class="balance-copy">
        <span class="balance-label">我的积分</span>
        <strong class="balance-points">{{ shop.points }}</strong>
        <p>每日签到可获得 10–30 积分，攒够 {{ shop.price }} 积分即可兑换一个限定头像。</p>
      </div>
      <div class="balance-side">
        <span class="price-chip">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9"></circle>
            <path d="M12 7v10M9.5 9.5h5M9.5 14.5h5"></path>
          </svg>
          {{ shop.price }} 积分 / 个
        </span>
        <router-link to="/sign-in" class="balance-link">去签到</router-link>
      </div>
    </section>

    <!-- 筛选 -->
    <div class="shop-toolbar">
      <div class="shop-tabs" role="tablist" aria-label="头像筛选">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          type="button"
          role="tab"
          :aria-selected="activeFilter === tab.key"
          :class="{ active: activeFilter === tab.key }"
          @click="activeFilter = tab.key"
        >
          {{ tab.label }}
          <span>{{ tabCount(tab.key) }}</span>
        </button>
      </div>
      <span class="shop-hint">兑换后立即更换头像，已拥有的头像可反复切换</span>
    </div>

    <!-- 商品网格 -->
    <div v-if="loading" class="avatar-grid">
      <div v-for="n in 10" :key="n" class="avatar-skeleton"></div>
    </div>

    <div v-else-if="filteredAvatars.length" class="avatar-grid">
      <article
        v-for="avatar in filteredAvatars"
        :key="avatar.id"
        class="avatar-card"
        :class="{ 'is-owned': avatar.owned, 'is-equipped': avatar.equipped }"
      >
        <div class="avatar-media">
          <img :src="avatar.imageUrl" :alt="avatar.name" loading="lazy" />
          <span v-if="avatar.equipped" class="state-badge equipped">使用中</span>
          <span v-else-if="avatar.owned" class="state-badge owned">已拥有</span>
          <span v-else class="state-badge limited">限定</span>
          <div v-if="avatar.equipped" class="equipped-check" aria-hidden="true">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <path d="m5 12 5 5L20 7" />
            </svg>
          </div>
        </div>

        <div class="avatar-info">
          <div class="avatar-name-row">
            <h3>{{ avatar.name }}</h3>
            <span class="avatar-tagline">{{ avatar.tagline }}</span>
          </div>
          <div class="avatar-price">
            <template v-if="avatar.owned">
              <span class="owned-text">已兑换</span>
            </template>
            <template v-else>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="9"></circle>
                <path d="M12 7v10M9.5 9.5h5M9.5 14.5h5"></path>
              </svg>
              {{ avatar.price }} 积分
            </template>
          </div>
        </div>

        <button
          v-if="avatar.equipped"
          type="button"
          class="avatar-action is-current"
          disabled
        >
          正在使用
        </button>
        <button
          v-else-if="avatar.owned"
          type="button"
          class="avatar-action is-use"
          :disabled="busyId === avatar.id"
          @click="handleEquip(avatar)"
        >
          {{ busyId === avatar.id ? '切换中…' : '使用这个头像' }}
        </button>
        <button
          v-else
          type="button"
          class="avatar-action is-buy"
          :disabled="shop.points < avatar.price || busyId === avatar.id"
          @click="handlePurchase(avatar)"
        >
          <template v-if="shop.points < avatar.price">
            还差 {{ avatar.price - shop.points }} 积分
          </template>
          <template v-else>
            {{ busyId === avatar.id ? '兑换中…' : `${avatar.price} 积分兑换` }}
          </template>
        </button>
      </article>
    </div>

    <div v-else class="shop-empty ln-panel">
      <h3>这里还没有符合条件的头像</h3>
      <p>切换筛选看看，或者去签到攒积分兑换新的限定头像。</p>
      <router-link to="/sign-in" class="hero-action">去签到</router-link>
    </div>
  </InfoLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import InfoLayout from '@/components/content/InfoLayout.vue'
import { useAuthStore } from '@/store/auth'
import { equipAvatar, getAvatarShop, purchaseAvatar } from '@/api/avatarShop'

const authStore = useAuthStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const busyId = ref('')
const activeFilter = ref('all')
const shop = ref({
  points: 0,
  price: 1000,
  ownedCount: 0,
  avatars: []
})

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'owned', label: '已拥有' },
  { key: 'locked', label: '未拥有' }
]

const heroStats = computed(() => [
  { value: shop.value.points, label: '可用积分' },
  { value: shop.value.ownedCount, label: '已拥有头像' },
  { value: shop.value.price, label: '兑换所需积分' }
])

const filteredAvatars = computed(() => {
  if (activeFilter.value === 'owned') return shop.value.avatars.filter((item) => item.owned)
  if (activeFilter.value === 'locked') return shop.value.avatars.filter((item) => !item.owned)
  return shop.value.avatars
})

const tabCount = (key) => {
  if (key === 'owned') return shop.value.avatars.filter((item) => item.owned).length
  if (key === 'locked') return shop.value.avatars.filter((item) => !item.owned).length
  return shop.value.avatars.length
}

const loadShop = async () => {
  loading.value = true
  try {
    const data = await getAvatarShop()
    shop.value = {
      points: data.points || 0,
      price: data.price || 1000,
      ownedCount: data.ownedCount || 0,
      avatars: Array.isArray(data.avatars) ? data.avatars : []
    }
  } finally {
    loading.value = false
  }
}

const applyEquipped = (avatarId) => {
  shop.value.avatars.forEach((item) => {
    item.equipped = item.id === avatarId
  })
}

const syncAvatar = (avatarUrl) => {
  authStore.updateUser({ avatar: avatarUrl })
}

const handlePurchase = async (avatar) => {
  if (shop.value.points < avatar.price) return

  try {
    await ElMessageBox.confirm(
      `兑换「${avatar.name}」将消耗 ${avatar.price} 积分，兑换后会立即更换头像。`,
      '兑换头像',
      {
        confirmButtonText: '确认兑换',
        cancelButtonText: '再看看',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  busyId.value = avatar.id
  try {
    const result = await purchaseAvatar(avatar.id)
    shop.value.points = result.points
    const target = shop.value.avatars.find((item) => item.id === avatar.id)
    if (target) {
      target.owned = true
      shop.value.ownedCount = shop.value.avatars.filter((item) => item.owned).length
    }
    applyEquipped(avatar.id)
    syncAvatar(result.avatar)
    ElMessage.success(`已兑换「${avatar.name}」，头像已更新`)
  } finally {
    busyId.value = ''
  }
}

const handleEquip = async (avatar) => {
  busyId.value = avatar.id
  try {
    const result = await equipAvatar(avatar.id)
    applyEquipped(avatar.id)
    syncAvatar(result.avatar)
    ElMessage.success(`已切换为「${avatar.name}」`)
  } finally {
    busyId.value = ''
  }
}

onMounted(loadShop)
</script>

<style lang="scss" scoped>
.hero-action {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 40px;
  padding: 0 18px;
  border-radius: 12px;
  border: none;
  color: #fff;
  font-size: 13.5px;
  font-weight: 600;
  text-decoration: none;
  background: linear-gradient(120deg, #1a73e8, #6f5cf6 62%, #8b5cf6);
  box-shadow: 0 10px 24px rgba(47, 99, 235, 0.28);
  transition: transform var(--transition-fast), filter var(--transition-fast);

  &:hover {
    transform: translateY(-1px);
    filter: brightness(1.05);
  }

  &.ghost {
    color: var(--content-ink-2);
    background: var(--content-chip);
    border: 1px solid var(--content-chip-line);
    box-shadow: none;

    &:hover {
      border-color: var(--content-line-strong);
      color: var(--color-primary);
    }
  }
}

.balance-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px 26px;
  border-radius: 24px;
  border: 1px solid var(--content-line);
  background:
    radial-gradient(90% 100% at 100% 0%, var(--content-glow-b), transparent 58%),
    var(--content-surface);
  box-shadow: var(--content-shadow);
}

.balance-avatar {
  position: relative;
  flex-shrink: 0;
  width: 76px;
  height: 76px;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    border: 3px solid var(--content-surface);
    box-shadow: 0 10px 24px rgba(32, 60, 120, 0.16);
  }
}

.balance-avatar-ring {
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 1px solid var(--content-line-strong);
  border-style: dashed;
  pointer-events: none;
}

.balance-copy {
  flex: 1;
  min-width: 0;
}

.balance-label {
  display: block;
  color: var(--content-ink-3);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.balance-points {
  display: block;
  margin: 2px 0 4px;
  color: var(--color-primary);
  font-size: 34px;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.balance-copy p {
  margin: 0;
  color: var(--content-ink-2);
  font-size: 13px;
  line-height: 1.6;
}

.balance-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.price-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);
  color: var(--content-ink-2);
  font-size: 12px;
  white-space: nowrap;
}

.balance-link {
  color: var(--color-primary);
  font-size: 12.5px;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

.shop-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.shop-tabs {
  display: inline-flex;
  gap: 3px;
  padding: 4px;
  border-radius: 999px;
  border: 1px solid var(--content-line);
  background: var(--content-tabs);

  button {
    height: 32px;
    padding: 0 15px;
    border: none;
    border-radius: 999px;
    background: transparent;
    color: var(--content-ink-2);
    font-family: inherit;
    font-size: 12.5px;
    cursor: pointer;
    transition: all var(--transition-fast);

    span {
      margin-left: 5px;
      color: var(--content-ink-3);
      font-size: 11px;
    }

    &:hover {
      color: var(--color-primary);
    }

    &.active {
      color: var(--color-primary);
      background: var(--content-surface);
      box-shadow: var(--content-shadow);
      font-weight: 600;

      span {
        color: var(--color-primary);
      }
    }
  }
}

.shop-hint {
  color: var(--content-ink-3);
  font-size: 12px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 16px;
}

.avatar-skeleton {
  height: 300px;
  border-radius: 22px;
  background: linear-gradient(100deg, var(--content-sk-a) 28%, var(--content-sk-b) 50%, var(--content-sk-a) 72%);
  background-size: 220% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

.avatar-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 10px;
  border-radius: 22px;
  border: 1px solid var(--content-line);
  background: var(--content-surface);
  box-shadow: var(--content-shadow);
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:hover {
    transform: translateY(-3px);
    border-color: var(--content-line-strong);
    box-shadow: var(--content-shadow-lg);
  }

  &.is-equipped {
    border-color: var(--content-line-strong);
    box-shadow: 0 0 0 1px var(--content-line-strong), var(--content-shadow);
  }
}

.avatar-media {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 16px;
  background: var(--content-surface-tint);

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
    transition: transform 0.35s ease;
  }
}

.avatar-card:hover .avatar-media img {
  transform: scale(1.03);
}

.state-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 9px;
  border-radius: 999px;
  color: #fff;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
  backdrop-filter: blur(8px);

  &.equipped {
    background: linear-gradient(120deg, #1a73e8, #6f5cf6);
    box-shadow: 0 6px 14px rgba(47, 99, 235, 0.32);
  }

  &.owned {
    background: rgba(16, 185, 129, 0.92);
  }

  &.limited {
    background: rgba(15, 23, 42, 0.55);
    border: 1px solid rgba(255, 255, 255, 0.25);
  }
}

.equipped-check {
  position: absolute;
  right: 10px;
  bottom: 10px;
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #1a73e8, #8b5cf6);
  box-shadow: 0 6px 14px rgba(47, 99, 235, 0.35);
}

.avatar-info {
  padding: 12px 6px 8px;
}

.avatar-name-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;

  h3 {
    margin: 0;
    color: var(--content-ink);
    font-size: 16px;
    font-weight: 600;
    letter-spacing: -0.01em;
  }
}

.avatar-tagline {
  overflow: hidden;
  color: var(--content-ink-3);
  font-size: 11.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar-price {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 5px;
  color: #b45309;
  font-size: 12.5px;
  font-weight: 600;

  svg {
    flex-shrink: 0;
  }
}

.owned-text {
  color: var(--color-success);
  font-weight: 500;
}

.avatar-action {
  width: 100%;
  height: 36px;
  margin-top: 4px;
  border-radius: 10px;
  border: 1px solid transparent;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-fast);

  &.is-buy {
    color: #fff;
    background: linear-gradient(120deg, #1a73e8, #6f5cf6);
    box-shadow: 0 8px 18px rgba(47, 99, 235, 0.22);

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      filter: brightness(1.04);
    }

    &:disabled {
      color: var(--content-ink-3);
      background: var(--content-surface-tint);
      border-color: var(--content-chip-line);
      box-shadow: none;
      cursor: not-allowed;
    }
  }

  &.is-use {
    color: var(--color-primary);
    background: var(--content-chip);
    border-color: var(--content-chip-line);

    &:hover:not(:disabled) {
      border-color: var(--content-line-strong);
      background: var(--content-surface-tint);
    }
  }

  &.is-current {
    color: var(--content-ink-3);
    background: var(--content-surface-tint);
    border-color: var(--content-chip-line);
    cursor: default;
  }
}

.shop-empty {
  padding: 52px 24px;
  text-align: center;

  h3 {
    margin: 0 0 8px;
    color: var(--content-ink);
    font-size: 18px;
  }

  p {
    margin: 0 0 18px;
    color: var(--content-ink-2);
    font-size: 13.5px;
  }
}

@media (max-width: 900px) {
  .avatar-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 12px;
  }

  .balance-card {
    flex-wrap: wrap;
  }

  .balance-side {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

@media (max-width: 560px) {
  .balance-card {
    padding: 18px;
    gap: 14px;
  }

  .balance-avatar {
    width: 62px;
    height: 62px;
  }

  .balance-points {
    font-size: 28px;
  }

  .avatar-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .shop-tabs {
    width: 100%;

    button {
      flex: 1;
      padding: 0 8px;
    }
  }

  .shop-hint {
    display: none;
  }

  .avatar-name-row {
    align-items: flex-start;
    flex-direction: column;
    gap: 1px;
  }
}
</style>
