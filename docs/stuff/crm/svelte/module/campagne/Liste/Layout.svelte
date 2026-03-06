<script>
  import { onMount } from 'svelte'
  import ListeTableComponent from './ListeTableComponent.svelte'
  import AddListaComponent from './AddListaComponent.svelte'
  import {
    campagnaCorrente,
    listeCampagna,
    loading,
    error,
    showCampagne,
    caricaListeCampagna
  } from '../store.js'

  let currentSubView = $state('table')

  onMount(() => {
    let campagna;
    campagnaCorrente.subscribe(value => { campagna = value })()
    if (campagna) {
      caricaListeCampagna(campagna.id)
    }
  })

  function handleBack() {
    showCampagne()
  }

  function handleAddLista() {
    currentSubView = 'add'
  }

  function handleBackToTable() {
    currentSubView = 'table'
  }

  function getStatoLabel(stato) {
    return { 0: 'Bozza', 1: 'Attiva', 2: 'Completata', 3: 'Archiviata' }[stato] || ''
  }

  function getStatoBadgeClass(stato) {
    return { 0: 'bg-secondary', 1: 'bg-success', 2: 'bg-primary', 3: 'bg-warning' }[stato] || 'bg-secondary'
  }

  // Conta totale contatti sommando quelli di tutte le liste
  let totalContatti = $state(0)
  $effect(() => {
    let liste;
    listeCampagna.subscribe(value => { liste = value })()
    totalContatti = liste.reduce((sum, l) => sum + (l.contattiCount || 0), 0)
  })
</script>

<div class="liste-view">
  {#if currentSubView === 'table'}
    <div class="view-header">
      <div class="header-top">
        <button class="btn btn-link p-0" onclick={handleBack}>
          <i class="bi bi-arrow-left me-2"></i>
          Torna alle campagne
        </button>
      </div>

      <div class="header-main">
        <div class="header-info">
          <h1>
            <i class="bi bi-megaphone me-2"></i>
            {$campagnaCorrente?.nome || 'Campagna'}
          </h1>
          {#if $campagnaCorrente?.descrizione}
            <p class="description">{$campagnaCorrente.descrizione}</p>
          {/if}
          <div class="meta-info">
            <span class="badge {getStatoBadgeClass($campagnaCorrente?.stato)}">
              {getStatoLabel($campagnaCorrente?.stato)}
            </span>
            <span class="badge {$campagnaCorrente?.tipo === 'inbound' ? 'bg-info' : 'bg-primary'}">
              {$campagnaCorrente?.tipo?.toUpperCase() || 'OUTBOUND'}
            </span>
            <span class="text-muted">
              <i class="bi bi-list-ul me-1"></i>
              {$listeCampagna.length} liste
            </span>
            <span class="text-muted">
              <i class="bi bi-people-fill me-1"></i>
              {totalContatti} contatti
            </span>
          </div>
        </div>

        <button class="btn btn-primary" onclick={handleAddLista}>
          <i class="bi bi-plus-lg me-2"></i>
          Aggiungi Lista
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
      <ListeTableComponent />
    {/if}
  {:else if currentSubView === 'add'}
    <AddListaComponent onBack={handleBackToTable} />
  {/if}
</div>

<style>
  .liste-view {
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
