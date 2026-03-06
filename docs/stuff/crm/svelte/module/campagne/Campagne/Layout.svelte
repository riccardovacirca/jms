<script>
  import { onMount } from 'svelte'
  import CampagneTableComponent from './CampagneTableComponent.svelte'
  import CampagnaFormComponent from './CampagnaFormComponent.svelte'
  import CampagneFiltersComponent from './CampagneFiltersComponent.svelte'
  import { caricaCampagne, loading, error, campagneFilters, campagne } from '../store.js'

  let currentSubView = $state('table')
  let editingCampagna = $state(null)

  function handleNewCampagna() {
    editingCampagna = null
    currentSubView = 'form'
  }

  function handleEditCampagna(campagna) {
    editingCampagna = campagna
    currentSubView = 'form'
  }

  function handleFormClose() {
    currentSubView = 'table'
    editingCampagna = null
  }

  async function handleFormSave() {
    currentSubView = 'table'
    editingCampagna = null
    await caricaCampagne()
  }

  onMount(() => {
    caricaCampagne()
  })
</script>

<div class="campagne-view">
  {#if currentSubView === 'table'}
    <div class="view-header">
      <div>
        <h1>Gestione Campagne</h1>
        <p>Organizza le liste di contatti in campagne</p>
      </div>
      <button class="btn btn-primary" onclick={handleNewCampagna}>
        <i class="bi bi-plus-lg me-2"></i>
        Nuova Campagna
      </button>
    </div>

    {#if $error}
      <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        {$error}
        <button type="button" class="btn-close" onclick={() => error.set(null)} aria-label="Chiudi"></button>
      </div>
    {/if}

    <CampagneFiltersComponent />

    {#if $loading}
      <div class="text-center py-5">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Caricamento...</span>
        </div>
      </div>
    {:else}
      <CampagneTableComponent onEdit={handleEditCampagna} />
    {/if}
  {:else if currentSubView === 'form'}
    <CampagnaFormComponent
      campagna={editingCampagna}
      onSave={handleFormSave}
      onCancel={handleFormClose}
    />
  {/if}
</div>

<style>
  .campagne-view {
    padding: 2rem;
    max-width: 1600px;
    margin: 0 auto;
  }

  .view-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 2rem;
  }

  .view-header h1 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .view-header p {
    margin: 0;
    color: #7f8c8d;
    font-size: 1rem;
  }
</style>
