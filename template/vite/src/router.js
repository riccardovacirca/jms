import { auth, checkAuth } from './store.js';
import { MODULE_CONFIG, CONTEXT_CONFIG, DEFAULT_ROUTES } from './modules.config.js';

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
    const path = window.location.hash.slice(1) || DEFAULT_ROUTES.root;

    console.info('[Router] Navigating to', path);

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

    // Verifica SOLO se l'utente è autenticato (binario)
    if (context.requiresAuth && !auth.state.isAuthenticated) {
      console.info('[Router] Unauthorized, redirecting to', context.redirectTo);
      window.location.hash = context.redirectTo;
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

      console.info('[Router] Module loaded:', moduleName);
    } catch (err) {
      console.warn('[Router] Module not found, showing fallback:', moduleName, err.message);
      this.showFallback(config.context, config.title);
    }
  }

  /**
   * Gestisce i cambiamenti nello stato di autenticazione.
   * Reindirizza se necessario in base al contesto del modulo corrente.
   */
  handleAuthChange() {
    const currentPath = window.location.hash.slice(1) || DEFAULT_ROUTES.root;
    const moduleName = Object.keys(MODULE_CONFIG).find(
      name => MODULE_CONFIG[name].path === currentPath
    );

    if (moduleName) {
      const config = MODULE_CONFIG[moduleName];
      const context = CONTEXT_CONFIG[config.context];

      // Se siamo in un modulo privato e non siamo più autenticati
      if (context.requiresAuth && !auth.state.isAuthenticated) {
        window.location.hash = DEFAULT_ROUTES.unauthorized;
      }

      // Se siamo loggati e siamo su auth, reindirizza alla home
      // (Il modulo auth gestisce internamente quando settare isAuthenticated = true)
      if (auth.state.isAuthenticated && config.context === 'auth') {
        window.location.hash = DEFAULT_ROUTES.afterLogin;
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
        </div>
      </div>
    `;
  }

  /**
   * Mostra fallback quando il modulo non esiste
   */
  showFallback(contextName, pageTitle) {
    const context = CONTEXT_CONFIG[contextName];
    document.title = context.fallbackTitle || pageTitle || 'App';
    
    this.container.innerHTML = `
      <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light">
        <div class="text-center">
          <h1 class="display-4 text-muted">${context.fallbackTitle || pageTitle}</h1>
          <p class="lead text-muted">${context.fallbackMessage || 'Modulo non configurato'}</p>
          <p class="text-muted small">Contesto: <code>${contextName}</code></p>
        </div>
      </div>
    `;
  }
}

// Avvia il router
new Router();
