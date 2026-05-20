<template>
  <div class="page-wrap">
    <div class="tab-bar">
      <div class="tab-btn" :class="{active:activeTab==='product'}" @click="switchTab('product')">📈 商品趋势</div>
      <div class="tab-btn" :class="{active:activeTab==='category'}" @click="switchTab('category')">📊 品类趋势</div>
    </div>

    <!-- 商品趋势 -->
    <template v-if="activeTab==='product'">
      <div class="filter-card">
        <div class="filter-title">查询条件</div>
        <div class="filter-row">
          <div class="filter-item">
            <div class="filter-label">选择商品</div>
            <div style="display:flex;align-items:center;gap:8px;">
              <div v-if="selectedProduct" style="display:flex;align-items:center;gap:6px;padding:4px 10px;background:#f5f7fa;border-radius:8px;border:1px solid #e4e7ed;max-width:280px;">
                <el-image :src="selectedProduct.imageUrl" style="width:28px;height:28px;border-radius:4px;flex-shrink:0;" fit="cover">
                  <template #error><span>📦</span></template>
                </el-image>
                <span style="font-size:12px;color:#222;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:180px;">{{ selectedProduct.title }}</span>
                <el-icon style="cursor:pointer;color:#c0c4cc;flex-shrink:0;" @click="clearProduct"><Close /></el-icon>
              </div>
              <span v-else style="font-size:13px;color:#bbb;">未选择商品</span>
              <el-button size="small" @click="openPickerDialog">选择商品</el-button>
            </div>
          </div>
          <div class="filter-item">
            <div class="filter-label">日期范围</div>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" size="small" style="width:240px" />
          </div>
          <div class="filter-item">
            <div class="filter-label">维度</div>
            <el-select v-model="query.dimension" size="small" style="width:100px">
              <el-option label="销量" value="sales" /><el-option label="浏览量" value="views" /><el-option label="排名" value="rank" />
            </el-select>
          </div>
          <el-button type="primary" size="small" @click="loadTrend" :loading="trendLoading">🔍 查询</el-button>
        </div>
      </div>
      <div class="chart-card">
        <div class="section-title">商品趋势图</div>
        <div v-if="!hasTrend && !trendLoading" class="empty-chart">请输入商品ID并点击查询</div>
        <div v-if="trendLoading" class="empty-chart" style="color:#409eff;">
          <el-icon class="is-loading" style="font-size:22px;margin-right:8px;"><Loading /></el-icon>加载中...
        </div>
        <div ref="chartRef" style="height:360px;" v-show="hasTrend"></div>
      </div>
    </template>

    <!-- 品类趋势 -->
    <template v-if="activeTab==='category'">
      <div class="filter-card">
        <div class="filter-title">查询条件</div>
        <div class="filter-row">
          <div class="filter-item">
            <div class="filter-label">品类</div>
            <el-select v-model="catQuery.category" placeholder="全部品类" clearable size="small" style="width:140px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <div class="filter-item">
            <div class="filter-label">时间范围</div>
            <el-select v-model="catQuery.days" size="small" style="width:100px">
              <el-option label="近7天" :value="7" /><el-option label="近30天" :value="30" /><el-option label="近90天" :value="90" />
            </el-select>
          </div>
          <div class="filter-item">
            <div class="filter-label">指标</div>
            <el-select v-model="catQuery.metric" size="small" style="width:130px">
              <el-option label="平均AI评分" value="avgAiScore" /><el-option label="平均销量" value="avgSales" /><el-option label="商品数量" value="productCount" />
            </el-select>
          </div>
          <el-button type="primary" size="small" @click="loadCategoryTrend" :loading="catLoading">🔍 查询</el-button>
        </div>
      </div>
      <div class="chart-card">
        <div class="section-title">品类趋势图</div>
        <div v-if="!hasCatTrend && !catLoading" class="empty-chart">请选择品类并点击查询</div>
        <div v-if="catLoading" class="empty-chart" style="color:#409eff;">
          <el-icon class="is-loading" style="font-size:22px;margin-right:8px;"><Loading /></el-icon>加载中...
        </div>
        <div ref="catChartRef" style="height:360px;" v-show="hasCatTrend"></div>
      </div>
    </template>
  </div>

  <!-- 商品选择弹窗 -->
  <el-dialog v-model="pickerVisible" title="选择商品" width="820px" :close-on-click-modal="false">
    <div style="display:flex;gap:8px;margin-bottom:8px;">
      <el-input v-model="pickerKeyword" placeholder="输入商品名称搜索" clearable style="flex:1" @keyup.enter="pickerPage=1;searchPickerProducts()" />
      <el-select v-model="pickerPlatform" placeholder="平台" clearable style="width:110px">
        <el-option label="1688" value="1688" /><el-option label="淘宝" value="taobao" />
        <el-option label="京东" value="jd" /><el-option label="拼多多" value="pdd" />
      </el-select>
      <el-button type="primary" @click="pickerPage=1;searchPickerProducts()" :loading="pickerLoading">搜索</el-button>
    </div>
    <div style="display:flex;flex-wrap:wrap;gap:16px;align-items:center;padding:8px 12px;background:#f8faff;border-radius:8px;margin-bottom:12px;">
      <span style="font-size:12px;color:#888;font-weight:500;">供应商筛选：</span>
      <el-checkbox v-model="pickerFilter.dropShipping" @change="pickerPage=1;searchPickerProducts()">
        <span style="font-size:13px;">🚚 支持一件代发</span>
      </el-checkbox>
      <el-checkbox v-model="pickerFilter.douyinSheet" @change="pickerPage=1;searchPickerProducts()">
        <span style="font-size:13px;">🎵 抖音电子面单</span>
      </el-checkbox>
      <el-checkbox v-model="pickerFilter.delivery48h" @change="pickerPage=1;searchPickerProducts()">
        <span style="font-size:13px;">⚡ 48小时发货</span>
      </el-checkbox>
      <el-checkbox v-model="pickerFilter.freeShipping" @change="pickerPage=1;searchPickerProducts()">
        <span style="font-size:13px;">🆓 包邮</span>
      </el-checkbox>
    </div>
    <el-table
      :data="pickerProducts"
      v-loading="pickerLoading"
      stripe
      highlight-current-row
      @current-change="onPickerSelect"
      style="width:100%"
      max-height="380"
    >
      <el-table-column label="图片" width="64" align="center">
        <template #default="{ row }">
          <el-image :src="row.imageUrl" style="width:44px;height:44px;border-radius:6px;" fit="cover">
            <template #error><div style="width:44px;height:44px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;">📦</div></template>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="商品名称" min-width="220" show-overflow-tooltip />
      <el-table-column prop="platform" label="平台" width="70" align="center">
        <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="90" align="right">
        <template #default="{ row }"><span style="color:#f56c6c;font-weight:600;">¥{{ row.price }}</span></template>
      </el-table-column>
      <el-table-column prop="sales" label="销量" width="80" align="right" />
      <el-table-column label="AI决策" width="76" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.verdict" :type="row.verdict==='上架'?'success':row.verdict==='测试'?'warning':'danger'" size="small">{{ row.verdict }}</el-tag>
          <span v-else style="color:#bbb;font-size:12px;">-</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pickerPage"
      :page-size="10"
      :total="pickerTotal"
      layout="total, prev, pager, next"
      style="margin-top:10px;justify-content:flex-end;display:flex;"
      @current-change="searchPickerProducts"
    />
    <template #footer>
      <span style="font-size:13px;color:#666;margin-right:12px;">
        <template v-if="pickerCurrent">已选：<b style="color:#409eff;">{{ pickerCurrent.title }}</b></template>
        <template v-else>点击行选择商品</template>
      </span>
      <el-button @click="pickerVisible=false">取消</el-button>
      <el-button type="primary" @click="confirmPicker" :disabled="!pickerCurrent">确认选择</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Loading, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { trendApi, productApi } from '@/api'

