<template>
  <InfoLayout
    eyebrow="Join Us"
    title="加入团队"
    description="和一群认真的人，一起做点有意义的事。"
    :particles="false"
  >
    <article class="ln-panel ln-panel-pad ln-narrow ln-prose">
      <h2>我们是谁</h2>
      <p>
        纸域空间是一支热爱内容与技术的小团队。我们相信深度表达的价值，也在探索如何让知识传播得更优雅、更高效。
      </p>

      <h2>开放的岗位</h2>
      <div class="job-list">
        <div v-for="job in jobs" :key="job.title" class="job-card">
          <span class="job-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="7" width="20" height="14" rx="2"></rect>
              <path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"></path>
            </svg>
          </span>
          <div>
            <h3>{{ job.title }}</h3>
            <p>{{ job.desc }}</p>
          </div>
        </div>
      </div>

      <h2>加入我们</h2>
      <p>
        如果你对这些方向感兴趣，欢迎发送简历与个人作品到
        <a class="ln-link" href="mailto:join@zhiguang.com">join@zhiguang.com</a>，
        我们会在一周内回复。
      </p>
    </article>
  </InfoLayout>
</template>

<script setup>
import InfoLayout from '@/components/content/InfoLayout.vue'

const jobs = [
  { title: '前端工程师', desc: '熟悉 Vue 3 生态，热爱细节与交互体验。' },
  { title: '后端工程师', desc: '熟悉 Spring Boot，关注系统稳定性与可维护性。' },
  { title: '内容运营', desc: '对技术、阅读、写作有热情，愿意挖掘好内容。' }
]
</script>

<style lang="scss" scoped>
.job-list {
  display: grid;
  gap: 12px;
  margin: 18px 0 28px;
}

.job-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  transition: border-color var(--transition-fast), transform var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    transform: translateY(-1px);
  }

  h3 {
    margin: 0 0 6px;
    color: var(--content-ink);
    font-size: 15.5px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--content-ink-2);
    font-size: 14px;
    line-height: 1.7;
  }
}

.job-icon {
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
</style>
