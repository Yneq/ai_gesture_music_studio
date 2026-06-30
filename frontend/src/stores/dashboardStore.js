import { defineStore } from 'pinia'
import apiClient from '../services/api'
import { connectWebSocket, disconnectWebSocket } from '../services/websocketService'

const PALETTE = [
  'text-emerald-400', 'text-blue-400', 'text-purple-400',
  'text-orange-400', 'text-pink-400', 'text-yellow-400',
]
const _colorCache = {}
function userColor(username) {
  if (!_colorCache[username]) {
    const hash = [...(username || '?')].reduce((a, c) => a + c.charCodeAt(0), 0)
    _colorCache[username] = PALETTE[hash % PALETTE.length]
  }
  return _colorCache[username]
}

// Diatonic chord voicings in C major for each zone note
const GUITAR_CHORDS = {
  C4: ['C3', 'E3', 'G3', 'C4', 'E4'],        // C major
  D4: ['D3', 'F3', 'A3', 'D4', 'F4'],        // D minor
  E4: ['E2', 'B2', 'E3', 'G3', 'B3'],        // E minor
  F4: ['F3', 'A3', 'C4', 'F4'],              // F major
  G4: ['G2', 'B2', 'D3', 'G3', 'B3'],        // G major
  A4: ['A2', 'E3', 'A3', 'C4', 'E4'],        // A minor
  B4: ['B3', 'D4', 'F4', 'B4'],              // B diminished
  C5: ['C4', 'E4', 'G4', 'C5'],              // C major (high)
}

export const DRUM_LABELS = {
  C4: 'Kick', D4: 'Snare', E4: 'Hi-hat', F4: 'Open Hi-hat',
  G4: 'Tom H', A4: 'Tom L', B4: 'Crash', C5: 'Clap',
}

// All audio nodes live outside Pinia (reactive() breaks AudioNode internals)
let synth = null
let audioNodes = []
let drumKit = null
let guitarStrings = []  // individual PluckSynth voices for chord strumming

// Sustain tracking for piano / synth
let currentInstrument = 'piano'
let sustainedNote = null
let releaseTimer = null

function releaseSustained() {
  clearTimeout(releaseTimer)
  releaseTimer = null
  if (!synth || !sustainedNote) { sustainedNote = null; return }
  try {
    // Sampler needs the note name; monophonic synths don't
    if (currentInstrument === 'piano') synth.triggerRelease(sustainedNote)
    else synth.triggerRelease()
  } catch { /* ignore */ }
  sustainedNote = null
}

const SUSTAIN_MS = { piano: 3000, synth: 3000 }

function triggerNote(note) {
  if (currentInstrument === 'drum') {
    const trigger = drumKit?.triggers[note] ?? drumKit?.triggers['C4']
    trigger?.()
    return
  }

  if (currentInstrument === 'guitar') {
    // Strum chord across individual PluckSynth string voices (28 ms per string)
    const chordNotes = GUITAR_CHORDS[note] ?? [note]
    chordNotes.forEach((n, i) => {
      try { guitarStrings[i % guitarStrings.length]?.triggerAttackRelease(n, 3, `+${i * 0.028}`) } catch { /* ignore */ }
    })
    return
  }

  // Piano / Synth: legato sustain — release previous note, attack new one
  releaseSustained()
  sustainedNote = note
  try { synth?.triggerAttack(note) } catch { /* ignore */ }
  releaseTimer = setTimeout(releaseSustained, SUSTAIN_MS[currentInstrument] ?? 3000)
}

// ─── Drum Kit ────────────────────────────────────────────────────────────────

function disposeDrumKit() {
  if (!drumKit) return
  drumKit.synths.forEach(s => s.dispose())
  drumKit.nodes.forEach(n => n.dispose())
  drumKit = null
}

