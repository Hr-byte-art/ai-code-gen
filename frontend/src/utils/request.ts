import axios from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'

// 处理大数字精度丢失：将超过安全整数范围的数字转为字符串
const jsonBigIntReviver = (_key: string, value: any) => {
  if (typeof value === 'number' && value > Number.MAX_SAFE_INTEGER) {
    return String(value)
  }
  return value
}

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
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
    const { data } = response

    // 如果响应成功（code为0表示成功）
    if (data.code === 0) {
      return data
    }

    // 显示错误消息
    message.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    // 处理HTTP错误状态码
    if (error.response) {
      const { status } = error.response

      switch (status) {
        case 401:
          message.error('请先登录')
          router.push('/user/login')
          break
        case 403:
          message.error('没有权限访问')
          break
        case 404:
          message.error('请求的资源不存在')
          break
        case 500:
          message.error('服务器内部错误')
          break
        default:
          message.error(`请求失败: ${status}`)
      }
    } else if (error.message?.includes('timeout')) {
      message.error('请求超时，请稍后重试')
    } else {
      message.error('网络连接失败')
    }

    return Promise.reject(error)
  }
)

export default request
