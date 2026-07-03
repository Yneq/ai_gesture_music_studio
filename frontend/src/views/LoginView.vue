<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import apiClient from '../services/api'

const auth = useAuthStore()
const router = useRouter()

// ── Login ─────────────────────────────────────────────────────────────────────
const username = ref('')
const password = ref('')
const loginError = ref('')
const loginLoading = ref(false)
const showPassword = ref(false)

// ── Google Sign-In ─────────────────────────────────────────────────────────────
const googleBtnRef = ref(null)
const googleReady  = ref(false)
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''
const isLocalhost = ['localhost', '127.0.0.1'].includes(window.location.hostname)

async function handleGoogleCredential({ credential }) {
  loginError.value = ''
  loginLoading.value = true
  try {
    await auth.loginWithGoogle(credential)
    router.push('/')
  } catch (e) {
    loginError.value = e.response?.data?.message ?? 'Google 登入失敗，請再試一次。'
  } finally {
    loginLoading.value = false
  }
}

function initGoogle() {
  if (!GOOGLE_CLIENT_ID || !window.google) return
  window.google.accounts.id.initialize({
    client_id: GOOGLE_CLIENT_ID,
    callback: handleGoogleCredential,
    auto_select: false,
    cancel_on_tap_outside: true,
  })
  if (googleBtnRef.value) {
    const w = googleBtnRef.value.getBoundingClientRect().width || 280
    window.google.accounts.id.renderButton(googleBtnRef.value, {
      theme: 'outline', size: 'large', shape: 'rectangular',
      width: Math.min(Math.floor(w), 400),
    })
  }
  googleReady.value = true
}

async function submitLogin() {
  loginError.value = ''
  loginLoading.value = true
  try {
    await auth.login(username.value, password.value)
    router.push('/')
  } catch (e) {
    loginError.value = e.response?.data?.message ?? '登入失敗，請確認帳號密碼。'
  } finally {
    loginLoading.value = false
  }
}

function fillTestAccount() {
  username.value = 'guest'
  password.value = 'guest123'
}

// ── Register modal ────────────────────────────────────────────────────────────
const showRegister = ref(false)
const regUsername = ref('')
const regEmail = ref('')
const regPassword = ref('')
const regError = ref('')
const regLoading = ref(false)
const usernameStatus = ref(null)  // null | 'checking' | 'available' | 'taken'
let checkTimer = null

watch(regUsername, (val) => {
  usernameStatus.value = null
  clearTimeout(checkTimer)
  if (val.length < 3) return
  usernameStatus.value = 'checking'
  checkTimer = setTimeout(async () => {
    try {
      const { data } = await apiClient.get('/auth/check-username', { params: { username: val } })
      usernameStatus.value = data.taken ? 'taken' : 'available'
    } catch {
      usernameStatus.value = null
    }
  }, 500)
})

function openRegister() {
  regUsername.value = ''
  regEmail.value = ''
  regPassword.value = ''
  regError.value = ''
  usernameStatus.value = null
  showRegister.value = true
}

async function submitRegister() {
  if (usernameStatus.value === 'taken') return
  regError.value = ''
  regLoading.value = true
  try {
    await auth.register(regUsername.value, regPassword.value, regEmail.value || null)
    await auth.login(regUsername.value, regPassword.value)
    router.push('/')
  } catch (e) {
    regError.value = e.response?.data?.message ?? '註冊失敗，請確認輸入內容。'
  } finally {
    regLoading.value = false
  }
}

// ── Music: Caravan (Whiplash) ─────────────────────────────────────────────────
const audioEl = ref(null)
const musicOn = ref(false)

function tryAutoplay() {
  const a = audioEl.value
  if (!a || musicOn.value) return   // already playing — don't restart
  a.volume = 0.6
  a.currentTime = 119
  a.play().then(() => { musicOn.value = true }).catch(() => {})
}

function toggleMusic() {
  const a = audioEl.value
  if (!a) return
  if (musicOn.value) {
    a.pause()
    musicOn.value = false
  } else {
    a.play().catch(() => {})
    musicOn.value = true
  }
}

onMounted(() => {
  setTimeout(tryAutoplay, 400)
  // Load Google Identity Services
  if (GOOGLE_CLIENT_ID) {
    if (window.google) {
      initGoogle()
    } else {
      const script = document.createElement('script')
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      script.onload = initGoogle
      document.head.appendChild(script)
    }
  }
})
onUnmounted(() => { audioEl.value?.pause() })
</script>

