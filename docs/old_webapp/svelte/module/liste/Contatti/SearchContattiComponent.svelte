<script>
  import { aggiungiContattoALista, loading, error } from '../store.js'

  let { listaId = null, onBack = () => {}, onCreateNew = () => {} } = $props()

  let searchQuery = $state('')
  let searchResults = $state([])
  let searching = $state(false)

  async function handleSearch() {
    if (!searchQuery || searchQuery.trim().length < 2) {
      searchResults = []
      return
    }

    searching = true

    try {
      const response = await fetch(`/api/liste/contatti/search?q=${encodeURIComponent(searchQuery.trim())}`, {
        credentials: 'include'
      })

      if (!response.ok) {
        throw new Error('Errore durante la ricerca')
      }

      const data = await response.json()

      searchResults = data.out || []
    } catch (err) {
      console.error('Errore ricerca:', err)
      searchResults = []
    } finally {
      searching = false
    }
  }

  async function handleAddContatto(contatto) {
    try {
      await aggiungiContattoALista(listaId, contatto.id)

      alert(`Contatto "${formatContattoDisplay(contatto)}" aggiunto alla lista`)

      searchQuery = ''
      searchResults = []
    } catch (err) {
      alert(`Errore durante l'aggiunta: ${err.message}`)
    }
  }

  function formatContattoDisplay(contatto) {
    const parts = []

    if (contatto.nome) parts.push(contatto.nome)
    if (contatto.cognome) parts.push(contatto.cognome)
    if (contatto.ragioneSociale && parts.length === 0) parts.push(contatto.ragioneSociale)

    return parts.join(' ') || 'Senza nome'
  }
</script>

<div class="search-view">
  <div class="search-header">
    <button class="btn btn-link p-0 mb-3" onclick={onBack}>
      <i class="bi bi-arrow-left me-2"></i>
      Torna alla tabella
    </button>
    <h2>Aggiungi Contatto Esistente</h2>
    <p class="text-muted">Cerca un contatto da aggiungere alla lista</p>
  </div>

  {#if $error}
    <div class="alert alert-danger">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      {$error}
    </div>
  {/if}

  <div class="search-card">
    <div class="search-section">
      <div class="input-group mb-3">
        <input
          type="text"
          class="form-control form-control-lg"
          placeholder="Cerca per nome, telefono, email..."
          bind:value={searchQuery}
          oninput={handleSearch}
        />
        <button
          class="btn btn-outline-secondary"
          type="button"
          onclick={handleSearch}
          disabled={searching}
        >
          {#if searching}
            <span class="spinner-border spinner-border-sm"></span>
          {:else}
            <i class="bi bi-search"></i>
          {/if}
        </button>
      </div>

      <div class="create-new-section">
        <p class="text-muted small mb-2">
          <i class="bi bi-info-circle me-1"></i>
          Non trovi il contatto che cerchi?
        </p>
        <button
          type="button"
          class="btn btn-outline-primary"
          onclick={onCreateNew}
        >
          <i class="bi bi-plus-lg me-1"></i>
          Crea Nuovo Contatto
        </button>
      </div>
    </div>

    <div class="results-section">
      {#if searching}
        <div class="text-center py-5">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Ricerca in corso...</span>
          </div>
        </div>
      {:else if searchQuery && searchQuery.trim().length >= 2 && searchResults.length === 0}
        <div class="empty-results">
          <i class="bi bi-search"></i>
          <p>Nessun contatto trovato</p>
          <small class="text-muted">Prova con termini diversi o crea un nuovo contatto</small>
        </div>
      {:else if searchResults.length > 0}
        <div class="results-list">
          <p class="text-muted small mb-3">
            Trovati {searchResults.length} contatti
          </p>
          {#each searchResults as contatto (contatto.id)}
            <div class="result-item">
              <div class="result-info">
                <div class="result-name">{formatContattoDisplay(contatto)}</div>
                <div class="result-details">
                  {#if contatto.telefono}
                    <span class="badge bg-light text-dark">
                      <i class="bi bi-telephone"></i>
                      {contatto.telefono}
                    </span>
                  {/if}
                  {#if contatto.email}
                    <span class="badge bg-light text-dark">
                      <i class="bi bi-envelope"></i>
                      {contatto.email}
                    </span>
                  {/if}
                  {#if contatto.blacklist}
                    <span class="badge bg-danger">
                      <i class="bi bi-shield-x"></i>
                      Blacklist
                    </span>
                  {/if}
                </div>
              </div>
              <button
                type="button"
                class="btn btn-sm btn-primary"
                onclick={() => handleAddContatto(contatto)}
                disabled={$loading || contatto.blacklist}
              >
                {#if $loading}
                  <span class="spinner-border spinner-border-sm me-1"></span>
                {/if}
                <i class="bi bi-plus-lg"></i>
                Aggiungi
              </button>
            </div>
          {/each}
        </div>
      {:else}
        <div class="empty-results">
          <i class="bi bi-search"></i>
          <p>Cerca un contatto da aggiungere</p>
          <small class="text-muted">Inizia a digitare nel campo di ricerca (minimo 2 caratteri)</small>
        </div>
      {/if}
    </div>
  </div>
</div>

<style>
  .search-view {
    max-width: 900px;
    margin: 0 auto;
  }

  .search-header {
    margin-bottom: 2rem;
  }

  .search-header h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.75rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .search-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }

  .search-section {
    padding: 2rem;
    border-bottom: 1px solid #dee2e6;
  }

  .create-new-section {
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 1px solid #dee2e6;
  }

  .results-section {
    padding: 2rem;
    min-height: 300px;
    max-height: 500px;
    overflow-y: auto;
  }

  .results-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .result-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    border: 1px solid #dee2e6;
    border-radius: 8px;
    transition: all 0.2s;
  }

  .result-item:hover {
    background-color: #f8f9fa;
    border-color: #667eea;
  }

  .result-info {
    flex: 1;
  }

  .result-name {
    font-weight: 500;
    color: #2c3e50;
    margin-bottom: 0.5rem;
  }

  .result-details {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .result-details .badge {
    font-weight: normal;
  }

  .empty-results {
    text-align: center;
    padding: 3rem 2rem;
    color: #6c757d;
  }

  .empty-results i {
    font-size: 3rem;
    opacity: 0.3;
    margin-bottom: 1rem;
  }

  .empty-results p {
    font-size: 1.1rem;
    font-weight: 500;
    margin-bottom: 0.5rem;
    color: #495057;
  }
</style>
