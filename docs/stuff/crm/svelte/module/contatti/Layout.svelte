<script>
  import SearchBarComponent from './SearchBarComponent.svelte';
  import ContactListComponent from './ContactListComponent.svelte';
  import ContactFormComponent from './ContactFormComponent.svelte';
  import { currentView, selectedContatto, showList } from './store.js';

  function handleSave(savedContatto) {
    showList();
  }

  function handleCancel() {
    showList();
  }
</script>

<div class="contatti-module">
  {#if $currentView === 'list'}
    <div class="module-header">
      <h1>Gestione Contatti</h1>
      <p>Gestisci e organizza i contatti del CRM</p>
    </div>

    <SearchBarComponent />
    <ContactListComponent />
  {:else if $currentView === 'new'}
    <ContactFormComponent onSave={handleSave} onCancel={handleCancel} />
  {:else if $currentView === 'edit'}
    <ContactFormComponent contatto={$selectedContatto} onSave={handleSave} onCancel={handleCancel} />
  {/if}
</div>

<style>
  .contatti-module {
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
