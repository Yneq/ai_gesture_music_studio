<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'
import { useDashboardStore, DRUM_LABELS } from '../stores/dashboardStore'

const auth = useAuthStore()
const dashboard = useDashboardStore()
const router = useRouter()

onMounted(() => {
  dashboard.connect()
  dashboard.fetchRecentGestures()
})
onUnmounted(() => dashboard.disconnect())

function logout() {
  dashboard.disconnect()
  auth.logout()
  router.push('/login')
}

const INSTRUMENTS = [
  { id: 'piano',  label: 'Piano',  icon: '🎹' },
  { id: 'guitar', label: 'Acoustic', icon: '🎸' },
  { id: 'synth',  label: 'Synth',  icon: '🎛️' },
  { id: 'drum',   label: 'Drum',   icon: '🥁' },
]

const GESTURE_EMOJI = {
  OPEN_HAND: '🖐', FIST: '✊', THUMB_UP: '👍',
  OK: '👌', PEACE: '✌️', POINT: '👆',
}
</script>

<template>
  <div class="min-h-screen bg-slate-900 text-slate-100 flex flex-col">

    <!-- Header -->
    <header class="bg-slate-800 border-b border-slate-700 px-6 py-3 flex items-center justify-between">
      <h1 class="text-lg font-bold tracking-wide">AI Gesture Music Studio</h1>
      <div class="flex items-center gap-4">
        <span class="flex items-center gap-2 text-sm">
          <span class="w-2.5 h-2.5 rounded-full"
            :class="dashboard.wsConnected ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'"></span>
          <span :class="dashboard.wsConnected ? 'text-emerald-400' : 'text-slate-400'">
            {{ dashboard.wsConnected ? 'Live' : '未連線' }}
          </span>
        </span>
        <span class="text-slate-400 text-sm">{{ auth.user?.username }}</span>
        <button @click="logout" class="text-sm text-slate-400 hover:text-white transition-colors">登出</button>
      </div>
    </header>

    <main class="flex-1 grid grid-cols-1 md:grid-cols-3 gap-4 p-4">

      <!-- Left: Current Note + Instrument Selector -->
      <div class="bg-slate-800 rounded-2xl p-5 flex flex-col gap-4">
        <h2 class="text-slate-400 text-xs uppercase tracking-widest font-semibold">即時音符</h2>

        <!-- Big note display -->
        <div class="flex-1 flex flex-col items-center justify-center gap-1">
          <div class="text-7xl font-black transition-all duration-200"
            :class="dashboard.currentNote ? dashboard.noteColor : 'text-slate-700'">
            {{ dashboard.selectedInstrument === 'drum' && dashboard.currentNote
                ? (DRUM_LABELS[dashboard.currentNote] ?? dashboard.currentNote)
                : (dashboard.currentNote ?? '—') }}
          </div>
          <div v-if="dashboard.currentUsername"
            class="text-sm font-medium" :class="dashboard.noteColor">
            {{ dashboard.currentUsername }}
          </div>
          <div class="text-slate-500 text-xs">{{ dashboard.selectedInstrument }}</div>
        </div>

        <!-- Instrument Selector -->
        <div class="space-y-2">
          <p class="text-slate-400 text-xs">樂器</p>
          <div class="grid grid-cols-4 gap-2">
            <button
              v-for="inst in INSTRUMENTS"
              :key="inst.id"
              @click="dashboard.setInstrument(inst.id)"
              class="flex flex-col items-center gap-1 rounded-xl py-2 text-xs font-semibold transition-colors"
              :class="dashboard.selectedInstrument === inst.id
                ? 'bg-emerald-600 text-white'
                : 'bg-slate-700 text-slate-400 hover:bg-slate-600'"
            >
              <span class="text-lg">{{ inst.icon }}</span>
              {{ inst.label }}
            </button>
          </div>
        </div>

        <!-- Audio toggle -->
        <button v-if="!dashboard.audioEnabled"
          @click="dashboard.enableAudio()"
          class="w-full bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold rounded-xl py-2 transition-colors">
          🔊 啟用音效
        </button>
        <div v-else class="text-center text-emerald-400 text-sm">🎵 音效已啟用</div>
      </div>

      <!-- Center: Note Feed -->
      <div class="bg-slate-800 rounded-2xl p-5 flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <h2 class="text-slate-400 text-xs uppercase tracking-widest font-semibold">即時音符串流</h2>
          <span class="text-slate-600 text-xs">最近 20 筆</span>
        </div>

        <div class="flex-1 overflow-y-auto space-y-1.5 pr-1">
          <transition-group name="feed">
            <div v-for="event in dashboard.recentNotes" :key="event.id"
              class="flex items-center gap-2 bg-slate-700/60 rounded-lg px-3 py-2 text-sm">
              <!-- User color dot -->
              <span class="w-2 h-2 rounded-full flex-shrink-0"
                :class="event.color.replace('text-', 'bg-')"></span>
              <span class="font-bold w-8" :class="event.color">{{ event.note }}</span>
              <span class="text-slate-300 text-xs flex-1 truncate">{{ event.username }}</span>
              <span class="text-slate-500 text-xs">{{ event.instrument }}</span>
              <span class="text-slate-600 text-xs">{{ event.time }}</span>
            </div>
          </transition-group>
          <p v-if="!dashboard.recentNotes.length"
            class="text-slate-600 text-sm text-center py-8">
            比 POINT 手勢進入音階區域即可觸發
          </p>
        </div>
      </div>

      <!-- Right: Gesture History -->
      <div class="bg-slate-800 rounded-2xl p-5 flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <h2 class="text-slate-400 text-xs uppercase tracking-widest font-semibold">指令手勢紀錄</h2>
          <button @click="dashboard.fetchRecentGestures()"
            class="text-slate-500 hover:text-white text-xs transition-colors">重新整理</button>
        </div>

        <div class="flex-1 overflow-y-auto space-y-1.5 pr-1">
          <div v-for="g in dashboard.recentGestures" :key="g.id"
            class="flex items-center gap-3 bg-slate-700/60 rounded-lg px-3 py-2 text-sm">
            <span class="text-xl">{{ GESTURE_EMOJI[g.gesture] ?? '🤚' }}</span>
            <div class="flex-1 min-w-0">
              <div class="text-slate-200 font-medium truncate">{{ g.gesture }}</div>
              <div class="text-slate-500 text-xs">信心 {{ (g.confidence * 100).toFixed(0) }}%</div>
            </div>
          </div>
          <p v-if="!dashboard.recentGestures.length"
            class="text-slate-600 text-sm text-center py-8">尚無紀錄</p>
        </div>

        <div class="border-t border-slate-700 pt-3 space-y-1 text-xs text-slate-500">
          <div>✊ FIST ・ 👍 THUMB_UP ・ 👌 OK</div>
          <div>✌️ PEACE ・ 🖐 OPEN_HAND → 送指令</div>
          <div>👆 POINT + 音區 → 觸發音符</div>
        </div>
      </div>

    </main>
  </div>
</template>

<style scoped>
.feed-enter-active { transition: all 0.25s ease; }
.feed-enter-from  { opacity: 0; transform: translateY(-8px); }
</style>
