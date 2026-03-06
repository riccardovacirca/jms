import { LitElement, html } from 'lit';

export class PolicyComponent extends LitElement {
  static properties = {
    _view: { state: true },
    _policies: { state: true },
    _loading: { state: true },
    _page: { state: true },
    _size: { state: true },
    _total: { state: true },
    _filtered: { state: true },

    // Filtri
    _filterCodEnte: { state: true },
    _filterMatricola: { state: true },
    _filterRepertorio: { state: true },
    _filterSdc: { state: true },
    _filterPolicy: { state: true },

    // Liste lookup
    _enteList: { state: true },
    _rapportoList: { state: true },
    _repertorioList: { state: true },
    _sdcList: { state: true },

    // Form
    _formId: { state: true },
    _formCodEnte: { state: true },
    _formMatricola: { state: true },
    _formSlugSdc: { state: true },
    _formCodiceRepertorio: { state: true },
    _formCodicePolicy: { state: true },
    _formDataInizioValidita: { state: true },

    // Dettaglio
    _currentPolicy: { state: true }
  };

  constructor() {
    super();
    this._view = 'list';
    this._policies = [];
    this._loading = false;
    this._page = 0;
    this._size = 20;
    this._total = 0;
    this._filtered = 0;

    this._filterCodEnte = '';
    this._filterMatricola = '';
    this._filterRepertorio = '';
    this._filterSdc = '';
    this._filterPolicy = '';

    this._enteList = [];
    this._rapportoList = [];
    this._repertorioList = [];
    this._sdcList = [];

    this._resetForm();
    this._currentPolicy = null;
    this._searchTimer = null;
  }

  createRenderRoot() {
    return this;
  }

