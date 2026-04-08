import { LitElement, html } from 'lit';
import { ref } from 'lit/directives/ref.js';
import { authorized, headerNavItems, headerUserSlot } from '../../store.js';

/**
 * Header component — barra di navigazione persistente a 4 aree fisse:
 *   logo | link | tema | user
 *
 * - logo: testo configurabile tramite attributo `app-name` (default: "App").
 * - link: popolata dai moduli tramite lo store `headerNavItems`.
 *         Ogni modulo registra al massimo un item (link singolo o dropdown).
 * - tema: pulsante light/dark fisso.
 * - user: slot occupabile da un solo modulo tramite lo store `headerUserSlot`.
 *         Il modulo registra il tag del proprio custom element già definito.
 */
class Header extends LitElement {

  static properties = {
    'app-name':  {},
    _authorized: { state: true },
    _navItems:   { state: true },
    _userSlot:   { state: true },
    _menuOpen:   { state: true },
    _theme:      { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this['app-name'] = 'App';
    this._authorized = authorized.get();
    this._navItems   = headerNavItems.get();
    this._userSlot   = headerUserSlot.get();
    this._menuOpen   = false;
    this._theme      = 'light';
    this._currentTag = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubAuthorized = authorized.subscribe(v => { this._authorized = v; });
    this._unsubNavItems   = headerNavItems.subscribe(v => { this._navItems = v; });
    this._unsubUserSlot   = headerUserSlot.subscribe(v => {
      this._currentTag = null;
      this._userSlot = v;
    });
    this._onDocClick = () => { this._openDropdown = null; this.requestUpdate(); };
    document.addEventListener('click', this._onDocClick);
    const saved = localStorage.getItem('bs-theme') || 'light';
    this._theme = saved;
    document.documentElement.dataset.bsTheme = saved;
    this._openDropdown = null;
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubAuthorized();
    this._unsubNavItems();
    this._unsubUserSlot();
    document.removeEventListener('click', this._onDocClick);
  }

  _closeMenu() {
    this._menuOpen = false;
  }

  _toggleMenu() {
    this._menuOpen = !this._menuOpen;
  }

  /**
   * Alterna il tema chiaro/scuro e persiste la scelta in localStorage.
   */
  _toggleTheme() {
    const next = this._theme === 'dark' ? 'light' : 'dark';
    this._theme = next;
    document.documentElement.dataset.bsTheme = next;
    localStorage.setItem('bs-theme', next);
  }

  /**
   * Apre/chiude il dropdown di un nav item identificato da id.
   *
   * @param {Event} e - evento click
   * @param {string} id - identificatore dell'item
   */
  _toggleDropdown(e, id) {
    e.stopPropagation();
    this._openDropdown = this._openDropdown === id ? null : id;
    this.requestUpdate();
  }

  /**
   * Monta il custom element registrato nello slot user all'interno del
   * contenitore wrapper. Sostituisce l'elemento solo se il tag è cambiato.
   *
   * @param {Element|undefined} el - elemento wrapper fornito da ref()
   */
  _mountUserSlot(el) {
    if (!el || !this._userSlot) return;
    const tag = this._userSlot.tag;
    if (this._currentTag === tag) return;
    this._currentTag = tag;
    el.innerHTML = '';
    el.appendChild(document.createElement(tag));
  }

  /**
   * Renderizza un singolo item dell'area link:
   * link semplice oppure dropdown con sottovoci.
   *
   * @param {Object} item - voce di navigazione
   * @returns {import('lit').TemplateResult}
   */
  _renderNavItem(item) {
    if (item.items) {
      const open = this._openDropdown === item.id;
      return html`
        <li class="nav-item dropdown">
          <button class="nav-link dropdown-toggle btn btn-link"
                  @click=${e => this._toggleDropdown(e, item.id)}>
            ${item.label}
          </button>
          ${open ? html`
            <ul class="dropdown-menu show">
              ${item.items.map(sub => html`
                <li>
                  <a class="dropdown-item" href="${sub.href}"
                     @click=${this._closeMenu}>${sub.label}</a>
                </li>
              `)}
            </ul>
          ` : ''}
        </li>
      `;
    }
    return html`
      <li class="nav-item">
        <a class="nav-link" href="${item.href}" @click=${this._closeMenu}>${item.label}</a>
      </li>
    `;
  }

  render() {
    const visibleItems = this._navItems.filter(item => !item.auth || this._authorized);

    return html`
      <nav class="navbar navbar-expand-md bg-body border-bottom px-3">

        <!-- area: logo -->
        <a class="navbar-brand fw-bold" href="/" @click=${this._closeMenu}>
          ${this['app-name']}
        </a>

        <button class="navbar-toggler" type="button" @click=${this._toggleMenu}
                aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse ${this._menuOpen ? 'show' : ''}">

          <!-- area: link -->
          <ul class="navbar-nav me-auto mb-2 mb-md-0">
            ${visibleItems.map(item => this._renderNavItem(item))}
          </ul>

          <div class="d-flex align-items-center gap-1 pb-2 pb-md-0">

            <!-- area: tema -->
            <button class="btn btn-link p-1 text-body" @click=${this._toggleTheme}
                    title="${this._theme === 'dark' ? 'Tema chiaro' : 'Tema scuro'}">
              <i class="bi ${this._theme === 'dark' ? 'bi-sun' : 'bi-moon'}"></i>
            </button>

            <!-- area: user -->
            ${this._userSlot
              ? html`<div ${ref(el => this._mountUserSlot(el))}></div>`
              : ''
            }

          </div>
        </div>
      </nav>
    `;
  }
}

customElements.define('header-component', Header);

export default Header;
