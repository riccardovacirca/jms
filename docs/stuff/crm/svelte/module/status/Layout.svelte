<script>
  import { onMount, onDestroy } from 'svelte';
  import { setContextSidebar, clearContextSidebar } from '../sidebar/store.js';
  import { setContextHeader, clearContextHeader } from '../header/store.js';
  import StatusSidebarLayout from './sidebar/Layout.svelte';
  import StatusHeaderLayout from './header/Layout.svelte';
  import HealthLayout from "./Health/Layout.svelte";
  import LogsLayout from "./Logs/Layout.svelte";

  let state = $state({ currentView: 'health' });

  function handleViewChange(view) {
    state.currentView = view;
  }

  onMount(() => {
    // Registra sidebar passando l'oggetto state direttamente
    setContextSidebar(StatusSidebarLayout, {
      state,
      onViewChange: handleViewChange
    });

    // Registra header passando l'oggetto state direttamente
    setContextHeader(StatusHeaderLayout, {
      state,
      onViewChange: handleViewChange
    }, 'Status');
  });

  onDestroy(() => {
    clearContextSidebar();
    clearContextHeader();
  });
</script>

<div class="status-dashboard">
  <main class="dashboard-content">
    {#if state.currentView === 'health'}
      <HealthLayout />
    {:else if state.currentView === 'logs'}
      <LogsLayout />
    {/if}
  </main>
</div>

<style>
  .status-dashboard {
    width: 100%;
    height: 100%;
    padding: 2rem;
    background: #f5f7fa;
    overflow-y: auto;
  }

  .dashboard-content {
    max-width: 1600px;
    margin: 0 auto;
  }
</style>
