# Frontend Architecture - Context-Based Routing

## Panoramica

Questa guida descrive l'architettura del frontend basata su **routing centralizzato** e **gestione dei contesti**.

L'applicazione distingue tre contesti:
- **public**: Accessibile a tutti (es. home page, landing)
- **private**: Richiede autenticazione (es. dashboard, gestione dati)
- **auth**: Moduli di autenticazione (login, cambio password)

Il routing è gestito tramite **hash URLs** (`/#home`, `/#auth`) con lazy loading dei moduli.

---

## Struttura File

```
vite/src/
├── index.html              # Layout principale con container dinamico
├── main.js                 # Router + Context Manager
├── store.js                # State management (auth, currentModule)
├── util.js                 # Utilities (logger, fetchWithRefresh)
├── modules.config.js       # Configurazione moduli e contesti
└── modules/
    ├── home/
    │   ├── index.js        # Entry point del modulo
    │   ├── component.js    # Web component
    │   └── home.css
    └── auth/
        ├── index.js
        ├── login.js
        ├── changepass.js
        └── auth.css
```

---

## Step 1: Configurazione Moduli (`modules.config.js`)

**File**: `vite/src/modules.config.js`

Questo file contiene la **configurazione dichiarativa** di tutti i moduli e contesti.

```javascript
/**
 * Configurazione dei moduli disponibili nell'applicazione.
 * Ogni modulo ha:
 * - context: 'public' | 'private' | 'auth'
 * - path: URL hash path (es. /home)
 * - title: Titolo della pagina
 */
export const MODULE_CONFIG = {
  home: {
    context: 'public',
    path: '/home',
    title: 'Home'
  },
  auth: {
    context: 'auth',
    path: '/auth',
    title: 'Login'
  }
};

/**
 * Configurazione dei contesti.
 * Definisce il comportamento di ogni contesto:
 * - requiresAuth: Se richiede autenticazione
 * - redirectTo: Dove reindirizzare se requisiti non soddisfatti
 */
export const CONTEXT_CONFIG = {
  public: {
    requiresAuth: false
  },
  private: {
    requiresAuth: true,
    redirectTo: '/auth'
  },
  auth: {
    requiresAuth: false
  }
};

/**
 * Route di default dell'applicazione
 */
export const DEFAULT_ROUTES = {
  root: '/home',              // Dove andare quando si accede a /
  unauthorized: '/auth',      // Dove andare se non autenticato
  afterLogin: '/home'         // Dove andare dopo login
};
```

**Cosa fa:**
- Definisce tutti i moduli disponibili
- Associa ogni modulo a un contesto
- Configura il comportamento dei contesti
- Definisce le route di default

---

## Step 2: Layout Principale (`index.html`)

**File**: `vite/src/index.html`

HTML minimalista con un singolo container gestito dinamicamente dal router.

```html
<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>App</title>
</head>
<body>
  <!-- Container dinamico gestito dal router -->
  <div id="app"></div>

  <!-- Entry point dell'applicazione -->
  <script type="module" src="/main.js"></script>
</body>
</html>
```

**Cosa fa:**
- Crea un container vuoto `#app`
- Carica `main.js` che avvia il router

---

## Step 3: Router e Context Manager (`main.js`)

**File**: `vite/src/main.js`

Il cuore dell'applicazione: gestisce routing, contesti e lazy loading dei moduli.

