import { authorized } from './store.js';
import { MODULE_CONFIG, DEFAULT_ROUTE } from './config.js';

class Router {
  constructor() {
    this.container = document.getElementById('app');

    window.addEventListener('hashchange', () => this.route());

    // nanostores chiama il subscriber immediatamente con il valore corrente,
    // quindi route() parte subito senza bisogno di un init() esplicito.
    authorized.subscribe(() => this.route());
  }

  route() {
    const path = window.location.hash.slice(1) || DEFAULT_ROUTE;

    const moduleName = Object.keys(MODULE_CONFIG).find(
      name => MODULE_CONFIG[name].path === path
    );

    if (!moduleName) {
      this.showStatus('404 — Pagina non trovata');
      return;
    }

    this.loadModule(moduleName);
  }

  async loadModule(moduleName) {
    const config = MODULE_CONFIG[moduleName];

    if (config.authorization !== null && !authorized.get()) {
      const redirectTo = config.authorization?.redirectTo;
      const redirectExists = redirectTo && Object.values(MODULE_CONFIG).some(
        m => m.path === redirectTo
      );

      if (redirectExists) {
        window.location.hash = redirectTo;
      } else {
        this.showStatus('403 — Accesso negato');
      }
      return;
    }

    this.showStatus('Caricamento...');

    try {
      const module = await import(`./modules/${moduleName}/index.js`);
      this.container.innerHTML = '';
      module.default.mount(this.container);
    } catch (err) {
      this.showStatus('Modulo non disponibile');
    }
  }

  showStatus(message) {
    this.container.innerHTML = `
      <div style="display:flex;align-items:center;justify-content:center;min-height:100vh">
        <p style="color:#888;font-family:sans-serif">${message}</p>
      </div>
    `;
  }
}

new Router();
