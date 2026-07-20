<script setup lang="ts">
import {reactive, ref} from "vue";
import {Clock, House} from "@element-plus/icons-vue";
import {post} from "@/net";
import {ElMessage, FormItemRule} from "element-plus";
import router from '@/router/index'

const active = ref(0)
const isEmailValid = ref(false)
const countdown = ref(0)       // 倒计时秒数
let timer: number | undefined = undefined
const form = reactive({
  email: '',
  code: "",
  password: "",
  password_confirmation: "",
})
const sendValidatedEmail = () => {
  console.log("Sending email")
  post('/api/auth/valid-email-reset', {
    email: form.email
  }, (message) => {
    countdown.value = 60
    ElMessage.success(message)

    // 如果之前有未清除的定时器，先清除，防止重复触发
    if (timer) clearInterval(timer)

    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  }, (message) => {
    ElMessage.error(message)
  })
}

const validatePassword = (rule: FormItemRule, value: string, callback: (error?: Error) => void) => {
  if (value === '') {
    callback(new Error('Password is required'));
  } else if (value !== form.password) {
    callback(new Error("Two inputs don't match"));
  } else {
    callback()
  }
}

const rules = reactive({
  email: [
    {
      type: 'email',
      message: 'Please input correct email address',
      trigger: ['blur', 'change'],
      required: true,
    },
  ],
  code: [
    {
      required: true, message: 'Please input your verification code', trigger: ['blur']
    }
  ],
  password:[
    {
      min: 6,
      max: 16,
      message: 'the length of passwords must be between 6 and 16 characters',
      trigger: ['blur', 'change'],
      required: true,
    }
  ],
  password_confirmation: [
    {
      validator: validatePassword,
      trigger: ['blur', 'change']
    }
  ],
})

const startResetPassword = () => {
  post('api/auth/start-reset-password',{
    email: form.email,
    code: form.code
  }, ()=>{
    active.value++
  })
}

const DoResetPassword = () => {
  post('api/auth/reset-password',{
    password: form.password,
    email: form.email
  }, (message)=>{
    ElMessage.info(message)
    router.push("/")
  })
}
const onValidate = (prop: string, isValid: boolean)=>{
  if(prop === 'email'){
    isEmailValid.value = isValid;
  }
}
</script>

<template>
  <div class="gap-x-2" style="display: flex; justify-content: center; align-content: center; flex-direction: column; margin-top: 30px;margin-bottom: 30px" v-if="active===0">
    <div>
      <el-steps style="max-width: 600px" :active="active" finish-status="success" simple>
        <el-step title="Verify your email address" />
        <el-step title="Reset your password" />
      </el-steps>
    </div>
    <div style="display: flex; justify-content: center; align-items: center; flex-direction: column;">
      <h1>
        Reset the password
      </h1>
      <span>Please input your email address first</span>
    </div>

    <el-form :model="form" :rules="rules" @validate="onValidate">
      <el-form-item prop="email">
        <el-input type="text" placeholder="your email" v-model="form.email">
          <template #prefix>
            <el-icon><House /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item prop="code" class="verification-item">
          <el-input placeholder="Enter your verification code" type="email" v-model="form.code">
            <template #prefix>
              <el-icon><Clock /></el-icon>
            </template>
            <template #append>
              <el-button class="verify-btn" :class="{ 'btn-valid': isEmailValid }" :disabled="!isEmailValid" @click="sendValidatedEmail()"  >
                {{ countdown > 0 ? `Resend in ${countdown}s` : 'Get the verification code' }}
              </el-button>
            </template>
          </el-input>
      </el-form-item>
    </el-form>

    <el-button type="danger" @click=startResetPassword>Start to reset your password</el-button>

  </div>

  <div class="gap-x-2" style="display: flex; justify-content: center; align-content: center; flex-direction: column; margin-top: 30px;margin-bottom: 30px" v-else>
    <div>
      <el-steps style="max-width: 600px" :active="active" finish-status="success" simple>
        <el-step title="Verify your email address" />
        <el-step title="Reset your password" />
      </el-steps>
    </div>
    <div style="display: flex; justify-content: center; align-items: center; flex-direction: column;">
      <h1>
        Reset the password
      </h1>
      <span>Please input your new password</span>
    </div>

    <el-form :model="form" :rules="rules" >
      <el-form-item prop="password">
        <el-input type="password" placeholder="Password" v-model="form.password">
          <template #prefix>
            <el-icon><Clock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item prop="password_confirmation"> <!-- Error弹窗按照 el-form-item 的位置 -->
        <el-input type="password" placeholder="Please input the password again" v-model="form.password_confirmation">
          <template #prefix>
            <el-icon><Clock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
    </el-form>

    <el-button type="danger" @click="DoResetPassword">Reset your password</el-button>

  </div>
</template>

<style scoped>
span{
  font-size: 14px;
}

 /* 当邮箱有效时，应用这个样式 */
 .verify-btn.btn-valid {
   background-color: #42b983 !important; /* 你喜欢的绿色 */
   border-color: #42b983 !important;
   color: #fff !important;
 }

/* 如果你想单独控制 disabled 状态下的样式（可选） */
/*.verify-btn:disabled {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #a8abb2;
}*/


</style>