```javascript
import { auth, checkAuth } from './store.js';
import { MODULE_CONFIG, CONTEXT_CONFIG, DEFAULT_ROUTES } from './modules.config.js';
import { info, error } from './util.js';

/**
 * Router centralizzato con gestione dei contesti.
 * Ascolta i cambiamenti dell'hash URL e dello stato di autenticazione.
 */
class Router {
  constructor() {
    this.currentModule = null;
    this.container = document.getElementById('app');

    // Ascolta cambiamenti nell'hash URL
    window.addEventListener('hashchange', () => this.route());

    // Ascolta cambiamenti nello stato di autenticazione
    auth.subscribe(() => this.handleAuthChange());

    // Inizializzazione
    this.init();
  }

  /**
   * Inizializza il router verificando lo stato di autenticazione
   * e caricando il modulo corrente.
   */
  async init() {
    await checkAuth();
    this.route();
  }

  /**
   * Gestisce il routing in base all'hash URL corrente.
   * Trova il modulo corrispondente e lo carica.
   */
  route() {
    const hash = window.location.hash.slice(1) || DEFAULT_ROUTES.root.slice(1);
    const path = '/' + hash;

    info('Router', 'Navigating to', { path });

    // Trova il modulo corrispondente al path
    const moduleName = Object.keys(MODULE_CONFIG).find(
      name => MODULE_CONFIG[name].path === path
    );

    if (!moduleName) {
      this.showNotFound(path);
      return;
    }

    this.loadModule(moduleName);
  }

  /**
   * Carica un modulo verificando SOLO i requisiti di autenticazione.
   * Il router non conosce la business logic dei moduli.
   * @param {string} moduleName - Nome del modulo da caricare
   */
  async loadModule(moduleName) {
    const config = MODULE_CONFIG[moduleName];
    const context = CONTEXT_CONFIG[config.context];

    // ✅ Verifica SOLO se l'utente è autenticato (binario)
    if (context.requiresAuth && !auth.state.isAuthenticated) {
      info('Router', 'Unauthorized, redirecting', { to: context.redirectTo });
      window.location.hash = context.redirectTo.slice(1);
      return;
    }

    try {
      // Dynamic import del modulo
      const module = await import(`./modules/${moduleName}/index.js`);

      // Pulisce il container
      this.container.innerHTML = '';

      // Monta il modulo
      module.default.mount(this.container);
      this.currentModule = moduleName;

      // Aggiorna il titolo della pagina
      document.title = config.title || 'App';

      info('Router', 'Module loaded', { module: moduleName });
    } catch (err) {
      error('Router', 'Failed to load module', { module: moduleName, error: err.message });
      this.showError(moduleName);
    }
  }

  /**
   * Gestisce i cambiamenti nello stato di autenticazione.
   * Reindirizza se necessario in base al contesto del modulo corrente.
   */
  handleAuthChange() {
    const currentPath = window.location.hash.slice(1) || DEFAULT_ROUTES.root.slice(1);
    const moduleName = Object.keys(MODULE_CONFIG).find(
      name => MODULE_CONFIG[name].path === '/' + currentPath
    );

    if (moduleName) {
      const config = MODULE_CONFIG[moduleName];
      const context = CONTEXT_CONFIG[config.context];

      // ✅ Se siamo in un modulo privato e non siamo più autenticati
      if (context.requiresAuth && !auth.state.isAuthenticated) {
        window.location.hash = DEFAULT_ROUTES.unauthorized.slice(1);
      }

      // ✅ Se siamo loggati e siamo su auth, reindirizza alla home
      // (Il modulo auth gestisce internamente quando settare isAuthenticated = true)
      if (auth.state.isAuthenticated && config.context === 'auth') {
        window.location.hash = DEFAULT_ROUTES.afterLogin.slice(1);
      }
    }
  }

  /**
   * Mostra pagina 404
   */
  showNotFound(path) {
    this.container.innerHTML = `
      <div class="min-vh-100 d-flex align-items-center justify-content-center">
        <div class="text-center">
          <h1 class="display-1 text-muted">404</h1>
          <p class="lead">Pagina non trovata: ${path}</p>
          <a href="#home" class="btn btn-primary">Torna alla Home</a>
        </div>
      </div>
    `;
  }

  /**
   * Mostra pagina di errore
   */
  showError(moduleName) {
    this.container.innerHTML = `
      <div class="min-vh-100 d-flex align-items-center justify-content-center">
        <div class="text-center">
          <h1 class="display-4 text-danger">Errore</h1>
          <p class="lead">Impossibile caricare il modulo: ${moduleName}</p>
          <a href="#home" class="btn btn-primary">Torna alla Home</a>
        </div>
      </div>
    `;
  }
}

// Avvia il router
new Router();
```

