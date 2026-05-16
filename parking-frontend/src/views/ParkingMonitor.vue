<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'
import { useParkingStore } from '../stores/parking'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const parkingStore = useParkingStore()
const canvasRef = ref(null)
const spaces = ref([])
const routePath = ref([])
const routeDashedFrom = ref(-1)
const target = ref('manual')
const recommendResult = ref(null)
const recommendPos = computed(() => {
  if (!recommendResult.value) return null
  const id = recommendResult.value.recommendedSpace.id
  return spacePositions.find(p => p.id === id) || null
})
const hoveredSpace = ref(null)   // { spaceCode, zone, type, status, x, y }
const tooltipPos = reactive({ x: 0, y: 0 })
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ id: null, spaceCode: '', area: '', status: 0, xcoordinate: 0, ycoordinate: 0 })
let ctx = null

// ==================== Parking Lot Grid Constants ====================
const W = 34           // Parking spot width (px)
const H = 60           // Parking spot depth (px), H ≈ 1.76W
const PAD = 35         // Canvas inner padding (px)
const TOTAL_GX = 26    // Total horizontal grid units
const CANVAS_W = PAD * 2 + TOTAL_GX * W   // 954
const CANVAS_H = PAD * 2 + 9 * H          // 610

// ==================== Space Position Generation ====================
function generatePositions() {
  const list = []
  let id = 1

  function add(gx, row, code, isCharging) {
    list.push({
      id: id++,
      code,
      gx,
      row,
      isCharging,
      x: PAD + gx * W + W / 2,
      y: PAD + row * H + H / 2
    })
  }

  // ---------- Area A (row 0) ----------
  // Left: 5 charging (gx 0-4) + pillar(gx 5) + 5 regular (gx 6-10)
  ;['A001','A002','A003','A004','A005'].forEach((c, i) => add(i, 0, c, true))
  ;['A006','A007','A008','A009','A010'].forEach((c, i) => add(i + 6, 0, c, false))
  // gx 11=pillar, 12-13=elevator1, 14=pillar
  // Right: 5 regular (gx 15-19) + pillar(gx 20) + 5 regular (gx 21-25)
  ;['A011','A012','A013','A014','A015'].forEach((c, i) => add(i + 15, 0, c, false))
  ;['A016','A017','A018','A019','A020'].forEach((c, i) => add(i + 21, 0, c, false))

  // ---------- Area B (rows 2-3) ----------
  // Left: 2 rows x 5 regular
  ;['B001','B002','B003','B004','B005'].forEach((c, i) => add(i, 2, c, false))
  ;['B006','B007','B008','B009','B010'].forEach((c, i) => add(i, 3, c, false))

  // Center: gx 5-20, 2 rows, pillars at gx 10 and 15
  let bc = 11
  for (let row = 2; row <= 3; row++) {
    for (let gx = 5; gx <= 9; gx++) add(gx, row, `B${String(bc++).padStart(3,'0')}`, false)
    // gx 10 = pillar
    for (let gx = 11; gx <= 14; gx++) add(gx, row, `B${String(bc++).padStart(3,'0')}`, false)
    // gx 15 = pillar
    for (let gx = 16; gx <= 20; gx++) add(gx, row, `B${String(bc++).padStart(3,'0')}`, false)
  }

  // Right: 2 rows x 5 regular
  for (let row = 2; row <= 3; row++) {
    for (let gx = 21; gx <= 25; gx++) add(gx, row, `B${String(bc++).padStart(3,'0')}`, false)
  }

  // ---------- Area C (rows 5-6) ----------
  // Left: 2 rows x 5 regular
  let cc = 1
  for (let row = 5; row <= 6; row++) {
    for (let gx = 0; gx <= 4; gx++) add(gx, row, `C${String(cc++).padStart(3,'0')}`, false)
  }

  // Center: same pattern as B center
  let cc2 = 11
  for (let row = 5; row <= 6; row++) {
    for (let gx = 5; gx <= 9; gx++) add(gx, row, `C${String(cc2++).padStart(3,'0')}`, false)
    for (let gx = 11; gx <= 14; gx++) add(gx, row, `C${String(cc2++).padStart(3,'0')}`, false)
    for (let gx = 16; gx <= 20; gx++) add(gx, row, `C${String(cc2++).padStart(3,'0')}`, false)
  }

  // Right: 2 rows x 5 charging
  let cc3 = 39
  for (let row = 5; row <= 6; row++) {
    for (let gx = 21; gx <= 25; gx++) add(gx, row, `C${String(cc3++).padStart(3,'0')}`, true)
  }

  // ---------- Area D (row 8) ----------
  // Left: 5 regular (gx 0-4) + pillar(gx 5) + 5 regular (gx 6-10)
  ;['D001','D002','D003','D004','D005'].forEach((c, i) => add(i, 8, c, false))
  ;['D006','D007','D008','D009','D010'].forEach((c, i) => add(i + 6, 8, c, false))
  // gx 11=pillar, 12-13=elevator2, 14=pillar
  // Right: 5 regular (gx 15-19) + pillar(gx 20) + 5 regular (gx 21-25)
  ;['D011','D012','D013','D014','D015'].forEach((c, i) => add(i + 15, 8, c, false))
  ;['D016','D017','D018','D019','D020'].forEach((c, i) => add(i + 21, 8, c, false))

  return list
}

