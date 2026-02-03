import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true',
    theme: 'dark' as 'light' | 'dark',
    loading: false
  }),
  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('sidebarCollapsed', String(this.sidebarCollapsed))
    },
    setTheme(theme: 'light' | 'dark') {
      this.theme = theme
    },
    setLoading(loading: boolean) {
      this.loading = loading
    }
  }
})
