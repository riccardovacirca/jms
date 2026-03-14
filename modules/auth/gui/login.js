import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';
import { MODULE_CONFIG, DEFAULT_MODULE } from '../../config.js';

class AuthLoginPage extends LitElement {

  static properties = {
    _loading: { state: true },
    _error:   { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading = false;
    this._error   = null;
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
          password: this.querySelector('#password').value,
        })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Credenziali non valide');

      authorized.set(true);
      user.set(data.out);

      if (data.out.must_change_password) {
        window.location.hash = '/auth/changepass';
      } else {
        window.location.hash = MODULE_CONFIG[DEFAULT_MODULE].route;
      }
    } catch (e) {
      this._error   = e.message;
      this._loading = false;
    }
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
          <div class="text-center mt-3">
            <button class="btn btn-link btn-sm p-0 text-muted"
                    @click=${() => { window.location.hash = '/auth/forgot'; }}>
              Password dimenticata?
            </button>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('auth-login-page', AuthLoginPage);
