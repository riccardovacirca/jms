<script>
  import { onMount, onDestroy } from 'svelte'
  import { VonageClient } from '@vonage/client-sdk'

  // ─── props ──────────────────────────────────────────────────────────────
  // userId: nome dell'utente registrato su Vonage. Finisce nel claim "sub"
  // del JWT SDK generato dal backend. Corrisponde all'operatorId usato in
  // POST /api/voice/calls quando operatorType è "app".
  const { userId } = $props()

  // ─── stato reattivo (pilotano il template) ──────────────────────────────
  let sessionActive = $state(false)   // sessione WebRTC stabilita
  let incomingCall  = $state(null)    // { callId, from, channelType } | null
  let error         = $state(null)    // ultimo messaggio di errore

  // ─── stato interno (non reattivo) ───────────────────────────────────────
  let client        // istanza VonageClient, creata in step 1
  let lastToken     // ultimo JWT ricevuto — serve a deleteSession in step 5
  let refreshTimer  // handle del setTimeout programmato in step 4

  // ─── costanti ────────────────────────────────────────────────────────────
  // TTL sessione: 15 min (ref: docs/vonage/client_sdk/en/doc4.md).
  // Il refresh viene anticipato di 2 min per dare margine alla latenza
  // della richiesta fetchToken → refreshSession.
  const REFRESH_DELAY_MS = 13 * 60 * 1000

  // ─── lifecycle ───────────────────────────────────────────────────────────

  // step 1 — punto di ingresso del componente.
  // Il client viene solo istanziato qui; nessuna connessione ancora aperta.
  // La sessione viene stabilita in connect() (step 2).
  onMount(() => {
    client = new VonageClient()
    connect()
  })

  // step 5 — il componente esce dal DOM.
  // Tutto il contesto della sessione deve essere liberato → teardown().
  onDestroy(() => {
    teardown()
  })

  // ─── connessione ─────────────────────────────────────────────────────────

  // step 2 — recupera il JWT e apre la sessione WebRTC.
  // Da qui in poi il browser è "online" e può ricevere chiamate.
  // Successivo: registra i listener degli eventi (step 3),
  //             programma il refresh periodico (step 4).
  async function connect() {
    let token
    let id

    error = null

    try {
      token = await fetchToken()                    // step 2a — JWT dal backend
      id    = await client.createSession(token)     // step 2b — WebRTC attivo

      lastToken     = token
      sessionActive = true

      registerListeners()   // step 3
      scheduleRefresh()     // step 4
    } catch (e) {
      error         = e.message
      sessionActive = false
    }
  }

  // Recupera un JWT fresco dal backend.
  // Chiamato da: connect() durante step 2a, e dal callback di
  // scheduleRefresh() durante step 4. Stesso endpoint in entrambi i casi.
  async function fetchToken() {
    let response
    let data

    response = await fetch(`/api/voice/sdk-token?userId=${userId}`)

    if (!response.ok) {
      throw new Error(`Recupero token fallito: ${response.status}`)
    }

    data = await response.json()

    return data.out.token
  }

  // ─── eventi ──────────────────────────────────────────────────────────────

  // step 3 — registra i listener sulla sessione appena creata.
  // Tutti gli eventi che Vonage può generare verso questa sessione vengono
  // catturati qui e indirizzati ai rispettivi handler sotto.
  function registerListeners() {
    // Chiamata in ingresso: il CRM ha avviato una chiamata con
    // operatorType="app" e operatorId=<questo userId>.
    // Il ponte audio non è ancora attivo — serve answer() oppure reject().
    client.on('callInvite', onCallInvite)

    // La chiamata in arrivo è stata annullata prima che l'operatore
    // rispondesse. Possibili cause: timeout Vonage (default 60s sul NCCO
    // connect), reject da altra sessione dello stesso utente, cancel
    // esplicito dal chiamatore.
    client.on('callInviteCancel', onCallInviteCancel)

    // Evento terminale della chiamata: il ponte audio è stato chiuso.
    // Arriva sia dopo un hangup sia in alcuni casi di errore media.
    client.on('callHangup', onCallHangup)

    // Errore sulla sessione dopo la creazione iniziale.
    // La sessione potrebbe essere stata invalidata sul lato Vonage.
    client.on('sessionError', onSessionError)
  }

  // Chiamato da: listener callInvite (step 3).
  // Auto-accetta la chiamata immediatamente senza mostrare notifica
  async function onCallInvite(callId, from, channelType) {
    try {
      await client.answer(callId)
      incomingCall = { callId, from, channelType }
    } catch (e) {
      error = 'Errore auto-accept: ' + e.message
    }
  }

  // Chiamato da: listener callInviteCancel (step 3).
  // La chiamata è stata ritirata prima di una risposta da parte sua.
  // Resetta solo se il callId corrisponde a quello corrente.
  function onCallInviteCancel(callId, reason) {
    if (incomingCall && incomingCall.callId === callId) {
      incomingCall = null
    }
  }

  // Chiamato da: listener callHangup (step 3).
  // Stato terminale — il pannello chiamata viene nascosto.
  // Idempotente: se incomingCall è già null (es. dopo reject), è un no-op.
  function onCallHangup(callId, callQuality, reason) {
    incomingCall = null
  }

  // Chiamato da: listener sessionError (step 3).
  // La sessione non è più affidabile. L'operatore può riconnettersi
  // premendo il pulsante "Riprova" nel template, che chiama connect().
  function onSessionError(reason) {
    sessionActive = false
    error         = String(reason)
  }

  // ─── azioni operatore ────────────────────────────────────────────────────

  // Pulsante "Accetta" nel template.
  // Precondizione: incomingCall è stato impostato da onCallInvite.
  // Dopo answer() il ponte audio è attivo: Vonage connette direttamente
  // il cliente a questo operatore. La chiamata resta aperta fino a
  // callHangup (step 3).
  async function acceptCall() {
    try {
      await client.answer(incomingCall.callId)
    } catch (e) {
      error = e.message
    }
  }

  // Pulsante "Rifiuta" nel template.
  // Resetta incomingCall nel finally: se Vonage invia comunque un
  // callHangup successivo, onCallHangup lo gestisce come no-op.
  async function rejectCall() {
    try {
      await client.reject(incomingCall.callId)
    } catch (e) {
      error = e.message
    } finally {
      incomingCall = null
    }
  }

  // Pulsante "Riaggancia" per chiamata attiva.
  // Chiama l'API backend che esegue PUT /v1/calls/{uuid} con action=hangup
  async function hangupCall() {
    let response

    try {
      response = await fetch(`/api/voice/calls/${incomingCall.callId}/hangup`, {
        method: 'PUT'
      })

      if (!response.ok) {
        throw new Error(`Hangup failed: ${response.status}`)
      }

      incomingCall = null
    } catch (e) {
      error = 'Errore riagganciare: ' + e.message
    }
  }

  // ─── refresh sessione ────────────────────────────────────────────────────

  // step 4 — programma il refresh prima della scadenza del TTL.
  // Dopo ogni refresh si riprogramma per il prossimo ciclo: la sessione
  // resta attiva finché il componente è vivo (step 5 la ferma).
  function scheduleRefresh() {
    if (refreshTimer) clearTimeout(refreshTimer)

    refreshTimer = setTimeout(async () => {
      let token

      try {
        token = await fetchToken()                  // stesso endpoint di step 2a
        await client.refreshSession(token)          // rinnova senza ricreare sessione

        lastToken = token
        scheduleRefresh()                           // riprogramma per il prossimo ciclo
      } catch (e) {
        // Il refresh è fallito: la sessione scadrà alla fine del TTL corrente.
        // L'operatore deve riconnettersi manualmente con "Riprova".
        sessionActive = false
        error         = e.message
      }
    }, REFRESH_DELAY_MS)
  }

  // ─── cleanup ─────────────────────────────────────────────────────────────

  // step 5 — chiamato da onDestroy.
  // Ordine deliberato: prima ferma il timer (evita che fetchToken parta
  // dopo il destroy), poi segnala a Vonage la chiusura della sessione.
  function teardown() {
    if (refreshTimer) clearTimeout(refreshTimer)

    if (client && sessionActive) {
      // deleteSession notifica Vonage che questa sessione non deve più
      // ricevere chiamate. Fire-and-forget: onDestroy non è async.
      client.deleteSession(lastToken)
    }
  }