**Cosa fa:**
- Gestisce il routing tramite hash URL
- Verifica SOLO se l'utente è autenticato (binario: true/false)
- Carica dinamicamente i moduli (lazy loading)
- Reindirizza automaticamente in base allo stato di autenticazione
- Gestisce errori 404 e errori di caricamento

**Cosa NON fa:**
- ❌ Non conosce la business logic dei moduli (es. cambio password obbligatorio)
- ❌ Non accede a campi specifici come `must_change_password`
- ❌ Ogni modulo gestisce internamente il proprio flusso

---

## Step 4: Modulo Home - Entry Point (`modules/home/index.js`)

**File**: `vite/src/modules/home/index.js`

Ogni modulo ha un **entry point** che espone un metodo `mount()`.

```javascript
import 'bootstrap/dist/css/bootstrap.min.css';
import './home.css';
import HomeComponent from './component.js';

/**
 * Entry point del modulo home.
 * Espone un metodo mount() chiamato dal router.
 */
export default {
  /**
   * Monta il modulo nel container fornito
   * @param {HTMLElement} container - Container dove montare il modulo
   */
  mount(container) {
    const component = new HomeComponent();
    container.appendChild(component);
  }
};
```

**Cosa fa:**
- Importa le dipendenze (CSS, componenti)
- Espone un metodo `mount()` standard
- Crea e inserisce il web component nel container

---

## Step 5: Modulo Home - Component (`modules/home/component.js`)

**File**: `vite/src/modules/home/component.js`

Il web component che implementa la logica e il rendering del modulo.

```javascript
import { auth, logout } from '../../store.js';
import { info } from '../../util.js';

/**
 * Web component del modulo home.
 * Mostra un messaggio di benvenuto recuperato dall'API /api/hello.
 */
export default class HomeComponent extends HTMLElement {
  connectedCallback() {
    this._render();
    this._loadHello();
    auth.subscribe(() => this._render());
  }

  /**
   * Carica il messaggio dall'endpoint /api/hello
   */
  async _loadHello() {
    try {
      const res = await fetch('/api/hello');
      const data = await res.json();
      const messageEl = this.querySelector('#hello-message');
      if (messageEl && !data.err) {
        messageEl.textContent = data.out;
      }
    } catch (e) {
      console.error('Failed to load hello message', e);
    }
  }

  /**
   * Renderizza il component
   */
  _render() {
    const { isAuthenticated, user } = auth.state;

    this.innerHTML = `
      <div class="min-vh-100 bg-light">
        <header class="d-flex align-items-center px-4 py-3 bg-white border-bottom">
          <span class="fw-bold fs-5">App</span>
          <div class="ms-auto d-flex align-items-center gap-2">
            ${isAuthenticated
              ? `<span class="text-muted small">${user?.username}</span>
                 <button class="btn btn-sm btn-outline-danger" id="btn-logout">Esci</button>`
              : `<button class="btn btn-sm btn-outline-primary" id="btn-login">Accedi</button>`
            }
          </div>
        </header>
        <main class="container py-5">
          <div class="text-center py-5">
            <h1 class="display-5">Benvenuto</h1>
            <p class="text-muted" id="hello-message">Caricamento...</p>
          </div>
        </main>
      </div>
    `;

    this.querySelector('#btn-logout')?.addEventListener('click', () => {
      info('home', 'User logout');
      logout();
    });

    this.querySelector('#btn-login')?.addEventListener('click', () => {
      window.location.hash = 'auth';
    });

    this._loadHello();
  }
}

customElements.define('home-component', HomeComponent);
```

**Cosa fa:**
- Implementa un web component standard
- Chiama `/api/hello` per recuperare un messaggio
- Mostra UI diversa in base allo stato di autenticazione
- Usa hash navigation per cambiare pagina

---

## Step 6: Modulo Home - CSS (`modules/home/home.css`)

**File**: `vite/src/modules/home/home.css`

