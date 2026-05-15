<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const form = reactive({ username: 'admin', password: 'Admin@2026' })
const loading = ref(false)

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function doLogin() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await http.post('/auth/login', form)
      if (res.data.code === 200) {
        localStorage.setItem('token', res.data.data.token)
        ElMessage.success('登录成功')
        router.push('/')
      } else {
        ElMessage.error(res.data.message || '登录失败')
      }
    } catch {
      ElMessage.error('登录失败')
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-wrapper">
    <el-card class="login-card">
      <template #header><h2 style="text-align:center">智慧停车管理系统</h2></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="doLogin">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-wrapper {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  width: 420px;
}
</style>
