import { authorized } from './store.js';

const AUTH_ERRORS = new Set(['Non autenticato', 'Token non valido o scaduto']);

/**
 * Inizializzazione globale dell'applicazione.
 * Chiamata dal router all'avvio, prima di qualsiasi modulo.
 *
 * Installa un interceptor su fetch: se una risposta API indica sessione
 * scaduta o assente, imposta authorized=false e il router reindirizza al login.
 */
export default function init() {
  const _fetch = window.fetch.bind(window);
  window.fetch = async function (...args) {
    const res   = await _fetch(...args);
    const clone = res.clone();
    try {
      const data = await clone.json();
      if (data.err && AUTH_ERRORS.has(data.log)) {
        authorized.set(false);
      }
    } catch (_) {}
    return res;
  };
}
