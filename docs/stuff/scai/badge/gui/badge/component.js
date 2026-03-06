import { LitElement, html } from 'lit';

export class BadgeComponent extends LitElement {
  static properties = {
    _view: { state: true },
    _badges: { state: true },
    _loading: { state: true },
    _page: { state: true },
    _size: { state: true },
    _total: { state: true },
    _filtered: { state: true },

    // Filtri
    _filterNumero: { state: true },
    _filterMatricola: { state: true },
    _filterCognome: { state: true },
    _filterEnte: { state: true },
    _filterTipo: { state: true },
    _filterAttivo: { state: true },

    // Liste lookup
    _enteList: { state: true },
    _tipoList: { state: true },

    // Form
    _formId: { state: true },
    _formSeqId: { state: true },
    _formIdEnte: { state: true },
    _formIdTipoTessera: { state: true },
    _formNumero: { state: true },
    _formMatricola: { state: true },
    _formCodFis: { state: true },
    _formCognome: { state: true },
    _formNome: { state: true },
    _formTecnologia: { state: true },
    _formDataInizioValidita: { state: true },
    _formDataFineValidita: { state: true },
    _formDataProduzione: { state: true },
    _formDataRitiro: { state: true },
    _formAttivo: { state: true },

    // Dettaglio
    _currentBadge: { state: true }
  };

