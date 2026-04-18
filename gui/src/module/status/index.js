import 'bootstrap/dist/css/bootstrap.min.css';
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
    this._message = res.ok ? 'App is running' : 'App is not running';
  }

  render() {
    return html`
      <div class="d-flex align-items-center justify-content-center min-vh-100">
        <p class="text-muted">${this._message ?? '...'}</p>
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
