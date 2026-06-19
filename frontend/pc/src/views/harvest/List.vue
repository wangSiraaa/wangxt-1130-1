<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">采收计划管理</div>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">新建采收计划</el-button>
    </div>

    <div class="toolbar">
      <el-select v-model="query.plotId" placeholder="地块" clearable filterable style="width:180px">
        <el-option v-for="p in plots" :key="p.id" :label="p.plotName" :value="p.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:140px">
        <el-option label="计划中" value="PLANNED" />
        <el-option label="已锁定" value="LOCKED" />
        <el-option label="已激活" value="ACTIVE" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-checkbox v-model="query.lockedOnly">仅显示已锁定</el-checkbox>
      <el-button type="primary" icon="Search" @click="load">查询</el-button>
      <el-button icon="Refresh" @click="reset">重置</el-button>
      <el-button type="warning" plain icon="Unlock" @click="handleUnlockExpired">解锁到期计划</el-button>
    </div>

    <el-table :data="list" v-loading="loading">
      <el-table-column prop="planNo" label="计划编号" width="150" />
      <el-table-column prop="plotName" label="地块" width="120" />
      <el-table-column prop="cropType" label="作物" width="80" />
      <el-table-column prop="plannedHarvestDate" label="计划采收日" width="120" />
      <el-table-column prop="plannedYield" label="预计产量(kg)" width="120" />
      <el-table-column prop="harvestMethod" label="采收方式" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isLocked === 1 ? 'danger' : row.status === 'COMPLETED' ? 'success' : row.status === 'ACTIVE' ? 'primary' : 'info'">
            {{ row.isLocked === 1 ? '🔒 已锁定' : row.status === 'COMPLETED' ? '已完成' : row.status === 'ACTIVE' ? '已激活' : row.status === 'CANCELLED' ? '已取消' : '计划中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="锁定原因" min-width="260">
        <template #default="{ row }">
          <span v-if="row.isLocked === 1" style="color:#F56C6C">{{ row.lockReason }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="解锁日期" width="120">
        <template #default="{ row }">{{ row.lockExpireDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-if="row.isLocked !== 1 && row.status === 'ACTIVE'" @click="handleComplete(row)">完成采收</el-button>
          <el-button link type="danger" v-if="row.status === 'PLANNED' || row.status === 'ACTIVE'" @click="handleCancel(row)">取消</el-button>
          <el-tooltip v-if="row.isLocked === 1" :content="row.lockReason" placement="top">
            <el-button link type="info" disabled>锁定中</el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end;display:flex"
      v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
      :total="total" @size-change="load" @current-change="load" />

    <el-dialog v-model="showCreateDialog" title="新建采收计划" width="520px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="地块" required>
          <el-select v-model="createForm.plotId" placeholder="请选择地块" filterable style="width:100%">
            <el-option v-for="p in plots" :key="p.id" :label="p.plotName + '（'+ p.cropType +'）'" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划采收日" required>
          <el-date-picker v-model="createForm.plannedHarvestDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="预计产量(kg)">
          <el-input-number v-model="createForm.plannedYield" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="采收方式">
          <el-select v-model="createForm.harvestMethod" style="width:100%">
            <el-option label="人工" value="人工" />
            <el-option label="机械" value="机械" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Unlock } from '@element-plus/icons-vue'
import { api } from '../../api'

const loading = ref(false)
const list = ref([])
const plots = ref([])
const total = ref(0)
const showCreateDialog = ref(false)
const user = JSON.parse(localStorage.getItem('user') || '{}')

const query = reactive({ plotId: null, status: '', lockedOnly: false, pageNum: 1, pageSize: 10 })

const createForm = reactive({
  plotId: null, plannedHarvestDate: '', plannedYield: 0, harvestMethod: '机械', remark: '',
  createBy: user.id, createByName: user.realName
})

onMounted(async () => {
  plots.value = await api.listPlots()
  await load()
})

async function load() {
  loading.value = true
  try {
    const res = await api.queryHarvestPlans({ ...query })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {} finally { loading.value = false }
}

function reset() { query.plotId = null; query.status = ''; query.lockedOnly = false; query.pageNum = 1; load() }

async function handleCreate() {
  if (!createForm.plotId) return ElMessage.warning('请选择地块')
  if (!createForm.plannedHarvestDate) return ElMessage.warning('请选择计划采收日')
  try {
    await api.createHarvestPlan(createForm)
    ElMessage.success('采收计划创建成功')
    showCreateDialog.value = false
    Object.assign(createForm, { plotId: null, plannedHarvestDate: '', plannedYield: 0, harvestMethod: '机械', remark: '' })
    load()
  } catch (e) {}
}

async function handleComplete(row) {
  try {
    await ElMessageBox.confirm('确认完成采收？', '提示')
    await api.completeHarvest(row.id, { operatorId: user.id })
    ElMessage.success('采收完成')
    load()
  } catch (e) {}
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm('确认取消该采收计划？', '提示')
    await api.cancelHarvest(row.id)
    ElMessage.success('已取消')
    load()
  } catch (e) {}
}

async function handleUnlockExpired() {
  try {
    await api.unlockExpiredPlans()
    ElMessage.success('到期计划已自动解锁')
    load()
  } catch (e) {}
}
</script>
