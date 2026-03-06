import { LitElement, html } from 'lit';

class RapportiComponent extends LitElement {

  static properties = {
    _view:         { state: true },
    _items:        { state: true },
    _total:        { state: true },
    _page:         { state: true },
    _size:         { state: true },
    _search:       { state: true },
    _loading:      { state: true },
    _error:        { state: true },
    _detailId:     { state: true },
    _detail:       { state: true },
    _formOptions:  { state: true },
    _statoFilter:  { state: true }
  };

  createRenderRoot() { return this; }

  constructor() {
    super();
    this._view         = 'list';
    this._items        = [];
    this._total        = 0;
    this._page         = 0;        // start index for DataTable
    this._size         = 20;
    this._search       = {
      matricola: '',
      cognome: '',
      nome: '',
      cod_fis: '',
      cod_ente: ''
    };
    this._loading      = false;
    this._error        = null;
    this._detailId     = null;
    this._detail       = null;
    this._formOptions  = null;
    this._statoFilter  = 'tutti';  // tutti | attivi | disattivi
    this._searchTimer  = null;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadFormOptions();
    this._loadList();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    clearTimeout(this._searchTimer);
  }

  async _loadFormOptions() {
    // Carica opzioni per filtri (select cod_ente, ecc.)
    try {
      const res = await fetch('/api/scai/rapporto/create', { credentials: 'include' });
      const data = await res.json();
      if (data.data && data.data.form_select) {
        this._formOptions = data.data.form_select;
      }
    } catch (e) {
      console.error('Errore caricamento opzioni form:', e);
    }
  }

  async _loadList() {
    this._loading = true;
    this._error   = null;

    const params = new URLSearchParams({
      draw: '1',
      start: String(this._page),
      length: String(this._size),
      orderby: 'id',
      ordertype: 'desc'
    });

    // Aggiungi filtri di ricerca
    if (this._search.matricola) params.append('search[matricola]', this._search.matricola);
    if (this._search.cognome) params.append('search[cognome]', this._search.cognome);
    if (this._search.nome) params.append('search[nome]', this._search.nome);
    if (this._search.cod_fis) params.append('search[cod_fis]', this._search.cod_fis);
    if (this._search.cod_ente) params.append('search[cod_ente]', this._search.cod_ente);
    if (this._statoFilter !== 'tutti') params.append('search[stato]', this._statoFilter);

    try {
      const res  = await fetch(`/api/scai/rapporto?${params}`, { credentials: 'include' });
      const data = await res.json();

      if (data.data && data.data.data) {
        this._items = data.data.data;
        this._total = data.data.recordsFiltered || 0;
      } else {
        this._error = 'Formato risposta non valido';
      }
    } catch (e) {
      this._error = 'Errore di rete';
      console.error('Errore loadList:', e);
    } finally {
      this._loading = false;
    }
  }

