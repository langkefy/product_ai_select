<template>
  <el-card class="stat-card" shadow="hover">
    <div class="inner">
      <div>
        <div class="label">{{ title }}</div>
        <div class="value">{{ value }}<span class="unit">{{ unit }}</span></div>
        <div v-if="trend !== undefined" class="trend" :class="trend >= 0 ? 'up' : 'down'">
          {{ trend >= 0 ? '↑' : '↓' }} {{ Math.abs(trend) }}%
        </div>
      </div>
      <el-icon :size="44" :color="color"><component :is="icon" /></el-icon>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  title: String,
  value: [String, Number],
  unit: { type: String, default: '' },
  icon: String,
  color: { type: String, default: '#409eff' },
  trend: Number,
})
</script>

<style scoped>
.stat-card { border-radius: 8px; }
.inner { display: flex; justify-content: space-between; align-items: center; }
.label { font-size: 13px; color: #999; margin-bottom: 6px; }
.value { font-size: 28px; font-weight: 700; color: #222; }
.unit { font-size: 13px; color: #999; margin-left: 4px; }
.trend { font-size: 12px; margin-top: 4px; }
.up { color: #67c23a; }
.down { color: #f56c6c; }
</style>

