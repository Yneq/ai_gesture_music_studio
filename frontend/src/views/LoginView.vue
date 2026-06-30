<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const auth = useAuthStore()
const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message ?? '登入失敗，請確認帳號密碼。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-slate-900 flex items-center justify-center px-4">
    <div class="w-full max-w-sm space-y-6">
      <div class="text-center">
        <h1 class="text-2xl font-bold text-white">AI Gesture Music Studio</h1>
        <p class="text-slate-400 text-sm mt-1">登入以開始演奏</p>
      </div>

      <form @submit.prevent="submit" class="bg-slate-800 rounded-2xl p-6 space-y-4">
        <div>
          <label class="block text-slate-300 text-sm mb-1">帳號</label>
          <input
            v-model="username"
            type="text"
            required
            autocomplete="username"
            class="w-full bg-slate-700 text-white rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>

        <div>
          <label class="block text-slate-300 text-sm mb-1">密碼</label>
          <input
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            class="w-full bg-slate-700 text-white rounded-lg px-3 py-2 outline-none focus:ring-2 focus:ring-emerald-500"
          />
        </div>

        <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-semibold rounded-lg py-2 transition-colors"
        >
          {{ loading ? '登入中…' : '登入' }}
        </button>
      </form>

      <p class="text-center text-slate-400 text-sm">
        還沒有帳號？
        <router-link to="/register" class="text-emerald-400 hover:underline">立即註冊</router-link>
      </p>
    </div>
  </div>
</template>
