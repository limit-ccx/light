<template>
  <InfoLayout
    eyebrow="Privacy"
    title="隐私政策"
    description="纸域空间重视你的隐私。本政策说明我们如何收集、使用和保护你的个人信息。"
    :particles="false"
  >
    <template #meta>
      <span class="meta-dot"></span>
      <span>最后更新：2024 年 1 月 1 日</span>
    </template>

    <article class="ln-panel ln-panel-pad ln-narrow ln-prose">
      <h2>我们收集的信息</h2>
      <p>
        为了提供服务，我们可能会收集你的邮箱地址、昵称、头像、设备信息以及你在平台上的操作记录（如发布、收藏、评论等）。
      </p>

      <h2>信息的使用</h2>
      <p>
        我们使用这些信息来识别用户身份、提供个性化推荐、改进产品体验，并在必要时与你取得联系。
      </p>

      <h2>信息的保护</h2>
      <p>
        我们采取合理的技术和管理措施保护你的数据安全，不会将你的个人信息出售或非法提供给第三方。
      </p>

      <h2>Cookie 与本地存储</h2>
      <p>
        我们使用 Cookie 和浏览器本地存储来保存登录状态与偏好设置，你可以选择清除它们，但这可能影响部分功能。
      </p>

      <h2>联系我们</h2>
      <p>
        如果你对本隐私政策有任何疑问，请通过
        <a class="ln-link" href="mailto:contact@zhiguang.com">contact@zhiguang.com</a>
        联系我们。
      </p>
    </article>
  </InfoLayout>
</template>

<script setup>
import InfoLayout from '@/components/content/InfoLayout.vue'
</script>

<style lang="scss" scoped>
.meta-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--content-ink-3);
}
</style>
