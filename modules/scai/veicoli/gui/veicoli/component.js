import { LitElement, html } from 'lit';

export class VeicoliComponent extends LitElement {
  static properties = {
    _view: { state: true },
    _veicoli: { state: true },
    _loading: { state: true },
    _page: { state: true },
    _size: { state: true },
    _total: { state: true },
    _filtered: { state: true },

    // Filtri
    _filterTarga: { state: true },
    _filterModello: { state: true },
    _filterMarca: { state: true },
    _filterTipo: { state: true },
    _filterAlimentazione: { state: true },

    // Liste lookup
    _tipoList: { state: true },
    _alimentazioneList: { state: true },

    // Form veicolo
    _formId: { state: true },
    _formTarga: { state: true },
    _formModello: { state: true },
    _formMarca: { state: true },
    _formColore: { state: true },
    _formAnnoImmatricolazione: { state: true },
    _formVeicoloTipoId: { state: true },
    _formVeicoloAlimentazioneId: { state: true },
    _formNote: { state: true },

    // Dettaglio
    _currentVeicolo: { state: true }
  };

  constructor() {
    super();
    this._view = 'list';
    this._veicoli = [];
    this._loading = false;
    this._page = 0;
    this._size = 20;
    this._total = 0;
    this._filtered = 0;

    this._filterTarga = '';
    this._filterModello = '';
    this._filterMarca = '';
    this._filterTipo = '';
    this._filterAlimentazione = '';

    this._tipoList = [];
    this._alimentazioneList = [];

    this._resetForm();
    this._currentVeicolo = null;
    this._searchTimer = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadList();
    this._loadTipoList();
    this._loadAlimentazioneList();
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

      if (this._filterTarga) params.append('targa', this._filterTarga);
      if (this._filterModello) params.append('modello', this._filterModello);
      if (this._filterMarca) params.append('marca', this._filterMarca);
      if (this._filterTipo) params.append('veicolo_tipo_id', this._filterTipo);
      if (this._filterAlimentazione) params.append('veicolo_alimentazione_id', this._filterAlimentazione);

      const res = await fetch(`/api/scai/veicolo?${params}`, {
        credentials: 'include'
      });

      if (res.ok) {
        const data = await res.json();
        this._veicoli = data.data || [];
        this._total = data.recordsTotal || 0;
        this._filtered = data.recordsFiltered || 0;
      }
    } catch (err) {
      console.error('Errore caricamento veicoli:', err);
      alert('Errore durante il caricamento dei veicoli');
    } finally {
      this._loading = false;
    }
  }

  async _loadTipoList() {
    try {
      const res = await fetch('/api/scai/veicolo/tipo?length=1000', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        this._tipoList = data.data || [];
      }
    } catch (err) {
      console.error('Errore caricamento tipi veicolo:', err);
    }
  }

  async _loadAlimentazioneList() {
    try {
      const res = await fetch('/api/scai/veicolo/alimentazione?length=1000', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        this._alimentazioneList = data.data || [];
      }
    } catch (err) {
      console.error('Errore caricamento alimentazioni:', err);
    }
  }

  async _loadDetail(id) {
    this._loading = true;
    try {
      const res = await fetch(`/api/scai/veicolo/${id}`, {
        credentials: 'include'
      });

      if (res.ok) {
        this._currentVeicolo = await res.json();
        this._view = 'detail';
      } else {
        alert('Errore durante il caricamento del dettaglio');
      }
    } catch (err) {
      console.error('Errore caricamento dettaglio:', err);
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

  _onPageChange(newPage) {
    this._page = newPage * this._size;
    this._loadList();
  }

  _showCreateForm() {
    this._resetForm();
    this._view = 'form';
  }

  _showEditForm(veicolo) {
    this._formId = veicolo.id;
    this._formTarga = veicolo.targa || '';
    this._formModello = veicolo.modello || '';
    this._formMarca = veicolo.marca || '';
    this._formColore = veicolo.colore || '';
    this._formAnnoImmatricolazione = veicolo.anno_immatricolazione || '';
    this._formVeicoloTipoId = veicolo.veicolo_tipo_id || '';
    this._formVeicoloAlimentazioneId = veicolo.veicolo_alimentazione_id || '';
    this._formNote = veicolo.note || '';
    this._view = 'form';
  }

  _resetForm() {
    this._formId = null;
    this._formTarga = '';
    this._formModello = '';
    this._formMarca = '';
    this._formColore = '';
    this._formAnnoImmatricolazione = '';
    this._formVeicoloTipoId = '';
    this._formVeicoloAlimentazioneId = '';
    this._formNote = '';
  }

  async _saveForm(e) {
    e.preventDefault();
    this._loading = true;

    const payload = {
      targa: this._formTarga,
      modello: this._formModello,
      marca: this._formMarca,
      colore: this._formColore,
      anno_immatricolazione: this._formAnnoImmatricolazione ? parseInt(this._formAnnoImmatricolazione) : null,
      veicolo_tipo_id: this._formVeicoloTipoId ? parseInt(this._formVeicoloTipoId) : null,
      veicolo_alimentazione_id: this._formVeicoloAlimentazioneId ? parseInt(this._formVeicoloAlimentazioneId) : null,
      note: this._formNote
    };

    try {
      const url = this._formId
        ? `/api/scai/veicolo/${this._formId}`
        : '/api/scai/veicolo';
      const method = this._formId ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        alert(this._formId ? 'Veicolo aggiornato con successo' : 'Veicolo creato con successo');
        this._view = 'list';
        this._loadList();
      } else {
        const error = await res.json();
        alert('Errore durante il salvataggio: ' + (error.message || 'Errore sconosciuto'));
      }
    } catch (err) {
      console.error('Errore salvataggio:', err);
      alert('Errore durante il salvataggio');
    } finally {
      this._loading = false;
    }
  }

  async _delete(id) {
    if (!confirm('Sei sicuro di voler eliminare questo veicolo?')) return;

    this._loading = true;
    try {
      const res = await fetch(`/api/scai/veicolo/${id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (res.ok) {
        alert('Veicolo eliminato con successo');
        this._loadList();
      } else {
        alert('Errore durante l\'eliminazione');
      }
    } catch (err) {
      console.error('Errore eliminazione:', err);
      alert('Errore durante l\'eliminazione');
    } finally {
      this._loading = false;
    }
  }

  _backToList() {
    this._view = 'list';
    this._currentVeicolo = null;
  }

  render() {
    if (this._view === 'list') return this._renderList();
    if (this._view === 'detail') return this._renderDetail();
    if (this._view === 'form') return this._renderForm();
  }

  _renderList() {
    const totalPages = Math.ceil(this._filtered / this._size);
    const currentPage = Math.floor(this._page / this._size);

    return html`
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Veicoli (${this._filtered})</h5>
          <button class="btn btn-primary btn-sm" @click=${this._showCreateForm}>
            <i class="bi bi-plus-circle"></i> Nuovo Veicolo
          </button>
        </div>
        <div class="card-body">
          <!-- Filtri -->
          <div class="row g-2 mb-3">
            <div class="col-md-2">
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Targa..."
                .value=${this._filterTarga}
                @input=${(e) => {
                  this._filterTarga = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-2">
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Modello..."
                .value=${this._filterModello}
                @input=${(e) => {
                  this._filterModello = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-2">
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Marca..."
                .value=${this._filterMarca}
                @input=${(e) => {
                  this._filterMarca = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-3">
              <select
                class="form-select form-select-sm"
                .value=${this._filterTipo}
                @change=${(e) => {
                  this._filterTipo = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutti i Tipi</option>
                ${this._tipoList.map(tipo => html`
                  <option value="${tipo.id}">${tipo.descrizione}</option>
                `)}
              </select>
            </div>
            <div class="col-md-3">
              <select
                class="form-select form-select-sm"
                .value=${this._filterAlimentazione}
                @change=${(e) => {
                  this._filterAlimentazione = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutte le Alimentazioni</option>
                ${this._alimentazioneList.map(alim => html`
                  <option value="${alim.id}">${alim.descrizione}</option>
                `)}
              </select>
            </div>
          </div>

          ${this._loading ? html`
            <div class="text-center py-4">
              <div class="spinner-border" role="status">
                <span class="visually-hidden">Caricamento...</span>
              </div>
            </div>
          ` : html`
            <div class="table-responsive">
              <table class="table table-hover table-sm">
                <thead>
                  <tr>
                    <th>Targa</th>
                    <th>Modello</th>
                    <th>Marca</th>
                    <th>Anno</th>
                    <th>Tipo</th>
                    <th>Alimentazione</th>
                    <th style="width: 120px;">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  ${this._veicoli.length === 0 ? html`
                    <tr>
                      <td colspan="7" class="text-center text-muted">Nessun veicolo trovato</td>
                    </tr>
                  ` : this._veicoli.map(v => html`
                    <tr>
                      <td><strong>${v.targa}</strong></td>
                      <td>${v.modello || '-'}</td>
                      <td>${v.marca || '-'}</td>
                      <td>${v.anno_immatricolazione || '-'}</td>
                      <td>
                        ${v.tipo_descrizione ? html`
                          <span class="badge bg-info">${v.tipo_descrizione}</span>
                        ` : '-'}
                      </td>
                      <td>
                        ${v.alimentazione_descrizione ? html`
                          <span class="badge bg-success">${v.alimentazione_descrizione}</span>
                        ` : '-'}
                      </td>
                      <td>
                        <div class="btn-group btn-group-sm">
                          <button class="btn btn-outline-primary" @click=${() => this._loadDetail(v.id)} title="Dettagli">
                            <i class="bi bi-eye"></i>
                          </button>
                          <button class="btn btn-outline-warning" @click=${() => this._showEditForm(v)} title="Modifica">
                            <i class="bi bi-pencil"></i>
                          </button>
                          <button class="btn btn-outline-danger" @click=${() => this._delete(v.id)} title="Elimina">
                            <i class="bi bi-trash"></i>
                          </button>
                        </div>
                      </td>
                    </tr>
                  `)}
                </tbody>
              </table>
            </div>

            <!-- Paginazione -->
            ${totalPages > 1 ? html`
              <nav>
                <ul class="pagination pagination-sm justify-content-center mb-0">
                  <li class="page-item ${currentPage === 0 ? 'disabled' : ''}">
                    <button class="page-link" @click=${() => this._onPageChange(currentPage - 1)}>Precedente</button>
                  </li>
                  ${Array.from({ length: totalPages }, (_, i) => html`
                    <li class="page-item ${i === currentPage ? 'active' : ''}">
                      <button class="page-link" @click=${() => this._onPageChange(i)}>${i + 1}</button>
                    </li>
                  `)}
                  <li class="page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}">
                    <button class="page-link" @click=${() => this._onPageChange(currentPage + 1)}>Successivo</button>
                  </li>
                </ul>
              </nav>
            ` : ''}
          `}
        </div>
      </div>
    `;
  }

  _renderDetail() {
    if (!this._currentVeicolo) return html`<p>Caricamento...</p>`;

    const v = this._currentVeicolo;

    return html`
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Dettaglio Veicolo</h5>
          <button class="btn btn-secondary btn-sm" @click=${this._backToList}>
            <i class="bi bi-arrow-left"></i> Torna alla Lista
          </button>
        </div>
        <div class="card-body">
          <div class="row">
            <div class="col-md-6">
              <dl class="row">
                <dt class="col-sm-4">Targa:</dt>
                <dd class="col-sm-8"><strong>${v.targa}</strong></dd>

                <dt class="col-sm-4">Modello:</dt>
                <dd class="col-sm-8">${v.modello || '-'}</dd>

                <dt class="col-sm-4">Marca:</dt>
                <dd class="col-sm-8">${v.marca || '-'}</dd>

                <dt class="col-sm-4">Colore:</dt>
                <dd class="col-sm-8">${v.colore || '-'}</dd>

                <dt class="col-sm-4">Anno:</dt>
                <dd class="col-sm-8">${v.anno_immatricolazione || '-'}</dd>

                <dt class="col-sm-4">Tipo:</dt>
                <dd class="col-sm-8">
                  ${v.tipo_descrizione ? html`
                    <span class="badge bg-info">${v.tipo_descrizione}</span>
                  ` : '-'}
                </dd>

                <dt class="col-sm-4">Alimentazione:</dt>
                <dd class="col-sm-8">
                  ${v.alimentazione_descrizione ? html`
                    <span class="badge bg-success">${v.alimentazione_descrizione}</span>
                  ` : '-'}
                </dd>
              </dl>
            </div>
            <div class="col-md-6">
              <dl class="row">
                <dt class="col-sm-4">Note:</dt>
                <dd class="col-sm-8">${v.note || '-'}</dd>

                <dt class="col-sm-4">Creato il:</dt>
                <dd class="col-sm-8">${v.created_at ? new Date(v.created_at).toLocaleString() : '-'}</dd>

                <dt class="col-sm-4">Creato da:</dt>
                <dd class="col-sm-8">${v.created_by || '-'}</dd>

                <dt class="col-sm-4">Aggiornato il:</dt>
                <dd class="col-sm-8">${v.updated_at ? new Date(v.updated_at).toLocaleString() : '-'}</dd>

                <dt class="col-sm-4">Aggiornato da:</dt>
                <dd class="col-sm-8">${v.updated_by || '-'}</dd>
              </dl>
            </div>
          </div>

          <div class="mt-3">
            <button class="btn btn-warning btn-sm me-2" @click=${() => this._showEditForm(v)}>
              <i class="bi bi-pencil"></i> Modifica
            </button>
            <button class="btn btn-danger btn-sm" @click=${() => this._delete(v.id)}>
              <i class="bi bi-trash"></i> Elimina
            </button>
          </div>
        </div>
      </div>
    `;
  }

  _renderForm() {
    const isUpdate = !!this._formId;

    return html`
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">${isUpdate ? 'Modifica' : 'Nuovo'} Veicolo</h5>
          <button class="btn btn-secondary btn-sm" @click=${this._backToList}>
            <i class="bi bi-arrow-left"></i> Annulla
          </button>
        </div>
        <div class="card-body">
          <form @submit=${this._saveForm}>
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label">Targa *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="20"
                  required
                  .value=${this._formTarga}
                  @input=${(e) => this._formTarga = e.target.value}
                  ?disabled=${isUpdate}
                />
                ${isUpdate ? html`<small class="text-muted">La targa non può essere modificata</small>` : ''}
              </div>

              <div class="col-md-6">
                <label class="form-label">Modello</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="128"
                  .value=${this._formModello}
                  @input=${(e) => this._formModello = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Marca</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="128"
                  .value=${this._formMarca}
                  @input=${(e) => this._formMarca = e.target.value}
                />
              </div>

              <div class="col-md-3">
                <label class="form-label">Colore</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="64"
                  .value=${this._formColore}
                  @input=${(e) => this._formColore = e.target.value}
                />
              </div>

              <div class="col-md-3">
                <label class="form-label">Anno Immatricolazione</label>
                <input
                  type="number"
                  class="form-control"
                  min="1900"
                  max="2100"
                  .value=${this._formAnnoImmatricolazione}
                  @input=${(e) => this._formAnnoImmatricolazione = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Tipo Veicolo</label>
                <select
                  class="form-select"
                  .value=${this._formVeicoloTipoId}
                  @change=${(e) => this._formVeicoloTipoId = e.target.value}
                >
                  <option value="">-- Seleziona Tipo --</option>
                  ${this._tipoList.map(tipo => html`
                    <option value="${tipo.id}">${tipo.descrizione}</option>
                  `)}
                </select>
              </div>

              <div class="col-md-6">
                <label class="form-label">Alimentazione</label>
                <select
                  class="form-select"
                  .value=${this._formVeicoloAlimentazioneId}
                  @change=${(e) => this._formVeicoloAlimentazioneId = e.target.value}
                >
                  <option value="">-- Seleziona Alimentazione --</option>
                  ${this._alimentazioneList.map(alim => html`
                    <option value="${alim.id}">${alim.descrizione}</option>
                  `)}
                </select>
              </div>

              <div class="col-12">
                <label class="form-label">Note</label>
                <textarea
                  class="form-control"
                  rows="3"
                  .value=${this._formNote}
                  @input=${(e) => this._formNote = e.target.value}
                ></textarea>
              </div>
            </div>

            <div class="mt-4">
              <button type="submit" class="btn btn-primary" ?disabled=${this._loading}>
                ${this._loading ? html`
                  <span class="spinner-border spinner-border-sm me-2"></span>
                ` : ''}
                ${isUpdate ? 'Aggiorna' : 'Crea'} Veicolo
              </button>
              <button type="button" class="btn btn-secondary ms-2" @click=${this._backToList}>
                Annulla
              </button>
            </div>
          </form>
        </div>
      </div>
    `;
  }
}

customElements.define('veicoli-component', VeicoliComponent);
