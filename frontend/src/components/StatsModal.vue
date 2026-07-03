<script setup>
import { ref, watch } from 'vue'
import apiClient from '../services/api'

const props = defineProps({ modelValue: Boolean, username: String, avatarUrl: String })
const emit  = defineEmits(['update:modelValue'])
const close = () => emit('update:modelValue', false)

const stats   = ref(null)
const loading = ref(false)

watch(() => props.modelValue, async (open) => {
  if (!open) return
  loading.value = true
  stats.value = null
  try {
    const { data } = await apiClient.get('/stats/me')
    stats.value = data
  } finally {
    loading.value = false
  }
})

const INSTRUMENT_ICON = { piano: '🎹', guitar: '🎸', synth: '🎛️', drum: '🥁' }
const GESTURE_ICON    = { OPEN_HAND: '🖐', FIST: '✊', THUMB_UP: '👍', PEACE: '✌️', POINT: '👆', ILoveYou: '🤟' }
</script>

<template>
  <Teleport to="body">
    <Transition name="stats-modal">
      <div v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center px-4"
        style="background:rgba(6,2,0,0.78);backdrop-filter:blur(6px)"
        @click.self="close">

        <div class="relative w-full max-w-sm rounded-2xl p-7"
          style="background:rgba(12,5,0,0.92);backdrop-filter:blur(28px);
                 border:1px solid rgba(212,175,55,0.35);
                 box-shadow:0 0 0 1px rgba(212,175,55,0.07) inset,0 16px 60px rgba(0,0,0,0.85)">

          <!-- Corner ornaments -->
          <span class="absolute top-2.5 left-2.5  text-[9px]" style="color:rgba(212,175,55,0.35)">✦</span>
          <span class="absolute top-2.5 right-2.5 text-[9px]" style="color:rgba(212,175,55,0.35)">✦</span>
          <span class="absolute bottom-2.5 left-2.5  text-[9px]" style="color:rgba(212,175,55,0.35)">✦</span>
          <span class="absolute bottom-2.5 right-2.5 text-[9px]" style="color:rgba(212,175,55,0.35)">✦</span>

          <!-- Close -->
          <button @click="close"
            class="absolute top-3 right-4 text-xl leading-none transition-colors"
            style="color:rgba(212,175,55,0.4);"
            @mouseenter="e=>e.target.style.color='rgba(212,175,55,0.9)'"
            @mouseleave="e=>e.target.style.color='rgba(212,175,55,0.4)'">×</button>

          <!-- Header: avatar + username -->
          <div class="flex flex-col items-center gap-3 mb-6">
            <div class="w-16 h-16 rounded-full overflow-hidden flex items-center justify-center"
              style="border:2px solid rgba(212,175,55,0.5);box-shadow:0 0 20px rgba(212,175,55,0.2)">
              <img v-if="avatarUrl" :src="avatarUrl" class="w-full h-full object-cover" referrerpolicy="no-referrer">
              <span v-else class="text-2xl font-black" style="color:#D4AF37;background:rgba(212,175,55,0.15);width:100%;height:100%;display:flex;align-items:center;justify-content:center">
                {{ username?.charAt(0)?.toUpperCase() }}
              </span>
            </div>
            <div class="text-center">
              <p style="font-family:'Georgia',serif;font-size:1rem;font-weight:700;letter-spacing:0.1em;color:#FFF5D6">
                {{ username }}
              </p>
              <div class="flex items-center justify-center gap-2 mt-1.5">
                <div class="h-px w-12" style="background:linear-gradient(to right,transparent,rgba(212,175,55,0.5))"></div>
                <span style="color:rgba(212,175,55,0.5);font-size:10px">❖</span>
                <div class="h-px w-12" style="background:linear-gradient(to left,transparent,rgba(212,175,55,0.5))"></div>
              </div>
            </div>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="flex justify-center py-8">
            <span style="color:rgba(212,175,55,0.4);font-size:0.8rem">載入中…</span>
          </div>

          <!-- Stats grid -->
          <div v-else-if="stats" class="space-y-2">
            <!-- Top row: notes today + total -->
            <div class="grid grid-cols-2 gap-2">
              <div class="rounded-xl p-3 text-center"
                style="background:rgba(212,175,55,0.08);border:1px solid rgba(212,175,55,0.2)">
                <div class="text-2xl font-black" style="color:#D4AF37">{{ stats.todayNotes }}</div>
                <div class="text-[11px] mt-0.5" style="color:rgba(212,175,55,0.5)">今日音符</div>
              </div>
              <div class="rounded-xl p-3 text-center"
                style="background:rgba(212,175,55,0.08);border:1px solid rgba(212,175,55,0.2)">
                <div class="text-2xl font-black" style="color:#D4AF37">{{ stats.totalNotes }}</div>
                <div class="text-[11px] mt-0.5" style="color:rgba(212,175,55,0.5)">累計音符</div>
              </div>
            </div>

            <!-- Favourites row -->
            <div class="grid grid-cols-3 gap-2">
              <div class="rounded-xl p-3 text-center"
                style="background:rgba(212,175,55,0.06);border:1px solid rgba(212,175,55,0.15)">
                <div class="text-2xl">{{ INSTRUMENT_ICON[stats.topInstrument] ?? '🎵' }}</div>
                <div class="text-xs font-semibold mt-1" style="color:#FFF5D6">
                  {{ stats.topInstrument ?? '—' }}
                </div>
                <div class="text-[10px]" style="color:rgba(212,175,55,0.4)">最愛樂器</div>
              </div>
              <div class="rounded-xl p-3 text-center"
                style="background:rgba(212,175,55,0.06);border:1px solid rgba(212,175,55,0.15)">
                <div class="text-xl font-black" style="color:rgba(52,211,153,0.85)">
                  {{ stats.topNote ?? '—' }}
                </div>
                <div class="text-[10px] mt-1" style="color:rgba(212,175,55,0.4)">× {{ stats.topNoteCount }}</div>
                <div class="text-[10px]" style="color:rgba(212,175,55,0.4)">最常音符</div>
              </div>
              <div class="rounded-xl p-3 text-center"
                style="background:rgba(212,175,55,0.06);border:1px solid rgba(212,175,55,0.15)">
                <div class="text-2xl">{{ GESTURE_ICON[stats.topGesture] ?? '🤚' }}</div>
                <div class="text-[10px] mt-1" style="color:rgba(212,175,55,0.4)">× {{ stats.topGestureCount }}</div>
                <div class="text-[10px]" style="color:rgba(212,175,55,0.4)">最常手勢</div>
              </div>
            </div>

            <!-- Total gestures -->
            <div class="rounded-xl px-4 py-2.5 flex items-center justify-between"
              style="background:rgba(212,175,55,0.06);border:1px solid rgba(212,175,55,0.15)">
              <span style="color:rgba(212,175,55,0.5);font-size:0.7rem">累計手勢指令</span>
              <span class="font-bold" style="color:#D4AF37">{{ stats.totalGestures }}</span>
            </div>
          </div>

          <p v-else class="text-center py-6 text-sm" style="color:rgba(212,175,55,0.3)">載入失敗</p>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.stats-modal-enter-active, .stats-modal-leave-active { transition: opacity 0.2s, transform 0.2s; }
.stats-modal-enter-from, .stats-modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>