const activeTab = ref('product')
const query = ref({ productId: null, dimension: 'sales' })
const dateRange = ref([])
const hasTrend = ref(false)
const trendLoading = ref(false)
const chartRef = ref(null)
let chartInstance = null

// 已选商品展示
const selectedProduct = ref(null)
function clearProduct() { selectedProduct.value = null; query.value.productId = null; hasTrend.value = false }

// 商品选择弹窗
const pickerVisible = ref(false)
const pickerKeyword = ref('')
const pickerPlatform = ref('')
const pickerLoading = ref(false)
const pickerProducts = ref([])
const pickerTotal = ref(0)
const pickerPage = ref(1)
const pickerCurrent = ref(null)
const pickerFilter = ref({ dropShipping: false, douyinSheet: false, delivery48h: false, freeShipping: false })

function openPickerDialog() {
  pickerVisible.value = true
  pickerKeyword.value = ''
  pickerPlatform.value = ''
  pickerCurrent.value = null
  pickerPage.value = 1
  pickerFilter.value = { dropShipping: false, douyinSheet: false, delivery48h: false, freeShipping: false }
  searchPickerProducts()
}

async function searchPickerProducts() {
  pickerLoading.value = true
  try {
    const params = {
      keyword: pickerKeyword.value || undefined,
      platform: pickerPlatform.value || undefined,
      page: pickerPage.value,
      size: 10,
      sortField: 'sales',
      sortOrder: 'desc'
    }
    if (pickerFilter.value.dropShipping) params.dropShipping = 1
    if (pickerFilter.value.douyinSheet) params.douyinSheetSupport = 1
    if (pickerFilter.value.delivery48h) params.deliveryIn48h = 1
    if (pickerFilter.value.freeShipping) params.freeShipping = 1
    const res = await productApi.list(params)
    if (res.code === 200) {
      pickerProducts.value = res.data?.records || []
      pickerTotal.value = res.data?.total || 0
    }
  } finally {
    pickerLoading.value = false
  }
}

