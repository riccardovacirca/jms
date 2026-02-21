// Registra un componente come custom element associandolo al tag HTML indicato.
// Uso: mount('app-main', AppMain) → <app-main> istanzia AppMain nel DOM.
export function mount(tag, component) {
  customElements.define(tag, component)
}


// --- logger ---
// Configurazione globale. backendEnabled attiva l'invio dei log al server.
const CONFIG = { consoleEnabled: true, backendEnabled: false, debugToBackend: false }

// Formatta un messaggio di log con timestamp, livello e modulo di provenienza.
function fmt(module, level, message, data) {
  return `[${new Date().toISOString()}] [${level}] [${module}] ${message}${data ? ' ' + JSON.stringify(data) : ''}`
}

// Invia il log a /api/logs. Gli errori di rete vengono ignorati silenziosamente.
async function send(module, level, message, data) {
  try {
    await fetch('/api/logs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ module, level, message, data, timestamp: new Date().toISOString() })
    })
  } catch (_) {}
}

// Dispatcher centrale: scrive in console e/o invia al backend secondo CONFIG.
// I DEBUG vengono inviati al backend solo se debugToBackend è abilitato.
function log(module, level, message, data) {
  if (CONFIG.consoleEnabled) {
    level === 'ERROR' ? console.error(fmt(module, level, message, data))
    : level === 'WARN'  ? console.warn(fmt(module, level, message, data))
    : console.log(fmt(module, level, message, data))
  }
  if (CONFIG.backendEnabled && (level !== 'DEBUG' || CONFIG.debugToBackend))
    send(module, level, message, data)
}

// API pubblica del logger. Il primo argomento (m) è il nome del modulo chiamante.
// action() e api()/apiResponse() sono shortcut semantici per log applicativi.
export const debug       = (m, msg, d) => log(m, 'DEBUG', msg, d)
export const info        = (m, msg, d) => log(m, 'INFO',  msg, d)
export const warn        = (m, msg, d) => log(m, 'WARN',  msg, d)
export const error       = (m, msg, d) => log(m, 'ERROR', msg, d)
export const action      = (m, act, d) => log(m, 'INFO',  'action:' + act, d)
export const api         = (m, method, endpoint, d) => log(m, 'DEBUG', method + ' ' + endpoint, d)
export const apiResponse = (m, endpoint, ok, d)     => log(m, ok ? 'INFO' : 'ERROR', 'response:' + endpoint, d)
export const configure   = (opts) => Object.assign(CONFIG, opts)


// --- fetchWithRefresh ---
// Wrapper attorno a fetch che gestisce il rinnovo automatico del token.
// Se la risposta è 401, tenta un refresh una sola volta e ripete la richiesta.
// Più chiamate concorrenti che ricevono 401 condividono lo stesso refresh
// tramite refreshPromise, evitando richieste duplicate.
// Se il refresh fallisce, reindirizza alla root (sessione scaduta).
let refreshing = false
let refreshPromise = null

async function refreshToken() {
  const res = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
  if (!res.ok) { window.location.href = '/auth'; throw new Error('Session expired') }
}

export async function fetchWithRefresh(url, options = {}) {
  options.credentials = 'include'
  let res = await fetch(url, options)
  if (res.status === 401 && !url.includes('/api/auth/')) {
    if (!refreshing) {
      refreshing = true
      refreshPromise = refreshToken().finally(() => { refreshing = false })
    }
    await refreshPromise
    res = await fetch(url, options)
  }
  return res
}
