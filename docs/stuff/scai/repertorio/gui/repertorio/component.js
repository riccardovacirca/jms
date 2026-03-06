import { LitElement, html } from 'lit';

export class RepertorioComponent extends LitElement {
  static properties = {
    _view: { state: true },
    _repertori: { state: true },
    _selectedRepertorio: { state: true },
    _loading: { state: true },
    _page: { state: true },
    _size: { state: true },
    _totalRecords: { state: true },
    _totalPages: { state: true },

    // Filtri
    _filterCodice: { state: true },
    _filterDescrizione: { state: true },
    _filterSlugSdc: { state: true },

    // Form fields
    _formCodiceRepertorio: { state: true },
    _formDescrizione: { state: true },
    _formSlugSdc: { state: true },
    _formLivello: { state: true },
    _formFlagParcheggio: { state: true },
    _formFlagStruttura: { state: true },

    // Lista SDC per select
    _sdcList: { state: true },

    _searchTimer: { state: true }
  };

  constructor() {
    super();
    this._view = 'list';
    this._repertori = [];
    this._selectedRepertorio = null;
    this._loading = false;
    this._page = 0;
    this._size = 25;
    this._totalRecords = 0;
    this._totalPages = 0;

    this._filterCodice = '';
    this._filterDescrizione = '';
    this._filterSlugSdc = '';

    this._formCodiceRepertorio = '';
    this._formDescrizione = '';
    this._formSlugSdc = '';
    this._formLivello = '';
    this._formFlagParcheggio = '';
    this._formFlagStruttura = '';

    this._sdcList = [];
    this._searchTimer = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadSdcList();
    this._loadList();
  }

  async _loadSdcList() {
    try {
      const res = await fetch('/api/scai/sdc?length=1000', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        this._sdcList = data.data || [];
      }
    } catch (error) {
      console.error('Errore caricamento SDC:', error);
    }
  }

