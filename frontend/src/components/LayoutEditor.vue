<script setup>
import { ref, computed } from 'vue'
import { useDashboardStore, DRUM_LABELS } from '../stores/dashboardStore'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue'])
const close = () => emit('update:modelValue', false)

const dashboard = useDashboardStore()

// ── Palette: all chromatic notes C3–C6 ───────────────────────────────────────
const PITCH_CLASSES = ['C','C#','D','D#','E','F','F#','G','G#','A','A#','B']
const PALETTE = []
for (let oct = 3; oct <= 5; oct++) {
  for (const pc of PITCH_CLASSES) PALETTE.push(`${pc}${oct}`)
}
PALETTE.push('C6')

// ── Drag state (not reactive — just tracks the drag session) ─────────────────
let dragSrc = null   // { type:'slot', index } | { type:'palette', note }
const draggingOverSlot = ref(null)

function onDragStartSlot(i, e) {
  dragSrc = { type: 'slot', index: i }
  e.dataTransfer.effectAllowed = 'move'
}
function onDragStartPalette(note, e) {
  dragSrc = { type: 'palette', note }
  e.dataTransfer.effectAllowed = 'copy'
}
function onDragOverSlot(i, e) {
  e.preventDefault()
  draggingOverSlot.value = i
}
function onDragLeaveSlot() {
  draggingOverSlot.value = null
}
function onDropToSlot(targetIdx) {
  draggingOverSlot.value = null
  if (!dragSrc) return
  const notes = [...dashboard.activeNotes]
  if (dragSrc.type === 'palette') {
    notes[targetIdx] = dragSrc.note
  } else if (dragSrc.type === 'slot' && dragSrc.index !== targetIdx) {
    ;[notes[dragSrc.index], notes[targetIdx]] = [notes[targetIdx], notes[dragSrc.index]]
  }
  dashboard.setActiveNotes(notes)
  dragSrc = null
}
function onDragEnd() {
  dragSrc = null
  draggingOverSlot.value = null
}

// ── Save ──────────────────────────────────────────────────────────────────────
const saveName = ref('')
const saving = ref(false)
async function save() {
  const name = saveName.value.trim()
  if (!name) return
  saving.value = true
  try { await dashboard.saveLayout(name); saveName.value = '' }
  finally { saving.value = false }
}

// ── Delete ────────────────────────────────────────────────────────────────────
async function del(id) {
  await dashboard.deleteLayout(id)
}

const isDrum = computed(() => dashboard.selectedInstrument === 'drum')

// Drum pad icons shown in the ring slots and palette
const DRUM_ICONS = { C4: '🥁', D4: '🪘', E4: '🎩', F4: '👒', G4: '🔺', A4: '🔻', B4: '💫', C5: '👏' }
const DRUM_PALETTE = Object.entries(DRUM_LABELS).map(([note, name]) => ({ note, name, icon: DRUM_ICONS[note] ?? '🥁' }))

// Whether a note has a sharp (to style it differently)
function isSharp(note) { return note.includes('#') }
</script>

