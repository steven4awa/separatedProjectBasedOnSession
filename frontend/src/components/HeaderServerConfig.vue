<template>
  <!-- 顶部触发与展示容器 -->
  <div class="hover-header-wrapper">
    <!-- 提示条 / 触控区 -->
    <div class="trigger-bar">
      <span> Hover 展开服务器配置 </span>
    </div>

    <!-- 悬浮弹出的配置面板 -->
    <div class="config-panel">
      <el-form :inline="true" size="small">
        <el-form-item label="服务器地址">
          <el-input
              v-model="inputAddress"
              placeholder="ip"
              clearable
              style="width: 240px"
          >
            <template #prefix>http://</template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleConfirm">
            确认替换
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { userStore } from '@/stores/'
import { ElMessage } from 'element-plus'
import axios from "axios";

const serverStore = userStore()

// 本地输入框绑定的临时变量，避免直接修改 store 导致未点击确定就生效
const inputAddress = ref('')

// 初始化及 Store 变化时同步到本地输入框
watchEffect(() => {
  inputAddress.value = serverStore.serverAddress
})

// 点击确认按钮触发
const handleConfirm = () => {
  if (!inputAddress.value.trim()) {
    ElMessage.warning('请输入有效的服务器地址')
    return
  }

  // 更新 Pinia 中的状态（拦截器会自动读取最新状态）
  // serverStore.setServerAddress(inputAddress.value)
  axios.defaults.baseURL = `http://${serverStore.serverAddress}:8080`
  serverStore.serverAddress = inputAddress
  ElMessage.success(`接口基准地址已更新为: http://${serverStore.serverAddress}`)
}
</script>

<style scoped>
.hover-header-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  /* 向上收起面板，仅留出触发条 */
  transform: translateY(-100%);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 当鼠标悬浮在整个区域（包括面板和触发条）时展开 */
.hover-header-wrapper:hover {
  transform: translateY(0);
}

.trigger-bar {
  position: absolute;
  bottom: -24px;
  background-color: #409eff;
  color: #fff;
  font-size: 12px;
  padding: 2px 16px;
  border-radius: 0 0 8px 8px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.config-panel {
  width: 100%;
  background-color: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 12px 24px 4px;
  display: flex;
  justify-content: center;
  border-bottom: 1px solid #ebedf0;
}
</style>