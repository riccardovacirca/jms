import { LitElement, html } from 'lit';

export class EnteModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'ente';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Enti</h2>
        <ente-component></ente-component>
      </div>
    `;
  }
}

customElements.define('ente-module', EnteModuleComponent);
