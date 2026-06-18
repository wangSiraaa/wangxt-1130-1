<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">数据概览</div>
    </div>

    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="6" v-for="(stat, key) in stats" :key="key">
        <div class="stat-card">
          <div class="stat-icon" :class="'icon-' + key">
            <el-icon size="28"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">{{ stat.label }}</div>
            <div class="stat-value">{{ stat.value }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card>
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span style="font-weight:600">最新安全间隔期提醒</span>
              <el-button type="primary" link @click="$router.push('/reminders')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="warnings" size="small" v-loading="loading">
            <el-table-column prop="plotName" label="地块" width="120" />
            <el-table-column prop="pesticideName" label="农药" width="120" />
            <el-table-column prop="lastOperationDate" label="施药日期" width="120" />
            <el-table-column prop="safeEndDate" label="解禁日期" width="120" />
            <el-table-column label="剩余天数" width="100">
              <template #default="{ row }">
                <el-tag :type="row.remainingDays > 10 ? 'success' : row.remainingDays > 3 ? 'warning' : 'danger'">
                  {{ row.remainingDays }} 天
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reminderLevel" label="级别" width="90">
              <template #default="{ row }">
                <el-tag :type="levelType(row.reminderLevel)">{{ levelText(row.reminderLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remindContent" label="提醒内容" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card style="margin-bottom:16px">
          <template #header><span style="font-weight:600">业务流程指引</span></template>
          <el-steps direction="vertical" :active="0">
            <el-step title="农艺师开处方" description="诊断病虫害，开具施药处方并提交审批" />
            <el-step title="处方审批" description="审核处方内容，确认用药合理合规" />
            <el-step title="仓库出库" description="禁用农药禁出，按批号发药，库存自动扣减" />
            <el-step title="飞手作业" description="风速超限禁飞，登记天气地块，完成作业" />
            <el-step title="间隔期提醒" description="安全期内禁采收，到期自动提醒解禁" />
          </el-steps>
        </el-card>
        <el-card>
          <template #header><span style="font-weight:600">快捷入口</span></template>
          <div class="shortcuts">
            <div class="shortcut-item" @click="$router.push('/prescription/create')">
              <el-icon size="32" color="#409EFF"><DocumentAdd /></el-icon>
              <span>开处方</span>
            </div>
            <div class="shortcut-item" @click="$router.push('/outbound')">
              <el-icon size="32" color="#67C23A"><Box /></el-icon>
              <span>农药出库</span>
            </div>
            <div class="shortcut-item" @click="$router.push('/flight')">
              <el-icon size="32" color="#E6A23C"><Promotion /></el-icon>
              <span>飞行作业</span>
            </div>
            <div class="shortcut-item" @click="$router.push('/reminders')">
              <el-icon size="32" color="#F56C6C"><Bell /></el-icon>
              <span>间隔期提醒</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { House, Document, Box, Promotion, Bell, Location, Grid, Goods, DocumentAdd } from '@element-plus/icons-vue'
import { api } from '../api'

const loading = ref(false)
const stats = reactive({
  plotCount: { label: '地块总数', value: 0, icon: Location },
  pesticideCount: { label: '农药种类', value: 0, icon: Grid },
  pilotCount: { label: '飞手人数', value: 0, icon: Promotion },
  forbiddenPesticideCount: { label: '禁用农药', value: 0, icon: Bell }
})
const warnings = ref([])

onMounted(async () => {
  try {
    const data = await api.getDashboard()
    stats.plotCount.value = data.plotCount || 0
    stats.pesticideCount.value = data.pesticideCount || 0
    stats.pilotCount.value = data.pilotCount || 0
    stats.forbiddenPesticideCount.value = data.forbiddenPesticideCount || 0
  } catch (e) {}
  try {
    loading.value = true
    const res = await api.queryReminders({ pageNum: 1, pageSize: 8, unreadOnly: false })
    warnings.value = res.records || []
  } catch (e) {} finally {
    loading.value = false
  }
})

function levelType(l) {
  return l === 'DANGER' ? 'danger' : l === 'WARNING' ? 'warning' : 'success'
}
function levelText(l) {
  return l === 'DANGER' ? '危险' : l === 'WARNING' ? '警告' : '提示'
}
</script>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}
.stat-icon {
  width: 60px; height: 60px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}
.icon-plotCount { background: linear-gradient(135deg, #409EFF, #66b1ff); }
.icon-pesticideCount { background: linear-gradient(135deg, #67C23A, #85ce61); }
.icon-pilotCount { background: linear-gradient(135deg, #E6A23C, #ebb563); }
.icon-forbiddenPesticideCount { background: linear-gradient(135deg, #F56C6C, #f78989); }
.stat-label { color: #909399; font-size: 13px; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; color: #303133; }
.shortcuts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 0;
  border-radius: 8px;
  background: #f5f7fa;
  cursor: pointer;
  transition: all .2s;
  font-size: 14px;
  color: #606266;
}
.shortcut-item:hover {
  background: #ecf5ff;
  transform: translateY(-2px);
}
</style>
