<script>
  /**
   * Componente principale della sessione WebRTC per voice3.
   *
   * Implementa il flusso completo operator-first progressive dialer:
   *
   *   FASE 1 - Connessione operatore:
   *     - Recupera il JWT SDK dal backend (/api/voice3/sdk-token)
   *     - Crea la sessione WebRTC tramite VonageClient.createSession()
   *     - Pianifica il refresh automatico del token ogni 13 minuti (scadenza 1 ora)
   *
   *   FASE 2 - Avvio chiamata (quando operatore è pronto):
   *     - Chiama /api/voice3/prepare-call per registrare il numero cliente sul backend
   *     - Esegue client.serverCall() che triggera il webhook /api/voice3/answer su Vonage
   *     - Il backend risponde immediatamente con NCCO: operatore entra in conversazione
   *       con musica di attesa (startOnEnter: false, musicOnHold)
   *     - In background (delay 1s): il backend chiama il cliente tramite API Vonage
   *
   *   FASE 3 - Chiamata attiva:
   *     - Il cliente risponde → conversazione si connette (startOnEnter: true nel NCCO cliente)
   *     - Lo stato passa da 'waiting_customer' a 'connected'
   *     - L'operatore può riagganciare tramite /api/voice3/calls/{uuid}/hangup
   *
   *   FASE 4 - Fine chiamata:
   *     - callHangup event → stato torna a 'idle'
   *     - teardown() pulisce timer e sessione WebRTC
   */
  import { onMount, onDestroy } from 'svelte'
  import { VonageClient } from '@vonage/client-sdk'
  import { callState } from './store.js'

  // userId dell'operatore passato dal Layout padre
  const { userId } = $props()

  // Stato reattivo: true quando la sessione WebRTC è attiva
  let sessionActive = $state(false)

  // Messaggio di errore visualizzato all'operatore
  let error = $state(null)

  // Numero cliente da chiamare (inserito dall'operatore)
  let customerNumber = $state('')

  // Istanza del Vonage Client SDK
  let client

  // Ultimo token JWT utilizzato (necessario per deleteSession al teardown)
  let lastToken

  // Timer per il refresh automatico del token SDK
  let refreshTimer

  // Intervallo di refresh: 13 minuti (il token scade dopo 1 ora)
  const REFRESH_DELAY_MS = 13 * 60 * 1000

  onMount(() => {
    // Inizializza il client SDK e avvia subito la connessione
    client = new VonageClient()
    connect()
  })

  onDestroy(() => {
    // Pulizia: cancella timer e chiude la sessione WebRTC
    teardown()
  })

  /**
   * Crea la sessione WebRTC con Vonage.
   * Recupera il JWT dal backend, lo passa al VonageClient,
   * registra i listener sugli eventi e pianifica il refresh.
   */
  async function connect() {
    let token
    let id

    error = null

    try {
      // Recupera il JWT SDK dal backend (firmato con la private key Vonage)
      token = await fetchToken()

      // Crea la sessione WebRTC: il client si autentica su Vonage
      id = await client.createSession(token)

      lastToken = token
      sessionActive = true

      // Registra i listener sugli eventi Vonage (hangup, errori sessione)
      registerListeners()

      // Pianifica il rinnovo automatico del token prima della scadenza
      scheduleRefresh()
    } catch (e) {
      error = e.message
      sessionActive = false
    }
  }

  /**
   * Recupera il JWT SDK dal backend.
   * Il token è firmato RS256 con la private key Vonage e include
   * i claims necessari per il Client SDK (acl, sub, application_id).
   */
  async function fetchToken() {
    let response
    let data

    response = await fetch(`/api/voice3/sdk-token?userId=${userId}`)

    if (!response.ok) {
      throw new Error(`Token fetch failed: ${response.status}`)
    }

    data = await response.json()
    return data.out.token
  }

  /**
   * Registra i listener sugli eventi del Vonage Client SDK.
   *   callHangup: la chiamata è terminata (operatore o cliente ha riagganciato)
   *   sessionError: errore della sessione WebRTC
   */
  function registerListeners() {
    client.on('callHangup', onCallHangup)
    client.on('sessionError', onSessionError)
  }

  /**
   * Handler callHangup: resetta lo stato della chiamata a 'idle'.
   * Viene invocato quando qualsiasi leg della chiamata viene chiuso.
   */
  function onCallHangup(callId, callQuality, reason) {
    callState.set({
      active: false,
      callId: null,
      customerNumber: null,
      conversationName: null,
      status: 'idle'
    })
  }

  /**
   * Handler sessionError: la sessione WebRTC si è chiusa inaspettatamente.
   * Mostra il messaggio di errore e disattiva la sessione.
   */
  function onSessionError(reason) {
    sessionActive = false
    error = String(reason)
  }

  /**
   * Avvia il flusso operator-first:
   *
   *   Step 1: POST /api/voice3/prepare-call
   *     Registra il numero cliente nel backend (mappa in-memory userId → customerNumber).
   *     Necessario PRIMA di serverCall() perché il webhook answer non ha accesso
   *     al numero cliente se non è stato precedentemente registrato.
   *
   *   Step 2: client.serverCall()
   *     Triggera il webhook GET/POST /api/voice3/answer su Vonage.
   *     Il backend risponde con NCCO: operatore entra in conversazione con musica di attesa.
   *     In background (delay 1s): il backend chiama il cliente tramite API Vonage.
   */
  async function startCall() {
    let callId
    let prepareResponse

    if (!customerNumber || customerNumber.trim() === '') {
      error = 'Inserisci numero cliente'
      return
    }

    error = null

    console.log('=== VOICE3 START CALL ===')
    console.log('Customer Number:', customerNumber.trim())
    console.log('Operator userId:', userId)

    try {
      // Step 1: registra il numero cliente nel backend prima di avviare serverCall()
      console.log('Step 1: Chiamata prepare-call...')
      prepareResponse = await fetch('/api/voice3/prepare-call', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userId,
          customerNumber: customerNumber.trim()
        })
      })

      if (!prepareResponse.ok) {
        throw new Error('Errore prepare-call: ' + prepareResponse.status)
      }

      console.log('prepare-call completato')

      // Step 2: serverCall() triggera il webhook /api/voice3/answer
      // Il backend risponde a Vonage con NCCO: operatore in conversazione con musica di attesa
      // Contemporaneamente (async, delay 1s): il backend chiama il cliente
      console.log('Step 2: Esecuzione client.serverCall()...')
      callId = await client.serverCall({})
      console.log('serverCall() completato, callId:', callId)

      // Aggiorna lo stato: operatore in attesa che il cliente risponda
      callState.set({
        active: true,
        callId: callId,
        customerNumber: customerNumber.trim(),
        conversationName: null,
        status: 'waiting_customer'
      })

      console.log('Stato chiamata aggiornato: in attesa del cliente...')

    } catch (e) {
      console.error('ERRORE in startCall():', e)
      error = 'Errore avvio chiamata: ' + e.message
      callState.set({
        active: false,
        callId: null,
        customerNumber: null,
        conversationName: null,
        status: 'idle'
      })
    }
  }

  /**
   * Riaggancia la chiamata corrente.
   * Chiama PUT /api/voice3/calls/{uuid}/hangup che termina
   * sia il leg dell'operatore che quello del cliente.
   */
  async function hangupCall() {
    let response

    try {
      response = await fetch(`/api/voice3/calls/${$callState.callId}/hangup`, {
        method: 'PUT'
      })

      if (!response.ok) {
        throw new Error(`Riagganciare fallito: ${response.status}`)
      }

      // Resetta lo stato: la chiamata è terminata
      callState.set({
        active: false,
        callId: null,
        customerNumber: null,
        conversationName: null,
        status: 'idle'
      })
    } catch (e) {
      error = 'Errore riagganciare: ' + e.message
    }
  }

  /**
   * Pianifica il refresh automatico del token JWT SDK.
   * Il token scade dopo 1 ora; il refresh avviene ogni 13 minuti
   * per garantire continuità della sessione senza interruzioni.
   */
  async function scheduleRefresh() {
    refreshTimer = setTimeout(async () => {
      let token

      try {
        token = await fetchToken()
        await client.refreshSession(token)
        lastToken = token
        // Ri-pianifica il prossimo refresh
        scheduleRefresh()
      } catch (e) {
        sessionActive = false
        error = 'Errore refresh sessione: ' + e.message
      }
    }, REFRESH_DELAY_MS)
  }

  /**
   * Pulizia completa: cancella il timer di refresh e chiude la sessione WebRTC.
   * Chiamato in onDestroy o quando l'operatore si disconnette manualmente.
   */
  async function teardown() {
    // Cancella il timer di refresh per evitare memory leak
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }

    // Chiude la sessione WebRTC su Vonage
    if (sessionActive && lastToken) {
      try {
        await client.deleteSession(lastToken)
      } catch (e) {
        console.error('Errore deleteSession:', e)
      }
    }

    // Resetta lo stato locale
    sessionActive = false
    callState.set({
      active: false,
      callId: null,
      customerNumber: null,
      conversationName: null,
      status: 'idle'
    })
  }

  // Disconnessione manuale da parte dell'operatore
  async function disconnect() {
    await teardown()
  }
