import { writable } from 'svelte/store'

// Componente sidebar del modulo attivo
export const contextSidebar = writable(null)

// Props da passare al componente sidebar
export const contextSidebarProps = writable({})

// Setta il componente sidebar del modulo
export function setContextSidebar(component, props = {}) {
  contextSidebar.set(component)
  contextSidebarProps.set(props)
}

// Rimuove il componente sidebar
export function clearContextSidebar() {
  contextSidebar.set(null)
  contextSidebarProps.set({})
}