  connectedCallback() {
    super.connectedCallback();
    this._loadList();
    this._loadEnteList();
    this._loadRepertorioList();
    this._loadSdcList();
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

      if (this._filterCodEnte) params.append('cod_ente', this._filterCodEnte);
      if (this._filterMatricola) params.append('matricola', this._filterMatricola);
      if (this._filterRepertorio) params.append('codice_repertorio', this._filterRepertorio);
      if (this._filterSdc) params.append('slug_sdc', this._filterSdc);
      if (this._filterPolicy) params.append('codice_policy', this._filterPolicy);

      const res = await fetch(`/api/scai/policy?${params}`, {
        credentials: 'include'
      });

      if (res.ok) {
        const data = await res.json();
        this._policies = data.data || [];
        this._total = data.recordsTotal || 0;
        this._filtered = data.recordsFiltered || 0;
      }
    } catch (err) {
      console.error('Errore caricamento policy:', err);
      alert('Errore durante il caricamento delle policy');
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

  async _loadRapportoList(codEnte) {
    if (!codEnte) {
      this._rapportoList = [];
      return;
    }
    try {
      const res = await fetch(`/api/scai/rapporti?cod_ente=${codEnte}&length=1000`, {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        this._rapportoList = data.data || [];
      }
    } catch (err) {
      console.error('Errore caricamento rapporti:', err);
    }
  }

  async _loadRepertorioList() {
    try {
      const res = await fetch('/api/scai/repertorio?length=1000', {
        credentials: 'include'
      });
      if (res.ok) {
        const data = await res.json();
        this._repertorioList = data.data || [];
      }
    } catch (err) {
      console.error('Errore caricamento repertori:', err);
    }
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
    } catch (err) {
      console.error('Errore caricamento sdc:', err);
    }
  }

  async _loadDetail(id) {
    this._loading = true;
    try {
      const res = await fetch(`/api/scai/policy/${id}`, {
        credentials: 'include'
      });

      if (res.ok) {
        this._currentPolicy = await res.json();
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

  _showEditForm(policy) {
    this._formId = policy.id;
    this._formCodEnte = policy.cod_ente || '';
    this._formMatricola = policy.matricola || '';
    this._formSlugSdc = policy.slug_sdc || '';
    this._formCodiceRepertorio = policy.codice_repertorio || '';
    this._formCodicePolicy = policy.codice_policy || '';
    this._formDataInizioValidita = policy.data_inizio_validita ?
      new Date(policy.data_inizio_validita).toISOString().slice(0, 16) : '';

    // Load rapporti for selected ente
    if (policy.cod_ente) {
      this._loadRapportoList(policy.cod_ente);
    }

    this._view = 'form';
  }

  _resetForm() {
    this._formId = null;
    this._formCodEnte = '';
    this._formMatricola = '';
    this._formSlugSdc = '';
    this._formCodiceRepertorio = '';
    this._formCodicePolicy = '';
    this._formDataInizioValidita = '';
    this._rapportoList = [];
  }

  async _saveForm(e) {
    e.preventDefault();
    this._loading = true;

    const payload = {
      cod_ente: this._formCodEnte,
      matricola: this._formMatricola,
      slug_sdc: this._formSlugSdc || null,
      codice_repertorio: this._formCodiceRepertorio,
      codice_policy: this._formCodicePolicy,
      data_inizio_validita: this._formDataInizioValidita
    };

    try {
      const url = this._formId
        ? `/api/scai/policy/${this._formId}`
        : '/api/scai/policy';
      const method = this._formId ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        alert(this._formId ? 'Policy aggiornata con successo' : 'Policy creata con successo');
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
    if (!confirm('Sei sicuro di voler eliminare questa policy?')) return;

    this._loading = true;
    try {
      const res = await fetch(`/api/scai/policy/${id}`, {
        method: 'DELETE',
        credentials: 'include'
      });

      if (res.ok) {
        alert('Policy eliminata con successo');
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
    this._currentPolicy = null;
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
          <h5 class="mb-0">Policy Rapporti (${this._filtered})</h5>
          <button class="btn btn-primary btn-sm" @click=${this._showCreateForm}>
            <i class="bi bi-plus-circle"></i> Nuova Policy
          </button>
        </div>
        <div class="card-body">
          <!-- Filtri -->
          <div class="row g-2 mb-3">
            <div class="col-md-2">
              <select
                class="form-select form-select-sm"
                .value=${this._filterCodEnte}
                @change=${(e) => {
                  this._filterCodEnte = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutti gli Enti</option>
                ${this._enteList.map(ente => html`
                  <option value="${ente.cod_ente}">${ente.cod_ente}</option>
                `)}
              </select>
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
              <select
                class="form-select form-select-sm"
                .value=${this._filterRepertorio}
                @change=${(e) => {
                  this._filterRepertorio = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutti i Repertori</option>
                ${this._repertorioList.map(rep => html`
                  <option value="${rep.codice_repertorio}">${rep.codice_repertorio}</option>
                `)}
              </select>
            </div>
            <div class="col-md-3">
              <select
                class="form-select form-select-sm"
                .value=${this._filterSdc}
                @change=${(e) => {
                  this._filterSdc = e.target.value;
                  this._onFilterChange();
                }}
              >
                <option value="">Tutti i Sistemi di Campo</option>
                ${this._sdcList.map(sdc => html`
                  <option value="${sdc.slug}">${sdc.descrizione_breve}</option>
                `)}
              </select>
            </div>
            <div class="col-md-3">
              <input
                type="text"
                class="form-control form-control-sm"
                placeholder="Codice Policy..."
                .value=${this._filterPolicy}
                @input=${(e) => {
                  this._filterPolicy = e.target.value;
                  this._onFilterChange();
                }}
              />
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
                    <th>Ente</th>
                    <th>Matricola</th>
                    <th>Dipendente</th>
                    <th>Repertorio</th>
                    <th>SDC</th>
                    <th>Codice Policy</th>
                    <th>Data Validità</th>
                    <th style="width: 120px;">Azioni</th>
                  </tr>
                </thead>
                <tbody>
                  ${this._policies.length === 0 ? html`
                    <tr>
                      <td colspan="8" class="text-center text-muted">Nessuna policy trovata</td>
                    </tr>
                  ` : this._policies.map(p => html`
                    <tr>
                      <td><span class="badge bg-secondary">${p.cod_ente}</span></td>
                      <td>${p.matricola}</td>
                      <td>${p.nome_rapporto && p.cognome_rapporto ?
                        `${p.cognome_rapporto} ${p.nome_rapporto}` : '-'}</td>
                      <td><span class="badge bg-info">${p.codice_repertorio}</span></td>
                      <td>${p.descrizione_sdc || '-'}</td>
                      <td><strong>${p.codice_policy}</strong></td>
                      <td>${p.data_inizio_validita ?
                        new Date(p.data_inizio_validita).toLocaleDateString() : '-'}</td>
                      <td>
                        <div class="btn-group btn-group-sm">
                          <button class="btn btn-outline-primary" @click=${() => this._loadDetail(p.id)} title="Dettagli">
                            <i class="bi bi-eye"></i>
                          </button>
                          <button class="btn btn-outline-warning" @click=${() => this._showEditForm(p)} title="Modifica">
                            <i class="bi bi-pencil"></i>
                          </button>
                          <button class="btn btn-outline-danger" @click=${() => this._delete(p.id)} title="Elimina">
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
    if (!this._currentPolicy) return html`<p>Caricamento...</p>`;

    const p = this._currentPolicy;

    return html`
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">Dettaglio Policy</h5>
          <button class="btn btn-secondary btn-sm" @click=${this._backToList}>
            <i class="bi bi-arrow-left"></i> Torna alla Lista
          </button>
        </div>
        <div class="card-body">
          <div class="row">
            <div class="col-md-6">
              <dl class="row">
                <dt class="col-sm-5">Ente:</dt>
                <dd class="col-sm-7"><span class="badge bg-secondary">${p.cod_ente}</span></dd>

                <dt class="col-sm-5">Matricola:</dt>
                <dd class="col-sm-7">${p.matricola}</dd>

                <dt class="col-sm-5">Dipendente:</dt>
                <dd class="col-sm-7">${p.nome_rapporto && p.cognome_rapporto ?
                  `${p.cognome_rapporto} ${p.nome_rapporto}` : '-'}</dd>

                <dt class="col-sm-5">Repertorio:</dt>
                <dd class="col-sm-7"><span class="badge bg-info">${p.codice_repertorio}</span></dd>

                <dt class="col-sm-5">Sistema di Campo:</dt>
                <dd class="col-sm-7">${p.descrizione_sdc || '-'}</dd>

                <dt class="col-sm-5">Codice Policy:</dt>
                <dd class="col-sm-7"><strong>${p.codice_policy}</strong></dd>

                <dt class="col-sm-5">Data Validità:</dt>
                <dd class="col-sm-7">${p.data_inizio_validita ?
                  new Date(p.data_inizio_validita).toLocaleString() : '-'}</dd>
              </dl>
            </div>
            <div class="col-md-6">
              <dl class="row">
                <dt class="col-sm-5">Creato il:</dt>
                <dd class="col-sm-7">${p.created_at ? new Date(p.created_at).toLocaleString() : '-'}</dd>

                <dt class="col-sm-5">Creato da:</dt>
                <dd class="col-sm-7">${p.created_by || '-'}</dd>

                <dt class="col-sm-5">Aggiornato il:</dt>
                <dd class="col-sm-7">${p.updated_at ? new Date(p.updated_at).toLocaleString() : '-'}</dd>

                <dt class="col-sm-5">Aggiornato da:</dt>
                <dd class="col-sm-7">${p.updated_by || '-'}</dd>
              </dl>
            </div>
          </div>

          <div class="mt-3">
            <button class="btn btn-warning btn-sm me-2" @click=${() => this._showEditForm(p)}>
              <i class="bi bi-pencil"></i> Modifica
            </button>
            <button class="btn btn-danger btn-sm" @click=${() => this._delete(p.id)}>
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
          <h5 class="mb-0">${isUpdate ? 'Modifica' : 'Nuova'} Policy</h5>
          <button class="btn btn-secondary btn-sm" @click=${this._backToList}>
            <i class="bi bi-arrow-left"></i> Annulla
          </button>
        </div>
        <div class="card-body">
          <form @submit=${this._saveForm}>
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label">Ente *</label>
                <select
                  class="form-select"
                  required
                  .value=${this._formCodEnte}
                  @change=${(e) => {
                    this._formCodEnte = e.target.value;
                    this._formMatricola = '';
                    this._loadRapportoList(e.target.value);
                  }}
                  ?disabled=${isUpdate}
                >
                  <option value="">-- Seleziona Ente --</option>
                  ${this._enteList.map(ente => html`
                    <option value="${ente.cod_ente}">${ente.descrizione_ente}</option>
                  `)}
                </select>
                ${isUpdate ? html`<small class="text-muted">L'ente non può essere modificato</small>` : ''}
              </div>

              <div class="col-md-6">
                <label class="form-label">Dipendente *</label>
                <select
                  class="form-select"
                  required
                  .value=${this._formMatricola}
                  @change=${(e) => this._formMatricola = e.target.value}
                  ?disabled=${!this._formCodEnte || isUpdate}
                >
                  <option value="">-- Seleziona Dipendente --</option>
                  ${this._rapportoList.map(rap => html`
                    <option value="${rap.matricola}">
                      ${rap.matricola} - ${rap.cognome} ${rap.nome}
                    </option>
                  `)}
                </select>
                ${isUpdate ? html`<small class="text-muted">Il dipendente non può essere modificato</small>` : ''}
              </div>

              <div class="col-md-6">
                <label class="form-label">Repertorio *</label>
                <select
                  class="form-select"
                  required
                  .value=${this._formCodiceRepertorio}
                  @change=${(e) => this._formCodiceRepertorio = e.target.value}
                  ?disabled=${isUpdate}
                >
                  <option value="">-- Seleziona Repertorio --</option>
                  ${this._repertorioList.map(rep => html`
                    <option value="${rep.codice_repertorio}">
                      ${rep.codice_repertorio} - ${rep.descrizione}
                    </option>
                  `)}
                </select>
                ${isUpdate ? html`<small class="text-muted">Il repertorio non può essere modificato</small>` : ''}
              </div>

              <div class="col-md-6">
                <label class="form-label">Sistema di Campo</label>
                <select
                  class="form-select"
                  .value=${this._formSlugSdc}
                  @change=${(e) => this._formSlugSdc = e.target.value}
                >
                  <option value="">-- Nessun Sistema --</option>
                  ${this._sdcList.map(sdc => html`
                    <option value="${sdc.slug}">${sdc.descrizione_breve}</option>
                  `)}
                </select>
              </div>

              <div class="col-md-6">
                <label class="form-label">Codice Policy *</label>
                <input
                  type="text"
                  class="form-control"
                  maxlength="16"
                  required
                  .value=${this._formCodicePolicy}
                  @input=${(e) => this._formCodicePolicy = e.target.value}
                  ?disabled=${isUpdate}
                />
                ${isUpdate ? html`<small class="text-muted">Il codice policy non può essere modificato</small>` : ''}
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
            </div>

            <div class="mt-4">
              <button type="submit" class="btn btn-primary" ?disabled=${this._loading}>
                ${this._loading ? html`
                  <span class="spinner-border spinner-border-sm me-2"></span>
                ` : ''}
                ${isUpdate ? 'Aggiorna' : 'Crea'} Policy
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

customElements.define('policy-component', PolicyComponent);
