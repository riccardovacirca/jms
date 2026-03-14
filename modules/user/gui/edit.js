import { LitElement, html } from 'lit';
import { user } from '../../store.js';

/**
 * Vista modifica / creazione account.
 *
 * Admin, nessun id  (/account/edit)      → form vuoto per creare un nuovo account.
 * Admin, id presente (/account/edit/42)  → form precompilato con i dati dell'account 42.
 * Operatore           (/account/edit)    → form precompilato con i propri dati (id ignorato).
 */
class UserEditPage extends LitElement {

  static properties = {
    _user:    { state: true },
    _loading: { state: true },
    _saving:  { state: true },
    _error:   { state: true },
    _formErr: { state: true },
    _form:    { state: true },
    _isNew:   { state: true },
    _targetId: { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._user     = user.get();
    this._loading  = false;
    this._saving   = false;
    this._error    = null;
    this._formErr  = null;
    this._form     = {};
    this._isNew    = false;
    this._targetId = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubUser = user.subscribe(v => { this._user = v; });

    const parts = window.location.hash.replace(/^#\//, '').split('/');
    // parts: ['account', 'edit'] o ['account', 'edit', '42']
    const rawId = parts[2];

    if (this._isAdmin) {
      if (rawId) {
        this._targetId = parseInt(rawId, 10);
        this._isNew    = false;
        this._loadUser(this._targetId);
      } else {
        this._targetId = null;
        this._isNew    = true;
        this._form     = { ruolo: 'operatore', attivo: true, send_notification: false };
      }
    } else {
      this._targetId = this._user?.id ?? null;
      this._isNew    = false;
      this._loadUser(this._targetId);
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubUser();
  }

  get _isAdmin() {
    return this._user?.ruolo === 'admin';
  }

  get _canSendMail() {
    const perms = this._user?.permissions || [];
    return perms.includes('can_send_mail');
  }

  async _loadUser(id) {
    this._loading = true;
    this._error   = null;
    const r    = await fetch(`/api/user/${id}`);
    const data = await r.json();
    this._loading = false;
    if (!data.err) {
      const u    = data.out;
      this._form = {
        username: u.username,
        email:    u.email || '',
        ruolo:    u.ruolo,
        attivo:   u.attivo,
        password: '',
      };
    } else {
      this._error = data.log;
    }
  }

  _onInput(field, e) {
    const val  = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    this._form = { ...this._form, [field]: val };
  }

  async _generatePassword() {
    const r    = await fetch('/api/user/generate-password');
    const data = await r.json();
    if (!data.err) {
      this._form = { ...this._form, password: data.out.password };
    }
  }

  async _save() {
    this._saving  = true;
    this._formErr = null;

    const url    = this._isNew ? '/api/user' : `/api/user/${this._targetId}`;
    const method = this._isNew ? 'POST' : 'PUT';
    const body   = { ...this._form };
    if (!body.password) { delete body.password; }
    if (!this._isAdmin) { delete body.ruolo; delete body.attivo; }

    const r    = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    const data = await r.json();
    this._saving = false;
    if (!data.err) {
      window.location.hash = '/user';
    } else {
      this._formErr = data.log;
    }
  }

  render() {
    const title = this._isNew
      ? 'Nuovo account'
      : this._isAdmin
        ? `Modifica account #${this._targetId}`
        : 'Modifica profilo';

    return html`
      <div class="container py-4" style="max-width:560px">
        <div class="d-flex align-items-center gap-3 mb-4">
          <button class="btn btn-outline-secondary btn-sm"
                  @click=${() => { window.location.hash = '/user'; }}>
            ← Indietro
          </button>
          <h4 class="mb-0">${title}</h4>
        </div>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}
        ${this._loading ? html`<div class="text-muted">Caricamento...</div>` : html`
          <div class="card">
            <div class="card-body">
              ${this._formErr ? html`<div class="alert alert-danger py-2">${this._formErr}</div>` : ''}
              <div class="mb-3">
                <label class="form-label">Username *</label>
                <input class="form-control" .value=${this._form.username || ''}
                       @input=${e => this._onInput('username', e)}>
              </div>
              <div class="mb-3">
                <label class="form-label">Email</label>
                <input class="form-control" type="email" .value=${this._form.email || ''}
                       @input=${e => this._onInput('email', e)}>
              </div>
              <div class="mb-3">
                <label class="form-label">
                  Password ${this._isNew ? '*' : '(lascia vuoto per non cambiare)'}
                </label>
                <div class="input-group">
                  <input class="form-control" type="text" .value=${this._form.password || ''}
                         @input=${e => this._onInput('password', e)}>
                  <button class="btn btn-outline-secondary" type="button"
                          @click=${this._generatePassword}>Genera</button>
                </div>
              </div>
              ${this._isAdmin ? html`
                <div class="mb-3">
                  <label class="form-label">Ruolo *</label>
                  <select class="form-select" .value=${this._form.ruolo || 'operatore'}
                          @change=${e => this._onInput('ruolo', e)}>
                    <option value="operatore">Operatore</option>
                    <option value="admin">Admin</option>
                  </select>
                </div>
                ${!this._isNew ? html`
                  <div class="mb-3 form-check">
                    <input class="form-check-input" type="checkbox" id="chk-attivo"
                           .checked=${!!this._form.attivo}
                           @change=${e => this._onInput('attivo', e)}>
                    <label class="form-check-label" for="chk-attivo">Account attivo</label>
                  </div>
                ` : ''}
              ` : ''}
              ${this._isNew && this._canSendMail ? html`
                <div class="mb-3 form-check">
                  <input class="form-check-input" type="checkbox" id="chk-notify"
                         .checked=${!!this._form.send_notification}
                         @change=${e => this._onInput('send_notification', e)}>
                  <label class="form-check-label" for="chk-notify">
                    Invia email di benvenuto con le credenziali
                  </label>
                </div>
              ` : ''}
              <div class="d-flex gap-2">
                <button class="btn btn-primary" @click=${this._save} ?disabled=${this._saving}>
                  ${this._saving ? 'Salvataggio...' : 'Salva'}
                </button>
                <button class="btn btn-outline-secondary"
                        @click=${() => { window.location.hash = '/user'; }}>
                  Annulla
                </button>
              </div>
            </div>
          </div>
        `}
      </div>
    `;
  }
}

customElements.define('user-edit-page', UserEditPage);
