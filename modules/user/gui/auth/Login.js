import { LitElement, html } from 'lit';
import { authorized, user } from '../../../store.js';
import { MODULE_CONFIG, DEFAULT_MODULE } from '../../../config.js';

class Login extends LitElement {

  static properties = {
    _loading:   { state: true },
    _error:     { state: true },
    _twoFactor: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading   = false;
    this._error     = null;
    this._twoFactor = false;
  }

  _onKeydown(e) {
    if (e.key === 'Enter') this._submit();
  }

  async _submit() {
    this._loading = true;
    this._error   = null;
    try {
      const res = await fetch('/api/user/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: this.querySelector('#username').value,
          password: this.querySelector('#password').value,
        })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Credenziali non valide');

      if (data.out && data.out.two_factor_required) {
        this._twoFactor = true;
        this._loading   = false;
        return;
      }
      authorized.set(true);
      user.set(data.out);
      window.location.hash = data.out.must_change_password
        ? '/user/auth/change-password'
        : MODULE_CONFIG[DEFAULT_MODULE].route;
    } catch (e) {
      this._error   = e.message;
      this._loading = false;
    }
  }

  async _submit2fa() {
    this._loading = true;
    this._error   = null;
    try {
      const res = await fetch('/api/user/auth/2fa', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pin: this.querySelector('#pin').value })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'PIN non valido');
      authorized.set(true);
      user.set(data.out);
      window.location.hash = data.out.must_change_password
        ? '/user/auth/change-password'
        : MODULE_CONFIG[DEFAULT_MODULE].route;
    } catch (e) {
      this._error   = e.message;
      this._loading = false;
    }
  }

  render() {
    if (this._twoFactor) {
      return html`
        <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
          <div style="width:100%;max-width:360px">
            <h4 class="mb-2">Verifica in due passaggi</h4>
            <p class="text-muted small mb-4">Inserisci il PIN inviato alla tua email.</p>
            ${this._error ? html`<div class="alert alert-danger py-2">${this._error}</div>` : ''}
            <div class="mb-3">
              <label class="form-label" for="pin">PIN</label>
              <input id="pin" class="form-control" autocomplete="one-time-code"
                     ?disabled=${this._loading}
                     @keydown=${e => { if (e.key === 'Enter') this._submit2fa(); }}>
            </div>
            <button class="btn btn-primary w-100" ?disabled=${this._loading}
                    @click=${this._submit2fa}>
              ${this._loading ? 'Verifica in corso...' : 'Verifica PIN'}
            </button>
          </div>
        </div>
      `;
    }
    return html`
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
        <div style="width:100%;max-width:360px">
          <h4 class="mb-4">Accedi</h4>
          ${this._error ? html`<div class="alert alert-danger py-2">${this._error}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <input id="username" class="form-control" ?disabled=${this._loading}
                   @keydown=${this._onKeydown}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="password">Password</label>
            <input id="password" type="password" class="form-control"
                   ?disabled=${this._loading} @keydown=${this._onKeydown}>
          </div>
          <button class="btn btn-primary w-100" ?disabled=${this._loading}
                  @click=${this._submit}>
            ${this._loading ? 'Accesso in corso...' : 'Accedi'}
          </button>
          <div class="d-flex justify-content-between mt-3">
            <button class="btn btn-link btn-sm p-0 text-muted"
                    @click=${() => { window.location.hash = '/user/auth/forgot-password'; }}>
              Password dimenticata?
            </button>
            <button class="btn btn-link btn-sm p-0 text-muted"
                    @click=${() => { window.location.hash = '/user/auth/register'; }}>
              Registrati
            </button>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('user-login', Login);