function buildDrumKit(Tone) {
  disposeDrumKit()

  const room = new Tone.Reverb({ decay: 0.6, wet: 0.14 }).toDestination()

  const kick = new Tone.MembraneSynth({
    pitchDecay: 0.09, octaves: 7,
    envelope: { attack: 0.001, decay: 0.45, sustain: 0, release: 0.12 },
  }).connect(room)
  kick.volume.value = -3

  const snareHP = new Tone.Filter(2500, 'highpass').connect(room)
  const snare = new Tone.NoiseSynth({
    noise: { type: 'white' },
    envelope: { attack: 0.001, decay: 0.14, sustain: 0, release: 0.04 },
  }).connect(snareHP)
  snare.volume.value = -5

  const hihatHP = new Tone.Filter(10000, 'highpass').toDestination()
  const hihat = new Tone.NoiseSynth({
    noise: { type: 'white' },
    envelope: { attack: 0.001, decay: 0.045, sustain: 0, release: 0.01 },
  }).connect(hihatHP)
  hihat.volume.value = -9

  const openHP = new Tone.Filter(8000, 'highpass').toDestination()
  const openHihat = new Tone.NoiseSynth({
    noise: { type: 'white' },
    envelope: { attack: 0.001, decay: 0.3, sustain: 0.05, release: 0.2 },
  }).connect(openHP)
  openHihat.volume.value = -9

  const tomH = new Tone.MembraneSynth({
    pitchDecay: 0.06, octaves: 3,
    envelope: { attack: 0.001, decay: 0.22, sustain: 0, release: 0.08 },
  }).toDestination()
  tomH.volume.value = -5

  const tomL = new Tone.MembraneSynth({
    pitchDecay: 0.08, octaves: 4,
    envelope: { attack: 0.001, decay: 0.3, sustain: 0, release: 0.1 },
  }).toDestination()
  tomL.volume.value = -5

  const crashHP = new Tone.Filter(5000, 'highpass').toDestination()
  const crash = new Tone.NoiseSynth({
    noise: { type: 'white' },
    envelope: { attack: 0.005, decay: 0.9, sustain: 0.08, release: 0.6 },
  }).connect(crashHP)
  crash.volume.value = -11

  const clapBP = new Tone.Filter({ frequency: 1800, type: 'bandpass', Q: 0.8 }).toDestination()
  const clap = new Tone.NoiseSynth({
    noise: { type: 'pink' },
    envelope: { attack: 0.001, decay: 0.08, sustain: 0, release: 0.04 },
  }).connect(clapBP)
  clap.volume.value = -6

  drumKit = {
    synths: [kick, snare, hihat, openHihat, tomH, tomL, crash, clap],
    nodes: [room, snareHP, hihatHP, openHP, crashHP, clapBP],
    triggers: {
      C4: () => kick.triggerAttackRelease('C1', '8n'),
      D4: () => snare.triggerAttackRelease('8n'),
      E4: () => hihat.triggerAttackRelease('16n'),
      F4: () => openHihat.triggerAttackRelease('8n'),
      G4: () => tomH.triggerAttackRelease('A2', '8n'),
      A4: () => tomL.triggerAttackRelease('E2', '8n'),
      B4: () => crash.triggerAttackRelease('4n'),
      C5: () => clap.triggerAttackRelease('16n'),
    },
  }
}

// ─── Non-drum Synths ──────────────────────────────────────────────────────────

