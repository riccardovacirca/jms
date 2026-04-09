import { LitElement, html } from 'lit';
import { user } from '../../../store.js';

const PAGE_SIZE = 20;

/**
 * Interfaccia amministrativa per la gestione degli account utente.
 * Consente a admin e root di creare, modificare, resettare la password ed eliminare account.
 */
class Users extends LitElement {

  static properties = {
    _loading:  { state: true },
    _saving:   { state: true },
    _deleting: { state: true },
    _error:    { state: true },
    _formErr:  { state: true },
    _items:    { state: true },
    _total:    { state: true },
    _page:     { state: true },
    _search:   { state: true },
    _modal:    { state: true },
    _selected: { state: true },
    _form:     { state: true },
    _myLevel:  { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading  = false;
    this._saving   = false;
    this._deleting = false;
    this._error    = null;
    this._formErr  = null;
    this._items    = [];
    this._total    = 0;
    this._page     = 1;
    this._search   = '';
    this._modal    = null;
    this._selected = null;
    this._form     = {};
    this._myLevel  = user.get()?.ruolo_level ?? 0;
  }

  connectedCallback() {
    super.connectedCallback();
    this._unsubUser = user.subscribe(v => { this._myLevel = v?.ruolo_level ?? 0; });
    this._load();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._unsubUser();
  }

  async _load() {
    const params = new URLSearchParams({ page: this._page, pageSize: PAGE_SIZE });
    if (this._search) params.set('search', this._search);
    this._loading = true;
    this._error   = null;
    const r       = await fetch(`/api/user/accounts?${params}`);
    const data    = await r.json();
    this._loading = false;
    if (!data.err) {
      this._items = data.out.items ?? [];
      this._total = data.out.total ?? 0;
    } else {
      this._error = data.log;
    }
  }

  _openCreate() {
    this._form    = { username: '', email: '', password: '', ruolo: 'user', must_change_password: false };
    this._formErr = null;
    this._modal   = 'create';
  }

  _openEdit(item) {
    this._selected = item;
    this._form     = {
      username:             item.username ?? '',
      email:                item.email ?? '',
      ruolo:                item.ruolo ?? 'user',
      attivo:               item.attivo ?? true,
      must_change_password: item.must_change_password ?? false,
    };
    this._formErr = null;
    this._modal   = 'edit';
  }

  _openPassword(item) {
    this._selected = item;
    this._form     = { new_password: '' };
    this._formErr  = null;
    this._modal    = 'password';
  }

  _openDelete(item) {
    this._selected = item;
    this._modal    = 'delete';
  }

  _closeModal() {
    this._modal    = null;
    this._selected = null;
    this._formErr  = null;
  }

  _field(key, value) {
    this._form = { ...this._form, [key]: value };
  }

  async _generatePassword() {
    const r    = await fetch('/api/user/auth/generate-password');
    const data = await r.json();
    if (!data.err) {
      const key  = this._modal === 'password' ? 'new_password' : 'password';
      this._form = { ...this._form, [key]: data.out.password };
    }
  }

  async _save() {
    this._saving  = true;
    this._formErr = null;
    let r, data;
    if (this._modal === 'create') {
      r    = await fetch('/api/user/accounts', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(this._form),
      });
    } else {
      r    = await fetch(`/api/user/accounts/${this._selected.id}`, {
        method:  'PUT',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(this._form),
      });
    }
    data         = await r.json();
    this._saving = false;
    if (!data.err) {
      this._closeModal();
      this._load();
    } else {
      this._formErr = data.log;
    }
  }

  async _savePassword() {
    this._saving  = true;
    this._formErr = null;
    const r       = await fetch(`/api/user/accounts/${this._selected.id}/password`, {
      method:  'PUT',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(this._form),
    });
    const data    = await r.json();
    this._saving  = false;
    if (!data.err) {
      this._closeModal();
    } else {
      this._formErr = data.log;
    }
  }

  async _confirmDelete() {
    this._deleting = true;
    const r        = await fetch(`/api/user/accounts/${this._selected.id}`, { method: 'DELETE' });
    const data     = await r.json();
    this._deleting = false;
    if (!data.err) {
      this._closeModal();
      this._load();
    } else {
      this._error = data.log;
      this._closeModal();
    }
  }

  _onSearch(e) {
    this._search = e.target.value;
    this._page   = 1;
    this._load();
  }

  _onPage(p) {
    this._page = p;
    this._load();
  }

