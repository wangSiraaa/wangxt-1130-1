<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">安全间隔期提醒</div>
      <div style="display:flex;gap:8px;align-items:center">
        <el-select v-model="query.plotId" placeholder="选择地块" clearable filterable style="width:200px">
          <el-option v-for="p in plots" :key="p.id" :label="p.plotName" :value="p.id" />
        </el-select>
        <el-switch v-model="query.unreadOnly" active-text="仅未读" />
        <el-button type="primary" icon="Search" @click="load">查询</el-button>
      </div>
    </div>

    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="6" v-for="(c, k) in counters" :key="k">
        <div class="stat-card" :style="{background: c.bg}">
          <div class="stat-value" :style="{color: c.color}">{{ c.value }}</div>
          <div class="stat-label" :style="{color: c.color}">{{ c.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table :data="list" v-loading="loading" @row-click="markRead">
      <el-table-column prop="plotName" label="地块" width="140" />
      <el-table-column prop="cropName" label="作物" width="100" />
      <el-table-column prop="pesticideName" label="农药" width="140" />
      <el-table-column prop="lastOperationDate" label="上次施药" width="120" />
      <el-table-column prop="safeEndDate" label="解禁日期" width="120" />
      <el-table-column label="剩余间隔期" width="130">
        <template #default="{ row }">
          <el-tag :type="row.remainingDays > 10 ? 'success' : row.remainingDays > 3 ? 'warning' : 'danger'">
            {{ row.remainingDays }} 天
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reminderLevel" label="级别" width="90">
        <template #default="{ row }">
          <el-tag :type="levelType(row.reminderLevel)" effect="dark">{{ levelText(row.reminderLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reminderType" label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ typeText(row.reminderType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.readFlag === 1" type="info" plain>已读</el-tag>
          <el-tag v-else type="danger">未读</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remindContent" label="提醒内容" show-overflow-tooltip />
      <el-table-column prop="createTime" label="生成时间" width="160" />
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end;display:flex"
      v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
      :total="total" @size-change="load" @current-change="load" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { api } from '../api'

const loading = ref(false)
const list = ref([])
const plots = ref([])
const total = ref(0)
const query = reactive({ plotId: null, unreadOnly: false, pageNum: 1, pageSize: 10 })
const counters = reactive({
  danger: { label: '危险-禁采收', value: 0, color: '#fff', bg: 'linear-gradient(135deg, #F56C6C, #f78989)' },
  warning: { label: '警告-即将解禁', value: 0, color: '#fff', bg: 'linear-gradient(135deg, #E6A23C, #ebb563)' },
  info: { label: '提示-可采收', value: 0, color: '#fff', bg: 'linear-gradient(135deg, #67C23A, #85ce61)' },
  unread: { label: '未读提醒', value: 0, color: '#fff', bg: 'linear-gradient(135deg, #909399, #a6a9ad)' }
})

onMounted(async () => {
  plots.value = await api.listPlots()
  await load()
})
async function load() {
  loading.value = true
  try {
    const res = await api.queryReminders(query)
    list.value = res.records || []
    total.value = res.total || 0
    counters.danger.value = (res.records || []).filter(r => r.reminderLevel === 'DANGER').length
    counters.warning.value = (res.records || []).filter(r => r.reminderLevel === 'WARNING').length
    counters.info.value = (res.records || []).filter(r => r.reminderLevel === 'INFO').length
    counters.unread.value = (res.records || []).filter(r => r.readFlag !== 1).length
  } catch (e) {} finally { loading.value = false }
}
function levelType(l) { return l === 'DANGER' ? 'danger' : l === 'WARNING' ? 'warning' : 'success' }
function levelText(l) { return l === 'DANGER' ? '危险' : l === 'WARNING' ? '警告' : '提示' }
function typeText(t) {
  const m = { BEFORE_INTERVAL: '间隔期内', APPROACHING: '即将到期', EXPIRED: '已到期' }
  return m[t] || t
}
async function markRead(row) {
  if (row.readFlag === 1) return
  try {
    await api.markReminderRead(row.id, {})
    const r = list.value.find(x => x.id === row.id)
    if (r) r.readFlag = 1
    counters.unread.value--
  } catch (e) {}
}
</script>

<style scoped>
.stat-card {
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.stat-value { font-size: 32px; font-weight: 700; margin-bottom: 4px; }
.stat-label { font-size: 13px; opacity: .95; }
</style>
