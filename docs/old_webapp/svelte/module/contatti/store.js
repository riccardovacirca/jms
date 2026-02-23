import { writable } from 'svelte/store';

// Store del modulo contatti
export const contatti = writable([]);
export const loading = writable(false);
export const error = writable(null);
export const searchQuery = writable('');

// Paginazione
export const pagination = writable({
  offset: 0,
  limit: 50,
  total: 0,
  hasNext: false
});

// Contatto selezionato per modifica
export const selectedContatto = writable(null);

// Vista corrente: 'list', 'new', 'edit', 'view'
export const currentView = writable('list');

// Funzioni di navigazione
export function showList() {
  currentView.set('list');
  selectedContatto.set(null);
}

export function showNew() {
  currentView.set('new');
  selectedContatto.set(null);
}

export function showEdit(contatto) {
  currentView.set('edit');
  selectedContatto.set(contatto);
}

export function showView(contatto) {
  currentView.set('view');
  selectedContatto.set(contatto);
}
