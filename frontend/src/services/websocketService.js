import { Client } from '@stomp/stompjs'

const WS_URL = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080/ws'

let stompClient = null

export function connectWebSocket({ onNote, onConnect, onDisconnect }) {
  stompClient = new Client({
    brokerURL: WS_URL,
    reconnectDelay: 3000,
    onConnect: () => {
      stompClient.subscribe('/topic/notes', (message) => {
        try {
          onNote(JSON.parse(message.body))
        } catch {
          // ignore malformed messages
        }
      })
      onConnect?.()
    },
    onDisconnect: () => onDisconnect?.(),
    onStompError: () => onDisconnect?.(),
  })

  stompClient.activate()
}

export function disconnectWebSocket() {
  stompClient?.deactivate()
  stompClient = null
}
