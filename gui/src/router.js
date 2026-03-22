import { authorized } from './store.js';
import { MODULE_CONFIG, DEFAULT_MODULE } from './config.js';
import appInit from './init.js';

/**
 * Router SPA hash-based con supporto per container multipli e moduli persistent.
 *
 * Gestisce la navigazione leggendo window.location.hash e caricando
 * dinamicamente i moduli in base alla configurazione in MODULE_CONFIG.
 *
 * Architettura multi-container:
 *   - Ogni modulo dichiara il proprio `container` (ID elemento DOM)
 *   - Moduli persistent vengono montati all'avvio e non smontati mai
 *   - Moduli non-persistent vengono montati/smontati durante la navigazione
 *
 * Flusso di avvio:
 *   1. _init() esegue appInit() e le procedure init dichiarate in MODULE_CONFIG
 *   2. _mountPersistentModules() monta tutti i moduli con persistent: true
 *   3. authorized.subscribe() fa scattare route() per gestire moduli dinamici
 *
 * Controllo accessi:
 *   - authorization: null (rotta libera)
 *   - authorization: { redirectTo } (rotta protetta)
 *     se non autorizzato reindirizza a redirectTo (se registrata) o mostra 403
 */
class Router
{
  /**
   * Inizializza i container per ciascuna area e avvia _init().
   * Gli event listener vengono registrati solo al termine di _init() per
   * garantire che authorized e user siano già impostati al primo routing.
   */
  constructor() {
    this.containers = {};          // Map: containerId -> HTMLElement
    this.currentModule = null;     // Nome del modulo corrente montato nell'area main
    this.currentModuleInstance = null;  // Istanza del modulo corrente (per chiamare unmount())
    this._navId = 0;
    this._init();
  }

  /**
   * Esegue in parallelo le procedure init dichiarate in MODULE_CONFIG,
   * monta i moduli persistent, poi registra gli event listener e avvia il routing.
   *
   * Le init vengono eseguite prima di qualsiasi montaggio modulo per garantire
   * che lo stato condiviso (es. sessione utente) sia corretto.
   */
  async _init() {
    appInit();

    // Esegui tutte le init in parallelo.
    // init può essere una funzione (chiamata direttamente) o true (carica automaticamente
    // ./modules/<path>/init.js ed esegue il default export).
    await Promise.all(
      Object.entries(MODULE_CONFIG)
        .filter(([_, c]) => c.init)
        .map(([_, c]) => {
          if (typeof c.init === 'function') return c.init();
          return import(`./module/${c.path}/init.js`).then(m => m.default());
        })
    );

    // Monta i moduli persistent (es. header, footer)
    await this._mountPersistentModules();

    // Registra listener per navigazione
    window.addEventListener('hashchange', () => this.route());

    // nanostores chiama il subscriber immediatamente con il valore corrente,
    // quindi route() parte subito senza bisogno di un init() esplicito.
    authorized.subscribe(() => this.route());
  }

  /**
   * Monta tutti i moduli con persistent: true nei loro container.
   * I moduli persistent rimangono sempre montati e non vengono mai smontati.
   * L'ordine di montaggio è determinato dall'attributo priority (più basso = per primo).
   */
  async _mountPersistentModules() {
    const persistentModules = Object.entries(MODULE_CONFIG)
      .filter(([_, config]) => config.persistent)
      .sort(([_, a], [__, b]) => (a.priority || 999) - (b.priority || 999));

    for (const [moduleName, config] of persistentModules) {
      if (config.path === null) {
        throw new Error(`[Router] Module "${moduleName}" is persistent but has path: null — persistent modules require a path`);
      }
      try {
        const module = await import(`./module/${config.path}/index.js`);
        const container = this._getContainer(config.container);
        if (container) {
          container.innerHTML = '';
          module.default.mount(container);
        }
      } catch (err) {
        console.warn(`[Router] Failed to load persistent module "${moduleName}":`, err);
      }
    }
  }

