<template>
  <div>
    <van-nav-bar title="我的" fixed placeholder />
    <div class="page">
      <div class="user-card">
        <van-avatar size="64" :style="{ background: 'linear-gradient(135deg,#1989fa,#4facfe)' }">
          {{ user?.realName?.charAt(0) }}
        </van-avatar>
        <div style="flex:1;margin-left:16px">
          <div style="font-size:18px;font-weight:600;color:#fff">{{ user?.realName }}</div>
          <div style="color:rgba(255,255,255,0.85);font-size:13px;margin-top:4px">{{ roleText }}</div>
          <div style="color:rgba(255,255,255,0.7);font-size:12px;margin-top:2px">用户名: {{ user?.username }}</div>
        </div>
      </div>

      <div class="card">
        <van-cell title="账号ID" :value="user?.id" is-link />
        <van-cell title="手机号" :value="user?.phone || '-' " />
        <van-cell title="邮箱" :value="user?.email || '-'" />
        <van-cell title="所属组织" :value="user?.orgName || '-'" />
      </div>

      <van-cell-group inset style="margin-bottom:12px;border-radius:12px;overflow:hidden">
        <van-cell title="待作业任务" :label="pendingTasks + ' 个待处理'" is-link @click="$router.push('/tasks')">
          <template #right-icon><van-badge :content="pendingTasks" /></template>
        </van-cell>
        <van-cell title="安全提醒" :label="warnCount + ' 条未读'" is-link @click="$router.push('/warnings')">
          <template #right-icon><van-badge :content="warnCount" /></template>
        </van-cell>
        <van-cell v-if="isAgronomist" title="我开的处方" is-link @click="$router.push('/prescriptions')" />
      </van-cell-group>

      <van-cell-group inset style="margin-bottom:12px;border-radius:12px;overflow:hidden">
        <van-cell title="设置" icon="setting-o" is-link />
        <van-cell title="关于" icon="info-o" is-link @click="showAbout = true" />
        <van-cell title="意见反馈" icon="comment-o" is-link />
      </van-cell-group>

      <div style="padding:0 16px;margin-top:24px">
        <van-button block type="danger" plain @click="logout">退出登录</van-button>
      </div>

      <div style="text-align:center;margin-top:40px;color:#c8c9cc;font-size:12px">
        <p>植保施药审批系统 v1.0.0</p>
        <p>© 2024 Farm Plant Protection</p>
      </div>
    </div>

    <van-dialog v-model:show="showAbout" title="关于系统" show-cancel-button>
      <div style="padding:16px 8px;line-height:1.8;color:#646566;font-size:14px">
        <p style="font-weight:600;color:#323233;margin-bottom:8px">大型农场植保施药审批系统</p>
        <p>面向农艺师、仓管员、飞手的全流程植保作业管理系统。</p>
        <p>核心功能：</p>
        <p>· 病虫害处方开具与审批</p>
        <p>· 禁用农药出库拦截</p>
        <p>· 飞行风速安全校验</p>
        <p>· 安全间隔期自动提醒</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showToast } from 'vant'
import { api } from '../api'

const router = useRouter()
const user = ref(null)
const pendingTasks = ref(0)
const warnCount = ref(0)
const showAbout = ref(false)

onMounted(async () => {
  user.value = JSON.parse(localStorage.getItem('m_user') || '{}')
  try {
    const list = await api.getPendingOperations(user.value.id)
    pendingTasks.value = list.filter(x => !x.flightStatus || x.flightStatus === 'PENDING').length
  } catch (e) {}
  try {
    const r = await api.queryReminders({ pageNum: 1, pageSize: 1, unreadOnly: true })
    warnCount.value = r.total || 0
  } catch (e) {}
})

const isAgronomist = computed(() => user.value?.roleCode === 'AGRONOMIST')
const roleText = computed(() => ({
  ADMIN: '系统管理员', AGRONOMIST: '农艺师', WAREHOUSE: '仓库管理员', PILOT: '无人机飞手'
}[user.value?.roleCode] || ''))

function logout() {
  showDialog({
    title: '提示',
    message: '确定要退出登录吗？',
    showCancelButton: true,
    confirmButtonText: '退出'
  }).then(() => {
    localStorage.removeItem('m_user')
    showToast('已退出')
    setTimeout(() => router.replace('/login'), 500)
  }).catch(() => {})
}
</script>

<style scoped>
.user-card {
  background: linear-gradient(135deg, #1989fa, #4facfe);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  color: #fff;
  box-shadow: 0 4px 16px rgba(25, 137, 250, 0.25);
}
</style>
