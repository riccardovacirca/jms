import { LitElement, html } from 'lit';

function emptyForm() {
  return {
    nome: '', cognome: '', ragioneSociale: '', telefono: '',
    email: '', indirizzo: '', citta: '', cap: '', provincia: '',
    note: '', stato: 1, consenso: false, blacklist: false
  };
}

class ContattiComponent extends LitElement {

  static properties = {
    _view:      { state: true },
    _items:     { state: true },
    _total:     { state: true },
    _page:      { state: true },
    _size:      { state: true },
    _search:    { state: true },
    _loading:   { state: true },
    _error:     { state: true },
    _editing:   { state: true },
    _form:      { state: true },
    _deleteId:  { state: true },
    _formError: { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._view      = 'list';
    this._items     = [];
    this._total     = 0;
    this._page      = 1;
    this._size      = 20;
    this._search    = '';
    this._loading   = false;
    this._error     = null;
    this._editing   = null;
    this._form      = emptyForm();
    this._deleteId  = null;
    this._formError = null;
    this._searchTimer = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadList();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    clearTimeout(this._searchTimer);
  }

  async _loadList() {
    const url = this._search
      ? `/api/contatti/search?q=${encodeURIComponent(this._search)}&page=${this._page}&size=${this._size}`
      : `/api/contatti?page=${this._page}&size=${this._size}`;
    this._loading = true;
    this._error   = null;
    try {
      const res  = await fetch(url);
      const data = await res.json();
      if (data.err) {
        this._error = data.log;
      } else {
        this._items = data.out.items;
        this._total = data.out.total;
      }
    } catch (e) {
      this._error = 'Errore di rete';
    } finally {
      this._loading = false;
    }
  }

  _onSearch(e) {
    this._search = e.target.value;
    this._page   = 1;
    clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => this._loadList(), 400);
  }

  _goToPage(page) {
    this._page = page;
    this._loadList();
  }

  _newContact() {
    this._editing   = null;
    this._form      = emptyForm();
    this._formError = null;
    this._view      = 'form';
  }

  _editContact(item) {
    this._editing = item;
    this._form = {
      nome:           item.nome           || '',
      cognome:        item.cognome        || '',
      ragioneSociale: item.ragioneSociale || '',
      telefono:       item.telefono       || '',
      email:          item.email          || '',
      indirizzo:      item.indirizzo      || '',
      citta:          item.citta          || '',
      cap:            item.cap            || '',
      provincia:      item.provincia      || '',
      note:           item.note           || '',
      stato:          item.stato          ?? 1,
      consenso:       item.consenso       ?? false,
      blacklist:      item.blacklist      ?? false
    };
    this._formError = null;
    this._view      = 'form';
  }

  _updateField(field, val) {
    this._form = { ...this._form, [field]: val };
  }

