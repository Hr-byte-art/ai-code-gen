const normalizeApiBaseUrl = (value?: string) => {
  const baseUrl = value?.trim()
  if (!baseUrl) return '/api'
  return baseUrl.replace(/\/$/, '')
}

export const API_BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL)

export const buildApiUrl = (path: string) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}