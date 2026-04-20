import { LitElement, html } from 'lit';
import { UIRegistry } from '../../store.js';

// Inietta l'animazione una sola volta nel documento.
const _style = document.createElement('style');
_style.textContent = '@keyframes notifications-slide-in { from { transform: translateX(110%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }';
document.head.appendChild(_style);

/**
 * Contenitore notifiche a comparsa.
 *
 * Si monta su document.body (non in #header) tramite header/index.js.
 * Legge UIRegistry.notifications e mostra un popup per ogni voce.
 * I popup compaiono da destra in alto, si impilano verso il basso.
 * Un click sul popup lo chiude rimuovendolo dalla coda.
 */
class Notifications extends LitElement {

  static properties = {
    _items: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._items = UIRegistry.notifications.get();
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsub = UIRegistry.notifications.subscribe(v => { this._items = [...v]; });
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsub();
  }

  /**
   * Rimuove una notifica dalla coda per ID.
   *
   * @param {string} id
   */
  _dismiss(id) {
    UIRegistry.notifications.set(UIRegistry.notifications.get().filter(n => n.id !== id));
  }

  render() {
    if (this._items.length === 0) {
      return html``;
    }
    return html`
      <div style="position:fixed;top:1rem;right:1rem;z-index:9999;display:flex;flex-direction:column;gap:0.5rem;max-width:340px;pointer-events:none">
        ${this._items.map(n => html`
          <div class="alert alert-${n.type ?? 'info'} shadow d-flex align-items-start gap-2 mb-0"
               style="cursor:pointer;animation:notifications-slide-in 0.3s ease;pointer-events:auto"
               @click=${() => this._dismiss(n.id)}>
            <span class="flex-grow-1 small">${n.message}</span>
            <i class="bi bi-x-lg flex-shrink-0 ms-1" style="font-size:0.75rem;margin-top:2px"></i>
          </div>
        `)}
      </div>
    `;
  }
}

customElements.define('header-notifications', Notifications);
