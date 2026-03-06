import { LitElement, html } from 'lit';

export class SdcComponent extends LitElement {
  static properties = {
    _view: { state: true },
    _sistemi: { state: true },
    _selectedSdc: { state: true },
    _loading: { state: true },
    _page: { state: true },
    _size: { state: true },
    _totalRecords: { state: true },
    _totalPages: { state: true },

    // Filtri
    _filterSlug: { state: true },
    _filterCode: { state: true },
    _filterDescrizione: { state: true },

    // Form fields
    _formCode: { state: true },
    _formSlug: { state: true },
    _formDescrizioneBreve: { state: true },
    _formDescrizioneLunga: { state: true },

    _searchTimer: { state: true }
  };

  constructor() {
    super();
    this._view = 'list';
    this._sistemi = [];
    this._selectedSdc = null;
    this._loading = false;
    this._page = 0;
    this._size = 25;
    this._totalRecords = 0;
    this._totalPages = 0;

    this._filterSlug = '';
    this._filterCode = '';
    this._filterDescrizione = '';

    this._formCode = '';
    this._formSlug = '';
    this._formDescrizioneBreve = '';
    this._formDescrizioneLunga = '';

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

      if (this._filterSlug) {
        params.append('slug', this._filterSlug);
      }
      if (this._filterCode) {
        params.append('code', this._filterCode);
      }
      if (this._filterDescrizione) {
        params.append('descrizione_breve', this._filterDescrizione);
      }

      const res = await fetch(`/api/scai/sdc?${params}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._sistemi = data.data || [];
      this._totalRecords = data.recordsTotal || 0;
      this._totalPages = Math.ceil(this._totalRecords / this._size);
    } catch (error) {
      console.error('Errore caricamento sistemi di campo:', error);
      alert('Errore durante il caricamento dei sistemi di campo');
    } finally {
      this._loading = false;
    }
  }

  async _loadDetail(id) {
    this._loading = true;
    try {
      const res = await fetch(`/api/scai/sdc/${id}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._selectedSdc = data;
      this._view = 'detail';
    } catch (error) {
      console.error('Errore caricamento dettaglio sistema di campo:', error);
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
    this._filterSlug = '';
    this._filterCode = '';
    this._filterDescrizione = '';
    this._page = 0;
    this._loadList();
  }

  _onPageChange(newPage) {
    this._page = newPage;
    this._loadList();
  }

  _onCreateNew() {
    this._formCode = '';
    this._formSlug = '';
    this._formDescrizioneBreve = '';
    this._formDescrizioneLunga = '';
    this._selectedSdc = null;
    this._view = 'form';
  }

  _onEdit() {
    if (!this._selectedSdc) return;

    this._formCode = this._selectedSdc.code || '';
    this._formSlug = this._selectedSdc.slug || '';
    this._formDescrizioneBreve = this._selectedSdc.descrizione_breve || '';
    this._formDescrizioneLunga = this._selectedSdc.descrizione_lunga || '';
    this._view = 'form';
  }

  async _onSave() {
    if (!this._formCode || !this._formSlug || !this._formDescrizioneBreve || !this._formDescrizioneLunga) {
      alert('Compilare tutti i campi obbligatori');
      return;
    }

    this._loading = true;
    try {
      const payload = {
        code: this._formCode,
        slug: this._formSlug,
        descrizione_breve: this._formDescrizioneBreve,
        descrizione_lunga: this._formDescrizioneLunga
      };

      const isUpdate = this._selectedSdc && this._selectedSdc.id;
      const url = isUpdate
        ? `/api/scai/sdc/${this._selectedSdc.id}`
        : `/api/scai/sdc`;
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

      alert(isUpdate ? 'Sistema di campo aggiornato con successo' : 'Sistema di campo creato con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore salvataggio sistema di campo:', error);
      alert('Errore durante il salvataggio');
    } finally {
      this._loading = false;
    }
  }

  async _onDelete() {
    if (!this._selectedSdc || !this._selectedSdc.id) return;

    if (!confirm(`Confermi l'eliminazione del sistema di campo "${this._selectedSdc.descrizione_breve}"?`)) {
      return;
    }

    this._loading = true;
    try {
      const res = await fetch(`/api/scai/sdc/${this._selectedSdc.id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      alert('Sistema di campo eliminato con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore eliminazione sistema di campo:', error);
      alert('Errore durante l\'eliminazione');
    } finally {
      this._loading = false;
    }
  }

  _onCancel() {
    this._view = this._selectedSdc ? 'detail' : 'list';
  }

  _onBackToList() {
    this._view = 'list';
    this._selectedSdc = null;
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
          <h5 class="mb-0">Anagrafica Sistemi di Campo</h5>
          <button
            class="btn btn-primary btn-sm"
            @click=${this._onCreateNew}
            ?disabled=${this._loading}
          >
            <i class="bi bi-plus-circle"></i> Nuovo Sistema
          </button>
        </div>

        <div class="card-body">
          <!-- Filtri -->
          <div class="row g-3 mb-3">
            <div class="col-md-3">
              <label class="form-label">Slug</label>
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Filtra per slug..."
                .value=${this._filterSlug}
                @input=${(e) => {
                  this._filterSlug = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-2">
              <label class="form-label">Codice</label>
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Filtra per codice..."
                .value=${this._filterCode}
                @input=${(e) => {
                  this._filterCode = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-5">
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
                    <th>Codice</th>
                    <th>Slug</th>
                    <th>Descrizione Breve</th>
                    <th class="text-end">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  ${this._sistemi.length === 0 ? html`
                    <tr>
                      <td colspan="5" class="text-center text-muted py-4">
                        Nessun sistema di campo trovato
                      </td>
                    </tr>
                  ` : this._sistemi.map(sdc => html`
                    <tr>
                      <td>${sdc.id}</td>
                      <td><span class="badge bg-secondary">${sdc.code}</span></td>
                      <td><strong>${sdc.slug}</strong></td>
                      <td>${sdc.descrizione_breve}</td>
                      <td class="text-end">
                        <button
                          class="btn btn-sm btn-outline-primary"
                          @click=${() => this._loadDetail(sdc.id)}
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
              Totale: ${this._totalRecords} sistemi di campo
            </div>
          `}
        </div>
      </div>
    `;
  }

  _renderDetail() {
    if (!this._selectedSdc) return html``;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onBackToList}>
          <i class="bi bi-arrow-left"></i> Torna alla Lista
        </button>
      </div>

      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Dettaglio Sistema di Campo</h5>
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
                <div>${this._selectedSdc.id}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Codice</label>
                <div><span class="badge bg-secondary">${this._selectedSdc.code}</span></div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Slug</label>
                <div><code>${this._selectedSdc.slug}</code></div>
              </div>
            </div>

            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold">Descrizione Breve</label>
                <div>${this._selectedSdc.descrizione_breve}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Descrizione Completa</label>
                <div>${this._selectedSdc.descrizione_lunga}</div>
              </div>
            </div>
          </div>

          <hr />
          <div class="row">
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold text-muted">Data Creazione</label>
                <div class="small">${this._selectedSdc.created_at || 'N/A'}</div>
              </div>
            </div>
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold text-muted">Ultimo Aggiornamento</label>
                <div class="small">${this._selectedSdc.updated_at || 'N/A'}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderForm() {
    const isUpdate = this._selectedSdc && this._selectedSdc.id;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onCancel}>
          <i class="bi bi-arrow-left"></i> Annulla
        </button>
      </div>

      <div class="card">
        <div class="card-header">
          <h5 class="mb-0">${isUpdate ? 'Modifica Sistema di Campo' : 'Nuovo Sistema di Campo'}</h5>
        </div>

        <div class="card-body">
          <form @submit=${(e) => { e.preventDefault(); this._onSave(); }}>
            <div class="row">
              <div class="col-md-6">
                <div class="mb-3">
                  <label class="form-label">
                    Codice <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="8"
                    required
                    .value=${this._formCode}
                    @input=${(e) => this._formCode = e.target.value}
                  />
                  <small class="form-text text-muted">Massimo 8 caratteri - Codice legacy SCAI</small>
                </div>

                <div class="mb-3">
                  <label class="form-label">
                    Slug <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="64"
                    required
                    pattern="[a-z0-9-]+"
                    .value=${this._formSlug}
                    @input=${(e) => this._formSlug = e.target.value}
                    ?disabled=${isUpdate}
                  />
                  <small class="form-text text-muted">
                    Massimo 64 caratteri - Solo lettere minuscole, numeri e trattini
                  </small>
                </div>
              </div>

              <div class="col-md-6">
                <div class="mb-3">
                  <label class="form-label">
                    Descrizione Breve <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="255"
                    required
                    .value=${this._formDescrizioneBreve}
                    @input=${(e) => this._formDescrizioneBreve = e.target.value}
                  />
                  <small class="form-text text-muted">Massimo 255 caratteri</small>
                </div>

                <div class="mb-3">
                  <label class="form-label">
                    Descrizione Completa <span class="text-danger">*</span>
                  </label>
                  <textarea
                    class="form-control"
                    rows="3"
                    maxlength="512"
                    required
                    .value=${this._formDescrizioneLunga}
                    @input=${(e) => this._formDescrizioneLunga = e.target.value}
                  ></textarea>
                  <small class="form-text text-muted">Massimo 512 caratteri</small>
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

customElements.define('sdc-component', SdcComponent);
