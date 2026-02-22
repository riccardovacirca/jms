// --- logger ---
// Configurazione globale. backendEnabled attiva l'invio dei log al server.
const CONFIG = {
  consoleEnabled:  true,
  backendEnabled:  false,
  debugToBackend:  false
};

/**
 * Formatta un messaggio di log con timestamp, livello e modulo di provenienza.
 * @param {string} module - Nome del modulo chiamante
 * @param {string} level - Livello di log (DEBUG, INFO, WARN, ERROR)
 * @param {string} message - Testo del messaggio
 * @param {*} data - Dati aggiuntivi opzionali (serializzati in JSON)
 * @returns {string} Stringa formattata
 */
function fmt(module, level, message, data) {
  const ts    = new Date().toISOString();
  const extra = data ? ' ' + JSON.stringify(data) : '';
  return `[${ts}] [${level}] [${module}] ${message}${extra}`;
}

/**
 * Invia un messaggio di log a /api/logs.
 * Gli errori di rete vengono ignorati silenziosamente.
 * @param {string} module - Nome del modulo chiamante
 * @param {string} level - Livello di log
 * @param {string} message - Testo del messaggio
 * @param {*} data - Dati aggiuntivi opzionali
 */
async function send(module, level, message, data) {
  try {
    await fetch('/api/logs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        module,
        level,
        message,
        data,
        timestamp: new Date().toISOString()
      })
    });
  } catch (_) {}
}

/**
 * Dispatcher centrale: scrive in console e/o invia al backend secondo CONFIG.
 * I messaggi DEBUG vengono inviati al backend solo se debugToBackend è abilitato.
 * @param {string} module - Nome del modulo chiamante
 * @param {string} level - Livello di log
 * @param {string} message - Testo del messaggio
 * @param {*} data - Dati aggiuntivi opzionali
 */
function log(module, level, message, data) {
  if (CONFIG.consoleEnabled) {
    level === 'ERROR' ? console.error(fmt(module, level, message, data))
    : level === 'WARN'  ? console.warn(fmt(module, level, message, data))
    : console.log(fmt(module, level, message, data));
  }
  if (CONFIG.backendEnabled && (level !== 'DEBUG' || CONFIG.debugToBackend)) {
    send(module, level, message, data);
  }
}

/**
 * Emette un messaggio di livello DEBUG.
 * @param {string} m - Nome del modulo chiamante
 * @param {string} msg - Testo del messaggio
 * @param {*} d - Dati aggiuntivi opzionali
 */
function debug(m, msg, d) {
  log(m, 'DEBUG', msg, d);
}

/**
 * Emette un messaggio di livello INFO.
 * @param {string} m - Nome del modulo chiamante
 * @param {string} msg - Testo del messaggio
 * @param {*} d - Dati aggiuntivi opzionali
 */
function info(m, msg, d) {
  log(m, 'INFO', msg, d);
}

/**
 * Emette un messaggio di livello WARN.
 * @param {string} m - Nome del modulo chiamante
 * @param {string} msg - Testo del messaggio
 * @param {*} d - Dati aggiuntivi opzionali
 */
function warn(m, msg, d) {
  log(m, 'WARN', msg, d);
}

/**
 * Emette un messaggio di livello ERROR.
 * @param {string} m - Nome del modulo chiamante
 * @param {string} msg - Testo del messaggio
 * @param {*} d - Dati aggiuntivi opzionali
 */
function error(m, msg, d) {
  log(m, 'ERROR', msg, d);
}

/**
 * Shortcut per loggare un'azione utente a livello INFO.
 * @param {string} m - Nome del modulo chiamante
 * @param {string} act - Nome dell'azione (es. 'submit', 'delete')
 * @param {*} d - Dati aggiuntivi opzionali
 */
function action(m, act, d) {
  log(m, 'INFO', 'action:' + act, d);
}

/**
 * Shortcut per loggare una chiamata API a livello DEBUG.
 * @param {string} m - Nome del modulo chiamante
 * @param {string} method - Metodo HTTP (GET, POST, ecc.)
 * @param {string} endpoint - URL dell'endpoint
 * @param {*} d - Dati aggiuntivi opzionali
 */
function api(m, method, endpoint, d) {
  log(m, 'DEBUG', method + ' ' + endpoint, d);
}

/**
 * Shortcut per loggare la risposta di una chiamata API.
 * Livello INFO se ok, ERROR altrimenti.
 * @param {string} m - Nome del modulo chiamante
 * @param {string} endpoint - URL dell'endpoint
 * @param {boolean} ok - true se la risposta è andata a buon fine
 * @param {*} d - Dati aggiuntivi opzionali
 */
function apiResponse(m, endpoint, ok, d) {
  log(m, ok ? 'INFO' : 'ERROR', 'response:' + endpoint, d);
}

/**
 * Aggiorna la configurazione del logger per merge parziale.
 * @param {Partial<typeof CONFIG>} opts - Chiavi da sovrascrivere
 */
function configure(opts) {
  Object.assign(CONFIG, opts);
}


// --- fetchWithRefresh ---
// Più chiamate concorrenti che ricevono 401 condividono lo stesso refresh
// tramite refreshState.promise, evitando richieste duplicate.
const refreshState = {
  active:  false,
  promise: null
};

/**
 * Tenta di rinnovare il token di accesso chiamando /api/auth/refresh.
 * Se il refresh fallisce, reindirizza a /auth (sessione scaduta).
 * @returns {Promise<void>}
 */
async function refreshToken() {
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    credentials: 'include'
  });
  if (!res.ok) {
    window.location.href = '/auth';
    throw new Error('Session expired');
  }
}

/**
 * Wrapper attorno a fetch che gestisce il rinnovo automatico del token.
 * Se la risposta è 401, tenta un refresh una sola volta e ripete la richiesta.
 * @param {string} url - URL della risorsa
 * @param {RequestInit} options - Opzioni fetch
 * @returns {Promise<Response>}
 */
async function fetchWithRefresh(url, options = {}) {
  options.credentials = 'include';
  let res = await fetch(url, options);
  if (res.status === 401 && !url.includes('/api/auth/')) {
    if (!refreshState.active) {
      refreshState.active = true;
      refreshState.promise = refreshToken().finally(() => {
        refreshState.active = false;
      });
    }
    await refreshState.promise;
    res = await fetch(url, options);
  }
  return res;
}

export {
  debug,
  info,
  warn,
  error,
  action,
  api,
  apiResponse,
  configure,
  fetchWithRefresh
};
