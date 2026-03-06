import { LitElement, html } from 'lit';

export class SedeModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'sede';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Sedi</h2>
        <sede-component></sede-component>
      </div>
    `;
  }
}

customElements.define('sede-module', SedeModuleComponent);