  async _loadList() {
    this._loading = true;
    try {
      const params = new URLSearchParams({
        draw: '1',
        start: String(this._page),
        length: String(this._size),
        orderby: 'id',
        ordertype: 'desc'
      });

      if (this._filterCodice) {
        params.append('codice_repertorio', this._filterCodice);
      }
      if (this._filterDescrizione) {
        params.append('descrizione', this._filterDescrizione);
      }
      if (this._filterSlugSdc) {
        params.append('slug_sdc', this._filterSlugSdc);
      }

      const res = await fetch(`/api/scai/repertorio?${params}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._repertori = data.data || [];
      this._totalRecords = data.recordsTotal || 0;
      this._totalPages = Math.ceil(this._totalRecords / this._size);
    } catch (error) {
      console.error('Errore caricamento repertori:', error);
      alert('Errore durante il caricamento dei repertori');
    } finally {
      this._loading = false;
    }
  }

  async _loadDetail(id) {
    this._loading = true;
    try {
      const res = await fetch(`/api/scai/repertorio/${id}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._selectedRepertorio = data;
      this._view = 'detail';
    } catch (error) {
      console.error('Errore caricamento dettaglio repertorio:', error);
      alert('Errore durante il caricamento del dettaglio');
    } finally {
      this._loading = false;
    }
  }

  _onFilterChange() {
    clearTimeout(this._searchTimer);
    this._searchTimer = setTimeout(() => {
      this._page = 0;
      this._loadList();
    }, 400);
  }

  _onClearFilters() {
    this._filterCodice = '';
    this._filterDescrizione = '';
    this._filterSlugSdc = '';
    this._page = 0;
    this._loadList();
  }

  _onPageChange(newPage) {
    this._page = newPage;
    this._loadList();
  }

  _onCreateNew() {
    this._formCodiceRepertorio = '';
    this._formDescrizione = '';
    this._formSlugSdc = '';
    this._formLivello = '';
    this._formFlagParcheggio = '';
    this._formFlagStruttura = '';
    this._selectedRepertorio = null;
    this._view = 'form';
  }

  _onEdit() {
    if (!this._selectedRepertorio) return;

    this._formCodiceRepertorio = this._selectedRepertorio.codice_repertorio || '';
    this._formDescrizione = this._selectedRepertorio.descrizione || '';
    this._formSlugSdc = this._selectedRepertorio.slug_sdc || '';
    this._formLivello = this._selectedRepertorio.livello || '';
    this._formFlagParcheggio = this._selectedRepertorio.flag_parcheggio || '';
    this._formFlagStruttura = this._selectedRepertorio.flag_struttura || '';
    this._view = 'form';
  }

  async _onSave() {
    if (!this._formCodiceRepertorio || !this._formSlugSdc) {
      alert('Compilare tutti i campi obbligatori (codice repertorio e sistema di campo)');
      return;
    }

    this._loading = true;
    try {
      const payload = {
        codice_repertorio: this._formCodiceRepertorio,
        descrizione: this._formDescrizione,
        slug_sdc: this._formSlugSdc,
        livello: this._formLivello,
        flag_parcheggio: this._formFlagParcheggio,
        flag_struttura: this._formFlagStruttura
      };

      const isUpdate = this._selectedRepertorio && this._selectedRepertorio.id;
      const url = isUpdate
        ? `/api/scai/repertorio/${this._selectedRepertorio.id}`
        : `/api/scai/repertorio`;
      const method = isUpdate ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      alert(isUpdate ? 'Repertorio aggiornato con successo' : 'Repertorio creato con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore salvataggio repertorio:', error);
      alert('Errore durante il salvataggio');
    } finally {
      this._loading = false;
    }
  }

  async _onDelete() {
    if (!this._selectedRepertorio || !this._selectedRepertorio.id) return;

    if (!confirm(`Confermi l'eliminazione del repertorio "${this._selectedRepertorio.codice_repertorio}"?`)) {
      return;
    }

    this._loading = true;
    try {
      const res = await fetch(`/api/scai/repertorio/${this._selectedRepertorio.id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      alert('Repertorio eliminato con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore eliminazione repertorio:', error);
      alert('Errore durante l\'eliminazione');
    } finally {
      this._loading = false;
    }
  }

  _onCancel() {
    this._view = this._selectedRepertorio ? 'detail' : 'list';
  }

  _onBackToList() {
    this._view = 'list';
    this._selectedRepertorio = null;
  }

  render() {
    if (this._view === 'list') {
      return this._renderList();
    } else if (this._view === 'detail') {
      return this._renderDetail();
    } else if (this._view === 'form') {
      return this._renderForm();
    }
  }

  _renderList() {
    return html`
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Anagrafica Repertorio</h5>
          <button
            class="btn btn-primary btn-sm"
            @click=${this._onCreateNew}
            ?disabled=${this._loading}
          >
            <i class="bi bi-plus-circle"></i> Nuovo Repertorio
          </button>
        </div>

        <div class="card-body">
          <!-- Filtri -->
          <div class="row g-3 mb-3">
            <div class="col-md-3">
              <label class="form-label">Codice Repertorio</label>
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Filtra per codice..."
                .value=${this._filterCodice}
                @input=${(e) => {
                  this._filterCodice = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-4">
              <label class="form-label">Descrizione</label>
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Filtra per descrizione..."
                .value=${this._filterDescrizione}
                @input=${(e) => {
                  this._filterDescrizione = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-3">
              <label class="form-label">Sistema di Campo</label>
              <select
                class="form-select form-select-sm"
                .value=${this._filterSlugSdc}
                @change=${(e) => {
                  this._filterSlugSdc = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutti</option>
                ${this._sdcList.map(sdc => html`
                  <option value="${sdc.slug}">${sdc.descrizione_breve}</option>
                `)}
              </select>
            </div>
            <div class="col-md-2 d-flex align-items-end">
              <button
                class="btn btn-secondary btn-sm w-100"
                @click=${this._onClearFilters}
              >
                Pulisci Filtri
              </button>
            </div>
          </div>

          <!-- Tabella -->
          ${this._loading ? html`
            <div class="text-center py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Caricamento...</span>
              </div>
            </div>
          ` : html`
            <div class="table-responsive">
              <table class="table table-hover table-sm">
                <thead class="table-light">
                  <tr>
                    <th>ID</th>
                    <th>Codice</th>
                    <th>Descrizione</th>
                    <th>Sistema Campo</th>
                    <th>Livello</th>
                    <th class="text-end">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  ${this._repertori.length === 0 ? html`
                    <tr>
                      <td colspan="6" class="text-center text-muted py-4">
                        Nessun repertorio trovato
                      </td>
                    </tr>
                  ` : this._repertori.map(rep => html`
                    <tr>
                      <td>${rep.id}</td>
                      <td><strong>${rep.codice_repertorio}</strong></td>
                      <td>${rep.descrizione || '-'}</td>
                      <td><span class="badge bg-info">${rep.slug_sdc}</span></td>
                      <td>${rep.livello || '-'}</td>
                      <td class="text-end">
                        <button
                          class="btn btn-sm btn-outline-primary"
                          @click=${() => this._loadDetail(rep.id)}
                        >
                          <i class="bi bi-eye"></i> Dettagli
                        </button>
                      </td>
                    </tr>
                  `)}
                </tbody>
              </table>
            </div>

            <!-- Paginazione -->
            ${this._totalPages > 1 ? html`
              <nav class="mt-3">
                <ul class="pagination pagination-sm justify-content-center mb-0">
                  <li class="page-item ${this._page === 0 ? 'disabled' : ''}">
                    <button
                      class="page-link"
                      @click=${() => this._onPageChange(this._page - 1)}
                      ?disabled=${this._page === 0}
                    >
                      Precedente
                    </button>
                  </li>
                  <li class="page-item disabled">
                    <span class="page-link">
                      Pagina ${Math.floor(this._page / this._size) + 1} di ${this._totalPages}
                    </span>
                  </li>
                  <li class="page-item ${this._page + this._size >= this._totalRecords ? 'disabled' : ''}">
                    <button
                      class="page-link"
                      @click=${() => this._onPageChange(this._page + this._size)}
                      ?disabled=${this._page + this._size >= this._totalRecords}
                    >
                      Successiva
                    </button>
                  </li>
                </ul>
              </nav>
            ` : ''}

            <div class="text-muted text-center mt-2 small">
              Totale: ${this._totalRecords} repertori
            </div>
          `}
        </div>
      </div>
    `;
  }

  _renderDetail() {
    if (!this._selectedRepertorio) return html``;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onBackToList}>
          <i class="bi bi-arrow-left"></i> Torna alla Lista
        </button>
      </div>

      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Dettaglio Repertorio</h5>
          <div>
            <button
              class="btn btn-warning btn-sm me-2"
              @click=${this._onEdit}
            >
              <i class="bi bi-pencil"></i> Modifica
            </button>
            <button
              class="btn btn-danger btn-sm"
              @click=${this._onDelete}
            >
              <i class="bi bi-trash"></i> Elimina
            </button>
          </div>
        </div>

        <div class="card-body">
          <div class="row">
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold">ID</label>
                <div>${this._selectedRepertorio.id}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Codice Repertorio</label>
                <div><strong>${this._selectedRepertorio.codice_repertorio}</strong></div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Descrizione</label>
                <div>${this._selectedRepertorio.descrizione || '-'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Sistema di Campo</label>
                <div><span class="badge bg-info">${this._selectedRepertorio.slug_sdc}</span></div>
              </div>
            </div>

            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold">Livello</label>
                <div>${this._selectedRepertorio.livello || '-'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Flag Parcheggio</label>
                <div>${this._selectedRepertorio.flag_parcheggio || '-'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Flag Struttura</label>
                <div>${this._selectedRepertorio.flag_struttura || '-'}</div>
              </div>
            </div>
          </div>

          <hr />
          <div class="row">
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold text-muted">Data Creazione</label>
                <div class="small">${this._selectedRepertorio.created_at || 'N/A'}</div>
              </div>
            </div>
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold text-muted">Ultimo Aggiornamento</label>
                <div class="small">${this._selectedRepertorio.updated_at || 'N/A'}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderForm() {
    const isUpdate = this._selectedRepertorio && this._selectedRepertorio.id;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onCancel}>
          <i class="bi bi-arrow-left"></i> Annulla
        </button>
      </div>

      <div class="card">
        <div class="card-header">
          <h5 class="mb-0">${isUpdate ? 'Modifica Repertorio' : 'Nuovo Repertorio'}</h5>
        </div>

        <div class="card-body">
          <form @submit=${(e) => { e.preventDefault(); this._onSave(); }}>
            <div class="row">
              <div class="col-md-6">
                <div class="mb-3">
                  <label class="form-label">
                    Codice Repertorio <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="6"
                    required
                    .value=${this._formCodiceRepertorio}
                    @input=${(e) => this._formCodiceRepertorio = e.target.value}
                    ?disabled=${isUpdate}
                  />
                  <small class="form-text text-muted">Massimo 6 caratteri</small>
                </div>

                <div class="mb-3">
                  <label class="form-label">Descrizione</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="255"
                    .value=${this._formDescrizione}
                    @input=${(e) => this._formDescrizione = e.target.value}
                  />
                  <small class="form-text text-muted">Massimo 255 caratteri</small>
                </div>

                <div class="mb-3">
                  <label class="form-label">
                    Sistema di Campo <span class="text-danger">*</span>
                  </label>
                  <select
                    class="form-select"
                    required
                    .value=${this._formSlugSdc}
                    @change=${(e) => this._formSlugSdc = e.target.value}
                  >
                    <option value="">-- Seleziona Sistema --</option>
                    ${this._sdcList.map(sdc => html`
                      <option value="${sdc.slug}">${sdc.descrizione_breve}</option>
                    `)}
                  </select>
                </div>
              </div>

              <div class="col-md-6">
                <div class="mb-3">
                  <label class="form-label">Livello</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="8"
                    .value=${this._formLivello}
                    @input=${(e) => this._formLivello = e.target.value}
                  />
                  <small class="form-text text-muted">Tipologia/livello (max 8 caratteri)</small>
                </div>

                <div class="mb-3">
                  <label class="form-label">Flag Parcheggio</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="4"
                    .value=${this._formFlagParcheggio}
                    @input=${(e) => this._formFlagParcheggio = e.target.value}
                  />
                  <small class="form-text text-muted">Massimo 4 caratteri</small>
                </div>

                <div class="mb-3">
                  <label class="form-label">Flag Struttura</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="4"
                    .value=${this._formFlagStruttura}
                    @input=${(e) => this._formFlagStruttura = e.target.value}
                  />
                  <small class="form-text text-muted">Massimo 4 caratteri</small>
                </div>
              </div>
            </div>

            <div class="mt-4 d-flex justify-content-end gap-2">
              <button
                type="button"
                class="btn btn-secondary"
                @click=${this._onCancel}
                ?disabled=${this._loading}
              >
                Annulla
              </button>
              <button
                type="submit"
                class="btn btn-primary"
                ?disabled=${this._loading}
              >
                ${this._loading ? html`
                  <span class="spinner-border spinner-border-sm me-2"></span>
                ` : ''}
                ${isUpdate ? 'Aggiorna' : 'Crea'}
              </button>
            </div>
          </form>
        </div>
      </div>
    `;
  }
}

customElements.define('repertorio-component', RepertorioComponent);
