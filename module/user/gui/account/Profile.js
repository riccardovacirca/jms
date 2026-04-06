import { LitElement, html } from 'lit';

class Profile extends LitElement {

  static properties = {
    _loading:         { state: true },
    _saving:          { state: true },
    _error:           { state: true },
    _formErr:         { state: true },
    _profile:         { state: true },
    _isNew:           { state: true },
    _editing:         { state: true },
    _form:            { state: true },
    _settings:        { state: true },
    _loadingSettings: { state: true },
    _newKey:          { state: true },
    _newVal:          { state: true },
    _settingsErr:     { state: true },
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._loading         = false;
    this._saving          = false;
    this._error           = null;
    this._formErr         = null;
    this._profile         = null;
    this._isNew           = false;
    this._editing         = false;
    this._form            = {};
    this._settings        = [];
    this._loadingSettings = false;
    this._newKey          = '';
    this._newVal          = '';
    this._settingsErr     = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._load();
  }

  async _load() {
    this._loading = true;
    this._error   = null;
    const r       = await fetch('/api/user/users/sid');
    const data    = await r.json();
    this._loading = false;
    if (!data.err) {
      this._profile = data.out;
      if (data.out === null) {
        this._isNew   = true;
        this._editing = true;
        this._form    = { nome: '', cognome: '', nickname: '', immagine: '' };
      } else {
        this._isNew   = false;
        this._editing = false;
        this._fillForm(data.out);
        this._loadSettings();
      }
    } else {
      this._error = data.log;
    }
  }

  _fillForm(p) {
    this._form = {
      nome:     p.nome     || '',
      cognome:  p.cognome  || '',
      nickname: p.nickname || '',
      immagine: p.immagine || '',
    };
  }

  _onInput(field, e) {
    this._form = { ...this._form, [field]: e.target.value };
  }

  async _save() {
    this._saving  = true;
    this._formErr = null;
    const r       = await fetch('/api/user/users/sid', {
      method:  'PUT',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(this._form),
    });
    const data    = await r.json();
    this._saving  = false;
    if (!data.err) {
      if (this._isNew) { this._load(); }
      else { this._profile = { ...this._profile, ...this._form }; this._editing = false; }
    } else {
      this._formErr = data.log;
    }
  }

  async _loadSettings() {
    this._loadingSettings = true;
    const r               = await fetch('/api/user/users/sid/settings');
    const data            = await r.json();
    this._loadingSettings = false;
    if (!data.err) { this._settings = data.out || []; }
    else { this._settingsErr = data.log; }
  }

  async _addSetting() {
    this._settingsErr = null;
    if (!this._newKey.trim()) { this._settingsErr = 'La chiave è obbligatoria'; return; }
    const r    = await fetch('/api/user/users/sid/settings', {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ chiave: this._newKey, valore: this._newVal }),
    });
    const data = await r.json();
    if (!data.err) { this._newKey = ''; this._newVal = ''; this._loadSettings(); }
    else { this._settingsErr = data.log; }
  }

  async _deleteSetting(chiave) {
    this._settingsErr = null;
    const r    = await fetch(`/api/user/users/sid/settings/${encodeURIComponent(chiave)}`, { method: 'DELETE' });
    const data = await r.json();
    if (!data.err) { this._loadSettings(); }
    else { this._settingsErr = data.log; }
  }

  _renderForm() {
    return html`
      <div class="card">
        <div class="card-body">
          ${this._formErr ? html`<div class="alert alert-danger py-2">${this._formErr}</div>` : ''}
          <div class="mb-3">
            <label class="form-label">Nome *</label>
            <input class="form-control" .value=${this._form.nome || ''}
                   ?disabled=${!this._editing} @input=${e => this._onInput('nome', e)}>
          </div>
          <div class="mb-3">
            <label class="form-label">Cognome *</label>
            <input class="form-control" .value=${this._form.cognome || ''}
                   ?disabled=${!this._editing} @input=${e => this._onInput('cognome', e)}>
          </div>
          <div class="mb-3">
            <label class="form-label">Nickname</label>
            <input class="form-control" .value=${this._form.nickname || ''}
                   ?disabled=${!this._editing} @input=${e => this._onInput('nickname', e)}>
          </div>
          <div class="mb-3">
            <label class="form-label">Immagine (URL)</label>
            <input class="form-control" .value=${this._form.immagine || ''}
                   ?disabled=${!this._editing} @input=${e => this._onInput('immagine', e)}>
          </div>
          ${this._editing ? html`
            <div class="d-flex gap-2">
              <button class="btn btn-primary" @click=${this._save} ?disabled=${this._saving}>
                ${this._saving ? 'Salvataggio...' : 'Salva'}
              </button>
              ${!this._isNew ? html`
                <button class="btn btn-outline-secondary"
                        @click=${() => { this._editing = false; this._fillForm(this._profile); }}>
                  Annulla
                </button>
              ` : ''}
            </div>
          ` : html`
            <button class="btn btn-outline-primary" @click=${() => { this._editing = true; }}>
              Modifica
            </button>
          `}
        </div>
      </div>
    `;
  }

  _renderSettings() {
    return html`
      <div class="card mt-4">
        <div class="card-header">Impostazioni</div>
        <div class="card-body">
          ${this._settingsErr ? html`<div class="alert alert-danger py-2">${this._settingsErr}</div>` : ''}
          ${this._loadingSettings ? html`<div class="text-muted">Caricamento...</div>` : html`
            ${this._settings.length === 0
              ? html`<p class="text-muted mb-3">Nessuna impostazione.</p>`
              : html`
                <table class="table table-sm mb-3">
                  <thead><tr><th>Chiave</th><th>Valore</th><th></th></tr></thead>
                  <tbody>
                    ${this._settings.map(s => html`
                      <tr>
                        <td class="align-middle">${s.chiave}</td>
                        <td class="align-middle">${s.valore ?? ''}</td>
                        <td class="text-end">
                          <button class="btn btn-outline-danger btn-sm"
                                  @click=${() => this._deleteSetting(s.chiave)}>
                            Elimina
                          </button>
                        </td>
                      </tr>
                    `)}
                  </tbody>
                </table>
              `}
            <div class="d-flex gap-2 align-items-end">
              <div class="flex-grow-1">
                <label class="form-label mb-1">Chiave</label>
                <input class="form-control form-control-sm" .value=${this._newKey}
                       @input=${e => { this._newKey = e.target.value; }}>
              </div>
              <div class="flex-grow-1">
                <label class="form-label mb-1">Valore</label>
                <input class="form-control form-control-sm" .value=${this._newVal}
                       @input=${e => { this._newVal = e.target.value; }}>
              </div>
              <button class="btn btn-outline-secondary btn-sm" @click=${this._addSetting}>
                Aggiungi
              </button>
            </div>
          `}
        </div>
      </div>
    `;
  }

  render() {
    return html`
      <div class="container py-4 w-max-600">
        <h4 class="mb-4">${this._isNew ? 'Crea profilo' : 'Il mio profilo'}</h4>
        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}
        ${this._loading ? html`<div class="text-muted">Caricamento...</div>` : html`
          ${this._renderForm()}
          ${!this._isNew ? this._renderSettings() : ''}
        `}
      </div>
    `;
  }
}

customElements.define("user-profile", Profile);
