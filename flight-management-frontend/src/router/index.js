import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', component: () => import('@/views/auth/Login.vue'), meta: { guest: true } },
  { path: '/', component: () => import('@/views/Dashboard.vue'), meta: { auth: true } },
  { path: '/flights', component: () => import('@/views/flights/Flights.vue'), meta: { auth: true } },
  { path: '/airlines', component: () => import('@/views/references/Airlines.vue'), meta: { auth: true } },
  { path: '/airports', component: () => import('@/views/references/Airports.vue'), meta: { auth: true } },
  { path: '/aircraft', component: () => import('@/views/references/Aircraft.vue'), meta: { auth: true } },
  { path: '/routes', component: () => import('@/views/references/Routes.vue'), meta: { auth: true } },
  { path: '/reports', component: () => import('@/views/reports/Reports.vue'), meta: { auth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()

  if (to.meta.auth && !auth.isAuthenticated) {
    next('/login')
  } else if (to.meta.guest && auth.isAuthenticated) {
    next('/')
  } else {
    next()
  }
})

export default router
