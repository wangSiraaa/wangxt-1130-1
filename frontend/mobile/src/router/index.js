import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页', tabbar: true }
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: () => import('../views/Tasks.vue'),
    meta: { title: '待作业', tabbar: true }
  },
  {
    path: '/warnings',
    name: 'Warnings',
    component: () => import('../views/Warnings.vue'),
    meta: { title: '安全提醒', tabbar: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { title: '我的', tabbar: true }
  },
  {
    path: '/operation/:outboundId',
    name: 'Operation',
    component: () => import('../views/Operation.vue'),
    meta: { title: '飞行作业登记' }
  },
  {
    path: '/prescription/:id',
    name: 'Prescription',
    component: () => import('../views/PrescriptionDetail.vue'),
    meta: { title: '处方详情' }
  },
  {
    path: '/prescription-create',
    name: 'PrescriptionCreate',
    component: () => import('../views/PrescriptionCreate.vue'),
    meta: { title: '开处方' }
  },
  {
    path: '/prescriptions',
    name: 'Prescriptions',
    component: () => import('../views/Prescriptions.vue'),
    meta: { title: '处方列表' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('m_user')
  if (to.path === '/login') {
    next()
  } else if (!user) {
    next('/login')
  } else {
    document.title = to.meta?.title ? `${to.meta.title} - 植保飞手端` : '植保飞手端'
    next()
  }
})

export default router
