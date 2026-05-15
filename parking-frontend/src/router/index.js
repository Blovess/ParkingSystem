import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/Layout.vue'),
    redirect: '/entry-exit',
    children: [
      { path: 'entry-exit', name: 'EntryExit', component: () => import('../views/EntryExit.vue') },
      { path: 'parking-monitor', name: 'ParkingMonitor', component: () => import('../views/ParkingMonitor.vue') },
      { path: 'records', name: 'Records', component: () => import('../views/Records.vue') },
      { path: 'users', name: 'Users', component: () => import('../views/Users.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
