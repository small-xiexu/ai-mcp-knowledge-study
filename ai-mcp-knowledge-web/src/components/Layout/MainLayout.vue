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
        <!-- 折叠态：只展示扁平菜单（图标 + tooltip） -->
        <template v-if="appStore.sidebarCollapsed">
          <el-tooltip
            v-for="r in flatMenuRoutes"
            :key="r.path"
            :content="r.meta?.title"
            placement="right"
            :disabled="!appStore.sidebarCollapsed"
            effect="dark"
            :offset="12"
          >
            <router-link
              :to="resolveMenuPath(r.path)"
              class="nav-item"
              :class="{ active: activeMenu === resolveMenuPath(r.path) }"
            >
              <div class="nav-icon-container">
                <el-icon><component :is="r.meta?.icon" /></el-icon>
              </div>
              <span v-show="!appStore.sidebarCollapsed" class="nav-label">
                {{ r.meta?.title }}
              </span>
            </router-link>
          </el-tooltip>
        </template>

        <!-- 展开态：按分组展示，默认只展开“常用” -->
        <template v-else>
          <div
            v-for="g in menuGroups"
            :key="g.key"
            class="menu-group"
          >
            <div class="menu-group-header" @click="toggleGroup(g.key)">
              <span class="menu-group-title">{{ g.title }}</span>
              <el-icon class="menu-group-arrow" :class="{ open: isGroupOpen(g.key) }">
                <CaretBottom />
              </el-icon>
            </div>
            <div v-show="isGroupOpen(g.key)" class="menu-group-items">
              <router-link
                v-for="r in g.routes"
                :key="r.path"
                :to="resolveMenuPath(r.path)"
                class="nav-item"
                :class="{ active: activeMenu === resolveMenuPath(r.path) }"
              >
                <div class="nav-icon-container">
                  <el-icon><component :is="r.meta?.icon" /></el-icon>
                </div>
                <span class="nav-label">
                  {{ r.meta?.title }}
                </span>
              </router-link>
            </div>
          </div>
        </template>
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
          <el-dropdown trigger="click" popper-class="gemini-dropdown" @command="handleUserCommand">
            <div class="user-profile">
              <span class="pro-tag">{{ currentUserLabel }}</span>
              <el-avatar :size="32" class="user-avatar">
                {{ currentUserInitial }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useAuthStore } from '@/store/auth'
import { usePermission } from '@/composables/usePermission'
import routes from '@/router/routes'
import { Menu as MenuIcon, Setting, ArrowDown, SwitchButton, CaretBottom } from '@element-plus/icons-vue'

const appStore = useAppStore()
const authStore = useAuthStore()
const { hasPermission } = usePermission()
const route = useRoute()
const router = useRouter()

type MenuGroupKey = 'common' | 'knowledge' | 'integration' | 'org'

const groupDefs: Array<{ key: MenuGroupKey; title: string; defaultOpen: boolean; order: number }> = [
  { key: 'common', title: '常用', defaultOpen: true, order: 1 },
  { key: 'knowledge', title: '知识库', defaultOpen: false, order: 2 },
  { key: 'integration', title: '配置与集成', defaultOpen: false, order: 3 },
  { key: 'org', title: '组织与审计', defaultOpen: false, order: 4 }
]

const buildGroupOpen = (): Record<MenuGroupKey, boolean> => {
  const defaults = groupDefs.reduce((acc, g) => {
    acc[g.key] = g.defaultOpen
    return acc
  }, {} as Record<MenuGroupKey, boolean>)

  const raw = localStorage.getItem('menuGroupOpen')
  if (!raw) {
    return defaults
  }
  try {
    const parsed = JSON.parse(raw) as Partial<Record<MenuGroupKey, boolean>>
    return { ...defaults, ...parsed }
  } catch {
    return defaults
  }
}

const groupOpen = ref<Record<MenuGroupKey, boolean>>(buildGroupOpen())

const toggleGroup = (key: MenuGroupKey) => {
  groupOpen.value[key] = !groupOpen.value[key]
  localStorage.setItem('menuGroupOpen', JSON.stringify(groupOpen.value))
}

