<script>
  import { onMount, onDestroy } from 'svelte'
  import { setContextSidebar, clearContextSidebar } from '../sidebar/store.js'
  import { currentView, sdkConfig } from './store.js'
  import SdkSessionComponent from './SdkSessionComponent.svelte'

  onMount(() => {
    // TODO: Add sidebar if needed
    // setContextSidebar(SidebarLayout, { state }, 'Voice2')
  })

  onDestroy(() => {
    clearContextSidebar()
  })

  function handleUserIdChange(event) {
    sdkConfig.update(config => ({
      ...config,
      userId: event.target.value
    }))
  }

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

<div class="voice2-module">
  <div class="header">
    <h2>Voice Module 2.0 - Progressive Dialer (Operator-First)</h2>
    <p class="subtitle">
      Pattern operator-first: l'operatore si connette per primo e aspetta con musica di attesa,
      poi il sistema chiama il cliente.
    </p>
  </div>

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
  {:else}
    <SdkSessionComponent userId={$sdkConfig.userId} />
  {/if}
</div>

<style>
  .voice2-module {
    padding: 2rem;
    max-width: 800px;
    margin: 0 auto;
  }

  .header {
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid #007bff;
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
    background: #007bff;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 1rem;
  }

  button:hover {
    background: #0056b3;
  }
</style>
