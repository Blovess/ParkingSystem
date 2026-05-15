<script setup>
import { ref, reactive, onMounted } from 'vue'
import http from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const users = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const filter = reactive({ pageNum: 1, pageSize: 10, username: '' })
const form = reactive({ id: null, username: '', password: '', role: 'user' })

async function load() {
  loading.value = true
  try {
    const params = { ...filter }
    if (!params.username) delete params.username
    const res = await http.get('/user/list', { params })
    if (res.data.code === 200) {
      users.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

function openAdd() {
  isEdit.value = false
  form.id = null
  form.username = ''
  form.password = ''
  form.role = 'user'
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  form.id = row.id
  form.username = row.username
  form.password = ''
  form.role = row.role
  dialogVisible.value = true
}

async function doDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该用户?', '提示', { type: 'warning' })
    await http.delete(`/user/${row.id}`)
    ElMessage.success('删除成功')
    await load()
  } catch {}
}

async function doSave() {
  try {
    const data = { username: form.username, role: form.role }
    if (form.password) data.password = form.password
    let res
    if (isEdit.value) {
      res = await http.put(`/user/${form.id}`, data)
    } else {
      res = await http.post('/user', data)
    }
    if (res.data.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      await load()
    } else {
      ElMessage.error(res.data.message)
    }
  } catch {
    ElMessage.error('操作失败')
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
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>用户管理</span>
        <el-button type="primary" @click="openAdd">新增用户</el-button>
      </div>
    </template>
    <el-form :model="filter" inline>
      <el-form-item label="用户名">
        <el-input v-model="filter.username" clearable placeholder="搜索用户名" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色" width="100" />
      <el-table-column prop="createTime" label="创建时间" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="doDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password :placeholder="isEdit ? '留空则不修改' : ''" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="doSave">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>