  /**
   * Ottiene il riferimento a un container DOM per ID.
   * Crea una cache per evitare lookup ripetuti.
   *
   * @param {string} containerId - ID dell'elemento DOM
   * @returns {HTMLElement|null} - Elemento DOM o null se non trovato
   */
  _getContainer(containerId) {
    if (!this.containers[containerId]) {
      this.containers[containerId] = document.getElementById(containerId);
    }
    return this.containers[containerId];
  }

  /**
   * Determina il modulo da caricare in base all'hash corrente e allo stato
   * di autorizzazione, poi delega a loadModule().
   *
   * Gestisce solo moduli non-persistent (i persistent sono già montati).
   * Salta il caricamento se il modulo richiesto è già montato e l'utente
   * è ancora autorizzato.
   */
  route() {
    const hash = window.location.hash.slice(1).split('?')[0];

    // /#/ non è una rotta valida: reindirizza alla root
    if (hash === '/') {
      window.location.replace('/');
      return;
    }

    const moduleName = hash
      ? Object.keys(MODULE_CONFIG).find(name => {
          const route = MODULE_CONFIG[name].route;
          if (!route) return false;
          return hash === route || hash.startsWith(route + '/');
        })
      : DEFAULT_MODULE;

    if (!moduleName) {
      this.currentModule = null;
      this.showStatus('main', '404 — Pagina non trovata');
      return;
    }

    const config = MODULE_CONFIG[moduleName];

    // Moduli persistent non vengono gestiti dal routing
    if (config.persistent) {
      return;
    }

    const isAuthorized = config.authorization === null || authorized.get();

    if (moduleName === this.currentModule && isAuthorized) return;

    this.loadModule(moduleName);
  }

  /**
   * Carica e monta il modulo indicato nel suo container specifico.
   *
   * Prima del caricamento verifica l'autorizzazione: se la rotta è protetta
   * e l'utente non è autorizzato, esegue il redirect o mostra 403.
   *
   * Usa un contatore (_navId) per gestire navigazioni rapide: se durante
   * l'import() arriva una nuova navigazione, il risultato stale viene scartato.
   *
   * @param {string} moduleName - Chiave in MODULE_CONFIG del modulo da caricare.
   */
  async loadModule(moduleName) {
    const config = MODULE_CONFIG[moduleName];

    if (config.authorization !== null && !authorized.get()) {
      const redirectTo     = config.authorization?.redirectTo;
      const redirectExists = redirectTo && Object.values(MODULE_CONFIG).some(
        m => m.route === redirectTo
      );

      if (redirectExists) {
        window.location.hash = redirectTo;
      } else {
        this.currentModule = null;
        this.showStatus(config.container, '403 — Accesso negato');
      }

      return;
    }

    if (config.path === null) {
      this.currentModule = moduleName;
      return;
    }

    this.showStatus(config.container, 'Caricamento...');

    const navId = ++this._navId;
    try {
      const module = await import(`./module/${config.path}/index.js`);
      if (navId !== this._navId) return;

      const container = this._getContainer(config.container);
      if (container) {
        this.currentModuleInstance?.unmount?.();
        container.innerHTML = '';
        module.default.mount(container);
        this.currentModule = moduleName;
        this.currentModuleInstance = module.default;
      } else {
        console.error(`[Router] Container "${config.container}" not found for module "${moduleName}"`);
        this.showStatus('main', 'Errore di configurazione');
      }
    } catch (err) {
      if (navId !== this._navId) return;
      this.currentModule = null;
      this.currentModuleInstance = null;
      this.showStatus(config.container, 'Modulo non disponibile');
      console.error(`[Router] Failed to load module "${moduleName}":`, err);
    }
  }

  /**
   * Mostra un messaggio di stato (caricamento, errore, 404, 403) in un container.
   *
   * @param {string} containerId - ID del container
   * @param {string} message - Testo da visualizzare
   */
  showStatus(containerId, message) {
    const container = this._getContainer(containerId);
    if (container) {
      container.innerHTML = `
        <div style="display:flex;align-items:center;justify-content:center;min-height:100vh">
          <p style="color:#888;font-family:sans-serif">${message}</p>
        </div>
      `;
    }
  }
}

new Router();
