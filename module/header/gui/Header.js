import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';

/**
 * Header component - modulo persistent visualizzato sempre.
 * Mostra contenuti diversi in base allo stato di autenticazione.
 * Su schermi piccoli collassa in un menu hamburger.
 */
class Header extends LitElement {

  static properties = {
    _authorized:    { state: true },
    _user:          { state: true },
    _menuOpen:      { state: true },
    _userMenuOpen:  { state: true },
    _theme:         { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._authorized   = authorized.get();
    this._user         = user.get();
    this._menuOpen     = false;
    this._userMenuOpen = false;
    this._theme        = 'light';
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubAuthorized = authorized.subscribe(v => { this._authorized = v; });
    this._unsubUser       = user.subscribe(v => { this._user = v; });
    this._onDocClick      = () => { this._userMenuOpen = false; };
    document.addEventListener('click', this._onDocClick);
    const saved = localStorage.getItem('bs-theme') || 'light';
    this._theme = saved;
    document.documentElement.dataset.bsTheme = saved;
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
      authorized.set(false);
      user.set(null);
      window.location.hash = '/user/auth/login';
    }
  }

  _toggleMenu() {
    this._menuOpen = !this._menuOpen;
  }

  _closeMenu() {
    this._menuOpen = false;
  }

  /**
   * Apre/chiude il dropdown utente bloccando la propagazione per non
   * triggerare immediatamente il listener sul document.
   *
   * @param {Event} e evento click
   */
  _toggleUserMenu(e) {
    e.stopPropagation();
    this._userMenuOpen = !this._userMenuOpen;
  }

  /**
   * Alterna il tema chiaro/scuro, aggiorna data-bs-theme su <html>
   * e persiste la scelta in localStorage.
   */
  _toggleTheme() {
    const next = this._theme === 'dark' ? 'light' : 'dark';
    this._theme = next;
    document.documentElement.dataset.bsTheme = next;
    localStorage.setItem('bs-theme', next);
  }

  render() {
    return html`
      <nav class="navbar navbar-expand-md bg-body border-bottom px-3">
        <a class="navbar-brand fw-bold" href="/" @click=${this._closeMenu}>App</a>
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
          <div class="d-flex align-items-center gap-1 pb-2 pb-md-0">
            <button class="btn btn-link p-1 text-body" @click=${this._toggleTheme}
                    title="${this._theme === 'dark' ? 'Tema chiaro' : 'Tema scuro'}">
              <i class="bi ${this._theme === 'dark' ? 'bi-sun' : 'bi-moon'}"></i>
            </button>
            ${this._authorized
              ? html`
                  <div class="position-relative">
                    <button class="btn btn-link d-flex align-items-center gap-2 text-decoration-none text-body p-1"
                            @click=${this._toggleUserMenu}>
                      <i class="bi bi-person-circle fs-5"></i>
                      <span class="small">${this._user?.username}</span>
                      <i class="bi bi-chevron-down" style="font-size:11px"></i>
                    </button>
                    ${this._userMenuOpen ? html`
                      <ul class="dropdown-menu dropdown-menu-end show position-absolute"
                          style="top:100%; right:0; min-width:160px;"
                          @click=${e => e.stopPropagation()}>
                        <li>
                          <a class="dropdown-item" href="#/user/profile"
                             @click=${() => { this._userMenuOpen = false; this._closeMenu(); }}>
                            <i class="bi bi-person me-2"></i>Profilo
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
                  </div>`
              : html`
                  <button class="btn btn-sm btn-outline-primary"
                          @click=${() => { window.location.hash = '/user/auth/login'; this._closeMenu(); }}>
                    Accedi
                  </button>`
            }
          </div>
        </div>
      </nav>
    `;
  }
}

customElements.define('header-component', Header);

export default Header;
