import { LitElement, html } from 'lit';

export class PolicyModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'policy';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Policy Rapporti</h2>
        <policy-component></policy-component>
      </div>
    `;
  }
}

customElements.define('policy-module', PolicyModuleComponent);