</script>

<!-- ─── template ────────────────────────────────────────────────────────── -->

<div class="sdk-session">
  {#if error}
    <div class="sdk-error">
      <span>{error}</span>
      <button onclick={connect}>Riprova</button>
    </div>
  {/if}

  <div class="session-status {sessionActive ? 'active' : 'inactive'}">
    {sessionActive ? 'Online' : 'Disconnesso'}
  </div>

  {#if incomingCall}
    <div class="active-call">
      <p class="call-info">In chiamata con <strong>{incomingCall.from}</strong></p>
      <button class="btn-hangup" onclick={hangupCall}>Riaggancia</button>
    </div>
  {/if}
</div>

<!-- ─── styles ──────────────────────────────────────────────────────────── -->

<style>
  .sdk-session {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .session-status {
    font-size: 0.85rem;
    font-weight: 600;
    padding: 0.25rem 0.6rem;
    border-radius: 4px;
    display: inline-block;
    width: fit-content;
  }

  .session-status.active {
    background: #d4edda;
    color: #155724;
  }

  .session-status.inactive {
    background: #f8d7da;
    color: #721c24;
  }

  .sdk-error {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    background: #fff3cd;
    color: #856404;
    padding: 0.5rem 0.75rem;
    border-radius: 4px;
    font-size: 0.875rem;
  }

  .sdk-error button {
    background: #856404;
    color: #fff;
    border: none;
    padding: 0.25rem 0.6rem;
    border-radius: 3px;
    cursor: pointer;
    font-size: 0.8rem;
  }

  .sdk-error button:hover {
    background: #6d5203;
  }

  .active-call {
    background: #d4edda;
    border: 1px solid #c3e6cb;
    border-radius: 6px;
    padding: 1rem;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .call-info {
    margin: 0;
    color: #155724;
    font-size: 0.95rem;
    font-weight: 500;
  }

  .btn-hangup {
    background: #dc3545;
    color: #fff;
    border: none;
    padding: 0.5rem 1.25rem;
    border-radius: 4px;
    cursor: pointer;
    font-weight: 600;
  }

  .btn-hangup:hover {
    background: #c82333;
  }
</style>
