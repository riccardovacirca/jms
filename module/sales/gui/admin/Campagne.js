import { LitElement, html } from 'lit';

function emptyForm() {
  return { nome: '', descrizione: '', stato: 1 };
}

/**
 * Componente di amministrazione delle campagne.
 * Gestisce la vista elenco, il form di creazione/modifica e la gestione delle liste associate.
 * Montato dalla dashboard tramite il tag `sales-admin-campagne`.
 */
class AdminCampagne extends LitElement {

  static properties = {
    _view:       { state: true },
    _items:      { state: true },
    _total:      { state: true },
    _page:       { state: true },
    _size:       { state: true },
    _loading:    { state: true },
    _error:      { state: true },
    _editing:    { state: true },
    _form:       { state: true },
    _deleteId:   { state: true },
    _formError:  { state: true },
    _campagna:   { state: true },
    _liste:      { state: true },
    _ltTotal:    { state: true },
    _ltPage:     { state: true },
    _ltLoading:  { state: true },
    _addSearch:  { state: true },
    _addResults: { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._view       = 'list';
    this._items      = [];
    this._total      = 0;
    this._page       = 1;
    this._size       = 20;
    this._loading    = false;
    this._error      = null;
    this._editing    = null;
    this._form       = emptyForm();
    this._deleteId   = null;
    this._formError  = null;
    this._campagna   = null;
    this._liste      = [];
    this._ltTotal    = 0;
    this._ltPage     = 1;
    this._ltLoading  = false;
    this._addSearch  = '';
    this._addResults = [];
    this._addTimer   = null;
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    clearTimeout(this._addTimer);
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadList();
  }

  async _loadList() {
    this._loading = true;
    this._error   = null;
    try {
      const res  = await fetch(`/api/sales/campagne?page=${this._page}&size=${this._size}`);
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

  _goToPage(page) {
    this._page = page;
    this._loadList();
  }

  _newCampagna() {
    this._editing   = null;
    this._form      = emptyForm();
    this._formError = null;
    this._view      = 'form';
  }

  _editCampagna(item) {
    this._editing = item;
    this._form = {
      nome:        item.nome        || '',
      descrizione: item.descrizione || '',
      stato:       item.stato       ?? 1
    };
    this._formError = null;
    this._view      = 'form';
  }

  _updateField(field, val) {
    this._form = { ...this._form, [field]: val };
  }

  async _saveCampagna() {
    const body = {
      nome:        this._form.nome        || null,
      descrizione: this._form.descrizione || null,
      stato:       this._form.stato
    };
    const url    = this._editing ? `/api/sales/campagne/${this._editing.id}` : '/api/sales/campagne';
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
      const res  = await fetch(`/api/sales/campagne/${id}`, { method: 'DELETE' });
      const data = await res.json();
      if (!data.err) {
        this._loadList();
      }
    } catch (e) {
      this._error = 'Errore nella cancellazione';
    }
  }

  async _openListe(item) {
    this._campagna = item;
    this._ltPage   = 1;
    this._view     = 'liste';
    await this._loadListe();
  }

  async _loadListe() {
    this._ltLoading = true;
    try {
      const res  = await fetch(`/api/sales/campagne/${this._campagna.id}/liste?page=${this._ltPage}&size=${this._size}`);
      const data = await res.json();
      if (!data.err) {
        this._liste   = data.out.items;
        this._ltTotal = data.out.total;
      }
    } catch (e) {
      /* silent */
    } finally {
      this._ltLoading = false;
    }
  }

  _goToLtPage(page) {
    this._ltPage = page;
    this._loadListe();
  }

  _onAddSearch(e) {
    this._addSearch = e.target.value;
    clearTimeout(this._addTimer);
    if (!this._addSearch.trim()) {
      this._addResults = [];
      return;
    }
    this._addTimer = setTimeout(async () => {
      try {
        const res  = await fetch(`/api/sales/liste?page=1&size=8`);
        const data = await res.json();
        if (!data.err) {
          const q       = this._addSearch.toLowerCase();
          const already = new Set(this._liste.map(l => l.id));
          this._addResults = data.out.items.filter(
            l => !already.has(l.id) && l.nome.toLowerCase().includes(q)
          );
        }
      } catch (e) { /* silent */ }
    }, 300);
  }

  async _addLista(listaId) {
    try {
      const res  = await fetch(`/api/sales/campagne/${this._campagna.id}/liste`, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ listaId })
      });
      const data = await res.json();
      if (!data.err) {
        this._addSearch  = '';
        this._addResults = [];
        this._loadListe();
      }
    } catch (e) { /* silent */ }
  }

  async _removeLista(listaId) {
    try {
      const res  = await fetch(`/api/sales/campagne/${this._campagna.id}/liste/${listaId}`, { method: 'DELETE' });
      const data = await res.json();
      if (!data.err) {
        this._loadListe();
      }
    } catch (e) { /* silent */ }
  }

  render() {
    if (this._view === 'form')  return this._renderForm();
    if (this._view === 'liste') return this._renderListe();
    return this._renderList();
  }

  _renderList() {
    const pages = Math.ceil(this._total / this._size);
    return html`
      <div class="container-fluid py-4">
        <div class="d-flex align-items-center gap-2 mb-4">
          <h1 class="h4 mb-0">Campagne</h1>
          <button class="btn btn-sm btn-primary ms-auto"
                  @click=${this._newCampagna}>Nuova campagna</button>
        </div>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}

        ${this._deleteId ? html`
          <div class="alert alert-warning d-flex align-items-center gap-3 mb-3">
            <span>Eliminare la campagna?</span>
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
                    <th>Nome</th>
                    <th>Descrizione</th>
                    <th>Stato</th>
                    <th>Liste</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  ${this._items.length === 0
                    ? html`<tr><td colspan="5" class="text-center text-muted py-4">Nessuna campagna trovata</td></tr>`
                    : this._items.map(item => html`
                      <tr>
                        <td>
                          <button class="btn btn-link btn-sm p-0 text-start"
                                  @click=${() => this._openListe(item)}>${item.nome}</button>
                        </td>
                        <td>${item.descrizione || ''}</td>
                        <td>
                          <span class="badge ${item.stato === 1 ? 'bg-success' : 'bg-secondary'}">
                            ${item.stato === 1 ? 'Attiva' : 'Inattiva'}
                          </span>
                        </td>
                        <td><span class="badge bg-light text-dark border">${item.listeCount ?? 0}</span></td>
                        <td class="text-end">
                          <button class="btn btn-sm btn-outline-primary me-1"
                                  @click=${() => this._editCampagna(item)}>Modifica</button>
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
            <p class="text-muted small mt-2">${this._total} campagne totali</p>
          `}
      </div>`;
  }

  _renderForm() {
    const title = this._editing ? 'Modifica campagna' : 'Nuova campagna';
    return html`
      <div class="container py-4" style="max-width:640px">
        <div class="d-flex align-items-center gap-3 mb-4">
          <button class="btn btn-sm btn-outline-secondary"
                  @click=${() => { this._view = 'list'; }}>← Torna</button>
          <h1 class="h4 mb-0">${title}</h1>
          <button class="btn btn-sm btn-primary ms-auto" @click=${this._saveCampagna}>Salva</button>
        </div>

        ${this._formError ? html`<div class="alert alert-danger">${this._formError}</div>` : ''}

        <div class="row g-3">
          <div class="col-12">
            <label class="form-label">Nome <span class="text-danger">*</span></label>
            <input class="form-control" .value=${this._form.nome}
                   @input=${e => this._updateField('nome', e.target.value)}>
          </div>
          <div class="col-12">
            <label class="form-label">Descrizione</label>
            <textarea class="form-control" rows="3"
                      .value=${this._form.descrizione}
                      @input=${e => this._updateField('descrizione', e.target.value)}></textarea>
          </div>
          <div class="col-md-4">
            <label class="form-label">Stato</label>
            <select class="form-select"
                    @change=${e => this._updateField('stato', Number(e.target.value))}>
              <option value="1" ?selected=${this._form.stato === 1}>Attiva</option>
              <option value="0" ?selected=${this._form.stato === 0}>Inattiva</option>
            </select>
          </div>
        </div>
      </div>`;
  }

  _renderListe() {
    const pages = Math.ceil(this._ltTotal / this._size);
    return html`
      <div class="container-fluid py-4">
        <div class="d-flex align-items-center gap-2 mb-4">
          <button class="btn btn-sm btn-outline-secondary"
                  @click=${() => { this._view = 'list'; this._campagna = null; }}>← Torna</button>
          <h1 class="h4 mb-0">Campagna: ${this._campagna?.nome}</h1>
          <span class="badge bg-light text-dark border ms-2">${this._ltTotal} liste</span>
        </div>

        <div class="mb-3" style="max-width:460px;position:relative">
          <input class="form-control form-control-sm" type="search"
                 placeholder="Cerca lista da aggiungere..."
                 .value=${this._addSearch} @input=${this._onAddSearch}>
          ${this._addResults.length > 0 ? html`
            <div class="list-group shadow-sm"
                 style="position:absolute;z-index:10;width:100%;top:calc(100% + 2px)">
              ${this._addResults.map(l => html`
                <button type="button"
                        class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-1 px-2"
                        @click=${() => this._addLista(l.id)}>
                  <span>${l.nome}</span>
                  <span class="badge bg-primary ms-2">Aggiungi</span>
                </button>`)}
            </div>` : ''}
        </div>

        ${this._ltLoading
          ? html`<p class="text-muted">Caricamento...</p>`
          : html`
            <div class="table-responsive">
              <table class="table table-sm table-hover align-middle">
                <thead class="table-light">
                  <tr>
                    <th>Nome lista</th>
                    <th>Stato</th>
                    <th>Contatti</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  ${this._liste.length === 0
                    ? html`<tr><td colspan="4" class="text-center text-muted py-4">Nessuna lista associata</td></tr>`
                    : this._liste.map(l => html`
                      <tr>
                        <td>
                          ${l.nome}
                          ${l.isDefault
                            ? html`<span class="badge bg-warning text-dark ms-2 small">default</span>`
                            : ''}
                        </td>
                        <td>
                          <span class="badge ${l.stato === 1 ? 'bg-success' : 'bg-secondary'}">
                            ${l.stato === 1 ? 'Attiva' : 'Inattiva'}
                          </span>
                        </td>
                        <td><span class="badge bg-light text-dark border">${l.contattiCount ?? 0}</span></td>
                        <td class="text-end">
                          <button class="btn btn-sm btn-outline-danger"
                                  @click=${() => this._removeLista(l.id)}>Rimuovi</button>
                        </td>
                      </tr>`)}
                </tbody>
              </table>
            </div>
            ${pages > 1 ? html`
              <nav>
                <ul class="pagination pagination-sm">
                  ${Array.from({ length: pages }, (_, i) => i + 1).map(p => html`
                    <li class="page-item ${p === this._ltPage ? 'active' : ''}">
                      <button class="page-link" @click=${() => this._goToLtPage(p)}>${p}</button>
                    </li>`)}
                </ul>
              </nav>` : ''}
          `}
      </div>`;
  }
}

customElements.define('sales-admin-campagne', AdminCampagne);

export default AdminCampagne;
