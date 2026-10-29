<template>
  <footer class="footer">
    <div class="footer-container container">
      <div class="footer-main">
        <div class="footer-brand">
          <div class="footer-logo">
            <span class="logo-icon">L</span>
            <span class="logo-text">纸域空间</span>
          </div>
          <p class="footer-desc">一个专注于知识分享与交流的平台，让知识发光发热。</p>
        </div>

        <div class="footer-links">
          <div class="footer-column">
          <h4 class="footer-title">关于</h4>
          <ul class="footer-list">
            <li><router-link to="/about">关于我们</router-link></li>
            <li><router-link to="/join-us">加入团队</router-link></li>
            <li><router-link to="/contact">联系方式</router-link></li>
          </ul>
        </div>

        <div class="footer-column">
          <h4 class="footer-title">帮助</h4>
          <ul class="footer-list">
            <li><router-link to="/guide">使用指南</router-link></li>
            <li><router-link to="/faq">常见问题</router-link></li>
            <li><router-link to="/feedback">建议反馈</router-link></li>
          </ul>
        </div>

        <div class="footer-column">
          <h4 class="footer-title">关注</h4>
          <ul class="footer-list">
            <li><router-link to="/contact">微信公众号</router-link></li>
            <li><a href="https://weibo.com" target="_blank" rel="noopener">微博</a></li>
            <li><a href="https://zhihu.com" target="_blank" rel="noopener">知乎</a></li>
          </ul>
        </div>
        </div>
      </div>

      <div class="footer-bottom">
        <p class="copyright">© 2024 纸域空间. All rights reserved.</p>
        <div class="footer-legal">
          <router-link to="/privacy">隐私政策</router-link>
          <router-link to="/terms">用户协议</router-link>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.footer {
  background: var(--color-bg);
  border-top: 1px solid var(--color-border);
  color: var(--color-text-muted);
  margin-top: auto;
}

.footer-container {
  padding-top: 48px;
  padding-bottom: 24px;
}

.footer-main {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 48px;
  padding-bottom: 32px;
  border-bottom: 1px solid var(--color-border);
}

.footer-brand {
  .footer-logo {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .logo-icon {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, var(--color-primary), #6B9BD2);
      color: white;
      border-radius: var(--radius-md);
      @include flex-center;
      font-size: 18px;
      font-weight: 600;
    }

    .logo-text {
      font-family: var(--font-artistic);
      font-size: 22px;
      font-weight: 400;
      letter-spacing: 0.06em;
      color: var(--color-text);
    }
  }

  .footer-desc {
    font-size: 14px;
    line-height: 1.6;
    color: var(--color-text-muted);
  }
}

.footer-links {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
}

.footer-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.footer-list {
  li {
    margin-bottom: 8px;
  }

  a {
    font-size: 14px;
    color: var(--color-text-muted);
    transition: color var(--transition-fast);

    &:hover {
      color: var(--color-primary);
    }
  }
}

.footer-bottom {
  @include flex-between;
  padding-top: 24px;
  font-size: 13px;
}

.copyright {
  color: var(--color-text-muted);
}

.footer-legal {
  display: flex;
  gap: 24px;

  a {
    color: var(--color-text-muted);
    transition: color var(--transition-fast);

    &:hover {
      color: var(--color-primary);
    }
  }
}

@media (max-width: 768px) {
  .footer-main {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .footer-links {
    grid-template-columns: repeat(2, 1fr);
  }

  .footer-bottom {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }
}
</style>