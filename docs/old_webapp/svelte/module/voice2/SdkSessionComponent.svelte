<script>
  import { onMount, onDestroy } from 'svelte'
  import { VonageClient } from '@vonage/client-sdk'
  import { callState } from './store.js'

  const { userId } = $props()

  let sessionActive = $state(false)
  let error = $state(null)
  let customerNumber = $state('')

  let client
  let lastToken
  let refreshTimer

  const REFRESH_DELAY_MS = 13 * 60 * 1000

  onMount(() => {
    client = new VonageClient()
    connect()
  })

  onDestroy(() => {
    teardown()
  })

  async function connect() {
    let token
    let id

    error = null

    try {
      token = await fetchToken()
      id = await client.createSession(token)

      lastToken = token
      sessionActive = true

      registerListeners()
      scheduleRefresh()
    } catch (e) {
      error = e.message
      sessionActive = false
    }
  }

  async function fetchToken() {
    let response
    let data

    response = await fetch(`/api/voice2/sdk-token?userId=${userId}`)

    if (!response.ok) {
      throw new Error(`Token fetch failed: ${response.status}`)
    }

    data = await response.json()
    return data.out.token
  }

  function registerListeners() {
    // Chiamata in uscita via serverCall() - eventi di stato
    client.on('callHangup', onCallHangup)
    client.on('sessionError', onSessionError)
  }

  function onCallHangup(callId, callQuality, reason) {
    callState.set({
      active: false,
      callId: null,
      customerNumber: null,
      conversationName: null,
      status: 'idle'
    })
  }

  function onSessionError(reason) {
    sessionActive = false
    error = String(reason)
  }

  /**
   * Operator-first progressive dialer flow:
   * 1. Operator clicks "Pronto per chiamata"
   * 2. Frontend calls serverCall() with customerNumber
   * 3. Backend answer webhook receives customerNumber and returns NCCO with conversation + startOnEnter: false
   * 4. Operator hears hold music
   * 5. Backend automatically triggers customer call asynchronously (after 1s delay)
   * 6. Customer answers → conversation starts
   */
  async function startCall() {
    let callId
    let prepareResponse

    if (!customerNumber || customerNumber.trim() === '') {
      error = 'Inserisci numero cliente'
      return
    }

    error = null

    console.log('=== VOICE2 START CALL ===')
    console.log('Customer Number:', customerNumber.trim())
    console.log('Operator userId:', userId)

    try {
      // Step 1: Prepare call on backend (store customerNumber for later)
      console.log('Step 1: Calling prepare-call endpoint...')
      prepareResponse = await fetch('/api/voice2/prepare-call', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: userId,
          customerNumber: customerNumber.trim()
        })
      })

      if (!prepareResponse.ok) {
        throw new Error('Failed to prepare call: ' + prepareResponse.status)
      }

      console.log('prepare-call successful')

      // Step 2: serverCall() triggers answer_url webhook
      // Backend will retrieve customerNumber from storage and call customer
      console.log('Step 2: Calling client.serverCall()...')
      callId = await client.serverCall({})
      console.log('serverCall() returned callId:', callId)

      callState.set({
        active: true,
        callId: callId,
        customerNumber: customerNumber.trim(),
        conversationName: null,
        status: 'waiting_customer'
      })

      console.log('Call state updated, waiting for customer...')

    } catch (e) {
      console.error('ERROR in startCall():', e)
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

  async function hangupCall() {
    let response

    try {
      response = await fetch(`/api/voice2/calls/${$callState.callId}/hangup`, {
        method: 'PUT'
      })

      if (!response.ok) {
        throw new Error(`Hangup failed: ${response.status}`)
      }

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

  async function scheduleRefresh() {
    refreshTimer = setTimeout(async () => {
      let token

      try {
        token = await fetchToken()
        await client.refreshSession(token)
        lastToken = token
        scheduleRefresh()
      } catch (e) {
        sessionActive = false
        error = 'Errore refresh sessione: ' + e.message
      }
    }, REFRESH_DELAY_MS)
  }

  async function teardown() {
    if (refreshTimer) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }

    if (sessionActive && lastToken) {
      try {
        await client.deleteSession(lastToken)
      } catch (e) {
        console.error('Errore deleteSession:', e)
      }
    }

    sessionActive = false
    callState.set({
      active: false,
      callId: null,
      customerNumber: null,
      conversationName: null,
      status: 'idle'
    })
  }

  async function disconnect() {
    await teardown()
  }
</script>

<div class="sdk-session">
  <h3>Sessione Operatore WebRTC (Progressive Dialer)</h3>

  {#if !sessionActive}
    <div class="status disconnected">
      <p>Sessione non attiva</p>
      {#if error}
        <p class="error">{error}</p>
      {/if}
      <button onclick={connect}>Connetti</button>
    </div>
  {:else}
    <div class="status connected">
      <p>✓ Sessione attiva - Operatore: <strong>{userId}</strong></p>
      <button class="btn-secondary" onclick={disconnect}>Disconnetti</button>
    </div>

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
    {:else}
      <div class="active-call">
        {#if $callState.status === 'waiting_customer'}
          <p class="call-info">
            🎵 In attesa cliente <strong>{$callState.customerNumber}</strong>...
          </p>
          <p class="call-status">Chiamata in corso...</p>
        {:else if $callState.status === 'connected'}
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
    background: #007bff;
    color: white;
  }

  .btn-primary:hover {
    background: #0056b3;
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
