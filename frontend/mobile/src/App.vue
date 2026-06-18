<template>
  <router-view />
  <van-tabbar v-if="showTabbar" route active-color="#1989fa" inactive-color="#646566">
    <van-tabbar-item to="/home" icon="wap-home-o">首页</van-tabbar-item>
    <van-tabbar-item to="/tasks" icon="todo-list-o">
      <span>待作业</span>
      <van-badge :content="pendingCount" :hidden="pendingCount === 0" />
    </van-tabbar-item>
    <van-tabbar-item to="/warnings" icon="warning-o">
      <span>提醒</span>
      <van-badge :content="warningCount" :hidden="warningCount === 0" />
    </van-tabbar-item>
    <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api } from './api'

const route = useRoute()
const showTabbar = computed(() => !!route.meta?.tabbar)
const pendingCount = ref(0)
const warningCount = ref(0)

onMounted(loadCounts)
watch(() => route.fullPath, loadCounts)

async function loadCounts() {
  const user = JSON.parse(localStorage.getItem('m_user') || 'null')
  if (!user) return
  try {
    const list = await api.getPendingOperations(user.id)
    pendingCount.value = list.filter(x => !x.flightStatus || x.flightStatus === 'PENDING').length
  } catch (e) {}
  try {
    const res = await api.queryReminders({ pageNum: 1, pageSize: 1, unreadOnly: true })
    warningCount.value = res.total || 0
  } catch (e) {}
}
</script>