const isGroupOpen = (key: MenuGroupKey) => {
  return Boolean(groupOpen.value[key])
}

const normalizeGroupKey = (raw: unknown): MenuGroupKey => {
  if (raw === 'integration' || raw === 'org' || raw === 'knowledge' || raw === 'common') {
    return raw
  }
  return 'common'
}

const allMenuRoutes = computed(() => {
  const layoutRoute = routes.find(r => r.path === '/')
  return (layoutRoute?.children || []).filter(item => {
    if (item.meta?.hidden) {
      return false
    }
    const permission = typeof item.meta?.permission === 'string' ? item.meta.permission : ''
    if (!permission) {
      return true
    }
    return hasPermission(permission)
  })
})

const menuGroups = computed(() => {
  const byKey: Record<MenuGroupKey, any[]> = {
    common: [],
    knowledge: [],
    integration: [],
    org: []
  }

  for (const r of allMenuRoutes.value) {
    const raw = typeof r.meta?.group === 'string' ? r.meta.group : 'common'
    const key = normalizeGroupKey(raw)
    byKey[key].push(r)
  }

  const toOrder = (meta: any) => {
    const v = meta?.order
    return typeof v === 'number' ? v : 999
  }

  return groupDefs
    .slice()
    .sort((a, b) => a.order - b.order)
    .map(def => ({
      key: def.key,
      title: def.title,
      routes: byKey[def.key].slice().sort((a, b) => toOrder(a.meta) - toOrder(b.meta))
    }))
    .filter(g => g.routes.length > 0)
})

const flatMenuRoutes = computed(() => {
  return menuGroups.value.flatMap(g => g.routes)
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/gateway-tools/')) {
    return '/gateway-tools'
  }
  return route.path
})

const ensureActiveGroupOpen = () => {
  const active = activeMenu.value
  const matched = allMenuRoutes.value.find(r => resolveMenuPath(r.path) === active)
  if (!matched) {
    return
  }
  const key = normalizeGroupKey(typeof matched.meta?.group === 'string' ? matched.meta.group : 'common')
  if (!groupOpen.value[key]) {
    groupOpen.value[key] = true
    localStorage.setItem('menuGroupOpen', JSON.stringify(groupOpen.value))
  }
}

watch(
  () => route.path,
  () => {
    if (!appStore.sidebarCollapsed) {
      ensureActiveGroupOpen()
    }
  },
  { immediate: true }
)

const currentPageTitle = computed(() => {
  return route.meta.title || '工作台'
})

function resolveMenuPath(path?: string) {
  if (!path) return '/'
  return path.startsWith('/') ? path : `/${path}`
}

const currentUserLabel = computed(() => {
  if (authStore.profile?.displayName) {
    return authStore.profile.displayName
  }
  if (authStore.profile?.username) {
    return authStore.profile.username
  }
  return '未登录'
})

const currentUserInitial = computed(() => {
  const label = currentUserLabel.value
  if (!label || label === '未登录') {
    return 'U'
  }
  return label.substring(0, 1).toUpperCase()
})

const handleUserCommand = async (command: string | number | object) => {
  if (command !== 'logout') {
    return
  }
  await authStore.logout()
  await router.replace('/login')
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

.menu-group {
  display: flex;
  flex-direction: column;
}

.menu-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin: 6px 6px 2px;
  border-radius: 14px;
  color: var(--gemini-text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.menu-group-header:hover {
  background-color: rgba(255, 255, 255, 0.06);
  color: var(--gemini-text-primary);
}

.menu-group-arrow {
  transition: transform 0.18s ease;
  opacity: 0.9;
}

.menu-group-arrow.open {
  transform: rotate(180deg);
}

.menu-group-items {
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
  cursor: pointer;
}

.pro-tag {
  color: var(--gemini-text-primary);
  font-weight: 600;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
  padding: 4px 10px;
  border-radius: 12px;
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-avatar {
  background: rgba(138, 180, 248, 0.25);
  color: #d3e3fd;
  font-weight: 600;
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
