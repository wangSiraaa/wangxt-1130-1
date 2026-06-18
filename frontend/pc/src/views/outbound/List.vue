<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">农药出库管理</div>
    </div>

    <div class="toolbar">
      <el-select v-model="query.status" placeholder="出库状态" clearable style="width:140px">
        <el-option label="待出库" value="PENDING" />
        <el-option label="已出库" value="OUTBOUND" />
        <el-option label="已完成" value="COMPLETED" />
      </el-select>
      <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
        start-placeholder="开始日期" end-placeholder="结束日期" />
      <el-button type="primary" icon="Search" @click="load">查询</el-button>
      <el-button icon="Refresh" @click="reset">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" @row-click="goDetail">
      <el-table-column prop="outboundNo" label="出库单号" width="150" />
      <el-table-column prop="prescriptionNo" label="关联处方" width="150" />
      <el-table-column prop="plotName" label="地块" width="120" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'OUTBOUND' ? 'primary' : 'success'">
            {{ row.status === 'PENDING' ? '待出库' : row.status === 'OUTBOUND' ? '已出库' : '已完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseKeeperName" label="仓管员" width="100" />
      <el-table-column prop="receiverName" label="领用人" width="100" />
      <el-table-column prop="expectedDate" label="预计出库" width="120" />
      <el-table-column prop="outboundTime" label="出库时间" width="160" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="goDetail(row.id)">查看</el-button>
          <el-button link type="success" v-if="row.status === 'PENDING'" @click.stop="goDetail(row.id)">出库确认</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:16px;justify-content:flex-end;display:flex"
      v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
      :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
      :total="total" @size-change="load" @current-change="load" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../../api'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ status: '', dateRange: [], pageNum: 1, pageSize: 10 })

onMounted(load)
async function load() {
  loading.value = true
  try {
    const [startDate, endDate] = query.dateRange || []
    const res = await api.queryOutbounds({ ...query, startDate, endDate })
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {} finally { loading.value = false }
}
function reset() { query.status = ''; query.dateRange = []; query.pageNum = 1; load() }
function goDetail(id) {
  router.push(typeof id === 'object' ? `/outbound/${id.id}` : `/outbound/${id}`)
}
</script>
