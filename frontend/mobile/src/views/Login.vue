<template>
  <div class="login">
    <div class="bg"></div>
    <div class="login-box">
      <div class="logo">
        <van-icon name="arrow-up" size="48" color="#fff" />
      </div>
      <h2 class="title">植保飞手作业系统</h2>
      <p class="subtitle">Farm Drone Operation Platform</p>

      <van-form @submit="handleLogin">
        <van-cell-group inset>
          <van-field
            v-model="form.username"
            label="用户名"
            placeholder="请输入用户名"
            :rules="[{ required: true, message: '请填写用户名' }]"
            left-icon="contact"
          />
          <van-field
            v-model="form.password"
            type="password"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请填写密码' }]"
            left-icon="lock"
          />
        </van-cell-group>
        <div style="margin: 24px 16px;">
          <van-button round block type="primary" native-type="submit" :loading="loading">
            登录
          </van-button>
        </div>
      </van-form>

      <div class="tips">
        <p>测试账号：</p>
        <p>飞手: pilot1 / 123456 &nbsp;|&nbsp; 农艺师: agronomist1 / 123456</p>
        <p>管理员: admin / 123456 &nbsp;|&nbsp; 仓管: warehouse1 / 123456</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { api } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'pilot1', password: '123456' })

async function handleLogin() {
  try {
    loading.value = true
    const user = await api.login(form)
    localStorage.setItem('m_user', JSON.stringify(user))
    showToast({ type: 'success', message: '登录成功' })
    router.replace('/home')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: #fff;
}
.bg {
  position: absolute; top: 0; left: 0; right: 0; height: 45%;
  background: linear-gradient(135deg, #1989fa 0%, #4facfe 50%, #00f2fe 100%);
  border-radius: 0 0 50% 50% / 0 0 20% 20%;
  transform: scaleX(1.5);
}
.login-box {
  position: relative;
  z-index: 2;
  padding: 60px 0 40px;
  text-align: center;
}
.logo {
  width: 90px; height: 90px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255,255,255,0.4);
}
.title { color: #fff; font-size: 22px; margin: 8px 0; font-weight: 600; }
.subtitle { color: rgba(255,255,255,0.8); font-size: 12px; margin-bottom: 32px; }
.tips {
  margin-top: 24px;
  padding: 0 20px;
  font-size: 12px;
  color: #969799;
  line-height: 1.8;
}
:deep(.van-cell) { border-radius: 10px !important; }
</style>
