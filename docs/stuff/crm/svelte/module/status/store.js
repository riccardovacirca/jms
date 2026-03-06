import { writable } from 'svelte/store';

// Stato condiviso del modulo status
export const healthData = writable(null);
export const logsData = writable([]);
export const loading = writable(false);
export const error = writable(null);
