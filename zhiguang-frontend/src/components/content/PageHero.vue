<template>
  <section class="page-hero">
    <div class="hero-orb orb-blue" aria-hidden="true"></div>
    <div class="hero-orb orb-violet" aria-hidden="true"></div>
    <div class="hero-grid" aria-hidden="true"></div>

    <div class="hero-main">
      <span v-if="eyebrow" class="hero-eyebrow">
        <i></i>{{ eyebrow }}
      </span>
      <h1 class="hero-title">{{ title }}</h1>
      <p v-if="description" class="hero-desc">{{ description }}</p>

      <div v-if="$slots.meta" class="hero-meta">
        <slot name="meta" />
      </div>

      <div v-if="$slots.actions" class="hero-actions">
        <slot name="actions" />
      </div>
    </div>

    <aside class="hero-side">
      <slot name="aside">
        <div class="hero-orbit" aria-hidden="true">
          <span class="ring ring-1"></span>
          <span class="ring ring-2"></span>
          <span class="ring ring-3"></span>
          <span class="orbit-dot dot-1"></span>
          <span class="orbit-dot dot-2"></span>
          <span class="orbit-dot dot-3"></span>
          <span class="orbit-core">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1" />
              <circle cx="12" cy="12" r="3.4" />
            </svg>
          </span>
        </div>
      </slot>

      <ul v-if="stats.length" class="hero-stats">
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
  stats: { type: Array, default: () => [] }
})
</script>

<style lang="scss" scoped>
.page-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(220px, 0.8fr);
  gap: 24px;
  overflow: hidden;
  padding: clamp(28px, 4vw, 44px);
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
  width: 300px;
  height: 300px;
  right: -70px;
  top: -130px;
  background: var(--content-orb-a);
}

.orb-violet {
  width: 240px;
  height: 240px;
  left: 32%;
  bottom: -150px;
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
  justify-content: center;
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
  text-transform: uppercase;

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
  font-size: clamp(30px, 4.2vw, 44px);
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--content-ink);
}

.hero-desc {
  max-width: 52ch;
  margin: 0;
  color: var(--content-ink-2);
  font-size: 14.5px;
  line-height: 1.8;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
  color: var(--content-ink-3);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
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
  width: 180px;
  height: 140px;
}

.ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(26, 115, 232, 0.16);
}

.ring-1 {
  inset: 8px 26px;
  transform: rotate(-18deg);
}

.ring-2 {
  inset: 22px 10px;
  border-color: rgba(168, 85, 247, 0.18);
  transform: rotate(14deg);
}

.ring-3 {
  inset: 32px 40px;
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
  top: 16px;
  right: 36px;
  background: #4285f4;
  box-shadow: 0 0 14px rgba(66, 133, 244, 0.7);
}

.dot-2 {
  bottom: 26px;
  left: 32px;
  background: #a855f7;
  box-shadow: 0 0 14px rgba(168, 85, 247, 0.6);
}

.dot-3 {
  top: 58px;
  right: 7px;
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
  width: 60px;
  height: 60px;
  margin: -30px 0 0 -30px;
  border-radius: 50%;
  color: #fff;
  background: linear-gradient(135deg, #1a73e8 0%, #6f5cf6 60%, #a855f7 100%);
  box-shadow: 0 16px 34px rgba(70, 90, 220, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.4);

  svg {
    width: 24px;
    height: 24px;
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
  .page-hero {
    grid-template-columns: 1fr;
  }

  .hero-side {
    align-items: flex-start;
    flex-direction: row;
  }

  .hero-orbit {
    display: none;
  }

  .hero-stats {
    flex-wrap: wrap;
  }
}

@media (max-width: 560px) {
  .page-hero {
    padding: 24px 20px;
    border-radius: 22px;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-stats {
    width: 100%;
    gap: 8px;

    li {
      flex: 1;
      min-width: 0;
      padding: 9px 10px;
    }

    span {
      font-size: 10.5px;
      letter-spacing: 0;
      white-space: nowrap;
    }
  }
}
</style>
