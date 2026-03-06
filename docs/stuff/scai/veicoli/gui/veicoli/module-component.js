import { LitElement, html } from 'lit';

export class VeicoliModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'veicoli';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Veicoli</h2>
        <veicoli-component></veicoli-component>
      </div>
    `;
  }
}

customElements.define('veicoli-module', VeicoliModuleComponent);
