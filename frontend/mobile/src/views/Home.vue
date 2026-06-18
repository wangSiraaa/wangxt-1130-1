<template>
  <div>
    <van-nav-bar title="首页" fixed placeholder />
    <div class="page">
      <div class="user-header card">
        <div style="display:flex;align-items:center;gap:12px">
          <van-avatar size="56" :style="{ background: 'linear-gradient(135deg,#1989fa,#4facfe)' }">
            {{ user?.realName?.charAt(0) }}
          </van-avatar>
          <div style="flex:1">
            <div style="font-size:18px;font-weight:600">{{ user?.realName }}</div>
            <div style="color:#969799;font-size:12px;margin-top:2px">{{ roleText }}</div>
          </div>
          <van-tag round type="primary">{{ today }}</van-tag>
        </div>
      </div>

      <div class="stat-grid">
        <div class="stat-item" @click="$router.push('/tasks')">
          <div class="stat-num warn">{{ pendingTasks }}</div>
          <div class="stat-name">待作业任务</div>
        </div>
        <div class="stat-item" @click="$router.push('/warnings')">
          <div class="stat-num danger">{{ warnings }}</div>
          <div class="stat-name">安全提醒</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">{{ todayOps }}</div>
          <div class="stat-name">今日作业</div>
        </div>
        <div class="stat-item">
          <div class="stat-num success">{{ totalArea }}亩</div>
          <div class="stat-name">本月作业</div>
        </div>
      </div>

      <div class="section" v-if="isPilot">
        <van-grid column-num="4" :border="false">
          <van-grid-item icon="edit" text="作业登记" @click="$router.push('/tasks')" />
          <van-grid-item icon="warning-o" text="安全提醒" @click="$router.push('/warnings')" />
          <van-grid-item icon="friends-o" text="处方查询" @click="$router.push('/prescriptions')" />
          <van-grid-item icon="location-o" text="地块档案" @click="showPlots = true" />
        </van-grid>
      </div>

      <div class="section" v-if="isAgronomist">
        <van-grid column-num="4" :border="false">
          <van-grid-item icon="edit" text="开处方" @click="$router.push('/prescription-create')" />
          <van-grid-item icon="todo-list-o" text="处方列表" @click="$router.push('/prescriptions')" />
          <van-grid-item icon="warning-o" text="间隔期提醒" @click="$router.push('/warnings')" />
          <van-grid-item icon="location-o" text="地块档案" @click="showPlots = true" />
        </van-grid>
      </div>

      <div class="card" v-if="latestWarns.length > 0">
        <div class="card-title">
          <span><van-icon name="warning-o" color="#ee0a24" /> 最新提醒</span>
          <span style="color:#1989fa;font-size:12px;font-weight:normal" @click="$router.push('/warnings')">查看全部</span>
        </div>
        <div v-for="w in latestWarns" :key="w.id" class="warn-item" @click="openWarn(w)">
          <div style="display:flex;align-items:center;gap:8px">
            <van-tag round :type="w.reminderLevel === 'DANGER' ? 'danger' : w.reminderLevel === 'WARNING' ? 'warning' : 'primary'" size="medium">
              {{ w.remainingDays }}天
            </van-tag>
            <span style="font-weight:500">{{ w.plotName }} · {{ w.pesticideName }}</span>
          </div>
          <div style="font-size:12px;color:#969799;margin-top:4px">{{ w.remindContent }}</div>
        </div>
      </div>

      <div class="card" v-if="latestTasks.length > 0">
        <div class="card-title">
          <span><van-icon name="todo-list-o" color="#1989fa" /> 待作业</span>
          <span style="color:#1989fa;font-size:12px;font-weight:normal" @click="$router.push('/tasks')">查看全部</span>
        </div>
        <div v-for="t in latestTasks" :key="t.outboundId" class="task-item" @click="goTask(t)">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-weight:600">{{ t.plotName }}</span>
            <van-tag round type="warning">待飞行</van-tag>
          </div>
          <div style="font-size:12px;color:#969799;margin-top:6px">
            {{ t.outboundNo }} · 预计{{ t.expectedDate }}
          </div>
        </div>
      </div>
    </div>

    <van-popup v-model:show="showPlots" round position="bottom" :style="{ height: '70%' }">
      <div style="padding:16px;font-weight:600;font-size:16px;border-bottom:1px solid #f2f3f5">地块档案</div>
      <van-list v-model:loading="plotLoading" :finished="true" immediate-check>
        <div v-for="p in plots" :key="p.id" class="plot-item card" style="margin:12px 16px">
          <div class="info-row"><span class="info-label">地块</span><span class="info-value">{{ p.plotName }}</span></div>
          <div class="info-row"><span class="info-label">面积</span><span class="info-value">{{ p.area }}亩 · {{ p.cropName }}</span></div>
          <div class="info-row"><span class="info-label">位置</span><span class="info-value">{{ p.location }}</span></div>
          <div class="info-row"><span class="info-label">土壤/灌溉</span><span class="info-value">{{ p.soilType }} / {{ p.irrigationMethod }}</span></div>
        </div>
      </van-list>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'

