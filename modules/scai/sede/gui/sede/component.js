import { LitElement, html } from 'lit';

export class SedeComponent extends LitElement {
  static properties = {
    _view: { state: true },
    _sedi: { state: true },
    _selectedSede: { state: true },
    _loading: { state: true },
    _page: { state: true },
    _size: { state: true },
    _totalRecords: { state: true },
    _totalPages: { state: true },

    // Filtri
    _filterCodSede: { state: true },
    _filterNome: { state: true },
    _filterDescrizione: { state: true },

    // Form fields
    _formCodSede: { state: true },
    _formNome: { state: true },
    _formIndirizzo: { state: true },
    _formCap: { state: true },
    _formCityCode: { state: true },
    _formProvinceCode: { state: true },
    _formIstatRegion: { state: true },
    _formNazione: { state: true },
    _formDescrizioneBreve: { state: true },
    _formDescrizioneLunga: { state: true },

    _searchTimer: { state: true }
  };

  constructor() {
    super();
    this._view = 'list';
    this._sedi = [];
    this._selectedSede = null;
    this._loading = false;
    this._page = 0;
    this._size = 25;
    this._totalRecords = 0;
    this._totalPages = 0;

    this._filterCodSede = '';
    this._filterNome = '';
    this._filterDescrizione = '';

    this._formCodSede = '';
    this._formNome = '';
    this._formIndirizzo = '';
    this._formCap = '';
    this._formCityCode = '';
    this._formProvinceCode = '';
    this._formIstatRegion = '';
    this._formNazione = 'IT';
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

      if (this._filterCodSede) {
        params.append('cod_sede', this._filterCodSede);
      }
      if (this._filterNome) {
        params.append('nome', this._filterNome);
      }
      if (this._filterDescrizione) {
        params.append('descrizione_breve', this._filterDescrizione);
      }

      const res = await fetch(`/api/scai/sede?${params}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._sedi = data.data || [];
      this._totalRecords = data.recordsTotal || 0;
      this._totalPages = Math.ceil(this._totalRecords / this._size);
    } catch (error) {
      console.error('Errore caricamento sedi:', error);
      alert('Errore durante il caricamento delle sedi');
    } finally {
      this._loading = false;
    }
  }

  async _loadDetail(id) {
    this._loading = true;
    try {
      const res = await fetch(`/api/scai/sede/${id}`, {
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      const data = await res.json();
      this._selectedSede = data;
      this._view = 'detail';
    } catch (error) {
      console.error('Errore caricamento dettaglio sede:', error);
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
    this._filterCodSede = '';
    this._filterNome = '';
    this._filterDescrizione = '';
    this._page = 0;
    this._loadList();
  }

  _onPageChange(newPage) {
    this._page = newPage;
    this._loadList();
  }

  _onCreateNew() {
    this._formCodSede = '';
    this._formNome = '';
    this._formIndirizzo = '';
    this._formCap = '';
    this._formCityCode = '';
    this._formProvinceCode = '';
    this._formIstatRegion = '';
    this._formNazione = 'IT';
    this._formDescrizioneBreve = '';
    this._formDescrizioneLunga = '';
    this._selectedSede = null;
    this._view = 'form';
  }

  _onEdit() {
    if (!this._selectedSede) return;

    this._formCodSede = this._selectedSede.cod_sede || '';
    this._formNome = this._selectedSede.nome || '';
    this._formIndirizzo = this._selectedSede.indirizzo || '';
    this._formCap = this._selectedSede.cap || '';
    this._formCityCode = this._selectedSede.city_code || '';
    this._formProvinceCode = this._selectedSede.province_code || '';
    this._formIstatRegion = this._selectedSede.istat_region || '';
    this._formNazione = this._selectedSede.nazione || 'IT';
    this._formDescrizioneBreve = this._selectedSede.descrizione_breve || '';
    this._formDescrizioneLunga = this._selectedSede.descrizione_lunga || '';
    this._view = 'form';
  }

  async _onSave() {
    if (!this._formCodSede || !this._formNome || !this._formIndirizzo ||
        !this._formDescrizioneBreve || !this._formDescrizioneLunga) {
      alert('Compilare tutti i campi obbligatori');
      return;
    }

    this._loading = true;
    try {
      const payload = {
        cod_sede: this._formCodSede,
        nome: this._formNome,
        indirizzo: this._formIndirizzo,
        cap: this._formCap,
        city_code: this._formCityCode,
        province_code: this._formProvinceCode,
        istat_region: this._formIstatRegion,
        nazione: this._formNazione,
        descrizione_breve: this._formDescrizioneBreve,
        descrizione_lunga: this._formDescrizioneLunga
      };

      const isUpdate = this._selectedSede && this._selectedSede.id;
      const url = isUpdate
        ? `/api/scai/sede/${this._selectedSede.id}`
        : `/api/scai/sede`;
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

      alert(isUpdate ? 'Sede aggiornata con successo' : 'Sede creata con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore salvataggio sede:', error);
      alert('Errore durante il salvataggio');
    } finally {
      this._loading = false;
    }
  }

  async _onDelete() {
    if (!this._selectedSede || !this._selectedSede.id) return;

    if (!confirm(`Confermi l'eliminazione della sede "${this._selectedSede.nome}"?`)) {
      return;
    }

    this._loading = true;
    try {
      const res = await fetch(`/api/scai/sede/${this._selectedSede.id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (!res.ok) {
        throw new Error(`HTTP error! status: ${res.status}`);
      }

      alert('Sede eliminata con successo');
      this._view = 'list';
      this._loadList();
    } catch (error) {
      console.error('Errore eliminazione sede:', error);
      alert('Errore durante l\'eliminazione');
    } finally {
      this._loading = false;
    }
  }

  _onCancel() {
    this._view = this._selectedSede ? 'detail' : 'list';
  }

  _onBackToList() {
    this._view = 'list';
    this._selectedSede = null;
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
          <h5 class="mb-0">Anagrafica Sedi</h5>
          <button
            class="btn btn-primary btn-sm"
            @click=${this._onCreateNew}
            ?disabled=${this._loading}
          >
            <i class="bi bi-plus-circle"></i> Nuova Sede
          </button>
        </div>

        <div class="card-body">
          <!-- Filtri -->
          <div class="row g-3 mb-3">
            <div class="col-md-3">
              <label class="form-label">Codice Sede</label>
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Filtra per codice..."
                .value=${this._filterCodSede}
                @input=${(e) => {
                  this._filterCodSede = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-3">
              <label class="form-label">Nome</label>
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Filtra per nome..."
                .value=${this._filterNome}
                @input=${(e) => {
                  this._filterNome = e.target.value;
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
                    <th>Nome</th>
                    <th>Indirizzo</th>
                    <th>Provincia</th>
                    <th class="text-end">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  ${this._sedi.length === 0 ? html`
                    <tr>
                      <td colspan="6" class="text-center text-muted py-4">
                        Nessuna sede trovata
                      </td>
                    </tr>
                  ` : this._sedi.map(sede => html`
                    <tr>
                      <td>${sede.id}</td>
                      <td><strong>${sede.cod_sede}</strong></td>
                      <td>${sede.nome}</td>
                      <td>
                        <small class="text-muted">
                          ${sede.indirizzo || 'N/A'}
                          ${sede.cap ? `, ${sede.cap}` : ''}
                        </small>
                      </td>
                      <td>${sede.province_code || 'N/A'}</td>
                      <td class="text-end">
                        <button
                          class="btn btn-sm btn-outline-primary"
                          @click=${() => this._loadDetail(sede.id)}
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
              Totale: ${this._totalRecords} sedi
            </div>
          `}
        </div>
      </div>
    `;
  }

  _renderDetail() {
    if (!this._selectedSede) return html``;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onBackToList}>
          <i class="bi bi-arrow-left"></i> Torna alla Lista
        </button>
      </div>

      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Dettaglio Sede</h5>
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
            <!-- Colonna sinistra: Dati identificativi -->
            <div class="col-md-6">
              <h6 class="text-primary mb-3">Dati Identificativi</h6>

              <div class="mb-3">
                <label class="form-label fw-bold">ID</label>
                <div>${this._selectedSede.id}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Codice Sede</label>
                <div>${this._selectedSede.cod_sede}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Nome</label>
                <div>${this._selectedSede.nome}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Descrizione Breve</label>
                <div>${this._selectedSede.descrizione_breve}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Descrizione Completa</label>
                <div>${this._selectedSede.descrizione_lunga}</div>
              </div>
            </div>

            <!-- Colonna destra: Dati geografici -->
            <div class="col-md-6">
              <h6 class="text-primary mb-3">Dati Geografici</h6>

              <div class="mb-3">
                <label class="form-label fw-bold">Indirizzo</label>
                <div>${this._selectedSede.indirizzo}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">CAP</label>
                <div>${this._selectedSede.cap || 'N/A'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Codice Comune ISTAT</label>
                <div>${this._selectedSede.city_code || 'N/A'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Provincia</label>
                <div>${this._selectedSede.province_code || 'N/A'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Regione ISTAT</label>
                <div>${this._selectedSede.istat_region || 'N/A'}</div>
              </div>
              <div class="mb-3">
                <label class="form-label fw-bold">Nazione</label>
                <div>${this._selectedSede.nazione || 'N/A'}</div>
              </div>
            </div>
          </div>

          <!-- Audit -->
          <hr />
          <div class="row">
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold text-muted">Data Creazione</label>
                <div class="small">${this._selectedSede.created_at || 'N/A'}</div>
              </div>
            </div>
            <div class="col-md-6">
              <div class="mb-3">
                <label class="form-label fw-bold text-muted">Ultimo Aggiornamento</label>
                <div class="small">${this._selectedSede.updated_at || 'N/A'}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  _renderForm() {
    const isUpdate = this._selectedSede && this._selectedSede.id;

    return html`
      <div class="mb-3">
        <button class="btn btn-sm btn-outline-secondary" @click=${this._onCancel}>
          <i class="bi bi-arrow-left"></i> Annulla
        </button>
      </div>

      <div class="card">
        <div class="card-header">
          <h5 class="mb-0">${isUpdate ? 'Modifica Sede' : 'Nuova Sede'}</h5>
        </div>

        <div class="card-body">
          <form @submit=${(e) => { e.preventDefault(); this._onSave(); }}>
            <!-- Dati identificativi -->
            <h6 class="text-primary mb-3">Dati Identificativi</h6>
            <div class="row">
              <div class="col-md-4">
                <div class="mb-3">
                  <label class="form-label">
                    Codice Sede <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="10"
                    required
                    .value=${this._formCodSede}
                    @input=${(e) => this._formCodSede = e.target.value}
                    ?disabled=${isUpdate}
                  />
                  <small class="form-text text-muted">Massimo 10 caratteri</small>
                </div>
              </div>
              <div class="col-md-8">
                <div class="mb-3">
                  <label class="form-label">
                    Nome Sede <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="255"
                    required
                    .value=${this._formNome}
                    @input=${(e) => this._formNome = e.target.value}
                  />
                </div>
              </div>
            </div>

            <div class="row">
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
                </div>
              </div>
              <div class="col-md-6">
                <div class="mb-3">
                  <label class="form-label">
                    Descrizione Completa <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="512"
                    required
                    .value=${this._formDescrizioneLunga}
                    @input=${(e) => this._formDescrizioneLunga = e.target.value}
                  />
                </div>
              </div>
            </div>

            <!-- Dati geografici -->
            <hr />
            <h6 class="text-primary mb-3">Dati Geografici</h6>
            <div class="row">
              <div class="col-md-8">
                <div class="mb-3">
                  <label class="form-label">
                    Indirizzo <span class="text-danger">*</span>
                  </label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="512"
                    required
                    .value=${this._formIndirizzo}
                    @input=${(e) => this._formIndirizzo = e.target.value}
                  />
                </div>
              </div>
              <div class="col-md-4">
                <div class="mb-3">
                  <label class="form-label">CAP</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="5"
                    pattern="[0-9]{5}"
                    .value=${this._formCap}
                    @input=${(e) => this._formCap = e.target.value}
                  />
                  <small class="form-text text-muted">5 cifre</small>
                </div>
              </div>
            </div>

            <div class="row">
              <div class="col-md-3">
                <div class="mb-3">
                  <label class="form-label">Codice Comune ISTAT</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="4"
                    .value=${this._formCityCode}
                    @input=${(e) => this._formCityCode = e.target.value}
                  />
                  <small class="form-text text-muted">4 caratteri</small>
                </div>
              </div>
              <div class="col-md-3">
                <div class="mb-3">
                  <label class="form-label">Provincia</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="4"
                    .value=${this._formProvinceCode}
                    @input=${(e) => this._formProvinceCode = e.target.value}
                  />
                  <small class="form-text text-muted">Sigla provincia</small>
                </div>
              </div>
              <div class="col-md-3">
                <div class="mb-3">
                  <label class="form-label">Regione ISTAT</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="2"
                    .value=${this._formIstatRegion}
                    @input=${(e) => this._formIstatRegion = e.target.value}
                  />
                  <small class="form-text text-muted">2 cifre</small>
                </div>
              </div>
              <div class="col-md-3">
                <div class="mb-3">
                  <label class="form-label">Nazione</label>
                  <input
                    type="text"
                    class="form-control"
                    maxlength="2"
                    .value=${this._formNazione}
                    @input=${(e) => this._formNazione = e.target.value}
                  />
                  <small class="form-text text-muted">ISO 2 lettere (es: IT)</small>
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

customElements.define('sede-component', SedeComponent);
