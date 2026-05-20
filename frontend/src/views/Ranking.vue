<template>
  <div class="page-wrap">
    <!-- 筛选块 -->
    <div class="filter-card">
      <div class="filter-title">筛选条件</div>
      <div class="filter-body">
        <div class="filter-row">
          <div class="filter-item">
            <div class="filter-label">时间维度</div>
            <el-radio-group v-model="timeDim" @change="onTimeDimChange" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="today">今日</el-radio-button>
              <el-radio-button label="week">本周</el-radio-button>
              <el-radio-button label="month">本月</el-radio-button>
            </el-radio-group>
          </div>
          <div class="filter-item">
            <div class="filter-label">平台</div>
            <el-select v-model="query.platform" placeholder="全部" clearable size="small" style="width:110px">
              <el-option label="1688" value="1688" /><el-option label="淘宝" value="taobao" />
              <el-option label="京东" value="jd" /><el-option label="拼多多" value="pdd" />
            </el-select>
          </div>
          <div class="filter-item">
            <div class="filter-label">品类</div>
            <el-select v-model="query.category" placeholder="全部" clearable size="small" style="width:110px">
              <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
            </el-select>
          </div>
          <div class="filter-item">
            <div class="filter-label">AI决策</div>
            <el-select v-model="query.verdict" placeholder="全部" clearable size="small" style="width:100px">
              <el-option label="上架" value="上架" /><el-option label="测试" value="测试" /><el-option label="放弃" value="放弃" />
            </el-select>
          </div>
          <div class="filter-item">
            <div class="filter-label">排序字段</div>
            <el-select v-model="query.sortField" size="small" style="width:100px">
              <el-option label="销量" value="sales" /><el-option label="评分" value="rating" />
              <el-option label="AI评分" value="aiScore" /><el-option label="时间" value="createTime" />
            </el-select>
          </div>
          <div class="filter-item">
            <div class="filter-label">关键词</div>
            <el-input v-model="query.keyword" placeholder="搜索商品" clearable size="small" style="width:140px" />
          </div>
        </div>
        <div class="filter-actions">
          <el-button type="primary" size="small" @click="loadRanking">🔍 查询</el-button>
          <el-button size="small" @click="showReport">📊 AI报告</el-button>
          <el-button type="success" size="small" @click="exportExcel">📥 导出Excel</el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <div class="table-header">
        <span class="table-title">商品排行榜</span>
        <span class="table-count">共 {{ pagination.total }} 条</span>
      </div>
      <el-table :data="products" v-loading="loading" stripe style="width:100%">
        <el-table-column label="#" width="46" type="index" align="center" />
        <el-table-column label="图片" width="64" align="center">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" style="width:44px;height:44px;border-radius:8px;" fit="cover">
              <template #error><div style="width:44px;height:44px;border-radius:8px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;">📦</div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="商品名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <router-link :to="`/product/${row.id}`" class="product-link">{{ row.title }}</router-link>
            <div v-if="row.aiTitle" style="font-size:11px;color:#409eff;margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" :title="row.aiTitle">
              🤖 {{ row.aiTitle }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="platform" label="平台" width="78" align="center">
          <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="90" align="right">
          <template #default="{ row }"><span class="price-text">¥{{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="88" sortable align="right" />
        <el-table-column label="AI评分" width="150">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:6px;">
              <el-progress :percentage="row.aiScore||0" :color="aiColor(row.aiScore)" :stroke-width="8" :show-text="false" style="flex:1" />
              <span style="font-size:13px;font-weight:700;color:#222;min-width:28px;text-align:right;">{{ row.aiScore||0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="决策" width="76" align="center">
          <template #default="{ row }">
            <el-tag :type="verdictType(row.verdict)" size="small">{{ row.verdict||'未分析' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-text type="primary" size="small" style="cursor:pointer;margin-right:10px;" @click="triggerAI(row)">AI分析</el-text>
            <el-text type="danger" size="small" style="cursor:pointer;" @click="deleteProduct(row)">删除</el-text>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="query.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top:14px;display:flex;justify-content:flex-end;"
        @current-change="loadRanking"
      />
    </div>

    <!-- AI报告弹窗 -->
    <el-dialog v-model="reportVisible" title="🤖 AI选品分析报告" width="600px">
      <div v-loading="reportLoading" style="min-height:100px;white-space:pre-wrap;line-height:1.9;font-size:14px;color:#333;">{{ reportContent }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { rankingApi, productApi } from '@/api'

const query = ref({ platform:'', category:'', keyword:'', verdict:'', sortField:'sales', sortOrder:'desc', page:1, size:20 })
const timeDim = ref('all')
const products = ref([])
const categories = ref([])
const loading = ref(false)
const pagination = ref({ page:1, total:0 })
const reportVisible = ref(false)
const reportLoading = ref(false)
const reportContent = ref('')

const aiColor = s => !s?'#eee':s>=80?'#67c23a':s>=60?'#e6a23c':'#f56c6c'
const verdictType = v => v==='上架'?'success':v==='测试'?'warning':v==='放弃'?'danger':'info'

async function loadRanking() {
  loading.value = true
  try {
    const now = new Date()
    let startDate = null
    if (timeDim.value==='today') startDate = now.toISOString().slice(0,10)
    else if (timeDim.value==='week') {
      const d=new Date(now); d.setDate(d.getDate()-d.getDay()+(d.getDay()===0?-6:1))
      startDate = d.toISOString().slice(0,10)
    } else if (timeDim.value==='month') startDate = `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-01`
    const raw = { ...query.value, page:pagination.value.page, startDate, endDate:timeDim.value!=='all'?now.toISOString().slice(0,10):null }
    const params = Object.fromEntries(Object.entries(raw).filter(([,v])=>v!==null&&v!==undefined&&v!==''))
    const res = await rankingApi.top(params)
    if (res.code===200) { products.value=res.data.records||[]; pagination.value.total=res.data.total||0 }
  } finally { loading.value=false }
}

function onTimeDimChange() { pagination.value.page=1; loadRanking() }
async function loadCategories() { const res=await productApi.categories(); if(res.code===200) categories.value=res.data||[] }
async function triggerAI(row) { const res=await productApi.aiAnalyze({productId:row.id}); if(res.code===200){Object.assign(row,res.data);ElMessage.success('AI分析完成')} }
async function deleteProduct(row) {
  await ElMessageBox.confirm('确认删除该商品?','提示',{type:'warning'})
  const res=await productApi.remove(row.id)
  if(res.code===200){products.value=products.value.filter(p=>p.id!==row.id);ElMessage.success('已删除')}
}
async function showReport() {
  reportVisible.value=true; reportLoading.value=true; reportContent.value=''
  try { const res=await rankingApi.report({platform:query.value.platform,category:query.value.category}); if(res.code===200) reportContent.value=res.data }
  finally { reportLoading.value=false }
}
function exportExcel() { window.open(rankingApi.exportUrl({platform:query.value.platform||'',limit:100}),'_blank') }
onMounted(()=>{loadRanking();loadCategories()})
</script>

<style scoped>
.page-wrap { display:flex;flex-direction:column;gap:16px; }

.filter-card { background:#fff;border-radius:16px;padding:20px;box-shadow:var(--card-shadow); }
.filter-title { font-size:14px;font-weight:700;color:#1a1a2e;margin-bottom:14px;padding-left:8px;border-left:3px solid #409eff; }
.filter-body { display:flex;flex-wrap:wrap;align-items:flex-end;gap:12px;justify-content:space-between; }
.filter-row { display:flex;flex-wrap:wrap;gap:16px;align-items:flex-end;flex:1; }
.filter-item { display:flex;flex-direction:column;gap:5px; }
.filter-label { font-size:12px;color:#888;font-weight:500; }
.filter-actions { display:flex;gap:8px;align-items:center; }

.table-card { background:#fff;border-radius:16px;padding:20px;box-shadow:var(--card-shadow); }
.table-header { display:flex;align-items:center;justify-content:space-between;margin-bottom:16px; }
.table-title { font-size:15px;font-weight:700;color:#1a1a2e; }
.table-count { font-size:13px;color:#999;background:#f5f7fa;padding:3px 10px;border-radius:20px; }

.product-link { color:#1a1a2e;text-decoration:none;font-weight:500;font-size:13px; }
.product-link:hover { color:#409eff; }
.price-text { color:#f56c6c;font-weight:600; }

@media (max-width:767px) {
  .filter-row { gap:10px; }
  .filter-actions { width:100%; }
}
</style>
