import { LitElement, html } from 'lit';
import { UIRegistry, user } from '../../store.js';

/**
 * Pagina di default del dashboard.
 *
 * Mostra le schede statistiche registrate dai moduli tramite `UIRegistry.sidebarStats`.
 * Ogni voce ha la forma:
 *   { key, label, icon, color, value: string | (() => Promise<string>), minRuoloLevel }
 *
 * Le voci con `value` funzione vengono risolte asincronamente; durante il caricamento
 * mostrano "…" e aggiornano la card quando il Promise si risolve.
 */
class Stats extends LitElement {

  static properties = {
    _cards: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._cards = [];
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubStats = UIRegistry.sidebarStats.subscribe(() => this._loadCards());
    this._unsubUser  = user.subscribe(() => this._loadCards());
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubStats();
    this._unsubUser();
  }

  /**
   * Filtra le voci per ruolo corrente, risolve i valori asincroni e aggiorna lo stato.
   */
  _loadCards() {
    const level = user.get()?.ruolo_level ?? 0;
    const items = UIRegistry.sidebarStats.get().filter(item => level >= (item.minRuoloLevel ?? 0));

    this._cards = items.map(item => ({
      key:   item.key,
      label: item.label,
      icon:  item.icon,
      color: item.color,
      value: typeof item.value === 'function' ? '…' : (item.value ?? '—'),
    }));

    items.forEach((item, i) => {
      if (typeof item.value !== 'function') return;
      item.value().then(v => {
        this._cards = this._cards.map((c, j) => j === i ? { ...c, value: v } : c);
      }).catch(() => {
        this._cards = this._cards.map((c, j) => j === i ? { ...c, value: '—' } : c);
      });
    });
  }

  render() {
    return html`
      <div class="mb-4">
        <h1 class="h4 mb-1">Benvenuto</h1>
        <p class="text-muted small mb-0">Riepilogo attività</p>
      </div>

      ${this._cards.length > 0 ? html`
        <div class="row g-3 mb-4">
          ${this._cards.map(card => this._statCard(card.icon, card.label, card.value, card.color))}
        </div>
      ` : ''}
    `;
  }

  /**
   * @param {string} icon
   * @param {string} label
   * @param {string} value
   * @param {string} color
   * @returns {import('lit').TemplateResult}
   */
  _statCard(icon, label, value, color) {
    return html`
      <div class="col-sm-6 col-xl-3">
        <div class="card border-0 shadow-sm">
          <div class="card-body d-flex align-items-center gap-3">
            <div class="rounded-3 p-3 bg-${color} bg-opacity-10">
              <i class="bi ${icon} fs-4 text-${color}"></i>
            </div>
            <div>
              <div class="fw-bold fs-5">${value}</div>
              <div class="text-muted small">${label}</div>
            </div>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('dashboard-stats', Stats);

export default Stats;
