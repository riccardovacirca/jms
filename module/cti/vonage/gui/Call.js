/**
 * CTI Bar — barra CTI fissa sul fondo della finestra.
 *
 * Implementa il flusso operator-first progressive dialer con UI a barra fissa.
 * Importabile da qualunque modulo che richieda funzionalità CTI.
 *
 * Utilizzo dal modulo ospitante (es. CRM):
 *   import '<path>/module/cti/vonage/Call.js';
 *   // Aggiungi al template:
 *   <cti-bar></cti-bar>
 *
 * Flusso:
 *   FASE 1 — Connessione: POST /api/cti/vonage/sdk/auth → assegna operatore e JWT SDK.
 *   FASE 2 — Coda: GET /api/cti/vonage/queue/next → estrae contatto dalla coda → dialog conferma.
 *   FASE 3 — Chiamata: client.serverCall() → webhook answer → cliente chiamato in background.
 *   FASE 4 — Conversazione: callAnswered → timer e stats attivi.
 *   FASE 5 — Fine: callHangup o PUT /call/{uuid}/hangup → stato idle.
 *   FASE 6 — Disconnessione: DELETE /api/cti/vonage/sdk/auth → operatore rilasciato.
 */
import { LitElement, html } from 'lit';
import { VonageClient } from '@vonage/client-sdk';
import { callState, targetNumber, currentContact } from './store.js';
import { authorized, user } from '../../../store.js';
import './cti-bar.css';

const REFRESH_DELAY_MS = 13 * 60 * 1000;
const NET_STATS_INTERVAL_MS = 5000;
const TIMER_INTERVAL_MS = 1000;

class Bar extends LitElement
{
  static properties = {
    _sessionActive: { state: true },
    _sessionError:  { state: true },
    _callState:     { state: true },
    _targetNumber:  { state: true },
    _muted:         { state: true },
    _outputVolume:  { state: true },
    _inputGain:     { state: true },
    _showDtmf:      { state: true },
    _iceState:           { state: true },
    _netStats:           { state: true },
    _callSeconds:        { state: true },
    _showMenu:           { state: true },
    _showOperatorDialog: { state: true },
    _operators:          { state: true },
    _operatorsLoading:   { state: true },
    _operatorsSyncing:   { state: true },
    _showNewOperator:    { state: true },
    _newOperatorName:    { state: true },
    _newOperatorDisplay: { state: true },
    _newOperatorSaving:  { state: true },
    _newOperatorError:   { state: true },
    _showContactModal:   { state: true },
    _pendingContact:     { state: true },
    _contactLoading:     { state: true },
    _connecting:         { state: true },
    _dtmfInput:          { state: true },
    _showAddModal:       { state: true },
    _showCallLog:        { state: true },
    _callLog:            { state: true },
    _callLogLoading:     { state: true },
    _callLogTotal:       { state: true },
    _callLogPage:        { state: true },
    _showErrorModal:     { state: true },
    _user:               { state: true },
    _authorized:         { state: true },
    _autoDialerActive:   { state: true },
  };

  createRenderRoot()
  {
    return this;
  }

  constructor()
  {
    super();
    this._sessionActive = false;
    this._sessionError  = null;
    this._callState     = callState.get();
    this._targetNumber  = targetNumber.get();
    this._muted         = false;
    this._outputVolume  = 5;
    this._inputGain     = 5;
    this._showDtmf      = false;
    this._iceState      = 'new';
    this._netStats      = { packetLoss: null, latency: null };
    this._callSeconds        = 0;
    this._showMenu           = false;
    this._showOperatorDialog = false;
    this._operators          = [];
    this._operatorsLoading   = false;
    this._operatorsSyncing   = false;
    this._showNewOperator    = false;
    this._newOperatorName    = '';
    this._newOperatorDisplay = '';
    this._newOperatorSaving  = false;
    this._newOperatorError   = null;
    this._showContactModal   = false;
    this._pendingContact     = null;
    this._contactLoading     = false;
    this._connecting         = false;
    this._dtmfInput          = '';
    this._showAddModal       = false;
    this._addFormData        = { nome: '', cognome: '', phone: '', note: '' };
    this._showCallLog        = false;
    this._callLog            = [];
    this._callLogLoading     = false;
    this._callLogTotal       = 0;
    this._callLogPage        = 1;
    this._showErrorModal     = false;
    this._user               = user.get();
    this._authorized         = authorized.get();
    this._client             = null;
    this._lastToken          = null;
    this._refreshTimer       = null;
    this._callTimer          = null;
    this._netStatsTimer      = null;
    this._autoDialerActive  = false;
    this._autoDialerTimer   = null;
    this._unsubCall          = null;
    this._unsubTarget        = null;
    this._unsubUser          = null;
    this._unsubAuthorized    = null;
    this._gainNode           = null;
    this._menuClickOutside   = null;
  }

  connectedCallback()
  {
    super.connectedCallback();
    this._unsubCall   = callState.subscribe(v => { this._callState = v; });
    this._unsubTarget = targetNumber.subscribe(v => { this._targetNumber = v; });
    this._unsubUser       = user.subscribe(v => { this._user = v; });
    this._unsubAuthorized = authorized.subscribe(v => { this._authorized = v; });
    this._client      = new VonageClient();
  }

  disconnectedCallback()
  {
    super.disconnectedCallback();
    if (this._unsubCall) {
      this._unsubCall();
    }
    if (this._unsubTarget) {
      this._unsubTarget();
    }
    if (this._unsubUser) {
      this._unsubUser();
    }
    if (this._unsubAuthorized) {
      this._unsubAuthorized();
    }
    if (this._menuClickOutside) {
      document.removeEventListener('click', this._menuClickOutside);
    }
    this._teardown();
  }

  /**
   * Focalizza l'input DTMF all'apertura della tastiera.
   *
   * @param {Map} changedProperties proprietà cambiate nel ciclo di render
   */
  updated(changedProperties)
  {
    let input;

    if (changedProperties.has('_showDtmf') && this._showDtmf) {
      input = this.querySelector('.cti-dtmf-input');
      if (input) {
        input.focus();
      }
    }
  }

