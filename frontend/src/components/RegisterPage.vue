<script setup>

import router from "@/router/index.js";
import {Clock, House} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import {ElMessage} from "element-plus";

const form = reactive({
  userName: "",
  password: "",
  password_confirmation: "",
  email: '',
  code: ""
})
const formRef = ref()
const isEmailValid = ref(false)

const onValidate = (prop, isValid)=>{
  if(prop === 'email'){
    isEmailValid.value = isValid;
  }
}

const validateUserName = (rule, value , callback) => {
  if (!value) {
    callback(new Error('Username is required'));
    return
  }

  // 只能输入中文和英文
  const reg = /^[A-Za-z\u4e00-\u9fa5]+$/

  if (!reg.test(value)) {
    callback(new Error('You can only input Chinese and English'))
    return
  }

  callback()
}

const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('Password is required'));
  } else if (value !== form.password) {
     callback(new Error("Two inputs don't match"));
  } else {
    callback()
  }
}

const rules = reactive({
  userName: [
    {
      validator: validateUserName,
      trigger: ['blur', 'change']
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
  ]
})

const registerForm = ()=>{
  formRef.value.validate((valid)=>{
    if(valid){

    } else
      ElMessage.error('Please fill in all fields');
  })
}
</script>

<template>
  <div class="gap-x-2" style="display: flex; justify-content: center; align-content: center; flex-direction: column; margin-top: 30px;margin-bottom: 30px">

    <div style="display: flex; justify-content: center; align-items: center; flex-direction: column;">
      <h3>
        Register a account via this page
      </h3>
    </div>
    <el-form :rules="rules" :model="form" @validate="onValidate" ref="formRef">
      <!--  `prop` 告诉 Element我是负责 form.userName 这个字段的    -->
      <el-form-item prop="userName" style="width: 70%;margin: 20px auto 0;">
        <el-input type="text" placeholder="Username" v-model="form.userName">
          <template #prefix>
            <el-icon><House /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item style="width: 70%;margin: 20px auto 0;" prop="password">
        <el-input type="password" placeholder="Password" v-model="form.password">
          <template #prefix>
            <el-icon><Clock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item style="margin: 20px auto 0; width: 70%;" prop="password_confirmation"> <!-- Error弹窗按照 el-form-item 的位置 -->
        <el-input type="password" placeholder="Please input the password again" v-model="form.password_confirmation">
        <template #prefix>
          <el-icon><Clock /></el-icon>
        </template>
        </el-input>
      </el-form-item>
      <el-form-item style="margin: 20px auto 0; width: 70%" prop="email">
        <el-input placeholder="Enter your email address" type="email" v-model="form.email">
          <template #prefix>
            <el-icon><Clock /></el-icon>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item style="margin: 20px auto 0;width: 70%;" prop="code">
          <div style="display: flex; gap: 3px">
            <el-input placeholder="Enter your verification code" type="email" v-model="form.code" style="width: 70%">
              <template #prefix>
                <el-icon><Clock /></el-icon>
              </template>
            </el-input>
            <el-button style="padding: 5px" class="verify-btn" type="success" :disabled="!isEmailValid">Get the verification code</el-button>
          </div>
      </el-form-item>
    </el-form>



    <el-button style="width: 270px;margin: 20px auto 10px;" type="warning" plain @click="registerForm">Register</el-button>

    <el-divider ><span>have a account already?</span><a @click="router.push('/')">To login page</a></el-divider>

  </div>
</template>

<style scoped>
h3 {
  font-size: 1.2rem;
}

@media (max-width: 440px) {
  .verify-btn {
    font-size: 0;   /* 隐藏原文字 */
  }

  .verify-btn::after {
    content: "code";
    font-size: 14px;
  }
}
</style>