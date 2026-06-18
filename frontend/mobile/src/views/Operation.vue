<template>
  <div>
    <van-nav-bar title="飞行作业登记" left-arrow @click-left="$router.back()" fixed placeholder />
    <div class="page">
      <div class="card" v-loading="loading" v-if="outbound">
        <div class="card-title">
          <span>出库单信息</span>
          <van-tag round type="primary">{{ outbound.outboundNo }}</van-tag>
        </div>
        <div class="info-row"><span class="info-label">地块</span><span class="info-value">{{ outbound.plotName }}</span></div>
        <div class="info-row"><span class="info-label">作物</span><span class="info-value">{{ outbound.cropName }}</span></div>
        <div class="info-row"><span class="info-label">处方号</span><span class="info-value">{{ outbound.prescriptionNo }}</span></div>
        <div class="info-row"><span class="info-label">病虫害</span><span class="info-value">{{ outbound.pestName }}</span></div>
        <div class="info-row"><span class="info-label">仓管员</span><span class="info-value">{{ outbound.warehouseKeeperName }} · {{ outbound.outboundTime }}</span></div>

        <div style="margin-top:12px;padding-top:12px;border-top:1px dashed #ebedf0">
          <div style="color:#969799;font-size:12px;margin-bottom:6px">农药明细</div>
          <div v-for="p in outbound.details" :key="p.id" class="pest-item">
            <span>{{ p.pesticideName }}</span>
            <span style="color:#969799">{{ p.actualQuantity }}{{ p.unit }} · {{ p.batchNo }}</span>
          </div>
        </div>
      </div>

      <div class="card" v-if="checkResult">
        <div class="card-title">
          <span>安全预检</span>
          <van-tag round :type="checkResult.canProceed ? 'success' : 'danger'">
            {{ checkResult.canProceed ? '可作业' : '禁止作业' }}
          </van-tag>
        </div>
        <div v-if="checkResult.warnings?.length" style="margin-top:8px">
          <div v-for="(w, i) in checkResult.warnings" :key="i"
            class="check-warn" :class="'warn-' + w.level">
            <van-icon :name="w.level === 'DANGER' ? 'warning' : w.level === 'WARNING' ? 'info-o' : 'passed'" />
            {{ w.message }}
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title"><span>作业天气登记</span></div>
        <van-form>
          <van-field v-model="form.operationDate" label="作业日期" readonly :clickable="false">
            <template #input>
              <van-date-picker v-model="datePickerVisible" title="选择作业日期"
                @confirm="onDateConfirm" v-model:show="datePickerVisible" :max-date="new Date()">
                <template #default="{ currentDate }">
                  <div style="padding:10px;text-align:center;color:#1989fa">
                    {{ currentDate?.toISOString?.()?.slice(0,10) || form.operationDate }}
                  </div>
                </template>
              </van-date-picker>
              <span @click="datePickerVisible = true" style="padding:10px 0;display:block">
                {{ form.operationDate }}
              </span>
            </template>
          </van-field>
          <van-field v-model="form.operationTimeStr" label="作业时段" placeholder="如 上午/下午">
            <template #right-icon>
              <van-icon name="clock-o" />
            </template>
          </van-field>

          <van-field label="天气" required>
            <template #input>
              <div style="display:flex;gap:8px;flex-wrap:wrap;padding:6px 0">
                <van-tag round :type="form.weather === w ? 'primary' : 'default'"
                  v-for="w in weathers" :key="w" @click="form.weather = w" style="padding:8px 16px">{{ w }}</van-tag>
              </div>
            </template>
          </van-field>

          <van-field v-model.number="form.windSpeed" type="digit" label="风速(m/s)"
            placeholder="必填，5级风≈8.0m/s" required>
            <template #extra>
              <span v-if="form.windSpeed" :style="{color: windOk ? '#07c160' : '#ee0a24', fontWeight:600}">
                {{ windOk ? '合格' : '超限' }}
              </span>
            </template>
          </van-field>

          <van-field v-model.number="form.temperature" type="digit" label="温度(°C)" placeholder="作业环境温度" />
          <van-field v-model.number="form.humidity" type="number" label="湿度(%)" placeholder="0-100" />
          <van-field v-model.number="form.windDirection" label="风向" placeholder="如 东北风、东南风" />
        </van-form>
      </div>

      <div class="card">
        <div class="card-title"><span>作业登记</span></div>
        <van-form>
          <van-field v-model.number="form.plannedArea" type="digit" label="计划面积(亩)" />
          <van-field v-model.number="form.actualArea" type="digit" label="实际面积(亩)" />
          <van-field v-model.number="form.altitude" type="number" label="飞行高度(m)" />
          <van-field v-model.number="form.flightSpeed" type="digit" label="飞行速度(m/s)" />
          <van-field v-model.number="form.flightSortie" type="number" label="架次" placeholder="总飞行架次" />
          <van-field v-model="form.flightDurationStr" label="飞行时长" placeholder="如：1小时20分">
            <template #right-icon><van-icon name="clock-o" /></template>
          </van-field>
          <van-field v-model.number="form.actualSprayVolume" type="digit" label="实际喷药量(L)" />
          <van-field v-model="form.pilotRemark" type="textarea" label="作业备注" rows="3" placeholder="作业情况、问题等" autosize />
        </van-form>
      </div>

      <div style="padding:0 0 24px">
        <van-button block type="warning" plain :loading="checking" @click="runPreCheck" style="margin-bottom:10px">
          安全预检
        </van-button>
        <van-button block type="primary" :loading="submitting" @click="handleStart" :disabled="!checkResult?.canProceed && !skipWarn">
          {{ operation?.status === 'IN_PROGRESS' ? '更新作业信息' : '开始/登记作业' }}
        </van-button>
        <div style="text-align:center;margin-top:10px">
          <van-checkbox v-model="skipWarn" shape="square" label-position="right">
            确认已知晓安全提示，作业条件责任自负
          </van-checkbox>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { api } from '../api'

