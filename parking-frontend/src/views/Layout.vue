<script setup>
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<template>
  <el-container style="height:100vh">
    <el-aside width="220px" style="background:#304156">
      <div style="color:#fff;text-align:center;padding:20px 0;font-size:18px;font-weight:bold">
        智慧停车
      </div>
      <el-menu
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/entry-exit">
          <span>出入场管理</span>
        </el-menu-item>
        <el-menu-item index="/parking-monitor">
          <span>停车场监控</span>
        </el-menu-item>
        <el-menu-item index="/records">
          <span>停车信息管理</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;border-bottom:1px solid #e6e6e6;display:flex;align-items:center;justify-content:flex-end">
        <el-button text @click="logout">退出登录</el-button>
      </el-header>
      <el-main style="background:#f0f2f5">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
