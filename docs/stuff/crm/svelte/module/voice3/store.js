/**
 * Store del modulo voice3.
 *
 * Gestisce lo stato della sessione WebRTC dell'operatore e
 * lo stato della chiamata in corso.
 *
 * Flusso operator-first:
 *   1. L'operatore configura il proprio userId e attiva la sessione
 *   2. La sessione WebRTC viene creata tramite JWT SDK (fetchToken → createSession)
 *   3. L'operatore inserisce il numero cliente e clicca "Pronto per Chiamata"
 *   4. Il backend viene notificato (prepare-call), poi serverCall() avvia il webhook
 *   5. Il sistema chiama il cliente in modo asincrono (delay 1s)
 *   6. La chiamata si connette quando il cliente risponde
 */
import { writable } from 'svelte/store'

// Vista corrente del modulo (al momento solo 'session')
export const currentView = writable('session')

// Configurazione operatore: userId e stato attivazione sessione
export const sdkConfig = writable({
  userId: '',
  sessionActive: false
})

// Stato della chiamata in corso
// status: 'idle' | 'waiting_customer' | 'connected'
export const callState = writable({
  active: false,
  callId: null,
  customerNumber: null,
  conversationName: null,
  status: 'idle'
})