const route = useRoute()
const router = useRouter()
const user = JSON.parse(localStorage.getItem('m_user') || '{}')

const outboundId = route.params.outboundId
const loading = ref(false)
const checking = ref(false)
const submitting = ref(false)
const outbound = ref(null)
const operation = ref(null)
const checkResult = ref(null)
const datePickerVisible = ref(false)
const skipWarn = ref(false)

const form = reactive({
  operationDate: new Date().toISOString().slice(0, 10),
  operationTimeStr: '',
  weather: '晴', windSpeed: null, temperature: null, humidity: null, windDirection: '',
  plannedArea: 0, actualArea: 0, altitude: null, flightSpeed: null, flightSortie: null,
  flightDurationStr: '', actualSprayVolume: null, pilotRemark: ''
})

const weathers = ['晴', '多云', '阴', '小雨', '中雨', '大雨', '雾']

const windOk = computed(() => {
  if (form.windSpeed == null) return true
  const max = 5.0
  return form.windSpeed <= max
})

watch([() => form.windSpeed, () => form.weather, () => form.humidity, () => form.temperature], runPreCheckAuto)

function onDateConfirm(values) {
  form.operationDate = values.map(v => String(v).padStart(2, '0')).join('-').replace(/^(\d{4}-\d{2}-\d{2}).*/, '$1')
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    outbound.value = await api.getOutbound(outboundId)
    if (outbound.value.details?.length) {
      form.plannedArea = outbound.value.occurrenceArea || 0
    }
    const [s, e] = [form.operationDate, form.operationDate]
    const flights = await api.queryFlights({ pageNum: 1, pageSize: 999, outboundId, startDate: s, endDate: e })
    const list = flights.records || []
    if (list.length) {
      operation.value = list[0]
      Object.assign(form, {
        operationDate: list[0].operationDate, operationTimeStr: list[0].operationTimeStr,
        weather: list[0].weather, windSpeed: list[0].windSpeed, temperature: list[0].temperature,
        humidity: list[0].humidity, windDirection: list[0].windDirection,
        plannedArea: list[0].plannedArea || form.plannedArea, actualArea: list[0].actualArea,
        altitude: list[0].altitude, flightSpeed: list[0].flightSpeed,
        flightSortie: list[0].flightSortie, actualSprayVolume: list[0].actualSprayVolume,
        pilotRemark: list[0].pilotRemark
      })
    }
    await runPreCheck()
  } catch (e) {} finally { loading.value = false }
}

async function runPreCheckAuto() {
  if (!form.windSpeed && !form.weather) return
  try {
    checkResult.value = await api.preCheckFlight({
      outboundId, operationDate: form.operationDate, windSpeed: form.windSpeed
    })
  } catch (e) {}
}

async function runPreCheck() {
  if (!form.operationDate) return showToast('请选择作业日期')
  checking.value = true
  try {
    checkResult.value = await api.preCheckFlight({
      outboundId, operationDate: form.operationDate, windSpeed: form.windSpeed
    })
    if (checkResult.value.warnings?.length) {
      const hasDanger = checkResult.value.warnings.some(w => w.level === 'DANGER')
      if (hasDanger) {
        showDialog({
          title: '安全警告',
          message: '存在严重安全风险，建议取消本次作业！' +
            checkResult.value.warnings.filter(w => w.level === 'DANGER').map(w => '\n· ' + w.message).join(''),
          confirmButtonText: '我已知晓'
        })
      } else {
        showToast('预检完成：需注意相关提示')
      }
    } else {
      showToast({ type: 'success', message: '预检通过' })
    }
  } catch (e) {} finally { checking.value = false }
}

async function handleStart() {
  if (!form.operationDate) return showToast('请选择作业日期')
  if (form.windSpeed == null) return showToast('请输入风速')
  if (!form.weather) return showToast('请选择天气')
  if (!checkResult) await runPreCheck()
  if (!checkResult.canProceed && !skipWarn.value) {
    return showDialog({
      title: '安全风险',
      message: '存在禁止作业的风险项，请先确认预检结果或勾选责任确认。',
      confirmButtonText: '好的'
    })
  }
  try {
    submitting.value = true
    const data = {
      plotId: outbound.value.plotId, plotName: outbound.value.plotName,
      operationDate: form.operationDate,
      operationTimeStr: form.operationTimeStr,
      weather: form.weather, windSpeed: form.windSpeed, temperature: form.temperature,
      humidity: form.humidity, windDirection: form.windDirection,
      plannedArea: form.plannedArea, actualArea: form.actualArea,
      altitude: form.altitude, flightSpeed: form.flightSpeed,
      flightSortie: form.flightSortie, actualSprayVolume: form.actualSprayVolume,
      pilotRemark: form.pilotRemark
    }
    if (operation.value && operation.value.status === 'IN_PROGRESS') {
      Object.assign(operation.value, data)
      showToast({ type: 'success', message: '作业信息已更新' })
      router.back()
    } else {
      await api.createFlight({
        outboundId, pilotId: user.id, pilotName: user.realName, operation: data
      })
      showToast({ type: 'success', message: '作业登记成功' })
      setTimeout(() => router.back(), 800)
    }
  } catch (e) {
    console.error(e)
  } finally { submitting.value = false }
}
</script>

<style scoped>
.pest-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}
.check-warn {
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}
.warn-DANGER { background: #fef0f0; color: #ee0a24; }
.warn-WARNING { background: #fff7e8; color: #ff976a; }
.warn-INFO { background: #e8f3ff; color: #1989fa; }
</style>
