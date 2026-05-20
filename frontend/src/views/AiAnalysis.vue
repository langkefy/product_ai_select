<template>
  <div class="ai-analysis-page">
    <!-- 顶部操作栏 -->
    <el-card class="action-card" shadow="never">
      <div class="action-row">
        <div class="action-left">
          <el-select v-model="form.platform" placeholder="选择平台" style="width:140px">
            <el-option label="抖音" value="抖音" />
            <el-option label="淘宝" value="淘宝" />
            <el-option label="拼多多" value="拼多多" />
            <el-option label="快手" value="快手" />
          </el-select>
          <el-input v-model="form.category" placeholder="品类（可选，如：女装、家居）" style="width:220px" clearable />
          <el-button type="primary" :loading="generating" @click="generateReport" :icon="MagicStick">
            {{ generating ? 'AI分析中...' : '🤖 生成AI分析报告' }}
          </el-button>
        </div>
        <div class="action-right">
          <el-text type="info" size="small">每次分析结果自动入库，可查看历史记录</el-text>
        </div>
      </div>
    </el-card>

    <div class="content-layout">
      <!-- 左侧：报告内容展示 -->
      <div class="report-main" v-if="report">
        <!-- 报告标题 -->
        <el-card class="report-header-card" shadow="never">
          <div class="report-header">
            <div class="report-title-row">
              <el-icon class="title-icon"><DataAnalysis /></el-icon>
              <span class="report-title">{{ report.title }}</span>
              <el-tag :type="report.aiGenerated ? 'success' : 'warning'" size="small">
                {{ report.aiGenerated ? '🤖 AI生成' : '📋 规则引擎' }}
              </el-tag>
            </div>
            <el-text type="info" size="small">生成时间：{{ formatTime(report.createTime) }}</el-text>
          </div>
        </el-card>

        <!-- 当前热门分类 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-header">
              <span class="section-icon">🔥</span>
              <span class="section-title">当前热门分类</span>
              <el-tag type="danger" size="small">实时热门</el-tag>
            </div>
          </template>
          <div class="category-grid">
            <div
              v-for="(item, idx) in parseJson(report.hotCategories)"
              :key="idx"
              class="category-card hot"
            >
              <div class="cat-rank">#{{ idx + 1 }}</div>
              <div class="cat-name">{{ item.name }}</div>
              <div class="cat-meta">
                <el-tag :type="trendType(item.trend)" size="small">{{ item.trend }}</el-tag>
                <el-tag :type="compType(item.competition)" size="small">竞争{{ item.competition }}</el-tag>
              </div>
              <div class="cat-reason">{{ item.reason }}</div>
            </div>
          </div>
        </el-card>

        <!-- 即将热门分类 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-header">
              <span class="section-icon">📈</span>
              <span class="section-title">即将热门分类</span>
              <el-tag type="warning" size="small">趋势预测</el-tag>
            </div>
          </template>
          <div class="rising-list">
            <div
              v-for="(item, idx) in parseJson(report.risingCategories)"
              :key="idx"
              class="rising-card"
            >
              <div class="rising-left">
                <span class="rising-num">{{ idx + 1 }}</span>
                <div class="rising-info">
                  <span class="rising-name">{{ item.name }}</span>
                  <span class="rising-timing">⏰ {{ item.timing }}</span>
                </div>
              </div>
              <div class="rising-reason">{{ item.reason }}</div>
            </div>
          </div>
        </el-card>

        <!-- 分类选品推荐 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-header">
              <span class="section-icon">🛍️</span>
              <span class="section-title">分类选品推荐</span>
              <el-tag type="success" size="small">AI推荐</el-tag>
            </div>
          </template>
          <div class="product-reco-grid">
            <div
              v-for="(item, idx) in parseJson(report.productRecommendations)"
              :key="idx"
              class="product-reco-card"
            >
              <div class="reco-category">{{ item.category }}</div>
              <div class="reco-name">{{ item.productName }}</div>
              <div class="reco-price">💰 {{ item.priceRange }}</div>
              <div class="reco-point">✨ {{ item.keySellingPoint }}</div>
              <div class="reco-reason">{{ item.reason }}</div>
            </div>
          </div>
        </el-card>

        <!-- 上架建议 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-header">
              <span class="section-icon">📋</span>
              <span class="section-title">上架建议</span>
              <el-tag size="small">运营指南</el-tag>
            </div>
          </template>
          <div class="advice-grid" v-if="listingAdvice">
            <div class="advice-item">
              <div class="advice-label">📝 标题优化</div>
              <div class="advice-content">{{ listingAdvice.title }}</div>
            </div>
            <div class="advice-item">
              <div class="advice-label">📸 主图建议</div>
              <div class="advice-content">{{ listingAdvice.cover }}</div>
            </div>
            <div class="advice-item">
              <div class="advice-label">💲 定价策略</div>
              <div class="advice-content">{{ listingAdvice.price }}</div>
            </div>
            <div class="advice-item">
              <div class="advice-label">🏷️ 标签关键词</div>
              <div class="advice-content">{{ listingAdvice.tags }}</div>
            </div>
            <div class="advice-item full-width">
              <div class="advice-label">⏰ 上架时机</div>
              <div class="advice-content">{{ listingAdvice.timing }}</div>
            </div>
          </div>
        </el-card>

        <!-- 推广分析 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-header">
              <span class="section-icon">📣</span>
              <span class="section-title">推广分析</span>
              <el-tag type="primary" size="small">{{ report.platform }}专属</el-tag>
            </div>
          </template>
          <div v-if="promotionAnalysis">
            <div class="promo-overview">{{ promotionAnalysis.overview }}</div>
            <div class="promo-meta-row">
              <div class="promo-meta-item">
                <el-icon><Money /></el-icon>
                <span>建议预算：{{ promotionAnalysis.budget }}</span>
              </div>
              <div class="promo-meta-item">
                <el-icon><User /></el-icon>
                <span>目标人群：{{ promotionAnalysis.targetAudience }}</span>
              </div>
            </div>
            <el-divider content-position="left">推广渠道</el-divider>
            <div class="channel-list">
              <div
                v-for="(ch, idx) in promotionAnalysis.channels"
                :key="idx"
                class="channel-card"
                :class="'priority-' + ch.priority"
              >
                <div class="channel-header">
                  <span class="channel-name">{{ ch.name }}</span>
                  <el-tag :type="priorityType(ch.priority)" size="small">{{ ch.priority }}优先级</el-tag>
                </div>
                <div class="channel-desc">{{ ch.desc }}</div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 营销推广流程 -->
        <el-card class="section-card" shadow="never">
          <template #header>
            <div class="section-header">
              <span class="section-icon">🚀</span>
              <span class="section-title">营销推广流程</span>
              <el-tag type="success" size="small">抖音店铺全流程</el-tag>
            </div>
          </template>
          <div class="flow-timeline">
            <div
              v-for="(step, idx) in parseJson(report.marketingFlow)"
              :key="idx"
              class="flow-step"
              :class="{ last: idx === parseJson(report.marketingFlow).length - 1 }"
            >
              <div class="flow-left">
                <div class="flow-dot">{{ step.step }}</div>
                <div class="flow-line" v-if="idx < parseJson(report.marketingFlow).length - 1"></div>
              </div>
              <div class="flow-content">
                <div class="flow-phase">
                  {{ step.phase }}
                  <el-tag size="small" type="info">{{ step.duration }}</el-tag>
                </div>
                <div class="flow-kpi">🎯 考核：{{ step.kpi }}</div>
                <ul class="flow-actions">
                  <li v-for="(action, ai) in step.actions" :key="ai">{{ action }}</li>
                </ul>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 报告为空时的引导 -->
      <div class="report-empty" v-else>
        <el-empty description="点击「生成AI分析报告」开始分析">
          <el-button type="primary" @click="generateReport" :loading="generating">
            🤖 立即生成分析报告
          </el-button>
        </el-empty>
      </div>

      <!-- 右侧：历史记录 -->
      <div class="history-panel">
        <el-card shadow="never" class="history-card">
          <template #header>
            <div class="history-header">
              <span>📁 历史报告</span>
              <el-button text size="small" @click="loadHistory">刷新</el-button>
            </div>
          </template>
          <div class="history-list">
            <div
              v-for="item in historyList"
              :key="item.id"
              class="history-item"
              :class="{ active: report && report.id === item.id }"
              @click="loadReport(item.id)"
            >
              <div class="history-title">{{ item.title }}</div>
              <div class="history-meta">
                <el-tag size="small" :type="item.aiGenerated ? 'success' : 'warning'">
                  {{ item.aiGenerated ? 'AI' : '规则' }}
                </el-tag>
                <span class="history-time">{{ formatTime(item.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!historyList.length" description="暂无历史报告" :image-size="60" />
          </div>
          <div class="history-pagination" v-if="total > pageSize">
            <el-pagination
              small
              layout="prev, pager, next"
              :total="total"
              :page-size="pageSize"
              v-model:current-page="currentPage"
              @current-change="loadHistory"
            />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, DataAnalysis, Money, User } from '@element-plus/icons-vue'
import { aiReportApi } from '@/api/index.js'

const form = ref({ platform: '抖音', category: '' })
const generating = ref(false)
const report = ref(null)
const historyList = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const listingAdvice = computed(() => {
  if (!report.value?.listingAdvice) return null
  return parseJsonObj(report.value.listingAdvice)
})

const promotionAnalysis = computed(() => {
  if (!report.value?.promotionAnalysis) return null
  return parseJsonObj(report.value.promotionAnalysis)
})

function parseJson(str) {
  if (!str) return []
  try {
    const parsed = typeof str === 'string' ? JSON.parse(str) : str
    return Array.isArray(parsed) ? parsed : []
  } catch { return [] }
}

function parseJsonObj(str) {
  if (!str) return {}
  try {
    return typeof str === 'string' ? JSON.parse(str) : str
  } catch { return {} }
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function trendType(t) {
  return t === '上升' ? 'success' : t === '下降' ? 'danger' : 'info'
}
function compType(c) {
  return c === '高' ? 'danger' : c === '中' ? 'warning' : 'success'
}
function priorityType(p) {
  return p === '高' ? 'danger' : p === '中' ? 'warning' : 'info'
}

async function generateReport() {
  generating.value = true
  try {
    const res = await aiReportApi.generate({
      platform: form.value.platform,
      category: form.value.category || undefined
    })
    report.value = res.data
    ElMessage.success('AI分析报告生成成功！')
    await loadHistory()
  } catch (e) {
    ElMessage.error('生成失败：' + (e.message || '未知错误'))
  } finally {
    generating.value = false
  }
}

async function loadHistory() {
  try {
    const res = await aiReportApi.list({ page: currentPage.value, size: pageSize.value })
    historyList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch {}
}

async function loadReport(id) {
  try {
    const res = await aiReportApi.detail(id)
    report.value = res.data
  } catch {}
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.ai-analysis-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-card {
  border-radius: 12px;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.action-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.content-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.report-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-empty {
  flex: 1;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 12px;
}

.history-panel {
  width: 280px;
  flex-shrink: 0;
}

.history-card {
  border-radius: 12px;
  position: sticky;
  top: 0;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.history-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #eef0f6;
  transition: all 0.2s;
}

.history-item:hover {
  border-color: #409eff;
  background: #f0f7ff;
}

.history-item.active {
  border-color: #409eff;
  background: linear-gradient(135deg, #f0f7ff, #e8f4fd);
}

.history-title {
  font-size: 12px;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-time {
  font-size: 11px;
  color: #999;
}

/* 报告头部 */
.section-card {
  border-radius: 12px;
}

.report-header-card {
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

:deep(.report-header-card .el-card__body) {
  padding: 20px 24px;
}

.report-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.report-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  color: #fff;
  font-size: 22px;
}

.report-title {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  flex: 1;
}

:deep(.report-header-card .el-text) {
  color: rgba(255,255,255,0.8) !important;
}

/* Section header */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 18px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  flex: 1;
}

/* 热门分类 */
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.category-card.hot {
  border-radius: 10px;
  padding: 14px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffe0e0 100%);
  border: 1px solid #ffd0d0;
  position: relative;
}

.cat-rank {
  font-size: 11px;
  color: #f56c6c;
  font-weight: 700;
  margin-bottom: 4px;
}

.cat-name {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.cat-meta {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.cat-reason {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

/* 即将热门 */
.rising-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rising-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fffbf0, #fff3d0);
  border: 1px solid #ffe9a0;
}

.rising-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rising-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f7b733, #fc4a1a);
  color: #fff;
  font-weight: 700;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rising-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rising-name {
  font-weight: 700;
  font-size: 14px;
  color: #1a1a2e;
}

.rising-timing {
  font-size: 11px;
  color: #f7b733;
}

.rising-reason {
  flex: 1;
  font-size: 13px;
  color: #555;
  line-height: 1.5;
}

/* 选品推荐 */
.product-reco-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.product-reco-card {
  border-radius: 10px;
  padding: 14px;
  background: linear-gradient(135deg, #f0fff4, #d8f5e5);
  border: 1px solid #b8e6c8;
}

.reco-category {
  font-size: 11px;
  color: #67c23a;
  font-weight: 600;
  background: rgba(103,194,58,0.1);
  border-radius: 4px;
  padding: 2px 6px;
  display: inline-block;
  margin-bottom: 6px;
}

.reco-name {
  font-size: 14px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 6px;
}

.reco-price {
  font-size: 12px;
  color: #f56c6c;
  font-weight: 600;
  margin-bottom: 4px;
}

.reco-point {
  font-size: 12px;
  color: #409eff;
  margin-bottom: 6px;
}

.reco-reason {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

/* 上架建议 */
.advice-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.advice-item {
  padding: 14px;
  border-radius: 10px;
  background: #f8f9ff;
  border: 1px solid #e8ecff;
}

.advice-item.full-width {
  grid-column: 1 / -1;
}

.advice-label {
  font-size: 13px;
  font-weight: 700;
  color: #5a6fff;
  margin-bottom: 6px;
}

.advice-content {
  font-size: 13px;
  color: #444;
  line-height: 1.7;
}

/* 推广分析 */
.promo-overview {
  font-size: 14px;
  color: #333;
  line-height: 1.8;
  padding: 12px;
  background: #f8f9ff;
  border-radius: 8px;
  margin-bottom: 14px;
}

.promo-meta-row {
  display: flex;
  gap: 24px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.promo-meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #555;
}

.channel-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.channel-card {
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #e5e9f0;
}

.channel-card.priority-高 { background: linear-gradient(135deg, #fff0f0, #ffe4e4); border-color: #ffd0d0; }
.channel-card.priority-中 { background: linear-gradient(135deg, #fffbf0, #fff0d0); border-color: #ffe0a0; }
.channel-card.priority-低 { background: #f8f9fa; }

.channel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.channel-name {
  font-weight: 700;
  font-size: 14px;
  color: #1a1a2e;
}

.channel-desc {
  font-size: 12px;
  color: #666;
  line-height: 1.6;
}

/* 营销流程 */
.flow-timeline {
  display: flex;
  flex-direction: column;
}

.flow-step {
  display: flex;
  gap: 16px;
}

.flow-left {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.flow-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #a78bfa);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.flow-line {
  width: 2px;
  flex: 1;
  min-height: 20px;
  background: linear-gradient(180deg, #409eff, #e8ecff);
  margin: 4px 0;
}

.flow-content {
  flex: 1;
  padding-bottom: 20px;
}

.flow-phase {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 6px;
}

.flow-kpi {
  font-size: 12px;
  color: #409eff;
  margin-bottom: 8px;
}

.flow-actions {
  margin: 0;
  padding-left: 18px;
}

.flow-actions li {
  font-size: 13px;
  color: #555;
  line-height: 1.8;
}

.history-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

@media (max-width: 900px) {
  .content-layout {
    flex-direction: column;
  }
  .history-panel {
    width: 100%;
  }
  .history-card {
    position: static;
  }
  .advice-grid {
    grid-template-columns: 1fr;
  }
}
</style>

