import { LitElement, html } from 'lit';

class ContattiModuleComponent extends LitElement {

  static properties = {
    _tab: { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._tab = 'contatti';
  }

  render() {
    return html`
      <div>
        <ul class="nav nav-tabs px-3 pt-3 bg-white border-bottom">
          <li class="nav-item">
            <button class="nav-link ${this._tab === 'contatti' ? 'active' : ''}"
                    @click=${() => { this._tab = 'contatti'; }}>Contatti</button>
          </li>
          <li class="nav-item">
            <button class="nav-link ${this._tab === 'liste' ? 'active' : ''}"
                    @click=${() => { this._tab = 'liste'; }}>Liste</button>
          </li>
        </ul>
        ${this._tab === 'contatti'
          ? html`<contatti-layout></contatti-layout>`
          : html`<liste-layout></liste-layout>`}
      </div>`;
  }
}

customElements.define('contatti-module', ContattiModuleComponent);

export default ContattiModuleComponent;
