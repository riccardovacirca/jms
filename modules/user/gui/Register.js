import { LitElement, html } from 'lit';

class UserRegisterPage extends LitElement {

  static properties = {
    _loading: { state: true },
    _error:   { state: true },
    _success: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading = false;
    this._error   = null;
    this._success = false;
  }

  async _submit() {
    const username = this.querySelector('#username').value;
    const email    = this.querySelector('#email').value;
    const password = this.querySelector('#password').value;
    const confirm  = this.querySelector('#confirm').value;

    if (!username || !email || !password || !confirm) {
      this._error = 'Tutti i campi sono obbligatori';
      return;
    }
    if (password !== confirm) {
      this._error = 'Le password non coincidono';
      return;
    }

    this._loading = true;
    this._error   = null;
    try {
      const res = await fetch('/api/user/accounts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Errore durante la registrazione');
      this._success = true;
    } catch (e) {
      this._error = e.message;
    } finally {
      this._loading = false;
    }
  }

  render() {
    if (this._success) {
      return html`
        <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
          <div style="width:100%;max-width:360px">
            <div class="alert alert-success">Account creato con successo.</div>
            <button class="btn btn-primary w-100"
                    @click=${() => { window.location.hash = '/user/auth/login'; }}>
              Accedi
            </button>
          </div>
        </div>
      `;
    }
    return html`
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-light">
        <div style="width:100%;max-width:360px">
          <h4 class="mb-4">Registrazione</h4>
          ${this._error ? html`<div class="alert alert-danger py-2">${this._error}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <input id="username" class="form-control" ?disabled=${this._loading}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="email">Email</label>
            <input id="email" type="email" class="form-control" ?disabled=${this._loading}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="password">Password</label>
            <input id="password" type="password" class="form-control" ?disabled=${this._loading}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="confirm">Conferma password</label>
            <input id="confirm" type="password" class="form-control" ?disabled=${this._loading}>
          </div>
          <button class="btn btn-primary w-100"
                  ?disabled=${this._loading}
                  @click=${this._submit}>
            ${this._loading ? 'Registrazione in corso...' : 'Registrati'}
          </button>
          <div class="text-center mt-3">
            <button class="btn btn-link btn-sm p-0 text-muted"
                    @click=${() => { window.location.hash = '/user/auth/login'; }}>
              ← Torna al login
            </button>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define('user-register-page', UserRegisterPage);
