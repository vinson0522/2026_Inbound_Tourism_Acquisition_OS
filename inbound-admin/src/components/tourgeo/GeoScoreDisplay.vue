<template>
  <span v-if="score == null" class="geo-score geo-score--empty">—</span>
  <span v-else class="geo-score" :style="{ color: scoreColor }">{{ score.toFixed(1) }}</span>
</template>

<script setup lang="ts">
const props = defineProps<{ score: number | null }>();

const scoreColor = computed(() => {
  if (props.score == null) return 'inherit';
  if (props.score >= 70) return 'var(--tg-score-high, #059669)';
  if (props.score >= 40) return 'var(--tg-score-mid, #d97706)';
  return 'var(--tg-score-low, #dc2626)';
});
</script>

<style scoped>
.geo-score {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.geo-score--empty {
  color: var(--tg-color-text-secondary, #6b7280);
}
</style>
