import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

import App from './App.vue'
import router from './router'
import axios from "axios";

const app = createApp(App)
axios.defaults.baseURL = 'http://localhost:8080'

app.use(createPinia())
app.use(router)

app.use(ElementPlus)
app.mount('#app')



// element-plus
const media = window.matchMedia('(prefers-color-scheme: dark)')

function updateTheme() {
    if (media.matches) {
        document.documentElement.classList.add('dark')
    } else {
        document.documentElement.classList.remove('dark')
    }
}

// 初始化
updateTheme()

// 监听系统变化
media.addEventListener('change', updateTheme)