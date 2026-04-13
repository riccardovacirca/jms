import { LitElement, html } from 'lit';
import { Logger } from '../../../../util/Logger.js';

/**
 * Gestione operatori CTI.
 * Mostra la lista degli operatori con l'account utente associato permanentemente.
 * Consente sincronizzazione da Vonage, creazione, e assegnazione/rimozione account.
 */
class Operators extends LitElement
{
  static properties = {
    _items:          { state: true },
    _loading:        { state: true },
    _syncing:        { state: true },
    _showNew:        { state: true },
    _newName:        { state: true },
    _saving:         { state: true },
    _error:          { state: true },
    _formErr:        { state: true },
    _assigningId:    { state: true },
    _accounts:       { state: true },
    _accountsLoading: { state: true },
    _selectedAccountId: { state: true },
    _assignSaving:   { state: true },
    _assignErr:      { state: true },
    _queueOpId:      { state: true },
    _queueItems:     { state: true },
    _queueLoading:   { state: true },
    _queueError:     { state: true },
  };

  createRenderRoot()
  {
    return this;
  }

  constructor()
  {
    super();
    this._items             = [];
    this._loading           = false;
    this._syncing           = false;
    this._showNew           = false;
    this._newName           = '';
    this._saving            = false;
    this._error             = null;
    this._formErr           = null;
    this._assigningId       = null;
    this._accounts          = [];
    this._accountsLoading   = false;
    this._selectedAccountId = '';
    this._assignSaving      = false;
    this._assignErr         = null;
    this._queueOpId         = null;
    this._queueItems        = [];
    this._queueLoading      = false;
    this._queueError        = null;
  }

  connectedCallback()
  {
    super.connectedCallback();
    this._load();
  }

  /** Carica la lista degli operatori dal backend. */
  async _load()
  {
    let response;
    let data;

    this._loading = true;
    this._error   = null;

    Logger.debug('[CTI-Admin] Caricamento lista operatori');

    try {
      response = await fetch('/api/cti/vonage/admin/operator');
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore caricamento operatori');
      }
      this._items = data.out || [];
      Logger.debug('[CTI-Admin] Lista operatori caricata', { count: this._items.length });
    } catch (e) {
      this._error = e.message;
      Logger.error('[CTI-Admin] Errore caricamento operatori', { message: e.message });
    }

