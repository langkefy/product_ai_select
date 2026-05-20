import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.href = '/login'
      return Promise.reject(err)
    }
    const msg = err.response?.data?.msg || err.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

// 商品 API
export const productApi = {
  list: (params) => http.get('/products', { params }),
  detail: (id) => http.get(`/products/${id}`),
  aiAnalyze: (data) => http.post('/products/ai-analyze', data),
  aiAnalyzeAll: () => http.post('/products/ai-analyze-all'),
  remove: (id) => http.delete(`/products/${id}`),
  categories: () => http.get('/products/categories'),
  recommendKeywords: (params) => http.get('/products/recommend-keywords', { params }),
}

// 采集 API
export const collectApi = {
  createTask: (data) => http.post('/collect/task', data),
  executeTask: (id) => http.post(`/collect/task/${id}/execute`),
  syncTask: (id) => http.get(`/collect/task/${id}/sync`),
  getTasks: (params) => http.get('/collect/tasks', { params }),
  retryTask: (id) => http.post(`/collect/task/${id}/retry`),
  fillDetail: () => http.post('/collect/fill-detail'),
  fillDetailByTask: (id) => http.post(`/collect/task/${id}/fill-detail`),
  clearTaskProducts: (id) => http.delete(`/collect/task/${id}/products`),
  // 平台配置
  getPlatforms: () => http.get('/platform-config'),
  createPlatform: (data) => http.post('/platform-config', data),
  updatePlatform: (id, data) => http.put(`/platform-config/${id}`, data),
  activatePlatform: (id) => http.post(`/platform-config/${id}/activate`),
  removePlatform: (id) => http.delete(`/platform-config/${id}`),
}

// 关键词配置 API
export const keywordApi = {
  list: () => http.get('/keywords'),
  create: (data) => http.post('/keywords', data),
  update: (id, data) => http.put(`/keywords/${id}`, data),
  remove: (id) => http.delete(`/keywords/${id}`),
  trigger: (id) => http.post(`/keywords/${id}/trigger`),
}

// 排行 API
export const rankingApi = {
  top: (params) => http.get('/ranking/top', { params }),
  aiScore: (params) => http.get('/ranking/ai-score', { params }),
  report: (params) => http.get('/ranking/report', { params }),
  history: (productId, params) => http.get(`/ranking/history/${productId}`, { params }),
  exportUrl: (params) => {
    const qs = new URLSearchParams(params).toString()
    return `/api/ranking/export?${qs}`
  },
}

// 趋势 API
export const trendApi = {
  productTrend: (id, params) => http.get(`/trend/product/${id}`, { params }),
  dashboard: () => http.get('/trend/dashboard'),
  categoryTrend: (params) => http.get('/trend/category', { params }),
}


// AI 分析报告 API
export const aiReportApi = {
  generate: (params) => http.post('/ai-analysis/generate', null, { params }),
  list: (params) => http.get('/ai-analysis/list', { params }),
  detail: (id) => http.get(`/ai-analysis/${id}`),
}

export default http
