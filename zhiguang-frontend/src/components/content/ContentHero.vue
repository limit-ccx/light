<template>
  <section class="content-hero">
    <div class="hero-orb orb-blue" aria-hidden="true"></div>
    <div class="hero-orb orb-violet" aria-hidden="true"></div>
    <div class="hero-grid" aria-hidden="true"></div>

    <div class="hero-main">
      <span class="hero-eyebrow">
        <i></i>{{ eyebrow }}
      </span>
      <h1 class="hero-title">{{ title }}</h1>
      <p class="hero-desc">{{ description }}</p>

      <nav class="hero-tabs" aria-label="内容分区">
        <router-link
          v-for="tab in tabs"
          :key="tab.key"
          :to="tab.to"
          class="hero-tab"
          :class="{ 'is-active': active === tab.key }"
        >
          {{ tab.label }}
        </router-link>
      </nav>
    </div>

    <aside class="hero-side">
      <div class="hero-orbit" aria-hidden="true">
        <span class="ring ring-1"></span>
        <span class="ring ring-2"></span>
        <span class="ring ring-3"></span>
        <span class="orbit-dot dot-1"></span>
        <span class="orbit-dot dot-2"></span>
        <span class="orbit-dot dot-3"></span>
        <span class="orbit-core">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
            <path d="M9 7h7M9 11h5" />
          </svg>
        </span>
      </div>

      <ul class="hero-stats">
        <li v-for="stat in stats" :key="stat.label">
          <b>{{ stat.value }}</b>
          <span>{{ stat.label }}</span>
        </li>
      </ul>
    </aside>
  </section>
</template>

<script setup>
defineProps({
  eyebrow: { type: String, default: '纸域空间' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  active: { type: String, default: 'articles' },
  stats: { type: Array, default: () => [] }
})

const tabs = [
  { key: 'articles', label: '文章', to: '/articles' },
  { key: 'columns', label: '专栏', to: '/columns' },
  { key: 'collections', label: '合集', to: '/collections' }
]
</script>

<style lang="scss" scoped>
.content-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(260px, 0.85fr);
  gap: 28px;
  overflow: hidden;
  padding: clamp(28px, 4vw, 46px);
  border-radius: 28px;
  border: 1px solid var(--content-hero-line);
  background: var(--content-hero-bg);
  box-shadow: var(--content-hero-shadow);
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}

.orb-blue {
  width: 320px;
  height: 320px;
  right: -60px;
  top: -120px;
  background: var(--content-orb-a);
}

.orb-violet {
  width: 260px;
  height: 260px;
  left: 30%;
  bottom: -160px;
  background: var(--content-orb-b);
}

.hero-grid {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background-image:
    linear-gradient(var(--content-grid) 1px, transparent 1px),
    linear-gradient(90deg, var(--content-grid) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: radial-gradient(80% 90% at 70% 30%, #000 0%, transparent 75%);
  -webkit-mask-image: radial-gradient(80% 90% at 70% 30%, #000 0%, transparent 75%);
  pointer-events: none;
}

.hero-main {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 13px;
  border-radius: 999px;
  border: 1px solid var(--content-hero-line);
  background: var(--content-eyebrow);
  color: var(--content-ink-2);
  font-size: 12px;
  letter-spacing: 0.12em;

  i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: linear-gradient(135deg, #4285f4, #a855f7);
    box-shadow: 0 0 10px rgba(66, 133, 244, 0.6);
  }
}

.hero-title {
  margin: 16px 0 10px;
  font-size: clamp(30px, 4.2vw, 46px);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--content-ink);

}

.hero-desc {
  max-width: 46ch;
  margin: 0 0 22px;
  color: var(--content-ink-2);
  font-size: 14.5px;
  line-height: 1.8;
}

.hero-tabs {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border-radius: 999px;
  border: 1px solid var(--content-line);
  background: var(--content-tabs);
  backdrop-filter: blur(8px);
}

.hero-tab {
  padding: 8px 22px;
  border-radius: 999px;
  color: var(--content-ink-2);
  font-size: 13.5px;
  font-weight: 500;
  transition: all var(--transition-fast);

  &:hover {
    color: var(--color-primary);
  }

  &.is-active {
    color: #fff;
    background: linear-gradient(120deg, #1a73e8, #5b6cf5 60%, #8b5cf6);
    box-shadow: 0 8px 22px rgba(47, 99, 235, 0.3);
  }
}

.hero-side {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.hero-orbit {
  position: relative;
  width: 190px;
  height: 150px;
}

.ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(26, 115, 232, 0.16);
}

.ring-1 {
  inset: 8px 28px;
  transform: rotate(-18deg);
}

.ring-2 {
  inset: 22px 10px;
  border-color: rgba(168, 85, 247, 0.18);
  transform: rotate(14deg);
}

.ring-3 {
  inset: 34px 42px;
  border-style: dashed;
  border-color: rgba(26, 115, 232, 0.22);
}

.orbit-dot {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-1 {
  top: 18px;
  right: 38px;
  background: #4285f4;
  box-shadow: 0 0 14px rgba(66, 133, 244, 0.7);
}

.dot-2 {
  bottom: 30px;
  left: 34px;
  background: #a855f7;
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.6);
}

.dot-3 {
  top: 62px;
  right: 8px;
  width: 6px;
  height: 6px;
  background: #ec4899;
  box-shadow: 0 0 12px rgba(236, 72, 153, 0.55);
}

.orbit-core {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  margin: -32px 0 0 -32px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #1a73e8 0%, #6f5cf6 60%, #a855f7 100%);
  box-shadow: 0 16px 34px rgba(70, 90, 220, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  svg {
    width: 26px;
    height: 26px;
  }
}

.hero-stats {
  display: flex;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    min-width: 76px;
    padding: 10px 14px;
    border-radius: 14px;
    border: 1px solid var(--content-stat-line);
    background: var(--content-stat);
    backdrop-filter: blur(10px);
    box-shadow: 0 8px 20px rgba(32, 60, 120, 0.05);
  }

  b {
    display: block;
    font-size: 20px;
    font-weight: 600;
    line-height: 1.1;
    color: var(--content-ink);
    font-variant-numeric: tabular-nums;
  }

  span {
    display: block;
    margin-top: 3px;
    color: var(--content-ink-3);
    font-size: 11.5px;
    letter-spacing: 0.04em;
  }
}

@media (max-width: 900px) {
  .content-hero {
    grid-template-columns: 1fr;
  }

  .hero-side {
    align-items: flex-start;
  }

  .hero-orbit {
    display: none;
  }

  .hero-stats {
    flex-wrap: wrap;
  }
}

@media (max-width: 560px) {
  .content-hero {
    padding: 24px 20px;
    border-radius: 22px;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-tabs {
    width: 100%;
  }

  .hero-tab {
    flex: 1;
    text-align: center;
    padding: 8px 10px;
  }

  .hero-stats {
    width: 100%;
    gap: 8px;

    li {
      flex: 1;
      min-width: 0;
      padding: 9px 10px;
    }

    b {
      font-size: 18px;
    }

    span {
      font-size: 10.5px;
      letter-spacing: 0;
      white-space: nowrap;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-tab {
    transition: none;
  }
}
</style>
