<template>
  <div class="page-container" v-loading="loading">
    <div class="page-header">
      <div class="page-title">出库单详情 - {{ data?.outboundNo }}</div>
      <div style="display:flex;gap:8px">
        <el-button icon="ArrowLeft" @click="$router.back()">返回</el-button>
      </div>
    </div>

    <el-card v-if="data">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="出库单号">{{ data.outboundNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="data.status === 'PENDING' ? 'warning' : data.status === 'OUTBOUND' ? 'primary' : 'success'">
            {{ data.status === 'PENDING' ? '待出库' : data.status === 'OUTBOUND' ? '已出库' : '已完成' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="关联处方">
          <el-link type="primary" @click="$router.push('/prescription/' + data.prescriptionId)">
            {{ data.prescriptionNo }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="地块">{{ data.plotName }}（{{ data.cropName }}）</el-descriptions-item>
        <el-descriptions-item label="仓管员">{{ data.warehouseKeeperName }}</el-descriptions-item>
        <el-descriptions-item label="预计出库">{{ data.expectedDate }}</el-descriptions-item>
        <el-descriptions-item label="领用人" v-if="data.receiverName">{{ data.receiverName }}</el-descriptions-item>
        <el-descriptions-item label="出库时间" v-if="data.outboundTime" :span="2">{{ data.outboundTime }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">出库明细</el-divider>
      <el-table :data="details" border>
        <el-table-column label="序号" type="index" width="60" />
        <el-table-column label="农药名称" prop="pesticideName" min-width="160" />
        <el-table-column label="剂型" prop="formulation" width="100" />
        <el-table-column label="浓度" prop="concentration" width="100" />
        <el-table-column label="需要数量" width="130">
          <template #default="{ row }">{{ row.requiredQuantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="批号" v-if="data.status === 'PENDING'" width="160">
          <template #default="{ row, $index }">
            <el-select v-model="row.batchNo" placeholder="选择批号" filterable style="width:100%"
              @change="(v) => onBatchChange(v, $index)">
              <el-option v-for="s in row.stockList" :key="s.batchNo" :label="s.batchNo + '（剩余'+ s.currentQuantity + s.unit +'，'+ s.productionDate +'生产）'" :value="s.batchNo" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="批号" v-else prop="batchNo" width="140" />
        <el-table-column label="实际出库" width="130" v-if="data.status === 'PENDING'">
          <template #default="{ row }">
            <el-input-number v-model="row.actualQuantity" :min="0" :max="parseFloat(row.requiredQuantity)"
              :precision="3" step="0.1" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="实际出库" v-else width="130">
          <template #default="{ row }">{{ row.actualQuantity }} {{ row.unit }}</template>
        </el-table-column>
        <el-table-column label="单价(元)" prop="unitPrice" width="100" />
        <el-table-column label="禁用情况" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.isForbidden === 1" type="danger" effect="dark">禁用</el-tag>
            <el-tag v-else type="success" plain>正常</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="data.status === 'PENDING'" style="margin-top:24px">
        <el-divider content-position="left">出库确认</el-divider>
        <el-form :model="confirmForm" label-width="100px" style="max-width:500px">
          <el-form-item label="领用人" required>
            <el-select v-model="confirmForm.receiverId" placeholder="选择飞手" filterable style="width:100%" @change="onReceiver">
              <el-option v-for="p in pilots" :key="p.id" :label="p.realName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="confirmForm.remark" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>

        <div style="margin-top:20px;text-align:right">
          <el-alert type="warning" :closable="false" show-icon style="margin-bottom:12px;text-align:left">
            <template #title>
              <b>出库校验提示：</b>禁用农药无法出库；批号库存不足或过期无法出库；出库将自动扣减库存。
            </template>
          </el-alert>
          <el-button type="primary" :icon="Check" :loading="confirming" @click="handleConfirm">确认出库</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const route = useRoute()
const loading = ref(false)
const confirming = ref(false)
const data = ref(null)
const details = ref([])
const pilots = ref([])
const confirmForm = reactive({ receiverId: null, receiverName: '', remark: '' })

onMounted(async () => {
  pilots.value = await api.listUsers('PILOT').catch(() => [])
  await load()
})

async function load() {
  loading.value = true
  try {
    data.value = await api.getOutbound(route.params.id)
    details.value = (data.value.details || []).map(d => ({
      ...d, stockList: [], batchNo: '', actualQuantity: d.requiredQuantity
    }))
    if (data.value.status === 'PENDING') {
      await Promise.all(details.value.map(async (d, i) => {
        try {
          const stocks = await api.getPesticideStock(d.pesticideId)
          d.stockList = stocks
          if (stocks.length > 0) {
            d.batchNo = stocks[0].batchNo
            d.unitPrice = stocks[0].unitPrice
          }
        } catch (e) {}
      }))
    }
  } catch (e) {} finally { loading.value = false }
}

function onBatchChange(batchNo, idx) {
  const d = details.value[idx]
  const s = d.stockList.find(x => x.batchNo === batchNo)
  if (s) d.unitPrice = s.unitPrice
}
function onReceiver(id) {
  const p = pilots.value.find(x => x.id === id)
  if (p) confirmForm.receiverName = p.realName
}

async function handleConfirm() {
  try {
    if (!confirmForm.receiverId) return ElMessage.warning('请选择领用人')
    const hasForbidden = details.value.some(d => d.isForbidden === 1)
    if (hasForbidden) {
      const names = details.value.filter(d => d.isForbidden === 1).map(d => d.pesticideName).join('、')
      return ElMessage.error(`禁用农药【${names}】无法出库`)
    }
    const invalid = details.value.some(d => !d.batchNo)
    if (invalid) return ElMessage.warning('请为所有农药选择批号')

    await ElMessageBox.confirm(`确认将农药发放给【${confirmForm.receiverName}】？出库后库存将自动扣减。`, '出库确认', {
      type: 'warning', confirmButtonText: '确认出库'
    })
    confirming.value = true

    const confirmDetails = details.value.map(d => ({
      id: d.id, pesticideId: d.pesticideId, batchNo: d.batchNo,
      actualQuantity: d.actualQuantity, unitPrice: d.unitPrice
    }))
    await api.confirmOutbound(data.value.id, {
      details: confirmDetails,
      receiverId: confirmForm.receiverId,
      receiverName: confirmForm.receiverName,
      remark: confirmForm.remark
    })
    ElMessage.success('出库成功')
    await load()
  } catch (e) {
    console.error(e)
  } finally { confirming.value = false }
}
</script>