  /**
   * Crea la sessione WebRTC con Vonage.
   * Richiede un operatore libero al backend e inizializza il Vonage Client SDK.
   */
  async _connect()
  {
    let token;

    this._sessionError = null;
    this._connecting   = true;

    try {
      token = await this._fetchToken();
      await this._client.createSession(token);
      this._lastToken     = token;
      this._sessionActive = true;
      this._registerListeners();
      this._scheduleRefresh();
      this._startCallTimer();
    } catch (e) {
      this._sessionError  = e.message;
      this._sessionActive = false;
    }

    this._connecting = false;
  }

  /**
   * Richiede al backend il JWT RS256 per il Vonage Client SDK.
   * Il backend assegna un operatore libero (o rinnova quello già assegnato).
   *
   * @returns {Promise<string>} token JWT da passare a VonageClient.createSession()
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
    this._stopNetStatsPolling();
    callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
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
    this._startNetStatsPolling();
  }

  /**
   * Handler sessionError: la sessione WebRTC si è chiusa inaspettatamente.
   *
   * @param {*} reason motivo dell'errore
   */
  _onSessionError(reason)
  {
    this._sessionActive = false;
    this._sessionError  = String(reason);
    this._stopCallTimer();
    this._stopNetStatsPolling();
    callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
  }

  /**
   * Avvia il flusso dialer: recupera il prossimo contatto dalla coda e mostra il dialog di conferma.
   * Se la coda è vuota, mostra un messaggio di errore.
   */
  async _fetchAndShowContact()
  {
    let contact;

    this._sessionError   = null;
    this._contactLoading = true;

    try {
      contact = await this._fetchNextContact();
      
      if (!contact) {
        this._sessionError = 'Coda vuota: nessun contatto disponibile';
        this._contactLoading = false;
        return;
      }
      
      this._pendingContact   = contact;
      this._showContactModal = true;
      currentContact.set(contact);
    } catch (e) {
      this._sessionError = 'Errore recupero contatto: ' + e.message;
    }

    this._contactLoading = false;
  }

  /**
   * Recupera il prossimo contatto dalla coda CTI.
   * La coda è condivisa tra tutti gli operatori e gestita dal backend.
   *
   * @returns {Promise<object|null>} contatto con il campo {@code phone} e opzionalmente {@code data}, o null se la coda è vuota
   */
  async _fetchNextContact()
  {
    let response;
    let data;

    response = await fetch('/api/cti/vonage/queue/next');

    data = await response.json();

    if (data.err) {
      throw new Error(data.log || 'Errore recupero dalla coda');
    }

    if (!data.out) {
      return null;
    }

    return data.out.contatto;
  }

  /**
   * Toggle auto-dialer on/off.
   * L'auto-dialer chiama automaticamente il prossimo contatto dalla coda ogni N secondi.
   */
  _toggleAutoDialer()
  {
    if (this._autoDialerActive) {
      this._stopAutoDialer();
    } else {
      this._startAutoDialer();
    }
  }

  /**
   * Avvia l'auto-dialer con intervallo configurabile (default 20 secondi).
   */
  _startAutoDialer()
  {
    const intervalMs = 20 * 1000;
    
    this._autoDialerActive = true;
    
    // Prima chiamata immediata
    this._fetchAndShowContact();
    
    // Successivamente ogni N secondi
    this._autoDialerTimer = setInterval(() => {
      if (!this._callState.active && this._sessionActive) {
        this._fetchAndShowContact();
      }
    }, intervalMs);
  }

  /**
   * Ferma l'auto-dialer e pulisce il timer.
   */
  _stopAutoDialer()
  {
    this._autoDialerActive = false;
    
    if (this._autoDialerTimer) {
      clearInterval(this._autoDialerTimer);
      this._autoDialerTimer = null;
    }
  }

  /**
   * Contatto mock usato quando il modulo dialer esterno non è disponibile.
   *
   * @returns {object} contatto di esempio
   */
  _mockContact()
  {
    return {
      id:       null,
      phone:    '12345678901',
      callback: null,
      data:     [
        { key: 'Nome',    value: 'Mario Rossi',                    type: 'string' },
        { key: 'Email',   value: 'mario.rossi@example.com',        type: 'string' },
        { key: 'Note',    value: '[mock — modulo dialer non configurato]', type: 'text' },
      ]
    };
  }

  /**
   * Conferma la chiamata dal dialog: imposta il numero nello store e avvia la chiamata.
   * client.serverCall() trigera il webhook /api/cti/vonage/answer.
   */
  async _confirmCall()
  {
    let number;
    let callId;
    let contactId;
    let callbackUrl;

    this._showContactModal = false;
    number = this._pendingContact ? this._pendingContact.phone : null;

    if (!number || number.trim() === '') {
      this._sessionError = 'Numero non disponibile per questo contatto';
      return;
    }

    contactId   = this._pendingContact ? (this._pendingContact.id       ?? null) : null;
    callbackUrl = this._pendingContact ? (this._pendingContact.callback ?? null) : null;

    targetNumber.set(number.trim());
    this._sessionError = null;

    try {
      callId = await this._client.serverCall({
        customerNumber: number.trim(),
        contactId:      contactId,
        callbackUrl:    callbackUrl,
      });
      callState.set({
        active: true,
        callId: callId,
        customerNumber: number.trim(),
        status: 'waiting_customer'
      });
    } catch (e) {
      this._sessionError = 'Errore avvio chiamata: ' + e.message;
      callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
    }
  }

