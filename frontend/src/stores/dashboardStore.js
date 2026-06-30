import { defineStore } from 'pinia'
import apiClient from '../services/api'
import { connectWebSocket, disconnectWebSocket } from '../services/websocketService'

let synth = null

const NOTE_COLORS = {
  C4: 'text-red-400', D4: 'text-orange-400', E4: 'text-yellow-400',
  F4: 'text-green-400', G4: 'text-teal-400', A4: 'text-blue-400',
  B4: 'text-indigo-400', C5: 'text-purple-400',
}

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    wsConnected: false,
    audioEnabled: false,
    currentNote: null,
    currentInstrument: 'piano',
    volume: 80,
    recentNotes: [],
    recentGestures: [],
  }),

  getters: {
    noteColor: (state) => NOTE_COLORS[state.currentNote] ?? 'text-white',
  },

  actions: {
    connect() {
      connectWebSocket({
        onNote: (event) => this._handleNoteEvent(event),
        onConnect: () => { this.wsConnected = true },
        onDisconnect: () => { this.wsConnected = false },
      })
    },

    disconnect() {
      disconnectWebSocket()
      this.wsConnected = false
    },

    async enableAudio() {
      const Tone = await import('tone')
      await Tone.start()
      synth = new Tone.Synth({
        oscillator: { type: 'triangle' },
        envelope: { attack: 0.02, decay: 0.1, sustain: 0.3, release: 0.8 },
      }).toDestination()
      synth.volume.value = Tone.gainToDb(this.volume / 127)
      this.audioEnabled = true
    },

    async fetchRecentGestures() {
      try {
        const { data } = await apiClient.get('/gestures/recent?limit=10')
        this.recentGestures = data
      } catch {
        // silently ignore if backend unreachable
      }
    },

    _handleNoteEvent(event) {
      this.currentNote = event.note
      this.currentInstrument = event.instrument

      this.recentNotes.unshift({
        id: event.id ?? Date.now(),
        note: event.note,
        instrument: event.instrument,
        volume: event.volume,
        time: new Date(event.createdAt ?? Date.now()).toLocaleTimeString(),
      })
      if (this.recentNotes.length > 20) this.recentNotes.pop()

      if (this.audioEnabled && synth) {
        try {
          synth.triggerAttackRelease(event.note, '8n')
        } catch {
          // ignore invalid notes
        }
      }
    },
  },
})
