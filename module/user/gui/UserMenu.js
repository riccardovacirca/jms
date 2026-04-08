import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';

/**
 * Dropdown utente per l'area "user" dell'header.
 * Mostra username e menu con Profilo, Account, Esci se autenticato,
 * pulsante Accedi altrimenti.
 * Si registra nello slot headerUserSlot tramite init.js del modulo user.
 */
class UserMenu extends LitElement {

  static properties = {
    _authorized:   { state: true },
    _user:         { state: true },
    _menuOpen:     { state: true }
  };

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
    this._unsubUser       = user.subscribe(v => { this._user = v; });
    this._onDocClick      = () => { this._menuOpen = false; };
    document.addEventListener('click', this._onDocClick);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubAuthorized();
    this._unsubUser();
    document.removeEventListener('click', this._onDocClick);
  }

  async _logout() {
    try {
      await fetch('/api/user/auth/logout', { method: 'POST' });
    } finally {
      sessionStorage.removeItem('redirectAfterLogin');
      authorized.set(false);
      user.set(null);
      window.location.hash = '/user/auth/login';
    }
  }

  /**
   * Apre/chiude il dropdown bloccando la propagazione per non triggerare
   * immediatamente il listener sul document.
   *
   * @param {Event} e - evento click
   */
  _toggle(e) {
    e.stopPropagation();
    this._menuOpen = !this._menuOpen;
  }

  render() {
    if (!this._authorized) {
      return html`
        <button class="btn btn-sm btn-outline-primary"
                @click=${() => { window.location.hash = '/user/auth/login'; }}>
          Accedi
        </button>
      `;
    }
    return html`
      <div class="position-relative">
        <button class="btn btn-link d-flex align-items-center gap-2 text-decoration-none text-body p-1"
                @click=${this._toggle}>
          <i class="bi bi-person-circle fs-5"></i>
          <span class="small">${this._user?.username}</span>
          <i class="bi bi-chevron-down" style="font-size:11px"></i>
        </button>
        ${this._menuOpen ? html`
          <ul class="dropdown-menu dropdown-menu-end show position-absolute"
              style="top:100%; right:0; min-width:160px;"
              @click=${e => e.stopPropagation()}>
            <li>
              <a class="dropdown-item" href="#/user/profile"
                 @click=${() => { this._menuOpen = false; }}>
                <i class="bi bi-person me-2"></i>Profilo
              </a>
            </li>
            <li>
              <a class="dropdown-item" href="#/user/account"
                 @click=${() => { this._menuOpen = false; }}>
                <i class="bi bi-gear me-2"></i>Account
              </a>
            </li>
            <li><hr class="dropdown-divider"></li>
            <li>
              <button class="dropdown-item text-danger" @click=${this._logout}>
                <i class="bi bi-box-arrow-right me-2"></i>Esci
              </button>
            </li>
          </ul>
        ` : ''}
      </div>
    `;
  }
}

customElements.define('user-menu', UserMenu);

export { UserMenu };
