import { LitElement, html } from 'lit';

class ForgotPassword extends LitElement {

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
    this._success = null;
  }

  _onKeydown(e) {
    if (e.key === 'Enter') this._submit();
  }

  async _submit() {
    this._loading = true;
    this._error   = null;
    this._success = null;
    try {
      const res = await fetch('/api/user/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username:   this.querySelector('#username').value,
          reset_link: window.location.origin + '/#/user/auth/reset-password',
        })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Errore durante il recupero password');
      this._success = data.log || 'Se l\'account esiste, riceverai un\'email con il link di reset.';
    } catch (e) {
      this._error = e.message;
    } finally {
      this._loading = false;
    }
  }

  render() {
    return html`
      <div class="d-flex align-items-center justify-content-center min-vh-100 bg-body">
        <div class="w-max-360">
          <h4 class="mb-4">Recupera password</h4>
          ${this._error   ? html`<div class="alert alert-danger  py-2">${this._error}</div>`   : ''}
          ${this._success ? html`<div class="alert alert-success py-2">${this._success}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="username">Username</label>
            <input id="username" class="form-control"
                   ?disabled=${this._loading}
                   @keydown=${this._onKeydown}>
          </div>
          <button class="btn btn-primary w-100"
                  ?disabled=${this._loading}
                  @click=${this._submit}>
            ${this._loading ? 'Invio in corso...' : 'Invia link di reset'}
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

customElements.define("user-forgot-password", ForgotPassword);
