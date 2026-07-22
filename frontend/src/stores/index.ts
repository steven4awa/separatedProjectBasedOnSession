import { reactive } from 'vue'
import { defineStore } from 'pinia'

interface Account {
  id: number
  name: string
  email: string
}

export const userStore = defineStore('store', () => {
  const auth = reactive<{user: Account | null| string}>({
    user: null
  })
  return {auth}
})