const user = ref(null)
const pendingTasks = ref(0)
const warnings = ref(0)
const todayOps = ref(0)
const totalArea = ref(0)
const latestWarns = ref([])
const latestTasks = ref([])
const plots = ref([])
const plotLoading = ref(false)
const showPlots = ref(false)

onMounted(async () => {
  user.value = JSON.parse(localStorage.getItem('m_user') || '{}')
  await loadData()
  try { plots.value = await api.listPlots() } catch (e) {}
})

const isPilot = computed(() => user.value?.roleCode === 'PILOT')
const isAgronomist = computed(() => user.value?.roleCode === 'AGRONOMIST')
const roleText = computed(() => ({
  ADMIN: '管理员', AGRONOMIST: '农艺师', WAREHOUSE: '仓库管理员', PILOT: '飞手'
}[user.value?.roleCode] || ''))
const today = computed(() => {
  const d = new Date()
  return `${d.getMonth()+1}月${d.getDate()}日`
})

async function loadData() {
  const u = user.value
  if (!u) return
  try {
    const list = await api.getPendingOperations(u.id)
    pendingTasks.value = list.filter(x => !x.flightStatus || x.flightStatus === 'PENDING').length
    latestTasks.value = list.slice(0, 3)
  } catch (e) {}
  try {
    const res = await api.queryReminders({ pageNum: 1, pageSize: 3, unreadOnly: false })
    warnings.value = res.total || 0
    latestWarns.value = res.records || []
  } catch (e) {}
  try {
    const today = new Date().toISOString().slice(0, 10)
    const [r1, r2] = await Promise.all([
      api.queryFlights({ pageNum: 1, pageSize: 1, startDate: today, endDate: today, pilotId: u.roleCode === 'PILOT' ? u.id : null }).catch(() => ({ total: 0 })),
      api.queryFlights({ pageNum: 1, pageSize: 999, pilotId: u.roleCode === 'PILOT' ? u.id : null }).catch(() => ({ records: [] }))
    ])
    todayOps.value = r1.total || 0
    totalArea.value = (r2.records || []).reduce((a, b) => a + (b.actualArea || 0), 0).toFixed(1)
  } catch (e) {}
}
function goTask(t) { location.hash = `#/operation/${t.outboundId}` }
function openWarn(w) {
  api.markReminderRead(w.id).catch(() => {})
  location.hash = '#/warnings'
}
</script>

<style scoped>
.user-header {
  background: linear-gradient(135deg, #1989fa, #4facfe);
  color: #fff;
}
.user-header .subtitle { color: rgba(255,255,255,0.8); }
.user-header :deep(.van-tag) { background: rgba(255,255,255,0.2); border: 1px solid rgba(255,255,255,0.4); color: #fff; }
:deep(.van-grid-item__text) { font-size: 13px; }
.warn-item, .task-item, .plot-item {
  padding: 12px;
  background: #f7f8fa;
  border-radius: 10px;
  margin-bottom: 8px;
}
.plot-item { background: #fff; margin: 0; }
.plot-item:not(:last-child) { margin-bottom: 12px; }
</style>
