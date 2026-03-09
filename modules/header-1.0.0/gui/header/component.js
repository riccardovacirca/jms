import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';

/**
 * Header component - modulo persistent visualizzato sempre.
 * Mostra contenuti diversi in base allo stato di autenticazione.
 * Su schermi piccoli collassa in un menu hamburger.
 */
class HeaderComponent extends LitElement {

  static properties = {
    _authorized: { state: true },
    _user:       { state: true },
    _menuOpen:   { state: true }
  };

  // Disabilita Shadow DOM: Bootstrap funziona normalmente
  createRenderRoot() { return this; }

  constructor() {
    super();
    this._authorized = authorized.get();
    this._user       = user.get();
    this._menuOpen   = false;
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubAuthorized = authorized.subscribe(v => { this._authorized = v; });
    this._unsubUser = user.subscribe(v => { this._user = v; });
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubAuthorized();
    this._unsubUser();
  }

  async _logout() {
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      authorized.set(false);
      user.set(null);
    }
  }

  _toggleMenu() {
    this._menuOpen = !this._menuOpen;
  }

  _closeMenu() {
    this._menuOpen = false;
  }

  render() {
    return html`
      <nav class="navbar navbar-expand-md bg-white border-bottom px-3">
        <a class="navbar-brand fw-bold" href="#/home" @click=${this._closeMenu}>App</a>
        <button class="navbar-toggler" type="button" @click=${this._toggleMenu}
                aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse ${this._menuOpen ? 'show' : ''}">
          <ul class="navbar-nav me-auto mb-2 mb-md-0">
            ${this._authorized
              ? html`
                  <li class="nav-item">
                    <a href="#/contatti" class="nav-link" @click=${this._closeMenu}>Contatti</a>
                  </li>`
              : ''
            }
          </ul>
          <div class="d-flex align-items-center gap-2 pb-2 pb-md-0">
            ${this._authorized
              ? html`
                  <span class="text-muted small">${this._user?.username}</span>
                  <button class="btn btn-sm btn-outline-danger" @click=${this._logout}>Esci</button>`
              : html`
                  <button class="btn btn-sm btn-outline-primary"
                          @click=${() => { window.location.hash = '/auth'; this._closeMenu(); }}>
                    Accedi
                  </button>`
            }
          </div>
        </div>
      </nav>
    `;
  }
}

customElements.define('header-component', HeaderComponent);

export default HeaderComponent;
