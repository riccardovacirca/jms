import { LitElement, html } from 'lit';

class StatusView extends LitElement
{
  static properties = {
    _message: { state: true }
  };

  constructor() {
    super();
    this._message = null;
  }

  createRenderRoot() {
    return this;
  }

  async connectedCallback() {
    super.connectedCallback();
    const res = await fetch('/api/status');
    const data = await res.json();
    this._message = data.err ? data.log : data.out;
  }

  render() {
    return html`
      <div style="display:flex;align-items:center;justify-content:center;min-height:100vh">
        <p style="color:#888;font-family:sans-serif">${this._message ?? '...'}</p>
      </div>
    `;
  }
}

customElements.define('app-status', StatusView);

const Status = {
  mount(container) {
    container.innerHTML = '<app-status></app-status>';
  }
};

export default Status;
