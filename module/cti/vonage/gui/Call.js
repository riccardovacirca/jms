/**
 * Componente principale della sessione CTI.
 *
 * Implementa il flusso operator-first progressive dialer:
 *
 *   FASE 1 — Connessione operatore:
 *     POST /api/cti/vonage/sdk/auth: il backend assegna un operatore libero (claim atomico)
 *     e genera il JWT RS256 per il Vonage Client SDK.
 *     Crea la sessione WebRTC tramite VonageClient.createSession().
 *     Pianifica il refresh automatico del token ogni 13 minuti (idempotente: restituisce
 *     lo stesso operatore già assegnato).
 *
 *   FASE 2 — Avvio chiamata:
 *     client.serverCall({ customerNumber }) triggera il webhook /api/cti/vonage/answer.
 *     Vonage inoltra il customerNumber come query param all'answer URL.
 *     Il backend risponde con NCCO operatore (musica di attesa),
 *     in background (delay 1s) il backend chiama il cliente.
 *
 *   FASE 3 — Chiamata attiva:
 *     Il cliente risponde → conversazione si connette (evento callAnswered).
 *     L'operatore può riagganciare tramite /api/cti/vonage/call/{uuid}/hangup.
 *
 *   FASE 4 — Fine chiamata:
 *     callHangup event → stato torna a idle.
 *
 *   FASE 5 — Disconnect:
 *     DELETE /api/cti/vonage/sdk/auth: rilascia l'operatore (torna disponibile).
 *     VonageClient.deleteSession() chiude la sessione WebRTC.
 *
 * Dipendenza npm: @vonage/client-sdk
 */
import { LitElement, html } from 'lit';
import { VonageClient } from '@vonage/client-sdk';
import { callState } from './store.js';

const REFRESH_DELAY_MS = 13 * 60 * 1000;

class Call extends LitElement
{
  static properties = {
    _sessionActive:  { state: true },
    _error:          { state: true },
    _customerNumber: { state: true },
    _callState:      { state: true }
  };

  createRenderRoot()
  {
    return this;
  }

  constructor()
  {
    super();
    this._sessionActive  = false;
    this._error          = null;
    this._customerNumber = '';
    this._callState      = callState.get();
    this._client         = null;
    this._lastToken      = null;
    this._refreshTimer   = null;
    this._unsubCallState = null;
  }

  connectedCallback()
  {
    super.connectedCallback();
    this._unsubCallState = callState.subscribe(v => { this._callState = v; });
    this._client = new VonageClient();
    this._connect();
  }

  disconnectedCallback()
  {
    super.disconnectedCallback();
    if (this._unsubCallState) {
      this._unsubCallState();
    }
    this._teardown();
  }

  /**
   * Crea la sessione WebRTC con Vonage.
   * Richiede l'assegnazione di un operatore al backend, poi crea la sessione.
   */
  async _connect()
  {
    let token;
    let id;

    this._error = null;

    try {
      token = await this._fetchToken();
      id = await this._client.createSession(token);
      this._lastToken = token;
      this._sessionActive = true;
      this._registerListeners();
      this._scheduleRefresh();
    } catch (e) {
      this._error = e.message;
      this._sessionActive = false;
    }
  }

  /**
   * Richiede al backend il JWT SDK per il Vonage Client SDK.
   * Il backend assegna dinamicamente un operatore libero (o rinnova quello già assegnato)
   * leggendo l'accountId dal cookie access_token.
   *
   * @returns {Promise<string>} token JWT RS256 da passare a VonageClient.createSession()
   */
  async _fetchToken()
  {
    let response;
    let data;

    response = await fetch('/api/cti/vonage/sdk/auth', { method: 'POST' });
    if (!response.ok) {
      throw new Error('Token fetch fallito: ' + response.status);
    }
    data = await response.json();
    if (data.err) {
      throw new Error(data.log || 'Errore recupero token SDK');
    }
    return data.out.token;
  }

  /** Registra i listener sugli eventi del Vonage Client SDK. */
  _registerListeners()
  {
    this._client.on('callHangup',   this._onCallHangup.bind(this));
    this._client.on('callAnswered', this._onCallAnswered.bind(this));
    this._client.on('sessionError', this._onSessionError.bind(this));
  }

  /**
   * Handler callHangup: resetta lo stato della chiamata a idle.
   *
   * @param {string} callId id della chiamata terminata
   */
  _onCallHangup(callId)
  {
    callState.set({
      active: false,
      callId: null,
      customerNumber: null,
      status: 'idle'
    });
  }

  /**
   * Handler callAnswered: il cliente ha risposto, conversazione attiva.
   *
   * @param {string} callId id della chiamata
   */
  _onCallAnswered(callId)
  {
    callState.set({
      active: true,
      callId: callId,
      customerNumber: this._callState.customerNumber,
      status: 'connected'
    });
  }

  /**
   * Handler sessionError: la sessione WebRTC si è chiusa inaspettatamente.
   *
   * @param {*} reason motivo dell'errore
   */
  _onSessionError(reason)
  {
    this._sessionActive = false;
    this._error = String(reason);
  }

