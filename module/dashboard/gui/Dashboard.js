import { LitElement, html } from 'lit';
import './Sidebar.js';
import './Stats.js';

/**
 * Componente radice del dashboard.
 *
 * Organizza il layout in due aree:
 *   - sinistra: `<dashboard-sidebar>` con le voci registrate dai moduli
 *   - destra: area content in cui viene montato il custom element selezionato
 *
 * Il montaggio del componente avviene imperativamente in `updated()`,
 * identicamente a come `Header` monta lo slot user.
 */
class Dashboard extends LitElement {

  static properties = {
    _activeTag: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._activeTag  = 'dashboard-stats';
    this._currentTag = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._onReset = () => {
      this._currentTag = null;
      this._activeTag  = 'dashboard-stats';
    };
    window.addEventListener('dashboard-reset', this._onReset);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener('dashboard-reset', this._onReset);
  }

  /**
   * Gestisce la selezione di una voce dalla sidebar:
   * esegue il lazy import del modulo e imposta il tag da montare.
   *
   * @param {CustomEvent} e - evento `dashboard-select` con la voce come detail
   */
  _onSelect(e) {
    const item = e.detail;
    if (!item.tag || !item.import) {
      return;
    }
    this._activeTag = null;
    item.import().then(() => {
      this._activeTag = item.tag;
    });
  }

  /**
   * Monta il custom element attivo nel contenitore content.
   * Sostituisce l'elemento solo se il tag è cambiato.
   */
  updated() {
    const el = this.querySelector('#dashboard-content');
    if (!el || !this._activeTag) return;
    if (this._currentTag === this._activeTag) return;
    this._currentTag = this._activeTag;
    el.innerHTML = '';
    el.appendChild(document.createElement(this._activeTag));
  }

  render() {
    return html`
      <div class="d-flex" style="min-height: calc(100vh - 57px)">
        <dashboard-sidebar @dashboard-select=${this._onSelect}></dashboard-sidebar>
        <div id="dashboard-content" class="flex-grow-1 p-4 overflow-auto"></div>
      </div>
    `;
  }
}

customElements.define('dashboard-root', Dashboard);

export default Dashboard;
