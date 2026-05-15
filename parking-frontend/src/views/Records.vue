<script setup>
import { ref, reactive, onMounted } from 'vue'
import http from '../api'
import { ElMessage } from 'element-plus'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const filter = reactive({ pageNum: 1, pageSize: 10, plateNumber: '', status: null, startTime: '', endTime: '' })

async function load() {
  loading.value = true
  try {
    const params = { ...filter }
    Object.keys(params).forEach(k => { if (params[k] === '' || params[k] === null) delete params[k] })
    const res = await http.get('/record/list', { params })
    if (res.data.code === 200) {
      records.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function doPay(recordId) {
  try {
    const res = await http.put(`/order/pay/${recordId}`)
    if (res.data.code === 200) {
      ElMessage.success('缴费成功')
      await load()
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('缴费失败')
  }
}

function onPageChange(page) {
  filter.pageNum = page
  load()
}

onMounted(load)
</script>

<template>
  <el-card>
    <template #header>停车信息管理</template>
    <el-form :model="filter" inline>
      <el-form-item label="车牌">
        <el-input v-model="filter.plateNumber" placeholder="车牌号" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filter.status" clearable placeholder="全部">
          <el-option label="进行中" :value="0" />
          <el-option label="已完成" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="records" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="plateNumber" label="车牌" width="120" />
      <el-table-column prop="spaceId" label="车位ID" width="80" />
      <el-table-column prop="entryTime" label="入场时间" />
      <el-table-column prop="exitTime" label="离场时间" />
      <el-table-column prop="amount" label="停车费用（元）" width="120">
        <template #default="{ row }">
          {{ row.amount != null ? row.amount : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="停车状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'warning' : 'success'" size="small">
            {{ row.status === 0 ? '进行中' : '已完成' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" type="primary" size="small" @click="doPay(row.id)">模拟缴费</el-button>
          <span v-else style="color:#999">-</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="filter.pageSize"
      @current-change="onPageChange"
    />
  </el-card>
</template>
