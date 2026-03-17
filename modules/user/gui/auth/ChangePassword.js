import { LitElement, html } from 'lit';
import { authorized, user } from '../../store.js';

class ChangePassword extends LitElement {

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
    const cur  = this.querySelector('#current_password').value;
    const nw   = this.querySelector('#new_password').value;
    const conf = this.querySelector('#confirm_password').value;
    if (!cur || !nw || !conf)    { this._error = 'Tutti i campi sono obbligatori'; return; }
    if (nw !== conf)             { this._error = 'Le password non coincidono'; return; }
    if (nw === cur)              { this._error = 'La nuova password deve essere diversa da quella attuale'; return; }

    this._loading = true;
    this._error   = null;
    try {
      const res = await fetch('/api/user/auth/change-password', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ current_password: cur, new_password: nw })
      });
      const data = await res.json();
      if (!res.ok || data.err) throw new Error(data.log || 'Errore durante il cambio password');
      try { await fetch('/api/user/auth/logout', { method: 'POST' }); } finally {
        authorized.set(false);
        user.set(null);
        window.location.hash = '/user/auth/login';
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
          <h4 class="mb-1">Imposta nuova password</h4>
          <p class="text-muted small mb-4">La password temporanea deve essere modificata prima di continuare.</p>
          ${this._error ? html`<div class="alert alert-danger py-2">${this._error}</div>` : ''}
          <div class="mb-3">
            <label class="form-label" for="current_password">Password attuale</label>
            <input id="current_password" type="password" class="form-control"
                   ?disabled=${this._loading} @keydown=${this._onKeydown}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="new_password">Nuova password</label>
            <input id="new_password" type="password" class="form-control"
                   ?disabled=${this._loading} @keydown=${this._onKeydown}>
          </div>
          <div class="mb-3">
            <label class="form-label" for="confirm_password">Conferma nuova password</label>
            <input id="confirm_password" type="password" class="form-control"
                   ?disabled=${this._loading} @keydown=${this._onKeydown}>
          </div>
          <button class="btn btn-primary w-100" ?disabled=${this._loading} @click=${this._submit}>
            ${this._loading ? 'Salvataggio in corso...' : 'Imposta password'}
          </button>
        </div>
      </div>
    `;
  }
}

customElements.define("user-change-password", ChangePassword);
