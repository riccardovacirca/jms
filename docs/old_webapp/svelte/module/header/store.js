import { writable } from 'svelte/store'

// Componente header del modulo attivo
export const contextHeader = writable(null)

// Props da passare al componente header
export const contextHeaderProps = writable({})

// Titolo del modulo attivo
export const contextTitle = writable('')

// Setta il componente header del modulo
export function setContextHeader(component, props = {}, title = '') {
  contextHeader.set(component)
  contextHeaderProps.set(props)
  contextTitle.set(title)
}

// Rimuove il componente header
export function clearContextHeader() {
  contextHeader.set(null)
  contextHeaderProps.set({})
  contextTitle.set('')
}
