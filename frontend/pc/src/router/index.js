import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../layout/Layout.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '首页', icon: 'House' }
      },
      {
        path: 'prescription',
        name: 'Prescription',
        component: () => import('../views/prescription/List.vue'),
        meta: { title: '病虫害处方', icon: 'Document', roles: ['AGRONOMIST', 'ADMIN', 'WAREHOUSE'] }
      },
      {
        path: 'prescription/create',
        name: 'PrescriptionCreate',
        component: () => import('../views/prescription/Create.vue'),
        meta: { title: '新增处方', roles: ['AGRONOMIST', 'ADMIN'] }
      },
      {
        path: 'prescription/:id',
        name: 'PrescriptionDetail',
        component: () => import('../views/prescription/Detail.vue'),
        meta: { title: '处方详情' }
      },
      {
        path: 'outbound',
        name: 'Outbound',
        component: () => import('../views/outbound/List.vue'),
        meta: { title: '农药出库', icon: 'Box', roles: ['WAREHOUSE', 'ADMIN'] }
      },
      {
        path: 'outbound/:id',
        name: 'OutboundDetail',
        component: () => import('../views/outbound/Detail.vue'),
        meta: { title: '出库单详情' }
      },
      {
        path: 'flight',
        name: 'Flight',
        component: () => import('../views/flight/List.vue'),
        meta: { title: '飞行作业', icon: 'Promotion', roles: ['PILOT', 'ADMIN', 'AGRONOMIST', 'WAREHOUSE'] }
      },
      {
        path: 'reminders',
        name: 'Reminders',
        component: () => import('../views/Reminders.vue'),
        meta: { title: '间隔期提醒', icon: 'Bell' }
      },
      {
        path: 'plots',
        name: 'Plots',
        component: () => import('../views/basic/Plots.vue'),
        meta: { title: '地块档案', icon: 'Location' }
      },
      {
        path: 'pesticides',
        name: 'Pesticides',
        component: () => import('../views/basic/Pesticides.vue'),
        meta: { title: '农药档案', icon: 'Grid' }
      },
      {
        path: 'stocks',
        name: 'Stocks',
        component: () => import('../views/basic/Stocks.vue'),
        meta: { title: '库存管理', icon: 'Goods' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.path === '/login') {
    next()
  } else if (!user) {
    next('/login')
  } else {
    const u = JSON.parse(user)
    if (to.meta.roles && !to.meta.roles.includes(u.roleCode)) {
      next('/dashboard')
    } else {
      document.title = to.meta.title ? `${to.meta.title} - 植保施药审批系统` : '植保施药审批系统'
      next()
    }
  }
})

export default router
