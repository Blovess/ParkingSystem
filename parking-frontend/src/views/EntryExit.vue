<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'
import { useParkingStore } from '../stores/parking'
import { ElMessage } from 'element-plus'

const router = useRouter()
const parkingStore = useParkingStore()

const plateResult = ref(null)
const plateError = ref('')
const plateLoading = ref(false)
const records = ref([])
let timer = null

async function uploadImage(file) {
  plateLoading.value = true
  plateResult.value = null
  plateError.value = ''
  try {
    const reader = new FileReader()
    reader.onload = async (e) => {
      const base64 = e.target.result.split(',')[1]
      try {
        const res = await http.post('/plate/recognize', { image: base64 })
        const body = res.data
        if (body.code === 200) {
          const inner = body.data
          if (inner && inner.code === 200) {
            plateResult.value = inner
            ElMessage.success(`识别成功: ${inner.plate}`)
          } else {
            plateError.value = inner ? (inner.message || '未检测到车牌') : '识别结果为空'
            ElMessage.warning(plateError.value)
          }
        } else if (body.code === 400) {
          plateError.value = '图片解码失败，请上传清晰的车辆照片'
          ElMessage.error(plateError.value)
        } else if (body.code === 404) {
          plateError.value = '未检测到车牌，请调整角度或更换图片'
          ElMessage.warning(plateError.value)
        } else if (body.code === 500) {
          plateError.value = '车牌识别服务异常，请确认微服务已启动'
          ElMessage.error(plateError.value)
        } else {
          plateError.value = body.message || '识别失败'
          ElMessage.error(plateError.value)
        }
      } catch {
        plateError.value = '网络请求失败，请检查后端服务'
        ElMessage.error(plateError.value)
      }
    }
    reader.readAsDataURL(file.raw || file)
  } finally {
    plateLoading.value = false
  }
}

async function doEntry() {
  if (!plateResult.value) {
    ElMessage.warning('请先识别车牌')
    return
  }
  const plate = plateResult.value.plate || plateResult.value.plate_number
  try {
    const res = await http.post('/parking/entry', { plateNumber: plate })
    if (res.data.code === 200) {
      const recordId = res.data.data.recordId
      parkingStore.setPendingPlateNumber(plate)
      parkingStore.setPendingRecordId(recordId)
      ElMessage.success('入场登记成功，请选择车位')
      router.push('/parking-monitor')
      await loadRecords()
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('入场失败')
  }
}

async function doExit() {
  if (!plateResult.value) {
    ElMessage.warning('请先识别车牌')
    return
  }
  const plate = plateResult.value.plate || plateResult.value.plate_number
  try {
    const res = await http.post('/parking/exit', { plateNumber: plate })
    if (res.data.code === 200) {
      const msg = res.data.data?.message || '缴费成功，一路顺风'
      ElMessage.success(msg)
      parkingStore.triggerRefresh()
      await loadRecords()
    } else {
      ElMessage.error(res.data.message || '请先进行缴费')
    }
  } catch {
    ElMessage.error('离场失败')
  }
}

async function loadRecords() {
  try {
    const res = await http.get('/record/list', { params: { pageNum: 1, pageSize: 10 } })
    if (res.data.code === 200) {
      records.value = res.data.data.records || []
    }
  } catch {}
}

onMounted(() => { loadRecords(); timer = setInterval(loadRecords, 5000) })
onUnmounted(() => clearInterval(timer))
</script>

<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card>
        <template #header>车牌识别</template>
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="uploadImage" accept="image/*" drag>
          <div style="font-size:24px">+</div>
          <div>点击或拖拽上传图片</div>
        </el-upload>
        <div v-if="plateLoading" style="text-align:center;margin-top:12px">
          <el-icon class="is-loading"><i class="el-icon-loading" /></el-icon> 识别中...
        </div>
        <div v-if="plateError" style="margin-top:12px">
          <el-alert :title="plateError" type="error" :closable="false" show-icon />
        </div>
        <div v-if="plateResult" style="margin-top:12px;text-align:center">
          <el-tag type="success" size="large">车牌: {{ plateResult.plate || plateResult.plate_number }}</el-tag>
          <div style="margin-top:6px;color:#999">置信度: {{ plateResult.confidence }}</div>
        </div>
        <div style="margin-top:16px;display:flex;gap:10px">
          <el-button type="primary" @click="doEntry" style="flex:1">模拟入场</el-button>
          <el-button type="warning" @click="doExit" style="flex:1">模拟离场</el-button>
        </div>
      </el-card>
    </el-col>
    <el-col :span="16">
      <el-card>
        <template #header>最近停车记录 (自动刷新)</template>
        <el-table :data="records" stripe size="small">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="plateNumber" label="车牌" width="120" />
          <el-table-column prop="spaceId" label="车位ID" width="80" />
          <el-table-column prop="entryTime" label="入场时间" />
          <el-table-column prop="amount" label="费用（元）" width="90">
            <template #default="{ row }">
              {{ row.amount != null ? row.amount : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'warning' : 'success'" size="small">
                {{ row.status === 0 ? '进行中' : '已完成' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>
