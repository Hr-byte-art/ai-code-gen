import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { lightTokens, darkTokens } from '@/theme/tokens'

export type ThemeMode = 'system' | 'light' | 'dark'

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>((localStorage.getItem('theme') as ThemeMode) || 'system')
  const resolved = ref<'light' | 'dark'>('light')

  function apply(theme: 'light' | 'dark') {
    const tokens = theme === 'dark' ? darkTokens : lightTokens
    const root = document.documentElement
    root.setAttribute('data-theme', theme)
    Object.entries(tokens).forEach(([key, value]) => {
      root.style.setProperty(key, value)
    })
    resolved.value = theme
  }

  function getSystemTheme(): 'light' | 'dark' {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }

  function setMode(newMode: ThemeMode) {
    mode.value = newMode
    localStorage.setItem('theme', newMode)
    apply(newMode === 'system' ? getSystemTheme() : newMode)
  }

  function init() {
    const systemTheme = getSystemTheme()
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (mode.value === 'system') {
        apply(e.matches ? 'dark' : 'light')
      }
    })
    apply(mode.value === 'system' ? systemTheme : mode.value)
  }

  return { mode, resolved, setMode, init }
})