function onPickerSelect(row) { pickerCurrent.value = row }

function confirmPicker() {
  if (!pickerCurrent.value) return
  selectedProduct.value = pickerCurrent.value
  query.value.productId = pickerCurrent.value.id
  pickerVisible.value = false
  hasTrend.value = false
  chartInstance = null
}

const catQuery = ref({ category:'', days:30, metric:'avgAiScore' })
const hasCatTrend = ref(false)
const catLoading = ref(false)
const catChartRef = ref(null)
let catChartInst = null

const categories = ref([])

// 切换 tab 时重置图表实例（因为 v-if 会销毁 DOM）
function switchTab(tab) {
  if (activeTab.value === tab) return
  activeTab.value = tab
  chartInstance = null
  catChartInst = null
}

async function loadTrend() {
  if (!query.value.productId) { ElMessage.warning('请先选择商品'); return }
  trendLoading.value = true
  hasTrend.value = false
  chartInstance = null
  try {
    const params = { dimension: query.value.dimension, startDate: dateRange.value?.[0], endDate: dateRange.value?.[1] }
    const res = await trendApi.productTrend(query.value.productId, params)
    if (res.code === 200 && res.data?.length) {
      hasTrend.value = true
      await nextTick()
      renderChart(res.data)
    } else {
      ElMessage.info('暂无趋势数据')
    }
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    trendLoading.value = false
  }
}

function renderChart(data) {
  if (!chartRef.value) return
  if (chartInstance) { chartInstance.dispose() }
  chartInstance = echarts.init(chartRef.value)
  const dimMap = { sales:'销量', views:'浏览量', rank:'排名' }
  chartInstance.setOption({
    tooltip:{ trigger:'axis', backgroundColor:'rgba(26,26,62,0.9)', borderColor:'transparent', textStyle:{color:'#fff'} },
    xAxis:{ type:'category', data:data.map(d=>d.statDate), axisLabel:{color:'#999'} },
    yAxis:{ type:'value', name:dimMap[query.value.dimension], splitLine:{lineStyle:{color:'#f0f0f0'}}, axisLabel:{color:'#999'} },
    series:[{ name:dimMap[query.value.dimension], type:'line', smooth:true,
      areaStyle:{ color:{type:'linear',x:0,y:0,x2:0,y2:1,colorStops:[{offset:0,color:'rgba(64,158,255,0.3)'},{offset:1,color:'rgba(64,158,255,0)'}]} },
      data:data.map(d=>d[query.value.dimension]), itemStyle:{color:'#409eff'}, lineStyle:{width:3}, symbol:'circle', symbolSize:5 }],
    grid:{left:50,right:20,top:20,bottom:30},
  })
  window.addEventListener('resize', ()=>chartInstance?.resize())
}

