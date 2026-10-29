<template>
  <div class="ln-page">
    <ParticleNetwork v-if="particles" />
    <Header />

    <main class="ln-main">
      <div class="container">
        <slot name="hero">
          <PageHero
            :eyebrow="eyebrow"
            :title="title"
            :description="description"
            :stats="stats"
          >
            <template v-if="$slots.meta" #meta>
              <slot name="meta" />
            </template>
            <template v-if="$slots.actions" #actions>
              <slot name="actions" />
            </template>
            <template v-if="$slots.aside" #aside>
              <slot name="aside" />
            </template>
          </PageHero>
        </slot>

        <div class="ln-body">
          <slot />
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import Header from '@/components/Header.vue'
import Footer from '@/components/Footer.vue'
import ParticleNetwork from '@/components/ParticleNetwork.vue'
import PageHero from '@/components/content/PageHero.vue'

defineProps({
  eyebrow: { type: String, default: '纸域空间' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  stats: { type: Array, default: () => [] },
  particles: { type: Boolean, default: true }
})
</script>
