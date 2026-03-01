import { authorized } from './store.js';
import { MODULE_CONFIG, DEFAULT_ROUTE } from './config.js';

/**
 * Router SPA hash-based.
 *
 * Gestisce la navigazione leggendo window.location.hash e caricando
 * dinamicamente il modulo corrispondente in MODULE_CONFIG.
 *
 * Flusso di avvio:
 *   1. _init() esegue le procedure init dichiarate in MODULE_CONFIG
 *   2. authorized.subscribe() fa scattare route() con lo stato già corretto
 *
 * Controllo accessi:
 *   - authorization: null (rotta libera)
 *   - authorization: { redirectTo } (rotta protetta)
 *     se non autorizzato reindirizza a redirectTo (se registrata) o mostra 403
 */
class Router
{
  /**
   * Inizializza il container, il tracking del modulo corrente e avvia _init().
   * Gli event listener vengono registrati solo al termine di _init() per
   * garantire che lo stato condiviso sia già corretto al primo routing.
   */
  constructor() {
    this.container     = document.getElementById('app');
    this.currentModule = null;
    this._navId        = 0;
    this._init();
  }

  /**
   * Esegue in parallelo le procedure init dichiarate in MODULE_CONFIG,
   * poi registra gli event listener e avvia il routing.
   *
   * Le init vengono caricate tramite dynamic import (solo il file init.js
   * del modulo, non il modulo intero) e completate prima del primo route().
   * Questo garantisce che lo stato condiviso sia già corretto quando il
   * router decide quale modulo caricare.
   */
  async _init() {
    await Promise.all(
      Object.values(MODULE_CONFIG)
        .filter(c => c.init)
        .map(c => c.init())
    );

    window.addEventListener('hashchange', () => this.route());

    // nanostores chiama il subscriber immediatamente con il valore corrente,
    // quindi route() parte subito senza bisogno di un init() esplicito.
    authorized.subscribe(() => this.route());
  }

  /**
   * Determina il modulo da caricare in base all'hash corrente e allo stato
   * di autorizzazione, poi delega a loadModule().
   *
   * Salta il caricamento se il modulo richiesto è già montato e l'utente
   * è ancora autorizzato: evita flash di "Caricamento..." e double render
   * quando authorized cambia su rotte libere.
   */
  route() {
    const path = window.location.hash.slice(1) || DEFAULT_ROUTE;

    const moduleName = Object.keys(MODULE_CONFIG).find(
      name => MODULE_CONFIG[name].path === path
    );

    if (!moduleName) {
      this.currentModule = null;
      this.showStatus('404 — Pagina non trovata');
      return;
    }

    const config       = MODULE_CONFIG[moduleName];
    const isAuthorized = config.authorization === null || authorized.get();

    if (moduleName === this.currentModule && isAuthorized) return;

    this.loadModule(moduleName);
  }

  /**
   * Carica e monta il modulo indicato nel container principale.
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
        m => m.path === redirectTo
      );

      if (redirectExists) {
        window.location.hash = redirectTo;
      } else {
        this.currentModule = null;
        this.showStatus('403 — Accesso negato');
      }

      return;
    }

    this.showStatus('Caricamento...');

    const navId = ++this._navId;
    try {
      const module = await import(`./modules/${moduleName}/index.js`);
      if (navId !== this._navId) return;
      this.container.innerHTML = '';
      module.default.mount(this.container);
      this.currentModule = moduleName;
    } catch (err) {
      if (navId !== this._navId) return;
      this.currentModule = null;
      this.showStatus('Modulo non disponibile');
    }
  }

  /**
   * Mostra un messaggio di stato (caricamento, errore, 404, 403) nel container.
   *
   * @param {string} message - Testo da visualizzare.
   */
  showStatus(message) {
    this.container.innerHTML = `
      <div style="display:flex;align-items:center;justify-content:center;min-height:100vh">
        <p style="color:#888;font-family:sans-serif">${message}</p>
      </div>
    `;
  }
}

new Router();
