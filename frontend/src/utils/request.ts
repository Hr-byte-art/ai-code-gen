import axios, { type AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'
import { API_BASE_URL } from '@/utils/apiBase'

type RequestConfig = AxiosRequestConfig & {
  silent?: boolean
  silentAuth?: boolean
}

const AUTH_ERROR_CODES = new Set([40100, 401])
const SILENT_AUTH_URLS = new Set(['/user/get/loginUser', '/user/logout'])

const getResponseMessage = (data: any, fallback: string) => {
  if (!data) return fallback
  if (typeof data === 'string') return data || fallback
  return data.message || data.msg || data.error || data.detail || fallback
}

const shouldSuppressMessage = (config: RequestConfig | undefined, code?: number, url?: string) => {
  if (config?.silent) return true
  if (config?.silentAuth && AUTH_ERROR_CODES.has(Number(code))) return true
  if (url && SILENT_AUTH_URLS.has(url) && AUTH_ERROR_CODES.has(Number(code))) return true
  return false
}

const shouldSkipAuthRedirect = (config: RequestConfig | undefined, url?: string) => {
  if (config?.silentAuth) return true
  if (url && SILENT_AUTH_URLS.has(url)) return true
  return false
}

const notifyError = (content: string, key = 'global-request-error') => {
  message.error({ content, key })
}

const redirectToLogin = () => {
  if (router.currentRoute.value.path !== '/user/login') {
    router.push({ path: '/user/login', query: { redirect: router.currentRoute.value.fullPath } })
  }
}

export const getErrorMessage = (error: any, fallback = '请求失败，请稍后重试') => {
  return error?.message || error?.response?.data?.message || error?.response?.data?.msg || fallback
}

// 处理大数字精度丢失：将超过安全整数范围的数字转为字符串
const jsonBigIntReviver = (_key: string, value: any) => {
  if (typeof value === 'number' && value > Number.MAX_SAFE_INTEGER) {
    return String(value)
  }
  return value
}

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  },
  // 用自定义 transformResponse 在 JSON.parse 时处理大数字
  transformResponse: [(data) => {
    try {
      return JSON.parse(data, jsonBigIntReviver)
    } catch {
      return data
    }
  }]
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data, config } = response
    const requestConfig = config as RequestConfig

    if (data?.code === 0) {
      return data
    }

    const code = Number(data?.code)
    const errorMessage = getResponseMessage(data, '请求失败')
    const error = new Error(errorMessage) as Error & { code?: number; response?: typeof response }
    error.code = code
    error.response = response

    if (AUTH_ERROR_CODES.has(code)) {
      if (!shouldSuppressMessage(requestConfig, code, config.url)) {
        notifyError('登录状态已失效，请重新登录', 'auth-error')
      }
      if (!shouldSkipAuthRedirect(requestConfig, config.url)) {
        redirectToLogin()
      }
      return Promise.reject(error)
    }

    if (!shouldSuppressMessage(requestConfig, code, config.url)) {
      notifyError(errorMessage)
    }
    return Promise.reject(error)
  },
  (error) => {
    const requestConfig = error.config as RequestConfig | undefined
    const status = Number(error.response?.status)
    const errorMessage = error.response
      ? getResponseMessage(error.response.data, status === 500 ? '服务器内部错误，请稍后重试' : `请求失败：${status}`)
      : error.message?.includes('timeout')
        ? '请求超时，请稍后重试'
        : '网络连接失败，请检查网络后重试'

    error.message = errorMessage

    if (status === 401) {
      if (!shouldSuppressMessage(requestConfig, status, error.config?.url)) {
        notifyError('登录状态已失效，请重新登录', 'auth-error')
      }
      if (!shouldSkipAuthRedirect(requestConfig, error.config?.url)) {
        redirectToLogin()
      }
      return Promise.reject(error)
    }

    if (!shouldSuppressMessage(requestConfig, status, error.config?.url)) {
      notifyError(errorMessage)
    }

    return Promise.reject(error)
  }
)

export default request
