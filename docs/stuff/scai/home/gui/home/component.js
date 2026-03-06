import { LitElement, html, css } from 'lit';

class HomeComponent extends LitElement {

  static properties = {
    _menuItems: { state: true }
  };

  // Disabilita Shadow DOM: Bootstrap funziona normalmente
  createRenderRoot() { return this; }

  constructor() {
    super();
    this._menuItems = [
      {
        id: 'report',
        title: 'Report',
        icon: 'bi-file-text',
        route: '/report',
        enabled: false,
        description: 'Visualizza e genera report'
      },
      {
        id: 'rapporti',
        title: 'Rapporti',
        icon: 'bi-person-badge',
        route: '/rapporti',
        enabled: true,
        description: 'Gestione rapporti dipendenti'
      },
      {
        id: 'veicoli',
        title: 'Gestione veicoli',
        icon: 'bi-car-front',
        route: '/veicoli',
        enabled: true,
        description: 'Gestione anagrafica veicoli'
      },
      {
        id: 'policy-rapporti',
        title: 'Policies associate ai rapporti',
        icon: 'bi-globe2',
        route: '/policy',
        enabled: false,
        description: 'Gestione policy associate ai rapporti'
      },
      {
        id: 'policy-default',
        title: 'Assegnazione le policy di default ad un rapporto.',
        icon: 'bi-globe2',
        route: '/policy-default',
        enabled: false,
        description: 'Configura policy di default'
      },
      {
        id: 'badge',
        title: 'Badge',
        icon: 'bi-person-vcard',
        route: '/badge',
        enabled: false,
        description: 'Gestione badge accessi'
      },
      {
        id: 'richieste-badge',
        title: 'Richieste di assegnazione badge',
        icon: 'bi-person-plus',
        route: '/richieste-badge',
        enabled: false,
        description: 'Gestione richieste assegnazione badge'
      },
      {
        id: 'timbrature',
        title: 'Timbrature',
        icon: 'bi-clock-history',
        route: '/timbrature',
        enabled: false,
        description: 'Gestione timbrature e presenze'
      },
      {
        id: 'visitatori',
        title: 'Visitatori abituali',
        icon: 'bi-people',
        route: '/visitatori',
        enabled: false,
        description: 'Gestione visitatori abituali'
      },
      {
        id: 'anagrafiche',
        title: 'Anagrafiche',
        icon: 'bi-card-list',
        route: '/anagrafiche',
        enabled: false,
        description: 'Gestione anagrafiche di sistema'
      }
    ];
  }

  _navigateTo(route) {
    // Dispatch custom event per routing
    this.dispatchEvent(new CustomEvent('navigate', {
      detail: { route },
      bubbles: true,
      composed: true
    }));
  }

  render() {
    return html`
      <!-- Hero Header -->
      <div class="bg-success text-white py-5">
        <div class="container-fluid">
          <h1 class="display-5 fw-bold mb-2">Sistema Controllo Accessi Integrato</h1>
          <p class="lead mb-0">
            <i class="bi bi-shield-check me-2"></i>
            Sicurezza Sedi
          </p>
        </div>
      </div>

      <!-- Dashboard Cards -->
      <div class="bg-light min-vh-100 py-5">
        <div class="container-fluid">
          <div class="row g-4">
            ${this._menuItems.map(item => html`
              <div class="col-12 col-md-6 col-lg-4">
                <div
                  class="card h-100 shadow-sm dashboard-card ${item.enabled ? '' : 'dashboard-card-disabled'}"
                  @click=${() => item.enabled && this._navigateTo(item.route)}
                  role="button"
                  tabindex="${item.enabled ? '0' : '-1'}"
                  @keypress=${(e) => {
                    if (item.enabled && (e.key === 'Enter' || e.key === ' ')) {
                      this._navigateTo(item.route);
                    }
                  }}
                >
                  <div class="card-body d-flex flex-column align-items-center justify-content-center text-center py-5">
                    <div class="mb-4">
                      <i class="bi ${item.icon} ${item.enabled ? 'text-success' : 'text-muted'}" style="font-size: 4rem;"></i>
                    </div>
                    <h5 class="card-title fw-bold mb-2">
                      ${item.title}
                      ${!item.enabled ? html`<span class="badge bg-secondary ms-2">Prossimamente</span>` : ''}
                    </h5>
                    ${item.description ? html`
                      <p class="card-text text-muted small mb-0">${item.description}</p>
                    ` : ''}
                  </div>
                </div>
              </div>
            `)}
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('home-component', HomeComponent);

export default HomeComponent;
