<template>
  <el-container style="height:100vh;background:#f0f2f7;">

    <!-- ===== 移动端顶栏 ===== -->
    <div class="mobile-header" v-if="isMobile">
      <el-button text @click="drawerVisible=true" class="menu-btn">
        <el-icon size="22"><Fold /></el-icon>
      </el-button>
      <span class="mobile-title">{{ $route.meta.title || '选品系统' }}</span>
      <el-dropdown @command="handleCommand">
        <el-avatar :size="32" class="avatar-btn">{{ username.slice(-2) }}</el-avatar>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 移动端抽屉 -->
    <el-drawer v-model="drawerVisible" direction="ltr" :size="240" :with-header="false" v-if="isMobile">
      <div class="sidebar-inner">
        <div class="logo-area">🛒 <span>选品系统</span></div>
        <el-menu :default-active="$route.path" router @select="drawerVisible=false"
          background-color="transparent" text-color="rgba(255,255,255,0.75)" active-text-color="#fff" class="side-menu">
          <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon><span>{{ m.label }}</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <!-- ===== PC侧边栏 ===== -->
    <div v-if="!isMobile" class="sidebar">
      <div class="logo-area">🛒 <span>选品系统</span></div>
      <el-menu :default-active="$route.path" router
        background-color="transparent" text-color="rgba(255,255,255,0.75)" active-text-color="#fff" class="side-menu">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon><span>{{ m.label }}</span>
        </el-menu-item>
      </el-menu>
      <!-- 底部用户信息 -->
      <div class="sidebar-bottom">
        <el-avatar :size="32" class="avatar-btn">{{ username.slice(-2) }}</el-avatar>
        <span class="username-text">{{ username }}</span>
        <el-button text class="logout-btn" @click="handleCommand('logout')">
          <el-icon><SwitchButton /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- ===== 主内容区 ===== -->
    <div class="main-wrapper" :style="isMobile ? 'padding-top:56px' : ''">
      <!-- PC顶栏 -->
      <div v-if="!isMobile" class="top-header">
        <div class="page-breadcrumb">
          <span class="page-title">{{ $route.meta.title }}</span>
        </div>
        <div class="header-right">
          <span class="date-text">{{ todayStr }}</span>
        </div>
      </div>
      <!-- 内容 -->
      <div class="page-content">
        <router-view />
      </div>
    </div>

  </el-container>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const username = ref(authStore.username)
const drawerVisible = ref(false)
const isMobile = ref(window.innerWidth < 768)

const todayStr = new Date().toLocaleDateString('zh-CN', { year:'numeric', month:'long', day:'numeric', weekday:'long' })

const menus = [
  { path: '/',        label: '数据看板', icon: 'DataBoard'   },
  { path: '/collect', label: '数据采集', icon: 'Download'    },
  { path: '/ranking', label: '商品排行', icon: 'Trophy'      },
  { path: '/trend',   label: '趋势分析', icon: 'TrendCharts' },
  { path: '/task',       label: '任务管理', icon: 'List'        },
  { path: '/ai-analysis', label: 'AI选品分析', icon: 'MagicStick' },
]

function onResize() { isMobile.value = window.innerWidth < 768 }
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    authStore.logout()
    router.replace('/login')
  }
}
</script>

<style scoped>
/* ===== 侧边栏 ===== */
.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: linear-gradient(180deg, #1a1a3e 0%, #16213e 60%, #0f3460 100%);
  display: flex;
  flex-direction: column;
  height: 100vh;
  position: relative;
  overflow: hidden;
}
.sidebar::before {
  content: '';
  position: absolute;
  top: -60px; right: -60px;
  width: 180px; height: 180px;
  background: rgba(64,158,255,0.12);
  border-radius: 50%;
}

.sidebar-inner {
  background: linear-gradient(180deg, #1a1a3e 0%, #0f3460 100%);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.logo-area {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  letter-spacing: 1px;
}
.logo-area span { background: linear-gradient(90deg, #409eff, #a78bfa); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }

.side-menu {
  flex: 1;
  border: none !important;
  padding: 10px 8px;
}
:deep(.side-menu .el-menu-item) {
  border-radius: 10px !important;
  margin: 3px 0;
  height: 46px;
  transition: all 0.2s;
}
:deep(.side-menu .el-menu-item:hover) {
  background: rgba(255,255,255,0.1) !important;
  color: #fff !important;
}
:deep(.side-menu .el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(64,158,255,0.35), rgba(90,120,255,0.35)) !important;
  color: #fff !important;
  box-shadow: 0 2px 12px rgba(64,158,255,0.25);
}

.sidebar-bottom {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid rgba(255,255,255,0.08);
  margin: 0 8px 8px;
}
.avatar-btn { background: linear-gradient(135deg,#409eff,#a78bfa) !important; cursor: pointer; font-size: 12px; font-weight: 600; }
.username-text { flex: 1; color: rgba(255,255,255,0.75); font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.logout-btn { color: rgba(255,255,255,0.5) !important; }
.logout-btn:hover { color: #f56c6c !important; }

/* ===== 主内容区 ===== */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.top-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #eef0f6;
  flex-shrink: 0;
  box-shadow: 0 1px 8px rgba(0,0,0,0.04);
}
.page-title { font-size: 17px; font-weight: 700; color: #1a1a2e; }
.date-text { font-size: 13px; color: #999; }

.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

/* ===== 移动端顶栏 ===== */
.mobile-header {
  position: fixed; top: 0; left: 0; right: 0;
  height: 56px;
  background: linear-gradient(135deg, #1a1a3e, #0f3460);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  z-index: 1000;
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
}
.mobile-title { color: #fff; font-size: 16px; font-weight: 700; }
.menu-btn { color: #fff !important; }

:deep(.el-drawer__body) { padding: 0; background: #1a1a3e; }

@media (max-width: 767px) {
  .page-content { padding: 12px; }
}
</style>