<template>
  <!-- Backdrop -->
  <Teleport to="body">
    <div v-if="modelValue"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm"
      @click.self="close">

      <div class="bg-slate-800 rounded-2xl shadow-2xl w-full max-w-xl mx-4 flex flex-col max-h-[90vh] overflow-hidden">

        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-slate-700 shrink-0">
          <h2 class="font-bold text-slate-100">🎵 自訂音階環</h2>
          <button @click="close" class="text-slate-400 hover:text-white text-xl leading-none">×</button>
        </div>

        <div class="overflow-y-auto flex-1 px-5 py-4 space-y-5">

          <!-- ── Current ring slots ────────────────────────────────────────── -->
          <section>
            <p class="text-xs text-slate-400 mb-2 uppercase tracking-wider">目前音階環（拖拉調整順序 / 從下方拖入替換）</p>
            <div class="flex gap-2">
              <div v-for="(note, i) in dashboard.activeNotes" :key="i"
                draggable="true"
                @dragstart="onDragStartSlot(i, $event)"
                @dragover="onDragOverSlot(i, $event)"
                @dragleave="onDragLeaveSlot"
                @drop="onDropToSlot(i)"
                @dragend="onDragEnd"
                class="flex-1 text-center py-2.5 rounded-xl text-sm font-bold cursor-grab active:cursor-grabbing select-none transition-colors"
                :class="draggingOverSlot === i
                  ? 'bg-emerald-500 text-white scale-105'
                  : 'bg-slate-700 text-slate-100 hover:bg-slate-600'">
                <template v-if="isDrum">
                  <div class="text-base leading-none">{{ DRUM_ICONS[note] ?? '🥁' }}</div>
                  <div class="text-[10px] font-semibold mt-0.5">{{ DRUM_LABELS[note] ?? note }}</div>
                </template>
                <template v-else>{{ note }}</template>
                <div class="text-slate-500 text-[10px] font-normal mt-0.5">{{ i + 1 }}</div>
              </div>
            </div>
          </section>

          <!-- ── Palette ───────────────────────────────────────────────────── -->
          <section v-if="!isDrum">
            <p class="text-xs text-slate-400 mb-2 uppercase tracking-wider">音符庫（拖到上方格子替換）</p>
            <div class="flex flex-wrap gap-1.5">
              <div v-for="note in PALETTE" :key="note"
                draggable="true"
                @dragstart="onDragStartPalette(note, $event)"
                @dragend="onDragEnd"
                class="px-2 py-1 rounded-lg text-xs font-mono font-semibold cursor-grab select-none transition-colors"
                :class="isSharp(note)
                  ? 'bg-slate-600 text-slate-300 hover:bg-slate-500'
                  : 'bg-slate-700 text-slate-100 hover:bg-slate-600'">
                {{ note }}
              </div>
            </div>
          </section>

          <!-- ── Drum palette ──────────────────────────────────────────────── -->
          <section v-else>
            <p class="text-xs text-slate-400 mb-2 uppercase tracking-wider">打擊板（拖到上方格子替換）</p>
            <div class="flex flex-wrap gap-2">
              <div v-for="pad in DRUM_PALETTE" :key="pad.note"
                draggable="true"
                @dragstart="onDragStartPalette(pad.note, $event)"
                @dragend="onDragEnd"
                class="flex flex-col items-center gap-0.5 px-3 py-2 rounded-xl text-xs font-semibold cursor-grab select-none transition-colors bg-slate-700 hover:bg-slate-600">
                <span class="text-xl leading-none">{{ pad.icon }}</span>
                <span class="text-slate-100">{{ pad.name }}</span>
              </div>
            </div>
          </section>

          <!-- ── Save ──────────────────────────────────────────────────────── -->
          <section class="flex gap-2">
            <input v-model="saveName"
              placeholder="為這個排列命名…"
              @keyup.enter="save"
              class="flex-1 bg-slate-700 text-slate-100 placeholder-slate-500 text-sm rounded-xl px-3 py-2 outline-none focus:ring-2 focus:ring-emerald-500" />
            <button @click="save" :disabled="!saveName.trim() || saving"
              class="px-4 py-2 text-sm font-semibold rounded-xl transition-colors disabled:opacity-40"
              :class="saveName.trim() ? 'bg-emerald-600 hover:bg-emerald-500 text-white' : 'bg-slate-700 text-slate-500'">
              {{ saving ? '儲存中…' : '儲存' }}
            </button>
          </section>

          <!-- ── Saved layouts ──────────────────────────────────────────────── -->
          <section v-if="dashboard.savedLayouts.length">
            <p class="text-xs text-slate-400 mb-2 uppercase tracking-wider">已儲存的排列</p>
            <div class="space-y-2">
              <div v-for="layout in dashboard.savedLayouts" :key="layout.id"
                class="flex items-center gap-3 bg-slate-700/60 rounded-xl px-4 py-2.5">
                <div class="flex-1 min-w-0">
                  <p class="text-slate-100 text-sm font-semibold truncate">{{ layout.name }}</p>
                  <p class="text-slate-500 text-xs truncate">
                    {{ isDrum ? layout.notes.map(n => DRUM_LABELS[n] ?? n).join(' · ') : layout.notes.join(' · ') }}
                  </p>
                </div>
                <button @click="dashboard.setActiveNotes([...layout.notes])"
                  class="text-xs px-3 py-1 bg-emerald-700 hover:bg-emerald-600 text-white rounded-lg transition-colors shrink-0">
                  載入
                </button>
                <button @click="del(layout.id)"
                  class="text-xs px-3 py-1 bg-slate-600 hover:bg-rose-700 text-slate-300 hover:text-white rounded-lg transition-colors shrink-0">
                  刪除
                </button>
              </div>
            </div>
          </section>
          <p v-else class="text-slate-600 text-xs text-center">尚無儲存的排列</p>

        </div>
      </div>
    </div>
  </Teleport>
</template>
