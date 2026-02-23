<script>
  import { onMount, onDestroy } from 'svelte'
  import { setContextSidebar, clearContextSidebar } from '../sidebar/store.js'
  import { operatorUserId, currentView } from './store.js'
  import VoiceSidebarLayout from './sidebar/Layout.svelte'
  import ConfigComponent from './ConfigComponent.svelte'
  import SdkSessionComponent from './SdkSessionComponent.svelte'
  import TestCallComponent from './TestCallComponent.svelte'

  let viewState = $state({ currentView: 'config' })

  function handleViewChange(view) {
    viewState.currentView = view
    currentView.set(view)
  }

  onMount(() => {
    setContextSidebar(VoiceSidebarLayout, {
      state: viewState,
      onViewChange: handleViewChange
    })
  })

  onDestroy(() => {
    clearContextSidebar()
  })
</script>

<div class="voice-module">
  {#if viewState.currentView === 'config'}
    <div class="voice-header">
      <h1>Configurazione Voice</h1>
      <p>Configura l'operatore per utilizzare il modulo WebRTC</p>
    </div>
    <div class="voice-content">
      <ConfigComponent />
    </div>

  {:else if viewState.currentView === 'test'}
    <div class="voice-header">
      <h1>Test Chiamate WebRTC</h1>
      <p>Interfaccia di test per verificare il funzionamento delle chiamate Vonage con WebRTC</p>
    </div>

    {#if $operatorUserId}
      <div class="voice-content">
        <SdkSessionComponent userId={$operatorUserId} />
        <TestCallComponent operatorUserId={$operatorUserId} />
      </div>
    {:else}
      <div class="voice-empty">
        <p>Configura l'userId Vonage dell'operatore prima di procedere.</p>
        <p>Vai alla sezione <strong>Configurazione</strong> nella sidebar.</p>
      </div>
    {/if}
  {/if}
</div>

<style>
  .voice-module {
    width: 100%;
    height: 100%;
    padding: 2rem;
    background: #f5f7fa;
    overflow-y: auto;
  }

  .voice-header {
    margin-bottom: 2rem;
  }

  .voice-header h1 {
    margin: 0 0 0.5rem;
    font-size: 1.8rem;
    color: #2c3e50;
  }

  .voice-header p {
    margin: 0;
    color: #7f8c8d;
    font-size: 0.95rem;
  }

  .voice-content {
    max-width: 800px;
  }

  .voice-empty {
    background: #fff3cd;
    border: 1px solid #ffc107;
    border-radius: 6px;
    padding: 1.5rem;
    color: #856404;
  }

  .voice-empty p {
    margin: 0 0 0.5rem;
  }
</style>
