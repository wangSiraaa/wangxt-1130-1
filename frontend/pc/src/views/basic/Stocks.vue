<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">农药库存管理</div>
      <el-input v-model="search" placeholder="搜索农药/批号" style="width:260px" clearable />
    </div>
    <el-table :data="filteredList" v-loading="loading" border stripe>
      <el-table-column prop="pesticideCode" label="农药编码" width="120" />
      <el-table-column prop="pesticideName" label="农药名称" width="180" />
      <el-table-column prop="batchNo" label="批号" width="160" />
      <el-table-column prop="productionDate" label="生产日期" width="120" />
      <el-table-column prop="expiryDate" label="有效期至" width="120" />
      <el-table-column label="入库数量" width="130">
        <template #default="{ row }">{{ row.initialQuantity }} {{ row.unit }}</template>
      </el-table-column>
      <el-table-column label="当前库存" width="130">
        <template #default="{ row }">
          <el-tag :type="row.currentQuantity > row.initialQuantity * 0.2 ? 'success' : 'warning'">
            {{ row.currentQuantity }} {{ row.unit }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="unitPrice" label="单价(元)" width="100" />
      <el-table-column prop="storageLocation" label="存放位置" width="140" />
      <el-table-column prop="supplier" label="供应商" width="140" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 1" type="danger">过期</el-tag>
          <el-tag v-else-if="row.currentQuantity <= 0" type="info">缺货</el-tag>
          <el-tag v-else type="success">正常</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { api } from '../../api'
const loading = ref(false)
const list = ref([])
const search = ref('')
const filteredList = computed(() => {
  if (!search.value) return list.value
  const s = search.value.toLowerCase()
  return list.value.filter(x =>
    (x.pesticideName || '').toLowerCase().includes(s) ||
    (x.pesticideCode || '').toLowerCase().includes(s) ||
    (x.batchNo || '').toLowerCase().includes(s)
  )
})
onMounted(async () => {
  loading.value = true
  try {
    const res = await api.queryStocks({ pageNum: 1, pageSize: 999 })
    list.value = res.records || []
  } catch (e) {} finally { loading.value = false }
})
</script>
