<template>
  <InfoLayout
    eyebrow="Contact"
    title="联系我们"
    description="有问题或建议？告诉我们，工作日通常会在 24 小时内回复。"
    :particles="false"
  >
    <div class="ln-panel ln-panel-pad ln-narrow contact-panel">
      <div class="contact-grid">
        <a class="contact-block" href="mailto:contact@zhiguang.com">
          <span class="contact-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
              <polyline points="22,6 12,13 2,6"></polyline>
            </svg>
          </span>
          <span class="contact-body">
            <span class="contact-label">电子邮箱</span>
            <span class="contact-value">contact@zhiguang.com</span>
          </span>
          <span class="contact-arrow">→</span>
        </a>

        <a class="contact-block" href="https://www.zhiguang.com" target="_blank" rel="noopener">
          <span class="contact-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="2" y1="12" x2="22" y2="12"></line>
              <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path>
            </svg>
          </span>
          <span class="contact-body">
            <span class="contact-label">官方网站</span>
            <span class="contact-value">www.zhiguang.com</span>
          </span>
          <span class="contact-arrow">→</span>
        </a>

        <div class="contact-block">
          <span class="contact-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
            </svg>
          </span>
          <span class="contact-body">
            <span class="contact-label">微信公众号</span>
            <span class="contact-value">LightNote</span>
          </span>
        </div>
      </div>

      <div class="contact-note">
        <h2>更快地得到回复</h2>
        <p>
          如果你遇到紧急问题，建议通过邮件详细描述，并附上截图；功能建议也可以前往
          <router-link class="ln-link" to="/feedback">建议反馈</router-link>
          提交。
        </p>
      </div>
    </div>
  </InfoLayout>
</template>

<script setup>
import InfoLayout from '@/components/content/InfoLayout.vue'
</script>

<style lang="scss" scoped>
.contact-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.contact-block {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 16px;
  border: 1px solid var(--content-line);
  background: var(--content-surface-tint);
  color: inherit;
  text-decoration: none;
  transition: border-color var(--transition-fast), transform var(--transition-fast), box-shadow var(--transition-fast);

  &:hover {
    border-color: var(--content-line-strong);
    transform: translateY(-1px);
    box-shadow: var(--content-shadow);

    .contact-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }
}

.contact-icon {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(135deg, #1a73e8, #6f5cf6 62%, #a855f7);
  box-shadow: 0 10px 20px rgba(70, 90, 220, 0.24);
}

.contact-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.contact-label {
  color: var(--content-ink-3);
  font-size: 12px;
  letter-spacing: 0.06em;
}

.contact-value {
  color: var(--content-ink);
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-arrow {
  color: var(--color-primary);
  font-size: 16px;
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--transition-fast);
}

.contact-note {
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px solid var(--content-line);

  h2 {
    margin: 0 0 8px;
    color: var(--content-ink);
    font-size: 15px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--content-ink-2);
    font-size: 14px;
    line-height: 1.8;
  }
}
</style>
