<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div class="page-title">处方详情 - {{ data?.prescriptionNo }}</div>
      <div style="display:flex;gap:8px">
        <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
        <el-button v-if="data?.status === 'DRAFT' && isAgronomist" type="primary" @click="handleSubmit">提交审批</el-button>
        <template v-if="data?.status === 'SUBMITTED' && isAdmin">
          <el-button type="success" @click="handleApprove(true)">通过</el-button>
          <el-button type="danger" @click="handleApprove(false)">驳回</el-button>
        </template>
        <el-button v-if="data?.status === 'APPROVED' && isWarehouse" type="primary" @click="goOutbound">生成出库单</el-button>
      </div>
    </div>

    <el-card v-if="data">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="处方单号">{{ data.prescriptionNo }}</el-descriptions-item>
        <el-descriptions-item label="处方状态">
          <el-tag :type="statusType(data.status)">{{ statusText(data.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处方日期">{{ data.prescriptionDate }}</el-descriptions-item>
        <el-descriptions-item label="地块">{{ data.plotName }}（{{ data.cropName }}）</el-descriptions-item>
        <el-descriptions-item label="发生程度">
          <el-tag :type="data.severity === '重' ? 'danger' : data.severity === '中' ? 'warning' : 'success'">{{ data.severity }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="发生面积">{{ data.occurrenceArea }} 亩</el-descriptions-item>
        <el-descriptions-item label="病虫害类别">{{ data.pestType }}</el-descriptions-item>
        <el-descriptions-item label="病虫害名称" :span="2">{{ data.pestName }}</el-descriptions-item>
        <el-descriptions-item label="症状描述" :span="3">{{ data.symptomDescription }}</el-descriptions-item>
        <el-descriptions-item label="诊断意见" :span="3">{{ data.diagnosisNote }}</el-descriptions-item>
        <el-descriptions-item label="农艺师">{{ data.agronomistName }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ data.approveBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批备注">{{ data.approveRemark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">农药明细</el-divider>
      <el-table :data="data.details" border>
        <el-table-column label="序号" type="index" width="60" />
        <el-table-column prop="pesticideName" label="农药名称" />
        <el-table-column prop="formulation" label="剂型" width="100" />
        <el-table-column prop="concentration" label="浓度" width="100" />
        <el-table-column label="每亩用量" width="140">
          <template #default="{ row }">{{ row.dosagePerMu }} {{ row.dosageUnit }}</template>
        </el-table-column>
        <el-table-column prop="usageMethod" label="使用方法" width="100" />
        <el-table-column prop="times" label="次数" width="80" />
        <el-table-column prop="intervalDays" label="间隔(天)" width="90" />
        <el-table-column prop="maxWindSpeed" label="风速限制" width="110">
          <template #default="{ row }">{{ row.maxWindSpeed || '5.0' }} m/s</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const isAgronomist = ['AGRONOMIST', 'ADMIN'].includes(user.roleCode)
const isAdmin = user.roleCode === 'ADMIN'
const isWarehouse = ['WAREHOUSE', 'ADMIN'].includes(user.roleCode)

const loading = ref(false)
const data = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    data.value = await api.getPrescription(route.params.id)
  } catch (e) {} finally {
    loading.value = false
  }
}
function statusType(s) {
  const m = { DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', COMPLETED: '' }
  return m[s] || ''
}
function statusText(s) {
  const m = { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审批', REJECTED: '已驳回', COMPLETED: '已完成' }
  return m[s] || s
}
async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确认提交此处方？', '提示', { type: 'warning' })
    await api.submitPrescription(data.value.id, user.id, user.realName)
    ElMessage.success('提交成功')
    load()
  } catch (e) {}
}
async function handleApprove(passed) {
  try {
    const remark = passed ? '' : await ElMessageBox.prompt('请输入驳回原因', '审批').then(r => r.value)
    await api.approvePrescription(data.value.id, user.id, remark, passed)
    ElMessage.success(passed ? '审批通过' : '已驳回')
    load()
  } catch (e) { if (e !== 'cancel') throw e }
}
function goOutbound() {
  ElMessageBox.confirm('确认生成此处方的出库单？', '提示', { type: 'warning' })
    .then(async () => {
      const out = await api.createOutbound({
        prescriptionId: data.value.id, warehouseKeeperId: user.id, warehouseKeeperName: user.realName
      })
      ElMessage.success('出库单已创建')
      router.push(`/outbound/${out.id}`)
    }).catch(() => {})
}
</script>
