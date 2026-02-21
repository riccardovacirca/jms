// Crea uno store reattivo tramite closure.
// state e listeners sono privati — accessibili solo tramite i metodi restituiti.
// set() aggiorna lo stato per merge parziale e notifica tutti i sottoscrittori.
// Lo stato vive in memoria: viene azzerato ad ogni ricaricamento della pagina.
function createStore(initial) {
  let state = { ...initial }
  const listeners = []
  return {
    get state() { return state },
    set(patch) {
      state = { ...state, ...patch }
      listeners.forEach(fn => fn(state))
    },
    subscribe(fn) { listeners.push(fn) }
  }
}

// Stato di autenticazione. Viene ripristinato all'avvio tramite checkAuth(),
// che interroga il server per verificare se esiste una sessione attiva.
export const auth = createStore({ isAuthenticated: false, user: null })

// Verifica la sessione sul server e aggiorna auth di conseguenza.
// Chiamata all'avvio: è l'unico modo per ripristinare lo stato
// di autenticazione dopo un ricaricamento della pagina.
export async function checkAuth() {
  try {
    const res  = await fetch('/api/auth/session')
    const data = await res.json()
    if (res.ok && !data.err) {
      auth.set({ isAuthenticated: true, user: data.out })
    }
  } catch (_) {}
}

// Chiama il server per invalidare la sessione, poi reindirizza alla home.
export async function logout() {
  try { await fetch('/api/auth/logout', { method: 'POST' }) } finally {
    window.location.href = '/home'
  }
}

// Store del modulo corrente — usato da sidebar-layout per evidenziare la voce attiva.
// navigate(name) aggiorna lo stato e notifica i sottoscrittori.
export const currentModule = createStore({ name: null })
currentModule.navigate = function(name) {
  this.set({ name })
}