  _onSearchChange(field, value) {
    this._search = { ...this._search, [field]: value };
    this._page = 0;
    clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => this._loadList(), 400);
  }

  _onStatoFilterChange(stato) {
    this._statoFilter = stato;
    this._page = 0;
    this._loadList();
  }

  _goToPage(pageIndex) {
    this._page = pageIndex * this._size;
    this._loadList();
  }

  async _viewDetail(rapportoId) {
    this._loading = true;
    this._error = null;
    this._detailId = rapportoId;

    try {
      const res = await fetch(`/api/scai/rapporto/${rapportoId}`, { credentials: 'include' });
      const data = await res.json();

      if (data.data) {
        this._detail = data.data;
        this._view = 'detail';
      } else {
        this._error = 'Rapporto non trovato';
      }
    } catch (e) {
      this._error = 'Errore caricamento dettaglio';
      console.error('Errore viewDetail:', e);
    } finally {
      this._loading = false;
    }
  }

  _downloadFoto(rapportoId) {
    window.open(`/api/scai/rapporto/foto/${rapportoId}`, '_blank');
  }

  _backToList() {
    this._view = 'list';
    this._detail = null;
    this._detailId = null;
  }

  render() {
    if (this._view === 'detail') {
      return this._renderDetail();
    }
    return this._renderList();
  }

  _renderList() {
    const pages = Math.ceil(this._total / this._size);
    const currentPage = Math.floor(this._page / this._size);

    return html`
      <div class="container-fluid py-4">
        <div class="d-flex align-items-center gap-2 mb-4">
          <h1 class="h4 mb-0">Rapporti di Lavoro</h1>
          <span class="badge bg-secondary ms-2">${this._total} totali</span>
        </div>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}

        <!-- Filtri di ricerca -->
        <div class="card mb-3">
          <div class="card-body">
            <div class="row g-3">
              <div class="col-md-2">
                <label class="form-label small">Codice Ente</label>
                ${this._formOptions?.cod_ente
                  ? html`
                    <select class="form-select form-select-sm"
                            @change=${(e) => this._onSearchChange('cod_ente', e.target.value)}>
                      <option value="">Tutti</option>
                      ${this._formOptions.cod_ente.map(opt => html`
                        <option value="${opt.code}">${opt.name}</option>
                      `)}
                    </select>
                  `
                  : html`
                    <input type="text" class="form-control form-control-sm"
                           placeholder="Codice ente..."
                           .value=${this._search.cod_ente}
                           @input=${(e) => this._onSearchChange('cod_ente', e.target.value)}>
                  `}
              </div>
              <div class="col-md-2">
                <label class="form-label small">Matricola</label>
                <input type="text" class="form-control form-control-sm"
                       placeholder="Matricola..."
                       .value=${this._search.matricola}
                       @input=${(e) => this._onSearchChange('matricola', e.target.value)}>
              </div>
              <div class="col-md-2">
                <label class="form-label small">Cognome</label>
                <input type="text" class="form-control form-control-sm"
                       placeholder="Cognome..."
                       .value=${this._search.cognome}
                       @input=${(e) => this._onSearchChange('cognome', e.target.value)}>
              </div>
              <div class="col-md-2">
                <label class="form-label small">Nome</label>
                <input type="text" class="form-control form-control-sm"
                       placeholder="Nome..."
                       .value=${this._search.nome}
                       @input=${(e) => this._onSearchChange('nome', e.target.value)}>
              </div>
              <div class="col-md-2">
                <label class="form-label small">Codice Fiscale</label>
                <input type="text" class="form-control form-control-sm"
                       placeholder="Codice fiscale..."
                       .value=${this._search.cod_fis}
                       @input=${(e) => this._onSearchChange('cod_fis', e.target.value)}>
              </div>
              <div class="col-md-2">
                <label class="form-label small">Stato</label>
                <select class="form-select form-select-sm"
                        @change=${(e) => this._onStatoFilterChange(e.target.value)}>
                  <option value="tutti" ?selected=${this._statoFilter === 'tutti'}>Tutti</option>
                  <option value="attivi" ?selected=${this._statoFilter === 'attivi'}>Attivi</option>
                  <option value="disattivi" ?selected=${this._statoFilter === 'disattivi'}>Cessati</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        ${this._loading
          ? html`<p class="text-muted">Caricamento...</p>`
          : html`
            <div class="table-responsive">
              <table class="table table-sm table-hover align-middle">
                <thead class="table-light">
                  <tr>
                    <th>Matricola</th>
                    <th>Cognome</th>
                    <th>Nome</th>
                    <th>Cod. Fiscale</th>
                    <th>Ente</th>
                    <th>Assunzione</th>
                    <th>Cessazione</th>
                    <th>Stato</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  ${this._items.length === 0
                    ? html`<tr><td colspan="9" class="text-center text-muted py-4">Nessun rapporto trovato</td></tr>`
                    : this._items.map(item => html`
                      <tr>
                        <td><strong>${item.matricola || '-'}</strong></td>
                        <td>${item.cognome || '-'}</td>
                        <td>${item.nome || '-'}</td>
                        <td><small class="font-monospace">${item.cod_fis || '-'}</small></td>
                        <td><span class="badge bg-info">${item.cod_ente || '-'}</span></td>
                        <td><small>${this._formatDate(item.data_assunzione)}</small></td>
                        <td><small>${this._formatDate(item.data_cessazione) || 'In corso'}</small></td>
                        <td>
                          ${item.data_cessazione && new Date(item.data_cessazione) < new Date()
                            ? html`<span class="badge bg-secondary">Cessato</span>`
                            : html`<span class="badge bg-success">Attivo</span>`}
                        </td>
                        <td class="text-end">
                          <button class="btn btn-sm btn-outline-primary"
                                  @click=${() => this._viewDetail(item.id)}>Dettaglio</button>
                        </td>
                      </tr>`)}
                </tbody>
              </table>
            </div>

            ${pages > 1 ? html`
              <nav aria-label="Paginazione rapporti">
                <ul class="pagination pagination-sm">
                  <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                    <button class="page-link" @click=${() => this._goToPage(currentPage - 1)}
                            ?disabled=${currentPage === 0}>Precedente</button>
                  </li>
                  ${Array.from({ length: Math.min(pages, 10) }, (_, i) => {
                    const pageNum = currentPage < 5 ? i : (currentPage > pages - 5 ? pages - 10 + i : currentPage - 5 + i);
                    return pageNum >= 0 && pageNum < pages ? html`
                      <li class="page-item ${pageNum === currentPage ? 'active' : ''}">
                        <button class="page-link" @click=${() => this._goToPage(pageNum)}>
                          ${pageNum + 1}
                        </button>
                      </li>` : '';
                  })}
                  <li class="page-item ${currentPage === pages - 1 ? 'disabled' : ''}">
                    <button class="page-link" @click=${() => this._goToPage(currentPage + 1)}
                            ?disabled=${currentPage === pages - 1}>Successiva</button>
                  </li>
                </ul>
              </nav>` : ''}
          `}
      </div>`;
  }

  _renderDetail() {
    if (!this._detail) return html`<div class="container py-4"><p>Caricamento...</p></div>`;

    const r = this._detail;
    const isActive = !r.data_cessazione || new Date(r.data_cessazione) > new Date();

    return html`
      <div class="container py-4" style="max-width:960px">
        <div class="d-flex align-items-center gap-3 mb-4">
          <button class="btn btn-sm btn-outline-secondary"
                  @click=${this._backToList}>← Torna alla lista</button>
          <h1 class="h4 mb-0">Dettaglio Rapporto</h1>
          <span class="badge ${isActive ? 'bg-success' : 'bg-secondary'} ms-auto">
            ${isActive ? 'Attivo' : 'Cessato'}
          </span>
        </div>

        ${this._error ? html`<div class="alert alert-danger">${this._error}</div>` : ''}

        <div class="card mb-3">
          <div class="card-header bg-light">
            <strong>Dati Anagrafici</strong>
          </div>
          <div class="card-body">
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label small text-muted">Matricola</label>
                <p class="mb-0"><strong>${r.matricola || '-'}</strong></p>
              </div>
              <div class="col-md-6">
                <label class="form-label small text-muted">Codice Rapporto</label>
                <p class="mb-0">${r.cod_rapporto || '-'}</p>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted">Cognome</label>
                <p class="mb-0"><strong>${r.cognome || '-'}</strong></p>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted">Nome</label>
                <p class="mb-0"><strong>${r.nome || '-'}</strong></p>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted">Sesso</label>
                <p class="mb-0">${r.sesso || '-'}</p>
              </div>
              <div class="col-md-6">
                <label class="form-label small text-muted">Codice Fiscale</label>
                <p class="mb-0 font-monospace">${r.cod_fis || '-'}</p>
              </div>
              <div class="col-md-6">
                <label class="form-label small text-muted">Email</label>
                <p class="mb-0">${r.email ? html`<a href="mailto:${r.email}">${r.email}</a>` : '-'}</p>
              </div>
              ${r.email_personale ? html`
                <div class="col-md-6">
                  <label class="form-label small text-muted">Email Personale</label>
                  <p class="mb-0"><a href="mailto:${r.email_personale}">${r.email_personale}</a></p>
                </div>
              ` : ''}
            </div>
          </div>
        </div>

        <div class="card mb-3">
          <div class="card-header bg-light">
            <strong>Dati Contrattuali</strong>
          </div>
          <div class="card-body">
            <div class="row g-3">
              <div class="col-md-4">
                <label class="form-label small text-muted">Codice Ente</label>
                <p class="mb-0"><span class="badge bg-info">${r.cod_ente || '-'}</span></p>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted">Data Assunzione</label>
                <p class="mb-0">${this._formatDate(r.data_assunzione)}</p>
              </div>
              <div class="col-md-4">
                <label class="form-label small text-muted">Data Cessazione</label>
                <p class="mb-0">${this._formatDate(r.data_cessazione) || 'In corso'}</p>
              </div>
              ${r.codice_azienda ? html`
                <div class="col-md-6">
                  <label class="form-label small text-muted">Azienda</label>
                  <p class="mb-0">${r.codice_azienda} - ${r.descrizione_azienda || ''}</p>
                </div>
              ` : ''}
              ${r.codice_mansione ? html`
                <div class="col-md-6">
                  <label class="form-label small text-muted">Mansione</label>
                  <p class="mb-0">${r.codice_mansione} - ${r.descrizione_mansione || ''}</p>
                </div>
              ` : ''}
            </div>
          </div>
        </div>

        <div class="card mb-3">
          <div class="card-header bg-light">
            <strong>Sedi e Ufficio</strong>
          </div>
          <div class="card-body">
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label small text-muted">Sede Primaria</label>
                <p class="mb-0">${r.cod_sede_primaria || '-'} ${r.descrizione_sede_primaria ? `- ${r.descrizione_sede_primaria}` : ''}</p>
              </div>
              ${r.cod_sede_secondaria ? html`
                <div class="col-md-6">
                  <label class="form-label small text-muted">Sede Secondaria</label>
                  <p class="mb-0">${r.cod_sede_secondaria} ${r.descrizione_sede_secondaria ? `- ${r.descrizione_sede_secondaria}` : ''}</p>
                </div>
              ` : ''}
              ${r.ufficio_piano || r.ufficio_numero_stanza || r.ufficio_telefono ? html`
                <div class="col-md-4">
                  <label class="form-label small text-muted">Piano</label>
                  <p class="mb-0">${r.ufficio_piano || '-'}</p>
                </div>
                <div class="col-md-4">
                  <label class="form-label small text-muted">Numero Stanza</label>
                  <p class="mb-0">${r.ufficio_numero_stanza || '-'}</p>
                </div>
                <div class="col-md-4">
                  <label class="form-label small text-muted">Telefono Ufficio</label>
                  <p class="mb-0">${r.ufficio_telefono || '-'}</p>
                </div>
              ` : ''}
              ${r.settore ? html`
                <div class="col-md-6">
                  <label class="form-label small text-muted">Settore</label>
                  <p class="mb-0">${r.settore}</p>
                </div>
              ` : ''}
              ${r.codice_struttura ? html`
                <div class="col-md-6">
                  <label class="form-label small text-muted">Struttura</label>
                  <p class="mb-0">${r.codice_struttura} ${r.descrizione_struttura ? `- ${r.descrizione_struttura}` : ''}</p>
                </div>
              ` : ''}
              <div class="col-md-6">
                <label class="form-label small text-muted">Servizio Fuori Sede</label>
                <p class="mb-0">${r.servizio_fuori_sede ? 'Sì' : 'No'}</p>
              </div>
            </div>
          </div>
        </div>

        ${r.p_badge ? html`
          <div class="card mb-3">
            <div class="card-header bg-light">
              <strong>Badge Virtuale</strong>
            </div>
            <div class="card-body">
              <p class="mb-0 font-monospace">${r.p_badge}</p>
            </div>
          </div>
        ` : ''}

        ${r.foto ? html`
          <div class="card mb-3">
            <div class="card-header bg-light">
              <strong>Fotografia</strong>
            </div>
            <div class="card-body text-center">
              <img src="${r.foto}" alt="Foto rapporto" class="img-thumbnail" style="max-width: 200px;">
            </div>
          </div>
        ` : html`
          <div class="text-center">
            <button class="btn btn-sm btn-outline-secondary"
                    @click=${() => this._downloadFoto(r.id)}>
              Scarica Foto
            </button>
          </div>
        `}
      </div>`;
  }

  _formatDate(dateString) {
    if (!dateString) return null;
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('it-IT', { year: 'numeric', month: '2-digit', day: '2-digit' });
    } catch {
      return dateString;
    }
  }
}

customElements.define('rapporti-layout', RapportiComponent);

export default RapportiComponent;