async function loadCategoryTrend() {
  catLoading.value = true
  hasCatTrend.value = false
  catChartInst = null
  try {
    const res = await trendApi.categoryTrend({ category: catQuery.value.category, days: catQuery.value.days })
    if (res.code === 200 && res.data?.length) {
      hasCatTrend.value = true
      await nextTick()
      renderCatChart(res.data)
    } else {
      ElMessage.info('暂无品类趋势数据')
    }
  } catch (e) {
    ElMessage.error('查询失败')
  } finally {
    catLoading.value = false
  }
}

function renderCatChart(data) {
  if (!catChartRef.value) return
  if (catChartInst) { catChartInst.dispose() }
  catChartInst = echarts.init(catChartRef.value)
  const catMap = {}, dates=[...new Set(data.map(d=>d.statDate))].sort()
  data.forEach(d=>{ if(!catMap[d.category]) catMap[d.category]={}; catMap[d.category][d.statDate]=d[catQuery.value.metric] })
  const colors=['#409eff','#67c23a','#e6a23c','#f56c6c','#9c59f7','#00b5ad']
  const series = Object.keys(catMap).map((cat,i)=>({
    name:cat, type:'line', smooth:true,
    data:dates.map(d=>catMap[cat][d]??null),
    itemStyle:{color:colors[i%colors.length]}, lineStyle:{width:2.5}, symbol:'circle', symbolSize:5,
    areaStyle:{opacity:0.05}
  }))
  catChartInst.setOption({
    tooltip:{trigger:'axis', backgroundColor:'rgba(26,26,62,0.9)', borderColor:'transparent', textStyle:{color:'#fff'}},
    legend:{type:'scroll', bottom:0, textStyle:{color:'#666'}},
    xAxis:{type:'category', data:dates, axisLabel:{color:'#999'}},
    yAxis:{type:'value', splitLine:{lineStyle:{color:'#f0f0f0'}}, axisLabel:{color:'#999'}},
    series, grid:{left:50,right:20,top:20,bottom:50},
  })
  window.addEventListener('resize', ()=>catChartInst?.resize())
}

async function loadCategories() {
  const res = await productApi.categories()
  const dbCats = (res.code === 200 ? res.data : null) || []
  // 合并数据库已有品类 + 预置常用品类（去重）
  const preset = ['女装','男装','童装','美妆护肤','生活用品','食品零食','数码电子','母婴用品','运动户外','宠物用品','家居家纺','鞋靴箱包']
  const merged = [...new Set([...dbCats.filter(Boolean), ...preset])]
  categories.value = merged
}
onMounted(loadCategories)
</script>

<style scoped>
.page-wrap { display:flex;flex-direction:column;gap:16px; }

.tab-bar { display:flex;gap:8px;background:#fff;padding:6px;border-radius:14px;box-shadow:var(--card-shadow);width:fit-content; }
.tab-btn { padding:8px 20px;border-radius:10px;font-size:14px;font-weight:600;cursor:pointer;color:#888;transition:all 0.2s; }
.tab-btn.active { background:linear-gradient(135deg,#409eff,#1677ff);color:#fff;box-shadow:0 4px 14px rgba(64,158,255,0.35); }
.tab-btn:hover:not(.active) { background:#f5f7fa;color:#555; }

.filter-card { background:#fff;border-radius:16px;padding:20px;box-shadow:var(--card-shadow); }
.filter-title { font-size:14px;font-weight:700;color:#1a1a2e;margin-bottom:14px;padding-left:8px;border-left:3px solid #409eff; }
.filter-row { display:flex;flex-wrap:wrap;gap:16px;align-items:flex-end; }
.filter-item { display:flex;flex-direction:column;gap:5px; }
.filter-label { font-size:12px;color:#888;font-weight:500; }

.chart-card { background:#fff;border-radius:16px;padding:20px;box-shadow:var(--card-shadow); }
.section-title { font-size:15px;font-weight:700;color:#1a1a2e;margin-bottom:16px; }
.empty-chart { height:200px;display:flex;align-items:center;justify-content:center;color:#bbb;font-size:14px; }
</style>
