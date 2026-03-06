<script>
  /**
   * Layout principale del modulo voice3.
   *
   * Entry point del modulo: gestisce la configurazione iniziale
   * dell'operatore (userId) e, una volta attivata la sessione,
   * delega tutta la logica WebRTC a SdkSessionComponent.
   *
   * Flusso di attivazione:
   *   1. L'operatore inserisce il proprio userId (es. "operatore_01")
   *   2. Clicca "Attiva Sessione"
   *   3. SdkSessionComponent crea la sessione WebRTC tramite Vonage Client SDK
   */
  import { onMount, onDestroy } from 'svelte'
  import { clearContextSidebar } from '../sidebar/store.js'
  import { currentView, sdkConfig } from './store.js'
  import SdkSessionComponent from './SdkSessionComponent.svelte'

  onMount(() => {
    // Nessuna sidebar contestuale per questo modulo
  })

  onDestroy(() => {
    clearContextSidebar()
  })

  // Aggiorna lo userId nello store quando l'operatore modifica il campo
  function handleUserIdChange(event) {
    sdkConfig.update(config => ({
      ...config,
      userId: event.target.value
    }))
  }

  // Attiva la sessione: il Layout cede il rendering a SdkSessionComponent
  function activateSession() {
    if ($sdkConfig.userId.trim() === '') {
      alert('Inserisci userId operatore')
      return
    }

    sdkConfig.update(config => ({
      ...config,
      sessionActive: true
    }))
  }
</script>

<div class="voice3-module">
  <div class="header">
    <h2>Voice Module 3.0 - Progressive Dialer (Operator-First)</h2>
    <p class="subtitle">
      Pattern operator-first: l'operatore si connette per primo e aspetta con musica di attesa,
      poi il sistema chiama il cliente automaticamente.
    </p>
  </div>

  <!-- Fase configurazione: l'operatore non ha ancora attivato la sessione WebRTC -->
  {#if !$sdkConfig.sessionActive}
    <div class="config-panel">
      <h3>Configurazione Operatore</h3>
      <label>
        User ID Operatore:
        <input
          type="text"
          value={$sdkConfig.userId}
          oninput={handleUserIdChange}
          placeholder="operatore_01"
        />
      </label>
      <p class="help-text">
        L'operatore deve essere registrato su Vonage.
        Usa: <code>vonage users create --name=operatore_01</code>
      </p>
      <button onclick={activateSession}>Attiva Sessione</button>
    </div>

  <!-- Fase sessione attiva: SdkSessionComponent gestisce WebRTC e le chiamate -->
  {:else}
    <SdkSessionComponent userId={$sdkConfig.userId} />
  {/if}
</div>

<style>
  .voice3-module {
    padding: 2rem;
    max-width: 800px;
    margin: 0 auto;
  }

  .header {
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid #28a745;
  }

  h2 {
    margin: 0 0 0.5rem 0;
    color: #333;
  }

  .subtitle {
    margin: 0;
    color: #666;
    font-size: 0.9rem;
  }

  .config-panel {
    padding: 1.5rem;
    background: white;
    border: 1px solid #ddd;
    border-radius: 8px;
  }

  .config-panel h3 {
    margin: 0 0 1rem 0;
  }

  label {
    display: block;
    margin-bottom: 1rem;
    font-weight: 500;
  }

  input {
    width: 100%;
    padding: 0.5rem;
    border: 1px solid #ccc;
    border-radius: 4px;
    margin-top: 0.5rem;
  }

  .help-text {
    margin: 0.5rem 0 1rem 0;
    color: #666;
    font-size: 0.85rem;
  }

  code {
    background: #f4f4f4;
    padding: 0.2rem 0.4rem;
    border-radius: 3px;
    font-family: monospace;
  }

  button {
    padding: 0.6rem 1.5rem;
    background: #28a745;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 1rem;
  }

  button:hover {
    background: #1e7e34;
  }
</style>
