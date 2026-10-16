<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  price: number | string
  originalPrice?: number | string | null
  size?: 'sm' | 'md' | 'lg' | 'xl'
  type?: 'accent' | 'danger'
}

const props = withDefaults(defineProps<Props>(), {
  originalPrice: null,
  size: 'md',
  type: 'accent'
})

const numPrice = computed(() => {
  const p = Number(props.price)
  return isNaN(p) ? 0 : p
})

const intPart = computed(() => Math.floor(numPrice.value))

const decPart = computed(() => {
  const str = numPrice.value.toFixed(2)
  const dotIndex = str.indexOf('.')
  return dotIndex !== -1 ? str.slice(dotIndex) : '.00'
})
</script>

<template>
  <div class="em-price" :class="[`em-price-${size}`, { 'em-price-danger': type === 'danger' }]">
    <span class="currency">¥</span>
    <span class="integer">{{ intPart }}</span>
    <span class="decimal">{{ decPart }}</span>
    <span v-if="originalPrice" class="origin-price">¥{{ originalPrice }}</span>
  </div>
</template>
