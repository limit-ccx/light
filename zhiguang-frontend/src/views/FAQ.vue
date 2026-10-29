<template>
  <InfoLayout
    eyebrow="FAQ"
    title="常见问题"
    description="关于纸域空间你可能想知道的事，答案都在这里。"
    :particles="false"
  >
    <div class="ln-panel ln-panel-pad ln-narrow faq-panel">
      <details
        v-for="(item, index) in faqs"
        :key="item.q"
        class="faq-item"
        :open="index === 0"
      >
        <summary>
          <span class="faq-q">{{ item.q }}</span>
          <span class="faq-icon" aria-hidden="true"></span>
        </summary>
        <p class="faq-a">{{ item.a }}</p>
      </details>
    </div>
  </InfoLayout>
</template>

<script setup>
import InfoLayout from '@/components/content/InfoLayout.vue'

const faqs = [
  {
    q: '纸域空间是免费的吗？',
    a: '目前所有核心功能均免费开放。未来可能会推出高级会员服务，但基础阅读和发布功能将一直保持免费。'
  },
  {
    q: '我可以转载别人的文章吗？',
    a: '未经授权请勿转载他人内容。如需引用，请标注原作者与出处，并遵守相关版权规定。'
  },
  {
    q: '如何修改个人资料？',
    a: '登录后进入“个人中心”，点击设置即可修改昵称、头像与简介。'
  },
  {
    q: '文章支持哪些格式？',
    a: '编辑器支持 Markdown，可插入标题、列表、代码块、引用、图片与链接。'
  },
  {
    q: '遇到 bug 怎么办？',
    a: '可以通过“建议反馈”页面提交问题，或直接发送邮件到 contact@zhiguang.com。'
  }
]
</script>

<style lang="scss" scoped>
.faq-panel {
  padding-top: 12px;
  padding-bottom: 12px;
}

.faq-item {
  border-bottom: 1px solid var(--content-line);

  &:last-child {
    border-bottom: none;
  }

  summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 20px 4px;
    cursor: pointer;
    list-style: none;
    user-select: none;

    &::-webkit-details-marker {
      display: none;
    }

    &:hover .faq-q {
      color: var(--color-primary);
    }
  }
}

.faq-q {
  color: var(--content-ink);
  font-size: 15.5px;
  font-weight: 600;
  line-height: 1.5;
  transition: color var(--transition-fast);
}

.faq-icon {
  position: relative;
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 9px;
  border: 1px solid var(--content-chip-line);
  background: var(--content-chip);

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 10px;
    height: 1.6px;
    border-radius: 2px;
    background: var(--content-ink-3);
    transform: translate(-50%, -50%);
    transition: transform var(--transition-fast), background-color var(--transition-fast);
  }

  &::after {
    transform: translate(-50%, -50%) rotate(90deg);
  }
}

.faq-item[open] .faq-icon {
  border-color: transparent;
  background: linear-gradient(135deg, #1a73e8, #8b5cf6);

  &::before,
  &::after {
    background: #fff;
  }

  &::after {
    transform: translate(-50%, -50%) rotate(0deg);
  }
}

.faq-a {
  margin: -4px 0 0;
  padding: 0 4px 22px;
  color: var(--content-ink-2);
  font-size: 14.5px;
  line-height: 1.85;
}

@media (max-width: 560px) {
  .faq-item summary {
    padding: 16px 2px;
  }

  .faq-q {
    font-size: 14.5px;
  }
}
</style>
