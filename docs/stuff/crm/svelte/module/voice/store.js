import { writable } from 'svelte/store'

// userId dell'operatore corrente nel contesto Vonage.
// Deve corrispondere al nome dell'utente registrato su Vonage
// (vonage users create --name='...') e usato come claim "sub" nel JWT SDK.
export const operatorUserId = writable(null)

// Vista corrente nel modulo voice
export const currentView = writable('config')

// Funzioni per gestire le viste
export function showConfig() {
  currentView.set('config')
}

export function showTest() {
  currentView.set('test')
}
