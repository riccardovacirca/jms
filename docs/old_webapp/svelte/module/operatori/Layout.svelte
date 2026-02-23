<script>
  import { currentView, selectedOperatore, showList } from './store.js';
  import OperatoriTableComponent from './OperatoriTableComponent.svelte';
  import OperatoreFormComponent from './OperatoreFormComponent.svelte';

  function handleSave() {
    showList();
  }

  function handleCancel() {
    showList();
  }
</script>

<div class="operatori-module">
  {#if $currentView === 'list'}
    <div class="module-header">
      <h1>Gestione Operatori</h1>
      <p>Gestisci gli operatori e le loro campagne assegnate</p>
    </div>

    <OperatoriTableComponent />
  {:else if $currentView === 'new'}
    <OperatoreFormComponent onSave={handleSave} onCancel={handleCancel} />
  {:else if $currentView === 'edit'}
    <OperatoreFormComponent operatore={$selectedOperatore} onSave={handleSave} onCancel={handleCancel} />
  {/if}
</div>

<style>
  .operatori-module {
    width: 100%;
    height: 100%;
    padding: 2rem;
    background: #f5f7fa;
    overflow-y: auto;
  }

  .module-header {
    margin-bottom: 2rem;
  }

  .module-header h1 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .module-header p {
    margin: 0;
    color: #7f8c8d;
    font-size: 1rem;
  }
</style>
