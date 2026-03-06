import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';

/**
 * Header component - modulo persistent visualizzato sempre.
 * Mostra contenuti diversi in base allo stato di autenticazione.
 */
class HeaderComponent extends LitElement {

  static properties = {
    _authorized: { state: true },
    _user:       { state: true }
  };

  // Disabilita Shadow DOM: Bootstrap funziona normalmente
  createRenderRoot() { return this; }

  constructor() {
    super();
    this._authorized = authorized.get();
    this._user       = user.get();
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

  render() {
    return html`
      <header class="d-flex align-items-center px-4 py-3 bg-white border-bottom">
        <a href="#/home" class="text-decoration-none text-dark">
          <span class="fw-bold fs-5">App</span>
        </a>
        <div class="ms-auto d-flex align-items-center gap-2">
          ${this._authorized
            ? html`
                <span class="text-muted small">${this._user?.username}</span>
                <button class="btn btn-sm btn-outline-danger" @click=${this._logout}>Esci</button>`
            : html`
                <button class="btn btn-sm btn-outline-primary"
                        @click=${() => window.location.hash = '/auth'}>Accedi</button>`
          }
        </div>
      </header>
    `;
  }
}

customElements.define('header-component', HeaderComponent);

export default HeaderComponent;