  /**
   * Determina se l'utente corrente può agire sull'account dato.
   * @param {{ ruolo: string }} item
   * @returns {boolean}
   */
  _canAct(item) {
    if (item.ruolo === 'root') return false;
    if (item.ruolo === 'admin' && this._myLevel < 3) return false;
    return true;
  }

  _roleBadge(ruolo) {
    const cls = { root: 'bg-danger', admin: 'bg-warning text-dark', user: 'bg-secondary' };
    return html`<span class="badge ${cls[ruolo] ?? 'bg-secondary'}">${ruolo}</span>`;
  }

  _renderModal() {
    if (!this._modal) return '';
    let title, body, footer;

    if (this._modal === 'create' || this._modal === 'edit') {
      const isCreate = this._modal === 'create';
      title = isCreate ? 'Nuovo utente' : `Modifica: ${this._selected?.username}`;
      body  = html`
        ${this._formErr ? html`<div class="alert alert-danger py-2">${this._formErr}</div>` : ''}
        <div class="mb-3">
          <label class="form-label">Username *</label>
          <input class="form-control" .value=${this._form.username ?? ''}
                 @input=${e => this._field('username', e.target.value)}>
        </div>
        <div class="mb-3">
          <label class="form-label">Email</label>
          <input class="form-control" type="email" .value=${this._form.email ?? ''}
                 @input=${e => this._field('email', e.target.value)}>
        </div>
        ${isCreate ? html`
          <div class="mb-3">
            <label class="form-label">Password *</label>
            <div class="input-group">
              <input class="form-control" type="text" .value=${this._form.password ?? ''}
                     @input=${e => this._field('password', e.target.value)}>
              <button class="btn btn-outline-secondary" type="button"
                      @click=${this._generatePassword}>Genera</button>
            </div>
          </div>
        ` : ''}
        <div class="mb-3">
          <label class="form-label">Ruolo *</label>
          <select class="form-select" @change=${e => this._field('ruolo', e.target.value)}>
            <option value="user" ?selected=${this._form.ruolo === 'user'}>user</option>
            ${this._myLevel >= 3
              ? html`<option value="admin" ?selected=${this._form.ruolo === 'admin'}>admin</option>`
              : ''}
          </select>
        </div>
        ${!isCreate ? html`
          <div class="mb-2 form-check">
            <input type="checkbox" class="form-check-input" id="chk-attivo"
                   .checked=${this._form.attivo ?? true}
                   @change=${e => this._field('attivo', e.target.checked)}>
            <label class="form-check-label" for="chk-attivo">Account attivo</label>
          </div>
        ` : ''}
        <div class="mb-1 form-check">
          <input type="checkbox" class="form-check-input" id="chk-mcp"
                 .checked=${this._form.must_change_password ?? false}
                 @change=${e => this._field('must_change_password', e.target.checked)}>
          <label class="form-check-label" for="chk-mcp">Forza cambio password al prossimo accesso</label>
        </div>
      `;
      footer = html`
        <button class="btn btn-secondary" @click=${this._closeModal}>Annulla</button>
        <button class="btn btn-primary" @click=${this._save} ?disabled=${this._saving}>
          ${this._saving ? 'Salvataggio...' : 'Salva'}
        </button>
      `;
    } else if (this._modal === 'password') {
      title = `Reset password: ${this._selected?.username}`;
      body  = html`
        ${this._formErr ? html`<div class="alert alert-danger py-2">${this._formErr}</div>` : ''}
        <div class="mb-2">
          <label class="form-label">Nuova password *</label>
          <div class="input-group">
            <input class="form-control" type="text" .value=${this._form.new_password ?? ''}
                   @input=${e => this._field('new_password', e.target.value)}>
            <button class="btn btn-outline-secondary" type="button"
                    @click=${this._generatePassword}>Genera</button>
          </div>
          <div class="form-text">L'utente dovrà cambiare la password al prossimo accesso.</div>
        </div>
      `;
      footer = html`
        <button class="btn btn-secondary" @click=${this._closeModal}>Annulla</button>
        <button class="btn btn-warning" @click=${this._savePassword} ?disabled=${this._saving}>
          ${this._saving ? 'Salvataggio...' : 'Reset password'}
        </button>
      `;
    } else if (this._modal === 'delete') {
      title  = 'Elimina account';
      body   = html`
        <p>Sei sicuro di voler disattivare l'account <strong>${this._selected?.username}</strong>?</p>
        <p class="text-muted small mb-0">L'account verrà disattivato e non potrà più accedere.</p>
      `;
      footer = html`
        <button class="btn btn-secondary" @click=${this._closeModal}>Annulla</button>
        <button class="btn btn-danger" @click=${this._confirmDelete} ?disabled=${this._deleting}>
          ${this._deleting ? 'Eliminazione...' : 'Disattiva'}
        </button>
      `;
    }

    return html`
      <div style="position:fixed;inset:0;background:rgba(0,0,0,.5);z-index:1040"></div>
      <div class="modal d-block" style="z-index:1050" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${title}</h5>
              <button type="button" class="btn-close" @click=${this._closeModal}></button>
            </div>
            <div class="modal-body">${body}</div>
            <div class="modal-footer">${footer}</div>
          </div>
        </div>
      </div>
    `;
  }

