<script>
  import { onMount } from 'svelte'
  import ContattiTableComponent from './ContattiTableComponent.svelte'
  import SearchContattiComponent from './SearchContattiComponent.svelte'
  import ContactFormComponent from '../../contatti/ContactFormComponent.svelte'
  import {
    currentLista,
    loading,
    error,
    showListe,
    caricaContattiLista,
    contattiPagination,
    aggiungiContattoALista
  } from '../store.js'

  let currentSubView;

  currentSubView = 'table'

  function handleBack() {
    showListe()
  }

  function handleAddContatto() {
    currentSubView = 'search'
  }

  function handleCreateNewContatto() {
    currentSubView = 'newContatto'
  }

  function handleBackToTable() {
    currentSubView = 'table'
    loadContatti()
  }

  async function handleContattoCreated(contattoCreato) {
    let lista;

    currentLista.subscribe(value => {
      lista = value
    })()

    if (lista && lista.id && contattoCreato && contattoCreato.id) {
      try {
        await aggiungiContattoALista(lista.id, contattoCreato.id)
        currentSubView = 'table'
      } catch (err) {
        console.error('Errore aggiunta contatto alla lista:', err)
        alert(`Contatto creato ma non aggiunto alla lista: ${err.message}`)
        currentSubView = 'table'
      }
    } else {
      currentSubView = 'table'
      loadContatti()
    }
  }

  async function handlePageChange(event) {
    const offset = event.detail.offset

    contattiPagination.update(p => ({ ...p, offset }))
    await loadContatti()
  }

  async function loadContatti() {
    let lista;
    let pagination;

    currentLista.subscribe(value => {
      lista = value
    })()

    contattiPagination.subscribe(value => {
      pagination = value
    })()

    if (lista && lista.id) {
      await caricaContattiLista(lista.id, pagination.limit, pagination.offset)
    }
  }

  onMount(() => {
    loadContatti()
  })
</script>

<div class="contatti-view">
  {#if currentSubView === 'table'}
    <div class="view-header">
      <div class="header-top">
        <button class="btn btn-link p-0" onclick={handleBack}>
          <i class="bi bi-arrow-left me-2"></i>
          Torna alle liste
        </button>
      </div>

      <div class="header-main">
        <div class="header-info">
          <h1>
            <i class="bi bi-list-ul me-2"></i>
            {$currentLista?.nome || 'Lista'}
          </h1>
          {#if $currentLista?.descrizione}
            <p class="description">{$currentLista.descrizione}</p>
          {/if}
          <div class="meta-info">
            <span class="badge {$currentLista?.attiva ? 'bg-success' : 'bg-secondary'}">
              {$currentLista?.attiva ? 'Attiva' : 'Disattivata'}
            </span>
            {#if $currentLista?.consenso}
              <span class="badge bg-info">Consenso richiesto</span>
            {/if}
            <span class="text-muted">
              <i class="bi bi-people-fill me-1"></i>
              {$currentLista?.contattiCount || 0} contatti
            </span>
          </div>
        </div>

        <button class="btn btn-primary" onclick={handleAddContatto}>
          <i class="bi bi-plus-lg me-2"></i>
          Aggiungi Contatto
        </button>
      </div>
    </div>

    {#if $error}
      <div class="alert alert-danger alert-dismissible fade show" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        {$error}
        <button type="button" class="btn-close" onclick={() => error.set(null)} aria-label="Chiudi"></button>
      </div>
    {/if}

    {#if $loading}
      <div class="text-center py-5">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Caricamento...</span>
        </div>
      </div>
    {:else}
      <ContattiTableComponent onPageChange={handlePageChange} />
    {/if}
  {:else if currentSubView === 'search'}
    <SearchContattiComponent
      listaId={$currentLista?.id}
      onBack={handleBackToTable}
      onCreateNew={handleCreateNewContatto}
    />
  {:else if currentSubView === 'newContatto'}
    <div class="form-container">
      <div class="form-header">
        <button class="btn btn-link p-0 mb-3" onclick={handleBackToTable}>
          <i class="bi bi-arrow-left me-2"></i>
          Torna alla lista
        </button>
        <h2>Crea Nuovo Contatto</h2>
        <p class="text-muted">Il contatto verrà automaticamente aggiunto alla lista</p>
      </div>
      <ContactFormComponent
        contatto={null}
        onSave={handleContattoCreated}
        onCancel={handleBackToTable}
      />
    </div>
  {/if}
</div>

<style>
  .contatti-view {
    padding: 2rem;
    max-width: 1600px;
    margin: 0 auto;
  }

  .view-header {
    margin-bottom: 2rem;
  }

  .header-top {
    margin-bottom: 1rem;
  }

  .header-main {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 2rem;
  }

  .header-info h1 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    color: #2c3e50;
    font-weight: 600;
    display: flex;
    align-items: center;
  }

  .description {
    margin: 0 0 0.75rem 0;
    color: #7f8c8d;
    font-size: 1rem;
  }

  .meta-info {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .meta-info .text-muted {
    font-size: 0.875rem;
  }

  .form-container {
    max-width: 900px;
    margin: 0 auto;
  }

  .form-header {
    margin-bottom: 2rem;
  }

  .form-header h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.75rem;
    color: #2c3e50;
    font-weight: 600;
  }

  @media (max-width: 768px) {
    .header-main {
      flex-direction: column;
      align-items: stretch;
    }

    .header-main button {
      width: 100%;
    }
  }
</style>