const spacePositions = generatePositions()

// ==================== Road Filter (spots replaced by roadway) ====================
const ROAD_CODES = new Set(['B005','B010','C005','C010','B039','B044','C039','C044'])

// ==================== Drawing Primitives ====================

function drawPillar(gx, row, pos) {
  const x = PAD + gx * W
  const y = PAD + row * H + (pos === 'bottom' ? H - W : 0)
  ctx.fillStyle = '#000'
  ctx.fillRect(x, y, W, W)
}

function drawElevator(gx, row, name) {
  const x = PAD + gx * W
  const y = PAD + row * H
  const w = 2 * W
  const h = H
  ctx.strokeStyle = '#000'
  ctx.lineWidth = 1
  ctx.strokeRect(x, y, w, h)
  ctx.fillStyle = '#000'
  ctx.font = 'bold 12px sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(name, x + w / 2, y + h / 2)
}

function drawHorizontalDashed(y) {
  ctx.beginPath()
  ctx.moveTo(PAD, y)
  ctx.lineTo(PAD + TOTAL_GX * W, y)
  ctx.stroke()
}

// ==================== Main Draw ====================

function draw() {
  if (!ctx) return
  const c = ctx.canvas
  ctx.clearRect(0, 0, c.width, c.height)

  // === Background ===
  ctx.fillStyle = '#fafafa'
  ctx.fillRect(0, 0, c.width, c.height)

  // === 1. Outer border (2px solid black) ===
  ctx.strokeStyle = '#000'
  ctx.lineWidth = 2
  ctx.setLineDash([])
  ctx.strokeRect(PAD, PAD, TOTAL_GX * W, 9 * H)

  // === 2. Dashed separation lines (roads & area borders) ===
  ctx.strokeStyle = '#000'
  ctx.lineWidth = 1
  ctx.setLineDash([6, 4])

  // 6 horizontal dashed lines
  drawHorizontalDashed(PAD + H)       // A bottom
  drawHorizontalDashed(PAD + 2 * H)   // B top
  drawHorizontalDashed(PAD + 4 * H)   // B bottom
  drawHorizontalDashed(PAD + 5 * H)   // C top
  drawHorizontalDashed(PAD + 7 * H)   // C bottom
  drawHorizontalDashed(PAD + 8 * H)   // D top

  // B area vertical dashed borders (left & right) + internal roads at gx=4, gx=21
  ctx.beginPath()
  ctx.moveTo(PAD, PAD + 2 * H)
  ctx.lineTo(PAD, PAD + 4 * H)
  ctx.moveTo(PAD + TOTAL_GX * W, PAD + 2 * H)
  ctx.lineTo(PAD + TOTAL_GX * W, PAD + 4 * H)
  // Road at gx=4 (left edge)
  ctx.moveTo(PAD + 4 * W, PAD + 2 * H)
  ctx.lineTo(PAD + 4 * W, PAD + 4 * H)
  // Road at gx=4 (right edge)
  ctx.moveTo(PAD + 5 * W, PAD + 2 * H)
  ctx.lineTo(PAD + 5 * W, PAD + 4 * H)
  // Road at gx=21 (left edge)
  ctx.moveTo(PAD + 21 * W, PAD + 2 * H)
  ctx.lineTo(PAD + 21 * W, PAD + 4 * H)
  // Road at gx=21 (right edge)
  ctx.moveTo(PAD + 22 * W, PAD + 2 * H)
  ctx.lineTo(PAD + 22 * W, PAD + 4 * H)
  ctx.stroke()

  // C area vertical dashed borders (left & right) + internal roads at gx=4, gx=21
  ctx.beginPath()
  ctx.moveTo(PAD, PAD + 5 * H)
  ctx.lineTo(PAD, PAD + 7 * H)
  ctx.moveTo(PAD + TOTAL_GX * W, PAD + 5 * H)
  ctx.lineTo(PAD + TOTAL_GX * W, PAD + 7 * H)
  // Road at gx=4 (left edge)
  ctx.moveTo(PAD + 4 * W, PAD + 5 * H)
  ctx.lineTo(PAD + 4 * W, PAD + 7 * H)
  // Road at gx=4 (right edge)
  ctx.moveTo(PAD + 5 * W, PAD + 5 * H)
  ctx.lineTo(PAD + 5 * W, PAD + 7 * H)
  // Road at gx=21 (left edge)
  ctx.moveTo(PAD + 21 * W, PAD + 5 * H)
  ctx.lineTo(PAD + 21 * W, PAD + 7 * H)
  // Road at gx=21 (right edge)
  ctx.moveTo(PAD + 22 * W, PAD + 5 * H)
  ctx.lineTo(PAD + 22 * W, PAD + 7 * H)
  ctx.stroke()

  ctx.setLineDash([])

  // === 3. Pillars ===
  // A row (bottom half)
  drawPillar(5, 0, 'bottom')
  drawPillar(11, 0, 'bottom')
  drawPillar(14, 0, 'bottom')
  drawPillar(20, 0, 'bottom')

  // B top row (bottom half)
  drawPillar(10, 2, 'bottom')
  drawPillar(15, 2, 'bottom')

  // B bottom row (top half)
  drawPillar(10, 3, 'top')
  drawPillar(15, 3, 'top')

  // C top row (bottom half)
  drawPillar(10, 5, 'bottom')
  drawPillar(15, 5, 'bottom')

  // C bottom row (top half)
  drawPillar(10, 6, 'top')
  drawPillar(15, 6, 'top')

  // D row (top half)
  drawPillar(5, 8, 'top')
  drawPillar(11, 8, 'top')
  drawPillar(14, 8, 'top')
  drawPillar(20, 8, 'top')

  // === 4. Elevators ===
  ctx.lineWidth = 1
  drawElevator(12, 0, '1号电梯')
  drawElevator(12, 8, '2号电梯')

  // === 5. Road direction labels ===
  ctx.fillStyle = '#000'
  ctx.textBaseline = 'middle'
  ctx.font = 'bold 14px sans-serif'
  ctx.textAlign = 'right'
  ctx.fillText('出口 →', PAD + TOTAL_GX * W - 8, PAD + 1 * H + H / 2)
  ctx.textAlign = 'left'
  ctx.fillText('← 入口', PAD + 8, PAD + 4 * H + H / 2)

  // === 6. Area labels ===
  ctx.font = 'bold 12px sans-serif'
  ctx.textAlign = 'left'
  ctx.textBaseline = 'top'
  ctx.fillText('A区', PAD + 4, PAD + 4)
  ctx.fillText('B区', PAD + 4, PAD + 2 * H + 4)
  ctx.fillText('C区', PAD + 4, PAD + 5 * H + 4)
  ctx.fillText('D区', PAD + 4, PAD + 8 * H + 4)

  // === 7. Parking spots (skip codes replaced by roadway) ===
  for (const pos of spacePositions) {
    if (ROAD_CODES.has(pos.code)) continue
    const s = spaces.value.find(sp => sp.id === pos.id)
    const occupied = s ? s.status === 1 : false
    const exists = !!s
    const selected = parkingStore.selectedSpaceId === pos.id
    const recommended = recommendResult.value && recommendResult.value.recommendedSpace.id === pos.id

    const mg = 2
    const sx = PAD + pos.gx * W + mg
    const sy = PAD + pos.row * H + mg
    const sw = W - 2 * mg
    const sh = H - 2 * mg

    // Fill by status
    if (selected) {
      ctx.fillStyle = '#409eff'
    } else if (recommended) {
      ctx.fillStyle = '#f5d742'   // golden yellow for recommended
    } else if (!exists) {
      ctx.fillStyle = '#e0e0e0'   // unknown (not yet in DB)
    } else if (occupied) {
      ctx.fillStyle = '#f56c6c'   // occupied
    } else {
      ctx.fillStyle = '#67c23a'   // free
    }
    ctx.fillRect(sx, sy, sw, sh)

    // Border: blue for charging, dark for regular
    ctx.strokeStyle = pos.isCharging ? '#0066CC' : '#444'
    ctx.lineWidth = pos.isCharging ? 1.5 : 1
    ctx.strokeRect(sx, sy, sw, sh)

    // Code text
    ctx.fillStyle = '#fff'
    ctx.font = '7px sans-serif'
    ctx.textAlign = 'left'
    ctx.textBaseline = 'top'
    ctx.fillText(pos.code, sx + 1, sy + 1)

    // Recommended label
    if (recommended) {
      ctx.fillStyle = '#d48806'
      ctx.font = 'bold 8px sans-serif'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'bottom'
      ctx.fillText('系统推荐', sx + sw / 2, sy + sh - 1)
    }
  }

  // === 8. Route path (solid for road network, dashed for last segment to spot) ===
  if (routePath.value.length > 0) {
    const split = routeDashedFrom.value >= 0 ? routeDashedFrom.value : routePath.value.length - 1

    // Solid: road network path
    ctx.strokeStyle = '#409eff'
    ctx.lineWidth = 2.5
    ctx.setLineDash([])
    ctx.beginPath()
    ctx.moveTo(routePath.value[0].x, routePath.value[0].y)
    for (let i = 1; i <= split; i++) {
      ctx.lineTo(routePath.value[i].x, routePath.value[i].y)
    }
    ctx.stroke()

    // Dashed: final segment from road to target spot
    if (split < routePath.value.length - 1) {
      ctx.setLineDash([6, 4])
      ctx.beginPath()
      ctx.moveTo(routePath.value[split].x, routePath.value[split].y)
      ctx.lineTo(routePath.value[split + 1].x, routePath.value[split + 1].y)
      ctx.stroke()
    }
  }
}

