<script>
  import { tuteliste, listeCampagna, campagnaCorrente, aggiungiListaCampagna, loading, error } from '../store.js'

  let { onBack = () => {} } = $props()

  let search = $state('')
  let listeDisponibili = $state([])

  // Filtra le liste già associate
  $effect(() => {
    const associateIds = new Set($listeCampagna.map(l => Number(l.listaId)))

    listeDisponibili = $tuteliste
      .filter(l => !associateIds.has(Number(l.id)))
      .filter(l => {
        if (!search) return true
        const s = search.toLowerCase()
        return l.nome?.toLowerCase().includes(s) || l.descrizione?.toLowerCase().includes(s)
      })
  })

  async function handleAdd(lista) {
    try {
      await aggiungiListaCampagna($campagnaCorrente.id, lista.id)
      onBack()
    } catch (err) {
      alert(`Errore durante l'aggiunta: ${err.message}`)
    }
  }

  function getStatoBadgeClass(stato) {
    return { 0: 'bg-secondary', 1: 'bg-success', 2: 'bg-primary', 3: 'bg-warning' }[stato] || 'bg-secondary'
  }

  function getStatoLabel(stato) {
    return { 0: 'Bozza', 1: 'Attiva', 2: 'Completata', 3: 'Archiviata' }[stato] || ''
  }
</script>

<div class="add-lista-view">
  <div class="view-header">
    <div class="header-top">
      <button class="btn btn-link p-0" onclick={onBack}>
        <i class="bi bi-arrow-left me-2"></i>
        Torna alle liste della campagna
      </button>
    </div>
    <h2>Aggiungi Lista alla Campagna</h2>
    <p class="text-muted">Seleziona una lista esistente da associare</p>
  </div>

  {#if $error}
    <div class="alert alert-danger">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      {$error}
    </div>
  {/if}

  <!-- Search -->
  <div class="search-card">
    <div class="input-group">
      <span class="input-group-text">
        <i class="bi bi-search"></i>
      </span>
      <input
        type="text"
        class="form-control"
        placeholder="Cerca per nome o descrizione..."
        bind:value={search}
      />
    </div>
  </div>

  <!-- Tabella liste disponibili -->
  <div class="table-card">
    {#if listeDisponibili.length === 0}
      <div class="empty-state">
        <i class="bi bi-list-ul"></i>
        <h3>Nessuna lista disponibile</h3>
        <p>Tutte le liste sono già associate a questa campagna, oppure non ne esistono.</p>
      </div>
    {:else}
      <div class="table-responsive">
        <table class="table table-hover">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Descrizione</th>
              <th class="text-center">Stato</th>
              <th class="text-center">Contatti</th>
              <th class="text-end">Azioni</th>
            </tr>
          </thead>
          <tbody>
            {#each listeDisponibili as lista (lista.id)}
              <tr>
                <td class="fw-medium">{lista.nome}</td>
                <td class="text-muted">{lista.descrizione || '-'}</td>
                <td class="text-center">
                  <span class="badge {getStatoBadgeClass(lista.stato)}">
                    {getStatoLabel(lista.stato)}
                  </span>
                </td>
                <td class="text-center">
                  <span class="badge bg-primary-subtle text-primary">
                    {lista.contattiCount || 0}
                  </span>
                </td>
                <td class="text-end">
                  <button
                    type="button"
                    class="btn btn-primary btn-sm"
                    onclick={() => handleAdd(lista)}
                    disabled={$loading}
                  >
                    <i class="bi bi-plus-lg me-1"></i>
                    Aggiungi
                  </button>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  </div>
</div>

<style>
  .add-lista-view {
    padding: 2rem;
    max-width: 1200px;
    margin: 0 auto;
  }

  .view-header {
    margin-bottom: 1.5rem;
  }

  .header-top {
    margin-bottom: 1rem;
  }

  .view-header h2 {
    margin: 0 0 0.25rem 0;
    font-size: 1.75rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .search-card {
    margin-bottom: 1.5rem;
  }

  .table-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }

  .table {
    margin-bottom: 0;
  }

  .table thead th {
    background: #f8f9fa;
    border-bottom: 2px solid #dee2e6;
    font-weight: 600;
    font-size: 0.875rem;
    color: #495057;
    padding: 1rem 0.75rem;
  }

  .table tbody td {
    padding: 0.875rem 0.75rem;
    vertical-align: middle;
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    color: #6c757d;
  }

  .empty-state i {
    font-size: 4rem;
    opacity: 0.3;
    margin-bottom: 1rem;
  }

  .empty-state h3 {
    font-size: 1.25rem;
    font-weight: 600;
    margin-bottom: 0.5rem;
    color: #495057;
  }

  .empty-state p {
    margin: 0;
  }
</style>