  /**
   * Renderizza una singola voce del campo {@code data} del contatto in base al tipo.
   * - {@code string} — testo su singola riga
   * - {@code number} — valore numerico, font monospace tabular
   * - {@code text}   — testo multiriga (pre-wrap)
   *
   * @param {{ key: string, value: string, type: 'string'|'number'|'text' }} item
   */
  _renderContactDataItem(item)
  {
    if (item.type === 'text') {
      return html`
        <tr>
          <th class="text-secondary fw-normal w-25 align-top small">${item.key}</th>
          <td class="small" style="white-space:pre-wrap">${item.value}</td>
        </tr>`;
    }
    if (item.type === 'number') {
      return html`
        <tr>
          <th class="text-secondary fw-normal w-25 small">${item.key}</th>
          <td class="font-monospace">${item.value}</td>
        </tr>`;
    }
    return html`
      <tr>
        <th class="text-secondary fw-normal w-25 small">${item.key}</th>
        <td>${item.value}</td>
      </tr>`;
  }

  /** Annulla il dialog senza avviare la chiamata. */
  _cancelCall()
  {
    this._showContactModal = false;
    this._pendingContact   = null;
  }

  /**
   * Riaggancia la chiamata corrente.
   * Chiama PUT /api/cti/vonage/call/{uuid}/hangup che termina entrambi i leg.
   */
  async _hangup()
  {
    let currentCallId;
    let response;
    let data;

    currentCallId = this._callState.callId;

    try {
      response = await fetch('/api/cti/vonage/call/' + currentCallId + '/hangup', {
        method: 'PUT'
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore hangup');
      }
      this._stopNetStatsPolling();
      callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
    } catch (e) {
      this._sessionError = 'Errore riagganciare: ' + e.message;
    }
  }

  /**
   * Attiva o disattiva il muto sul microfono locale.
   * Usa client.mute() / client.unmute() del Vonage Client SDK.
   */
  _toggleMute()
  {
    this._muted = !this._muted;
    if (this._muted) {
      this._client.mute();
    } else {
      this._client.unmute();
    }
  }

  /**
   * Regola il volume di uscita (cuffie).
   * Applica il volume a tutti gli elementi <audio> gestiti dall'SDK.
   *
   * @param {number} delta variazione (+1 o -1), clampata a [0, 10]
   */
  _adjustOutputVolume(delta)
  {
    let next;

    next = Math.max(0, Math.min(10, this._outputVolume + delta));
    this._outputVolume = next;
    document.querySelectorAll('audio').forEach(el => {
      el.volume = next / 10;
    });
  }

  /**
   * Regola il guadagno del microfono tramite Web Audio GainNode.
   * Il GainNode deve essere collegato dallo stream locale prima che sia efficace.
   *
   * @param {number} delta variazione (+1 o -1), clampata a [0, 10]
   */
  _adjustInputGain(delta)
  {
    let next;

    next = Math.max(0, Math.min(10, this._inputGain + delta));
    this._inputGain = next;
    if (this._gainNode) {
      // Scala 0-10 → 0.0-2.0 (5 = gain neutro 1.0)
      this._gainNode.gain.value = next / 5;
    }
  }

  /**
   * Invia un tono DTMF durante la chiamata attiva.
   * Chiama PUT /api/cti/vonage/call/{uuid}/dtmf con il digit selezionato.
   *
   * @param {string} digit cifra DTMF (0-9, *, #)
   */
  async _sendDtmf(digit)
  {
    let callId;
    let response;
    let data;

    if (!this._callState.active || !this._callState.callId) {
      return;
    }

    callId = this._callState.callId;

    try {
      response = await fetch('/api/cti/vonage/call/' + callId + '/dtmf', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ digits: digit })
      });
      data = await response.json();
      if (data.err) {
        console.error('[CTI] Errore DTMF:', data.log);
      }
    } catch (e) {
      console.error('[CTI] Errore DTMF:', e);
    }
  }

  /** Avvia il timer di durata chiamata (incrementa _callSeconds ogni secondo). */
  _startCallTimer()
  {
    this._callSeconds = 0;
    this._callTimer = setInterval(() => {
      this._callSeconds = this._callSeconds + 1;
    }, TIMER_INTERVAL_MS);
  }

  /** Ferma e azzera il timer di durata chiamata. */
  _stopCallTimer()
  {
    if (this._callTimer) {
      clearInterval(this._callTimer);
      this._callTimer = null;
    }
    this._callSeconds = 0;
  }

  /** Avvia il polling delle statistiche di rete RTCPeerConnection. */
  _startNetStatsPolling()
  {
    this._pollNetStats();
    this._netStatsTimer = setInterval(() => {
      this._pollNetStats();
    }, NET_STATS_INTERVAL_MS);
  }

  /** Ferma il polling e azzera le statistiche di rete. */
  _stopNetStatsPolling()
  {
    if (this._netStatsTimer) {
      clearInterval(this._netStatsTimer);
      this._netStatsTimer = null;
    }
    this._netStats = { packetLoss: null, latency: null };
    this._iceState = 'new';
  }

  /**
   * Legge le statistiche RTCPeerConnection dalla connessione attiva.
   * Cerca la prima RTCPeerConnection con iceConnectionState !== 'closed'.
   */
  async _pollNetStats()
  {
    let connections;
    let pc;
    let stats;
    let packetLoss;
    let latency;
    let inbound;
    let candidate;

    // Recupera la RTCPeerConnection attiva creata dall'SDK nel tab corrente
    connections = window.__ctiPeerConnections;
    pc = null;
    if (connections && connections.length > 0) {
      pc = connections.find(c => c.iceConnectionState !== 'closed') || null;
    }

    if (!pc) {
      return;
    }

    this._iceState = pc.iceConnectionState;

    try {
      stats = await pc.getStats();
      packetLoss = null;
      latency    = null;
      inbound    = null;
      candidate  = null;

      stats.forEach(report => {
        if (report.type === 'inbound-rtp' && report.kind === 'audio') {
          inbound = report;
        }
        if (report.type === 'candidate-pair' && report.state === 'succeeded') {
          candidate = report;
        }
      });

      if (inbound && inbound.packetsReceived > 0) {
        packetLoss = Math.round(
          (inbound.packetsLost / (inbound.packetsReceived + inbound.packetsLost)) * 100
        );
      }
      if (candidate && candidate.currentRoundTripTime !== undefined) {
        latency = Math.round(candidate.currentRoundTripTime * 1000);
      }

      this._netStats = { packetLoss, latency };
    } catch (e) {
      // Statistiche non disponibili, silenzioso
    }
  }

  /**
   * Pianifica il refresh automatico del token JWT SDK ogni 13 minuti.
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
        this._sessionError  = 'Errore refresh sessione: ' + e.message;
      }
    }, REFRESH_DELAY_MS);
  }

  /**
   * Pulizia: rilascia l'operatore assegnato, cancella i timer
   * e chiude la sessione WebRTC.
   */
  async _teardown()
  {
    if (this._refreshTimer) {
      clearTimeout(this._refreshTimer);
      this._refreshTimer = null;
    }
    this._stopCallTimer();
    this._stopNetStatsPolling();
    this._stopAutoDialer();
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
    callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
  }

  /** Disconnessione manuale dell'operatore. */
  async _disconnect()
  {
    await this._teardown();
  }

  /**
   * Formatta i secondi come HH:MM:SS.
   *
   * @param {number} seconds secondi totali
   * @returns {string} stringa formattata
   */
  _formatTime(seconds)
  {
    let h;
    let m;
    let s;

    h = Math.floor(seconds / 3600);
    m = Math.floor((seconds % 3600) / 60);
    s = seconds % 60;
    return [h, m, s].map(v => String(v).padStart(2, '0')).join(':');
  }

  /**
   * Apre o chiude il menu contestuale CTI.
   * Registra un listener click-outside per chiuderlo automaticamente.
   */
  _toggleMenu()
  {
    this._showMenu = !this._showMenu;
    if (this._showMenu) {
      this._menuClickOutside = (e) => {
        if (!this.contains(e.target)) {
          this._showMenu = false;
          document.removeEventListener('click', this._menuClickOutside);
          this._menuClickOutside = null;
        }
      };
      setTimeout(() => {
        document.addEventListener('click', this._menuClickOutside);
      }, 0);
    } else {
      if (this._menuClickOutside) {
        document.removeEventListener('click', this._menuClickOutside);
        this._menuClickOutside = null;
      }
    }
  }

  /**
   * Apre il dialog lista operatori e recupera i dati dal backend.
   */
  async _openOperatorList(e)
  {
    let response;
    let data;

    e.stopPropagation();
    this._showMenu           = false;
    this._showOperatorDialog = true;
    this._operatorsLoading   = true;
    this._operators          = [];

    try {
      response = await fetch('/api/cti/vonage/admin/operator');
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore caricamento operatori');
      }
      this._operators = data.out || [];
    } catch (e) {
      this._sessionError       = 'Errore operatori: ' + e.message;
      this._showOperatorDialog = false;
    }

    this._operatorsLoading = false;
  }

  /** Chiude il dialog lista operatori. */
  _closeOperatorDialog()
  {
    this._showOperatorDialog = false;
  }

  /**
   * Esegue la sincronizzazione degli operatori da Vonage, poi ricarica la lista locale.
   */
  async _syncOperators()
  {
    let response;
    let data;

    this._operatorsSyncing = true;

    try {
      response = await fetch('/api/cti/vonage/admin/operator/sync', { method: 'POST' });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore sincronizzazione');
      }
      this._operatorsLoading = true;
      response = await fetch('/api/cti/vonage/admin/operator');
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore ricaricamento operatori');
      }
      this._operators = data.out || [];
    } catch (e) {
      this._sessionError = 'Errore sync operatori: ' + e.message;
    }

    this._operatorsLoading = false;
    this._operatorsSyncing = false;
  }

  /**
   * Crea un nuovo operatore Vonage e lo registra localmente, poi ricarica la lista.
   */
  async _createOperator()
  {
    let response;
    let data;

    if (!this._newOperatorName.trim()) {
      this._newOperatorError = 'Il nome utente Vonage è obbligatorio';
      return;
    }

    this._newOperatorSaving = true;
    this._newOperatorError  = null;

    try {
      response = await fetch('/api/cti/vonage/admin/operator', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: this._newOperatorName.trim(), displayName: this._newOperatorDisplay.trim() || null })
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore creazione operatore');
      }
      this._showNewOperator    = false;
      this._newOperatorName    = '';
      this._newOperatorDisplay = '';
      this._operatorsLoading   = true;
      response = await fetch('/api/cti/vonage/admin/operator');
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore ricaricamento operatori');
      }
      this._operators = data.out || [];
    } catch (e) {
      this._newOperatorError = e.message;
    }

    this._operatorsLoading = false;
    this._newOperatorSaving = false;
  }

  /**
   * Aggiunge una cifra DTMF al campo input e la invia se la chiamata è attiva.
   *
   * @param {string} d cifra DTMF (0-9, *, #)
   */
  _dtmfDigit(d)
  {
    this._dtmfInput = this._dtmfInput + d;
    if (this._callState.active && this._callState.callId) {
      this._sendDtmf(d);
    }
  }

  /** Cancella l'ultima cifra dal campo DTMF. */
  _dtmfBackspace()
  {
    this._dtmfInput = this._dtmfInput.slice(0, -1);
  }

  /** Cancella tutto il contenuto del campo DTMF. */
  _dtmfClear()
  {
    this._dtmfInput = '';
  }

  /**
   * Gestisce l'input da tastiera nel campo DTMF.
   * Filtra ai soli caratteri validi (0-9, *, #) e invia DTMF se in chiamata.
   *
   * @param {InputEvent} e evento input
   */
  _dtmfKeyInput(e)
  {
    let raw;
    let filtered;
    let prev;
    let added;

    raw      = e.target.value;
    filtered = raw.replace(/[^0-9*#]/g, '');
    prev     = this._dtmfInput;
    this._dtmfInput = filtered;
    if (filtered !== raw) {
      e.target.value = filtered;
    }
    if (this._callState.active && this._callState.callId && filtered.length > prev.length) {
      added = filtered.slice(prev.length);
      for (const ch of added) {
        this._sendDtmf(ch);
      }
    }
  }

  /**
   * Apre la modal "Aggiungi contatto" pre-compilando il numero digitato.
   */
  _openAddModal()
  {
    this._showDtmf   = false;
    this._addFormData = { nome: '', cognome: '', phone: this._dtmfInput, note: '' };
    this._showAddModal = true;
  }

  /** Chiude la modal "Aggiungi contatto". */
  _closeAddModal()
  {
    this._showAddModal = false;
  }

  /**
   * Avvia la chiamata leggendo i dati dal form "Aggiungi contatto".
   * Popola {@code _pendingContact} e delega a {@code _confirmCall}.
   */
  async _confirmAdd()
  {
    let contact;
    let phone;
    let nome;
    let cognome;
    let note;
    let data;

    phone = (this.querySelector('#cti-add-tel')?.value || '').trim();
    if (!phone) {
      this._sessionError = 'Numero di telefono obbligatorio';
      return;
    }

    nome    = (this.querySelector('#cti-add-nome')?.value    || '').trim();
    cognome = (this.querySelector('#cti-add-cognome')?.value || '').trim();
    note    = (this.querySelector('#cti-add-note')?.value    || '').trim();

    data = [
      nome    ? { key: 'Nome',    value: nome,    type: 'string' } : null,
      cognome ? { key: 'Cognome', value: cognome, type: 'string' } : null,
      note    ? { key: 'Note',    value: note,    type: 'text'   } : null,
    ].filter(Boolean);

    contact = {
      id:       null,
      phone:    phone,
      callback: null,
      data:     data.length ? data : null,
    };

    this._showAddModal   = false;
    this._pendingContact = contact;
    await this._confirmCall();
  }

  /**
   * Apre la modal storico chiamate e carica la prima pagina.
   */
  async _openCallLog(e)
  {
    e.stopPropagation();
    this._showMenu    = false;
    this._showCallLog = true;
    await this._loadCallLogPage(1);
  }

  /** Chiude la modal storico chiamate. */
  _closeCallLog()
  {
    this._showCallLog = false;
  }

  /**
   * Carica una pagina del log chiamate dall'API.
   *
   * @param {number} page numero di pagina (da 1)
   */
  async _loadCallLogPage(page)
  {
    let response;
    let data;

    this._callLogLoading = true;
    this._callLogPage    = page;

    try {
      response = await fetch('/api/cti/vonage/call?page=' + page + '&size=15');
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore caricamento chiamate');
      }
      this._callLog      = data.out.items || [];
      this._callLogTotal = data.out.total || 0;
    } catch (e) {
      this._sessionError = 'Errore storico: ' + e.message;
      this._showCallLog  = false;
    }

    this._callLogLoading = false;
  }

  /**
   * Formatta un timestamp (epoch ms da Jackson o stringa ISO) in data/ora locale.
   *
   * @param {number|string|null} val valore dal JSON
   * @returns {string} data formattata o '—'
   */
  _formatCallDate(val)
  {
    let d;

    if (val == null) {
      return '—';
    }
    d = new Date(val);
    if (isNaN(d.getTime())) {
      return String(val);
    }
    return d.toLocaleString('it-IT', { dateStyle: 'short', timeStyle: 'short' });
  }

  /**
   * Formatta i secondi come MM:SS.
   *
   * @param {number|null} sec secondi totali
   * @returns {string} durata formattata o '—'
   */
  _formatCallDuration(sec)
  {
    let m;
    let s;

    if (sec == null) {
      return '—';
    }
    m = Math.floor(sec / 60);
    s = sec % 60;
    return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
  }

  /**
   * Restituisce la classe Bootstrap badge per un dato stato chiamata Vonage.
   *
   * @param {string} stato stato della chiamata
   * @returns {string} classe CSS
   */
  _callStatoBadge(stato)
  {
    if (stato === 'completed')                                            return 'text-bg-success';
    if (stato === 'answered')                                             return 'text-bg-primary';
    if (stato === 'failed' || stato === 'rejected' || stato === 'busy'
        || stato === 'timeout' || stato === 'unanswered')                return 'text-bg-danger';
    if (stato === 'started' || stato === 'ringing')                      return 'text-bg-warning';
    return 'text-bg-secondary';
  }

  /**
   * Restituisce il colore del dot ICE in base allo stato della connessione.
   *
   * @returns {string} colore CSS
   */
  _iceDotColor()
  {
    let connected;

    connected = this._iceState === 'connected' || this._iceState === 'completed';
    return connected ? '#22c55e' : '#ef4444';
  }

  render()
  {
    if (!this._authorized) {
      return html``;
    }

    const cs = this._callState;
    const inCall = cs.active;
    const waiting = cs.status === 'waiting_customer';

    return html`

      <div class="cti-bar">

        <!-- Sessione: Connetti / Timer -->
        ${!this._sessionActive
          ? html`<button class="btn btn-sm btn-primary" @click=${this._connect.bind(this)} ?disabled=${this._connecting}>
                   ${this._connecting
                     ? html`<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>`
                     : html`<i class="bi bi-headset"></i>`}&nbsp; Connetti
                 </button>`
          : html`<button class="btn btn-sm btn-outline-secondary cti-btn-timer" @click=${this._disconnect.bind(this)} title="Disconnetti">
                   ${this._formatTime(this._callSeconds)}
                 </button>`
        }

        <div class="cti-sep"></div>

        <!-- Chiamata: Chiama / Riaggancia + Numero -->
        <div class="cti-group">
          ${!inCall
            ? html`<button
                     class="btn btn-sm btn-outline-success"
                     @click=${this._fetchAndShowContact.bind(this)}
                     title="Chiama prossimo contatto"
                     ?disabled=${!this._sessionActive || this._contactLoading}
                   >${this._contactLoading ? '…' : html`<i class="bi bi-telephone-fill"></i>`}</button>`
            : html`<button
                     class="btn btn-sm btn-danger"
                     @click=${this._hangup.bind(this)}
                     title="Riaggancia"
                   ><i class="bi bi-telephone-x-fill"></i></button>`
          }
          <span class="cti-number ${inCall && !waiting ? 'cti-number-active' : inCall && waiting ? 'cti-number-waiting' : 'cti-number-idle'}">
            ${inCall
              ? (waiting ? '⟳ ' + cs.customerNumber : cs.customerNumber)
              : (this._targetNumber || '—')
            }
          </span>
          ${inCall ? html`<span class="cti-pulse"></span>` : ''}
        </div>

        <div class="cti-sep"></div>

        <!-- Auto-Dialer: Play/Pause -->
        <button
          class="btn btn-sm ${this._autoDialerActive ? 'btn-success' : 'btn-outline-secondary'}"
          @click=${this._toggleAutoDialer.bind(this)}
          title="${this._autoDialerActive ? 'Ferma auto-dialer' : 'Avvia auto-dialer'}"
          ?disabled=${!this._sessionActive || inCall}
        >${this._autoDialerActive ? html`<i class="bi bi-pause-fill"></i>` : html`<i class="bi bi-play-fill"></i>`}</button>

        <div class="cti-sep"></div>

        <!-- Volume cuffie: [−] 🔊 [+] -->
        <div class="cti-group" title="Volume cuffie">
          <button class="btn btn-sm btn-outline-secondary cti-btn-vol"
            @click=${() => this._adjustOutputVolume(-1)}
            ?disabled=${this._outputVolume <= 0}>−</button>
          <i class="bi bi-volume-up-fill text-secondary small"></i>
          <button class="btn btn-sm btn-outline-secondary cti-btn-vol"
            @click=${() => this._adjustOutputVolume(1)}
            ?disabled=${this._outputVolume >= 10}>+</button>
        </div>

        <!-- Microfono: [mute] [−] 🎙 [+] -->
        <div class="cti-group" title="Guadagno microfono">
          <button
            class="btn btn-sm ${this._muted ? 'btn-warning' : 'btn-outline-secondary'}"
            @click=${this._toggleMute.bind(this)}
            title="${this._muted ? 'Riattiva microfono' : 'Silenzia microfono'}"
            ?disabled=${!this._sessionActive}
          >${this._muted ? html`<i class="bi bi-mic-mute-fill"></i>` : html`<i class="bi bi-mic-fill"></i>`}</button>
          <button class="btn btn-sm btn-outline-secondary cti-btn-vol"
            @click=${() => this._adjustInputGain(-1)}
            ?disabled=${this._inputGain <= 0}>−</button>
          <i class="bi bi-mic text-secondary small"></i>
          <button class="btn btn-sm btn-outline-secondary cti-btn-vol"
            @click=${() => this._adjustInputGain(1)}
            ?disabled=${this._inputGain >= 10}>+</button>
        </div>

        <div class="cti-sep"></div>

        <!-- DTMF -->
        <div class="position-relative">
          <button
            class="btn btn-sm cti-btn-dtmf ${this._showDtmf ? 'btn-primary' : 'btn-outline-secondary'}"
            @click=${() => { this._showDtmf = !this._showDtmf; }}
            title="Tastiera DTMF"
            ?disabled=${!this._sessionActive}
          >#</button>
          ${this._showDtmf ? html`
            <div class="cti-dtmf-popup">
              <div class="d-flex gap-1 mb-2">
                <input
                  class="form-control form-control-sm font-monospace cti-dtmf-input"
                  type="text"
                  .value=${this._dtmfInput}
                  placeholder="Numero"
                  @input=${this._dtmfKeyInput.bind(this)}
                >
                <button class="btn btn-sm btn-outline-secondary" @click=${this._dtmfBackspace.bind(this)} title="Cancella ultima cifra">
                  <i class="bi bi-backspace"></i>
                </button>
                <button class="btn btn-sm btn-outline-secondary" @click=${this._dtmfClear.bind(this)} title="Cancella tutto">
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>
              <div class="cti-dtmf-grid">
                ${['1','2','3','4','5','6','7','8','9','*','0','#'].map(d => html`
                  <button class="btn btn-sm btn-outline-secondary cti-btn-dtmf" @click=${() => this._dtmfDigit(d)}>${d}</button>
                `)}
              </div>
              <button
                class="btn btn-success w-100 mt-2"
                @click=${this._openAddModal.bind(this)}
              ><i class="bi bi-person-plus-fill"></i>&nbsp; Aggiungi...</button>
            </div>
          ` : ''}
        </div>

        <div class="cti-sep"></div>

        <!-- Net stats + ICE dot -->
        <div class="d-flex align-items-center gap-1">
          ${this._netStats.latency !== null
            ? html`<span class="cti-stat small text-secondary" title="Latenza RTT">${this._netStats.latency}ms</span>`
            : ''
          }
          ${this._netStats.packetLoss !== null
            ? html`<span class="cti-stat small text-secondary" title="Packet loss">${this._netStats.packetLoss}%▾</span>`
            : ''
          }
          <span
            class="cti-dot ${this._sessionActive
              ? (this._iceState === 'connected' || this._iceState === 'completed' ? 'cti-dot-on' : 'cti-dot-err')
              : 'cti-dot-off'}"
            title="ICE: ${this._iceState}"
          ></span>
        </div>

        <!-- Errore -->
        ${this._sessionError ? html`
          <button class="cti-error" @click=${e => { e.stopPropagation(); this._showErrorModal = true; }}>${this._sessionError}</button>
        ` : ''}

        <div class="flex-grow-1"></div>

        <!-- Context menu -->
        <div class="position-relative">
          <button
            class="btn btn-sm btn-outline-secondary cti-btn-menu"
            @click=${this._toggleMenu.bind(this)}
            title="Menu CTI"
          ><i class="bi bi-three-dots-vertical"></i></button>
          ${this._showMenu ? html`
            <div class="cti-ctx-menu dropdown-menu show">
              ${this._user?.ruolo_level >= 2 ? html`
                <button class="dropdown-item" @click=${this._openOperatorList.bind(this)}><i class="bi bi-people-fill"></i>&nbsp; Lista operatori</button>
              ` : ''}
              <button class="dropdown-item" @click=${this._openCallLog.bind(this)}><i class="bi bi-clock-history"></i>&nbsp; Storico chiamate</button>
            </div>
          ` : ''}
        </div>

      </div>

      <!-- Dialog lista operatori -->
      ${this._showOperatorDialog ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1" @click=${this._closeOperatorDialog.bind(this)}>
          <div class="modal-dialog modal-lg modal-dialog-scrollable" @click=${e => e.stopPropagation()}>
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-people-fill"></i>&nbsp; Operatori CTI</h5>
                <button type="button" class="btn-close" @click=${this._closeOperatorDialog.bind(this)} aria-label="Chiudi"></button>
              </div>
              <div class="modal-body p-0">
                ${this._operatorsLoading
                  ? html`<div class="text-center text-secondary py-4">Caricamento...</div>`
                  : this._operators.length === 0
                    ? html`<div class="text-center text-secondary py-4">Nessun operatore registrato.</div>`
                    : html`
                      <table class="table table-sm mb-0">
                        <thead class="table-light">
                          <tr>
                            <th>ID</th>
                            <th>Vonage User ID</th>
                            <th>Nome</th>
                            <th>Attivo</th>
                          </tr>
                        </thead>
                        <tbody>
                          ${this._operators.map(op => html`
                            <tr>
                              <td>${op.id}</td>
                              <td class="font-monospace small">${op.vonageUserId}</td>
                              <td>${op.nome || '—'}</td>
                              <td><span class="badge ${op.attivo ? 'text-bg-success' : 'text-bg-secondary'}">${op.attivo ? 'Attivo' : 'Inattivo'}</span></td>
                            </tr>
                          `)}
                        </tbody>
                      </table>
                    `
                }
              </div>
              ${this._showNewOperator ? html`
                <div class="border-top p-3">
                  ${this._newOperatorError ? html`
                    <div class="alert alert-danger alert-sm py-1 px-2 mb-2 small">${this._newOperatorError}</div>
                  ` : ''}
                  <div class="mb-2">
                    <label class="form-label small mb-1">Nome utente Vonage <span class="text-danger">*</span></label>
                    <input type="text" class="form-control form-control-sm"
                      placeholder="es. operatore_03"
                      .value=${this._newOperatorName}
                      @input=${e => { this._newOperatorName = e.target.value; this._newOperatorError = null; }}
                      ?disabled=${this._newOperatorSaving}
                    >
                    <div class="form-text">Identificatore univoco su Vonage (solo lettere minuscole, numeri, underscore)</div>
                  </div>
                  <div class="mb-2">
                    <label class="form-label small mb-1">Nome visualizzato</label>
                    <input type="text" class="form-control form-control-sm"
                      placeholder="es. Operatore 03"
                      .value=${this._newOperatorDisplay}
                      @input=${e => { this._newOperatorDisplay = e.target.value; }}
                      ?disabled=${this._newOperatorSaving}
                    >
                  </div>
                  <div class="d-flex gap-2 justify-content-end">
                    <button type="button" class="btn btn-sm btn-outline-secondary" ?disabled=${this._newOperatorSaving} @click=${() => { this._showNewOperator = false; this._newOperatorError = null; }}>Annulla</button>
                    <button type="button" class="btn btn-sm btn-success" ?disabled=${this._newOperatorSaving} @click=${this._createOperator.bind(this)}>
                      ${this._newOperatorSaving
                        ? html`<span class="spinner-border spinner-border-sm me-1"></span> Creazione...`
                        : html`<i class="bi bi-person-plus-fill"></i>&nbsp; Crea operatore`
                      }
                    </button>
                  </div>
                </div>
              ` : ''}
              <div class="modal-footer">
                <button type="button" class="btn btn-sm btn-outline-secondary" @click=${this._closeOperatorDialog.bind(this)}>Chiudi</button>
                <button type="button" class="btn btn-sm btn-outline-primary" ?disabled=${this._operatorsSyncing || this._operatorsLoading || this._showNewOperator} @click=${this._syncOperators.bind(this)}>
                  ${this._operatorsSyncing
                    ? html`<span class="spinner-border spinner-border-sm me-1"></span> Sincronizzazione...`
                    : html`<i class="bi bi-arrow-repeat"></i>&nbsp; Sincronizza...`
                  }
                </button>
                <button type="button" class="btn btn-sm btn-primary" ?disabled=${this._operatorsSyncing || this._operatorsLoading || this._newOperatorSaving} @click=${() => { this._showNewOperator = !this._showNewOperator; this._newOperatorError = null; }}>
                  <i class="bi bi-person-plus-fill"></i>&nbsp; Nuovo...
                </button>
              </div>
            </div>
          </div>
        </div>
      ` : ''}

      <!-- Dialog conferma chiamata -->
      ${this._showContactModal && this._pendingContact ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1" @click=${this._cancelCall.bind(this)}>
          <div class="modal-dialog" @click=${e => e.stopPropagation()}>
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-telephone-fill"></i>&nbsp; Prossimo contatto</h5>
                <button type="button" class="btn-close" @click=${this._cancelCall.bind(this)} aria-label="Annulla"></button>
              </div>
              <div class="modal-body">
                <table class="table table-sm table-borderless mb-0">
                  <tbody>
                    <tr>
                      <th class="text-secondary fw-normal w-25 small">Telefono</th>
                      <td class="font-monospace fw-semibold">${this._pendingContact.phone}</td>
                    </tr>
                    ${(this._pendingContact.data || []).map(item => this._renderContactDataItem(item))}
                  </tbody>
                </table>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" @click=${this._cancelCall.bind(this)}>Annulla</button>
                <button type="button" class="btn btn-success" @click=${this._confirmCall.bind(this)}><i class="bi bi-telephone-fill"></i>&nbsp; Chiama</button>
              </div>
            </div>
          </div>
        </div>
      ` : ''}

      <!-- Modal aggiungi contatto e chiama -->
      ${this._showAddModal ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1" @click=${this._closeAddModal.bind(this)}>
          <div class="modal-dialog" @click=${e => e.stopPropagation()}>
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-telephone-plus-fill"></i>&nbsp; Nuovo contatto</h5>
                <button type="button" class="btn-close" @click=${this._closeAddModal.bind(this)} aria-label="Annulla"></button>
              </div>
              <div class="modal-body">
                <div class="row g-2">
                  <div class="col-6">
                    <label class="form-label text-secondary small mb-1">Nome</label>
                    <input id="cti-add-nome" class="form-control form-control-sm" type="text" placeholder="Nome">
                  </div>
                  <div class="col-6">
                    <label class="form-label text-secondary small mb-1">Cognome</label>
                    <input id="cti-add-cognome" class="form-control form-control-sm" type="text" placeholder="Cognome">
                  </div>
                  <div class="col-12">
                    <label class="form-label text-secondary small mb-1">Telefono</label>
                    <input id="cti-add-tel" class="form-control form-control-sm font-monospace" type="text" placeholder="Telefono" .value=${this._addFormData.phone}>
                  </div>
                  <div class="col-12">
                    <label class="form-label text-secondary small mb-1">Note</label>
                    <textarea id="cti-add-note" class="form-control form-control-sm" rows="2" placeholder="Note"></textarea>
                  </div>
                </div>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" @click=${this._closeAddModal.bind(this)}>Annulla</button>
                <button type="button" class="btn btn-success" @click=${this._confirmAdd.bind(this)}><i class="bi bi-telephone-fill"></i>&nbsp; Chiama</button>
              </div>
            </div>
          </div>
        </div>
      ` : ''}
      <!-- Modal storico chiamate -->
      ${this._showCallLog ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1" @click=${this._closeCallLog.bind(this)}>
          <div class="modal-dialog modal-xl modal-dialog-scrollable" @click=${e => e.stopPropagation()}>
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-clock-history"></i>&nbsp; Storico chiamate</h5>
                <button type="button" class="btn-close" @click=${this._closeCallLog.bind(this)} aria-label="Chiudi"></button>
              </div>
              <div class="modal-body p-0">
                ${this._callLogLoading
                  ? html`<div class="text-center text-secondary py-5">Caricamento...</div>`
                  : this._callLog.length === 0
                    ? html`<div class="text-center text-secondary py-5">Nessuna chiamata registrata.</div>`
                    : html`
                      <table class="table table-sm table-hover mb-0">
                        <thead class="table-light">
                          <tr>
                            <th>Data</th>
                            <th>Numero</th>
                            <th>Operatore</th>
                            <th>Stato</th>
                            <th>Durata</th>
                            <th>Costo</th>
                          </tr>
                        </thead>
                        <tbody>
                          ${this._callLog.map(c => html`
                            <tr>
                              <td class="small text-secondary">${this._formatCallDate(c.data_creazione)}</td>
                              <td class="font-monospace">${c.numero_destinatario || '—'}</td>
                              <td class="small">${c.operatore_nome || '—'}</td>
                              <td><span class="badge ${this._callStatoBadge(c.stato)}">${c.stato || '—'}</span></td>
                              <td class="font-monospace small">${this._formatCallDuration(c.durata)}</td>
                              <td class="small text-secondary">${c.costo ? c.costo + ' €' : '—'}</td>
                            </tr>
                          `)}
                        </tbody>
                      </table>
                    `
                }
              </div>
              ${!this._callLogLoading && this._callLogTotal > 15 ? html`
                <div class="modal-footer justify-content-between py-2">
                  <small class="text-secondary">${this._callLogTotal} chiamate totali</small>
                  <div class="d-flex gap-2">
                    <button class="btn btn-sm btn-outline-secondary"
                      ?disabled=${this._callLogPage <= 1}
                      @click=${() => this._loadCallLogPage(this._callLogPage - 1)}
                    ><i class="bi bi-chevron-left"></i></button>
                    <span class="small align-self-center text-secondary">Pagina ${this._callLogPage}</span>
                    <button class="btn btn-sm btn-outline-secondary"
                      ?disabled=${this._callLogPage * 15 >= this._callLogTotal}
                      @click=${() => this._loadCallLogPage(this._callLogPage + 1)}
                    ><i class="bi bi-chevron-right"></i></button>
                  </div>
                </div>
              ` : ''}
            </div>
          </div>
        </div>
      ` : ''}

      <!-- Modal dettaglio errore -->
      ${this._showErrorModal ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1" @click=${e => { e.stopPropagation(); this._showErrorModal = false; }}>
          <div class="modal-dialog" @click=${e => e.stopPropagation()}>
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title text-danger"><i class="bi bi-exclamation-triangle-fill"></i>&nbsp; Dettaglio errore</h5>
                <button type="button" class="btn-close" @click=${() => { this._showErrorModal = false; }} aria-label="Chiudi"></button>
              </div>
              <div class="modal-body">
                <pre class="mb-0" style="white-space: pre-wrap; word-break: break-word; font-size: 13px;">${this._sessionError}</pre>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-sm btn-outline-secondary" @click=${() => { this._showErrorModal = false; }}>Chiudi</button>
                <button type="button" class="btn btn-sm btn-outline-danger" @click=${() => { this._sessionError = null; this._showErrorModal = false; }}>
                  <i class="bi bi-x-circle"></i>&nbsp; Cancella errore
                </button>
              </div>
            </div>
          </div>
        </div>
      ` : ''}
    `;
  }
}

customElements.define('cti-bar', Bar);
