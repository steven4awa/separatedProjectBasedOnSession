<template>
  <div class="about">
    <h1>You've logged in</h1>
    <el-button type="warning" @click="logout()">Logout</el-button>
  </div>
</template>

<style>
@media (min-width: 1024px) {
  .about {
    min-height: 100vh;
    display: flex;
    align-items: center;
  }
}
</style>

<script setup>
import router from '@/router'
function logout(){
  router.push('/')
}
</script>