</script>

<div class="sdk-session">
  <h3>Sessione Operatore WebRTC (Progressive Dialer v3)</h3>

  <!-- Stato: sessione non attiva (disconnessa o errore) -->
  {#if !sessionActive}
    <div class="status disconnected">
      <p>Sessione non attiva</p>
      {#if error}
        <p class="error">{error}</p>
      {/if}
      <button onclick={connect}>Connetti</button>
    </div>

  <!-- Stato: sessione WebRTC attiva -->
  {:else}
    <div class="status connected">
      <p>✓ Sessione attiva - Operatore: <strong>{userId}</strong></p>
      <button class="btn-secondary" onclick={disconnect}>Disconnetti</button>
    </div>

    <!-- Nessuna chiamata in corso: mostra form per avviare una chiamata -->
    {#if !$callState.active}
      <div class="call-controls">
        <label>
          Numero Cliente:
          <input
            type="text"
            bind:value={customerNumber}
            placeholder="+39XXXXXXXXXX"
            disabled={$callState.active}
          />
        </label>
        <button class="btn-primary" onclick={startCall}>
          Pronto per Chiamata
        </button>
        {#if error}
          <p class="error">{error}</p>
        {/if}
      </div>

    <!-- Chiamata in corso: mostra stato e pulsante riagganciare -->
    {:else}
      <div class="active-call">
        {#if $callState.status === 'waiting_customer'}
          <!-- Operatore in attesa con musica: il backend sta chiamando il cliente -->
          <p class="call-info">
            🎵 In attesa cliente <strong>{$callState.customerNumber}</strong>...
          </p>
          <p class="call-status">Il sistema sta contattando il cliente...</p>
        {:else if $callState.status === 'connected'}
          <!-- Chiamata connessa: cliente e operatore in conversazione -->
          <p class="call-info">
            📞 In chiamata con <strong>{$callState.customerNumber}</strong>
          </p>
        {/if}
        <button class="btn-hangup" onclick={hangupCall}>Riaggancia</button>
      </div>
    {/if}
  {/if}
</div>

<style>
  .sdk-session {
    padding: 1.5rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    background: #f9f9f9;
  }

  h3 {
    margin: 0 0 1rem 0;
    font-size: 1.1rem;
  }

  .status {
    padding: 1rem;
    border-radius: 4px;
    margin-bottom: 1rem;
  }

  .disconnected {
    background: #fff3cd;
    border: 1px solid #ffc107;
  }

  .connected {
    background: #d1ecf1;
    border: 1px solid #17a2b8;
  }

  .call-controls {
    padding: 1rem;
    background: white;
    border-radius: 4px;
    border: 1px solid #ddd;
  }

  .call-controls label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
  }

  .call-controls input {
    width: 100%;
    padding: 0.5rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    margin-bottom: 1rem;
  }

  .active-call {
    padding: 1rem;
    background: #d4edda;
    border: 1px solid #28a745;
    border-radius: 4px;
  }

  .call-info {
    margin: 0 0 0.5rem 0;
    font-size: 1.1rem;
  }

  .call-status {
    margin: 0 0 1rem 0;
    color: #666;
    font-size: 0.9rem;
  }

  button {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 0.9rem;
  }

  .btn-primary {
    background: #28a745;
    color: white;
  }

  .btn-primary:hover {
    background: #1e7e34;
  }

  .btn-secondary {
    background: #6c757d;
    color: white;
  }

  .btn-secondary:hover {
    background: #545b62;
  }

  .btn-hangup {
    background: #dc3545;
    color: white;
  }

  .btn-hangup:hover {
    background: #c82333;
  }

  .error {
    color: #dc3545;
    margin: 0.5rem 0;
  }

  input:disabled {
    background: #e9ecef;
  }
</style>
