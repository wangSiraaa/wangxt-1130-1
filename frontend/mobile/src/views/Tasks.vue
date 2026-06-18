<template>
  <div>
    <van-nav-bar title="待作业任务" fixed placeholder />
    <div class="page">
      <van-dropdown-menu>
        <van-dropdown-item v-model="filter.status" :options="statusOptions" />
      </van-dropdown-menu>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多了" @load="onLoad">
          <div v-for="t in list" :key="t.outboundId" class="card task-card">
            <div class="card-title">
              <div>
                <span style="font-weight:600">{{ t.plotName }}</span>
                <van-tag round style="margin-left:8px" :type="t.flightStatus === 'COMPLETED' ? 'success' : t.flightStatus === 'IN_PROGRESS' ? 'primary' : 'warning'">
                  {{ t.flightStatus === 'COMPLETED' ? '已完成' : t.flightStatus === 'IN_PROGRESS' ? '飞行中' : '待飞行' }}
                </van-tag>
              </div>
              <span style="color:#969799;font-size:12px">{{ t.outboundNo }}</span>
            </div>
            <div class="info-row"><span class="info-label">处方号</span><span class="info-value">{{ t.prescriptionNo }}</span></div>
            <div class="info-row"><span class="info-label">作物</span><span class="info-value">{{ t.cropName }}</span></div>
            <div class="info-row"><span class="info-label">病虫害</span><span class="info-value">{{ t.pestName }}（{{ t.severity }}）</span></div>
            <div class="info-row"><span class="info-label">仓管员</span><span class="info-value">{{ t.warehouseKeeperName }}</span></div>
            <div class="info-row"><span class="info-label">领用时间</span><span class="info-value">{{ t.outboundTime }}</span></div>

            <div class="pesticides" style="margin-top:10px">
              <div style="color:#969799;font-size:12px;margin-bottom:6px">农药清单</div>
              <div v-for="p in t.pesticides" :key="p.id" class="pest-item">
                <span>{{ p.pesticideName }}</span>
                <span style="color:#969799">{{ p.actualQuantity }}{{ p.unit }}</span>
              </div>
            </div>

            <div style="display:flex;gap:10px;margin-top:14px">
              <van-button plain block type="primary" @click="goDetail(t.prescriptionId)">查看处方</van-button>
              <van-button block type="primary" :disabled="t.flightStatus === 'COMPLETED'" @click="goOperation(t)">
                {{ t.flightStatus === 'IN_PROGRESS' ? '完成作业' : t.flightStatus === 'COMPLETED' ? '已完成' : '开始作业' }}
              </van-button>
            </div>
          </div>
        </van-list>
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('m_user') || '{}')
const list = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const filter = ref({ status: '' })
const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '待飞行', value: 'PENDING' },
  { text: '飞行中', value: 'IN_PROGRESS' },
  { text: '已完成', value: 'COMPLETED' }
]

onMounted(loadAll)

async function loadAll() {
  try {
    const data = await api.getPendingOperations(user.id)
    let filtered = data
    if (filter.value.status) {
      filtered = data.filter(x => (x.flightStatus || 'PENDING') === filter.value.status)
    }
    list.value = filtered
  } catch (e) {}
}
function onLoad() {
  loading.value = true
  setTimeout(() => {
    finished.value = true
    loading.value = false
  }, 300)
}
function onRefresh() {
  loadAll().finally(() => refreshing.value = false)
}
function goDetail(id) { router.push('/prescription/' + id) }
function goOperation(t) {
  router.push('/operation/' + t.outboundId)
}
</script>

<style scoped>
.task-card { position: relative; overflow: hidden; }
.pesticides {
  background: #f7f8fa;
  border-radius: 8px;
  padding: 10px 12px;
}
.pest-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}
</style>