CSS specifico del modulo (opzionale).

```css
/* Stili specifici del modulo home */
home-component {
  display: block;
}
```

---

## Step 7: Aggiornamento `store.js`

**File**: `vite/src/store.js`

Aggiornare la funzione `logout()` per usare hash navigation.

```javascript
/**
 * Crea uno store reattivo tramite closure.
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

// Stato di autenticazione
const auth = createStore({ isAuthenticated: false, user: null });

/**
 * Verifica la sessione sul server e aggiorna auth.
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
 * Logout: invalida sessione e reindirizza a home.
 */
async function logout() {
  try {
    await fetch('/api/auth/logout', { method: 'POST' });
  } finally {
    auth.set({ isAuthenticated: false, user: null });
    window.location.hash = 'home';  // <-- Hash navigation
  }
}

// Store del modulo corrente
const currentModule = createStore({ name: null });

currentModule.navigate = function(name) {
  this.set({ name });
};

export { auth, checkAuth, logout, currentModule };
```

**Cambiamenti:**
- `logout()` usa `window.location.hash = 'home'` invece di `window.location.href`

---

## Step 8: Aggiornamento `vite.config.js`

**File**: `vite/vite.config.js`

Semplificare la configurazione Vite: con hash routing non servono più entry point multipli.

```javascript
import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  root: 'src',
  build: {
    outDir: '../../src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        index: resolve(__dirname, 'src/index.html'),
      }
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
});
```

**Cambiamenti:**
- Un solo entry point: `index.html`
- Rimossi tutti gli altri entry point (home, auth)
- Non servono più route rewrite plugins

---

## Step 9: Modulo Auth - Entry Point (`modules/auth/index.js`)

**File**: `vite/src/modules/auth/index.js`

Entry point del modulo auth che gestisce **internamente** il flusso multi-step (login → cambio password → autenticazione completa).

**IMPORTANTE**: Il modulo auth gestisce la propria business logic senza esporre dettagli implementativi allo store globale. Lo store `auth.isAuthenticated` diventa `true` SOLO quando l'utente ha completato TUTTI gli step richiesti.

```javascript
import 'bootstrap/dist/css/bootstrap.min.css';
import './auth.css';
import LoginComponent from './login.js';
import ChangePasswordComponent from './changepass.js';
import { auth } from '../../store.js';

/**
 * Entry point del modulo auth.
 *
 * GESTISCE INTERNAMENTE:
 * - Login
 * - Cambio password obbligatorio
 * - Aggiornamento dello stato auth globale
 *
 * Lo store auth.isAuthenticated diventa true SOLO quando
 * l'utente ha completato TUTTI gli step richiesti.
 */
export default {
  mount(container) {
    // Stato interno del modulo auth (NON esposto globalmente)
    const authModuleState = this._getAuthModuleState();

    let component;

    if (authModuleState.needsPasswordChange) {
      // Step 2: Cambio password obbligatorio
      component = new ChangePasswordComponent();
    } else {
      // Step 1: Login
      component = new LoginComponent();
    }

    container.appendChild(component);

    // Listener per cambiamenti interni del modulo auth
    this._subscribeToAuthChanges(container);
  },

  /**
   * Legge lo stato interno del modulo auth.
   * Questa logica è PRIVATA al modulo.
   */
  _getAuthModuleState() {
    const internalUser = this._getInternalUser();

    return {
      isLoggedIn: internalUser !== null,
      needsPasswordChange: internalUser?.must_change_password === true
    };
  },

  /**
   * Legge i dati utente da sessionStorage.
   * Questi dati sono DIVERSI da auth.state.user (che è pubblico e minimale).
   */
  _getInternalUser() {
    const data = sessionStorage.getItem('auth_internal_state');
    return data ? JSON.parse(data) : null;
  },

  /**
   * Salva lo stato interno del modulo auth
   */
  _setInternalUser(user) {
    if (user) {
      sessionStorage.setItem('auth_internal_state', JSON.stringify(user));
    } else {
      sessionStorage.removeItem('auth_internal_state');
    }
  },

  /**
   * Ascolta i cambiamenti interni e rimonta il component se necessario
   */
  _subscribeToAuthChanges(container) {
    // Listener custom per eventi del modulo auth
    window.addEventListener('auth:state-changed', () => {
      const state = this._getAuthModuleState();

      if (!state.isLoggedIn) {
        // Tornato a login
        container.innerHTML = '';
        this.mount(container);
      } else if (state.needsPasswordChange) {
        // Passato a cambio password (rimane nel modulo auth)
        container.innerHTML = '';
        this.mount(container);
      } else {
        // ✅ AUTENTICAZIONE COMPLETA
        // Solo ora aggiorna lo store globale
        const user = this._getInternalUser();
        auth.set({
          isAuthenticated: true,
          user: { username: user.username } // Espone solo dati minimali
        });
        // Il router reagirà e reindirizzerà automaticamente alla home
      }
    });
  }
};
```

