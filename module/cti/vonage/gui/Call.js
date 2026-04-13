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
import { Logger } from '../../../util/Logger.js';
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
    _showScheduleModal:  { state: true },
    _scheduleDateInput:  { state: true },
    _showQueueModal:     { state: true },
    _queueItems:         { state: true },
    _queueLoading:       { state: true },
    _queueError:         { state: true },
    _queueEditId:        { state: true },
    _queueEditDate:      { state: true },
    _queueSaving:        { state: true },
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
    this._showContactModal   = false;
    this._pendingContact     = null;
    this._pendingCodaId      = null;
    this._contactLoading     = false;
    this._showScheduleModal  = false;
    this._scheduleDateInput  = '';
    this._showQueueModal     = false;
    this._queueItems         = [];
    this._queueLoading       = false;
    this._queueError         = null;
    this._queueEditId        = null;
    this._queueEditDate      = '';
    this._queueSaving        = false;
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

    Logger.debug('[CTI] Connessione operatore: avvio');

    try {
      token = await this._fetchToken();
      await this._client.createSession(token);
      this._lastToken     = token;
      this._sessionActive = true;
      this._registerListeners();
      this._scheduleRefresh();
      this._startCallTimer();
      this._restoreContact();
      Logger.debug('[CTI] Connessione operatore: sessione WebRTC attiva');
    } catch (e) {
      this._sessionError  = e.message;
      this._sessionActive = false;
      Logger.error('[CTI] Connessione operatore: errore', { message: e.message });
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
    Logger.debug('[CTI] Fine chiamata: evento callHangup ricevuto', { callId });
    this._stopNetStatsPolling();
    callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
    targetNumber.set(null);
    if (this._autoDialerActive) {
      this._fetchAndCallDirect();
    }
  }

  /**
   * Handler callAnswered: il cliente ha risposto, conversazione attiva.
   *
   * @param {string} callId id della chiamata
   */
  _onCallAnswered(callId)
  {
    Logger.debug('[CTI] Conversazione: cliente risposto', { callId, customerNumber: this._callState.customerNumber });
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
    Logger.error('[CTI] Errore sessione WebRTC', { reason: String(reason) });
    this._sessionActive = false;
    this._sessionError  = String(reason);
    this._stopCallTimer();
    this._stopNetStatsPolling();
    callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
    targetNumber.set(null);
  }

  /**
   * Avvia il flusso dialer: recupera il prossimo contatto dalla coda e mostra il dialog di conferma.
   * Se la coda è vuota, mostra un messaggio di errore.
   */
  async _fetchAndShowContact()
  {
    let contact;

    this._sessionError = null;

    if (this._pendingContact) {
      this._showContactModal = true;
      return;
    }

    this._contactLoading = true;

    try {
      let result;
      result = await this._fetchNextContact();

      if (!result) {
        this._sessionError = 'Coda vuota: nessun contatto disponibile';
        this._contactLoading = false;
        return;
      }

      this._pendingContact   = result.contatto;
      this._pendingCodaId    = result.codaId;
      this._showContactModal = true;
      currentContact.set(result.contatto);
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

    return { contatto: data.out.contatto, codaId: data.out.codaId };
  }

  /**
   * Al reconnect, verifica se il backend ha un contatto corrente assegnato all'operatore
   * (rimasto in DB da un'estrazione precedente non confermata). Se presente, ripristina
   * {@code _pendingContact} e mostra il dialog di conferma senza riestrarre dalla coda.
   */
  async _restoreContact()
  {
    let response;
    let data;

    try {
      response = await fetch('/api/cti/vonage/queue/contact');
      data = await response.json();
      if (data.err) {
        Logger.warn('[CTI] Ripristino contatto: errore API', { log: data.log });
      } else if (data.out && data.out.contatto) {
        this._pendingContact   = data.out.contatto;
        this._pendingCodaId    = data.out.codaId;
        this._showContactModal = true;
        currentContact.set(data.out.contatto);
        Logger.debug('[CTI] Ripristino contatto: trovato', { codaId: data.out.codaId, phone: data.out.contatto.phone });
      } else {
        Logger.debug('[CTI] Ripristino contatto: nessun contatto in sospeso');
      }
    } catch (e) {
      Logger.error('[CTI] Ripristino contatto: errore rete', { message: e.message });
    }
  }

  /**
   * Toggle auto-dialer on/off.
   * L'auto-dialer chiama automaticamente il prossimo contatto dalla coda senza dialogo di conferma.
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
   * Avvia l'auto-dialer in modalità hands-free.
   * Estrae e chiama immediatamente il prossimo contatto senza mostrare il dialogo di conferma.
   */
  _startAutoDialer()
  {
    this._autoDialerActive = true;
    this._fetchAndCallDirect();
  }

  /**
   * Ferma l'auto-dialer e cancella l'eventuale timer di retry.
   */
  _stopAutoDialer()
  {
    this._autoDialerActive = false;
    if (this._autoDialerTimer) {
      clearTimeout(this._autoDialerTimer);
      this._autoDialerTimer = null;
    }
  }

  /**
   * Estrae il prossimo contatto dalla coda e avvia la chiamata direttamente, senza dialogo di conferma.
   * Se la coda è vuota, pianifica un nuovo tentativo dopo 30 secondi.
   * Usato dall'auto-dialer hands-free.
   */
  async _fetchAndCallDirect()
  {
    let contact;

    if (!this._autoDialerActive || !this._sessionActive || this._callState.active) {
      return;
    }

    this._sessionError   = null;
    this._contactLoading = true;

    try {
      let result;
      result = await this._fetchNextContact();
      if (!result) {
        this._autoDialerTimer = setTimeout(() => {
          this._autoDialerTimer = null;
          this._fetchAndCallDirect();
        }, 30 * 1000);
        this._contactLoading = false;
        return;
      }
      this._pendingContact = result.contatto;
      this._pendingCodaId  = result.codaId;
      currentContact.set(result.contatto);
      await this._confirmCall();
    } catch (e) {
      this._sessionError = 'Errore auto-dialer: ' + e.message;
    }

    this._contactLoading = false;
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
    let codaId;

    this._showContactModal = false;
    number = this._pendingContact ? this._pendingContact.phone : null;

    if (!number || number.trim() === '') {
      this._sessionError = 'Numero non disponibile per questo contatto';
      return;
    }

    contactId   = this._pendingContact ? (this._pendingContact.id       ?? null) : null;
    callbackUrl = this._pendingContact ? (this._pendingContact.callback ?? null) : null;
    codaId      = this._pendingCodaId;

    targetNumber.set(number.trim());
    this._sessionError = null;

    Logger.debug('[CTI] Avvio chiamata', { number: number.trim(), contactId, callbackUrl });

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
      this._pendingContact = null;
      this._pendingCodaId  = null;
      Logger.debug('[CTI] Avvio chiamata: in attesa risposta cliente', { callId, number: number.trim() });
      if (codaId) {
        fetch('/api/cti/vonage/queue/contatto/' + codaId, { method: 'DELETE' })
          .then(r => r.json())
          .then(r => { if (r.err) Logger.warn('[CTI] Rimozione contatto coda: errore API', { codaId, log: r.log }); })
          .catch(e => Logger.error('[CTI] Rimozione contatto coda: errore rete', { codaId, message: e.message }));
      }
    } catch (e) {
      this._sessionError = 'Errore avvio chiamata: ' + e.message;
      callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
      targetNumber.set(null);
      Logger.error('[CTI] Avvio chiamata: errore serverCall', { message: e.message });
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
    let label;

    label = item.key.replaceAll('_', ' ');

    if (item.type === 'text') {
      return html`
        <tr>
          <th class="text-secondary fw-normal w-25 align-top small">${label}</th>
          <td class="small" style="white-space:pre-wrap">${item.value}</td>
        </tr>`;
    }
    if (item.type === 'number') {
      return html`
        <tr>
          <th class="text-secondary fw-normal w-25 small">${label}</th>
          <td class="font-monospace">${item.value}</td>
        </tr>`;
    }
    return html`
      <tr>
        <th class="text-secondary fw-normal w-25 small">${label}</th>
        <td>${item.value}</td>
      </tr>`;
  }

  /** Chiude il dialog senza avviare la chiamata. Il contatto rimane in attesa per il prossimo tentativo. */
  _cancelCall()
  {
    this._showContactModal = false;
  }

  /**
   * Apre il dialog di pianificazione per il contatto corrente.
   * Pre-imposta la data/ora a domani alle 09:00.
   */
  _openScheduleModal()
  {
    let tomorrow;
    let pad;

    pad = n => String(n).padStart(2, '0');
    tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(9, 0, 0, 0);

    this._scheduleDateInput = tomorrow.getFullYear() + '-'
      + pad(tomorrow.getMonth() + 1) + '-'
      + pad(tomorrow.getDate()) + 'T'
      + pad(tomorrow.getHours()) + ':' + pad(tomorrow.getMinutes());

    this._showContactModal  = false;
    this._showScheduleModal = true;
  }

  /**
   * Conferma la pianificazione: aggiorna pianificato_al nel DB e passa al prossimo contatto.
   */
  async _confirmSchedule()
  {
    let response;
    let data;

    if (!this._scheduleDateInput || !this._pendingCodaId) {
      Logger.warn('[CTI] Pianificazione: dati mancanti', { codaId: this._pendingCodaId, date: this._scheduleDateInput });
      return;
    }

    Logger.debug('[CTI] Pianificazione contatto', { codaId: this._pendingCodaId, pianificatoAl: this._scheduleDateInput });

    try {
      response = await fetch('/api/cti/vonage/queue/contatto/' + this._pendingCodaId + '/pianifica', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pianificatoAl: this._scheduleDateInput + ':00' })
      });
      data = await response.json();
      if (data.err) {
        this._sessionError = data.log || 'Errore pianificazione';
        Logger.warn('[CTI] Pianificazione contatto: errore API', { codaId: this._pendingCodaId, log: data.log });
        return;
      }
      Logger.debug('[CTI] Pianificazione contatto: confermata', { codaId: this._pendingCodaId, pianificatoAl: this._scheduleDateInput });
      this._showScheduleModal = false;
      this._pendingContact    = null;
      this._pendingCodaId     = null;
    } catch (e) {
      this._sessionError = 'Errore pianificazione: ' + e.message;
      Logger.error('[CTI] Pianificazione contatto: errore rete', { codaId: this._pendingCodaId, message: e.message });
    }
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

    Logger.debug('[CTI] Riaggiancio operatore', { callId: currentCallId });

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
      targetNumber.set(null);
      Logger.debug('[CTI] Riaggiancio operatore: completato', { callId: currentCallId });
    } catch (e) {
      this._sessionError = 'Errore riagganciare: ' + e.message;
      Logger.error('[CTI] Riaggiancio operatore: errore', { callId: currentCallId, message: e.message });
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
   * Calcola la dimensione in px dell'icona volume/guadagno proporzionale al valore.
   * Range: 9px (valore 0) → 20px (valore 10).
   *
   * @param {number} value valore corrente (0–10)
   * @returns {number} dimensione in px
   */
  _iconSize(value)
  {
    return Math.round(9 + (value / 10) * 11);
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
        Logger.debug('[CTI] Refresh sessione: token rinnovato');
        this._scheduleRefresh();
      } catch (e) {
        this._sessionActive = false;
        this._sessionError  = 'Errore refresh sessione: ' + e.message;
        Logger.error('[CTI] Refresh sessione: errore', { message: e.message });
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
      Logger.debug('[CTI] Disconnessione operatore: rilascio sessione');
      try {
        await fetch('/api/cti/vonage/sdk/auth', { method: 'DELETE' });
      } catch (e) {
        Logger.error('[CTI] Disconnessione operatore: errore release operatore', { message: e.message });
        console.error('[CTI] Errore release operatore:', e);
      }
      if (this._lastToken) {
        try {
          await this._client.deleteSession(this._lastToken);
        } catch (e) {
          Logger.error('[CTI] Disconnessione operatore: errore deleteSession', { message: e.message });
          console.error('[CTI] Errore deleteSession:', e);
        }
      }
    }
    this._sessionActive = false;
    callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
    targetNumber.set(null);
    Logger.debug('[CTI] Disconnessione operatore: completata');
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
   * Apre la modal "Coda chiamate" e carica i contatti della coda personale.
   *
   * @param {Event} e evento click
   */
  async _openQueueModal(e)
  {
    e.stopPropagation();
    this._showMenu      = false;
    this._showQueueModal = true;
    await this._loadQueueItems();
  }

  /** Chiude la modal "Coda chiamate". */
  _closeQueueModal()
  {
    this._showQueueModal = false;
    this._queueEditId    = null;
    this._queueEditDate  = '';
    this._queueError     = null;
  }

  /** Carica i contatti della coda personale dell'operatore. */
  async _loadQueueItems()
  {
    let response;
    let data;

    this._queueLoading = true;
    this._queueError   = null;

    try {
      response = await fetch('/api/cti/vonage/queue/contatti');
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore caricamento coda');
      }
      this._queueItems = data.out || [];
    } catch (e) {
      this._queueError = e.message;
    }

    this._queueLoading = false;
  }

  /**
   * Avvia la modalità di ripianificazione per un contatto.
   * Pre-imposta la data al valore corrente di pianificatoAl o a domani alle 09:00.
   *
   * @param {{ id: number, pianificatoAl: string|null }} item elemento da ripianificare
   */
  _startReschedule(item)
  {
    let d;
    let pad;

    this._queueEditId = item.id;
    if (item.pianificatoAl) {
      this._queueEditDate = this._toDatetimeLocalValue(item.pianificatoAl);
    } else {
      pad = n => String(n).padStart(2, '0');
      d = new Date();
      d.setDate(d.getDate() + 1);
      d.setHours(9, 0, 0, 0);
      this._queueEditDate = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
                          + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }
  }

  /** Annulla la modalità di ripianificazione. */
  _cancelReschedule()
  {
    this._queueEditId   = null;
    this._queueEditDate = '';
  }

  /**
   * Salva la nuova data di pianificazione per un contatto.
   *
   * @param {number} id id del contatto nella coda personale
   */
  async _saveReschedule(id)
  {
    let response;
    let data;

    if (!this._queueEditDate) {
      return;
    }

    this._queueSaving = true;

    try {
      response = await fetch('/api/cti/vonage/queue/contatto/' + id + '/pianifica', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ pianificatoAl: this._queueEditDate + ':00' })
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore pianificazione');
      }
      this._queueEditId   = null;
      this._queueEditDate = '';
      await this._loadQueueItems();
    } catch (e) {
      this._queueError = 'Errore pianificazione: ' + e.message;
    }

    this._queueSaving = false;
  }

  /**
   * Rimette un contatto pianificato nella coda globale CTI.
   *
   * @param {number} id id del contatto nella coda personale
   */
  async _remettiInCoda(id)
  {
    let response;
    let data;

    try {
      response = await fetch('/api/cti/vonage/queue/contatto/' + id + '/rimetti', {
        method: 'DELETE'
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore rimessa in coda');
      }
      await this._loadQueueItems();
    } catch (e) {
      this._queueError = 'Errore rimessa in coda: ' + e.message;
    }
  }

  /**
   * Converte una stringa ISO datetime nel formato richiesto da input[type=datetime-local].
   *
   * @param {string} isoStr stringa ISO 8601 (es. "2026-04-15T09:30:00")
   * @returns {string} stringa nel formato "YYYY-MM-DDTHH:MM" o '' se non valida
   */
  _toDatetimeLocalValue(isoStr)
  {
    let d;
    let pad;

    pad = n => String(n).padStart(2, '0');
    d = new Date(isoStr);
    if (isNaN(d.getTime())) {
      return '';
    }
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
         + 'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
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
          <span class="cti-vol-icon-wrap">
            <i class="bi bi-volume-up-fill text-secondary" style="font-size:${this._iconSize(this._outputVolume)}px"></i>
          </span>
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
          <span class="cti-vol-icon-wrap">
            <i class="bi bi-mic text-secondary" style="font-size:${this._iconSize(this._inputGain)}px"></i>
          </span>
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
              <button class="dropdown-item" @click=${this._openQueueModal.bind(this)}><i class="bi bi-list-task"></i>&nbsp; Coda chiamate</button>
              <button class="dropdown-item" @click=${this._openCallLog.bind(this)}><i class="bi bi-clock-history"></i>&nbsp; Storico chiamate</button>
            </div>
          ` : ''}
        </div>

      </div>

      <!-- Dialog conferma chiamata -->
      ${this._showContactModal && this._pendingContact ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1" @click=${this._cancelCall.bind(this)}>
          <div class="modal-dialog modal-lg" @click=${e => e.stopPropagation()}>
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
                <button type="button" class="btn btn-secondary" @click=${this._openScheduleModal.bind(this)}>Pianifica...</button>
                <button type="button" class="btn btn-success" @click=${this._confirmCall.bind(this)}><i class="bi bi-telephone-fill"></i>&nbsp; Chiama</button>
              </div>
            </div>
          </div>
        </div>
      ` : ''}

      <!-- Dialog pianificazione richiamata -->
      ${this._showScheduleModal ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1">
          <div class="modal-dialog modal-sm">
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-calendar-event"></i>&nbsp; Pianifica richiamata</h5>
                <button type="button" class="btn-close"
                  @click=${() => { this._showScheduleModal = false; this._showContactModal = true; }}
                  aria-label="Annulla"></button>
              </div>
              <div class="modal-body">
                <label class="form-label text-secondary small mb-1">Data e ora richiamata</label>
                <input type="datetime-local" class="form-control"
                  .value=${this._scheduleDateInput}
                  @change=${e => { this._scheduleDateInput = e.target.value; }}>
              </div>
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary"
                  @click=${() => { this._showScheduleModal = false; this._showContactModal = true; }}>Annulla</button>
                <button type="button" class="btn btn-primary" @click=${this._confirmSchedule.bind(this)}>
                  <i class="bi bi-calendar-check"></i>&nbsp; Conferma
                </button>
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

      <!-- Modal coda chiamate -->
      ${this._showQueueModal ? html`
        <div class="modal-backdrop fade show"></div>
        <div class="modal d-block" tabindex="-1" @click=${this._closeQueueModal.bind(this)}>
          <div class="modal-dialog modal-lg modal-dialog-scrollable" @click=${e => e.stopPropagation()}>
            <div class="modal-content">
              <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-list-task"></i>&nbsp; Coda chiamate</h5>
                <button type="button" class="btn-close" @click=${this._closeQueueModal.bind(this)} aria-label="Chiudi"></button>
              </div>
              <div class="modal-body">
                ${this._queueLoading
                  ? html`<div class="text-center text-secondary py-4">Caricamento...</div>`
                  : this._queueError
                    ? html`<div class="alert alert-danger py-2 small">${this._queueError}</div>`
                    : html`
                      ${(() => {
                        const next = this._queueItems.find(i => i.disponibile);
                        if (!next) {
                          return html``;
                        }
                        return html`
                          <h6 class="text-secondary fw-semibold mb-2 small text-uppercase"><i class="bi bi-telephone"></i>&nbsp; Prossima chiamata</h6>
                          <div class="border rounded p-2 mb-3 small d-flex align-items-baseline gap-2 flex-wrap">
                            <span class="font-monospace fw-semibold">${next.contatto?.phone || '—'}</span>
                            ${(next.contatto?.data || []).slice(0, 4).map(d => html`
                              <span class="text-secondary">${d.key.replaceAll('_', ' ')}: <span class="text-body">${d.value}</span></span>
                            `)}
                          </div>
                        `;
                      })()}
                      <h6 class="text-secondary fw-semibold mb-2 small text-uppercase"><i class="bi bi-calendar-event"></i>&nbsp; Pianificati</h6>
                      ${(() => {
                        const planned = this._queueItems.filter(i => !i.disponibile);
                        if (planned.length === 0) {
                          return html`<p class="text-secondary small">Nessuna chiamata pianificata.</p>`;
                        }
                        return html`
                          <table class="table table-sm mb-0">
                            <thead class="table-light">
                              <tr>
                                <th class="small">Telefono</th>
                                <th class="small">Dati</th>
                                <th class="small">Pianificato al</th>
                                <th></th>
                              </tr>
                            </thead>
                            <tbody>
                              ${planned.map(item => html`
                                <tr>
                                  <td class="font-monospace small align-middle">${item.contatto?.phone || '—'}</td>
                                  <td class="small text-secondary align-middle">
                                    ${(item.contatto?.data || []).slice(0, 2).map(d => html`
                                      <div>${d.key.replaceAll('_', ' ')}: ${d.value}</div>
                                    `)}
                                  </td>
                                  <td class="small align-middle">
                                    ${this._queueEditId === item.id
                                      ? html`
                                        <input type="datetime-local" class="form-control form-control-sm"
                                          .value=${this._queueEditDate}
                                          @change=${e => { this._queueEditDate = e.target.value; }}>
                                      `
                                      : this._formatCallDate(item.pianificatoAl)
                                    }
                                  </td>
                                  <td class="text-end align-middle" style="white-space:nowrap">
                                    ${this._queueEditId === item.id
                                      ? html`
                                        <button class="btn btn-sm btn-primary me-1"
                                          ?disabled=${this._queueSaving}
                                          @click=${() => this._saveReschedule(item.id)}
                                          title="Salva">
                                          <i class="bi bi-check-lg"></i>
                                        </button>
                                        <button class="btn btn-sm btn-secondary"
                                          @click=${this._cancelReschedule.bind(this)}
                                          title="Annulla">
                                          <i class="bi bi-x-lg"></i>
                                        </button>
                                      `
                                      : html`
                                        <button class="btn btn-sm btn-outline-secondary me-1"
                                          @click=${() => this._startReschedule(item)}
                                          title="Ripianifica">
                                          <i class="bi bi-calendar-event"></i>
                                        </button>
                                        <button class="btn btn-sm btn-outline-warning"
                                          @click=${() => this._remettiInCoda(item.id)}
                                          title="Rimetti in coda globale">
                                          <i class="bi bi-arrow-return-left"></i>
                                        </button>
                                      `
                                    }
                                  </td>
                                </tr>
                              `)}
                            </tbody>
                          </table>
                        `;
                      })()}
                    `
                }
              </div>
              <div class="modal-footer py-2">
                <button type="button" class="btn btn-sm btn-outline-secondary me-auto"
                  @click=${this._loadQueueItems.bind(this)}
                  ?disabled=${this._queueLoading}>
                  <i class="bi bi-arrow-clockwise"></i>&nbsp; Aggiorna
                </button>
                <button type="button" class="btn btn-secondary btn-sm" @click=${this._closeQueueModal.bind(this)}>Chiudi</button>
              </div>
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
