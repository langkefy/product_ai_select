<template>
  <div>
    <el-tabs v-model="activeTab">
      <!-- 采集任务 -->
      <el-tab-pane label="🚀 采集任务" name="tasks">
        <!-- AI关键词推荐 -->
        <el-card shadow="hover" style="margin-bottom:16px;">
          <template #header>
            <span>🤖 AI选品关键词推荐</span>
            <span style="font-size:12px;color:#999;margin-left:8px;">结合当前季节 · 市场热点 · 电商趋势</span>
          </template>
          <div style="display:flex;flex-wrap:wrap;gap:8px;align-items:center;margin-bottom:12px;">
            <span style="font-size:13px;color:#666;font-weight:500;">品类方向：</span>
            <span
              v-for="cat in categoryOptions" :key="cat.value"
              :class="['cat-tag', selectedCategory === cat.value ? 'cat-tag--active' : '']"
              @click="toggleCategory(cat.value)"
            >{{ cat.icon }} {{ cat.label }}</span>
          </div>
          <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px;">
            <el-input
              v-model="customInput"
              placeholder="补充你的想法，如「学生党」「家有宝宝」「送礼」，不填走默认"
              clearable
              style="flex:1;max-width:480px;"
              size="default"
              @keyup.enter="getRecommend"
            >
              <template #prefix><el-icon><EditPen /></el-icon></template>
            </el-input>
            <el-button type="primary" @click="getRecommend" :loading="recommending" icon="MagicStick">
              获取推荐
            </el-button>
          </div>
          <!-- 推荐结果 -->
          <div v-if="recommending" style="text-align:center;padding:20px;color:#409eff;">
            <el-icon class="is-loading" style="font-size:20px;"><Loading /></el-icon>
            <span style="margin-left:8px;">AI正在分析当前市场热点...</span>
          </div>
          <div v-else-if="recommendations.length" style="display:flex;flex-wrap:wrap;gap:8px;">
            <div
              v-for="item in recommendations" :key="item.keyword"
              class="rec-tag"
              @click="applyKeyword(item)"
              :title="item.reason"
            >
              <span class="rec-keyword">{{ item.keyword }}</span>
              <el-tag size="small" type="info" style="margin-left:4px;font-size:11px;">{{ item.category }}</el-tag>
              <span class="rec-reason">{{ item.reason }}</span>
            </div>
          </div>
          <el-empty v-else-if="recommendLoaded" description="暂无推荐，请尝试其他品类或补充需求" :image-size="50" />
        </el-card>

        <el-row :gutter="16">
          <!-- 创建任务表单 -->
          <el-col :span="8">
            <el-card header="创建采集任务" shadow="hover">
              <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
                <el-form-item label="任务名称" prop="taskName">
                  <el-input v-model="form.taskName" placeholder="请输入任务名称" />
                </el-form-item>
                <el-form-item label="平台" prop="platform">
                  <el-select v-model="form.platform" placeholder="选择平台" style="width:100%">
                    <el-option label="淘宝" value="taobao" />
                    <el-option label="京东" value="jd" />
                    <el-option label="拼多多" value="pdd" />
                    <el-option label="1688" value="1688" />
                  </el-select>
                </el-form-item>
                <el-form-item label="关键词" prop="keyword">
                  <el-input v-model="form.keyword" placeholder="搜索关键词" />
                </el-form-item>
                <el-form-item label="最大数量" prop="maxCount">
                  <el-input-number v-model="form.maxCount" :min="10" :max="500" style="width:100%" />
                </el-form-item>
                <el-form-item label="供应商要求">
                  <div style="display:flex;flex-direction:column;gap:6px;">
                    <el-checkbox v-model="form.filterDropShipping">🚚 支持一件代发</el-checkbox>
                    <el-checkbox v-model="form.filterDelivery48h">⚡ 48小时内发货</el-checkbox>
                    <el-checkbox v-model="form.filterFreeShipping">🆓 包邮</el-checkbox>
                    <el-checkbox v-model="form.filterDouyinSheet">🎵 抖音电子面单</el-checkbox>
                  </div>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="submitForm" :loading="submitting" style="width:100%">
                    创建并执行
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>

          <!-- 任务列表 -->
          <el-col :span="16">
            <el-card shadow="hover">
              <template #header>
                <div style="display:flex;justify-content:space-between;align-items:center;">
                  <span>采集任务列表</span>
                  <el-button size="small" @click="loadTasks">刷新</el-button>
                </div>
              </template>
              <el-table :data="tasks" v-loading="loading" stripe>
                <el-table-column prop="taskName" label="任务名称" min-width="120" show-overflow-tooltip />
                <el-table-column prop="platform" label="平台" width="80">
                  <template #default="{ row }">
                    <el-tag size="small">{{ platformMap[row.platform] || row.platform }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="keyword" label="关键词" width="100" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="90">
                  <template #default="{ row }">
                    <el-tooltip
                      v-if="row.status === 'FAILED' && row.errorMsg"
                      :content="row.errorMsg"
                      placement="top"
                      effect="dark"
                      :show-after="300"
                      popper-class="error-tooltip"
                    >
                      <el-tag :type="statusType[row.status]" size="small" style="cursor:pointer;">
                        {{ statusLabel[row.status] }} ⚠️
                      </el-tag>
                    </el-tooltip>
                    <el-tag v-else :type="statusType[row.status]" size="small">{{ statusLabel[row.status] }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="进度" width="110">
                  <template #default="{ row }">
                    <span>{{ row.successCount || 0 }} / {{ row.totalCount || 0 }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="150" show-overflow-tooltip />
                <el-table-column label="补全状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.fillDetailStatus === 'RUNNING'" type="warning" size="small">补全中</el-tag>
                    <el-tag v-else-if="row.fillDetailStatus === 'DONE'" type="success" size="small">
                      {{ row.fillDetailSuccess }}/{{ row.fillDetailCount }}
                    </el-tag>
                    <el-tag v-else-if="row.fillDetailStatus === 'FAILED'" type="danger" size="small">补全失败</el-tag>
                    <span v-else style="color:#bbb;font-size:12px;">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="230" fixed="right">
                  <template #default="{ row }">
                    <el-text
                      type="primary"
                      size="small"
                      :style="{ cursor: row.status !== 'RUNNING' ? 'not-allowed' : 'pointer', opacity: row.status !== 'RUNNING' ? 0.4 : 1, marginRight: '8px' }"
                      @click="row.status === 'RUNNING' && syncTask(row)"
                    >同步</el-text>
                    <el-text
                      type="warning"
                      size="small"
                      :style="{ cursor: row.status !== 'FAILED' ? 'not-allowed' : 'pointer', opacity: row.status !== 'FAILED' ? 0.4 : 1, marginRight: '8px' }"
                      @click="row.status === 'FAILED' && retryTask(row)"
                    >重试</el-text>
                    <el-text
                      type="success"
                      size="small"
                      :style="{ cursor: row.status !== 'SUCCESS' ? 'not-allowed' : 'pointer', opacity: row.status !== 'SUCCESS' ? 0.4 : 1, marginRight: '8px' }"
                      @click="row.status === 'SUCCESS' && fillDetailByTask(row)"
                    >补全详情</el-text>
                    <el-text
                      v-if="row.status === 'FAILED' && row.errorMsg"
                      type="danger"
                      size="small"
                      style="cursor:pointer; margin-right:8px;"
                      @click="showError(row)"
                    >原因</el-text>
                    <el-text
                      type="danger"
                      size="small"
                      :style="{ cursor: row.status === 'RUNNING' ? 'not-allowed' : 'pointer', opacity: row.status === 'RUNNING' ? 0.4 : 1 }"
                      @click="row.status !== 'RUNNING' && clearProducts(row)"
                    >清除商品</el-text>
                  </template>
                </el-table-column>
              </el-table>
              <el-pagination
                v-model:current-page="pagination.page"
                v-model:page-size="pagination.size"
                :total="pagination.total"
                layout="total, prev, pager, next"
                style="margin-top:12px;justify-content:flex-end;display:flex;"
                @current-change="loadTasks"
              />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 关键词配置 -->
      <el-tab-pane label="⚙️ 关键词配置" name="keywords">        <el-card shadow="hover">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <span>关键词配置列表</span>
              <el-button type="primary" size="small" @click="openKwDialog(null)">+ 新增</el-button>
            </div>
          </template>
          <el-table :data="keywords" v-loading="kwLoading" stripe>
            <el-table-column prop="keyword" label="关键词" min-width="120" />
            <el-table-column prop="platform" label="平台" width="80">
              <template #default="{ row }"><el-tag size="small">{{ row.platform }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="category" label="品类" width="100" />
            <el-table-column prop="priority" label="优先级" width="80" />
            <el-table-column prop="maxCount" label="采集数量" width="90" />
            <el-table-column prop="cronExpr" label="Cron表达式" width="140" show-overflow-tooltip />
            <el-table-column label="启用" width="80">
              <template #default="{ row }">
                <el-switch :model-value="row.enabled === 1" @change="toggleKw(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="openKwDialog(row)">编辑</el-button>
                <el-button size="small" type="success" link @click="triggerKw(row)">立即触发</el-button>
                <el-button size="small" type="danger" link @click="deleteKw(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- 采集平台配置 -->
      <el-tab-pane label="🔌 采集平台" name="platform">
        <el-card shadow="hover">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <span>采集平台配置
                <el-tag size="small" type="success" v-if="activePlatform">当前激活: {{ activePlatform.name }}</el-tag>
              </span>
              <el-button type="primary" size="small" @click="openPlatformDialog(null)">+ 新增平台</el-button>
            </div>
          </template>
          <el-table :data="platforms" v-loading="platformLoading" stripe>
            <el-table-column prop="name" label="平台名称" min-width="130" />
            <el-table-column prop="platformType" label="类型" width="130">
              <template #default="{ row }">
                <el-tag
                  :type="row.platformType === 'OPENCLAW' ? 'warning' : row.platformType === 'ALI_OPEN' ? 'danger' : 'primary'"
                  size="small"
                >
                  {{ row.platformType === 'OPENCLAW' ? 'OpenClaw' : row.platformType === 'ALI_OPEN' ? '1688开放平台' : '99API' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="apiUrl" label="API地址" min-width="200" show-overflow-tooltip />
            <el-table-column label="API Key" min-width="140">
              <template #default="{ row }">{{ row.apiKey ? row.apiKey.substring(0,8) + '****' : '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">{{ row.isActive === 1 ? '✅ 激活' : '未激活' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-text
                  type="success"
                  size="small"
                  :style="{ cursor: row.isActive === 1 ? 'not-allowed' : 'pointer', opacity: row.isActive === 1 ? 0.4 : 1, marginRight: '10px' }"
                  @click="row.isActive !== 1 && activatePlatform(row)"
                >激活</el-text>
                <el-text
                  type="primary"
                  size="small"
                  style="cursor:pointer; margin-right:10px;"
                  @click="openPlatformDialog(row)"
                >编辑</el-text>
                <el-text
                  type="danger"
                  size="small"
                  :style="{ cursor: row.isActive === 1 ? 'not-allowed' : 'pointer', opacity: row.isActive === 1 ? 0.4 : 1 }"
                  @click="row.isActive !== 1 && deletePlatform(row)"
                >删除</el-text>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 平台配置弹窗 -->
    <el-dialog v-model="platformDialogVisible" :title="platformForm.id ? '编辑平台配置' : '新增平台配置'" width="560px">
      <el-form :model="platformForm" label-width="120px">
        <el-form-item label="平台名称"><el-input v-model="platformForm.name" placeholder="如: OpenClaw、99API、1688开放平台" /></el-form-item>
        <el-form-item label="平台类型">
          <el-select v-model="platformForm.platformType" style="width:100%">
            <el-option label="OpenClaw (api-gw.onebound.cn)" value="OPENCLAW" />
            <el-option label="99API (api.99api.com)" value="API99" />
            <el-option label="1688开放平台 (gw.open.1688.com)" value="ALI_OPEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="API / 网关地址">
          <el-input
            v-model="platformForm.apiUrl"
            :placeholder="platformForm.platformType === 'OPENCLAW' ? 'https://api-gw.onebound.cn'
              : platformForm.platformType === 'ALI_OPEN' ? 'https://gw.open.1688.com（可留空使用默认）'
              : 'https://api.99api.com'"
          />
        </el-form-item>
        <el-form-item :label="platformForm.platformType === 'ALI_OPEN' ? 'App Key' : 'API Key'">
          <el-input v-model="platformForm.apiKey" :placeholder="platformForm.platformType === 'ALI_OPEN' ? '应用 App Key' : 'API Key'" />
        </el-form-item>
        <el-form-item
          :label="platformForm.platformType === 'ALI_OPEN' ? 'App Secret' : 'API Secret'"
          v-if="platformForm.platformType !== 'API99'"
        >
          <el-input v-model="platformForm.apiSecret"
            :placeholder="platformForm.platformType === 'ALI_OPEN' ? '应用 App Secret' : 'OpenClaw 专用 Secret'"
          />
        </el-form-item>
        <el-form-item label="Access Token" v-if="platformForm.platformType === 'ALI_OPEN'">
          <el-input v-model="platformForm.accessToken" placeholder="OAuth2 Access Token（可选，部分接口需要）" />
          <div style="font-size:12px;color:#909399;margin-top:4px;">
            💡 通过1688开放平台 OAuth2 授权获取，不填则使用服务端签名调用（无需用户授权的接口）
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="platformForm.remark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="立即激活">
          <el-switch v-model="platformForm.isActive" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <!-- 1688开放平台说明 -->
        <el-alert
          v-if="platformForm.platformType === 'ALI_OPEN'"
          type="info"
          :closable="false"
          show-icon
          style="margin-top:4px;"
        >
          <template #title>
            <span style="font-size:12px;">
              1688开放平台需在
              <a href="https://open.1688.com" target="_blank" style="color:#409eff;">open.1688.com</a>
              申请应用，获取 App Key 和 App Secret。
              搜索接口：<code>alibaba.product.search</code>，详情：<code>alibaba.product.get</code>
            </span>
          </template>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="platformDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlatform" :loading="platformSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 错误详情弹窗 -->
    <el-dialog v-model="errorDialogVisible" title="任务失败原因" width="560px">
      <div style="background:#fff7f7;border:1px solid #ffccc7;border-radius:6px;padding:16px;">
        <div style="font-size:13px;color:#333;margin-bottom:8px;">
          <strong>任务：</strong>{{ errorTask.taskName }}（{{ errorTask.keyword }}）
        </div>
        <el-divider style="margin:8px 0;" />
        <pre style="white-space:pre-wrap;word-break:break-all;font-size:13px;color:#cf1322;line-height:1.7;margin:0;">{{ errorTask.errorMsg }}</pre>
      </div>
      <template #footer>
        <el-button @click="errorDialogVisible = false">关闭</el-button>
        <el-button type="warning" @click="retryTask(errorTask); errorDialogVisible = false">重新执行</el-button>
      </template>
    </el-dialog>

    <!-- 关键词编辑弹窗 -->
    <el-dialog v-model="kwDialogVisible" :title="kwForm.id ? '编辑关键词' : '新增关键词'" width="480px">
      <el-form :model="kwForm" label-width="100px">
        <el-form-item label="关键词"><el-input v-model="kwForm.keyword" /></el-form-item>
        <el-form-item label="平台">
          <el-select v-model="kwForm.platform" style="width:100%">
            <el-option label="1688" value="1688" />
            <el-option label="淘宝" value="taobao" />
            <el-option label="京东" value="jd" />
          </el-select>
        </el-form-item>
        <el-form-item label="品类"><el-input v-model="kwForm.category" /></el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="kwForm.priority" :min="1" :max="10" style="width:100%" />
        </el-form-item>
        <el-form-item label="采集数量">
          <el-input-number v-model="kwForm.maxCount" :min="10" :max="200" style="width:100%" />
        </el-form-item>
        <el-form-item label="Cron表达式"><el-input v-model="kwForm.cronExpr" placeholder="0 0 6 * * ?" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="kwDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveKw" :loading="kwSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Loading } from '@element-plus/icons-vue'
import { collectApi, keywordApi, productApi } from '@/api'

const activeTab = ref('tasks')

// --- AI关键词推荐 ---
const categoryOptions = [
  { value: '女装', label: '女装', icon: '👗' },
  { value: '男装', label: '男装', icon: '👔' },
  { value: '童装', label: '童装', icon: '🧒' },
  { value: '美妆护肤', label: '美妆护肤', icon: '💄' },
  { value: '生活用品', label: '生活用品', icon: '🏠' },
  { value: '食品零食', label: '食品零食', icon: '🍜' },
  { value: '数码电子', label: '数码电子', icon: '📱' },
  { value: '母婴用品', label: '母婴用品', icon: '👶' },
  { value: '运动户外', label: '运动户外', icon: '⛺' },
  { value: '宠物用品', label: '宠物用品', icon: '🐾' },
]
const selectedCategory = ref('')
const customInput = ref('')
const recommending = ref(false)
const recommendLoaded = ref(false)
const recommendations = ref([])

function toggleCategory(val) {
  selectedCategory.value = selectedCategory.value === val ? '' : val
}

async function getRecommend() {
  recommending.value = true
  recommendLoaded.value = false
  recommendations.value = []
  try {
    const params = {}
    if (selectedCategory.value) params.category = selectedCategory.value
    if (customInput.value?.trim()) params.customInput = customInput.value.trim()
    const res = await productApi.recommendKeywords(params)
    if (res.code === 200) {
      recommendations.value = res.data || []
      recommendLoaded.value = true
    }
  } finally {
    recommending.value = false
  }
}

function applyKeyword(item) {
  form.value.keyword = item.keyword
  if (!form.value.taskName) form.value.taskName = item.keyword + '采集'
  ElMessage.success(`已填入关键词「${item.keyword}」`)
}

// --- 采集任务 ---
const form = ref({ taskName: '', platform: '1688', keyword: '', maxCount: 100, filterDropShipping: false, filterDelivery48h: false, filterFreeShipping: false, filterDouyinSheet: false })
const rules = {
  taskName: [{ required: true, message: '请输入任务名称' }],
  platform: [{ required: true, message: '请选择平台' }],
  keyword:  [{ required: true, message: '请输入关键词' }],
  maxCount: [{ required: true, message: '请输入最大数量' }],
}
const formRef = ref()
const submitting = ref(false)
const loading = ref(false)
const tasks = ref([])
const pagination = ref({ page: 1, size: 10, total: 0 })
const platformMap = { taobao: '淘宝', jd: '京东', pdd: '拼多多', '1688': '1688' }
const statusType  = { PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger' }
const statusLabel = { PENDING: '待执行', RUNNING: '执行中', SUCCESS: '已完成', FAILED: '失败' }

async function submitForm() {
  if (submitting.value) return  // 防止重复提交
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = await collectApi.createTask(form.value)
    if (res.code === 200) {
      await collectApi.executeTask(res.data.id)
      ElMessage.success('任务创建并开始执行')
      loadTasks()
    }
  } finally {
    submitting.value = false
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await collectApi.getTasks({ page: pagination.value.page, size: pagination.value.size })
    if (res.code === 200) {
      tasks.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function syncTask(row) {
  const res = await collectApi.syncTask(row.id)
  if (res.code === 200) { Object.assign(row, res.data); ElMessage.success('已同步') }
}

async function retryTask(row) {
  const res = await collectApi.retryTask(row.id)
  if (res.code === 200) { Object.assign(row, res.data); ElMessage.success('已重新提交') }
}

async function clearProducts(row) {
  await ElMessageBox.confirm(
    `确认清除任务「${row.taskName}」采集的全部商品数据？此操作不可恢复。`,
    '清除商品数据',
    { type: 'warning', confirmButtonText: '确认清除', confirmButtonClass: 'el-button--danger' }
  )
  const res = await collectApi.clearTaskProducts(row.id)
  if (res.code === 200) {
    Object.assign(row, { totalCount: 0, successCount: 0, status: 'PENDING' })
    ElMessage.success(`已清除 ${res.data.deleted} 条商品数据`)
  }
}

async function fillDetailByTask(row) {
  try {
    await ElMessageBox.confirm(`将通过 item_get 接口补全任务「${row.taskName}」中缺少发货地/代发价的商品，确认继续？`, '补全商品详情', { type: 'info', confirmButtonText: '确认' })
  } catch { return }
  Object.assign(row, { fillDetailStatus: 'RUNNING' })
  try {
    const res = await collectApi.fillDetailByTask(row.id)
    if (res.code === 200) {
      const { total, success, failed } = res.data
      Object.assign(row, { fillDetailStatus: 'DONE', fillDetailCount: total, fillDetailSuccess: success, fillDetailFailed: failed })
      ElMessage.success(`补全完成：共 ${total} 件，成功 ${success} 件，失败 ${failed} 件`)
    }
  } catch {
    Object.assign(row, { fillDetailStatus: 'FAILED' })
  }
}

// 错误详情
const errorDialogVisible = ref(false)
const errorTask = ref({})
function showError(row) {
  errorTask.value = row
  errorDialogVisible.value = true
}

let pollTimer = null
function startPolling() {
  pollTimer = setInterval(() => {
    const running = tasks.value.filter(t => t.status === 'RUNNING')
    running.forEach(t => syncTask(t))
  }, 10000)
}

// --- 关键词配置 ---
const keywords = ref([])
const kwLoading = ref(false)
const kwDialogVisible = ref(false)
const kwForm = ref({ keyword: '', platform: '1688', category: '', priority: 5, maxCount: 50, cronExpr: '0 0 6 * * ?' })
const kwSaving = ref(false)

async function loadKeywords() {
  kwLoading.value = true
  try {
    const res = await keywordApi.list()
    if (res.code === 200) keywords.value = res.data || []
  } finally {
    kwLoading.value = false
  }
}

function openKwDialog(row) {
  if (row) {
    kwForm.value = { ...row }
  } else {
    kwForm.value = { keyword: '', platform: '1688', category: '', priority: 5, maxCount: 50, cronExpr: '0 0 6 * * ?' }
  }
  kwDialogVisible.value = true
}

async function saveKw() {
  kwSaving.value = true
  try {
    const res = kwForm.value.id
      ? await keywordApi.update(kwForm.value.id, kwForm.value)
      : await keywordApi.create(kwForm.value)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      kwDialogVisible.value = false
      loadKeywords()
    }
  } finally {
    kwSaving.value = false
  }
}

async function toggleKw(row) {
  const res = await keywordApi.update(row.id, { ...row, enabled: row.enabled === 1 ? 0 : 1 })
  if (res.code === 200) { row.enabled = row.enabled === 1 ? 0 : 1 }
}

async function triggerKw(row) {
  const res = await keywordApi.trigger(row.id)
  if (res.code === 200) { ElMessage.success('已触发采集，请到采集任务页面查看'); activeTab.value = 'tasks'; loadTasks() }
}

async function deleteKw(row) {
  await ElMessageBox.confirm('确认删除该关键词配置?', '提示', { type: 'warning' })
  const res = await keywordApi.remove(row.id)
  if (res.code === 200) { keywords.value = keywords.value.filter(k => k.id !== row.id); ElMessage.success('已删除') }
}

// --- 采集平台配置 ---
const platforms = ref([])
const platformLoading = ref(false)
const platformDialogVisible = ref(false)
const platformForm = ref({ id: null, name: '', platformType: 'OPENCLAW', apiUrl: '', apiKey: '', apiSecret: '', accessToken: '', remark: '', isActive: 1 })
const platformSaving = ref(false)
const activePlatform = ref(null)

async function loadPlatforms() {
  platformLoading.value = true
  try {
    const res = await collectApi.getPlatforms()
    if (res.code === 200) {
      platforms.value = res.data || []
      // 只更新 activePlatform 显示，不调用 activatePlatform API（避免每次刷新都切换平台）
      activePlatform.value = (res.data || []).find(p => p.isActive === 1) || null
    }
  } finally {
    platformLoading.value = false
  }
}

function openPlatformDialog(row) {
  if (row) {
    platformForm.value = { accessToken: '', ...row }
  } else {
    platformForm.value = { id: null, name: '', platformType: 'OPENCLAW', apiUrl: '', apiKey: '', apiSecret: '', accessToken: '', remark: '', isActive: 1 }
  }
  platformDialogVisible.value = true
}

async function savePlatform() {
  platformSaving.value = true
  try {
    const res = platformForm.value.id
      ? await collectApi.updatePlatform(platformForm.value.id, platformForm.value)
      : await collectApi.createPlatform(platformForm.value)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      platformDialogVisible.value = false
      loadPlatforms()
    }
  } finally {
    platformSaving.value = false
  }
}

async function activatePlatform(row) {
  const res = await collectApi.activatePlatform(row.id)
  if (res.code === 200) {
    activePlatform.value = row
    ElMessage.success('平台已激活')
    loadPlatforms()
  }
}

async function deletePlatform(row) {
  await ElMessageBox.confirm('确认删除该平台配置?', '提示', { type: 'warning' })
  const res = await collectApi.removePlatform(row.id)
  if (res.code === 200) { platforms.value = platforms.value.filter(p => p.id !== row.id); ElMessage.success('已删除') }
}

onMounted(() => { loadTasks(); loadKeywords(); loadPlatforms(); startPolling() })
onUnmounted(() => clearInterval(pollTimer))
</script>

<style scoped>
.rec-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f0f7ff, #e6f0ff);
  border: 1px solid #c6d9f7;
  cursor: pointer;
  transition: all 0.2s;
  max-width: 280px;
}
.rec-tag:hover {
  background: linear-gradient(135deg, #409eff22, #409eff33);
  border-color: #409eff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64,158,255,0.2);
}
.rec-keyword {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a2e;
  white-space: nowrap;
}
.rec-reason {
  font-size: 11px;
  color: #909399;
  margin-left: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 5px 13px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #555;
  background: #f5f7fa;
  border: 1.5px solid #dcdfe6;
  cursor: pointer;
  transition: all 0.18s;
  user-select: none;
}
.cat-tag:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}
.cat-tag--active {
  background: #409eff;
  border-color: #1a6fd4;
  color: #fff;
  box-shadow: 0 2px 8px rgba(64,158,255,0.45);
  font-weight: 700;
}
</style>



