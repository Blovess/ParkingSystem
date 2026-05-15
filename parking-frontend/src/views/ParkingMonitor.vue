<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'
import { useParkingStore } from '../stores/parking'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const parkingStore = useParkingStore()
const canvasRef = ref(null)
const spaces = ref([])
const routePath = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ id: null, spaceCode: '', area: '', status: 0, xcoordinate: 0, ycoordinate: 0 })
let ctx = null

const spacePositions = [
  { id: 1,  code: 'A101', x: 100, y: 100 },  { id: 2,  code: 'A102', x: 250, y: 100 },
  { id: 3,  code: 'A103', x: 400, y: 100 },  { id: 4,  code: 'A104', x: 550, y: 100 },
  { id: 5,  code: 'A105', x: 700, y: 100 },  { id: 6,  code: 'B101', x: 100, y: 400 },
  { id: 7,  code: 'B102', x: 250, y: 400 },  { id: 8,  code: 'B103', x: 400, y: 400 },
  { id: 9,  code: 'B104', x: 550, y: 400 },  { id: 10, code: 'B105', x: 700, y: 400 }
]

async function loadSpaces() {
  try {
    const res = await http.get('/space/list')
    if (res.data.code === 200) {
      spaces.value = res.data.data || []
      draw()
    }
  } catch {}
}

function draw() {
  if (!ctx) return
  const c = ctx.canvas
  ctx.clearRect(0, 0, c.width, c.height)
  ctx.fillStyle = '#fff'
  ctx.font = '16px sans-serif'
  ctx.fillText('入口 ↓', 370, 30)
  ctx.strokeStyle = '#999'
  ctx.lineWidth = 3
  ctx.beginPath(); ctx.moveTo(370, 50); ctx.lineTo(370, 80); ctx.stroke()

  for (const pos of spacePositions) {
    const s = spaces.value.find(sp => sp.id === pos.id)
    if (!s) continue
    const occupied = s.status === 1
    const selected = parkingStore.selectedSpaceId === pos.id
    ctx.fillStyle = selected ? '#409eff' : (occupied ? '#f56c6c' : '#67c23a')
    ctx.fillRect(pos.x - 20, pos.y - 15, 40, 30)
    ctx.fillStyle = '#fff'
    ctx.font = '10px sans-serif'
    ctx.fillText(pos.code, pos.x - 16, pos.y + 2)
  }

  if (routePath.value.length > 0) {
    ctx.strokeStyle = '#409eff'; ctx.lineWidth = 2; ctx.beginPath()
    ctx.moveTo(routePath.value[0].x, routePath.value[0].y)
    for (let i = 1; i < routePath.value.length; i++) ctx.lineTo(routePath.value[i].x, routePath.value[i].y)
    ctx.stroke()
  }
}

async function handleClick(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  for (const pos of spacePositions) {
    if (mx >= pos.x - 20 && mx <= pos.x + 20 && my >= pos.y - 15 && my <= pos.y + 15) {
      const s = spaces.value.find(sp => sp.id === pos.id)
      if (s && s.status === 1) return
      parkingStore.setSelectedSpaceId(pos.id)
      fetchRoute(pos.id)
      if (parkingStore.pendingRecordId) {
        try {
          const res = await http.put('/parking/assign-space', {
            recordId: parkingStore.pendingRecordId,
            spaceId: pos.id
          })
          if (res.data.code === 200) {
            ElMessage.success('车位已分配，入场完成')
            parkingStore.clearEntryState()
            await loadSpaces()
            setTimeout(() => router.push('/entry-exit'), 2000)
          } else {
            ElMessage.error(res.data.message)
          }
        } catch {
          ElMessage.error('车位分配失败')
        }
      }
      return
    }
  }
}

async function fetchRoute(spaceId) {
  try {
    const res = await http.post('/parking/route', { spaceId })
    if (res.data.code === 200) {
      routePath.value = res.data.data || []
      draw()
    }
  } catch {}
}

function openAdd() {
  isEdit.value = false
  form.id = null; form.spaceCode = ''; form.area = ''; form.status = 0
  form.xcoordinate = 0; form.ycoordinate = 0
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该车位?', '提示', { type: 'warning' })
    const res = await http.delete(`/space/${row.id}`)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      parkingStore.setSelectedSpaceId(null)
      routePath.value = []
      await loadSpaces()
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {}
}

async function doSave() {
  try {
    let res
    if (isEdit.value) {
      res = await http.put(`/space/${form.id}`, form)
    } else {
      res = await http.post('/space', form)
    }
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功，请检查数据库')
      dialogVisible.value = false
      parkingStore.setSelectedSpaceId(null)
      routePath.value = []
      await loadSpaces()
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  const c = canvasRef.value
  c.width = 800; c.height = 600
  ctx = c.getContext('2d')
  loadSpaces()
})

watch(() => parkingStore.refreshKey, () => {
  loadSpaces()
})

onUnmounted(() => {})
</script>

<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>停车场实时监控 (点击空闲车位规划路径)</span>
        <div style="display:flex;gap:10px;align-items:center">
          <el-button @click="router.push('/entry-exit')">返回入场</el-button>
          <el-button type="primary" @click="openAdd">+ 新增车位</el-button>
        </div>
      </div>
    </template>
    <div v-if="parkingStore.pendingPlateNumber" style="margin-bottom:12px;text-align:center">
      <el-alert type="info" :closable="false" show-icon>
        当前车辆：<strong>{{ parkingStore.pendingPlateNumber }}</strong>，请点击一个空闲车位
      </el-alert>
    </div>
    <div style="text-align:center">
      <canvas ref="canvasRef" @click="handleClick" style="border:1px solid #ccc;background:#1a1a2e;border-radius:4px" />
    </div>
    <div style="margin-top:12px;display:flex;gap:16px;font-size:13px">
      <span><span style="display:inline-block;width:12px;height:12px;background:#67c23a;border-radius:2px;vertical-align:middle;margin-right:4px"></span>空闲</span>
      <span><span style="display:inline-block;width:12px;height:12px;background:#f56c6c;border-radius:2px;vertical-align:middle;margin-right:4px"></span>占用</span>
      <span><span style="display:inline-block;width:12px;height:12px;background:#409eff;border-radius:2px;vertical-align:middle;margin-right:4px"></span>已选车位</span>
      <span v-if="parkingStore.selectedSpaceId">已选: <el-tag size="small">{{ parkingStore.selectedSpaceId }}</el-tag></span>
    </div>

    <div style="margin-top:20px">
      <el-table :data="spaces" stripe size="small">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="spaceCode" label="车位编号" width="120" />
        <el-table-column prop="area" label="区域" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '空闲' : '占用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="xcoordinate" label="X坐标" width="80" />
        <el-table-column prop="ycoordinate" label="Y坐标" width="80" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="doDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑车位' : '新增车位'" width="460px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="车位编号">
          <el-input v-model="form.spaceCode" />
        </el-form-item>
        <el-form-item label="区域">
          <el-input v-model="form.area" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="空闲" :value="0" />
            <el-option label="占用" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="X坐标">
          <el-input-number v-model="form.xcoordinate" :min="0" :max="800" />
        </el-form-item>
        <el-form-item label="Y坐标">
          <el-input-number v-model="form.ycoordinate" :min="0" :max="600" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
