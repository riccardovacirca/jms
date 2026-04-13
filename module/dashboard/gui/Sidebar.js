import { LitElement, html } from 'lit';
import { dashboardItems, user } from '../../store.js';

/**
 * Sidebar del dashboard.
 *
 * Ogni voce top-level è un pulsante a larghezza piena.
 * Se ha voci figlie, il click espande un accordion sotto di essa (uno alla volta).
 * Se non ha voci figlie e ha un `tag`, il click la seleziona direttamente.
 *
 * Struttura dati `dashboardItems`:
 *   - voce normale:  `{ key, label, icon, tag, import, minRuoloLevel }`
 *   - gruppo:        `{ key, label, icon, group: true, minRuoloLevel }` — solo toggle accordion
 *   - voce figlia:   `{ key, label, icon, tag, import, parent: 'parentKey', minRuoloLevel }`
 */
class Sidebar extends LitElement {

  static properties = {
    _items:       { state: true },
    _activeKey:   { state: true },
    _expandedKey: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._items       = dashboardItems.get();
    this._activeKey   = null;
    this._expandedKey = null;
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
   * Gestisce il click su una voce top-level.
   * Se ha figli: toglie/apre accordion (uno alla volta).
   * Se non ha figli e ha tag: la seleziona direttamente.
   *
   * @param {Object} item
   * @param {Array}  kids voci figlie visibili
   */
  _clickTop(item, kids) {
    if (kids.length > 0) {
      this._expandedKey = this._expandedKey === item.key ? null : item.key;
    } else if (!item.group && item.tag && item.import) {
      this._expandedKey = null;
      this._select(item);
    }
  }

  /**
   * Seleziona una voce foglia e notifica il componente padre.
   *
   * @param {Object} item
   */
  _select(item) {
    this._activeKey = item.key;
    this.dispatchEvent(new CustomEvent('dashboard-select', {
      detail:   item,
      bubbles:  true,
      composed: true,
    }));
  }

  render() {
    const visible    = this._visibleItems();
    const topLevel   = visible.filter(item => !item.parent);
    const childrenOf = key => visible.filter(item => item.parent === key);

    return html`
      <nav class="d-flex flex-column bg-body-tertiary border-end flex-shrink-0"
           style="width:220px;min-height:100%">
        <div class="p-2 d-flex flex-column gap-1">
          ${topLevel.length === 0 ? html`
            <span class="text-body-tertiary fst-italic small px-2 py-1">Nessuna voce</span>
          ` : topLevel.map(item => {
            const kids        = childrenOf(item.key);
            const isExpanded  = this._expandedKey === item.key;
            const hasActive   = kids.some(k => k.key === this._activeKey);
            const isActive    = kids.length === 0 && this._activeKey === item.key;

            return html`
              <div>
                <button
                  class="btn btn-sm w-100 text-start d-flex justify-content-between align-items-center
                         ${hasActive || isActive ? 'btn-primary' : 'btn-outline-secondary'}"
                  @click=${() => this._clickTop(item, kids)}>
                  <span>
                    ${item.icon ? html`<i class="bi ${item.icon} me-1"></i>` : ''}
                    ${item.label}
                  </span>
                  ${kids.length > 0 ? html`
                    <i class="bi ${isExpanded ? 'bi-chevron-up' : 'bi-chevron-down'} small ms-1"></i>
                  ` : ''}
                </button>

                ${isExpanded ? html`
                  <div class="d-flex flex-column gap-1 pt-1">
                    ${kids.map(child => html`
                      <button
                        class="btn btn-sm w-100 text-start
                               ${this._activeKey === child.key ? 'btn-primary' : 'btn-outline-secondary'}"
                        @click=${() => this._select(child)}>
                        ${child.icon ? html`<i class="bi ${child.icon} me-1"></i>` : ''}
                        ${child.label}
                      </button>
                    `)}
                  </div>
                ` : ''}
              </div>
            `;
          })}
        </div>
      </nav>
    `;
  }
}

customElements.define('dashboard-sidebar', Sidebar);

export default Sidebar;
