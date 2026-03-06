import { LitElement, html } from 'lit';

class HomeComponent extends LitElement {

  static properties = {
    _hello: { state: true }
  };

  // Disabilita Shadow DOM: Bootstrap funziona normalmente
  createRenderRoot() { return this; }

  constructor() {
    super();
    this._hello = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadHello();
  }

  async _loadHello() {
    try {
      const res = await fetch('/api/home/hello');
      if (!res.ok) {
        this._hello = null;
        return;
      }
      const data = await res.json();
      if (!data.err) this._hello = data.out;
      else this._hello = null;
    } catch (e) {
      this._hello = null;
    }
  }

  render() {
    return html`
      <div class="min-vh-100 bg-light">
        <main class="container py-5">
          <div class="text-center py-5">
            <h1 class="display-5">Benvenuto</h1>
            ${this._hello ? html`<p class="text-muted">${this._hello}</p>` : ''}
          </div>
        </main>
      </div>
    `;
  }
}

customElements.define('home-component', HomeComponent);

export default HomeComponent;
