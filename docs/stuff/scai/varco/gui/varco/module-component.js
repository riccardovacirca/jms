import { LitElement, html } from 'lit';

export class VarcoModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'varco';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Varchi</h2>
        <varco-component></varco-component>
      </div>
    `;
  }
}

customElements.define('varco-module', VarcoModuleComponent);
