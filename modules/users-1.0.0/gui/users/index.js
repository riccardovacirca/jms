
import { LitElement, html } from 'lit';
import { user } from '../../store.js';

/**
 * Modulo gestione utenti.
 * Admin: CRUD completo su tutti gli account.
 * Operatore: visualizzazione e modifica del proprio account.
 */
class UsersPage extends LitElement {

  static properties = {
    _user:     { state: true },
    _users:    { state: true },
    _loading:  { state: true },
    _error:    { state: true },
    _search:   { state: true },
    _panel:    { state: true },   // null | 'create' | user-object
    _form:     { state: true },
    _saving:   { state: true },
    _formErr:  { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._user    = user.get();
    this._users   = [];
    this._loading = false;
    this._error   = null;
    this._search  = '';
    this._panel   = null;
    this._form    = {};
    this._saving  = false;
    this._formErr = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubUser = user.subscribe(v => { this._user = v; });
    this._load();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubUser();
  }

  get _canSendMail() {
    const perms = this._user?.permissions || [];
    return perms.includes('can_send_mail');
  }

  get _isAdmin() {
    return this._user?.ruolo === 'admin';
  }

  async _load() {
    const qs = this._search ? `?q=${encodeURIComponent(this._search)}` : '';
    this._loading = true;
    this._error   = null;
    const r    = await fetch(`/api/users${qs}`);
    const data = await r.json();
    this._loading = false;
    if (!data.err) {
      this._users = Array.isArray(data.out) ? data.out : [];
    } else {
      this._error = data.log;
    }
  }

  _openCreate() {
    this._panel   = 'create';
    this._form    = { ruolo: 'operatore', attivo: true, send_notification: false };
    this._formErr = null;
  }

  _openEdit(u) {
    this._panel   = u;
    this._form    = { username: u.username, email: u.email || '', ruolo: u.ruolo, attivo: u.attivo, password: '' };
    this._formErr = null;
  }

  _closePanel() {
    this._panel = null;
    this._formErr = null;
  }

  _onInput(field, e) {
    const val = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    this._form = { ...this._form, [field]: val };
  }

  async _generatePassword() {
    const r    = await fetch('/api/users/generate-password');
    const data = await r.json();
    if (!data.err) {
      this._form = { ...this._form, password: data.out.password };
    }
  }

  async _save() {
    this._saving  = true;
    this._formErr = null;
    const isCreate = this._panel === 'create';
    const url    = isCreate ? '/api/users' : `/api/users/${this._panel.id}`;
    const method = isCreate ? 'POST' : 'PUT';
    const body   = { ...this._form };
    if (!body.password) { delete body.password; }
    if (!this._isAdmin) { delete body.ruolo; delete body.attivo; }

    const r    = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
    const data = await r.json();
    this._saving = false;
    if (!data.err) {
      this._closePanel();
      this._load();
    } else {
      this._formErr = data.log;
    }
  }

  async _delete(u) {
    if (!confirm(`Eliminare l'utente "${u.username}"?`)) { return; }
    const r    = await fetch(`/api/users/${u.id}`, { method: 'DELETE' });
    const data = await r.json();
    if (!data.err) {
      this._load();
    } else {
      alert(data.log);
    }
  }

  _renderForm() {
    const isCreate = this._panel === 'create';
    const title    = isCreate ? 'Nuovo utente' : `Modifica ${this._panel.username}`;
    return html`
      <div class="card mb-4">
        <div class="card-header d-flex justify-content-between align-items-center">
          <strong>${title}</strong>
          <button class="btn-close" @click=${this._closePanel}></button>
        </div>
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
            <label class="form-label">Password ${isCreate ? '*' : '(lascia vuoto per non cambiare)'}</label>
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
            ${!isCreate ? html`
              <div class="mb-3 form-check">
                <input class="form-check-input" type="checkbox" id="chk-attivo"
                       .checked=${!!this._form.attivo}
                       @change=${e => this._onInput('attivo', e)}>
                <label class="form-check-label" for="chk-attivo">Account attivo</label>
              </div>` : ''}
          ` : ''}
          ${isCreate && this._canSendMail ? html`
            <div class="mb-3 form-check">
              <input class="form-check-input" type="checkbox" id="chk-notify"
                     .checked=${!!this._form.send_notification}
                     @change=${e => this._onInput('send_notification', e)}>
              <label class="form-check-label" for="chk-notify">
                Invia email di benvenuto con le credenziali
              </label>
            </div>` : ''}
          <div class="d-flex gap-2">
            <button class="btn btn-primary" @click=${this._save} ?disabled=${this._saving}>
              ${this._saving ? 'Salvataggio...' : 'Salva'}
            </button>
            <button class="btn btn-outline-secondary" @click=${this._closePanel}>Annulla</button>
          </div>
        </div>
      </div>
    `;
  }

  _renderAdminList() {
    return html`
      <div class="d-flex gap-2 mb-3 align-items-center flex-wrap">
        <input class="form-control w-auto flex-grow-1" placeholder="Cerca utente..."
               .value=${this._search}
               @input=${e => { this._search = e.target.value; this._load(); }}>
        <button class="btn btn-primary btn-sm" @click=${this._openCreate}>+ Nuovo utente</button>
      </div>
      ${this._panel ? this._renderForm() : ''}
      ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}
      ${this._loading ? html`<div class="text-muted">Caricamento...</div>` : html`
        <div class="table-responsive">
          <table class="table table-hover align-middle">
            <thead class="table-light">
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Ruolo</th>
                <th>Stato</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              ${this._users.map(u => html`
                <tr>
                  <td>${u.username}</td>
                  <td class="text-muted small">${u.email || '—'}</td>
                  <td>
                    <span class="badge ${u.ruolo === 'admin' ? 'bg-danger' : 'bg-secondary'}">
                      ${u.ruolo}
                    </span>
                  </td>
                  <td>
                    <span class="badge ${u.attivo ? 'bg-success' : 'bg-warning text-dark'}">
                      ${u.attivo ? 'attivo' : 'disattivo'}
                    </span>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-sm btn-outline-secondary me-1"
                            @click=${() => this._openEdit(u)}>Modifica</button>
                    <button class="btn btn-sm btn-outline-danger"
                            @click=${() => this._delete(u)}>Elimina</button>
                  </td>
                </tr>
              `)}
            </tbody>
          </table>
        </div>
      `}
    `;
  }

  _renderSelfProfile() {
    return html`
      <h5 class="mb-3">Il mio profilo</h5>
      ${this._panel ? this._renderForm() : html`
        ${this._users.length > 0 ? html`
          <div class="card" style="max-width:420px">
            <div class="card-body">
              <p><strong>Username:</strong> ${this._users[0].username}</p>
              <p><strong>Email:</strong> ${this._users[0].email || '—'}</p>
              <p><strong>Ruolo:</strong>
                <span class="badge bg-secondary">${this._users[0].ruolo}</span>
              </p>
              <button class="btn btn-outline-primary btn-sm"
                      @click=${() => this._openEdit(this._users[0])}>
                Modifica profilo
              </button>
            </div>
          </div>
        ` : ''}
      `}
    `;
  }

  render() {
    return html`
      <div class="container py-4">
        <h4 class="mb-4">${this._isAdmin ? 'Gestione utenti' : 'Il mio profilo'}</h4>
        ${this._isAdmin ? this._renderAdminList() : this._renderSelfProfile()}
      </div>
    `;
  }
}

customElements.define('users-page', UsersPage);

function mount(container) {
  container.innerHTML = '<users-page></users-page>';
}

export default { mount };