  async _saveContact() {
    const body = {
      nome:            this._form.nome           || null,
      cognome:         this._form.cognome         || null,
      ragione_sociale: this._form.ragioneSociale  || null,
      telefono:        this._form.telefono,
      email:           this._form.email           || null,
      indirizzo:       this._form.indirizzo       || null,
      citta:           this._form.citta           || null,
      cap:             this._form.cap             || null,
      provincia:       this._form.provincia       || null,
      note:            this._form.note            || null,
      stato:           this._form.stato,
      consenso:        this._form.consenso,
      blacklist:       this._form.blacklist
    };
    const url    = this._editing ? `/api/contatti/${this._editing.id}` : '/api/contatti';
    const method = this._editing ? 'PUT' : 'POST';
    this._formError = null;
    try {
      const res  = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify(body)
      });
      const data = await res.json();
      if (data.err) {
        this._formError = data.log;
      } else {
        this._view = 'list';
        this._loadList();
      }
    } catch (e) {
      this._formError = 'Errore di rete';
    }
  }

  _confirmDelete(id) {
    this._deleteId = id;
  }

  async _doDelete() {
    const id = this._deleteId;
    this._deleteId = null;
    try {
      const res  = await fetch(`/api/contatti/${id}`, { method: 'DELETE' });
      const data = await res.json();
      if (!data.err) {
        this._loadList();
      }
    } catch (e) {
      this._error = 'Errore nella cancellazione';
    }
  }

  render() {
    if (this._view === 'form') {
      return this._renderForm();
    }
    return this._renderList();
  }

  _renderList() {
    const pages = Math.ceil(this._total / this._size);
    return html`
      <div class="container-fluid py-4">
        <div class="d-flex align-items-center gap-2 mb-4">
          <h1 class="h4 mb-0">Contatti</h1>
          <div class="ms-auto d-flex gap-2">
            <input class="form-control form-control-sm" type="search"
                   placeholder="Cerca..." style="width:220px"
                   .value=${this._search} @input=${this._onSearch}>
            <button class="btn btn-sm btn-primary" @click=${this._newContact}>Nuovo</button>
          </div>
        </div>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}

        ${this._deleteId ? html`
          <div class="alert alert-warning d-flex align-items-center gap-3 mb-3">
            <span>Eliminare il contatto?</span>
            <button class="btn btn-sm btn-danger"    @click=${this._doDelete}>Conferma</button>
            <button class="btn btn-sm btn-secondary" @click=${() => { this._deleteId = null; }}>Annulla</button>
          </div>` : ''}

        ${this._loading
          ? html`<p class="text-muted">Caricamento...</p>`
          : html`
            <div class="table-responsive">
              <table class="table table-sm table-hover align-middle">
                <thead class="table-light">
                  <tr>
                    <th>Cognome</th>
                    <th>Nome</th>
                    <th>Ragione Sociale</th>
                    <th>Telefono</th>
                    <th>Email</th>
                    <th>Stato</th>
                    <th>Liste</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  ${this._items.length === 0
                    ? html`<tr><td colspan="8" class="text-center text-muted py-4">Nessun contatto trovato</td></tr>`
                    : this._items.map(item => html`
                      <tr>
                        <td>${item.cognome         || ''}</td>
                        <td>${item.nome             || ''}</td>
                        <td>${item.ragioneSociale   || ''}</td>
                        <td>${item.telefono         || ''}</td>
                        <td>${item.email            || ''}</td>
                        <td>
                          ${item.blacklist
                            ? html`<span class="badge bg-danger">Blacklist</span>`
                            : html`<span class="badge ${item.stato === 1 ? 'bg-success' : 'bg-secondary'}">${item.stato === 1 ? 'Attivo' : 'Inattivo'}</span>`}
                        </td>
                        <td><span class="badge bg-light text-dark border">${item.listeCount ?? 0}</span></td>
                        <td class="text-end">
                          <button class="btn btn-sm btn-outline-primary me-1"
                                  @click=${() => this._editContact(item)}>Modifica</button>
                          <button class="btn btn-sm btn-outline-danger"
                                  @click=${() => this._confirmDelete(item.id)}>Elimina</button>
                        </td>
                      </tr>`)}
                </tbody>
              </table>
            </div>
            ${pages > 1 ? html`
              <nav>
                <ul class="pagination pagination-sm">
                  ${Array.from({ length: pages }, (_, i) => i + 1).map(p => html`
                    <li class="page-item ${p === this._page ? 'active' : ''}">
                      <button class="page-link" @click=${() => this._goToPage(p)}>${p}</button>
                    </li>`)}
                </ul>
              </nav>` : ''}
            <p class="text-muted small mt-2">${this._total} contatti totali</p>
          `}
      </div>`;
  }

  _renderForm() {
    const title = this._editing ? 'Modifica contatto' : 'Nuovo contatto';
    return html`
      <div class="container py-4" style="max-width:720px">
        <div class="d-flex align-items-center gap-3 mb-4">
          <button class="btn btn-sm btn-outline-secondary"
                  @click=${() => { this._view = 'list'; }}>← Torna</button>
          <h1 class="h4 mb-0">${title}</h1>
          <button class="btn btn-sm btn-primary ms-auto" @click=${this._saveContact}>Salva</button>
        </div>

        ${this._formError ? html`<div class="alert alert-danger">${this._formError}</div>` : ''}

        <div class="row g-3">
          <div class="col-md-4">
            <label class="form-label">Nome</label>
            <input class="form-control" .value=${this._form.nome}
                   @input=${e => this._updateField('nome', e.target.value)}>
          </div>
          <div class="col-md-4">
            <label class="form-label">Cognome</label>
            <input class="form-control" .value=${this._form.cognome}
                   @input=${e => this._updateField('cognome', e.target.value)}>
          </div>
          <div class="col-md-4">
            <label class="form-label">Ragione Sociale</label>
            <input class="form-control" .value=${this._form.ragioneSociale}
                   @input=${e => this._updateField('ragioneSociale', e.target.value)}>
          </div>
          <div class="col-md-4">
            <label class="form-label">Telefono <span class="text-danger">*</span></label>
            <input class="form-control" .value=${this._form.telefono}
                   @input=${e => this._updateField('telefono', e.target.value)}>
          </div>
          <div class="col-md-8">
            <label class="form-label">Email</label>
            <input class="form-control" type="email" .value=${this._form.email}
                   @input=${e => this._updateField('email', e.target.value)}>
          </div>
          <div class="col-12">
            <label class="form-label">Indirizzo</label>
            <input class="form-control" .value=${this._form.indirizzo}
                   @input=${e => this._updateField('indirizzo', e.target.value)}>
          </div>
          <div class="col-md-6">
            <label class="form-label">Città</label>
            <input class="form-control" .value=${this._form.citta}
                   @input=${e => this._updateField('citta', e.target.value)}>
          </div>
          <div class="col-md-2">
            <label class="form-label">CAP</label>
            <input class="form-control" .value=${this._form.cap}
                   @input=${e => this._updateField('cap', e.target.value)}>
          </div>
          <div class="col-md-4">
            <label class="form-label">Provincia</label>
            <input class="form-control" .value=${this._form.provincia}
                   @input=${e => this._updateField('provincia', e.target.value)}>
          </div>
          <div class="col-12">
            <label class="form-label">Note</label>
            <textarea class="form-control" rows="3"
                      .value=${this._form.note}
                      @input=${e => this._updateField('note', e.target.value)}></textarea>
          </div>
          <div class="col-md-3">
            <label class="form-label">Stato</label>
            <select class="form-select"
                    @change=${e => this._updateField('stato', Number(e.target.value))}>
              <option value="1" ?selected=${this._form.stato === 1}>Attivo</option>
              <option value="0" ?selected=${this._form.stato === 0}>Inattivo</option>
            </select>
          </div>
          <div class="col-auto d-flex align-items-end pb-1">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="chk-consenso"
                     .checked=${this._form.consenso}
                     @change=${e => this._updateField('consenso', e.target.checked)}>
              <label class="form-check-label" for="chk-consenso">Consenso</label>
            </div>
          </div>
          <div class="col-auto d-flex align-items-end pb-1">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="chk-blacklist"
                     .checked=${this._form.blacklist}
                     @change=${e => this._updateField('blacklist', e.target.checked)}>
              <label class="form-check-label" for="chk-blacklist">Blacklist</label>
            </div>
          </div>
        </div>
      </div>`;
  }
}

customElements.define('contatti-layout', ContattiComponent);

export default ContattiComponent;
