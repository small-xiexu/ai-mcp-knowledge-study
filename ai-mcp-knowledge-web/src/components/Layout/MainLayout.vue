<template>
  <div class="gemini-main-layout">
    <!-- 系统侧边栏 -->
    <aside
      class="app-sidebar"
      :class="{ collapsed: appStore.sidebarCollapsed }"
    >
      <div class="sidebar-top">
        <el-button 
          link 
          class="menu-toggle-btn" 
          @click="appStore.toggleSidebar"
        >
          <el-icon><MenuIcon /></el-icon>
        </el-button>
        
        <div v-show="!appStore.sidebarCollapsed" class="logo-text">
          Gemini
        </div>
      </div>



      <nav class="nav-menu">
        <el-tooltip
          v-for="route in menuRoutes"
          :key="route.path"
          :content="route.meta?.title"
          placement="right"
          :disabled="!appStore.sidebarCollapsed"
          effect="dark"
          :offset="12"
        >
          <router-link
            :to="resolveMenuPath(route.path)"
            class="nav-item"
            :class="{ active: activeMenu === resolveMenuPath(route.path) }"
          >
            <div class="nav-icon-container">
              <el-icon><component :is="route.meta?.icon" /></el-icon>
            </div>
            <span
              v-show="!appStore.sidebarCollapsed"
              class="nav-label"
            >
              {{ route.meta?.title }}
            </span>
          </router-link>
        </el-tooltip>
      </nav>

      <div class="sidebar-bottom">
        <el-tooltip content="设置" placement="right" :disabled="!appStore.sidebarCollapsed" effect="dark" :offset="12">
          <div class="nav-item settings-item">
            <div class="nav-icon-container">
               <el-icon><Setting /></el-icon>
            </div>
            <span v-show="!appStore.sidebarCollapsed" class="nav-label">设置</span>
          </div>
        </el-tooltip>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="app-content">
      <!-- 顶部中央标题 (Gemini Style) -->
      <header class="app-header">
        <div class="header-center">
          <span class="page-title">{{ currentPageTitle }}</span>
          <el-icon class="title-arrow"><ArrowDown /></el-icon>
        </div>
        <div class="header-actions">
           <!-- Profile/Actions -->
           <div class="user-profile">
             <span class="pro-tag">PRO</span>
             <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
           </div>
        </div>
      </header>

      <div class="page-container">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import routes from '@/router/routes'
import { Menu as MenuIcon, Setting, ArrowDown } from '@element-plus/icons-vue'

const appStore = useAppStore()
const route = useRoute()




const menuRoutes = computed(() => {
  const layoutRoute = routes.find(r => r.path === '/')
  return layoutRoute?.children || []
})

const activeMenu = computed(() => {
  // 简单匹配，假设路由结构不深
  return route.path
})

const currentPageTitle = computed(() => {
  return route.meta.title || 'Dashboard'
})

const resolveMenuPath = (path?: string) => {
  if (!path) return '/'
  return path.startsWith('/') ? path : `/${path}`
}
</script>

<style scoped>
.gemini-main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: var(--gemini-bg-primary);
  color: var(--gemini-text-primary);
  overflow: hidden;
  font-family: var(--gemini-font);
}

/* Sidebar */
.app-sidebar {
  width: 260px;
  background-color: var(--gemini-bg-primary);
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  transition: width 0.3s cubic-bezier(0.2, 0, 0, 1), transform 0.3s ease;
  flex-shrink: 0;
  z-index: 100;
}

.app-sidebar.collapsed {
  width: 72px;
  padding: 12px 14px;
}

.sidebar-top {
  height: 60px;
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  gap: 12px;
}

.menu-toggle-btn {
  font-size: 24px;
  color: var(--gemini-text-secondary);
  padding: 8px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.menu-toggle-btn:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.logo-text {
  font-size: 22px;
  font-weight: 600;
  color: #e8eaed;
  letter-spacing: -0.5px;
}

.new-chat-wrapper {
  margin-bottom: 32px;
}

.new-chat-btn {
  height: 48px;
  width: 140px;
  border-radius: 24px;
  background-color: #1e1f20;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--gemini-text-primary);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 16px;
  gap: 12px;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-btn.only-icon {
  width: 44px;
  padding: 0;
  justify-content: center;
  border-radius: 16px;
}

.new-chat-btn:hover {
  background-color: #2a2b2e;
  border-color: rgba(255, 255, 255, 0.2);
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 16px;
  border-radius: 24px;
  text-decoration: none;
  color: #e8eaed; /* Enhanced contrast */
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.app-sidebar.collapsed .nav-item {
  width: 48px;
  padding: 0;
  justify-content: center;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.08); /* More subtle hover */
}

.nav-item.active {
  background-color: rgba(138, 180, 248, 0.15); /* More vibrant active state */
  color: var(--gemini-accent);
}

.nav-icon-container {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.app-sidebar:not(.collapsed) .nav-icon-container {
  margin-right: 12px;
}

.app-sidebar.collapsed .nav-icon-container {
  margin-right: 0;
}

.nav-label {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  color: inherit;
}

.sidebar-bottom {
  margin-top: auto;
  padding-bottom: 24px;
}

.settings-item {
  margin-top: 10px;
}

/* Content Area */
.app-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #0e0e10; /* Surface-dim look */
  border-top-left-radius: 28px;
  border-bottom-left-radius: 28px;
  overflow: hidden;
  position: relative;
}

.app-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 10;
}

.header-center {
  background-color: rgba(30, 31, 32, 0.7);
  padding: 8px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.header-center:hover {
  background-color: rgba(42, 43, 46, 0.9);
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--gemini-text-primary);
}

.title-arrow {
  font-size: 12px;
  color: var(--gemini-text-secondary);
}

.header-actions {
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pro-tag {
  background: linear-gradient(90deg, #4285f4, #9b72cb, #d96570);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  font-weight: 700;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 2px 8px;
  border-radius: 12px;
}

.page-container {
  flex: 1;
  overflow: auto;
  padding: 0;
  position: relative;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
