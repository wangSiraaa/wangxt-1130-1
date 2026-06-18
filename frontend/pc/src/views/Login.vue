<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon size="48" color="#409EFF"><Promotion /></el-icon>
        <h2>大型农场植保施药审批系统</h2>
        <p>Farm Plant Protection Management System</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" :prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>测试账号：</p>
        <p>管理员: admin / 123456</p>
        <p>农艺师: agronomist1 / 123456</p>
        <p>仓管员: warehouse1 / 123456</p>
        <p>飞手: pilot1 / 123456</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { api } from '../api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  try {
    await formRef.value.validate()
    loading.value = true
    const user = await api.login(form)
    localStorage.setItem('user', JSON.stringify(user))
    ElMessage.success(`欢迎回来，${user.realName}！`)
    router.push('/')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 420px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h2 {
  margin: 12px 0 4px;
  font-size: 20px;
  color: #303133;
}
.login-header p {
  color: #909399;
  font-size: 13px;
}
.login-tips {
  margin-top: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
}
</style>
