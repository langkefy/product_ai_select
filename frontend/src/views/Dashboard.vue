<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card" :style="`--c1:${card.c1};--c2:${card.c2}`">
        <div class="stat-icon-wrap"><el-icon :size="28"><component :is="card.icon" /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ stats[card.key] ?? '-' }}<span class="stat-unit">{{ card.unit }}</span></div>
        </div>
        <div class="stat-bg-circle"></div>
      </div>
    </div>

    <!-- 图表行 -->
    <div class="chart-row">
      <div class="chart-card main-chart">
        <div class="section-header">
          <span class="section-title">📈 近7天采集趋势</span>
        </div>
        <div ref="lineChartRef" style="height:260px;"></div>
      </div>
      <div class="chart-card side-chart">
        <div class="section-header">
          <span class="section-title">🎯 AI决策分布</span>
        </div>
        <div ref="pieChartRef" style="height:260px;"></div>
      </div>
    </div>

    <!-- TOP5 -->
    <div class="top5-card">
      <div class="section-header">
        <span class="section-title">🏆 AI评分 TOP5</span>
      </div>
      <div v-if="top5.length === 0" class="empty-tip">暂无数据，请先采集商品并进行AI分析</div>
      <div v-for="(item, i) in top5" :key="item.id" class="top-item">
        <div class="top-rank" :class="'r'+(i+1)">{{ i+1 }}</div>
        <el-image :src="item.imageUrl" class="top-img" fit="cover">
          <template #error><div class="top-img-err">📦</div></template>
        </el-image>
        <div class="top-info">
          <router-link :to="`/product/${item.id}`" class="top-title">{{ item.title }}</router-link>
          <div class="top-meta">
            <el-tag size="small" style="margin-right:6px;">{{ item.platform }}</el-tag>
            <span>¥{{ item.price }}</span>
            <span style="margin:0 6px;color:#ddd;">|</span>
            <span>销量 {{ item.sales }}</span>
            <el-tag v-if="item.verdict" size="small" :type="verdictType(item.verdict)" style="margin-left:8px;">{{ item.verdict }}</el-tag>
          </div>
        </div>
        <div class="top-score">
          <el-progress type="circle" :percentage="item.aiScore||0" :width="52" :stroke-width="6" :color="aiColor(item.aiScore)" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { trendApi } from '@/api'

const stats = ref({})
const top5 = ref([])
const lineChartRef = ref(null)
const pieChartRef = ref(null)

const statCards = [
  { key:'totalProducts',  label:'总商品数',   unit:'件', icon:'Goods',      c1:'#409eff', c2:'#1677ff' },
  { key:'todayCollected', label:'今日新增',   unit:'件', icon:'Download',   c1:'#67c23a', c2:'#389e0d' },
  { key:'avgAiScore',     label:'平均AI评分', unit:'分', icon:'StarFilled', c1:'#e6a23c', c2:'#d48806' },
  { key:'activeTasks',    label:'活跃任务',   unit:'个', icon:'Loading',    c1:'#9c59f7', c2:'#7c3aed' },
]

const aiColor = s => !s ? '#eee' : s>=80 ? '#67c23a' : s>=60 ? '#e6a23c' : '#f56c6c'
const verdictType = v => v==='上架'?'success':v==='测试'?'warning':v==='放弃'?'danger':'info'

async function loadDashboard() {
  const res = await trendApi.dashboard()
  if (res.code === 200) {
    stats.value = res.data
    top5.value = res.data.top5Products || []
    await nextTick()
    initLineChart(res.data.weeklyTrend || [])
    initPieChart(res.data.verdictDistribution || [])
  }
}

function initLineChart(weeklyTrend) {
  const chart = echarts.init(lineChartRef.value)
  const countMap = {}
  weeklyTrend.forEach(item => {
    const d = item.collectDate || item.collect_date || ''
    if (d) countMap[d.slice(0,10)] = item.count
  })
  const dates = [], values = []
  for (let i=6;i>=0;i--) {
    const d = new Date(); d.setDate(d.getDate()-i)
    const key = d.toISOString().slice(0,10)
    dates.push(key.slice(5))
    values.push(countMap[key] || 0)
  }
  chart.setOption({
    tooltip: { trigger:'axis', backgroundColor:'rgba(26,26,62,0.9)', borderColor:'transparent', textStyle:{color:'#fff'} },
    xAxis: { type:'category', data:dates, axisLine:{lineStyle:{color:'#eee'}}, axisLabel:{color:'#999'} },
    yAxis: { type:'value', splitLine:{lineStyle:{color:'#f0f0f0'}}, axisLabel:{color:'#999'} },
    series: [{
      name:'采集商品数', type:'line', smooth:true,
      data: values,
      itemStyle: { color:'#409eff' },
      areaStyle: { color: { type:'linear', x:0,y:0,x2:0,y2:1, colorStops:[{offset:0,color:'rgba(64,158,255,0.3)'},{offset:1,color:'rgba(64,158,255,0)'}] } },
      lineStyle: { width:3 },
      symbol:'circle', symbolSize:6,
      label: { show:true, position:'top', color:'#409eff', fontSize:11 },
    }],
    grid: { left:36, right:16, top:16, bottom:28 },
  })
  window.addEventListener('resize', () => chart.resize())
}

