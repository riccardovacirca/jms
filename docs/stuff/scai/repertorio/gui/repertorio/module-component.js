import { LitElement, html } from 'lit';

export class RepertorioModuleComponent extends LitElement {
  static properties = {
    activeView: { type: String, state: true }
  };

  constructor() {
    super();
    this.activeView = 'repertorio';
  }

  createRenderRoot() {
    return this;
  }

  render() {
    return html`
      <div class="container-fluid mt-3">
        <h2>Gestione Repertorio</h2>
        <repertorio-component></repertorio-component>
      </div>
    `;
  }
}

customElements.define('repertorio-module', RepertorioModuleComponent);
