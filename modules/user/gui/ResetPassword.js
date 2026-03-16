import { LitElement, html } from 'lit';

class UserResetPasswordPage extends LitElement {

  static properties = {
    _loading: { state: true },
    _error:   { state: true },
    _token:   { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading = false;
    this._error   = null;
    this._token   = null;
  }

  connectedCallback() {
    super.connectedCallback();
    const match = window.location.hash.match(/[?&]token=([^&]+)/);
    this._token = match ? match[1] : null;
  }

  _onKeydown(e) {
    if (e.key === 'Enter') this._submit();
  }

  async _submit() {
    this._loading = true;
    this._error   = null;
    try {
      const password = this.querySelector('#password').value;
      const confirm  = this.querySelector('#confirm').value;
      if (password !== confirm) throw new Error('Le password non coincidono');
      const res = await fetch('/api/user/auth/reset-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token: this._token, new_password: password })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Errore durante il reset della password');
      window.location.hash = '/user/auth/login';
    } catch (e) {
      this._error = e.message;
    } finally {
      this._loading = false;
    }
  }

  render() {
    if (!this._token) {
      return html`
        <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
          <div style="width:100%;max-width:360px">
            <div class="alert alert-danger">Link di reset non valido o mancante.</div>
            <button class="btn btn-link p-0"
                    @click=${() => { window.location.hash = '/user/auth/login'; }}>
              ← Torna al login
            </button>
          </div>
        </div>
      `;
    }
    return html`
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
        <div style="width:100%;max-width:360px">
          <h4 class="mb-4">Nuova password</h4>
          ${this._error ? html`<div class="alert alert-danger py-2">${this._error}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="password">Nuova password</label>
            <input id="password" type="password" class="form-control"
                   ?disabled=${this._loading}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="confirm">Conferma password</label>
            <input id="confirm" type="password" class="form-control"
                   ?disabled=${this._loading}
                   @keydown=${this._onKeydown}>
          </div>
          <button class="btn btn-primary w-100"
                  ?disabled=${this._loading}
                  @click=${this._submit}>
            ${this._loading ? 'Salvataggio...' : 'Imposta nuova password'}
          </button>
        </div>
      </div>
    `;
  }
}

customElements.define('user-reset-password-page', UserResetPasswordPage);