<template>
  <audio ref="audioEl" src="/audio/caravan.mp3" loop preload="auto"
    @loadedmetadata="audioEl.currentTime = 119"></audio>

  <div class="relative min-h-screen overflow-hidden flex items-center justify-center"
    @click.once="tryAutoplay">

    <!-- Baroque palace orchestra background -->
    <div class="absolute inset-0 bg-cover bg-center bg-no-repeat"
      style="background-image: url('https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?auto=format&fit=crop&w=1920&q=80')">
    </div>
    <div class="absolute inset-0"
      style="background: linear-gradient(to bottom, rgba(18,8,0,0.72) 0%, rgba(28,14,2,0.55) 45%, rgba(18,8,0,0.80) 100%)">
    </div>
    <div class="absolute inset-0"
      style="background: radial-gradient(ellipse 70% 60% at 50% 45%, transparent 0%, rgba(8,3,0,0.72) 100%)">
    </div>
    <div class="absolute inset-x-0 bottom-0 h-48 pointer-events-none" style="
      background: repeating-linear-gradient(
        to bottom, transparent 0px, transparent 10px,
        rgba(212,175,55,0.04) 10px, rgba(212,175,55,0.04) 11px);
    "></div>

    <!-- Music toggle -->
    <button @click.stop="toggleMusic"
      class="music-btn absolute top-5 right-5 z-20 flex items-center justify-center w-10 h-10 rounded-full"
      :title="musicOn ? '關閉音樂' : '播放音樂 — Caravan'"
      style="background:rgba(12,5,0,0.65); border:1px solid rgba(212,175,55,0.4); backdrop-filter:blur(8px);">
      <span v-if="musicOn" class="absolute inset-0 rounded-full animate-ping opacity-20"
        style="background:rgba(212,175,55,0.5)"></span>
      <span class="text-base relative z-10" style="color:rgba(212,175,55,0.9)">
        {{ musicOn ? '🔊' : '🎵' }}
      </span>
    </button>

    <!-- ── Login panel ─────────────────────────────────────────────────────── -->
    <div class="relative z-10 w-full max-w-xs px-5 flex flex-col gap-6">

      <!-- Title -->
      <div class="text-center select-none">
        <div class="flex items-center justify-center gap-2 mb-4">
          <div class="h-px flex-1" style="background:linear-gradient(to right,transparent,rgba(212,175,55,0.6))"></div>
          <span style="color:rgba(212,175,55,0.8);font-size:18px">✦</span>
          <div class="h-px flex-1" style="background:linear-gradient(to left,transparent,rgba(212,175,55,0.6))"></div>
        </div>
        <h1 style="font-family:'Georgia',serif;font-size:2.6rem;font-weight:700;letter-spacing:0.12em;color:#FFF5D6;text-shadow:0 0 40px rgba(212,175,55,0.5),0 2px 6px rgba(0,0,0,0.9);line-height:1.15">
          MAESTRO
        </h1>
        <p style="font-family:'Georgia',serif;font-size:0.65rem;letter-spacing:0.32em;color:rgba(212,175,55,0.65);text-transform:uppercase;margin-top:6px">
          AI Gesture Music Studio
        </p>
        <div class="flex items-center justify-center gap-2 mt-4">
          <div class="h-px flex-1" style="background:linear-gradient(to right,transparent,rgba(212,175,55,0.6))"></div>
          <span style="color:rgba(212,175,55,0.8)">❖</span>
          <div class="h-px flex-1" style="background:linear-gradient(to left,transparent,rgba(212,175,55,0.6))"></div>
        </div>
      </div>

      <!-- Login card -->
      <form @submit.prevent="submitLogin" class="relative rounded-xl p-6 space-y-4" style="
        background:rgba(12,5,0,0.72); backdrop-filter:blur(22px);
        border:1px solid rgba(212,175,55,0.3);
        box-shadow:0 0 0 1px rgba(212,175,55,0.08) inset,0 8px 40px rgba(0,0,0,0.7);">
        <span class="absolute top-2.5 left-2.5  text-[10px]" style="color:rgba(212,175,55,0.45)">✦</span>
        <span class="absolute top-2.5 right-2.5 text-[10px]" style="color:rgba(212,175,55,0.45)">✦</span>
        <span class="absolute bottom-2.5 left-2.5  text-[10px]" style="color:rgba(212,175,55,0.45)">✦</span>
        <span class="absolute bottom-2.5 right-2.5 text-[10px]" style="color:rgba(212,175,55,0.45)">✦</span>

        <div class="space-y-1.5">
          <label style="display:block;color:rgba(212,175,55,0.55);font-size:0.65rem;letter-spacing:0.25em;text-transform:uppercase">帳號</label>
          <input v-model="username" type="text" required autocomplete="username" placeholder="username"
            class="w-full rounded-lg px-4 py-2.5 text-sm outline-none transition-all"
            style="background:rgba(255,240,200,0.05);border:1px solid rgba(212,175,55,0.22);color:#FFF5D6;"
            @focus="e=>{e.target.style.borderColor='rgba(212,175,55,0.6)';e.target.style.boxShadow='0 0 14px rgba(212,175,55,0.12)'}"
            @blur=" e=>{e.target.style.borderColor='rgba(212,175,55,0.22)';e.target.style.boxShadow=''}" />
        </div>

        <div class="space-y-1.5">
          <label style="display:block;color:rgba(212,175,55,0.55);font-size:0.65rem;letter-spacing:0.25em;text-transform:uppercase">密碼</label>
          <div class="relative">
            <input v-model="password" :type="showPassword ? 'text' : 'password'" required autocomplete="current-password" placeholder="password"
              class="w-full rounded-lg px-4 py-2.5 pr-10 text-sm outline-none transition-all"
              style="background:rgba(255,240,200,0.05);border:1px solid rgba(212,175,55,0.22);color:#FFF5D6;"
              @focus="e=>{e.target.style.borderColor='rgba(212,175,55,0.6)';e.target.style.boxShadow='0 0 14px rgba(212,175,55,0.12)'}"
              @blur=" e=>{e.target.style.borderColor='rgba(212,175,55,0.22)';e.target.style.boxShadow=''}" />
            <button type="button" @click="showPassword = !showPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-sm transition-colors select-none"
              :style="showPassword ? 'color:rgba(212,175,55,0.9)' : 'color:rgba(212,175,55,0.35)'">
              {{ showPassword ? '🙈' : '👁' }}
            </button>
          </div>
        </div>

        <p v-if="loginError" class="text-red-400 text-xs">{{ loginError }}</p>

        <button type="submit" :disabled="loginLoading"
          class="w-full rounded-lg py-2.5 text-sm font-bold tracking-widest uppercase disabled:opacity-50 transition-all"
          style="background:linear-gradient(135deg,#7A5C0E 0%,#D4AF37 50%,#7A5C0E 100%);color:#1A0800;box-shadow:0 2px 20px rgba(212,175,55,0.35);"
          @mouseenter="e=>{if(!loginLoading)e.target.style.filter='brightness(1.12)'}"
          @mouseleave="e=>e.target.style.filter=''">
          {{ loginLoading ? '載入中…' : '進場演奏' }}
        </button>

        <!-- Test account quick-fill -->
        <button type="button" @click="fillTestAccount"
          class="w-full rounded-lg py-2 text-xs tracking-wider transition-all"
          style="background:rgba(212,175,55,0.07);border:1px solid rgba(212,175,55,0.18);color:rgba(212,175,55,0.55);"
          @mouseenter="e=>{e.target.style.borderColor='rgba(212,175,55,0.4)';e.target.style.color='rgba(212,175,56,0.9)'}"
          @mouseleave="e=>{e.target.style.borderColor='rgba(212,175,55,0.18)';e.target.style.color='rgba(212,175,55,0.55)'}">
          🎭 使用測試帳號（guest / guest123）
        </button>
      </form>

      <!-- Google Sign-In (only on localhost — private IPs are rejected by Google OAuth) -->
      <div v-if="GOOGLE_CLIENT_ID && isLocalhost" class="flex flex-col items-center gap-3">
        <div class="flex items-center gap-2 w-full">
          <div class="h-px flex-1" style="background:rgba(212,175,55,0.2)"></div>
          <span style="color:rgba(212,175,55,0.35);font-size:0.65rem;letter-spacing:0.15em">OR</span>
          <div class="h-px flex-1" style="background:rgba(212,175,55,0.2)"></div>
        </div>

        <!-- Google button: gold visual layer + transparent renderButton on top -->
        <div class="relative w-full rounded-xl overflow-hidden google-gold-btn"
          style="border:1px solid rgba(212,175,55,0.32)">

          <!-- Gold visual (pointer-events:none — purely decorative) -->
          <div class="pointer-events-none flex items-center justify-center gap-3 py-3"
            style="background:rgba(255,255,255,0.04);backdrop-filter:blur(8px)">
            <svg viewBox="0 0 24 24" class="w-5 h-5 shrink-0" xmlns="http://www.w3.org/2000/svg">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
            </svg>
            <span v-if="googleReady" style="color:rgba(255,245,214,0.82);font-size:0.875rem;font-weight:600;letter-spacing:0.04em">
              使用 Google 繼續
            </span>
            <span v-else style="color:rgba(212,175,55,0.3);font-size:0.75rem">載入中…</span>
          </div>

          <!-- Google renderButton: absolute overlay, opacity ~0 so invisible but captures clicks -->
          <div ref="googleBtnRef"
            class="absolute inset-0"
            style="opacity:0.001;overflow:hidden"></div>
        </div>
      </div>

      <p class="text-center text-xs" style="color:rgba(212,175,55,0.3);letter-spacing:0.05em">
        還沒有帳號？
        <button type="button" @click="openRegister"
          style="color:rgba(212,175,55,0.65);"
          @mouseenter="e=>e.target.style.color='rgba(212,175,55,1)'"
          @mouseleave="e=>e.target.style.color='rgba(212,175,55,0.65)'">
          立即註冊
        </button>
      </p>
    </div>

    <!-- ── Register modal ──────────────────────────────────────────────────── -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showRegister"
          class="fixed inset-0 z-50 flex items-center justify-center px-5"
          style="background:rgba(8,3,0,0.75);backdrop-filter:blur(4px)"
          @click.self="showRegister = false">

          <div class="relative w-full max-w-xs rounded-2xl p-7 space-y-4" style="
            background:rgba(12,5,0,0.92); backdrop-filter:blur(28px);
            border:1px solid rgba(212,175,55,0.35);
            box-shadow:0 0 0 1px rgba(212,175,55,0.08) inset,0 12px 60px rgba(0,0,0,0.8);">

            <!-- Close -->
            <button @click="showRegister = false"
              class="absolute top-3 right-4 text-xl leading-none transition-colors"
              style="color:rgba(212,175,55,0.4);"
              @mouseenter="e=>e.target.style.color='rgba(212,175,55,0.9)'"
              @mouseleave="e=>e.target.style.color='rgba(212,175,55,0.4)'">×</button>

            <span class="absolute top-2.5 left-2.5  text-[10px]" style="color:rgba(212,175,55,0.35)">✦</span>
            <span class="absolute bottom-2.5 left-2.5  text-[10px]" style="color:rgba(212,175,55,0.35)">✦</span>
            <span class="absolute bottom-2.5 right-2.5 text-[10px]" style="color:rgba(212,175,55,0.35)">✦</span>

            <div class="text-center pb-1">
              <p style="font-family:'Georgia',serif;font-size:1.1rem;font-weight:700;letter-spacing:0.1em;color:#FFF5D6;text-shadow:0 0 20px rgba(212,175,55,0.3)">
                建立帳號
              </p>
              <div class="flex items-center justify-center gap-2 mt-2">
                <div class="h-px flex-1" style="background:linear-gradient(to right,transparent,rgba(212,175,55,0.5))"></div>
                <span style="color:rgba(212,175,55,0.6);font-size:11px">❖</span>
                <div class="h-px flex-1" style="background:linear-gradient(to left,transparent,rgba(212,175,55,0.5))"></div>
              </div>
            </div>

            <form @submit.prevent="submitRegister" class="space-y-3">
              <!-- Username -->
              <div class="space-y-1">
                <label style="display:block;color:rgba(212,175,55,0.55);font-size:0.62rem;letter-spacing:0.25em;text-transform:uppercase">帳號（3–50 字元）</label>
                <div class="relative">
                  <input v-model="regUsername" type="text" required minlength="3" maxlength="50" autocomplete="username"
                    placeholder="username"
                    class="w-full rounded-lg px-4 py-2.5 text-sm outline-none transition-all pr-20"
                    style="background:rgba(255,240,200,0.05);border:1px solid rgba(212,175,55,0.22);color:#FFF5D6;"
                    @focus="e=>{e.target.style.borderColor='rgba(212,175,55,0.6)';e.target.style.boxShadow='0 0 14px rgba(212,175,55,0.12)'}"
                    @blur=" e=>{e.target.style.borderColor='rgba(212,175,55,0.22)';e.target.style.boxShadow=''}" />
                  <span v-if="usernameStatus==='checking'" class="absolute right-3 top-1/2 -translate-y-1/2 text-xs" style="color:rgba(212,175,55,0.5)">檢查中…</span>
                  <span v-else-if="usernameStatus==='available'" class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-emerald-400">✓ 可用</span>
                  <span v-else-if="usernameStatus==='taken'" class="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-semibold text-red-400">✗ 已使用</span>
                </div>
              </div>

              <!-- Email -->
              <div class="space-y-1">
                <label style="display:block;color:rgba(212,175,55,0.55);font-size:0.62rem;letter-spacing:0.25em;text-transform:uppercase">Email（選填）</label>
                <input v-model="regEmail" type="email" autocomplete="email" placeholder="email@example.com"
                  class="w-full rounded-lg px-4 py-2.5 text-sm outline-none transition-all"
                  style="background:rgba(255,240,200,0.05);border:1px solid rgba(212,175,55,0.22);color:#FFF5D6;"
                  @focus="e=>{e.target.style.borderColor='rgba(212,175,55,0.6)';e.target.style.boxShadow='0 0 14px rgba(212,175,55,0.12)'}"
                  @blur=" e=>{e.target.style.borderColor='rgba(212,175,55,0.22)';e.target.style.boxShadow=''}" />
              </div>

              <!-- Password -->
              <div class="space-y-1">
                <label style="display:block;color:rgba(212,175,55,0.55);font-size:0.62rem;letter-spacing:0.25em;text-transform:uppercase">密碼（至少 6 字元）</label>
                <input v-model="regPassword" type="password" required minlength="6" autocomplete="new-password" placeholder="password"
                  class="w-full rounded-lg px-4 py-2.5 text-sm outline-none transition-all"
                  style="background:rgba(255,240,200,0.05);border:1px solid rgba(212,175,55,0.22);color:#FFF5D6;"
                  @focus="e=>{e.target.style.borderColor='rgba(212,175,55,0.6)';e.target.style.boxShadow='0 0 14px rgba(212,175,55,0.12)'}"
                  @blur=" e=>{e.target.style.borderColor='rgba(212,175,55,0.22)';e.target.style.boxShadow=''}" />
              </div>

              <p v-if="regError" class="text-red-400 text-xs">{{ regError }}</p>

              <button type="submit" :disabled="regLoading || usernameStatus==='taken'"
                class="w-full rounded-lg py-2.5 text-sm font-bold tracking-widest uppercase disabled:opacity-50 transition-all mt-1"
                style="background:linear-gradient(135deg,#7A5C0E 0%,#D4AF37 50%,#7A5C0E 100%);color:#1A0800;box-shadow:0 2px 20px rgba(212,175,55,0.35);"
                @mouseenter="e=>{if(!regLoading)e.target.style.filter='brightness(1.12)'}"
                @mouseleave="e=>e.target.style.filter=''">
                {{ regLoading ? '建立中…' : '建立帳號並進場' }}
              </button>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>

  </div>
</template>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s, transform 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; transform: scale(0.96); }

.google-gold-btn {
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.google-gold-btn:hover {
  border-color: rgba(212,175,55,0.7) !important;
  box-shadow: 0 0 18px rgba(212,175,55,0.2);
}
.google-gold-btn:hover > div:first-child {
  background: rgba(212,175,55,0.08) !important;
}

.music-btn {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}
.music-btn:hover {
  transform: scale(1.15);
  border-color: rgba(212,175,55,0.85) !important;
  box-shadow: 0 0 0 3px rgba(212,175,55,0.25), 0 0 18px rgba(212,175,55,0.35);
}
.music-btn:active { transform: scale(1.05); }
</style>