**Cosa fa:**
- Gestisce **internamente** il flusso multi-step (login → cambio password)
- Mantiene uno **stato privato** in sessionStorage
- Aggiorna `auth.isAuthenticated = true` SOLO quando l'autenticazione è completa
- Il router rimane agnostico rispetto alla business logic del modulo

---

## Step 10: Login Component (`modules/auth/login.js`)

**File**: `vite/src/modules/auth/login.js`

Component per il login. Gestisce lo step 1 del flusso di autenticazione.

```javascript
/**
 * Web component per il login.
 * Dopo il login, salva lo stato interno e notifica il modulo auth.
 */
export default class LoginComponent extends HTMLElement {
  connectedCallback() {
    this._render();
  }

  async _handleLogin(e) {
    e.preventDefault();
    const username = this.querySelector('#username').value;
    const password = this.querySelector('#password').value;

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });

      const data = await res.json();

      if (data.err) {
        this._showError(data.log);
        return;
      }

      // ✅ Salva stato INTERNO (non aggiorna ancora auth.state globale)
      const authModule = await import('./index.js');
      authModule.default._setInternalUser(data.out.user);

      // ✅ Notifica cambio stato al modulo auth
      // Il modulo auth deciderà se:
      // - Mostrare ChangePasswordComponent (se must_change_password = true)
      // - Settare auth.isAuthenticated = true (se autenticazione completa)
      window.dispatchEvent(new CustomEvent('auth:state-changed'));

    } catch (err) {
      this._showError('Errore di connessione');
    }
  }

  _render() {
    this.innerHTML = `
      <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light">
        <div class="card shadow" style="width: 400px">
          <div class="card-body">
            <h2 class="card-title text-center mb-4">Login</h2>
            <form id="login-form">
              <div class="mb-3">
                <label class="form-label">Username</label>
                <input type="text" class="form-control" id="username" required>
              </div>
              <div class="mb-3">
                <label class="form-label">Password</label>
                <input type="password" class="form-control" id="password" required>
              </div>
              <button type="submit" class="btn btn-primary w-100">Accedi</button>
              <div id="error" class="text-danger mt-2 d-none"></div>
            </form>
          </div>
        </div>
      </div>
    `;

    this.querySelector('#login-form').addEventListener('submit', (e) => this._handleLogin(e));
  }

  _showError(message) {
    const errorEl = this.querySelector('#error');
    errorEl.textContent = message;
    errorEl.classList.remove('d-none');
  }
}

customElements.define('login-component', LoginComponent);
```

**Cosa fa:**
- Esegue il login chiamando `/api/auth/login`
- Salva lo stato interno usando `_setInternalUser()` del modulo auth
- NON aggiorna `auth.isAuthenticated` direttamente
- Notifica il modulo auth tramite evento custom `auth:state-changed`
- Il modulo auth decide il prossimo step

---

## Step 11: ChangePassword Component (`modules/auth/changepass.js`)

**File**: `vite/src/modules/auth/changepass.js`

Component per il cambio password obbligatorio. Gestisce lo step 2 del flusso di autenticazione.

