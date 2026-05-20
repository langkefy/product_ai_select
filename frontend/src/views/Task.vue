<template>
  <div>
    <el-card shadow="hover" style="margin-bottom:16px;">
      <el-form inline :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索商品" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="query.platform" placeholder="全部" clearable style="width:120px">
            <el-option label="淘宝" value="taobao" />
            <el-option label="京东" value="jd" />
            <el-option label="拼多多" value="pdd" />
            <el-option label="1688" value="1688" />
          </el-select>
        </el-form-item>
        <el-form-item label="AI决策">
          <el-select v-model="query.verdict" placeholder="全部" clearable style="width:110px">
            <el-option label="上架" value="上架" />
            <el-option label="测试" value="测试" />
            <el-option label="放弃" value="放弃" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="query.sortField" style="width:110px">
            <el-option label="决策优先" value="verdict" />
            <el-option label="AI评分" value="aiScore" />
            <el-option label="销量" value="sales" />
            <el-option label="时间" value="createTime" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button type="success" :disabled="!selected.length" @click="batchAnalyze" :loading="analyzing">
            批量AI分析 ({{ selected.length }})
          </el-button>
          <el-button type="warning" @click="analyzeAll" :loading="analyzingAll">🤖 分析全部未分析</el-button>
          <el-button type="info" @click="fillDetail" :loading="fillingDetail">📦 补全发货地/代发价</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table :data="products" v-loading="loading" @selection-change="onSelect" stripe>
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="图片" width="70">
          <template #default="{ row }">
            <el-image :src="row.imageUrl" style="width:48px;height:48px;border-radius:4px;" fit="cover">
              <template #error><div style="width:48px;height:48px;background:#f5f5f5;display:flex;align-items:center;justify-content:center;">📦</div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="商品名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <router-link :to="`/product/${row.id}`" style="color:#409eff;text-decoration:none;">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="platform" label="平台" width="80">
          <template #default="{ row }"><el-tag size="small">{{ platformMap[row.platform] || row.platform }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="90">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="90" />
        <el-table-column label="AI评分" width="130">
          <template #default="{ row }">
            <span v-if="row.aiScore !== null">
              <el-progress :percentage="row.aiScore" :color="aiColor(row.aiScore)" :stroke-width="8" />
            </span>
            <el-tag v-else type="info" size="small">未分析</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="决策" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.verdict" :type="verdictType(row.verdict)" size="small">{{ row.verdict }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI售价" width="90">
          <template #default="{ row }">
            <span v-if="row.suggestedPrice" style="color:#67c23a;font-weight:600;">¥{{ row.suggestedPrice }}</span>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="AI新标题" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.aiTitle" style="font-size:12px;color:#409eff;font-weight:500;">{{ row.aiTitle }}</span>
            <span v-else style="color:#bbb;font-size:12px;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="AI分析" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span style="font-size:12px;color:#666;">{{ row.aiAnalysis || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="发货地" width="100" show-overflow-tooltip />
        <el-table-column label="代发价" width="90">
          <template #default="{ row }">
            <span v-if="row.agentPrice">¥{{ row.agentPrice }}</span>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="物流费" width="80">
          <template #default="{ row }">
            <span v-if="row.shippingFee != null">¥{{ row.shippingFee }}</span>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="建议售价" width="90">
          <template #default="{ row }">
            <span v-if="row.platformSuggestPrice">¥{{ row.platformSuggestPrice }}</span>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="48h发货" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.deliveryIn48h === 1" type="success" size="small">✓</el-tag>
            <el-tag v-else-if="row.deliveryIn48h === 0" type="danger" size="small">✗</el-tag>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="全包售后" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.fullAfterSales === 1" type="success" size="small">✓</el-tag>
            <el-tag v-else-if="row.fullAfterSales === 0" type="danger" size="small">✗</el-tag>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="抖音面单" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.douyinSheetSupport === 1" type="success" size="small">✓</el-tag>
            <el-tag v-else-if="row.douyinSheetSupport === 0" type="danger" size="small">✗</el-tag>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="库存同步" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.autoSyncStock === 1" type="success" size="small">✓</el-tag>
            <el-tag v-else-if="row.autoSyncStock === 0" type="danger" size="small">✗</el-tag>
            <span v-else style="color:#bbb;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="singleAnalyze(row)">AI分析</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="query.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top:12px;justify-content:flex-end;display:flex;"
        @current-change="loadProducts"
      />
    </el-card>

    <!-- 分析全部结果弹窗 -->
    <el-dialog v-model="analyzeAllVisible" title="🤖 全部未分析商品分析结果" width="800px" :close-on-click-modal="false">
      <div v-loading="analyzingAll" style="min-height:60px;">
        <div v-if="analyzeAllResults.length" style="margin-bottom:12px;font-size:13px;color:#666;">
          共分析 <b>{{ analyzeAllResults.length }}</b> 件商品，已按决策排序（上架优先）
        </div>
        <el-empty v-else-if="!analyzingAll" description="暂无未分析商品" />
        <el-table v-if="analyzeAllResults.length" :data="analyzeAllResults" stripe size="small" max-height="420">
          <el-table-column type="index" label="#" width="40" align="center" />
          <el-table-column prop="title" label="商品名称" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <router-link :to="`/product/${row.id}`" style="color:#409eff;text-decoration:none;">{{ row.title }}</router-link>
              </template>
            </el-table-column>
            <el-table-column prop="aiTitle" label="AI新标题" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span v-if="row.aiTitle" style="color:#409eff;font-size:12px;font-weight:500;">{{ row.aiTitle }}</span>
                <span v-else style="color:#bbb;font-size:12px;">-</span>
              </template>
            </el-table-column>
          <el-table-column label="AI评分" width="120">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:6px;">
                <el-progress :percentage="row.aiScore||0" :color="aiColor(row.aiScore)" :stroke-width="7" :show-text="false" style="flex:1" />
                <span style="font-size:12px;font-weight:700;min-width:24px;text-align:right;">{{ row.aiScore||0 }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="决策" width="72" align="center">
            <template #default="{ row }">
              <el-tag :type="verdictType(row.verdict)" size="small">{{ row.verdict||'未分析' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="aiAnalysis" label="分析理由" min-width="160" show-overflow-tooltip />
          <el-table-column prop="risk" label="风险" min-width="120" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button type="primary" @click="analyzeAllVisible=false;onSearch()">关闭并刷新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { productApi, collectApi } from '@/api'

const query = ref({ keyword: '', platform: '', verdict: '', sortField: 'verdict', sortOrder: 'asc', page: 1, size: 20 })
const products = ref([])
const loading = ref(false)
const analyzing = ref(false)
const analyzingAll = ref(false)
const fillingDetail = ref(false)
const analyzeAllVisible = ref(false)
const analyzeAllResults = ref([])
const selected = ref([])
const pagination = ref({ page: 1, total: 0 })
const platformMap = { taobao: '淘宝', jd: '京东', pdd: '拼多多', '1688': '1688' }

function aiColor(score) {
  if (score >= 80) return '#67c23a'
  if (score >= 60) return '#e6a23c'
  return '#f56c6c'
}

function verdictType(verdict) {
  if (verdict === '上架') return 'success'
  if (verdict === '测试') return 'warning'
  if (verdict === '放弃') return 'danger'
  return 'info'
}

function onSelect(rows) { selected.value = rows }

// 查询时重置到第1页
function onSearch() {
  pagination.value.page = 1
  loadProducts()
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await productApi.list({ ...query.value, page: pagination.value.page })
    if (res.code === 200) {
      products.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function singleAnalyze(row) {
  const res = await productApi.aiAnalyze({ productId: row.id })
  if (res.code === 200) { Object.assign(row, res.data); ElMessage.success('AI分析完成') }
}

async function batchAnalyze() {
  analyzing.value = true
  const ids = selected.value.map(p => p.id)
  try {
    const res = await productApi.aiAnalyze({ productIds: ids })
    if (res.code === 200) {
      const updated = res.data
      updated.forEach(u => {
        const idx = products.value.findIndex(p => p.id === u.id)
        if (idx > -1) Object.assign(products.value[idx], u)
      })
      ElMessage.success(`已完成 ${updated.length} 个商品的AI分析`)
    }
  } finally {
    analyzing.value = false
  }
}

async function analyzeAll() {
  try {
    await ElMessageBox.confirm('将对所有未分析商品进行AI分析，数量较多时耗时较长，确认继续？', '分析全部', { type: 'warning', confirmButtonText: '确认分析' })
  } catch { return }
  analyzingAll.value = true
  analyzeAllVisible.value = true
  analyzeAllResults.value = []
  try {
    const res = await productApi.aiAnalyzeAll()
    if (res.code === 200) {
      analyzeAllResults.value = res.data || []
      ElMessage.success(`分析完成，共 ${analyzeAllResults.value.length} 件商品`)
    }
  } finally {
    analyzingAll.value = false
  }
}

async function fillDetail() {
  try {
    await ElMessageBox.confirm('将通过 item_get 接口补全所有缺少发货地的商品信息（发货地、代发价、建议售价），需要激活 OpenClaw 平台配置，确认继续？', '补全商品详情', { type: 'warning', confirmButtonText: '确认' })
  } catch { return }
  fillingDetail.value = true
  try {
    const res = await collectApi.fillDetail()
    if (res.code === 200) {
      const { total, success, failed } = res.data
      ElMessage.success(`补全完成：共 ${total} 件，成功 ${success} 件，失败 ${failed} 件`)
      loadProducts()
    }
  } finally {
    fillingDetail.value = false
  }
}

onMounted(loadProducts)
</script>
