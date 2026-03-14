import { LitElement, html } from 'lit';
import { user } from '../../store.js';

/**
 * Lista account.
 * Admin: tabella con tutti gli account, ricerca, modifica, disattiva/riattiva.
 * Operatore: card con i propri dati e pulsante modifica profilo.
 */
class AuthAccountListPage extends LitElement {

  static properties = {
    _user:    { state: true },
    _users:   { state: true },
    _loading: { state: true },
    _error:   { state: true },
    _search:  { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._user    = user.get();
    this._users   = [];
    this._loading = false;
    this._error   = null;
    this._search  = '';
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

  get _isAdmin() {
    return this._user?.ruolo === 'admin';
  }

  async _load() {
    const qs      = this._search ? `?q=${encodeURIComponent(this._search)}` : '';
    this._loading = true;
    this._error   = null;
    const r       = await fetch(`/api/auth/account${qs}`);
    const data    = await r.json();
    this._loading = false;
    if (!data.err) {
      this._users = Array.isArray(data.out) ? data.out : [];
    } else {
      this._error = data.log;
    }
  }

  async _toggleActive(u) {
    const action  = u.attivo ? 'disattivare' : 'riattivare';
    if (!confirm(`Vuoi ${action} l'account "${u.username}"?`)) { return; }
    const body = { username: u.username, email: u.email, ruolo: u.ruolo, attivo: !u.attivo };
    const r    = await fetch(`/api/auth/account/${u.id}`, {
      method:  'PUT',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(body),
    });
    const data = await r.json();
    if (!data.err) {
      this._load();
    } else {
      alert(data.log);
    }
  }

  async _delete(u) {
    if (!confirm(`Disattivare l'account "${u.username}"?`)) { return; }
    const r    = await fetch(`/api/auth/account/${u.id}`, { method: 'DELETE' });
    const data = await r.json();
    if (!data.err) {
      this._load();
    } else {
      alert(data.log);
    }
  }

  _renderAdminList() {
    return html`
      <div class="d-flex gap-2 mb-3 align-items-center flex-wrap">
        <input class="form-control w-auto flex-grow-1" placeholder="Cerca account..."
               .value=${this._search}
               @input=${e => { this._search = e.target.value; this._load(); }}>
        <button class="btn btn-primary btn-sm"
                @click=${() => { window.location.hash = '/auth/account/edit'; }}>
          + Nuovo account
        </button>
      </div>
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
                <tr class="${u.attivo ? '' : 'text-muted'}">
                  <td>${u.username}</td>
                  <td class="small">${u.email || '—'}</td>
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
                            @click=${() => { window.location.hash = `/auth/account/edit/${u.id}`; }}>
                      Modifica
                    </button>
                    <button class="btn btn-sm ${u.attivo ? 'btn-outline-danger' : 'btn-outline-success'}"
                            @click=${() => u.attivo ? this._delete(u) : this._toggleActive(u)}>
                      ${u.attivo ? 'Disattiva' : 'Riattiva'}
                    </button>
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
    const u = this._users[0];
    return html`
      <h5 class="mb-3">Il mio profilo</h5>
      ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}
      ${this._loading ? html`<div class="text-muted">Caricamento...</div>` : html`
        ${u ? html`
          <div class="card" style="max-width:420px">
            <div class="card-body">
              <p><strong>Username:</strong> ${u.username}</p>
              <p><strong>Email:</strong> ${u.email || '—'}</p>
              <p><strong>Ruolo:</strong>
                <span class="badge bg-secondary">${u.ruolo}</span>
              </p>
              <button class="btn btn-outline-primary btn-sm"
                      @click=${() => { window.location.hash = '/auth/account/edit'; }}>
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
        <h4 class="mb-4">${this._isAdmin ? 'Gestione account' : 'Il mio profilo'}</h4>
        ${this._isAdmin ? this._renderAdminList() : this._renderSelfProfile()}
      </div>
    `;
  }
}

customElements.define('auth-account-list-page', AuthAccountListPage);
