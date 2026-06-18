<template>
  <div>
    <van-nav-bar title="开处方" left-arrow @click-left="$router.back()" fixed placeholder />
    <div class="page">
      <div class="card">
        <div class="card-title"><span>基本信息</span></div>
        <van-form>
          <van-field
            v-model="form.plotId" is-link readonly label="地块" placeholder="选择地块"
            :value="plotLabel" @click="showPlotPicker = true" />
          <van-field v-model="form.prescriptionDate" label="处方日期" readonly>
            <template #input>
              <span @click="datePickerVisible = true">{{ form.prescriptionDate }}</span>
              <van-date-picker v-model="datePickerVisible" :max-date="new Date()" @confirm="onDateConfirm" />
            </template>
          </van-field>
          <van-field v-model="form.pestType" is-link readonly label="病虫害类别" :value="form.pestType || '请选择'" @click="showPestTypePicker = true" />
          <van-field v-model="form.pestName" label="病虫害名称" placeholder="如：稻飞虱、稻瘟病" />
          <van-field v-model="form.severity" is-link readonly label="发生程度" :value="form.severity || '请选择'" @click="showSeverityPicker = true" />
          <van-field v-model.number="form.occurrenceArea" type="digit" label="发生面积" placeholder="单位：亩" />
          <van-field v-model="form.symptomDescription" type="textarea" label="症状描述" rows="2" autosize placeholder="描述病虫害症状" />
          <van-field v-model="form.diagnosisNote" type="textarea" label="诊断意见" rows="2" autosize placeholder="诊断与防治建议" />
        </van-form>
      </div>

      <div class="card">
        <div class="card-title">
          <span>农药明细（{{ form.details.length }}）</span>
          <van-icon name="plus" color="#1989fa" @click="addDetail" />
        </div>
        <div v-for="(d, i) in form.details" :key="i" class="detail-card">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="font-weight:500">农药 {{ i + 1 }}</span>
            <van-icon name="delete" color="#ee0a24" @click="form.details.splice(i, 1)" />
          </div>
          <van-cell-group inset style="margin-top:10px">
            <van-field v-model="d.pesticideName" label="农药名称" readonly is-link
              :value="d.pesticideName || '选择农药（仅非禁用）'" @click="showPesticidePicker(i)" />
            <van-field v-model="d.formulation" label="剂型" readonly />
            <van-field v-model="d.concentration" label="浓度" readonly />
            <van-field v-model.number="d.dosagePerMu" type="digit" label="每亩用量" placeholder="0.0" />
            <van-field v-model="d.dosageUnit" is-link readonly label="单位" :value="d.dosageUnit" @click="chooseUnit(i)" />
            <van-field v-model="d.usageMethod" is-link readonly label="使用方法" :value="d.usageMethod" @click="chooseUsage(i)" />
            <van-field v-model.number="d.times" type="number" label="施药次数" />
            <van-field v-model.number="d.intervalDays" type="number" label="间隔天数" />
          </van-cell-group>
        </div>
      </div>

      <div style="padding:0 0 24px">
        <van-button block type="primary" :loading="submitting" @click="handleSubmit">保存并提交</van-button>
        <van-button block plain type="primary" :loading="saving" style="margin-top:10px" @click="handleSave">保存草稿</van-button>
      </div>
    </div>

    <van-popup v-model:show="showPlotPicker" round position="bottom">
      <van-picker :columns="plotOptions" title="选择地块" @confirm="onPlotConfirm" @cancel="showPlotPicker = false" />
    </van-popup>
    <van-popup v-model:show="showPestTypePicker" round position="bottom">
      <van-picker :columns="pestTypeOptions" title="病虫害类别" @confirm="onPestTypeConfirm" @cancel="showPestTypePicker = false" />
    </van-popup>
    <van-popup v-model:show="showSeverityPicker" round position="bottom">
      <van-picker :columns="severityOptions" title="发生程度" @confirm="onSeverityConfirm" @cancel="showSeverityPicker = false" />
    </van-popup>
    <van-popup v-model:show="showPesticideList" round position="bottom" :style="{ height: '70%' }">
      <div style="padding:16px;font-weight:600;font-size:16px;border-bottom:1px solid #f2f3f5">选择农药（禁用农药已排除）</div>
      <van-search v-model="pesticideSearch" placeholder="搜索农药名称" shape="round" />
      <van-cell-group inset>
        <van-cell v-for="p in filteredPesticides" :key="p.id" :title="p.pesticideName"
          :label="p.formulation + ' ' + p.concentration + ' · 风速' + (p.maxWindSpeed || '5.0') + 'm/s'"
          is-link @click="selectPesticide(p)" />
      </van-cell-group>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { api } from '../api'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('m_user') || '{}')
const saving = ref(false)
const submitting = ref(false)
const datePickerVisible = ref(false)
const showPlotPicker = ref(false)
const showPestTypePicker = ref(false)
const showSeverityPicker = ref(false)
const showPesticideList = ref(false)
const pesticideSearch = ref('')
const currentPesticideIdx = ref(0)

