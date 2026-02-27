/**
 * Crea uno store reattivo tramite closure.
 * state e listeners sono privati — accessibili solo tramite i metodi restituiti.
 * set() aggiorna lo stato per merge parziale e notifica tutti i sottoscrittori.
 * Lo stato vive in memoria: viene azzerato ad ogni ricaricamento della pagina.
 * @param {Object} initial - Stato iniziale dello store
 * @returns {{ state: Object, set: function, subscribe: function }}
 */
function createStore(initial) {
  let state = { ...initial };
  const listeners = [];
  return {
    get state() {
      return state;
    },
    set(patch) {
      state = { ...state, ...patch };
      listeners.forEach(fn => fn(state));
    },
    subscribe(fn) {
      listeners.push(fn);
    }
  };
}

// Stato di autenticazione. Viene ripristinato all'avvio tramite checkAuth(),
// che interroga il server per verificare se esiste una sessione attiva.
const auth = createStore({ isAuthenticated: false, user: null });

/**
 * Verifica la sessione sul server e aggiorna auth di conseguenza.
 * È l'unico modo per ripristinare lo stato di autenticazione
 * dopo un ricaricamento della pagina.
 * @returns {Promise<void>}
 */
async function checkAuth() {
  try {
    const res = await fetch('/api/auth/session');
    const data = await res.json();
    if (res.ok && !data.err) {
      auth.set({ isAuthenticated: true, user: data.out });
    }
  } catch (_) {}
}

/**
 * Chiama il server per invalidare la sessione, poi reindirizza alla root.
 * Importa DEFAULT_ROUTES dinamicamente per evitare dipendenze circolari.
 * @returns {Promise<void>}
 */
async function logout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST' });
  } finally {
    auth.set({ isAuthenticated: false, user: null });
    // Importa dinamicamente per evitare circular dependency
    import('./modules.config.js').then(({ DEFAULT_ROUTES }) => {
      window.location.hash = DEFAULT_ROUTES.root.slice(1);
    });
  }
}

// Store del modulo corrente — usato da sidebar-layout per evidenziare la voce attiva.
const currentModule = createStore({ name: null });

/**
 * Aggiorna il modulo corrente e notifica i sottoscrittori.
 * @param {string} name - Identificatore del modulo
 */
currentModule.navigate = function(name) {
  this.set({ name });
};

export { auth, checkAuth, logout, currentModule };
