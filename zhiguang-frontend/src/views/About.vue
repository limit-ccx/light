<template>
  <InfoLayout
    eyebrow="关于纸域空间"
    title="Think Deep, Write Clean"
    description="我们相信，每一个认真记录的想法都值得被阅读。纸域空间为那些还在坚持深度表达的人而建。"
  >
    <section class="ln-panel ln-panel-pad about-section">
      <h2 class="ln-section-title">平台特点</h2>
      <div class="features-grid">
        <div v-for="feature in features" :key="feature.title" class="feature-card" :class="feature.tone">
          <span class="feature-icon">
            <span v-html="feature.icon"></span>
          </span>
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
        </div>
      </div>
    </section>

    <section class="ln-panel ln-panel-pad about-section">
      <h2 class="ln-section-title">技术栈</h2>
      <div class="tech-grid">
        <div v-for="tech in stack" :key="tech.title" class="tech-item">
          <span class="tech-icon">
            <span v-html="tech.icon"></span>
          </span>
          <div>
            <h3>{{ tech.title }}</h3>
            <p>{{ tech.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="ln-panel ln-panel-pad about-section contact-section">
      <h2 class="ln-section-title">联系我们</h2>
      <p class="contact-desc">如果你有任何问题或建议，欢迎通过以下方式联系我们。</p>
      <div class="contact-list">
        <a class="contact-item" href="mailto:contact@zhiguang.com">
          <span class="contact-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
              <polyline points="22,6 12,13 2,6"></polyline>
            </svg>
          </span>
          <span>contact@zhiguang.com</span>
        </a>
        <a class="contact-item" href="https://www.zhiguang.com" target="_blank" rel="noopener">
          <span class="contact-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="2" y1="12" x2="22" y2="12"></line>
              <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
            </svg>
          </span>
          <span>www.zhiguang.com</span>
        </a>
      </div>
    </section>
  </InfoLayout>
</template>

<script setup>
import InfoLayout from '@/components/content/InfoLayout.vue'

const features = [
  {
    title: '纯粹的内容',
    desc: '拒绝标题党与低质信息，每一篇推荐都经过严格筛选',
    tone: 'blue',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path></svg>'
  },
  {
    title: '优雅的编辑',
    desc: '沉浸式的写作体验，让技术表达回归本质',
    tone: 'violet',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"></path><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"></path></svg>'
  },
  {
    title: '同频的交流',
    desc: '与真正热爱技术的人建立连接，而非泛泛之交',
    tone: 'amber',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>'
  },
  {
    title: '时间的复利',
    desc: '持续记录与输出，让思考产生复利效应',
    tone: 'teal',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg>'
  }
]

const stack = [
  {
    title: '后端',
    desc: 'Java 17 + Spring Boot 3.x + MySQL 8.0 + Redis',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"></path><path d="M2 17l10 5 10-5"></path><path d="M2 12l10 5 10-5"></path></svg>'
  },
  {
    title: '前端',
    desc: 'Vue 3 + Vite + Element Plus + Pinia',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="3" width="20" height="14" rx="2"></rect><line x1="8" y1="21" x2="16" y2="21"></line><line x1="12" y1="17" x2="12" y2="21"></line></svg>'
  },
  {
    title: '安全',
    desc: 'Spring Security + JWT Token',
    icon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>'
  }
]
</script>

<style lang="scss" scoped>
.about-section {
  padding-top: 28px;
  padding-bottom: 28px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.feature-card {
  position: relative;
  padding: 22px 18px;
  border-radius: 18px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  transition: transform var(--transition-fast), border-color var(--transition-fast), box-shadow var(--transition-fast);

  &:hover {
    transform: translateY(-3px);
    border-color: var(--content-line-strong);
    box-shadow: var(--content-shadow);
  }

  &.blue { --tone: #1a73e8; --tone-soft: rgba(26, 115, 232, 0.1); }
  &.violet { --tone: #8b5cf6; --tone-soft: rgba(139, 92, 246, 0.1); }
  &.amber { --tone: #f59e0b; --tone-soft: rgba(245, 158, 11, 0.12); }
  &.teal { --tone: #14b8a6; --tone-soft: rgba(20, 184, 166, 0.12); }

  h3 {
    margin: 14px 0 8px;
    color: var(--content-ink);
    font-size: 15.5px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--content-ink-2);
    font-size: 13.5px;
    line-height: 1.7;
  }
}

.feature-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 13px;
  color: var(--tone);
  background: var(--tone-soft);
}

.tech-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.tech-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 18px;
  border-radius: 16px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);

  h3 {
    margin: 0 0 5px;
    color: var(--content-ink);
    font-size: 14.5px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--content-ink-2);
    font-size: 13px;
    line-height: 1.65;
  }
}

.tech-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  color: var(--color-primary);
  background: var(--content-chip);
  border: 1px solid var(--content-chip-line);
}

.contact-desc {
  margin: -6px 0 18px;
  color: var(--content-ink-2);
  font-size: 14px;
}

.contact-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.contact-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 18px 10px 12px;
  border-radius: 999px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  color: var(--content-ink-2);
  font-size: 14px;
  text-decoration: none;
  transition: all var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    color: var(--content-ink);
    transform: translateY(-1px);
  }
}

.contact-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: var(--color-primary);
  background: var(--content-chip);
}

@media (max-width: 1024px) {
  .features-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .tech-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .features-grid {
    grid-template-columns: 1fr;
  }

  .contact-list {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