function initPieChart(distribution) {
  const chart = echarts.init(pieChartRef.value)
  const colorMap = { '上架':'#67c23a','测试':'#e6a23c','放弃':'#f56c6c','未分析':'#c0c4cc' }
  const data = distribution.map(d => ({ name:d.verdict||'未分析', value:d.count, itemStyle:{color:colorMap[d.verdict||'未分析']||'#409eff'} }))
  chart.setOption({
    tooltip: { trigger:'item', formatter:'{b}: {c}件 ({d}%)', backgroundColor:'rgba(26,26,62,0.9)', borderColor:'transparent', textStyle:{color:'#fff'} },
    legend: { bottom:4, type:'scroll', textStyle:{color:'#666'} },
    series: [{
      type:'pie', radius:['38%','68%'], center:['50%','44%'],
      data: data.length ? data : [{name:'暂无数据',value:1,itemStyle:{color:'#eee'}}],
      label: { formatter:'{b}\n{c}件', fontSize:12 },
      itemStyle: { borderRadius:6, borderWidth:2, borderColor:'#fff' },
    }],
  })
  window.addEventListener('resize', () => chart.resize())
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 20px; }

/* 统计卡片 */
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card {
  background: linear-gradient(135deg, var(--c1), var(--c2));
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.12);
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover { transform: translateY(-3px); box-shadow: 0 8px 28px rgba(0,0,0,0.18); }
.stat-icon-wrap { width:56px;height:56px;border-radius:14px;background:rgba(255,255,255,0.22);display:flex;align-items:center;justify-content:center;color:#fff;flex-shrink:0; }
.stat-body { flex: 1; }
.stat-label { font-size:13px;color:rgba(255,255,255,0.82);margin-bottom:4px; }
.stat-value { font-size:30px;font-weight:700;color:#fff;line-height:1; }
.stat-unit { font-size:13px;margin-left:3px;color:rgba(255,255,255,0.75); }
.stat-bg-circle { position:absolute;right:-20px;top:-20px;width:90px;height:90px;border-radius:50%;background:rgba(255,255,255,0.1); }

/* 图表区 */
.chart-row { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.chart-card { background:#fff;border-radius:16px;padding:20px;box-shadow:var(--card-shadow); }
.section-header { margin-bottom:16px; }
.section-title { font-size:15px;font-weight:700;color:#1a1a2e; }

/* TOP5 */
.top5-card { background:#fff;border-radius:16px;padding:20px;box-shadow:var(--card-shadow); }
.empty-tip { text-align:center;color:#bbb;padding:40px 0;font-size:14px; }
.top-item { display:flex;align-items:center;gap:14px;padding:12px 0;border-bottom:1px solid #f5f7fa; }
.top-item:last-child { border-bottom:none; }
.top-rank { width:28px;height:28px;border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:13px;font-weight:700;background:#f0f2f7;color:#888;flex-shrink:0; }
.r1 { background:linear-gradient(135deg,#ffd700,#ffb300);color:#fff; }
.r2 { background:linear-gradient(135deg,#b0b8c8,#8a9ab5);color:#fff; }
.r3 { background:linear-gradient(135deg,#e8a96a,#c97d3c);color:#fff; }
.top-img { width:48px;height:48px;border-radius:10px;flex-shrink:0;object-fit:cover; }
.top-img-err { width:48px;height:48px;border-radius:10px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;font-size:22px; }
.top-info { flex:1;min-width:0; }
.top-title { font-size:13px;font-weight:600;color:#1a1a2e;text-decoration:none;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;display:block; }
.top-title:hover { color:#409eff; }
.top-meta { font-size:12px;color:#999;margin-top:4px;display:flex;align-items:center;flex-wrap:wrap;gap:2px; }
.top-score { flex-shrink:0; }

@media (max-width: 767px) {
  .stat-grid { grid-template-columns: repeat(2,1fr); gap:12px; }
  .chart-row { grid-template-columns: 1fr; }
  .stat-value { font-size:24px; }
}
</style>