  constructor() {
    super();
    this._view = 'list';
    this._badges = [];
    this._loading = false;
    this._page = 0;
    this._size = 20;
    this._total = 0;
    this._filtered = 0;

    this._filterNumero = '';
    this._filterMatricola = '';
    this._filterCognome = '';
    this._filterEnte = '';
    this._filterTipo = '';
    this._filterAttivo = '';

    this._enteList = [];
    this._tipoList = [];

    this._resetForm();
    this._currentBadge = null;
    this._searchTimer = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadList();
    this._loadEnteList();
    this._loadTipoList();
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

      if (this._filterNumero) params.append('numero', this._filterNumero);
      if (this._filterMatricola) params.append('matricola', this._filterMatricola);
      if (this._filterCognome) params.append('cognome', this._filterCognome);
      if (this._filterEnte) params.append('id_ente', this._filterEnte);
      if (this._filterTipo) params.append('id_tipo_tessera', this._filterTipo);
      if (this._filterAttivo) params.append('attivo', this._filterAttivo);

      const res = await fetch(`/api/scai/badge?${params}`, {
        credentials: 'include'
      });

      if (res.ok) {
        const data = await res.json();
        this._badges = data.data || [];
        this._total = data.recordsTotal || 0;
        this._filtered = data.recordsFiltered || 0;
      }
    } catch (err) {
      console.error('Errore caricamento badge:', err);
      alert('Errore durante il caricamento dei badge');
    } finally {
      this._loading = false;
    }
  }

  async _loadEnteList() {
    try {
      const res = await fetch('/api/scai/ente?length=1000', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        this._enteList = data.data || [];
      }
    } catch (err) {
      console.error('Errore caricamento enti:', err);
    }
  }

  async _loadTipoList() {
    try {
      const res = await fetch('/api/scai/badge/tipo?length=1000', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        this._tipoList = data.data || [];
      }
    } catch (err) {
      console.error('Errore caricamento tipi badge:', err);
    }
  }

  async _loadDetail(id) {
    this._loading = true;
    try {
      const res = await fetch(`/api/scai/badge/${id}`, {
        credentials: 'include'
      });

      if (res.ok) {
        this._currentBadge = await res.json();
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

  _showEditForm(badge) {
    this._formId = badge.id;
    this._formSeqId = badge.seq_id || '';
    this._formIdEnte = badge.id_ente || '';
    this._formIdTipoTessera = badge.id_tipo_tessera || '';
    this._formNumero = badge.numero || '';
    this._formMatricola = badge.matricola || '';
    this._formCodFis = badge.cod_fis || '';
    this._formCognome = badge.cognome || '';
    this._formNome = badge.nome || '';
    this._formTecnologia = badge.tecnologia || '';
    this._formDataInizioValidita = badge.data_inizio_validita ?
      new Date(badge.data_inizio_validita).toISOString().slice(0, 16) : '';
    this._formDataFineValidita = badge.data_fine_validita ?
      new Date(badge.data_fine_validita).toISOString().slice(0, 16) : '';
    this._formDataProduzione = badge.data_produzione ?
      new Date(badge.data_produzione).toISOString().slice(0, 16) : '';
    this._formDataRitiro = badge.data_ritiro ?
      new Date(badge.data_ritiro).toISOString().slice(0, 16) : '';
    this._formAttivo = badge.attivo !== undefined ? String(badge.attivo) : '1';
    this._view = 'form';
  }

  _resetForm() {
    this._formId = null;
    this._formSeqId = '';
    this._formIdEnte = '';
    this._formIdTipoTessera = '';
    this._formNumero = '';
    this._formMatricola = '';
    this._formCodFis = '';
    this._formCognome = '';
    this._formNome = '';
    this._formTecnologia = '';
    this._formDataInizioValidita = '';
    this._formDataFineValidita = '';
    this._formDataProduzione = '';
    this._formDataRitiro = '';
    this._formAttivo = '1';
  }

  async _saveForm(e) {
    e.preventDefault();
    this._loading = true;

    const payload = {
      seq_id: this._formSeqId ? parseInt(this._formSeqId) : null,
      id_ente: parseInt(this._formIdEnte),
      id_tipo_tessera: parseInt(this._formIdTipoTessera),
      numero: this._formNumero,
      matricola: this._formMatricola,
      cod_fis: this._formCodFis,
      cognome: this._formCognome,
      nome: this._formNome,
      tecnologia: this._formTecnologia,
      data_inizio_validita: this._formDataInizioValidita,
      data_fine_validita: this._formDataFineValidita,
      data_produzione: this._formDataProduzione,
      data_ritiro: this._formDataRitiro,
      attivo: parseInt(this._formAttivo)
    };

    try {
      const url = this._formId
        ? `/api/scai/badge/${this._formId}`
        : '/api/scai/badge';
      const method = this._formId ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        alert(this._formId ? 'Badge aggiornato con successo' : 'Badge creato con successo');
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
    if (!confirm('Sei sicuro di voler eliminare questo badge?')) return;

    this._loading = true;
    try {
      const res = await fetch(`/api/scai/badge/${id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (res.ok) {
        alert('Badge eliminato con successo');
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
    this._currentBadge = null;
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
          <h5 class="mb-0">Badge (${this._filtered})</h5>
          <button class="btn btn-primary btn-sm" @click=${this._showCreateForm}>
            <i class="bi bi-plus-circle"></i> Nuovo Badge
          </button>
        </div>
        <div class="card-body">
          <!-- Filtri -->
          <div class="row g-2 mb-3">
            <div class="col-md-2">
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Numero..."
                .value=${this._filterNumero}
                @input=${(e) => {
                  this._filterNumero = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-2">
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Matricola..."
                .value=${this._filterMatricola}
                @input=${(e) => {
                  this._filterMatricola = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-2">
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Cognome..."
                .value=${this._filterCognome}
                @input=${(e) => {
                  this._filterCognome = e.target.value;
                  this._onFilterChange();
                }}
              />
            </div>
            <div class="col-md-2">
              <select
                class="form-select form-select-sm"
                .value=${this._filterEnte}
                @change=${(e) => {
                  this._filterEnte = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutti gli Enti</option>
                ${this._enteList.map(ente => html`
                  <option value="${ente.id}">${ente.descrizione_ente}</option>
                `)}
              </select>
            </div>
            <div class="col-md-2">
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
                  <option value="${tipo.id}">${tipo.descrizione_tipo_badge}</option>
                `)}
              </select>
            </div>
            <div class="col-md-2">
              <select
                class="form-select form-select-sm"
                .value=${this._filterAttivo}
                @change=${(e) => {
                  this._filterAttivo = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutti</option>
                <option value="1">Attivi</option>
                <option value="0">Disattivi</option>
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
                    <th>Numero</th>
                    <th>Matricola</th>
                    <th>Dipendente</th>
                    <th>Ente</th>
                    <th>Tipo</th>
                    <th>Tecnologia</th>
                    <th>Validità</th>
                    <th>Stato</th>
                    <th style="width: 120px;">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  ${this._badges.length === 0 ? html`
                    <tr>
                      <td colspan="9" class="text-center text-muted">Nessun badge trovato</td>
                    </tr>
                  ` : this._badges.map(b => html`
                    <tr>
                      <td><strong>${b.numero}</strong></td>
                      <td>${b.matricola}</td>
                      <td>${b.cognome} ${b.nome}</td>
                      <td>
                        <span class="badge bg-secondary">${b.descrizione_ente || '-'}</span>
                      </td>
                      <td>
                        <span class="badge bg-info">${b.descrizione_tipo_badge || '-'}</span>
                      </td>
                      <td>${b.tecnologia || '-'}</td>
                      <td>
                        ${b.data_inizio_validita && b.data_fine_validita ? html`
                          <small>
                            ${new Date(b.data_inizio_validita).toLocaleDateString()} -
                            ${new Date(b.data_fine_validita).toLocaleDateString()}
                          </small>
                        ` : '-'}
                      </td>
                      <td>
                        ${b.attivo === 1 ? html`
                          <span class="badge bg-success">Attivo</span>
                        ` : html`
                          <span class="badge bg-danger">Disattivo</span>
                        `}
                      </td>
                      <td>
                        <div class="btn-group btn-group-sm">
                          <button class="btn btn-outline-primary" @click=${() => this._loadDetail(b.id)} title="Dettagli">
                            <i class="bi bi-eye"></i>
                          </button>
                          <button class="btn btn-outline-warning" @click=${() => this._showEditForm(b)} title="Modifica">
                            <i class="bi bi-pencil"></i>
                          </button>
                          <button class="btn btn-outline-danger" @click=${() => this._delete(b.id)} title="Elimina">
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
    if (!this._currentBadge) return html`<p>Caricamento...</p>`;

    const b = this._currentBadge;

    return html`
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Dettaglio Badge</h5>
          <button class="btn btn-secondary btn-sm" @click=${this._backToList}>
            <i class="bi bi-arrow-left"></i> Torna alla Lista
          </button>
        </div>
        <div class="card-body">
          <div class="row">
            <div class="col-md-6">
              <dl class="row">
                <dt class="col-sm-5">Numero:</dt>
                <dd class="col-sm-7"><strong>${b.numero}</strong></dd>

                <dt class="col-sm-5">Matricola:</dt>
                <dd class="col-sm-7">${b.matricola}</dd>

                <dt class="col-sm-5">Dipendente:</dt>
                <dd class="col-sm-7">${b.cognome} ${b.nome}</dd>

                <dt class="col-sm-5">Codice Fiscale:</dt>
                <dd class="col-sm-7">${b.cod_fis}</dd>

                <dt class="col-sm-5">Ente:</dt>
                <dd class="col-sm-7"><span class="badge bg-secondary">${b.descrizione_ente || '-'}</span></dd>

                <dt class="col-sm-5">Tipo Badge:</dt>
                <dd class="col-sm-7"><span class="badge bg-info">${b.descrizione_tipo_badge || '-'}</span></dd>

                <dt class="col-sm-5">Tecnologia:</dt>
                <dd class="col-sm-7">${b.tecnologia || '-'}</dd>

                <dt class="col-sm-5">Stato:</dt>
                <dd class="col-sm-7">
                  ${b.attivo === 1 ? html`
                    <span class="badge bg-success">Attivo</span>
                  ` : html`
                    <span class="badge bg-danger">Disattivo</span>
                  `}
                </dd>
              </dl>
            </div>
            <div class="col-md-6">
              <dl class="row">
                <dt class="col-sm-5">Data Inizio Validità:</dt>
                <dd class="col-sm-7">${b.data_inizio_validita ?
                  new Date(b.data_inizio_validita).toLocaleString() : '-'}</dd>

                <dt class="col-sm-5">Data Fine Validità:</dt>
                <dd class="col-sm-7">${b.data_fine_validita ?
                  new Date(b.data_fine_validita).toLocaleString() : '-'}</dd>

                <dt class="col-sm-5">Data Produzione:</dt>
                <dd class="col-sm-7">${b.data_produzione ?
                  new Date(b.data_produzione).toLocaleString() : '-'}</dd>

                <dt class="col-sm-5">Data Ritiro:</dt>
                <dd class="col-sm-7">${b.data_ritiro ?
                  new Date(b.data_ritiro).toLocaleString() : '-'}</dd>

                <dt class="col-sm-5">Creato il:</dt>
                <dd class="col-sm-7">${b.created_at ?
                  new Date(b.created_at).toLocaleString() : '-'}</dd>

                <dt class="col-sm-5">Aggiornato il:</dt>
                <dd class="col-sm-7">${b.updated_at ?
                  new Date(b.updated_at).toLocaleString() : '-'}</dd>
              </dl>
            </div>
          </div>

          <div class="mt-3">
            <button class="btn btn-warning btn-sm me-2" @click=${() => this._showEditForm(b)}>
              <i class="bi bi-pencil"></i> Modifica
            </button>
            <button class="btn btn-danger btn-sm" @click=${() => this._delete(b.id)}>
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
          <h5 class="mb-0">${isUpdate ? 'Modifica' : 'Nuovo'} Badge</h5>
          <button class="btn btn-secondary btn-sm" @click=${this._backToList}>
            <i class="bi bi-arrow-left"></i> Annulla
          </button>
        </div>
        <div class="card-body">
          <form @submit=${this._saveForm}>
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label">Numero Badge *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="20"
                  required
                  .value=${this._formNumero}
                  @input=${(e) => this._formNumero = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Seq ID</label>
                <input
                  type="number"
                  class="form-control"
                  .value=${this._formSeqId}
                  @input=${(e) => this._formSeqId = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Ente *</label>
                <select
                  class="form-select"
                  required
                  .value=${this._formIdEnte}
                  @change=${(e) => this._formIdEnte = e.target.value}
                >
                  <option value="">-- Seleziona Ente --</option>
                  ${this._enteList.map(ente => html`
                    <option value="${ente.id}">${ente.descrizione_ente}</option>
                  `)}
                </select>
              </div>

              <div class="col-md-6">
                <label class="form-label">Tipo Badge *</label>
                <select
                  class="form-select"
                  required
                  .value=${this._formIdTipoTessera}
                  @change=${(e) => this._formIdTipoTessera = e.target.value}
                >
                  <option value="">-- Seleziona Tipo --</option>
                  ${this._tipoList.map(tipo => html`
                    <option value="${tipo.id}">${tipo.descrizione_tipo_badge}</option>
                  `)}
                </select>
              </div>

              <div class="col-md-4">
                <label class="form-label">Matricola *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="15"
                  required
                  .value=${this._formMatricola}
                  @input=${(e) => this._formMatricola = e.target.value}
                />
              </div>

              <div class="col-md-4">
                <label class="form-label">Nome *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="128"
                  required
                  .value=${this._formNome}
                  @input=${(e) => this._formNome = e.target.value}
                />
              </div>

              <div class="col-md-4">
                <label class="form-label">Cognome *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="128"
                  required
                  .value=${this._formCognome}
                  @input=${(e) => this._formCognome = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Codice Fiscale *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="16"
                  required
                  pattern="[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]"
                  .value=${this._formCodFis}
                  @input=${(e) => this._formCodFis = e.target.value.toUpperCase()}
                />
                <small class="text-muted">Formato: RSSMRA80A01H501U</small>
              </div>

              <div class="col-md-6">
                <label class="form-label">Tecnologia *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="8"
                  required
                  .value=${this._formTecnologia}
                  @input=${(e) => this._formTecnologia = e.target.value}
                  placeholder="Es: RFID, NFC"
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Data Inizio Validità *</label>
                <input
                  type="datetime-local"
                  class="form-control"
                  required
                  .value=${this._formDataInizioValidita}
                  @input=${(e) => this._formDataInizioValidita = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Data Fine Validità *</label>
                <input
                  type="datetime-local"
                  class="form-control"
                  required
                  .value=${this._formDataFineValidita}
                  @input=${(e) => this._formDataFineValidita = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Data Produzione</label>
                <input
                  type="datetime-local"
                  class="form-control"
                  .value=${this._formDataProduzione}
                  @input=${(e) => this._formDataProduzione = e.target.value}
                />
              </div>

              <div class="col-md-6">
                <label class="form-label">Data Ritiro</label>
                <input
                  type="datetime-local"
                  class="form-control"
                  .value=${this._formDataRitiro}
                  @input=${(e) => this._formDataRitiro = e.target.value}
                />
              </div>

              <div class="col-md-12">
                <label class="form-label">Stato *</label>
                <select
                  class="form-select"
                  required
                  .value=${this._formAttivo}
                  @change=${(e) => this._formAttivo = e.target.value}
                >
                  <option value="1">Attivo</option>
                  <option value="0">Disattivo</option>
                </select>
              </div>
            </div>

            <div class="mt-4">
              <button type="submit" class="btn btn-primary" ?disabled=${this._loading}>
                ${this._loading ? html`
                  <span class="spinner-border spinner-border-sm me-2"></span>
                ` : ''}
                ${isUpdate ? 'Aggiorna' : 'Crea'} Badge
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

customElements.define('badge-component', BadgeComponent);
