<template>
  <el-container class="main-layout">
    <el-aside :width="sidebarWidth" class="sidebar">
      <div class="logo" :class="{ collapsed: appStore.sidebarCollapsed }">
        <h2>
          <span class="logo-full">AI MCP</span>
          <span class="logo-short">AI</span>
        </h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        class="sidebar-menu"
        router
      >
        <el-menu-item
          v-for="route in menuRoutes"
          :key="route.path"
          :index="resolveMenuPath(route.path)"
        >
          <router-link :to="resolveMenuPath(route.path)" class="menu-link">
            <el-icon><component :is="route.meta?.icon" /></el-icon>
            <span class="menu-title">{{ route.meta?.title }}</span>
          </router-link>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <span>AI 多模型编排管理系统</span>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/app'
import routes from '@/router/routes'

const appStore = useAppStore()
const route = useRoute()

// 获取菜单路由
const menuRoutes = computed(() => {
  const layoutRoute = routes.find(r => r.path === '/')
  return layoutRoute?.children || []
})

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 侧边栏宽度
const sidebarWidth = computed(() => appStore.sidebarCollapsed ? '64px' : '200px')

const resolveMenuPath = (path?: string) => {
  if (!path) return '/'
  return path.startsWith('/') ? path : `/${path}`
}

</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
}

.logo h2 {
  margin: 0;
  font-size: inherit;
}

.logo-short {
  display: none;
}

.logo.collapsed .logo-full {
  display: none;
}

.logo.collapsed .logo-short {
  display: inline-block;
  font-size: 18px;
  letter-spacing: 1px;
}

.sidebar-menu {
  border-right: none;
  background-color: #304156;
}

.sidebar-menu :deep(.el-menu-item) {
  color: #bfcbd9;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background-color: #263445;
  color: #fff;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background-color: #409eff;
  color: #fff;
}

.menu-link {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  color: inherit;
  text-decoration: none;
}

.menu-title {
  color: inherit;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
}

.header-right {
  font-size: 16px;
  font-weight: 500;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
