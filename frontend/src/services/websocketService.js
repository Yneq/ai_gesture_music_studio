import { Client } from '@stomp/stompjs'

// Use the current page's host so the WS goes through Vite's proxy on LAN too
const _proto = location.protocol === 'https:' ? 'wss' : 'ws'
const WS_URL = import.meta.env.VITE_WS_BASE_URL || `${_proto}://${location.host}/ws`

let stompClient = null

export function connectWebSocket({ onNote, onPresence, onConnect, onDisconnect }, username, token) {
  stompClient = new Client({
    brokerURL: WS_URL,
    connectHeaders: token ? { token } : {},
    reconnectDelay: 3000,
    onConnect: () => {
      stompClient.subscribe('/topic/notes', (message) => {
        try { onNote(JSON.parse(message.body)) } catch { /* ignore */ }
      })
      stompClient.subscribe('/topic/presence', (message) => {
        try { onPresence?.(JSON.parse(message.body)) } catch { /* ignore */ }
      })
      // Announce this user has joined
      if (username) {
        stompClient.publish({
          destination: '/app/presence/join',
          body: JSON.stringify({ username }),
        })
      }
      onConnect?.()
    },
    onDisconnect: () => onDisconnect?.(),
    onStompError: () => onDisconnect?.(),
  })

  stompClient.activate()
}

export function publishPresenceLeave(username) {
  if (!username || !stompClient?.connected) return
  stompClient.publish({
    destination: '/app/presence/leave',
    body: JSON.stringify({ username }),
  })
}

export function disconnectWebSocket() {
  stompClient?.deactivate()
  stompClient = null
}
