<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">病虫害处方列表</div>
      <el-button type="primary" :icon="Plus" @click="$router.push('/prescription/create')" v-if="['AGRONOMIST','ADMIN'].includes(user?.roleCode)">
        新增处方
      </el-button>
    </div>

    <div class="toolbar">
      <el-select v-model="query.status" placeholder="处方状态" clearable style="width:140px">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已提交" value="SUBMITTED" />
        <el-option label="已审批" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-select v-model="query.plotId" placeholder="选择地块" clearable filterable style="width:180px">
        <el-option v-for="p in plots" :key="p.id" :label="p.plotName" :value="p.id" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="load">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" @row-click="goDetail">
      <el-table-column prop="prescriptionNo" label="处方单号" width="150" />
      <el-table-column prop="plotName" label="地块" width="120" />
      <el-table-column prop="pestType" label="病虫害类" width="100" />
      <el-table-column prop="pestName" label="病虫害名" width="120" />
      <el-table-column prop="severity" label="严重度" width="90">
        <template #default="{ row }">
          <el-tag :type="row.severity === '重' ? 'danger' : row.severity === '中' ? 'warning' : 'success'">{{ row.severity }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="occurrenceArea" label="发生面积(亩)" width="120" />
      <el-table-column prop="prescriptionDate" label="开方日期" width="120" />
      <el-table-column prop="agronomistName" label="农艺师" width="100" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="goDetail(row.id)">查看</el-button>
          <el-button link type="primary" v-if="row.status === 'DRAFT' && isAgronomist" @click.stop="handleSubmit(row)">提交</el-button>
          <template v-if="row.status === 'SUBMITTED' && isAdmin">
            <el-button link type="success" @click.stop="handleApprove(row, true)">通过</el-button>
            <el-button link type="danger" @click.stop="handleApprove(row, false)">驳回</el-button>
          </template>
          <el-button link type="success" v-if="row.status === 'APPROVED' && isWarehouse" @click.stop="goOutbound(row)">生成出库单</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end;display:flex"
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      @size-change="load"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const isAgronomist = computed(() => ['AGRONOMIST', 'ADMIN'].includes(user.value.roleCode))
const isAdmin = computed(() => ['ADMIN'].includes(user.value.roleCode))
const isWarehouse = computed(() => ['WAREHOUSE', 'ADMIN'].includes(user.value.roleCode))

const loading = ref(false)
const list = ref([])
const plots = ref([])
const total = ref(0)
const query = reactive({ status: '', plotId: null, pageNum: 1, pageSize: 10 })

onMounted(async () => {
  plots.value = await api.listPlots()
  await load()
})

async function load() {
  loading.value = true
  try {
    const res = await api.queryPrescriptions(query)
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {} finally {
    loading.value = false
  }
}
function reset() {
  query.status = ''; query.plotId = null; query.pageNum = 1
  load()
}
function statusType(s) {
  const map = { DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', COMPLETED: '' }
  return map[s] || ''
}
function statusText(s) {
  const map = { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已审批', REJECTED: '已驳回', COMPLETED: '已完成' }
  return map[s] || s
}
function goDetail(id) {
  router.push(typeof id === 'object' ? `/prescription/${id.id}` : `/prescription/${id}`)
}
async function handleSubmit(row) {
  try {
    await ElMessageBox.confirm(`确定提交处方【${row.prescriptionNo}】吗？`, '提示', { type: 'warning' })
    await api.submitPrescription(row.id, user.value.id, user.value.realName)
    ElMessage.success('提交成功')
    load()
  } catch (e) {}
}
async function handleApprove(row, passed) {
  try {
    const remark = passed ? '' : await ElMessageBox.prompt('请输入驳回原因', '审批', { inputPlaceholder: '驳回原因...', confirmButtonText: '确认驳回' }).then(r => r.value)
    await api.approvePrescription(row.id, user.value.id, remark, passed)
    ElMessage.success(passed ? '审批通过' : '已驳回')
    load()
  } catch (e) {
    if (e !== 'cancel') throw e
  }
}
function goOutbound(row) {
  ElMessageBox.confirm(`确认生成处方【${row.prescriptionNo}】的出库单？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        const out = await api.createOutbound({
          prescriptionId: row.id,
          warehouseKeeperId: user.value.id,
          warehouseKeeperName: user.value.realName
        })
        ElMessage.success('出库单已创建')
        router.push(`/outbound/${out.id}`)
      } catch (e) {}
    }).catch(() => {})
}
</script>
