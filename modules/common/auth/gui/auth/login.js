import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';
import { MODULE_CONFIG, DEFAULT_MODULE } from '../../config.js';

class LoginComponent extends LitElement {

  static properties = {
    _loading: { state: true },
    _error:   { state: true },
    _view:    { state: true },
    _success: { state: true },
    _token:   { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading = false;
    this._error   = null;
    this._view    = 'login';
    this._success = null;
    this._token   = null;
  }

  connectedCallback() {
    super.connectedCallback();
    const match = window.location.hash.match(/[?&]token=([^&]+)/);
    if (match) {
      this._token = match[1];
      this._view  = 'reset';
    }
  }

  render() {
    let content;
    if (this._view === 'forgot') {
      content = this._renderForgot();
    } else if (this._view === 'reset') {
      content = this._renderReset();
    } else {
      content = this._renderLogin();
    }
    return html`
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
        <div style="width:100%;max-width:360px">
          ${content}
        </div>
      </div>
    `;
  }

  _renderLogin() {
    return html`
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
                @click=${() => { this._view = 'forgot'; this._error = null; }}>
          Password dimenticata?
        </button>
      </div>
    `;
  }

  _renderForgot() {
    return html`
      <h4 class="mb-4">Recupera password</h4>
      ${this._error   ? html`<div class="alert alert-danger py-2">${this._error}</div>`   : ''}
      ${this._success ? html`<div class="alert alert-success py-2">${this._success}</div>` : ''}
      <div class="mb-3">
        <label class="form-label" for="forgot-username">Username</label>
        <input id="forgot-username" class="form-control"
               ?disabled=${this._loading}
               @keydown=${this._onForgotKeydown}>
      </div>
      <button class="btn btn-primary w-100"
              ?disabled=${this._loading}
              @click=${this._submitForgot}>
        ${this._loading ? 'Invio in corso...' : 'Invia password temporanea'}
      </button>
      <div class="text-center mt-3">
        <button class="btn btn-link btn-sm p-0 text-muted"
                @click=${() => { this._view = 'login'; this._error = null; this._success = null; }}>
          ← Torna al login
        </button>
      </div>
    `;
  }

  _onKeydown(e) {
    if (e.key === 'Enter') this._submit();
  }

  _onForgotKeydown(e) {
    if (e.key === 'Enter') this._submitForgot();
  }

  _onResetKeydown(e) {
    if (e.key === 'Enter') this._submitReset();
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
        window.location.hash = MODULE_CONFIG[DEFAULT_MODULE].route;
      }
    } catch (e) {
      this._error   = e.message;
      this._loading = false;
    }
  }

  _renderReset() {
    return html`
      <h4 class="mb-4">Nuova password</h4>
      ${this._error   ? html`<div class="alert alert-danger py-2">${this._error}</div>`   : ''}
      ${this._success ? html`<div class="alert alert-success py-2">${this._success}</div>` : ''}
      <div class="mb-3">
        <label class="form-label" for="reset-password">Nuova password</label>
        <input id="reset-password" type="password" class="form-control"
               ?disabled=${this._loading}>
      </div>
      <div class="mb-3">
        <label class="form-label" for="reset-password-confirm">Conferma password</label>
        <input id="reset-password-confirm" type="password" class="form-control"
               ?disabled=${this._loading}
               @keydown=${this._onResetKeydown}>
      </div>
      <button class="btn btn-primary w-100"
              ?disabled=${this._loading}
              @click=${this._submitReset}>
        ${this._loading ? 'Salvataggio...' : 'Imposta nuova password'}
      </button>
    `;
  }

  async _submitReset() {
    this._loading = true;
    this._error   = null;
    this._success = null;
    try {
      const password = this.querySelector('#reset-password').value;
      const confirm  = this.querySelector('#reset-password-confirm').value;
      if (password !== confirm) throw new Error('Le password non coincidono');
      const res = await fetch('/api/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          token:        this._token,
          new_password: password
        })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Errore durante il reset della password');
      this._success = 'Password aggiornata. Puoi ora accedere.';
      window.location.hash = '/auth';
      this._view  = 'login';
      this._token = null;
    } catch (e) {
      this._error = e.message;
    } finally {
      this._loading = false;
    }
  }

  async _submitForgot() {
    this._loading = true;
    this._error   = null;
    this._success = null;
    try {
      const res = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: this.querySelector('#forgot-username').value })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Errore durante il recupero password');
      this._success = 'Se l\'utente esiste, riceverà una password temporanea via email.';
    } catch (e) {
      this._error = e.message;
    } finally {
      this._loading = false;
    }
  }
}

customElements.define('login-layout', LoginComponent);

export default LoginComponent;
