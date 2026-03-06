import { LitElement, html } from 'lit';

export class BadgeModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'badge';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Badge</h2>
        <badge-component></badge-component>
      </div>
    `;
  }
}

customElements.define('badge-module', BadgeModuleComponent);