    this._loading = false;
  }

  /** Sincronizza gli operatori da Vonage, poi ricarica la lista. */
  async _sync()
  {
    let response;
    let data;

    this._syncing = true;
    this._error   = null;

    Logger.debug('[CTI-Admin] Sincronizzazione operatori da Vonage: avvio');

    try {
      response = await fetch('/api/cti/vonage/admin/operator/sync', { method: 'POST' });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore sincronizzazione');
      }
      Logger.debug('[CTI-Admin] Sincronizzazione completata', { created: (data.out || []).length });
      await this._load();
    } catch (e) {
      this._error = e.message;
      Logger.error('[CTI-Admin] Errore sincronizzazione operatori', { message: e.message });
    }

    this._syncing = false;
  }

  /** Crea un nuovo operatore Vonage e ricarica la lista. */
  async _create()
  {
    let response;
    let data;

    if (!this._newName.trim()) {
      this._formErr = 'Il nome utente Vonage è obbligatorio';
      return;
    }

    this._saving  = true;
    this._formErr = null;

    Logger.debug('[CTI-Admin] Creazione operatore', { name: this._newName.trim() });

    try {
      response = await fetch('/api/cti/vonage/admin/operator', {
        method:  'POST',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ name: this._newName.trim() }),
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore creazione operatore');
      }
      Logger.debug('[CTI-Admin] Operatore creato', { vonageUserId: data.out && data.out.vonageUserId });
      this._showNew = false;
      this._newName = '';
      await this._load();
    } catch (e) {
      this._formErr = e.message;
      Logger.error('[CTI-Admin] Errore creazione operatore', { message: e.message });
    }

    this._saving = false;
  }

  /**
   * Apre il pannello di assegnazione per l'operatore dato, caricando la lista account.
   *
   * @param {Object} op operatore selezionato
   */
  async _openAssign(op)
  {
    let response;
    let data;

    this._assigningId       = op.id;
    this._selectedAccountId = op.account_id ? String(op.account_id) : '';
    this._assignErr         = null;
    this._accountsLoading   = true;
    this._accounts          = [];

    Logger.debug('[CTI-Admin] Apertura pannello assegnazione', { operatoreId: op.id, vonageUserId: op.vonage_user_id });

    try {
      response = await fetch(`/api/cti/vonage/admin/accounts?operatorId=${op.id}`);
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore caricamento account');
      }
      this._accounts = data.out || [];
      Logger.debug('[CTI-Admin] Account disponibili caricati', { count: this._accounts.length });
    } catch (e) {
      this._assignErr = e.message;
      Logger.error('[CTI-Admin] Errore caricamento account', { message: e.message });
    }

    this._accountsLoading = false;
  }

  /** Chiude il pannello di assegnazione. */
  _closeAssign()
  {
    this._assigningId       = null;
    this._selectedAccountId = '';
    this._assignErr         = null;
    this._accounts          = [];
  }

  /** Salva l'assegnazione dell'account selezionato all'operatore corrente. */
  async _saveAssign()
  {
    let response;
    let data;

    if (!this._selectedAccountId) {
      this._assignErr = 'Seleziona un account';
      return;
    }

    this._assignSaving = true;
    this._assignErr    = null;

    Logger.debug('[CTI-Admin] Assegnazione account a operatore', { operatoreId: this._assigningId, accountId: this._selectedAccountId });

    try {
      response = await fetch(`/api/cti/vonage/admin/operator/${this._assigningId}/account`, {
        method:  'PUT',
        headers: { 'Content-Type': 'application/json' },
        body:    JSON.stringify({ accountId: parseInt(this._selectedAccountId, 10) }),
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore assegnazione');
      }
      Logger.debug('[CTI-Admin] Account assegnato con successo', { operatoreId: this._assigningId, accountId: this._selectedAccountId });
      this._closeAssign();
      await this._load();
    } catch (e) {
      this._assignErr = e.message;
      Logger.error('[CTI-Admin] Errore assegnazione account', { message: e.message });
    }

    this._assignSaving = false;
  }

  /**
   * Rimuove l'associazione permanente tra account e operatore.
   *
   * @param {number} operatoreId id dell'operatore
   */
  async _removeAssign(operatoreId)
  {
    let response;
    let data;

    this._error = null;

    Logger.debug('[CTI-Admin] Rimozione assegnazione account', { operatoreId });

    try {
      response = await fetch(`/api/cti/vonage/admin/operator/${operatoreId}/account`, {
        method: 'DELETE',
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore rimozione assegnazione');
      }
      Logger.debug('[CTI-Admin] Assegnazione rimossa', { operatoreId });
      await this._load();
    } catch (e) {
      this._error = e.message;
      Logger.error('[CTI-Admin] Errore rimozione assegnazione', { operatoreId, message: e.message });
    }
  }

  /**
   * Apre il pannello coda dell'operatore dato e carica i contatti.
   *
   * @param {Object} op operatore selezionato
   */
  async _openQueue(op)
  {
    let response;
    let data;

    this._queueOpId    = op.id;
    this._queueItems   = [];
    this._queueError   = null;
    this._queueLoading = true;

    Logger.debug('[CTI-Admin] Caricamento coda operatore', { operatoreId: op.id });

    try {
      response = await fetch(`/api/cti/vonage/admin/operator/${op.id}/queue`);
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore caricamento coda');
      }
      this._queueItems = data.out || [];
      Logger.debug('[CTI-Admin] Coda operatore caricata', { operatoreId: op.id, count: this._queueItems.length });
    } catch (e) {
      this._queueError = e.message;
      Logger.error('[CTI-Admin] Errore caricamento coda operatore', { operatoreId: op.id, message: e.message });
    }

    this._queueLoading = false;
  }

  /** Chiude il pannello coda. */
  _closeQueue()
  {
    this._queueOpId  = null;
    this._queueItems = [];
    this._queueError = null;
  }

  /**
   * Rimuove forzatamente un contatto dalla coda personale dell'operatore (cleanup orfano).
   *
   * @param {number} id id del record in jms_cti_operatore_contatti
   */
  async _forceRemoveContact(id)
  {
    let response;
    let data;

    Logger.debug('[CTI-Admin] Rimozione forzata contatto coda', { id });

    try {
      response = await fetch(`/api/cti/vonage/admin/queue/contatto/${id}`, { method: 'DELETE' });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore rimozione');
      }
      this._queueItems = this._queueItems.filter(item => item.id !== id);
      Logger.debug('[CTI-Admin] Contatto rimosso dalla coda', { id });
    } catch (e) {
      this._queueError = e.message;
      Logger.error('[CTI-Admin] Errore rimozione contatto coda', { id, message: e.message });
    }
  }

  /**
   * Formatta una stringa datetime ISO in formato locale leggibile.
   *
   * @param {string|null} iso stringa ISO 8601
   * @returns {string} data/ora formattata o '—'
   */
  _fmtDatetime(iso)
  {
    let d;

    if (!iso) {
      return '—';
    }
    d = new Date(iso);
    return d.toLocaleString('it-IT', { dateStyle: 'short', timeStyle: 'short' });
  }

  render()
  {
    return html`
      <div class="p-3">

        <div class="d-flex justify-content-between align-items-center mb-3">
          <h6 class="mb-0">Operatori CTI</h6>
          <div class="d-flex gap-2">
            <button class="btn btn-sm btn-outline-secondary"
              ?disabled=${this._syncing || this._loading}
              @click=${this._sync.bind(this)}>
              ${this._syncing
                ? html`<span class="spinner-border spinner-border-sm me-1"></span>Sincronizzazione...`
                : html`<i class="bi bi-arrow-repeat"></i>&nbsp;Sincronizza`}
            </button>
            <button class="btn btn-sm btn-primary"
              ?disabled=${this._syncing || this._loading || this._saving}
              @click=${() => { this._showNew = !this._showNew; this._formErr = null; }}>
              <i class="bi bi-person-plus-fill"></i>&nbsp;Nuovo
            </button>
          </div>
        </div>

        ${this._error ? html`
          <div class="alert alert-danger py-2 px-3 small mb-3">${this._error}</div>
        ` : ''}

        ${this._showNew ? html`
          <div class="card mb-3">
            <div class="card-body">
              ${this._formErr ? html`
                <div class="alert alert-danger py-1 px-2 mb-2 small">${this._formErr}</div>
              ` : ''}
              <div class="mb-3">
                <label class="form-label small mb-1">Nome utente Vonage <span class="text-danger">*</span></label>
                <input type="text" class="form-control form-control-sm"
                  placeholder="es. operatore_03"
                  .value=${this._newName}
                  @input=${e => { this._newName = e.target.value; this._formErr = null; }}
                  ?disabled=${this._saving}>
                <div class="form-text">Identificatore univoco su Vonage (lettere minuscole, numeri, underscore)</div>
              </div>
              <div class="d-flex gap-2 justify-content-end">
                <button class="btn btn-sm btn-outline-secondary"
                  ?disabled=${this._saving}
                  @click=${() => { this._showNew = false; this._formErr = null; }}>Annulla</button>
                <button class="btn btn-sm btn-success"
                  ?disabled=${this._saving}
                  @click=${this._create.bind(this)}>
                  ${this._saving
                    ? html`<span class="spinner-border spinner-border-sm me-1"></span>Creazione...`
                    : html`<i class="bi bi-person-plus-fill"></i>&nbsp;Crea operatore`}
                </button>
              </div>
            </div>
          </div>
        ` : ''}

        ${this._assigningId !== null ? html`
          <div class="card mb-3 border-primary">
            <div class="card-header d-flex justify-content-between align-items-center">
              <span class="small fw-semibold">Assegna account utente</span>
              <button type="button" class="btn-close btn-sm" @click=${this._closeAssign.bind(this)}></button>
            </div>
            <div class="card-body">
              ${this._assignErr ? html`
                <div class="alert alert-danger py-1 px-2 mb-2 small">${this._assignErr}</div>
              ` : ''}
              ${this._accountsLoading ? html`
                <div class="text-secondary small">Caricamento account...</div>
              ` : html`
                <div class="mb-3">
                  <label class="form-label small mb-1">Account utente</label>
                  <select class="form-select form-select-sm"
                    .value=${this._selectedAccountId}
                    @change=${e => { this._selectedAccountId = e.target.value; }}>
                    <option value="">— seleziona —</option>
                    ${this._accounts.map(a => html`
                      <option value=${a.id} ?selected=${String(a.id) === this._selectedAccountId}>
                        ${a.username}
                      </option>
                    `)}
                  </select>
                </div>
                <div class="d-flex gap-2 justify-content-end">
                  <button class="btn btn-sm btn-outline-secondary"
                    ?disabled=${this._assignSaving}
                    @click=${this._closeAssign.bind(this)}>Annulla</button>
                  <button class="btn btn-sm btn-primary"
                    ?disabled=${this._assignSaving || !this._selectedAccountId}
                    @click=${this._saveAssign.bind(this)}>
                    ${this._assignSaving
                      ? html`<span class="spinner-border spinner-border-sm me-1"></span>Salvataggio...`
                      : 'Assegna'}
                  </button>
                </div>
              `}
            </div>
          </div>
        ` : ''}

        ${this._queueOpId !== null ? html`
          <div class="card mb-3 border-secondary">
            <div class="card-header d-flex justify-content-between align-items-center">
              <span class="small fw-semibold">
                <i class="bi bi-inbox me-1"></i>Coda contatti — operatore #${this._queueOpId}
                ${(() => {
                  let op;
                  op = this._items.find(o => o.id === this._queueOpId);
                  if (!op) return '';
                  return op.claim_scadenza && new Date(op.claim_scadenza) > new Date()
                    ? html` <span class="badge text-bg-success ms-1">Sessione attiva</span>`
                    : html` <span class="badge text-bg-secondary ms-1">Disconnesso</span>`;
                })()}
              </span>
              <button type="button" class="btn-close btn-sm" @click=${this._closeQueue.bind(this)}></button>
            </div>
            <div class="card-body p-0">
              ${this._queueError ? html`
                <div class="alert alert-danger py-2 px-3 mb-0 rounded-0 small">${this._queueError}</div>
              ` : ''}
              ${this._queueLoading ? html`
                <div class="text-secondary small p-3">Caricamento...</div>
              ` : this._queueItems.length === 0 ? html`
                <div class="text-secondary small p-3">Nessun contatto in coda.</div>
              ` : html`
                <div class="table-responsive">
                  <table class="table table-sm align-middle mb-0">
                    <thead>
                      <tr>
                        <th class="ps-3">ID</th>
                        <th>Telefono</th>
                        <th>Inserito</th>
                        <th>Disponibile da</th>
                        <th>Stato</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      ${this._queueItems.map(item => {
                        let phone;
                        let now;
                        let disponibile;

                        phone = item.contatto && item.contatto.phone ? item.contatto.phone : '—';
                        now = new Date();
                        disponibile = item.disponibile;

                        return html`
                          <tr>
                            <td class="ps-3 font-monospace small">${item.id}</td>
                            <td class="font-monospace">${phone}</td>
                            <td class="small text-secondary">${this._fmtDatetime(item.dataInserimento)}</td>
                            <td class="small text-secondary">${this._fmtDatetime(item.pianificatoAl)}</td>
                            <td>
                              <span class="badge ${disponibile ? 'text-bg-warning' : 'text-bg-info'}">
                                ${disponibile ? 'Orfano' : 'Pianificato'}
                              </span>
                            </td>
                            <td>
                              <button class="btn btn-sm btn-outline-danger"
                                title="Rimuovi dalla coda"
                                @click=${() => this._forceRemoveContact(item.id)}>
                                <i class="bi bi-trash"></i>
                              </button>
                            </td>
                          </tr>`;
                      })}
                    </tbody>
                  </table>
                </div>
              `}
            </div>
          </div>
        ` : ''}

        ${this._loading
          ? html`<div class="text-center text-secondary py-4">Caricamento...</div>`
          : this._items.length === 0
            ? html`<div class="text-center text-secondary py-4">Nessun operatore registrato.</div>`
            : html`
              <div class="table-responsive">
                <table class="table table-sm table-hover align-middle mb-0">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Vonage User ID</th>
                      <th>Attivo</th>
                      <th>Sessione</th>
                      <th>Utente associato</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    ${this._items.map(op => {
                      let sessioneAttiva;
                      let scadenzaLabel;

                      sessioneAttiva = op.claim_scadenza && new Date(op.claim_scadenza) > new Date();
                      scadenzaLabel  = sessioneAttiva ? `scade ${this._fmtDatetime(op.claim_scadenza)}` : '';

                      return html`
                      <tr>
                        <td>${op.id}</td>
                        <td class="font-monospace small">${op.vonage_user_id}</td>
                        <td>
                          <span class="badge ${op.attivo ? 'text-bg-success' : 'text-bg-secondary'}">
                            ${op.attivo ? 'Attivo' : 'Inattivo'}
                          </span>
                        </td>
                        <td>
                          ${sessioneAttiva
                            ? html`<span class="badge text-bg-success" title=${scadenzaLabel}>Connesso</span>`
                            : html`<span class="badge text-bg-secondary">Disconnesso</span>`}
                        </td>
                        <td>
                          ${op.account_username
                            ? html`<span class="font-monospace small">${op.account_username}</span>`
                            : html`<span class="text-secondary">—</span>`}
                        </td>
                        <td>
                          <div class="d-flex gap-1">
                            <button class="btn btn-sm btn-outline-primary"
                              title="Assegna account"
                              ?disabled=${this._assigningId !== null}
                              @click=${() => this._openAssign(op)}>
                              <i class="bi bi-person-check"></i>
                            </button>
                            ${op.account_id ? html`
                              <button class="btn btn-sm btn-outline-danger"
                                title="Rimuovi assegnazione"
                                ?disabled=${this._assigningId !== null}
                                @click=${() => this._removeAssign(op.id)}>
                                <i class="bi bi-person-x"></i>
                              </button>
                            ` : ''}
                            <button class="btn btn-sm btn-outline-secondary"
                              title="Coda contatti"
                              ?disabled=${this._assigningId !== null}
                              @click=${() => this._queueOpId === op.id ? this._closeQueue() : this._openQueue(op)}>
                              <i class="bi bi-inbox"></i>
                            </button>
                          </div>
                        </td>
                      </tr>`;
                    })}
                  </tbody>
                </table>
              </div>
            `
        }
      </div>
    `;
  }
}

customElements.define('cti-admin-operators', Operators);
