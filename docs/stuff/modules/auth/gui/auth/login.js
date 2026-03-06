import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';
import { DEFAULT_ROUTE } from '../../config.js';

class LoginComponent extends LitElement {

  static properties = {
    _loading: { state: true },
    _error:   { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading = false;
    this._error   = null;
  }

  render() {
    return html`
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
        <div style="width:100%;max-width:360px">
          <h4 class="mb-4">Accedi</h4>
          ${this._error ? html`<div class="alert alert-danger py-2">${this._error}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <input id="username" class="form-control"
                   ?disabled=${this._loading}
                   @keydown=${this._onKeydown}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="password">Password</label>
            <input id="password" type="password" class="form-control"
                   ?disabled=${this._loading}
                   @keydown=${this._onKeydown}>
          </div>
          <button class="btn btn-primary w-100"
                  ?disabled=${this._loading}
                  @click=${this._submit}>
            ${this._loading ? 'Accesso in corso...' : 'Accedi'}
          </button>
        </div>
      </div>
    `;
  }

  _onKeydown(e) {
    if (e.key === 'Enter') this._submit();
  }

  async _submit() {
    this._loading = true;
    this._error   = null;
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: this.querySelector('#username').value,
          password: this.querySelector('#password').value
        })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Credenziali non valide');

      if (data.out.must_change_password) {
        this.dispatchEvent(new CustomEvent('must-change-password', { bubbles: true }));
      } else {
        authorized.set(true);
        user.set(data.out);
        window.location.hash = DEFAULT_ROUTE;
      }
    } catch (e) {
      this._error   = e.message;
      this._loading = false;
    }
  }
}

customElements.define('login-layout', LoginComponent);

export default LoginComponent;
