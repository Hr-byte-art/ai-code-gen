import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import router from './router'
import App from './App.vue'

// Ant Design Vue 样式
import 'ant-design-vue/dist/reset.css'

const app = createApp(App)

// 注册插件
app.use(createPinia())
app.use(router)
app.use(Antd)

// 挂载应用
app.mount('#app')
