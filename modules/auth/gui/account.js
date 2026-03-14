import { LitElement, html } from 'lit';

/**
 * Pagina impostazioni del proprio account.
 * Permette di modificare username, email e password, e di disattivare l'account.
 */
class AuthAccountPage extends LitElement {

  static properties = {
    _loading:  { state: true },
    _saving:   { state: true },
    _deleting: { state: true },
    _error:    { state: true },
    _formErr:  { state: true },
    _account:  { state: true },
    _form:     { state: true },
    _editing:  { state: true },
    _confirm:  { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading  = false;
    this._saving   = false;
    this._deleting = false;
    this._error    = null;
    this._formErr  = null;
    this._account  = null;
    this._form     = {};
    this._editing  = false;
    this._confirm  = false;
  }

  connectedCallback() {
    super.connectedCallback();
    this._load();
  }

  async _load() {
    this._loading = true;
    this._error   = null;
    const r       = await fetch('/api/auth/account');
    const data    = await r.json();
    this._loading = false;
    if (!data.err) {
      this._account = data.out;
      this._fillForm(data.out);
    } else {
      this._error = data.log;
    }
  }

  _fillForm(a) {
    this._form = {
      username: a.username || '',
      email:    a.email    || '',
      password: '',
    };
  }

  _onInput(field, e) {
    this._form = { ...this._form, [field]: e.target.value };
  }

  async _generatePassword() {
    const r    = await fetch('/api/auth/account/generate-password');
    const data = await r.json();
    if (!data.err) {
      this._form = { ...this._form, password: data.out.password };
    }
  }

  async _save() {
    this._saving  = true;
    this._formErr = null;
    const body    = { ...this._form };
    if (!body.password) { delete body.password; }
    const r       = await fetch('/api/auth/account', {
      method:  'PUT',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(body),
    });
    const data    = await r.json();
    this._saving  = false;
    if (!data.err) {
      this._account = { ...this._account, username: this._form.username, email: this._form.email };
      this._editing = false;
      this._form    = { ...this._form, password: '' };
    } else {
      this._formErr = data.log;
    }
  }

  async _deleteAccount() {
    this._deleting = true;
    const r        = await fetch('/api/auth/account', { method: 'DELETE' });
    const data     = await r.json();
    this._deleting = false;
    if (!data.err) {
      window.location.hash = '/auth';
    } else {
      this._error   = data.log;
      this._confirm = false;
    }
  }

  render() {
    return html`
      <div class="container py-4" style="max-width:480px">
        <h4 class="mb-4">Il mio account</h4>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}
        ${this._loading ? html`<div class="text-muted">Caricamento...</div>` : html`
          <div class="card mb-3">
            <div class="card-body">
              ${this._account ? html`
                <p class="text-muted small mb-3">
                  Ruolo: <strong>${this._account.ruolo}</strong>
                </p>
              ` : ''}
              ${this._formErr ? html`<div class="alert alert-danger py-2">${this._formErr}</div>` : ''}

              <div class="mb-3">
                <label class="form-label">Username</label>
                <input class="form-control" .value=${this._form.username || ''}
                       ?disabled=${!this._editing}
                       @input=${e => this._onInput('username', e)}>
              </div>

              <div class="mb-3">
                <label class="form-label">Email</label>
                <input class="form-control" type="email" .value=${this._form.email || ''}
                       ?disabled=${!this._editing}
                       @input=${e => this._onInput('email', e)}>
              </div>

              ${this._editing ? html`
                <div class="mb-3">
                  <label class="form-label">
                    Nuova password
                    <span class="text-muted fw-normal">(lascia vuoto per non cambiare)</span>
                  </label>
                  <div class="input-group">
                    <input class="form-control" type="text" .value=${this._form.password || ''}
                           @input=${e => this._onInput('password', e)}>
                    <button class="btn btn-outline-secondary" type="button"
                            @click=${this._generatePassword}>Genera</button>
                  </div>
                </div>
              ` : ''}

              <div class="d-flex gap-2 flex-wrap">
                ${this._editing ? html`
                  <button class="btn btn-primary" @click=${this._save} ?disabled=${this._saving}>
                    ${this._saving ? 'Salvataggio...' : 'Salva'}
                  </button>
                  <button class="btn btn-outline-secondary"
                          @click=${() => { this._editing = false; this._fillForm(this._account); }}>
                    Annulla
                  </button>
                ` : html`
                  <button class="btn btn-outline-primary"
                          @click=${() => { this._editing = true; }}>
                    Modifica
                  </button>
                  <button class="btn btn-outline-secondary btn-sm"
                          @click=${() => { window.location.hash = '/auth/changepass'; }}>
                    Cambia password
                  </button>
                `}
              </div>
            </div>
          </div>

          <div class="card border-danger">
            <div class="card-body">
              <h6 class="text-danger mb-2">Zona pericolosa</h6>
              ${this._confirm ? html`
                <p class="small mb-2">Sei sicuro? L'account verrà disattivato.</p>
                <div class="d-flex gap-2">
                  <button class="btn btn-danger btn-sm"
                          @click=${this._deleteAccount} ?disabled=${this._deleting}>
                    ${this._deleting ? 'Eliminazione...' : 'Sì, disattiva account'}
                  </button>
                  <button class="btn btn-outline-secondary btn-sm"
                          @click=${() => { this._confirm = false; }}>
                    Annulla
                  </button>
                </div>
              ` : html`
                <button class="btn btn-outline-danger btn-sm"
                        @click=${() => { this._confirm = true; }}>
                  Disattiva account
                </button>
              `}
            </div>
          </div>
        `}
      </div>
    `;
  }
}

customElements.define('auth-account-page', AuthAccountPage);
