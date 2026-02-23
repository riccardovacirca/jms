<script>
  import { onMount } from 'svelte'
  import ListeTableComponent from './ListeTableComponent.svelte'
  import ListeFormComponent from './ListeFormComponent.svelte'
  import ListeFiltersComponent from './ListeFiltersComponent.svelte'
  import { caricaListe, loading, error, listeFilters, listePagination } from '../store.js'

  let currentSubView;
  let editingLista;

  currentSubView = 'table'
  editingLista = null

  function handleNewLista() {
    editingLista = null
    currentSubView = 'form'
  }

  function handleEditLista(lista) {
    editingLista = lista
    currentSubView = 'form'
  }

  function handleFormClose() {
    currentSubView = 'table'
    editingLista = null
  }

  async function handleFormSave() {
    currentSubView = 'table'
    editingLista = null
    await loadListe()
  }

  async function handleFilterChange() {
    await loadListe()
  }

  async function handlePageChange(event) {
    const offset = event.detail.offset

    listePagination.update(p => ({ ...p, offset }))
    await loadListe()
  }

  async function loadListe() {
    let filters;
    let pagination;

    listeFilters.subscribe(value => {
      filters = value
    })()

    listePagination.subscribe(value => {
      pagination = value
    })()

    await caricaListe(pagination.limit, pagination.offset, filters)
  }

  onMount(() => {
    loadListe()
  })
</script>

<div class="liste-view">
  {#if currentSubView === 'table'}
    <div class="view-header">
      <div>
        <h1>Gestione Liste</h1>
        <p>Organizza i contatti in liste per campagne e attività</p>
      </div>
      <button class="btn btn-primary" onclick={handleNewLista}>
        <i class="bi bi-plus-lg me-2"></i>
        Nuova Lista
      </button>
    </div>

    {#if $error}
      <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        {$error}
        <button type="button" class="btn-close" onclick={() => error.set(null)} aria-label="Chiudi"></button>
      </div>
    {/if}

    <ListeFiltersComponent onFilterChange={handleFilterChange} />

    {#if $loading}
      <div class="text-center py-5">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Caricamento...</span>
        </div>
      </div>
    {:else}
      <ListeTableComponent
        onEdit={handleEditLista}
        onPageChange={handlePageChange}
      />
    {/if}
  {:else if currentSubView === 'form'}
    <ListeFormComponent
      lista={editingLista}
      onSave={handleFormSave}
      onCancel={handleFormClose}
    />
  {/if}
</div>

<style>
  .liste-view {
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
