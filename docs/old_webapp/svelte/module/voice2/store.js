import { writable } from 'svelte/store'

export const currentView = writable('session')
export const sdkConfig = writable({
  userId: '',
  sessionActive: false
})
export const callState = writable({
  active: false,
  callId: null,
  customerNumber: null,
  conversationName: null,
  status: 'idle' // idle, waiting_customer, connected
})