async function buildSynth(Tone, instrument) {
  currentInstrument = instrument
  releaseSustained()

  if (instrument === 'drum') {
    synth?.dispose()
    audioNodes.forEach(n => n.dispose())
    audioNodes = []
    synth = null
    buildDrumKit(Tone)
    return
  }

  disposeDrumKit()
  synth?.dispose()
  audioNodes.forEach(n => n.dispose())
  audioNodes = []

  switch (instrument) {

    case 'guitar': {
      // 6 independent PluckSynth instances — one per chord string
      // (PolySynth rejects PluckSynth because it doesn't extend Monophonic)
      const reverb = new Tone.Reverb({ decay: 2.8, wet: 0.32 }).toDestination()
      guitarStrings = Array.from({ length: 6 }, () => {
        const s = new Tone.PluckSynth({ attackNoise: 1.5, dampening: 3200, resonance: 0.988 })
          .connect(reverb)
        s.volume.value = -4
        return s
      })
      audioNodes = [reverb, ...guitarStrings]
      synth = null
      break
    }

    case 'synth': {
      // FM synth with chorus + reverb → lush sustained electronic pad
      const reverb = new Tone.Reverb({ decay: 3.5, wet: 0.45 }).toDestination()
      const chorus = new Tone.Chorus({ frequency: 3, delayTime: 3.5, depth: 0.7, wet: 0.5 })
        .connect(reverb).start()
      audioNodes = [reverb, chorus]
      synth = new Tone.FMSynth({
        harmonicity: 3, modulationIndex: 12,
        oscillator: { type: 'sine' },
        envelope: { attack: 0.08, decay: 0.3, sustain: 0.85, release: 2.5 },
        modulation: { type: 'triangle' },
        modulationEnvelope: { attack: 0.4, decay: 0.01, sustain: 1, release: 0.5 },
      }).connect(chorus)
      break
    }

    default: { // piano — real Salamander Grand Piano samples
      const reverb = new Tone.Reverb({ decay: 1.8, wet: 0.18 }).toDestination()
      audioNodes = [reverb]
      synth = new Tone.Sampler({
        urls: {
          A0: 'A0.mp3', C1: 'C1.mp3', 'D#1': 'Ds1.mp3', 'F#1': 'Fs1.mp3',
          A1: 'A1.mp3', C2: 'C2.mp3', 'D#2': 'Ds2.mp3', 'F#2': 'Fs2.mp3',
          A2: 'A2.mp3', C3: 'C3.mp3', 'D#3': 'Ds3.mp3', 'F#3': 'Fs3.mp3',
          A3: 'A3.mp3', C4: 'C4.mp3', 'D#4': 'Ds4.mp3', 'F#4': 'Fs4.mp3',
          A4: 'A4.mp3', C5: 'C5.mp3', 'D#5': 'Ds5.mp3', 'F#5': 'Fs5.mp3',
          A5: 'A5.mp3', C6: 'C6.mp3', 'D#6': 'Ds6.mp3', 'F#6': 'Fs6.mp3',
          A6: 'A6.mp3', C7: 'C7.mp3', 'D#7': 'Ds7.mp3', 'F#7': 'Fs7.mp3',
          A7: 'A7.mp3', C8: 'C8.mp3',
        },
        release: 3,
        baseUrl: 'https://tonejs.github.io/audio/salamander/',
      }).connect(reverb)
    }
  }
  if (synth) synth.volume.value = -6
}

// ─── Store ────────────────────────────────────────────────────────────────────

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    wsConnected: false,
    audioEnabled: false,
    selectedInstrument: 'piano',
    currentNote: null,
    currentUsername: null,
    recentNotes: [],
    recentGestures: [],
  }),

  getters: {
    noteColor: (state) => userColor(state.currentUsername),
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
      await buildSynth(Tone, this.selectedInstrument)
      this.audioEnabled = true
    },

    async setInstrument(instrument) {
      this.selectedInstrument = instrument
      if (this.audioEnabled) {
        const Tone = await import('tone')
        await buildSynth(Tone, instrument)
      }
    },

    async fetchRecentGestures() {
      try {
        const { data } = await apiClient.get('/gestures/recent?limit=10')
        this.recentGestures = data
      } catch { /* ignore */ }
    },

    _handleNoteEvent(event) {
      this.currentNote = event.note
      this.currentUsername = event.username

      const isDrum = this.selectedInstrument === 'drum'
      this.recentNotes.unshift({
        id: event.id ?? Date.now(),
        username: event.username,
        note: isDrum ? (DRUM_LABELS[event.note] ?? event.note) : event.note,
        instrument: event.instrument,
        volume: event.volume,
        time: new Date(event.createdAt ?? Date.now()).toLocaleTimeString(),
        color: userColor(event.username),
      })
      if (this.recentNotes.length > 20) this.recentNotes.pop()

      if (this.audioEnabled) triggerNote(event.note)
    },
  },
})
