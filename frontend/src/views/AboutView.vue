<template>
  <div class="about">
    <h1>You've logged in</h1>
    <el-button type="warning" @click="logout()">Logout</el-button>
  </div>
</template>

<style>
@media (min-width: 1024px) {
  .about {
    display: flex;
    align-items: center;
  }
}
</style>

<script setup>
import router from '@/router'
import {userStore} from "@/stores";
const store = userStore()
function logout(){
  store.auth.user = null
  router.push('/')
}
</script>