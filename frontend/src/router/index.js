import { createRouter, createWebHistory,createWebHashHistory  } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import axios from "axios";

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/index',
      name: 'index',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../components/RegisterPage.vue'),
    }
  ],
})

// router.beforeEach(async (to, from, next) => {
//     // 不需要登录就能访问的页面白名单
//     const publicPages = ['/']
//     const authRequired = !publicPages.includes(to.path)
//
//     if (authRequired) {
//         // 调用后端接口检查登录状态
//         try {
//             await axios.get('/api/auth/status', { withCredentials: true })
//             next()  // 已登录，放行
//         } catch {
//             next('/')  // 未登录，跳转登录页
//         }
//     } else {
//         next()
//     }
// })

export default router
