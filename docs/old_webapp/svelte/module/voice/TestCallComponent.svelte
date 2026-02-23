<script>
  // Componente di test per chiamate WebRTC
  // Permette di testare il flusso completo: chiamata verso testNumber
  // con l'operatore che risponde via WebRTC nel browser.

  const { testNumber = '+393713989250', operatorUserId } = $props()

  let calling = $state(false)
  let lastCallUuid = $state(null)
  let error = $state(null)

  // Avvia una chiamata di test verso testNumber con operatore WebRTC
  async function makeTestCall() {
    let response
    let data

    calling = true
    error = null
    lastCallUuid = null

    try {
      // POST /api/voice/calls con operatorType="app"
      // Il sistema chiamerà testNumber e collegherà al browser via WebRTC
      response = await fetch('/api/voice/calls', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          toNumber: testNumber,           // numero da chiamare (il tuo test number)
          operatorType: 'app',             // WebRTC (non PSTN)
          operatorId: operatorUserId       // il tuo userId Vonage
        })
      })

      if (!response.ok) {
        throw new Error(`Chiamata fallita: ${response.status}`)
      }

      data = await response.json()

      if (data.err) {
        throw new Error(data.log || 'Errore sconosciuto')
      }

      lastCallUuid = data.out.uuid

    } catch (e) {
      error = e.message
    } finally {
      calling = false
    }
  }
</script>

<div class="test-call">
  <div class="test-header">
    <h3>Test Chiamata WebRTC</h3>
    <p class="test-description">
      Clicca il pulsante per chiamare <strong>{testNumber}</strong>.<br>
      Quando il numero risponde, sentirai l'audio nella tua cuffia.
    </p>
  </div>

  {#if error}
    <div class="test-error">
      <strong>Errore:</strong> {error}
    </div>
  {/if}

  {#if lastCallUuid}
    <div class="test-success">
      <strong>Chiamata avviata!</strong><br>
      UUID: <code>{lastCallUuid}</code><br>
      <span class="test-instruction">
        Aspetta la notifica "Chiamata in ingresso" sopra.<br>
        Quando appare, clicca "Accetta" per parlare.
      </span>
    </div>
  {/if}

  <button
    class="btn-test-call"
    onclick={makeTestCall}
    disabled={calling}
  >
    {calling ? 'Chiamata in corso...' : 'Chiama Numero Test'}
  </button>

  <div class="test-info">
    <strong>Come funziona:</strong>
    <ol>
      <li>Assicurati che la sessione WebRTC sia "Online" (vedi sopra)</li>
      <li>Clicca "Chiama Numero Test"</li>
      <li>Vonage chiama il numero {testNumber}</li>
      <li>Quando risponde, ricevi una notifica "Chiamata in ingresso"</li>
      <li>Clicca "Accetta" e puoi parlare tramite cuffia</li>
    </ol>
  </div>
</div>

<style>
  .test-call {
    background: #fff;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    padding: 1.5rem;
    margin-top: 1rem;
  }

  .test-header h3 {
    margin: 0 0 0.5rem;
    color: #2c3e50;
    font-size: 1.1rem;
  }

  .test-description {
    margin: 0 0 1rem;
    color: #555;
    font-size: 0.9rem;
    line-height: 1.5;
  }

  .test-error {
    background: #fee;
    border-left: 4px solid #e74c3c;
    padding: 0.75rem;
    margin-bottom: 1rem;
    color: #c0392b;
    font-size: 0.9rem;
  }

  .test-success {
    background: #e8f5e9;
    border-left: 4px solid #4caf50;
    padding: 0.75rem;
    margin-bottom: 1rem;
    color: #2e7d32;
    font-size: 0.9rem;
  }

  .test-success code {
    background: #fff;
    padding: 0.2rem 0.4rem;
    border-radius: 3px;
    font-family: monospace;
    font-size: 0.85rem;
  }

  .test-instruction {
    display: block;
    margin-top: 0.5rem;
    font-style: italic;
    color: #1976d2;
  }

  .btn-test-call {
    width: 100%;
    padding: 1rem;
    background: #2196f3;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-test-call:hover:not(:disabled) {
    background: #1976d2;
  }

  .btn-test-call:disabled {
    background: #bdbdbd;
    cursor: not-allowed;
  }

  .test-info {
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 1px solid #e0e0e0;
    font-size: 0.85rem;
    color: #666;
  }

  .test-info strong {
    display: block;
    margin-bottom: 0.5rem;
    color: #333;
  }

  .test-info ol {
    margin: 0;
    padding-left: 1.5rem;
    line-height: 1.6;
  }

  .test-info li {
    margin-bottom: 0.3rem;
  }
</style>
