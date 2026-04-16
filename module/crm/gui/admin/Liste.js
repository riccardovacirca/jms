import { LitElement, html } from 'lit';
import './ImportaLista.js';

function emptyForm() {
  return { nome: '', descrizione: '', consenso: false, stato: 1, scadenza: '' };
}

/**
 * Componente di amministrazione delle liste contatti.
 * Gestisce la vista elenco, il form di creazione/modifica e il wizard di importazione.
 * Montato dalla dashboard tramite il tag `crm-admin-liste`.
 */
class AdminListe extends LitElement {

  static properties = {
    _view:      { state: true },
    _items:     { state: true },
    _total:     { state: true },
    _page:      { state: true },
    _size:      { state: true },
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
    this._loading   = false;
    this._error     = null;
    this._editing   = null;
    this._form      = emptyForm();
    this._deleteId  = null;
    this._formError = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadList();
  }

  async _loadList() {
    this._loading = true;
    this._error   = null;
    try {
      const res  = await fetch(`/api/liste?page=${this._page}&size=${this._size}`);
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

  _newLista() {
    this._editing   = null;
    this._form      = emptyForm();
    this._formError = null;
    this._view      = 'form';
  }

  _editLista(item) {
    this._editing = item;
    this._form = {
      nome:        item.nome        || '',
      descrizione: item.descrizione || '',
      consenso:    item.consenso    ?? false,
      stato:       item.stato       ?? 1,
      scadenza:    item.scadenza    || ''
    };
    this._formError = null;
    this._view      = 'form';
  }

  _updateField(field, val) {
    this._form = { ...this._form, [field]: val };
  }

  async _saveLista() {
    const body = {
      nome:        this._form.nome        || null,
      descrizione: this._form.descrizione || null,
      consenso:    this._form.consenso,
      stato:       this._form.stato,
      scadenza:    this._form.scadenza    || null
    };
    const url    = this._editing ? `/api/liste/${this._editing.id}` : '/api/liste';
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
      const res  = await fetch(`/api/liste/${id}`, { method: 'DELETE' });
      const data = await res.json();
      if (!data.err) {
        this._loadList();
      }
    } catch (e) {
      this._error = 'Errore nella cancellazione';
    }
  }

  async _setDefault(id) {
    try {
      const res  = await fetch(`/api/liste/${id}/default`, { method: 'PUT' });
      const data = await res.json();
      if (data.err) {
        this._error = data.log;
      } else {
        this._loadList();
      }
    } catch (e) {
      this._error = 'Errore di rete';
    }
  }

  render() {
    if (this._view === 'form')     return this._renderForm();
    if (this._view === 'importer') return this._renderImporter();
    return this._renderList();
  }

  _renderList() {
    const pages = Math.ceil(this._total / this._size);
    return html`
      <div class="container-fluid py-4">
        <div class="d-flex align-items-center gap-2 mb-4">
          <h1 class="h4 mb-0">Liste contatti</h1>
          <button class="btn btn-sm btn-outline-success ms-auto"
                  @click=${() => { this._view = 'importer'; }}>Importa</button>
          <button class="btn btn-sm btn-primary"
                  @click=${this._newLista}>Nuova lista</button>
        </div>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}

        ${this._deleteId ? html`
          <div class="alert alert-warning d-flex align-items-center gap-3 mb-3">
            <span>Eliminare la lista?</span>
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
                    <th>Scadenza</th>
                    <th>Stato</th>
                    <th>Contatti</th>
                    <th></th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  ${this._items.length === 0
                    ? html`<tr><td colspan="7" class="text-center text-muted py-4">Nessuna lista trovata</td></tr>`
                    : this._items.map(item => html`
                      <tr>
                        <td>
                          ${item.nome}
                          ${item.isDefault
                            ? html`<span class="badge bg-warning text-dark ms-2 small">default</span>`
                            : ''}
                        </td>
                        <td>${item.descrizione || ''}</td>
                        <td>${item.scadenza    || ''}</td>
                        <td>
                          <span class="badge ${item.stato === 1 ? 'bg-success' : 'bg-secondary'}">
                            ${item.stato === 1 ? 'Attiva' : 'Inattiva'}
                          </span>
                        </td>
                        <td><span class="badge bg-light text-dark border">${item.contattiCount ?? 0}</span></td>
                        <td>
                          ${!item.isDefault
                            ? html`<button class="btn btn-sm btn-outline-warning"
                                           @click=${() => this._setDefault(item.id)}>Imposta default</button>`
                            : ''}
                        </td>
                        <td class="text-end">
                          <button class="btn btn-sm btn-outline-primary me-1"
                                  @click=${() => this._editLista(item)}>Modifica</button>
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
            <p class="text-muted small mt-2">${this._total} liste totali</p>
          `}
      </div>`;
  }

  _renderForm() {
    const title = this._editing ? 'Modifica lista' : 'Nuova lista';
    return html`
      <div class="container py-4" style="max-width:640px">
        <div class="d-flex align-items-center gap-3 mb-4">
          <button class="btn btn-sm btn-outline-secondary"
                  @click=${() => { this._view = 'list'; }}>← Torna</button>
          <h1 class="h4 mb-0">${title}</h1>
          <button class="btn btn-sm btn-primary ms-auto" @click=${this._saveLista}>Salva</button>
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
            <textarea class="form-control" rows="2"
                      .value=${this._form.descrizione}
                      @input=${e => this._updateField('descrizione', e.target.value)}></textarea>
          </div>
          <div class="col-md-4">
            <label class="form-label">Scadenza</label>
            <input class="form-control" type="date" .value=${this._form.scadenza}
                   @input=${e => this._updateField('scadenza', e.target.value)}>
          </div>
          <div class="col-md-4">
            <label class="form-label">Stato</label>
            <select class="form-select"
                    @change=${e => this._updateField('stato', Number(e.target.value))}>
              <option value="1" ?selected=${this._form.stato === 1}>Attiva</option>
              <option value="0" ?selected=${this._form.stato === 0}>Inattiva</option>
            </select>
          </div>
          <div class="col-auto d-flex align-items-end pb-1">
            <div class="form-check">
              <input class="form-check-input" type="checkbox" id="chk-consenso-lista"
                     .checked=${this._form.consenso}
                     @change=${e => this._updateField('consenso', e.target.checked)}>
              <label class="form-check-label" for="chk-consenso-lista">Consenso</label>
            </div>
          </div>
        </div>
      </div>`;
  }

  _renderImporter() {
    return html`
      <crm-importa-lista
        .listaId=${null}
        @cancel=${() => { this._view = 'list'; }}
        @done=${() => { this._view = 'list'; this._loadList(); }}>
      </crm-importa-lista>`;
  }
}

customElements.define('crm-admin-liste', AdminListe);

export default AdminListe;
