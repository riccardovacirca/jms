import { LitElement, html } from 'lit';

export class EnteComponent extends LitElement {
  static properties = {
    _view: { state: true },
    _enti: { state: true },
    _selectedEnte: { state: true },
    _loading: { state: true },
    _page: { state: true },
    _size: { state: true },
    _totalRecords: { state: true },
    _totalPages: { state: true },

    // Filtri
    _filterCodEnte: { state: true },
    _filterDescrizione: { state: true },

    // Form fields
    _formCodEnte: { state: true },
    _formDescrizione: { state: true },
    _formFlagAreas: { state: true },
    _formIdAziendaAreas: { state: true },

    _searchTimer: { state: true }
  };

  constructor() {
    super();
    this._view = 'list';
    this._enti = [];
    this._selectedEnte = null;
    this._loading = false;
    this._page = 0;
    this._size = 25;
    this._totalRecords = 0;
    this._totalPages = 0;

    this._filterCodEnte = '';
    this._filterDescrizione = '';

    this._formCodEnte = '';
    this._formDescrizione = '';
    this._formFlagAreas = 0;
    this._formIdAziendaAreas = null;

    this._searchTimer = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadList();
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

      if (this._filterCodEnte) {
        params.append('cod_ente', this._filterCodEnte);
      }
      if (this._filterDescrizione) {
        params.append('descrizione_ente', this._filterDescrizione);
      }

      const res = await fetch(`/api/scai/ente?${params}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._enti = data.data || [];
      this._totalRecords = data.recordsTotal || 0;
      this._totalPages = Math.ceil(this._totalRecords / this._size);
    } catch (error) {
      console.error('Errore caricamento enti:', error);
      alert('Errore durante il caricamento degli enti');
    } finally {
      this._loading = false;
    }
  }

  async _loadDetail(id) {
    this._loading = true;
    try {
      const res = await fetch(`/api/scai/ente/${id}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._selectedEnte = data;
      this._view = 'detail';
    } catch (error) {
      console.error('Errore caricamento dettaglio ente:', error);
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
    this._filterCodEnte = '';
    this._filterDescrizione = '';
    this._page = 0;
    this._loadList();
  }

  _onPageChange(newPage) {
    this._page = newPage;
    this._loadList();
  }

  _onCreateNew() {
    this._formCodEnte = '';
    this._formDescrizione = '';
    this._formFlagAreas = 0;
    this._formIdAziendaAreas = null;
    this._selectedEnte = null;
    this._view = 'form';
  }

  _onEdit() {
    if (!this._selectedEnte) return;

    this._formCodEnte = this._selectedEnte.cod_ente || '';
    this._formDescrizione = this._selectedEnte.descrizione_ente || '';
    this._formFlagAreas = this._selectedEnte.flag_areas || 0;
    this._formIdAziendaAreas = this._selectedEnte.id_azienda_areas || null;
    this._view = 'form';
  }

  async _onSave() {
    if (!this._formCodEnte || !this._formDescrizione) {
      alert('Compilare tutti i campi obbligatori');
      return;
    }

    this._loading = true;
    try {
      const payload = {
        cod_ente: this._formCodEnte,
        descrizione_ente: this._formDescrizione,
        flag_areas: this._formFlagAreas,
        id_azienda_areas: this._formIdAziendaAreas
      };

      const isUpdate = this._selectedEnte && this._selectedEnte.id;
      const url = isUpdate
        ? `/api/scai/ente/${this._selectedEnte.id}`
        : `/api/scai/ente`;
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

      alert(isUpdate ? 'Ente aggiornato con successo' : 'Ente creato con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore salvataggio ente:', error);
      alert('Errore durante il salvataggio');
    } finally {
      this._loading = false;
    }
  }

  async _onDelete() {
    if (!this._selectedEnte || !this._selectedEnte.id) return;

    if (!confirm(`Confermi l'eliminazione dell'ente "${this._selectedEnte.descrizione_ente}"?`)) {
      return;
    }

    this._loading = true;
    try {
      const res = await fetch(`/api/scai/ente/${this._selectedEnte.id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      alert('Ente eliminato con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore eliminazione ente:', error);
      alert('Errore durante l\'eliminazione');
    } finally {
      this._loading = false;
    }
  }

  _onCancel() {
    this._view = this._selectedEnte ? 'detail' : 'list';
  }

  _onBackToList() {
    this._view = 'list';
    this._selectedEnte = null;
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
          <h5 class="mb-0">Anagrafica Enti</h5>
          <button
            class="btn btn-primary btn-sm"
            @click=${this._onCreateNew}
            ?disabled=${this._loading}
          >
            <i class="bi bi-plus-circle"></i> Nuovo Ente
          </button>
        </div>

        <div class="card-body">
          <!-- Filtri -->
          <div class="row g-3 mb-3">
            <div class="col-md-4">
              <label class="form-label">Codice Ente</label>
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Filtra per codice..."
                .value=${this._filterCodEnte}
                @input=${(e) => {
                  this._filterCodEnte = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-6">
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
                    <th>Codice Ente</th>
                    <th>Descrizione</th>
                    <th>Flag AREAS</th>
                    <th class="text-end">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  ${this._enti.length === 0 ? html`
                    <tr>
                      <td colspan="5" class="text-center text-muted py-4">
                        Nessun ente trovato
                      </td>
                    </tr>
                  ` : this._enti.map(ente => html`
                    <tr>
                      <td>${ente.id}</td>
                      <td><strong>${ente.cod_ente}</strong></td>
                      <td>${ente.descrizione_ente}</td>
                      <td>
                        ${ente.flag_areas === 1 ? html`
                          <span class="badge bg-success">Abilitato</span>
                        ` : html`
                          <span class="badge bg-secondary">Disabilitato</span>
                        `}
                      </td>
                      <td class="text-end">
                        <button
                          class="btn btn-sm btn-outline-primary"
                          @click=${() => this._loadDetail(ente.id)}
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
              Totale: ${this._totalRecords} enti
            </div>
          `}
        </div>
      </div>
    `;
  }

  _renderDetail() {
    if (!this._selectedEnte) return html``;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onBackToList}>
          <i class="bi bi-arrow-left"></i> Torna alla Lista
        </button>
      </div>

      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Dettaglio Ente</h5>
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
                <div>${this._selectedEnte.id}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Codice Ente</label>
                <div>${this._selectedEnte.cod_ente}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Descrizione Ente</label>
                <div>${this._selectedEnte.descrizione_ente}</div>
              </div>
            </div>

            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold">Flag AREAS</label>
                <div>
                  ${this._selectedEnte.flag_areas === 1 ? html`
                    <span class="badge bg-success">Abilitato</span>
                  ` : html`
                    <span class="badge bg-secondary">Disabilitato</span>
                  `}
                </div>
              </div>
              ${this._selectedEnte.id_azienda_areas ? html`
                <div class="mb-3">
                  <label class="form-label fw-bold">ID Azienda AREAS</label>
                  <div>${this._selectedEnte.id_azienda_areas}</div>
                </div>
              ` : ''}
              <div class="mb-3">
                <label class="form-label fw-bold">Data Creazione</label>
                <div>${this._selectedEnte.created_at || 'N/A'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Ultimo Aggiornamento</label>
                <div>${this._selectedEnte.updated_at || 'N/A'}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderForm() {
    const isUpdate = this._selectedEnte && this._selectedEnte.id;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onCancel}>
          <i class="bi bi-arrow-left"></i> Annulla
        </button>
      </div>

      <div class="card">
        <div class="card-header">
          <h5 class="mb-0">${isUpdate ? 'Modifica Ente' : 'Nuovo Ente'}</h5>
        </div>

        <div class="card-body">
          <form @submit=${(e) => { e.preventDefault(); this._onSave(); }}>
            <div class="row">
              <div class="col-md-6">
                <div class="mb-3">
                  <label class="form-label">
                    Codice Ente <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="15"
                    required
                    .value=${this._formCodEnte}
                    @input=${(e) => this._formCodEnte = e.target.value}
                    ?disabled=${isUpdate}
                  />
                  <small class="form-text text-muted">Massimo 15 caratteri</small>
                </div>

                <div class="mb-3">
                  <label class="form-label">
                    Descrizione Ente <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="255"
                    required
                    .value=${this._formDescrizione}
                    @input=${(e) => this._formDescrizione = e.target.value}
                  />
                  <small class="form-text text-muted">Massimo 255 caratteri</small>
                </div>
              </div>

              <div class="col-md-6">
                <div class="mb-3">
                  <label class="form-label">Flag AREAS</label>
                  <select
                    class="form-select"
                    .value=${String(this._formFlagAreas)}
                    @change=${(e) => this._formFlagAreas = parseInt(e.target.value)}
                  >
                    <option value="0">Disabilitato</option>
                    <option value="1">Abilitato</option>
                  </select>
                  <small class="form-text text-muted">
                    Integrazione con sistema AREAS
                  </small>
                </div>

                <div class="mb-3">
                  <label class="form-label">ID Azienda AREAS</label>
                  <input
                    type="number"
                    class="form-control"
                    .value=${this._formIdAziendaAreas || ''}
                    @input=${(e) => this._formIdAziendaAreas = e.target.value ? parseInt(e.target.value) : null}
                  />
                  <small class="form-text text-muted">
                    Opzionale - ID azienda nel sistema AREAS
                  </small>
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

customElements.define('ente-component', EnteComponent);
