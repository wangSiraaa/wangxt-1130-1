<template>
  <div>
    <van-nav-bar title="安全间隔期提醒" fixed placeholder />
    <div class="page">
      <div class="stat-grid">
        <div class="stat-item">
          <div class="stat-num danger">{{ counters.danger }}</div>
          <div class="stat-name">危险-禁采收</div>
        </div>
        <div class="stat-item">
          <div class="stat-num warn">{{ counters.warning }}</div>
          <div class="stat-name">警告-将到期</div>
        </div>
        <div class="stat-item">
          <div class="stat-num success">{{ counters.info }}</div>
          <div class="stat-name">已到解禁期</div>
        </div>
        <div class="stat-item">
          <div class="stat-num">{{ counters.unread }}</div>
          <div class="stat-name">未读</div>
        </div>
      </div>

      <van-dropdown-menu>
        <van-dropdown-item v-model="filter.level" :options="levelOptions" />
      </van-dropdown-menu>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
          <div v-for="w in list" :key="w.id" class="card warn-card"
            :class="{'read': w.readFlag === 1}" @click="handleClick(w)">
            <div style="display:flex;justify-content:space-between;align-items:flex-start">
              <div style="display:flex;align-items:center;gap:8px">
                <van-tag round size="large"
                  :type="w.reminderLevel === 'DANGER' ? 'danger' : w.reminderLevel === 'WARNING' ? 'warning' : 'primary'">
                  {{ w.remainingDays }}天
                </van-tag>
                <span style="font-weight:600">{{ w.plotName }}</span>
                <van-tag v-if="w.readFlag !== 1" round type="danger" size="mini">新</van-tag>
              </div>
              <span style="color:#969799;font-size:12px">{{ w.reminderType === 'BEFORE_INTERVAL' ? '间隔期内' : w.reminderType === 'APPROACHING' ? '即将到期' : '已到期' }}</span>
            </div>
            <div style="margin-top:8px;color:#646566;font-size:13px">{{ w.remindContent }}</div>
            <div style="margin-top:10px;display:flex;justify-content:space-between;font-size:12px;color:#969799">
              <span>{{ w.pesticideName }} · 上次施药:{{ w.lastOperationDate }}</span>
              <span>解禁:{{ w.safeEndDate }}</span>
            </div>
          </div>
          <van-empty v-if="!loading && list.length === 0" description="暂无提醒" />
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { api } from '../api'

const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const list = ref([])
const page = reactive({ num: 1, size: 15 })
const filter = ref({ level: '' })
const counters = reactive({ danger: 0, warning: 0, info: 0, unread: 0 })

const levelOptions = [
  { text: '全部级别', value: '' },
  { text: '危险', value: 'DANGER' },
  { text: '警告', value: 'WARNING' },
  { text: '提示', value: 'INFO' }
]

onMounted(loadAll)

async function loadAll() {
  try {
    page.num = 1; list.value = []; finished.value = false
    const res = await api.queryReminders({ pageNum: 1, pageSize: 999, unreadOnly: false })
    const all = res.records || []
    counters.danger = all.filter(r => r.reminderLevel === 'DANGER').length
    counters.warning = all.filter(r => r.reminderLevel === 'WARNING').length
    counters.info = all.filter(r => r.reminderLevel === 'INFO').length
    counters.unread = all.filter(r => r.readFlag !== 1).length
    await onLoad()
  } catch (e) {} finally {}
}

async function onLoad() {
  loading.value = true
  try {
    const params = { pageNum: page.num, pageSize: page.size, unreadOnly: false }
    if (filter.value.level) params.reminderLevel = filter.value.level
    const res = await api.queryReminders(params)
    list.value = list.value.concat(res.records || [])
    if ((res.records || []).length < page.size) finished.value = true
    else page.num++
  } catch (e) {} finally { loading.value = false }
}

function onRefresh() {
  list.value = []; page.num = 1; finished.value = false
  loadAll().finally(() => refreshing.value = false)
}

async function handleClick(w) {
  if (w.readFlag !== 1) {
    try {
      await api.markReminderRead(w.id)
      const i = list.value.findIndex(x => x.id === w.id)
      if (i > -1) list.value[i].readFlag = 1
      counters.unread = Math.max(0, counters.unread - 1)
    } catch (e) {}
  }
}
</script>

<style scoped>
.warn-card { transition: opacity .2s; }
.warn-card.read { opacity: .6; }
</style>
