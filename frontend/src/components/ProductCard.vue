<template>
  <el-card class="product-card" shadow="hover" @click="$emit('click', product)">
    <el-image :src="product.imageUrl" style="width:100%;height:160px;" fit="cover">
      <template #error>
        <div style="height:160px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;font-size:40px;">📦</div>
      </template>
    </el-image>
    <div class="card-body">
      <div class="title">{{ product.title }}</div>
      <div class="meta">
        <span class="price">¥{{ product.price }}</span>
        <el-tag size="small">{{ platformMap[product.platform] || product.platform }}</el-tag>
      </div>
      <div class="scores">
        <span class="sales">销量 {{ product.sales }}</span>
        <el-progress v-if="product.aiScore != null" type="circle" :percentage="product.aiScore" :width="44" />
      </div>
    </div>
  </el-card>
</template>

<script setup>
defineProps({ product: { type: Object, required: true } })
defineEmits(['click'])
const platformMap = { taobao: '淘宝', jd: '京东', pdd: '拼多多' }
</script>

<style scoped>
.product-card { cursor: pointer; }
.card-body { padding: 10px 0 0; }
.title { font-size: 13px; color: #333; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; height: 38px; margin-bottom: 8px; }
.meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.price { font-size: 16px; font-weight: 700; color: #f56c6c; }
.scores { display: flex; justify-content: space-between; align-items: center; }
.sales { font-size: 12px; color: #999; }
</style>

