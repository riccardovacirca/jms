import { LitElement, html } from 'lit';

export class SdcModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'sdc';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Sistemi di Campo</h2>
        <sdc-component></sdc-component>
      </div>
    `;
  }
}

customElements.define('sdc-module', SdcModuleComponent);
