<template>
  <div>
    <van-nav-bar title="处方详情" left-arrow @click-left="$router.back()" fixed placeholder />
    <div class="page" v-loading="loading">
      <div class="card" v-if="data">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
          <span style="font-size:16px;font-weight:600">{{ data.prescriptionNo }}</span>
          <van-tag round size="large" :type="statusType(data.status)">{{ statusText(data.status) }}</van-tag>
        </div>
        <div class="info-row"><span class="info-label">处方日期</span><span class="info-value">{{ data.prescriptionDate }}</span></div>
        <div class="info-row"><span class="info-label">地块/作物</span><span class="info-value">{{ data.plotName }}（{{ data.cropName }}）</span></div>
        <div class="info-row"><span class="info-label">病虫害类别</span><span class="info-value">{{ data.pestType }}</span></div>
        <div class="info-row"><span class="info-label">病虫害名称</span><span class="info-value">{{ data.pestName }}</span></div>
        <div class="info-row"><span class="info-label">严重度</span><span class="info-value"><van-tag :type="data.severity === '重' ? 'danger' : data.severity === '中' ? 'warning' : 'success'">{{ data.severity }}</van-tag></span></div>
        <div class="info-row"><span class="info-label">发生面积</span><span class="info-value">{{ data.occurrenceArea }} 亩</span></div>
        <div class="info-row"><span class="info-label">农艺师</span><span class="info-value">{{ data.agronomistName }}</span></div>
      </div>

      <div class="card" v-if="data?.symptomDescription">
        <div class="card-title"><span>症状描述</span></div>
        <div style="color:#646566;font-size:13px;line-height:1.6">{{ data.symptomDescription }}</div>
      </div>

      <div class="card" v-if="data?.diagnosisNote">
        <div class="card-title"><span>诊断意见</span></div>
        <div style="color:#646566;font-size:13px;line-height:1.6">{{ data.diagnosisNote }}</div>
      </div>

      <div class="card" v-if="data?.details?.length">
        <div class="card-title"><span>农药明细（{{ data.details.length }}种）</span></div>
        <div v-for="(d, i) in data.details" :key="d.id" style="padding:10px;border-bottom:1px dashed #ebedf0" :class="i === data.details.length - 1 ? 'no-border' : ''">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-weight:600">{{ d.pesticideName }}</span>
            <van-tag v-if="d.isForbidden === 1" type="danger" size="mini">禁用</van-tag>
          </div>
          <div style="color:#969799;font-size:12px;margin-top:4px">
            {{ d.formulation }} · {{ d.concentration }} · 风速限制 {{ d.maxWindSpeed || '5.0' }}m/s
          </div>
          <div class="info-row" style="margin-top:6px">
            <span class="info-label">每亩用量</span>
            <span class="info-value">{{ d.dosagePerMu }} {{ d.dosageUnit }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">方式 / 次数</span>
            <span class="info-value">{{ d.usageMethod }} · {{ d.times }}次，间隔{{ d.intervalDays }}天</span>
          </div>
        </div>
      </div>

      <div class="card" v-if="data?.status === 'SUBMITTED' && data.approveRemark">
        <div class="card-title"><span>审批信息</span></div>
        <div class="info-row"><span class="info-label">审批人</span><span class="info-value">{{ data.approveBy || '-' }}</span></div>
        <div class="info-row"><span class="info-label">备注</span><span class="info-value">{{ data.approveRemark || '-' }}</span></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

const route = useRoute()
const loading = ref(false)
const data = ref(null)

onMounted(async () => {
  loading.value = true
  try {
    data.value = await api.getPrescription(route.params.id)
  } catch (e) {} finally { loading.value = false }
})

function statusType(s) {
  return { DRAFT: 'default', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', COMPLETED: 'primary' }[s] || ''
}
function statusText(s) {
  return { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审批', REJECTED: '已驳回', COMPLETED: '已完成' }[s] || s
}
</script>
