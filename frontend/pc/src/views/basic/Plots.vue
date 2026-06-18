<template>
  <div class="page-container">
    <div class="page-header"><div class="page-title">地块档案</div></div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="plotCode" label="地块编号" width="120" />
      <el-table-column prop="plotName" label="地块名称" width="160" />
      <el-table-column prop="location" label="位置" />
      <el-table-column label="面积" width="120">
        <template #default="{ row }">{{ row.area }} 亩</template>
      </el-table-column>
      <el-table-column prop="cropName" label="当前作物" width="120" />
      <el-table-column prop="variety" label="品种" width="120" />
      <el-table-column label="种植日期" width="130">
        <template #default="{ row }">{{ row.plantingDate || '-' }}</template>
      </el-table-column>
      <el-table-column prop="soilType" label="土壤类型" width="120" />
      <el-table-column prop="irrigationMethod" label="灌溉方式" width="120" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../../api'
const loading = ref(false)
const list = ref([])
onMounted(async () => {
  loading.value = true
  try { list.value = await api.listPlots() } catch (e) {}
  finally { loading.value = false }
})
</script>