const plots = ref([])
const pesticides = ref([])

const form = reactive({
  plotId: null, pestType: '', pestName: '', severity: '中', occurrenceArea: 0,
  symptomDescription: '', diagnosisNote: '',
  prescriptionDate: new Date().toISOString().slice(0, 10),
  agronomistId: user.id, agronomistName: user.realName,
  details: [{ pesticideId: null, pesticideName: '', formulation: '', concentration: '', maxWindSpeed: null,
    dosagePerMu: 0, dosageUnit: 'ml', usageMethod: '飞防', times: 1, intervalDays: 7, remark: '' }]
})

const plotOptions = computed(() => [{
  values: plots.value.map(p => ({ text: `${p.plotName}（${p.cropName}·${p.area}亩）`, value: p.id }))
}])
const plotLabel = computed(() => plots.value.find(p => p.id === form.plotId)?.plotName || '')
const pestTypeOptions = [{ values: [{ text: '病害', value: '病害' }, { text: '虫害', value: '虫害' }, { text: '草害', value: '草害' }] }]
const severityOptions = [{ values: [{ text: '轻', value: '轻' }, { text: '中', value: '中' }, { text: '重', value: '重' }] }]
const filteredPesticides = computed(() => {
  const list = pesticides.value.filter(p => p.isForbidden !== 1)
  if (!pesticideSearch.value) return list
  const s = pesticideSearch.value.toLowerCase()
  return list.filter(p => (p.pesticideName || '').toLowerCase().includes(s))
})

onMounted(async () => {
  try {
    plots.value = await api.listPlots()
    pesticides.value = await api.listPesticides()
  } catch (e) {}
})

function onDateConfirm(values) {
  form.prescriptionDate = values.map(v => String(v).padStart(2, '0')).join('-').slice(0, 10)
}
function onPlotConfirm({ selectedOptions }) {
  form.plotId = selectedOptions[0].value
  const p = plots.value.find(x => x.id === form.plotId)
  if (p) { form.plotName = p.plotName; form.cropName = p.cropName }
  showPlotPicker.value = false
}
function onPestTypeConfirm({ selectedOptions }) {
  form.pestType = selectedOptions[0].value
  showPestTypePicker.value = false
}
function onSeverityConfirm({ selectedOptions }) {
  form.severity = selectedOptions[0].value
  showSeverityPicker.value = false
}
function showPesticidePicker(i) { currentPesticideIdx.value = i; showPesticideList.value = true; pesticideSearch.value = '' }
function selectPesticide(p) {
  const d = form.details[currentPesticideIdx.value]
  d.pesticideId = p.id; d.pesticideName = p.pesticideName; d.pesticideCode = p.pesticideCode
  d.formulation = p.formulation; d.concentration = p.concentration; d.maxWindSpeed = p.maxWindSpeed
  showPesticideList.value = false
}
function chooseUnit(i) {
  const units = ['ml', 'g', 'L', 'kg']
  form.details[i].dosageUnit = units[(units.indexOf(form.details[i].dosageUnit) + 1) % units.length]
}
function chooseUsage(i) {
  const usages = ['飞防', '喷雾', '撒施', '灌根']
  form.details[i].usageMethod = usages[(usages.indexOf(form.details[i].usageMethod) + 1) % usages.length]
}
function addDetail() {
  form.details.push({ pesticideId: null, pesticideName: '', formulation: '', concentration: '', maxWindSpeed: null,
    dosagePerMu: 0, dosageUnit: 'ml', usageMethod: '飞防', times: 1, intervalDays: 7, remark: '' })
}
function validate() {
  if (!form.plotId) return '请选择地块'
  if (!form.pestType) return '请选择病虫害类别'
  if (!form.pestName) return '请输入病虫害名称'
  if (form.details.length === 0) return '请添加至少一种农药'
  for (let i = 0; i < form.details.length; i++) {
    const d = form.details[i]
    if (!d.pesticideId) return `请为第${i + 1}种农药选择药品`
    if (d.dosagePerMu <= 0) return `第${i + 1}种农药每亩用量必须大于0`
  }
  return null
}
async function handleSave() {
  const err = validate()
  if (err && form.details.length === 0) return showToast(err)
  try {
    saving.value = true
    const res = await api.createPrescription(form)
    showToast({ type: 'success', message: '草稿已保存' })
    router.replace('/prescription/' + res.id)
  } catch (e) {} finally { saving.value = false }
}
async function handleSubmit() {
  const err = validate()
  if (err) return showToast(err)
  try {
    submitting.value = true
    const res = await api.createPrescription(form)
    await api.submitPrescription(res.id, user.id, user.realName)
    showToast({ type: 'success', message: '处方已提交' })
    router.replace('/prescription/' + res.id)
  } catch (e) {} finally { submitting.value = false }
}
</script>

<style scoped>
.detail-card {
  background: #f7f8fa;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
}
.detail-card .no-border { border-bottom: none !important; }
</style>
