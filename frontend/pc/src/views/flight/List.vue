<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">飞行作业记录</div>
    </div>

    <div class="toolbar">
      <el-select v-model="query.status" placeholder="作业状态" clearable style="width:140px">
        <el-option label="待飞行" value="PENDING" />
        <el-option label="飞行中" value="IN_PROGRESS" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-select v-model="query.plotId" placeholder="地块" clearable filterable style="width:180px">
        <el-option v-for="p in plots" :key="p.id" :label="p.plotName" :value="p.id" />
      </el-select>
      <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" icon="Search" @click="load">查询</el-button>
      <el-button icon="Refresh" @click="reset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading">
      <el-table-column prop="operationNo" label="作业编号" width="150" />
      <el-table-column prop="prescriptionNo" label="处方号" width="150" />
      <el-table-column prop="outboundNo" label="出库单号" width="150" />
      <el-table-column prop="plotName" label="地块" width="120" />
      <el-table-column prop="operationDate" label="作业日期" width="120" />
      <el-table-column label="天气" width="180">
        <template #default="{ row }">
          <span v-if="row.weather">{{ row.weather }} | {{ row.temperature }}°C | 湿度{{ row.humidity }}%</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="windSpeed" label="风速(m/s)" width="110" />
      <el-table-column prop="pilotName" label="飞手" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'COMPLETED' ? 'success' : row.status === 'IN_PROGRESS' ? 'primary' : 'warning'">
            {{ row.status === 'COMPLETED' ? '已完成' : row.status === 'IN_PROGRESS' ? '飞行中' : '待飞行' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="actualArea" label="作业面积(亩)" width="120" />
      <el-table-column label="飞行时长" width="110">
        <template #default="{ row }">{{ row.flightDuration || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row.id)">详情</el-button>
          <el-button link type="primary" v-if="row.status === 'IN_PROGRESS' && isPilot" @click="handleComplete(row)">完成</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end;display:flex"
      v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
      :total="total" @size-change="load" @current-change="load" />

    <el-dialog v-model="completeDialogVisible" title="完成飞行作业" width="520px">
      <el-form :model="completeForm" label-width="110px">
        <el-form-item label="实际作业面积(亩)">
          <el-input-number v-model="completeForm.actualArea" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="飞行时长(分钟)">
          <el-input-number v-model="completeForm.flightDurationMinutes" :min="0" />
        </el-form-item>
        <el-form-item label="实际喷药量(L)">
          <el-input-number v-model="completeForm.actualSprayVolume" :min="0" :precision="2" step="1" />
        </el-form-item>
        <el-form-item label="作业效果">
          <el-rate v-model="completeForm.effectiveness" show-text :texts="['差','一般','良好','优秀','极佳']" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="completeForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmComplete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '../../api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('user') || '{}')
const isPilot = ['PILOT', 'ADMIN'].includes(user.roleCode)

const loading = ref(false)
const list = ref([])
const plots = ref([])
const total = ref(0)
const query = reactive({ status: '', plotId: null, dateRange: [], pageNum: 1, pageSize: 10 })

const completeDialogVisible = ref(false)
const currentRow = ref(null)
const completeForm = reactive({ actualArea: 0, flightDurationMinutes: 0, actualSprayVolume: 0, effectiveness: 3, remark: '' })

onMounted(async () => {
  plots.value = await api.listPlots()
  await load()
})
async function load() {
  loading.value = true
  try {
    const [startDate, endDate] = query.dateRange || []
    const res = await api.queryFlights({ ...query, startDate, endDate, pilotId: isPilot && user.roleCode === 'PILOT' ? user.id : null })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {} finally { loading.value = false }
}
function reset() { query.status = ''; query.plotId = null; query.dateRange = []; query.pageNum = 1; load() }
function goDetail(id) {
  // 预留详情页
  ElMessage.info('作业详情功能开发中')
}
function handleComplete(row) {
  currentRow.value = row
  completeForm.actualArea = row.plannedArea || 0
  completeForm.flightDurationMinutes = 0
  completeForm.actualSprayVolume = 0
  completeForm.effectiveness = 3
  completeForm.remark = ''
  completeDialogVisible.value = true
}
async function confirmComplete() {
  try {
    await api.completeFlight(currentRow.value.id, completeForm)
    ElMessage.success('作业完成')
    completeDialogVisible.value = false
    load()
  } catch (e) {}
}
</script>