```javascript
/**
 * Web component per il cambio password obbligatorio.
 * Quando la password è cambiata, aggiorna lo stato interno
 * e notifica il completamento dell'autenticazione.
 */
export default class ChangePasswordComponent extends HTMLElement {
  connectedCallback() {
    this._render();
  }

  async _handleChangePassword(e) {
    e.preventDefault();
    const newPassword = this.querySelector('#new-password').value;
    const confirmPassword = this.querySelector('#confirm-password').value;

    if (newPassword !== confirmPassword) {
      this._showError('Le password non coincidono');
      return;
    }

    try {
      const res = await fetch('/api/auth/change-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ new_password: newPassword })
      });

      const data = await res.json();

      if (data.err) {
        this._showError(data.log);
        return;
      }

      // ✅ Password cambiata, aggiorna stato interno
      const authModule = await import('./index.js');
      const currentUser = authModule.default._getInternalUser();
      currentUser.must_change_password = false;
      authModule.default._setInternalUser(currentUser);

      // ✅ Notifica cambio stato → auth.isAuthenticated diventa true
      // Il modulo auth setterà auth.isAuthenticated = true
      // Il router reindirizzerà automaticamente alla home
      window.dispatchEvent(new CustomEvent('auth:state-changed'));

    } catch (err) {
      this._showError('Errore di connessione');
    }
  }

  _render() {
    this.innerHTML = `
      <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light">
        <div class="card shadow" style="width: 400px">
          <div class="card-body">
            <h2 class="card-title text-center mb-4">Cambio Password Obbligatorio</h2>
            <p class="text-muted mb-4">Devi cambiare la password prima di continuare</p>
            <form id="changepass-form">
              <div class="mb-3">
                <label class="form-label">Nuova Password</label>
                <input type="password" class="form-control" id="new-password" required>
              </div>
              <div class="mb-3">
                <label class="form-label">Conferma Password</label>
                <input type="password" class="form-control" id="confirm-password" required>
              </div>
              <button type="submit" class="btn btn-primary w-100">Cambia Password</button>
              <div id="error" class="text-danger mt-2 d-none"></div>
            </form>
          </div>
        </div>
      </div>
    `;

    this.querySelector('#changepass-form').addEventListener('submit', (e) => this._handleChangePassword(e));
  }

  _showError(message) {
    const errorEl = this.querySelector('#error');
    errorEl.textContent = message;
    errorEl.classList.remove('d-none');
  }
}

customElements.define('changepass-component', ChangePasswordComponent);
```

**Cosa fa:**
- Gestisce il cambio password obbligatorio
- Aggiorna lo stato interno rimuovendo il flag `must_change_password`
- Notifica il completamento tramite evento `auth:state-changed`
- Il modulo auth setta `auth.isAuthenticated = true`
- Il router reindirizza automaticamente alla home

---

## Flusso Completo: Login → Cambio Password → Home

```
┌────────────────────────────────────────────────────────────────┐
│ 1. Utente compila form login                                  │
│    LoginComponent chiama /api/auth/login                      │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│ 2. Backend risponde: { user: { must_change_password: true } } │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│ 3. LoginComponent salva stato in sessionStorage               │
│    authModule._setInternalUser(user)                          │
│    Lancia evento: 'auth:state-changed'                        │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│ 4. Modulo auth rileva stato interno                           │
│    needsPasswordChange = true                                 │
│    Rimonta con ChangePasswordComponent                        │
│    ❌ auth.isAuthenticated rimane FALSE                       │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│ 5. Utente cambia password                                     │
│    ChangePasswordComponent chiama /api/auth/change-password   │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│ 6. Backend conferma cambio password                           │
│    ChangePasswordComponent aggiorna sessionStorage            │
│    must_change_password = false                               │
│    Lancia evento: 'auth:state-changed'                        │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│ 7. Modulo auth rileva stato completo                          │
│    needsPasswordChange = false                                │
│    ✅ Setta auth.isAuthenticated = TRUE                       │
└───────────────────────────┬────────────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────────────┐
│ 8. Router rileva auth.isAuthenticated = true                  │
│    Siamo su contesto 'auth' ma autenticati                    │
│    Redirect automatico a #home                                │
└────────────────────────────────────────────────────────────────┘
```

