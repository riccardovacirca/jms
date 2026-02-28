/**
 * Configurazione delle rotte dell'applicazione.
 *
 * authorization: null                     → rotta libera, nessun controllo
 * authorization: { redirectTo: '/path' }  → rotta protetta:
 *   - se authorized = false e /path è una rotta registrata → redirect
 *   - se authorized = false e /path non è registrata       → accesso negato
 */
export const MODULE_CONFIG = {
  index: {
    path: '/',
    authorization: null
  }
};

/**
 * Rotta di default quando l'hash è assente.
 */
export const DEFAULT_ROUTE = '/';
