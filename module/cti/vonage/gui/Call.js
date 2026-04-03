/**
 * CTI Bar — barra CTI fissa sul fondo della finestra.
 *
 * Implementa il flusso operator-first progressive dialer con UI a barra fissa.
 * Importabile da qualunque modulo che richieda funzionalità CTI.
 *
 * Utilizzo dal modulo ospitante (es. CRM):
 *   import '<path>/module/cti/vonage/Call.js';
 *   import { targetNumber } from '<path>/module/cti/vonage/store.js';
 *   // Imposta il numero prima che l'operatore prema Chiama:
 *   targetNumber.set('12345678901');
 *   // Aggiungi al template:
 *   <cti-bar></cti-bar>
 *
 * Flusso:
 *   FASE 1 — Connessione: POST /api/cti/vonage/sdk/auth → assegna operatore e JWT SDK.
 *   FASE 2 — Chiamata: client.serverCall() → webhook answer → cliente chiamato in background.
 *   FASE 3 — Conversazione: callAnswered → timer e stats attivi.
 *   FASE 4 — Fine: callHangup o PUT /call/{uuid}/hangup → stato idle.
 *   FASE 5 — Disconnessione: DELETE /api/cti/vonage/sdk/auth → operatore rilasciato.
 */
import { LitElement, html } from 'lit';
import { VonageClient } from '@vonage/client-sdk';
import { callState, targetNumber } from './store.js';

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
    _iceState:      { state: true },
    _netStats:      { state: true },
    _callSeconds:   { state: true }
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
    this._callSeconds   = 0;
    this._client        = null;
    this._lastToken     = null;
    this._refreshTimer  = null;
    this._callTimer     = null;
    this._netStatsTimer = null;
    this._unsubCall     = null;
    this._unsubTarget   = null;
    this._gainNode      = null;
  }

  connectedCallback()
  {
    super.connectedCallback();
    this._unsubCall   = callState.subscribe(v => { this._callState = v; });
    this._unsubTarget = targetNumber.subscribe(v => { this._targetNumber = v; });
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
    this._teardown();
  }

  /**
   * Crea la sessione WebRTC con Vonage.
   * Richiede un operatore libero al backend e inizializza il Vonage Client SDK.
   */
  async _connect()
  {
    let token;

    this._sessionError = null;

    try {
      token = await this._fetchToken();
      await this._client.createSession(token);
      this._lastToken     = token;
      this._sessionActive = true;
      this._registerListeners();
      this._scheduleRefresh();
    } catch (e) {
      this._sessionError  = e.message;
      this._sessionActive = false;
    }
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
    this._stopCallTimer();
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
    this._startCallTimer();
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
  }

  /**
   * Avvia il flusso operator-first.
   * Legge il numero dal targetNumber store (impostato dal modulo ospitante).
   * client.serverCall() trigera il webhook /api/cti/vonage/answer.
   */
  async _startCall()
  {
    let callId;

    if (!this._targetNumber || this._targetNumber.trim() === '') {
      this._sessionError = 'Numero cliente non impostato';
      return;
    }

    this._sessionError = null;

    try {
      callId = await this._client.serverCall({ customerNumber: this._targetNumber.trim() });
      callState.set({
        active: true,
        callId: callId,
        customerNumber: this._targetNumber.trim(),
        status: 'waiting_customer'
      });
    } catch (e) {
      this._sessionError = 'Errore avvio chiamata: ' + e.message;
      callState.set({ active: false, callId: null, customerNumber: null, status: 'idle' });
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

    try {
      response = await fetch('/api/cti/vonage/call/' + currentCallId + '/hangup', {
        method: 'PUT'
      });
      data = await response.json();
      if (data.err) {
        throw new Error(data.log || 'Errore hangup');
      }
      this._stopCallTimer();
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
    const cs = this._callState;

    return html`
      <div style="
        position: fixed;
        bottom: 0;
        left: 0;
        right: 0;
        z-index: 1050;
        background: #1a1a2e;
        color: #e2e8f0;
        padding: 5px 16px;
        display: flex;
        align-items: center;
        gap: 8px;
        box-shadow: 0 -2px 8px rgba(0,0,0,0.4);
        font-size: 13px;
        user-select: none;
      ">

        ${!this._sessionActive
          ? html`
              <button
                class="btn btn-sm btn-outline-light"
                style="min-width: 90px;"
                @click=${this._connect.bind(this)}
              >Connetti</button>
            `
          : html`
              <button
                class="btn btn-sm btn-outline-secondary"
                style="min-width: 90px; font-family: monospace; letter-spacing: 1px;"
                @click=${this._disconnect.bind(this)}
                title="Disconnetti"
              >${cs.active ? this._formatTime(this._callSeconds) : '00:00:00'}</button>
            `
        }

        <div style="width: 1px; height: 24px; background: #334155;"></div>

        ${!cs.active
          ? html`
              <button
                class="btn btn-sm btn-success"
                style="min-width: 36px;"
                @click=${this._startCall.bind(this)}
                title="Chiama ${this._targetNumber}"
                ?disabled=${!this._sessionActive || !this._targetNumber}
              >&#9742;</button>
            `
          : html`
              <button
                class="btn btn-sm btn-danger"
                style="min-width: 36px;"
                @click=${this._hangup.bind(this)}
                title="Riaggancia"
              >&#9587;</button>
            `
        }

        <span style="
          min-width: 130px;
          font-size: 12px;
          color: ${cs.active ? '#86efac' : '#64748b'};
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        ">
          ${cs.active
            ? (cs.status === 'waiting_customer'
                ? 'In attesa… ' + cs.customerNumber
                : cs.customerNumber)
            : (this._targetNumber || '—')
          }
        </span>

        <div style="width: 1px; height: 24px; background: #334155;"></div>

        <button
          class="btn btn-sm ${this._muted ? 'btn-warning' : 'btn-outline-light'}"
          style="min-width: 36px;"
          @click=${this._toggleMute.bind(this)}
          title="${this._muted ? 'Riattiva microfono' : 'Silenzia microfono'}"
          ?disabled=${!this._sessionActive}
        >${this._muted ? '🔇' : '🎤'}</button>

        <div style="width: 1px; height: 24px; background: #334155;"></div>

        <span style="color: #64748b; font-size: 12px;" title="Volume cuffie">&#127911;</span>
        <button
          class="btn btn-sm btn-outline-light"
          style="padding: 2px 8px; line-height: 1;"
          @click=${() => this._adjustOutputVolume(-1)}
          title="Volume −"
          ?disabled=${this._outputVolume <= 0}
        >−</button>
        <span style="min-width: 16px; text-align: center; font-size: 12px; color: #94a3b8;">
          ${this._outputVolume}
        </span>
        <button
          class="btn btn-sm btn-outline-light"
          style="padding: 2px 8px; line-height: 1;"
          @click=${() => this._adjustOutputVolume(1)}
          title="Volume +"
          ?disabled=${this._outputVolume >= 10}
        >+</button>

        <div style="width: 1px; height: 24px; background: #334155;"></div>

        <span style="color: #64748b; font-size: 12px;" title="Guadagno microfono">&#127908;</span>
        <button
          class="btn btn-sm btn-outline-light"
          style="padding: 2px 8px; line-height: 1;"
          @click=${() => this._adjustInputGain(-1)}
          title="Mic −"
          ?disabled=${this._inputGain <= 0}
        >−</button>
        <span style="min-width: 16px; text-align: center; font-size: 12px; color: #94a3b8;">
          ${this._inputGain}
        </span>
        <button
          class="btn btn-sm btn-outline-light"
          style="padding: 2px 8px; line-height: 1;"
          @click=${() => this._adjustInputGain(1)}
          title="Mic +"
          ?disabled=${this._inputGain >= 10}
        >+</button>

        <div style="width: 1px; height: 24px; background: #334155;"></div>

        <div style="position: relative;">
          <button
            class="btn btn-sm ${this._showDtmf ? 'btn-info' : 'btn-outline-light'}"
            style="min-width: 36px; font-family: monospace;"
            @click=${() => { this._showDtmf = !this._showDtmf; }}
            title="Tastiera DTMF"
            ?disabled=${!cs.active}
          >#</button>
          ${this._showDtmf ? html`
            <div style="
              position: absolute;
              bottom: 40px;
              left: 0;
              background: #0f172a;
              border: 1px solid #334155;
              border-radius: 6px;
              padding: 8px;
              display: grid;
              grid-template-columns: repeat(3, 36px);
              gap: 4px;
              z-index: 1100;
              box-shadow: 0 -4px 12px rgba(0,0,0,0.5);
            ">
              ${['1','2','3','4','5','6','7','8','9','*','0','#'].map(d => html`
                <button
                  class="btn btn-sm btn-outline-light"
                  style="padding: 5px 2px; font-family: monospace; font-size: 13px;"
                  @click=${() => this._sendDtmf(d)}
                >${d}</button>
              `)}
            </div>
          ` : ''}
        </div>

        <div style="width: 1px; height: 24px; background: #334155;"></div>

        <div style="display: flex; align-items: center; gap: 5px; font-size: 11px; color: #64748b;">
          ${this._netStats.latency !== null
            ? html`<span title="Latenza RTT">${this._netStats.latency}ms</span>`
            : ''
          }
          ${this._netStats.packetLoss !== null
            ? html`<span title="Packet loss">${this._netStats.packetLoss}%&#9660;</span>`
            : ''
          }
          <span
            style="
              width: 8px;
              height: 8px;
              border-radius: 50%;
              background: ${this._sessionActive ? this._iceDotColor() : '#475569'};
              display: inline-block;
              flex-shrink: 0;
            "
            title="Connessione: ${this._iceState}"
          ></span>
        </div>

        ${this._sessionError ? html`
          <span
            style="
              color: #f87171;
              font-size: 11px;
              max-width: 200px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            "
            title="${this._sessionError}"
          >${this._sessionError}</span>
        ` : ''}

        <div style="flex: 1;"></div>

        <button
          class="btn btn-sm btn-outline-secondary"
          style="min-width: 28px; opacity: 0.5;"
          disabled
          title="Menu contestuale (non disponibile)"
        >&#8942;</button>

      </div>
    `;
  }
}

customElements.define('cti-bar', Bar);
