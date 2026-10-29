<template>
  <InfoLayout
    eyebrow="Guide"
    title="使用指南"
    description="四步上手纸域空间：注册、写作、发现与互动。"
    :particles="false"
  >
    <div class="ln-panel ln-panel-pad ln-narrow guide-panel">
      <div
        v-for="(step, index) in steps"
        :key="step.title"
        class="guide-step"
      >
        <div class="step-rail">
          <span class="step-number">{{ index + 1 }}</span>
          <span v-if="index < steps.length - 1" class="step-line"></span>
        </div>
        <div class="step-body">
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
        </div>
      </div>
    </div>
  </InfoLayout>
</template>

<script setup>
import InfoLayout from '@/components/content/InfoLayout.vue'

const steps = [
  {
    title: '注册与登录',
    desc: '使用邮箱注册账号，登录后即可发布文章、收藏内容和与其他用户互动。'
  },
  {
    title: '发布文章',
    desc: '点击右上角的“写文章”按钮，进入编辑器。支持 Markdown 语法，可插入代码块、图片与链接。'
  },
  {
    title: '发现内容',
    desc: '在首页或专栏页浏览推荐文章，也可以通过搜索找到你感兴趣的专题或作者。'
  },
  {
    title: '互动与收藏',
    desc: '给喜欢的文章点赞、评论，或将它们加入个人收藏夹，方便日后回顾。'
  }
]
</script>

<style lang="scss" scoped>
.guide-panel {
  padding-top: clamp(28px, 4vw, 40px);
  padding-bottom: clamp(28px, 4vw, 40px);
}

.guide-step {
  display: flex;
  gap: 20px;
}

.step-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.step-number {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 13px;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  background: linear-gradient(135deg, #1a73e8, #6f5cf6 62%, #a855f7);
  box-shadow: 0 10px 22px rgba(70, 90, 220, 0.28), inset 0 1px 0 rgba(255, 255, 255, 0.35);
}

.step-line {
  flex: 1;
  width: 2px;
  min-height: 26px;
  margin: 10px 0;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--content-line-strong), var(--content-line));
}

.step-body {
  flex: 1;
  min-width: 0;
  padding-bottom: 30px;

  h3 {
    margin: 6px 0 8px;
    color: var(--content-ink);
    font-size: 17px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--content-ink-2);
    font-size: 14.5px;
    line-height: 1.8;
  }
}

.guide-step:last-child .step-body {
  padding-bottom: 0;
}

@media (max-width: 560px) {
  .guide-step {
    gap: 14px;
  }

  .step-number {
    width: 32px;
    height: 32px;
    border-radius: 10px;
    font-size: 13px;
  }
}
</style>
