<template>
  <div>
    <van-nav-bar title="处方列表" fixed placeholder />
    <div class="page">
      <van-tabs v-model:active="activeTab" sticky offset-top="46" line-width="24">
        <van-tab title="全部" name="" />
        <van-tab title="待提交" name="DRAFT" />
        <van-tab title="已提交" name="SUBMITTED" />
        <van-tab title="已审批" name="APPROVED" />
        <van-tab title="已驳回" name="REJECTED" />
      </van-tabs>

      <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
        <van-list v-model:loading="loading" :finished="finished" @load="onLoad">
          <div v-for="p in list" :key="p.id" class="card" @click="goDetail(p.id)">
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">{{ p.plotName }}</span>
              <van-tag round :type="statusType(p.status)">{{ statusText(p.status) }}</van-tag>
            </div>
            <div style="margin-top:8px;color:#969799;font-size:12px">
              {{ p.prescriptionNo }} · {{ p.prescriptionDate }}
            </div>
            <div class="info-row" style="margin-top:6px">
              <span class="info-label">病虫害</span>
              <span class="info-value">{{ p.pestType }} / {{ p.pestName }} · {{ p.severity }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">面积</span>
              <span class="info-value">{{ p.occurrenceArea }} 亩</span>
            </div>
            <div class="info-row">
              <span class="info-label">农艺师</span>
              <span class="info-value">{{ p.agronomistName }}</span>
            </div>
          </div>
          <van-empty v-if="!loading && list.length === 0" description="暂无处方" />
        </van-list>
      </van-pull-refresh>
    </div>

    <van-floating-bubble v-if="isAgronomist" axis="xy" :offset="['80vw', '85vh']" icon="edit" @click="goCreate" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('m_user') || '{}')
const isAgronomist = computed(() => ['AGRONOMIST', 'ADMIN'].includes(user.roleCode))

const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const list = ref([])
const activeTab = ref('')
const page = reactive({ num: 1, size: 15 })

watch(activeTab, () => {
  list.value = []; page.num = 1; finished.value = false; onLoad()
})
onMounted(() => {})

async function onLoad() {
  loading.value = true
  try {
    const params = { pageNum: page.num, pageSize: page.size }
    if (activeTab.value) params.status = activeTab.value
    if (user.roleCode === 'AGRONOMIST') params.agronomistId = user.id
    const res = await api.queryPrescriptions(params)
    list.value = list.value.concat(res.records || [])
    if ((res.records || []).length < page.size) finished.value = true
    else page.num++
  } catch (e) {} finally { loading.value = false }
}
function onRefresh() {
  list.value = []; page.num = 1; finished.value = false
  onLoad().finally(() => refreshing.value = false)
}
function statusType(s) {
  return { DRAFT: 'default', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', COMPLETED: 'primary' }[s] || 'default'
}
function statusText(s) {
  return { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审批', REJECTED: '已驳回', COMPLETED: '已完成' }[s] || s
}
function goDetail(id) { router.push('/prescription/' + id) }
function goCreate() { router.push('/prescription-create') }
</script>