  _renderPagination() {
    const total = Math.ceil(this._total / PAGE_SIZE);
    if (total <= 1) return '';
    const pages = Array.from({ length: total }, (_, i) => i + 1);
    return html`
      <nav>
        <ul class="pagination pagination-sm mb-0">
          <li class="page-item ${this._page <= 1 ? 'disabled' : ''}">
            <button class="page-link" @click=${() => this._onPage(this._page - 1)}>‹</button>
          </li>
          ${pages.map(p => html`
            <li class="page-item ${p === this._page ? 'active' : ''}">
              <button class="page-link" @click=${() => this._onPage(p)}>${p}</button>
            </li>
          `)}
          <li class="page-item ${this._page >= total ? 'disabled' : ''}">
            <button class="page-link" @click=${() => this._onPage(this._page + 1)}>›</button>
          </li>
        </ul>
      </nav>
    `;
  }

  render() {
    return html`
      <div class="container-fluid py-4">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h4 class="mb-0">Gestione utenti</h4>
          <button class="btn btn-primary btn-sm" @click=${this._openCreate}>
            <i class="bi bi-plus-lg me-1"></i>Nuovo utente
          </button>
        </div>
        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}
        <div class="mb-3">
          <input class="form-control form-control-sm"
                 style="max-width:300px"
                 placeholder="Cerca per username o email…"
                 .value=${this._search}
                 @input=${this._onSearch}>
        </div>
        ${this._loading ? html`<div class="text-muted">Caricamento...</div>` : html`
          <div class="table-responsive">
            <table class="table table-hover table-sm align-middle">
              <thead class="table-light">
                <tr>
                  <th>#</th>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Ruolo</th>
                  <th>Attivo</th>
                  <th>Cambio pwd</th>
                  <th>Creato il</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                ${this._items.length === 0 ? html`
                  <tr><td colspan="8" class="text-muted text-center py-3">Nessun utente trovato</td></tr>
                ` : this._items.map(item => html`
                  <tr>
                    <td class="text-muted">${item.id}</td>
                    <td>${item.username}</td>
                    <td class="text-muted">${item.email ?? '—'}</td>
                    <td>${this._roleBadge(item.ruolo)}</td>
                    <td>
                      ${item.attivo
                        ? html`<span class="badge bg-success">sì</span>`
                        : html`<span class="badge bg-secondary">no</span>`}
                    </td>
                    <td>
                      ${item.must_change_password
                        ? html`<span class="badge bg-warning text-dark">sì</span>`
                        : html`<span class="text-muted">—</span>`}
                    </td>
                    <td class="text-muted">
                      ${item.created_at ? new Date(item.created_at).toLocaleDateString('it-IT') : '—'}
                    </td>
                    <td>
                      ${this._canAct(item) ? html`
                        <div class="d-flex gap-1">
                          <button class="btn btn-outline-secondary btn-sm" title="Modifica"
                                  @click=${() => this._openEdit(item)}>
                            <i class="bi bi-pencil"></i>
                          </button>
                          <button class="btn btn-outline-warning btn-sm" title="Reset password"
                                  @click=${() => this._openPassword(item)}>
                            <i class="bi bi-key"></i>
                          </button>
                          <button class="btn btn-outline-danger btn-sm" title="Disattiva"
                                  @click=${() => this._openDelete(item)}>
                            <i class="bi bi-trash"></i>
                          </button>
                        </div>
                      ` : ''}
                    </td>
                  </tr>
                `)}
              </tbody>
            </table>
          </div>
          <div class="d-flex justify-content-between align-items-center">
            <span class="text-muted small">${this._total} utenti totali</span>
            ${this._renderPagination()}
          </div>
        `}
        ${this._renderModal()}
      </div>
    `;
  }
}

customElements.define('user-admin-users', Users);
