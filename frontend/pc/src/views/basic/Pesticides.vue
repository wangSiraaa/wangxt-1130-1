<template>
  <div class="page-container">
    <div class="page-header"><div class="page-title">农药档案</div></div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="pesticideCode" label="农药编码" width="120" />
      <el-table-column prop="pesticideName" label="通用名称" width="160" />
      <el-table-column prop="tradeName" label="商品名称" width="160" />
      <el-table-column prop="formulation" label="剂型" width="100" />
      <el-table-column prop="concentration" label="浓度" width="120" />
      <el-table-column prop="pesticideCategory" label="类别" width="100" />
      <el-table-column prop="toxicity" label="毒性" width="100" />
      <el-table-column label="安全间隔期(天)" width="140">
        <template #default="{ row }">{{ row.safetyIntervalDays || '-' }}</template>
      </el-table-column>
      <el-table-column label="最大风速(m/s)" width="130">
        <template #default="{ row }">{{ row.maxWindSpeed || '通用5.0' }}</template>
      </el-table-column>
      <el-table-column label="是否禁用" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.isForbidden === 1" type="danger" effect="dark">禁用</el-tag>
          <el-tag v-else type="success">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="manufacturer" label="生产厂家" show-overflow-tooltip />
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
  try { list.value = await api.listPesticides() } catch (e) {}
  finally { loading.value = false }
})
</script>
