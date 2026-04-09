<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  description: {
    type: String,
    default: ""
  }
});
</script>

<template>
  <article class="stat-card">
    <p class="stat-card__title">{{ title }}</p>
    <strong class="stat-card__value">{{ value }}</strong>
    <p class="stat-card__desc">{{ description }}</p>
  </article>
</template>