// ==================== Data Loading ====================

async function loadSpaces() {
  try {
    const res = await http.get('/space/list')
    if (res.data.code === 200) {
      spaces.value = res.data.data || []
      draw()
    }
  } catch {}
}

// ==================== Interaction ====================

function hitTest(mx, my) {
  for (const pos of spacePositions) {
    if (ROAD_CODES.has(pos.code)) continue
    const left = PAD + pos.gx * W
    const top = PAD + pos.row * H
    if (mx >= left && mx <= left + W && my >= top && my <= top + H) {
      return pos
    }
  }
  return null
}

async function handleClick(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  const hit = hitTest(mx, my)
  if (!hit) return

  const s = spaces.value.find(sp => sp.id === hit.id)
  if (s && s.status === 1) return

  // Clear recommendation when user picks manually
  if (recommendResult.value) {
    recommendResult.value = null
  }

  parkingStore.setSelectedSpaceId(hit.id)
  fetchRoute(hit.id)
  if (parkingStore.pendingRecordId) {
    try {
      const res = await http.put('/parking/assign-space', {
        recordId: parkingStore.pendingRecordId,
        spaceId: hit.id
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
}

// ==================== Hover Tooltip ====================

function handleHover(e) {
  const rect = canvasRef.value.getBoundingClientRect()
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  const hit = hitTest(mx, my)
  if (!hit) {
    hoveredSpace.value = null
    return
  }
  const s = spaces.value.find(sp => sp.id === hit.id)
  if (!s) {
    hoveredSpace.value = null
    return
  }
  hoveredSpace.value = s
  tooltipPos.x = e.clientX
  tooltipPos.y = e.clientY
}

function handleMouseLeave() {
  hoveredSpace.value = null
}

async function fetchRoute(spaceId) {
  const space = spaces.value.find(s => s.id === spaceId)
  if (!space || space.xcoordinate == null) return
  try {
    const res = await fetch('http://localhost:5000/api/path-plan', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ target_x: space.xcoordinate, target_y: space.ycoordinate })
    })
    const json = await res.json()
    if (json.code === 200) {
      const path = json.data.path || []
      // Append the target spot coordinate so the path ends at the actual spot
      const last = path.length > 0 ? path[path.length - 1] : null
      if (!last || last.x !== space.xcoordinate || last.y !== space.ycoordinate) {
        routeDashedFrom.value = path.length - 1  // last road point
        path.push({ x: space.xcoordinate, y: space.ycoordinate })
      } else {
        routeDashedFrom.value = -1
      }
      routePath.value = path
      draw()
    }
  } catch {}
}

// ==================== Recommend ====================

async function fetchRecommend() {
  const plate = parkingStore.pendingPlateNumber
  if (!plate) {
    ElMessage.warning('请先入场登记车辆')
    target.value = 'manual'
    return
  }
  if (!target.value || target.value === 'manual') return
  try {
    const res = await http.post('/parking/recommend', {
      plateNumber: plate,
      target: target.value,
    })
    if (res.data.code === 200) {
      const data = res.data.data
      recommendResult.value = data
      // Draw path from recommended path
      if (data.path && data.path.length > 0) {
        const path = data.path
        const last = path[path.length - 1]
        // Check if last point overlaps the spot coordinate
        const rc = data.recommendedSpace
        if (last.x !== rc.xcoordinate || last.y !== rc.ycoordinate) {
          routeDashedFrom.value = path.length - 1
          path.push({ x: rc.xcoordinate, y: rc.ycoordinate })
        } else {
          routeDashedFrom.value = -1
        }
        routePath.value = path
      }
      draw()
    } else {
      ElMessage.error(res.data.message || '推荐失败')
    }
  } catch {
    ElMessage.error('推荐请求失败')
  }
}

watch(target, (val) => {
  if (val && val !== 'manual') {
    fetchRecommend()
  } else {
    recommendResult.value = null
  }
})

// ==================== CRUD Dialog ====================

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
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
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

// ==================== Lifecycle ====================

onMounted(() => {
  const c = canvasRef.value
  c.width = CANVAS_W
  c.height = CANVAS_H
  ctx = c.getContext('2d')
  loadSpaces()
})

watch(() => parkingStore.refreshKey, () => {
  loadSpaces()
})

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

    <!-- Target selector -->
    <div style="margin-bottom:12px;display:flex;align-items:center;gap:12px;flex-wrap:wrap">
      <span style="font-weight:bold;font-size:14px">你想停在哪？</span>
      <el-select v-model="target" placeholder="请选择" style="width:240px" clearable>
        <el-option label="1号电梯附近" value="1号电梯" />
        <el-option label="2号电梯附近" value="2号电梯" />
        <el-option label="A区充电车位" value="A区充电" />
        <el-option label="C区充电车位" value="C区充电" />
        <el-option label="自选车位（我自己点地图）" value="manual" />
      </el-select>
      <span v-if="parkingStore.pendingPlateNumber" style="color:#909399;font-size:13px">
        当前车辆：<strong>{{ parkingStore.pendingPlateNumber }}</strong>
      </span>
    </div>

    <div v-if="recommendResult" class="recommend-bar">
      <span style="font-weight:bold;color:#d48806">系统推荐：{{ recommendResult.recommendedSpace.spaceCode }}</span>
      <span v-if="recommendResult.details"> ｜ 距离成本：{{ (recommendResult.details.distanceCost * 100).toFixed(0) }}% ｜ 拥堵成本：{{ (recommendResult.details.congestionCost * 100).toFixed(0) }}% ｜
        <el-tag size="small" type="warning">综合评分 {{ ((1 - recommendResult.details.compositeCost) * 100).toFixed(0) }}分</el-tag>
      </span>
      <span style="margin-left:auto;font-size:13px;color:#909399">目标：{{ recommendResult.details.target }}</span>
    </div>

    <div v-if="parkingStore.pendingPlateNumber && target === 'manual'" style="margin-bottom:12px;text-align:center">
      <el-alert type="info" :closable="false" show-icon>
        当前车辆：<strong>{{ parkingStore.pendingPlateNumber }}</strong>，请点击一个空闲车位
      </el-alert>
    </div>

    <div style="text-align:center">
      <div style="position:relative;display:inline-block">
        <canvas
          ref="canvasRef"
          @click="handleClick"
          @mousemove="handleHover"
          @mouseleave="handleMouseLeave"
          style="border:1px solid #ccc;border-radius:4px;background:#fafafa;display:block"
        />
        <!-- Flashing border overlay for recommended space -->
        <div
          v-if="recommendPos"
          class="recommend-flash"
          :style="{
            position: 'absolute',
            left: (PAD + recommendPos.gx * W) + 'px',
            top: (PAD + recommendPos.row * H) + 'px',
            width: W + 'px',
            height: H + 'px',
          }"
        ></div>
        <div
          v-if="hoveredSpace"
          :style="{
            position: 'fixed',
            left: tooltipPos.x + 12 + 'px',
            top: tooltipPos.y + 12 + 'px',
            background: '#fff',
            border: '1px solid #d9d9d9',
            borderRadius: '6px',
            padding: '8px 12px',
            fontSize: '13px',
            lineHeight: '1.8',
            boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
            zIndex: 2000,
            pointerEvents: 'none',
            whiteSpace: 'nowrap',
          }"
        >
          <div><strong>{{ hoveredSpace.spaceCode }}</strong></div>
          <div>区域：{{ hoveredSpace.zone || '-' }}</div>
          <div>类型：{{ hoveredSpace.type === 'CHARGING' ? '充电车位' : '普通车位' }}</div>
          <div>状态：<span :style="{ color: hoveredSpace.status === 0 ? '#67c23a' : '#f56c6c' }">{{ hoveredSpace.status === 0 ? '空闲' : '已占用' }}</span></div>
        </div>
      </div>
    </div>

    <div style="margin-top:12px;display:flex;gap:16px;font-size:13px;flex-wrap:wrap">
      <span><span style="display:inline-block;width:12px;height:12px;background:#67c23a;border-radius:2px;vertical-align:middle;margin-right:4px"></span>空闲</span>
      <span><span style="display:inline-block;width:12px;height:12px;background:#f56c6c;border-radius:2px;vertical-align:middle;margin-right:4px"></span>占用</span>
      <span><span style="display:inline-block;width:12px;height:12px;background:#409eff;border-radius:2px;vertical-align:middle;margin-right:4px"></span>已选车位</span>
      <span><span style="display:inline-block;width:12px;height:12px;background:#e0e0e0;border-radius:2px;vertical-align:middle;margin-right:4px;border:1px solid #aaa"></span>未录入</span>
      <span><span style="display:inline-block;width:12px;height:12px;border:2px solid #0066CC;border-radius:2px;vertical-align:middle;margin-right:4px"></span>充电桩车位</span>
      <span><span style="display:inline-block;width:12px;height:12px;background:#f5d742;border-radius:2px;vertical-align:middle;margin-right:4px"></span>系统推荐</span>
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
          <el-input-number v-model="form.xcoordinate" :min="0" :max="CANVAS_W" />
        </el-form-item>
        <el-form-item label="Y坐标">
          <el-input-number v-model="form.ycoordinate" :min="0" :max="CANVAS_H" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doSave">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.recommend-bar {
  margin-bottom: 12px;
  padding: 8px 14px;
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.recommend-flash {
  border: 3px solid #f5d742;
  border-radius: 3px;
  pointer-events: none;
  box-sizing: border-box;
  animation: flash-border 1.2s ease-in-out infinite;
  z-index: 10;
}
@keyframes flash-border {
  0%, 100% { border-color: #f5d742; opacity: 1; }
  50% { border-color: #e6a23c; opacity: 0.5; }
}
</style>
