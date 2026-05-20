<template>
  <div v-if="product" class="detail-page">
    <!-- 面包屑 -->
    <el-breadcrumb separator="/" style="margin-bottom:16px;">
      <el-breadcrumb-item :to="{ path: '/ranking' }">商品排行</el-breadcrumb-item>
      <el-breadcrumb-item>商品详情</el-breadcrumb-item>
    </el-breadcrumb>

    <el-row :gutter="20">
      <!-- 左侧：商品基础信息 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <el-image
            :src="product.imageUrl"
            style="width:100%;height:240px;border-radius:6px;"
            fit="contain"
          >
            <template #error>
              <div style="width:100%;height:240px;display:flex;align-items:center;justify-content:center;font-size:60px;background:#f5f5f5;border-radius:6px;">📦</div>
            </template>
          </el-image>
          <div style="margin-top:16px;">
            <div class="product-title">{{ product.title }}</div>
            <!-- AI新标题 -->
            <div v-if="product.aiTitle" style="margin-top:8px;padding:8px 10px;background:linear-gradient(135deg,#e8f4fd,#d6eaf8);border-radius:8px;border-left:3px solid #409eff;">
              <div style="font-size:11px;color:#409eff;font-weight:600;margin-bottom:3px;">🤖 AI优化标题</div>
              <div style="font-size:13px;color:#222;font-weight:500;line-height:1.5;">{{ product.aiTitle }}</div>
            </div>
            <el-row :gutter="10" style="margin-top:12px;">
              <el-col :span="12">
                <div class="info-label">平台</div>
                <el-tag>{{ platformMap[product.platform] || product.platform }}</el-tag>
              </el-col>
              <el-col :span="12">
                <div class="info-label">品类</div>
                <span>{{ product.category || '-' }}</span>
              </el-col>
            </el-row>
            <el-row :gutter="10" style="margin-top:12px;">
              <el-col :span="12">
                <div class="info-label">价格</div>
                <span class="price">¥{{ product.price }}</span>
              </el-col>
              <el-col :span="12">
                <div class="info-label">30天销量</div>
                <span class="sales">{{ product.sales }}</span>
              </el-col>
            </el-row>
            <div style="margin-top:12px;">
              <div class="info-label">商品评分</div>
              <el-rate :model-value="parseFloat(product.rating) || 0" disabled show-score />
            </div>
            <div style="margin-top:12px;" v-if="product.detailUrl">
              <el-button type="primary" link :href="product.detailUrl" target="_blank">
                查看原链接 →
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：AI分析 + 排名 -->
      <el-col :span="16">
        <!-- AI评分卡 -->
        <el-card shadow="hover" style="margin-bottom:16px;">
          <template #header>
            <span>🤖 AI分析结果</span>
            <el-button size="small" style="float:right;" @click="runAI" :loading="analyzing">重新分析</el-button>
          </template>
          <el-row :gutter="20" align="middle">
            <el-col :span="6" style="text-align:center;">
              <el-progress type="circle" :percentage="product.aiScore || 0"
                :color="aiColor(product.aiScore)" :width="90"
                :stroke-width="8" />
              <div style="margin-top:8px;font-size:12px;color:#666;">AI综合评分</div>
            </el-col>
            <el-col :span="18">
              <div style="white-space:pre-wrap;line-height:1.8;color:#444;font-size:13px;">
                {{ product.aiAnalysis || '暂无AI分析，请点击"重新分析"' }}
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 排名徽章 -->
        <el-card shadow="hover" style="margin-bottom:16px;">
          <template #header>📊 排名位置</template>
          <el-row :gutter="20">
            <el-col :span="8" class="rank-badge-col">
              <div class="rank-badge today">
                <div class="rank-num">{{ product.todayRank || '-' }}</div>
                <div class="rank-label">今日排名</div>
              </div>
            </el-col>
            <el-col :span="8" class="rank-badge-col">
              <div class="rank-badge week">
                <div class="rank-num">{{ product.weekRank || '-' }}</div>
                <div class="rank-label">本周排名</div>
              </div>
            </el-col>
            <el-col :span="8" class="rank-badge-col">
              <div class="rank-badge month">
                <div class="rank-num">{{ product.monthRank || '-' }}</div>
                <div class="rank-label">本月排名</div>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 供应商服务能力 -->
        <el-card shadow="hover" style="margin-bottom:16px;">
          <template #header>🚚 供应商服务能力</template>
          <el-row :gutter="12">
            <el-col :span="6" style="text-align:center;" v-for="item in serviceItems" :key="item.label">
              <div :class="['service-item', item.value === 1 ? 'service-yes' : item.value === 0 ? 'service-no' : 'service-unknown']">
                <div style="font-size:20px;">{{ item.icon }}</div>
                <div style="font-size:12px;margin-top:4px;font-weight:600;">{{ item.label }}</div>
                <el-tag :type="item.value === 1 ? 'success' : item.value === 0 ? 'danger' : 'info'" size="small" style="margin-top:4px;">
                  {{ item.value === 1 ? '支持' : item.value === 0 ? '不支持' : '未知' }}
                </el-tag>
              </div>
            </el-col>
          </el-row>
        </el-card>

        <!-- 趋势图 -->
        <el-card shadow="hover">
          <template #header>
            <span>📈 近30天销量趋势</span>
            <el-radio-group v-model="trendDim" size="small" style="float:right;" @change="renderTrend">
              <el-radio-button label="sales">销量</el-radio-button>
              <el-radio-button label="views">浏览量</el-radio-button>
              <el-radio-button label="rank">排名</el-radio-button>
            </el-radio-group>
          </template>
          <div v-if="!product.trendStats?.length" style="text-align:center;color:#999;padding:40px 0;">暂无趋势数据</div>
          <div v-else ref="trendRef" style="height:220px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
  <div v-else-if="loading" v-loading="true" style="height:400px;"></div>
  <el-empty v-else description="商品不存在" />
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api'

