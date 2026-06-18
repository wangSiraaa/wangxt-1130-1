<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="28" color="#409EFF"><Promotion /></el-icon>
        <span>植保审批系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#001529"
        text-color="#b0bec5"
        active-text-color="#409EFF"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-menu-item :index="'/' + route.path">
            <el-icon><component :is="route.meta.icon" /></el-icon>
            <span>{{ route.meta.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="breadcrumb">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="mr-3">
            <el-button type="primary" text @click="goReminders">
              <el-icon><Bell /></el-icon>
              <span style="margin-left:4px">提醒</span>
            </el-button>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar size="small" :style="{ backgroundColor: '#409EFF' }">
                {{ user?.realName?.charAt(0) }}
              </el-avatar>
              <span style="margin-left:8px">{{ user?.realName }}</span>
              <el-tag size="small" type="success" style="margin-left:8px">{{ roleText }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

const route = useRoute()
const router = useRouter()

const user = ref(null)
const unreadCount = ref(0)

onMounted(() => {
  const u = localStorage.getItem('user')
  if (u) {
    user.value = JSON.parse(u)
  }
  loadUnread()
})

const menuRoutes = computed(() => {
  const routes = router.options.routes.find(r => r.path === '/').children
  const u = user.value
  return routes.filter(r => {
    if (!r.meta?.roles) return true
    return u && r.meta.roles.includes(u.roleCode)
  })
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '首页')

const roleText = computed(() => {
  const map = { ADMIN: '管理员', AGRONOMIST: '农艺师', WAREHOUSE: '仓库管理员', PILOT: '飞手' }
  return map[user.value?.roleCode] || ''
})

async function loadUnread() {
  try {
    const res = await api.queryReminders({ pageNum: 1, pageSize: 1, unreadOnly: true })
    unreadCount.value = res.total || 0
  } catch (e) {}
}

function goReminders() {
  router.push('/reminders')
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        localStorage.removeItem('user')
        ElMessage.success('已退出登录')
        router.push('/login')
      }).catch(() => {})
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background: #001529;
  overflow-y: auto;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #1f3a5f;
}
:deep(.el-menu) {
  border-right: none;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eef0f4;
  padding: 0 24px;
}
.breadcrumb {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}
.user-info:hover {
  background: #f5f7fa;
}
.main {
  background: #f5f7fa;
  padding: 0;
  overflow-y: auto;
}
</style>