  /**
   * Avvia il flusso operator-first:
   *   client.serverCall({ customerNumber }) triggera il webhook /api/cti/vonage/answer.
   *   Vonage inoltra customerNumber come query param all'answer URL.
   *   Il backend risponde con NCCO operatore e in background chiama il cliente.
   */
  async _startCall()
  {
    let callId;

    if (!this._customerNumber || this._customerNumber.trim() === '') {
      this._error = 'Inserisci il numero cliente';
      return;
    }

    this._error = null;

    try {
      callId = await this._client.serverCall({ customerNumber: this._customerNumber.trim() });
      callState.set({
        active: true,
        callId: callId,
        customerNumber: this._customerNumber.trim(),
        status: 'waiting_customer'
      });
    } catch (e) {
      this._error = 'Errore avvio chiamata: ' + e.message;
      callState.set({
        active: false,
        callId: null,
        customerNumber: null,
        status: 'idle'
      });
    }
  }

  /**
   * Riagancia la chiamata corrente.
   * Chiama PUT /api/cti/vonage/call/{uuid}/hangup che termina entrambi i leg.
   */
  async _hangup()
  {
    let response;
    let data;
    let currentCallId;

    currentCallId = this._callState.callId;

    try {
      response = await fetch('/api/cti/vonage/call/' + currentCallId + '/hangup', {
        method: 'PUT'
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore hangup');
      }
      callState.set({
        active: false,
        callId: null,
        customerNumber: null,
        status: 'idle'
      });
    } catch (e) {
      this._error = 'Errore riagganciare: ' + e.message;
    }
  }

  /**
   * Pianifica il refresh automatico del token JWT SDK.
   * Il token scade dopo 1 ora; il refresh avviene ogni 13 minuti.
   * Il backend restituisce lo stesso operatore già assegnato (idempotente).
   */
  _scheduleRefresh()
  {
    this._refreshTimer = setTimeout(async () => {
      let token;

      try {
        token = await this._fetchToken();
        await this._client.refreshSession(token);
        this._lastToken = token;
        this._scheduleRefresh();
      } catch (e) {
        this._sessionActive = false;
        this._error = 'Errore refresh sessione: ' + e.message;
      }
    }, REFRESH_DELAY_MS);
  }

  /**
   * Pulizia: rilascia l'operatore assegnato, cancella il timer di refresh
   * e chiude la sessione WebRTC.
   * Chiamato in disconnectedCallback o su disconnessione manuale.
   */
  async _teardown()
  {
    if (this._refreshTimer) {
      clearTimeout(this._refreshTimer);
      this._refreshTimer = null;
    }
    if (this._sessionActive) {
      try {
        await fetch('/api/cti/vonage/sdk/auth', { method: 'DELETE' });
      } catch (e) {
        console.error('[CTI] Errore release operatore:', e);
      }
      if (this._lastToken) {
        try {
          await this._client.deleteSession(this._lastToken);
        } catch (e) {
          console.error('[CTI] Errore deleteSession:', e);
        }
      }
    }
    this._sessionActive = false;
    callState.set({
      active: false,
      callId: null,
      customerNumber: null,
      status: 'idle'
    });
  }

  /** Disconnessione manuale. */
  async _disconnect()
  {
    await this._teardown();
  }

  render()
  {
    if (!this._sessionActive) {
      return html`
        <div class="card">
          <div class="card-body">
            <p class="mb-2">Sessione non attiva</p>
            ${this._error ? html`<p class="text-danger small mb-2">${this._error}</p>` : ''}
            <button class="btn btn-primary btn-sm" @click=${this._connect.bind(this)}>
              Connetti
            </button>
          </div>
        </div>
      `;
    }

    return html`
      <div class="card">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-center mb-3">
            <span class="text-success fw-semibold">Sessione attiva</span>
            <button class="btn btn-outline-secondary btn-sm" @click=${this._disconnect.bind(this)}>
              Disconnetti
            </button>
          </div>

          ${!this._callState.active
            ? html`
                <div>
                  <input
                    class="form-control mb-2"
                    type="text"
                    placeholder="+39XXXXXXXXXX"
                    .value=${this._customerNumber}
                    @input=${e => { this._customerNumber = e.target.value; }}
                  />
                  <button class="btn btn-success btn-sm" @click=${this._startCall.bind(this)}>
                    Pronto per Chiamata
                  </button>
                  ${this._error ? html`<p class="text-danger small mt-2">${this._error}</p>` : ''}
                </div>
              `
            : html`
                <div class="p-3 bg-light rounded">
                  ${this._callState.status === 'waiting_customer'
                    ? html`
                        <p class="mb-1">
                          In attesa cliente
                          <strong>${this._callState.customerNumber}</strong>...
                        </p>
                        <p class="text-muted small mb-2">Il sistema sta contattando il cliente.</p>
                      `
                    : html`
                        <p class="mb-2">
                          In chiamata con
                          <strong>${this._callState.customerNumber}</strong>
                        </p>
                      `
                  }
                  <button class="btn btn-danger btn-sm" @click=${this._hangup.bind(this)}>
                    Riaggancia
                  </button>
                </div>
              `
          }
        </div>
      </div>
    `;
  }
}

customElements.define('cti-call', Call);
