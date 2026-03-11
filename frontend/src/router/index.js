import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login',    name: 'login',     component: () => import('@/views/LoginView.vue') },
  { path: '/register', name: 'register',  component: () => import('@/views/RegisterView.vue') },
  { path: '/',         name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { auth: true } },
  { path: '/board/:id',name: 'board',     component: () => import('@/views/BoardView.vue'),     meta: { auth: true } },
  { path: '/admin',    name: 'admin',     component: () => import('@/views/AdminView.vue'),     meta: { auth: true, admin: true } },
  { path: '/:p(.*)*',  redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth  && !auth.isAuthenticated)              return '/login'
  if (to.meta.admin && !auth.isAdmin)                       return '/'
  if ((to.path === '/login' || to.path === '/register') && auth.isAuthenticated) return '/'
})

export default router
