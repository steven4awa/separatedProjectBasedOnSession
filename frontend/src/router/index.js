import { createRouter, createWebHashHistory  } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import {userStore} from "@/stores";

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
    },
    {
      path: '/forget-password',
      name: 'forget-password',
      component: () => import('../components/ForgetPage.vue'),
    }
  ],
})

router.beforeEach(async (to) => {
  const store = userStore()

  // 已登录，禁止访问登录页
  if (store.auth.user && to.path === "/") {
    return "/index"
  }
  // 未登录，禁止访问后台
  if (!store.auth.user && to.path.startsWith("/index")) {
    return "/"
  }
})

export default router
