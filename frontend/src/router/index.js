import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '',        name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'collect', name: 'Collect',   component: () => import('@/views/Collect.vue'),   meta: { title: '数据采集' } },
      { path: 'ranking', name: 'Ranking',   component: () => import('@/views/Ranking.vue'),   meta: { title: '商品排行' } },
      { path: 'trend',   name: 'Trend',     component: () => import('@/views/Trend.vue'),     meta: { title: '趋势分析' } },
      { path: 'task',    name: 'Task',      component: () => import('@/views/Task.vue'),      meta: { title: '任务管理' } },
      { path: 'ai-analysis', name: 'AiAnalysis', component: () => import('@/views/AiAnalysis.vue'), meta: { title: 'AI选品分析' } },
      { path: 'product/:id', name: 'ProductDetail', component: () => import('@/views/ProductDetail.vue'), meta: { title: '商品详情' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.isLoggedIn()) {
    return '/login'
  }
  if (to.path === '/login' && authStore.isLoggedIn()) {
    return '/'
  }
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '选品系统'
})

export default router
