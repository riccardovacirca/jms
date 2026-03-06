import { LitElement, html } from 'lit';

class RapportiModuleComponent extends LitElement {

  static properties = {
    _tab: { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._tab = 'rapporti';
  }

  render() {
    return html`
      <div>
        <ul class="nav nav-tabs px-3 pt-3 bg-white border-bottom">
          <li class="nav-item">
            <button class="nav-link ${this._tab === 'rapporti' ? 'active' : ''}"
                    @click=${() => { this._tab = 'rapporti'; }}>Rapporti</button>
          </li>
          <!-- Aggiungere qui altri tab se necessario, es. Badge, Policy, Veicoli -->
        </ul>
        ${this._tab === 'rapporti'
          ? html`<rapporti-layout></rapporti-layout>`
          : ''}
      </div>`;
  }
}

customElements.define('rapporti-module', RapportiModuleComponent);

export default RapportiModuleComponent;