**Punti chiave:**
- ✅ Il router NON sa nulla di `must_change_password`
- ✅ Il modulo auth gestisce internamente il flusso multi-step
- ✅ `auth.isAuthenticated` diventa `true` SOLO a fine processo
- ✅ Un modulo auth alternativo può implementare flussi diversi

---

## Come Aggiungere un Nuovo Modulo

### Esempio: Modulo "Dashboard" (privato)

#### 1. Aggiungi configurazione in `modules.config.js`

```javascript
export const MODULE_CONFIG = {
  home: { context: 'public', path: '/home', title: 'Home' },
  auth: { context: 'auth', path: '/auth', title: 'Login' },
  dashboard: { context: 'private', path: '/dashboard', title: 'Dashboard' }  // <-- NUOVO
};
```

#### 2. Crea directory e file

```
modules/dashboard/
├── index.js
├── component.js
└── dashboard.css
```

#### 3. Implementa `index.js`

```javascript
import 'bootstrap/dist/css/bootstrap.min.css';
import './dashboard.css';
import DashboardComponent from './component.js';

export default {
  mount(container) {
    const component = new DashboardComponent();
    container.appendChild(component);
  }
};
```

#### 4. Implementa `component.js`

```javascript
import { auth } from '../../store.js';

export default class DashboardComponent extends HTMLElement {
  connectedCallback() {
    this._render();
  }

  _render() {
    const { user } = auth.state;

    this.innerHTML = `
      <div class="container py-5">
        <h1>Dashboard di ${user?.username}</h1>
        <p>Benvenuto nella tua area riservata!</p>
      </div>
    `;
  }
}

customElements.define('dashboard-component', DashboardComponent);
```

#### 5. Aggiungi link in home

In `modules/home/component.js`:

```javascript
<a href="#dashboard" class="btn btn-primary">Vai alla Dashboard</a>
```

**Fatto!** Il modulo è automaticamente:
- ✅ Protetto da autenticazione
- ✅ Lazy loaded
- ✅ Accessibile via `/#dashboard`

---

## Vantaggi di Questa Architettura

### 1. **Configurazione Dichiarativa**
Un file centrale (`modules.config.js`) definisce tutti i moduli e contesti.

### 2. **Lazy Loading**
I moduli vengono caricati solo quando necessario, riducendo il bundle size iniziale.

### 3. **Context-Aware**
Il router gestisce automaticamente i requisiti di autenticazione.

### 4. **Hash Routing**
Funziona senza configurazione server-side, ideale per SPA.

### 5. **Modulare**
Ogni modulo è self-contained con i suoi file (JS, CSS, componenti).

### 6. **Scalabile**
Aggiungere moduli richiede solo 3 passi: config + directory + implementazione.

### 7. **Type-Safe (opzionale)**
Facile aggiungere TypeScript per type checking.

### 8. **Separazione delle Responsabilità (Router vs Moduli)**
**Il router si occupa SOLO di routing e verifica di autenticazione binaria.**
- ✅ Verifica `auth.isAuthenticated` (true/false)
- ✅ Reindirizza se non autenticato
- ❌ NON conosce la business logic dei moduli

**I moduli gestiscono la propria business logic internamente.**
- ✅ Il modulo auth decide quando l'autenticazione è completa
- ✅ Può implementare flussi multi-step (login → 2FA → cambio password → completo)
- ✅ Mantiene stato privato senza inquinare lo store globale
- ✅ Setta `auth.isAuthenticated = true` solo a processo completato

**Vantaggi:**
- 🔄 **Moduli auth intercambiabili**: Un modulo auth alternativo (OAuth, SAML, 2FA) può sostituire quello corrente senza modificare il router
- 🔒 **Incapsulamento**: La logica specifica (es. `must_change_password`) rimane privata al modulo
- 🧩 **Disaccoppiamento**: Il router non dipende dai dettagli implementativi dei moduli
- 🎯 **Single Responsibility**: Ogni componente ha una responsabilità chiara e limitata

