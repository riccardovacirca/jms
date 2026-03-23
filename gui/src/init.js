import { authorized } from './store.js';

const AUTH_ERRORS = new Set(['Non autenticato', 'Token non valido o scaduto']);

/**
 * Stato del refresh token per evitare refresh multipli in parallelo.
 */
const refreshState = {
  inProgress: false,
  promise: null
};

/**
 * Inizializzazione globale dell'applicazione.
 * Chiamata dal router all'avvio, prima di qualsiasi modulo.
 *
 * Installa un interceptor su fetch che:
 * 1. Rileva errori di autenticazione (access token scaduto)
 * 2. Tenta automaticamente il refresh del token
 * 3. Ripete la richiesta originale se il refresh ha successo
 * 4. Imposta authorized=false solo se il refresh fallisce
 */
export default function init() {
  const _fetch = window.fetch.bind(window);

  window.fetch = async function (...args) {
    const res = await _fetch(...args);
    const clone = res.clone();

    try {
      const data = await clone.json();

      if (data.err && AUTH_ERRORS.has(data.log)) {
        const url = typeof args[0] === 'string' ? args[0] : args[0].url;

        if (url.includes('/auth/refresh') || url.includes('/auth/login')) {
          authorized.set(false);
          return res;
        }

        if (!refreshState.inProgress) {
          refreshState.inProgress = true;
          refreshState.promise = _fetch('/api/user/auth/refresh', { method: 'POST' })
            .then(async (refreshRes) => {
              const refreshData = await refreshRes.json();
              refreshState.inProgress = false;

              if (refreshRes.ok && !refreshData.err) {
                return true;
              }

              authorized.set(false);
              return false;
            })
            .catch(() => {
              refreshState.inProgress = false;
              authorized.set(false);
              return false;
            });
        }

        const refreshSuccess = await refreshState.promise;

        if (refreshSuccess) {
          return _fetch(...args);
        }
      }
    } catch (_) {}

    return res;
  };
}
