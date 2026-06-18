<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-title">新增病虫害处方</div>
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
    </div>

    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="施药地块" prop="plotId">
              <el-select v-model="form.plotId" placeholder="请选择地块" filterable style="width:100%" @change="onPlotChange">
                <el-option v-for="p in plots" :key="p.id" :label="p.plotName + '（'+ p.cropName +'）'" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="处方日期" prop="prescriptionDate">
              <el-date-picker v-model="form.prescriptionDate" type="date" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="病虫害类别" prop="pestType">
              <el-select v-model="form.pestType" placeholder="请选择" style="width:100%">
                <el-option label="病害" value="病害" />
                <el-option label="虫害" value="虫害" />
                <el-option label="草害" value="草害" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="病虫害名称" prop="pestName">
              <el-input v-model="form.pestName" placeholder="如：稻飞虱、稻瘟病等" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发生程度" prop="severity">
              <el-radio-group v-model="form.severity">
                <el-radio label="轻">轻</el-radio>
                <el-radio label="中">中</el-radio>
                <el-radio label="重">重</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发生面积(亩)" prop="occurrenceArea">
              <el-input-number v-model="form.occurrenceArea" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="症状描述" prop="symptomDescription">
              <el-input v-model="form.symptomDescription" type="textarea" :rows="2" placeholder="描述病虫害症状" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="诊断意见" prop="diagnosisNote">
              <el-input v-model="form.diagnosisNote" type="textarea" :rows="2" placeholder="诊断意见与防治建议" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">农药明细</el-divider>

        <el-table :data="form.details" border style="width:100%">
          <el-table-column label="序号" type="index" width="60" />
          <el-table-column label="农药名称" min-width="200">
            <template #default="{ row, $index }">
              <el-select v-model="row.pesticideId" placeholder="选择农药" filterable style="width:100%" @change="(v) => onPesticideChange(v, $index)">
                <el-option v-for="p in allowedPesticides" :key="p.id"
                  :label="p.pesticideName + '（'+ p.formulation +' '+ p.concentration +'）'" :value="p.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="剂型" width="100">
            <template #default="{ row }">{{ row.formulation || '-' }}</template>
          </el-table-column>
          <el-table-column label="浓度" width="100">
            <template #default="{ row }">{{ row.concentration || '-' }}</template>
          </el-table-column>
          <el-table-column label="最高风速(m/s)" width="120">
            <template #default="{ row }">{{ row.maxWindSpeed || '通用5.0' }}</template>
          </el-table-column>
          <el-table-column label="每亩用药量" width="180">
            <template #default="{ row }">
              <el-input-number v-model="row.dosagePerMu" :min="0" :precision="4" style="width:100%" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="100">
            <template #default="{ row }">
              <el-select v-model="row.dosageUnit" style="width:100%">
                <el-option label="ml" value="ml" />
                <el-option label="g" value="g" />
                <el-option label="L" value="L" />
                <el-option label="kg" value="kg" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="使用方法" width="180">
            <template #default="{ row }">
              <el-select v-model="row.usageMethod" style="width:100%">
                <el-option label="喷雾" value="喷雾" />
                <el-option label="飞防" value="飞防" />
                <el-option label="撒施" value="撒施" />
                <el-option label="灌根" value="灌根" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="施药次数" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.times" :min="1" :max="10" />
            </template>
          </el-table-column>
          <el-table-column label="间隔天数" width="100">
            <template #default="{ row }">
              <el-input-number v-model="row.intervalDays" :min="1" :max="30" />
            </template>
          </el-table-column>
          <el-table-column label="备注">
            <template #default="{ row }">
              <el-input v-model="row.remark" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" icon="Delete" @click="form.details.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top:12px">
          <el-button type="primary" plain :icon="Plus" @click="addDetail">添加农药</el-button>
        </div>

        <div style="margin-top:32px;text-align:right">
          <el-button :icon="ArrowLeft" @click="$router.back()">取消</el-button>
          <el-button :icon="EditPen" @click="handleSave">保存草稿</el-button>
          <el-button type="primary" :icon="Check" :loading="submitting" @click="handleSubmit">保存并提交</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Plus, EditPen, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../../api'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const plots = ref([])
const pesticides = ref([])

const user = JSON.parse(localStorage.getItem('user') || '{}')

const form = reactive({
  plotId: null, plotName: '', cropId: null, cropName: '',
  prescriptionDate: new Date().toISOString().slice(0, 10),
  pestType: '', pestName: '', severity: '中', occurrenceArea: 0,
  symptomDescription: '', diagnosisNote: '',
  agronomistId: user.id, agronomistName: user.realName,
  details: [{ pesticideId: null, formulation: '', concentration: '', maxWindSpeed: null, dosagePerMu: 0, dosageUnit: 'ml', usageMethod: '飞防', times: 1, intervalDays: 7, remark: '' }]
})

const rules = {
  plotId: [{ required: true, message: '请选择地块', trigger: 'change' }],
  prescriptionDate: [{ required: true, message: '请选择处方日期', trigger: 'change' }],
  pestType: [{ required: true, message: '请选择病虫害类别', trigger: 'change' }],
  pestName: [{ required: true, message: '请输入病虫害名称', trigger: 'blur' }],
  severity: [{ required: true, message: '请选择发生程度', trigger: 'change' }]
}

const allowedPesticides = computed(() => pesticides.value.filter(p => p.isForbidden !== 1))

onMounted(async () => {
  plots.value = await api.listPlots()
  pesticides.value = await api.listPesticides()
})

function onPlotChange(id) {
  const p = plots.value.find(x => x.id === id)
  if (p) { form.plotName = p.plotName; form.cropName = p.cropName }
}
function onPesticideChange(id, idx) {
  const p = pesticides.value.find(x => x.id === id)
  if (p) {
    form.details[idx].formulation = p.formulation
    form.details[idx].concentration = p.concentration
    form.details[idx].maxWindSpeed = p.maxWindSpeed
    form.details[idx].pesticideName = p.pesticideName
    form.details[idx].pesticideCode = p.pesticideCode
  }
}
function addDetail() {
  form.details.push({ pesticideId: null, formulation: '', concentration: '', maxWindSpeed: null, dosagePerMu: 0, dosageUnit: 'ml', usageMethod: '飞防', times: 1, intervalDays: 7, remark: '' })
}

async function validate() {
  await formRef.value.validate()
  if (form.details.length === 0) throw new Error('请至少添加一条农药明细')
  for (const d of form.details) {
    if (!d.pesticideId) throw new Error('请选择农药')
    if (d.dosagePerMu <= 0) throw new Error('每亩用药量必须大于0')
  }
}

async function handleSave() {
  try {
    await validate().catch(() => {
      ElMessage.warning('数据有问题，但仍可保存草稿')
    })
    const res = await api.createPrescription(form)
    ElMessage.success('草稿保存成功')
    router.push(`/prescription/${res.id}`)
  } catch (e) {}
}
async function handleSubmit() {
  try {
    await validate()
    submitting.value = true
    const res = await api.createPrescription(form)
    await api.submitPrescription(res.id, user.id, user.realName)
    ElMessage.success('处方创建并提交成功')
    router.push(`/prescription/${res.id}`)
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}
</script>
