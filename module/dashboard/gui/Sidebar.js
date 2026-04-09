import { LitElement, html } from 'lit';
import { dashboardItems, user } from '../../store.js';

/**
 * Sidebar del dashboard.
 *
 * Legge le voci registrate in `dashboardItems` e filtra quelle visibili
 * in base al ruolo dell'utente corrente (`minRuoloLevel`).
 * Emette l'evento `dashboard-select` con la voce selezionata come detail.
 */
class Sidebar extends LitElement {

  static properties = {
    _items:     { state: true },
    _activeKey: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._items     = dashboardItems.get();
    this._activeKey = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubItems = dashboardItems.subscribe(v => { this._items = v; });
    this._unsubUser  = user.subscribe(() => { this.requestUpdate(); });
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubItems();
    this._unsubUser();
  }

  /**
   * Restituisce le voci visibili per il ruolo corrente.
   *
   * @returns {Array}
   */
  _visibleItems() {
    const level = user.get()?.ruolo_level ?? 0;
    return this._items.filter(item => level >= (item.minRuoloLevel ?? 0));
  }

  /**
   * Seleziona una voce e notifica il componente padre.
   *
   * @param {Event} e
   * @param {Object} item
   */
  _select(e, item) {
    e.preventDefault();
    this._activeKey = item.key;
    this.dispatchEvent(new CustomEvent('dashboard-select', {
      detail:   item,
      bubbles:  true,
      composed: true,
    }));
  }

  render() {
    const items = this._visibleItems();
    return html`
      <nav class="d-flex flex-column bg-body-tertiary border-end flex-shrink-0"
           style="width: 220px; min-height: 100%">
        <ul class="nav flex-column p-2 gap-1">
          ${items.map(item => html`
            <li class="nav-item">
              <a href="#"
                 class="nav-link rounded ${this._activeKey === item.key ? 'active' : 'text-body'}"
                 @click=${e => this._select(e, item)}>
                <i class="bi ${item.icon} me-2"></i>${item.label}
              </a>
            </li>
          `)}
          ${items.length === 0 ? html`
            <li class="nav-item">
              <span class="nav-link text-body-tertiary fst-italic small">Nessuna voce</span>
            </li>
          ` : ''}
        </ul>
      </nav>
    `;
  }
}

customElements.define('dashboard-sidebar', Sidebar);

export default Sidebar;
