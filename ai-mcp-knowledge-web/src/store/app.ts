import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    theme: 'light' as 'light' | 'dark',
    loading: false
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    setTheme(theme: 'light' | 'dark') {
      this.theme = theme
    },
    setLoading(loading: boolean) {
      this.loading = loading
    }
  }
})