---

## Navigazione

### Da Codice JavaScript

```javascript
// Vai a home
window.location.hash = 'home';

// Vai a auth
window.location.hash = 'auth';

// Vai a dashboard
window.location.hash = 'dashboard';
```

### Da HTML

```html
<a href="#home">Home</a>
<a href="#auth">Login</a>
<a href="#dashboard">Dashboard</a>
```

---

## Testing

### Test di Routing

```javascript
// Simula navigazione
window.location.hash = 'home';

// Verifica che il modulo sia caricato
const homeComponent = document.querySelector('home-component');
assert(homeComponent !== null);
```

### Test di Autenticazione

```javascript
// Simula stato non autenticato
auth.set({ isAuthenticated: false, user: null });

// Prova ad accedere a modulo privato
window.location.hash = 'dashboard';

// Verifica redirect a auth
setTimeout(() => {
  assert(window.location.hash === '#auth');
}, 100);
```

---

## Migration da Architettura Precedente

Se hai già un'applicazione con routing basato su file HTML multipli:

1. **Converti ogni pagina in un modulo**:
   - `home.html` → `modules/home/`
   - `auth.html` → `modules/auth/`

2. **Aggiorna i link**:
   - `href="/home.html"` → `href="#home"`

3. **Rimuovi vite entry points**:
   - Un solo entry point: `index.html`

4. **Testa**:
   - Verifica che ogni modulo si carichi correttamente
   - Testa il flusso di autenticazione

---

## Troubleshooting

### Il modulo non si carica

**Problema**: `Failed to load module`

**Soluzione**:
- Verifica che esista `modules/<nome>/index.js`
- Verifica che `index.js` esporti `{ mount }`
- Controlla la console per errori di import

### Redirect loop

**Problema**: Viene reindirizzato continuamente tra `/auth` e `/home`

**Soluzione**:
- Verifica che `checkAuth()` aggiorni correttamente lo stato `auth`
- Verifica la configurazione dei contesti in `modules.config.js`

### 404 Not Found

**Problema**: Hash URL non riconosciuto

**Soluzione**:
- Verifica che il path sia configurato in `modules.config.js`
- Verifica che il path inizi con `/` (es. `/home` non `home`)

---

## Conclusione

Questa architettura fornisce una base solida e scalabile per applicazioni web moderne con:
- **Gestione centralizzata del routing** tramite hash URLs e lazy loading
- **Controllo granulare dei permessi** basato su contesti (public, private, auth)
- **Separazione delle responsabilità**: il router gestisce solo routing e autenticazione binaria, i moduli gestiscono la propria business logic
- **Modularità**: ogni modulo è self-contained e intercambiabile
- **Facile manutenzione e estensibilità**: aggiungere nuove funzionalità richiede modifiche minime

### Design Pattern Chiave: Separazione Router/Moduli

**Il router è agnostico rispetto ai dettagli implementativi dei moduli.**

Questo significa che:
- Un modulo auth può essere sostituito con uno completamente diverso (OAuth, SAML, 2FA, biometrico) senza modificare il router
- Un modulo auth può implementare flussi multi-step arbitrariamente complessi
- Lo store globale `auth` rimane minimale e pulito (`isAuthenticated` + `user` essenziale)
- Ogni modulo può avere il proprio stato privato senza inquinare il contesto globale

**Esempio pratico**: Se domani decidi di usare OAuth invece del login con username/password, devi solo sostituire `modules/auth/` mantenendo il contratto:
```javascript
// Il nuovo modulo auth OAuth deve solo:
auth.set({ isAuthenticated: true, user: { ... } })
// quando l'autenticazione è completa
```

Il router, gli altri moduli e l'intera applicazione continuano a funzionare senza modifiche.

Per domande o suggerimenti, consulta la documentazione o apri una issue.
