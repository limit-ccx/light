<template>
  <InfoLayout
    eyebrow="Terms"
    title="用户协议"
    description="使用纸域空间前，请阅读并同意以下条款。"
    :particles="false"
  >
    <article class="ln-panel ln-panel-pad ln-narrow ln-prose">
      <h2>接受条款</h2>
      <p>
        当你注册、登录或使用纸域空间提供的服务时，即表示你已阅读、理解并同意遵守本协议的所有内容。
      </p>

      <h2>账号与安全</h2>
      <p>
        你应对自己的账号和密码安全负责，不得将账号转让、出借或共享给他人。因你保管不善导致的损失由你自行承担。
      </p>

      <h2>内容规范</h2>
      <p>
        你在平台发布的内容应当合法、真实，不得含有虚假信息、侵权内容、暴力、仇恨言论或违反公序良俗的信息。
      </p>

      <h2>知识产权</h2>
      <p>
        你发布的内容版权归你所有，但授予纸域空间在平台范围内展示、传播和存储的权利。请尊重他人的知识产权。
      </p>

      <h2>服务变更与终止</h2>
      <p>
        我们保留根据运营需要修改或终止部分服务的权利。如你违反本协议，我们有权暂停或终止你的账号。
      </p>

      <h2>其他</h2>
      <p>
        本协议最终解释权归纸域空间所有。如有争议，双方应友好协商解决。
      </p>
    </article>
  </InfoLayout>
</template>

<script setup>
import InfoLayout from '@/components/content/InfoLayout.vue'
</script>