const route = useRoute()
const product = ref(null)
const loading = ref(false)
const analyzing = ref(false)
const trendDim = ref('sales')
const trendRef = ref(null)
let chartInst = null

const platformMap = { taobao: '淘宝', jd: '京东', pdd: '拼多多', '1688': '1688' }

const serviceItems = computed(() => {
  if (!product.value) return []
  return [
    { label: '48h发货', icon: '⚡', value: product.value.deliveryIn48h },
    { label: '全包售后', icon: '🛡️', value: product.value.fullAfterSales },
    { label: '抖音面单', icon: '🎵', value: product.value.douyinSheetSupport },
    { label: '库存同步', icon: '🔄', value: product.value.autoSyncStock },
  ]
})

function aiColor(score) {
  if (!score) return '#eee'
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await productApi.detail(route.params.id)
    if (res.code === 200) {
      product.value = res.data
      await nextTick()
      if (res.data.trendStats?.length) renderTrend()
    }
  } finally {
    loading.value = false
  }
}

function renderTrend() {
  const stats = product.value?.trendStats
  if (!stats?.length || !trendRef.value) return
  if (!chartInst) chartInst = echarts.init(trendRef.value)
  const dimLabel = { sales: '销量', views: '浏览量', rank: '排名' }
  chartInst.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: stats.map(d => d.statDate) },
    yAxis: { type: 'value', name: dimLabel[trendDim.value] },
    series: [{
      name: dimLabel[trendDim.value],
      type: 'line',
      smooth: true,
      areaStyle: { opacity: 0.15 },
      data: stats.map(d => d[trendDim.value]),
      itemStyle: { color: '#409eff' },
    }],
  })
  window.addEventListener('resize', () => chartInst?.resize())
}

async function runAI() {
  analyzing.value = true
  try {
    const res = await productApi.aiAnalyze({ productId: product.value.id })
    if (res.code === 200) {
      Object.assign(product.value, res.data)
      ElMessage.success('AI分析完成')
    }
  } finally {
    analyzing.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-page { padding: 4px; }
.product-title { font-size: 15px; font-weight: 600; color: #222; line-height: 1.5; }
.info-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.price { font-size: 20px; font-weight: 700; color: #f56c6c; }
.sales { font-size: 18px; font-weight: 600; color: #409eff; }
.rank-badge-col { text-align: center; }
.rank-badge { padding: 16px; border-radius: 10px; }
.rank-badge.today { background: linear-gradient(135deg, #667eea, #764ba2); }
.rank-badge.week  { background: linear-gradient(135deg, #f093fb, #f5576c); }
.rank-badge.month { background: linear-gradient(135deg, #4facfe, #00f2fe); }
.rank-num  { font-size: 32px; font-weight: 700; color: #fff; }
.rank-label { font-size: 12px; color: rgba(255,255,255,0.85); margin-top: 4px; }
.service-item { padding: 12px 6px; border-radius: 10px; display: flex; flex-direction: column; align-items: center; }
.service-yes     { background: #f0f9eb; }
.service-no      { background: #fef0f0; }
.service-unknown { background: #f5f7fa; }
</style>

