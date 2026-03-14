import { LitElement, html } from 'lit';

/**
 * Pagina profilo utente (self-service).
 * Crea il profilo se non esiste ancora, altrimenti permette di modificarlo.
 */
class UserProfilePage extends LitElement {

  static properties = {
    _loading:         { state: true },
    _saving:          { state: true },
    _deleting:        { state: true },
    _error:           { state: true },
    _formErr:         { state: true },
    _profile:         { state: true },
    _isNew:           { state: true },
    _editing:         { state: true },
    _form:            { state: true },
    _confirm:         { state: true },
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
    this._deleting        = false;
    this._error           = null;
    this._formErr         = null;
    this._profile         = null;
    this._isNew           = false;
    this._editing         = false;
    this._form            = {};
    this._confirm         = false;
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
    const r       = await fetch('/api/user');
    const data    = await r.json();
    this._loading = false;
    if (!data.err) {
      this._profile = data.out;
      if (data.out === null) {
        this._isNew   = true;
        this._editing = true;
        this._form    = { nome: '', cognome: '', nickname: '', immagine: '', flags: 0 };
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
      flags:    p.flags    ?? 0,
    };
  }

  _onInput(field, e) {
    const val  = e.target.type === 'number' ? parseInt(e.target.value, 10) || 0 : e.target.value;
    this._form = { ...this._form, [field]: val };
  }

  async _save() {
    this._saving  = true;
    this._formErr = null;
    const method  = this._isNew ? 'POST' : 'PUT';
    const r       = await fetch('/api/user', {
      method,
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(this._form),
    });
    const data    = await r.json();
    this._saving  = false;
    if (!data.err) {
      if (this._isNew) {
        this._load();
      } else {
        this._profile = { ...this._profile, ...this._form };
        this._editing = false;
      }
    } else {
      this._formErr = data.log;
    }
  }

  async _deleteProfile() {
    this._deleting = true;
    const r        = await fetch('/api/user', { method: 'DELETE' });
    const data     = await r.json();
    this._deleting = false;
    if (!data.err) {
      window.location.hash = '/home';
    } else {
      this._error   = data.log;
      this._confirm = false;
    }
  }

  async _loadSettings() {
    this._loadingSettings = true;
    this._settingsErr     = null;
    const r               = await fetch('/api/user/settings');
    const data            = await r.json();
    this._loadingSettings = false;
    if (!data.err) {
      this._settings = data.out || [];
    } else {
      this._settingsErr = data.log;
    }
  }

  async _addSetting() {
    this._settingsErr = null;
    if (!this._newKey.trim()) {
      this._settingsErr = 'La chiave è obbligatoria';
      return;
    }
    const r    = await fetch(`/api/user/settings/${encodeURIComponent(this._newKey)}`, {
      method:  'PUT',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify({ valore: this._newVal }),
    });
    const data = await r.json();
    if (!data.err) {
      this._newKey = '';
      this._newVal = '';
      this._loadSettings();
    } else {
      this._settingsErr = data.log;
    }
  }

  async _deleteSetting(chiave) {
    this._settingsErr = null;
    const r    = await fetch(`/api/user/settings/${encodeURIComponent(chiave)}`, { method: 'DELETE' });
    const data = await r.json();
    if (!data.err) {
      this._loadSettings();
    } else {
      this._settingsErr = data.log;
    }
  }

  _renderForm() {
    return html`
      <div class="card">
        <div class="card-body">
          ${this._formErr ? html`<div class="alert alert-danger py-2">${this._formErr}</div>` : ''}

          <div class="mb-3">
            <label class="form-label">Nome *</label>
            <input class="form-control" .value=${this._form.nome || ''}
                   ?disabled=${!this._editing}
                   @input=${e => this._onInput('nome', e)}>
          </div>

          <div class="mb-3">
            <label class="form-label">Cognome *</label>
            <input class="form-control" .value=${this._form.cognome || ''}
                   ?disabled=${!this._editing}
                   @input=${e => this._onInput('cognome', e)}>
          </div>

          <div class="mb-3">
            <label class="form-label">Nickname</label>
            <input class="form-control" .value=${this._form.nickname || ''}
                   ?disabled=${!this._editing}
                   @input=${e => this._onInput('nickname', e)}>
          </div>

          <div class="mb-3">
            <label class="form-label">Immagine (URL)</label>
            <input class="form-control" .value=${this._form.immagine || ''}
                   ?disabled=${!this._editing}
                   @input=${e => this._onInput('immagine', e)}>
          </div>

          <div class="mb-3">
            <label class="form-label">Flags</label>
            <input class="form-control" type="number" min="0" .value=${String(this._form.flags ?? 0)}
                   ?disabled=${!this._editing}
                   @input=${e => this._onInput('flags', e)}>
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

  _renderDanger() {
    return html`
      <div class="card border-danger mt-4">
        <div class="card-body">
          <h6 class="text-danger mb-2">Zona pericolosa</h6>
          ${this._confirm ? html`
            <p class="small mb-2">Sei sicuro? Il profilo verrà disattivato.</p>
            <div class="d-flex gap-2">
              <button class="btn btn-danger btn-sm"
                      @click=${this._deleteProfile} ?disabled=${this._deleting}>
                ${this._deleting ? 'Eliminazione...' : 'Sì, disattiva profilo'}
              </button>
              <button class="btn btn-outline-secondary btn-sm"
                      @click=${() => { this._confirm = false; }}>
                Annulla
              </button>
            </div>
          ` : html`
            <button class="btn btn-outline-danger btn-sm"
                    @click=${() => { this._confirm = true; }}>
              Disattiva profilo
            </button>
          `}
        </div>
      </div>
    `;
  }

  render() {
    const title = this._isNew ? 'Crea profilo' : 'Il mio profilo';
    return html`
      <div class="container py-4" style="max-width:600px">
        <h4 class="mb-4">${title}</h4>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}
        ${this._loading ? html`<div class="text-muted">Caricamento...</div>` : html`
          ${this._renderForm()}
          ${!this._isNew ? this._renderSettings() : ''}
          ${!this._isNew ? this._renderDanger() : ''}
        `}
      </div>
    `;
  }
}

customElements.define('user-profile-page', UserProfilePage);
