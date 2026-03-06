<script>
  import { createEventDispatcher } from 'svelte'
  import { contattiLista, contattiPagination, currentLista, rimuoviContattoDaLista } from '../store.js'
  import { currentModule } from '../../../store.js'

  let { onPageChange = () => {} } = $props()

  const dispatch = createEventDispatcher()

  async function handleRemove(contatto) {
    let lista;

    currentLista.subscribe(value => {
      lista = value
    })()

    const confirmMessage = `Sei sicuro di voler rimuovere "${contatto.nomeContatto}" da questa lista?\n\nIl contatto non verrà eliminato dal sistema.`

    if (!confirm(confirmMessage)) {
      return
    }

    try {
      await rimuoviContattoDaLista(lista.id, contatto.contattoId)
    } catch (err) {
      alert(`Errore durante la rimozione: ${err.message}`)
    }
  }

  function handleViewContatto(contattoId) {
    // Naviga al modulo contatti per visualizzare/modificare il contatto
    currentModule.navigate('contatti', `view/${contattoId}`)
  }

  function formatDate(dateString) {
    if (!dateString) return '-'

    const date = new Date(dateString)

    return date.toLocaleDateString('it-IT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  function formatTelefono(telefono) {
    if (!telefono) return '-'

    return telefono
  }

  function handlePreviousPage() {
    let pagination;

    contattiPagination.subscribe(value => {
      pagination = value
    })()

    if (pagination.offset > 0) {
      const newOffset = Math.max(0, pagination.offset - pagination.limit)

      dispatch('pageChange', { offset: newOffset })
    }
  }

  function handleNextPage() {
    let pagination;

    contattiPagination.subscribe(value => {
      pagination = value
    })()

    if (pagination.offset + pagination.limit < pagination.total) {
      const newOffset = pagination.offset + pagination.limit

      dispatch('pageChange', { offset: newOffset })
    }
  }
</script>

<div class="table-card">
  {#if $contattiLista.length === 0}
    <div class="empty-state">
      <i class="bi bi-people"></i>
      <h3>Nessun contatto in questa lista</h3>
      <p>Aggiungi contatti esistenti o crea nuovi contatti dal modulo Contatti</p>
    </div>
  {:else}
    <div class="table-responsive">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>Nome Contatto</th>
            <th>Telefono</th>
            <th class="text-center">Aggiunto il</th>
            <th class="text-end">Azioni</th>
          </tr>
        </thead>
        <tbody>
          {#each $contattiLista as contatto (contatto.id)}
            <tr>
              <td class="fw-medium">
                <button
                  class="btn btn-link p-0 text-start text-decoration-none"
                  onclick={() => handleViewContatto(contatto.contattoId)}
                  title="Visualizza dettagli contatto"
                >
                  {contatto.nomeContatto?.trim() || contatto.telefono || `Contatto #${contatto.contattoId}`}
                </button>
              </td>
              <td>
                <a href="tel:{contatto.telefono}" class="text-decoration-none">
                  {formatTelefono(contatto.telefono)}
                </a>
              </td>
              <td class="text-center text-muted small">{formatDate(contatto.createdAt)}</td>
              <td class="text-end">
                <button
                  type="button"
                  class="btn btn-outline-danger btn-sm"
                  title="Rimuovi dalla lista"
                  onclick={() => handleRemove(contatto)}
                >
                  <i class="bi bi-x-lg"></i>
                  Rimuovi
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>

    <div class="pagination-footer">
      <div class="pagination-info">
        Visualizzazione {$contattiPagination.offset + 1} - {Math.min($contattiPagination.offset + $contattiPagination.limit, $contattiPagination.total)} di {$contattiPagination.total}
      </div>
      <div class="pagination-controls">
        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          disabled={$contattiPagination.offset === 0}
          onclick={handlePreviousPage}
        >
          <i class="bi bi-chevron-left"></i>
          Precedente
        </button>
        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          disabled={$contattiPagination.offset + $contattiPagination.limit >= $contattiPagination.total}
          onclick={handleNextPage}
        >
          Successivo
          <i class="bi bi-chevron-right"></i>
        </button>
      </div>
    </div>
  {/if}
</div>

<style>
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
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: #495057;
    padding: 1rem 0.75rem;
  }

  .table tbody td {
    padding: 0.875rem 0.75rem;
    vertical-align: middle;
  }

  .table tbody tr:hover {
    background-color: #f8f9fa;
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
    font-size: 1rem;
  }

  .pagination-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 1.5rem;
    border-top: 1px solid #dee2e6;
    background: #f8f9fa;
  }

  .pagination-info {
    font-size: 0.875rem;
    color: #6c757d;
  }

  .pagination-controls {
    display: flex;
    gap: 0.5rem;
  }

  @media (max-width: 768px) {
    .pagination-footer {
      flex-direction: column;
      gap: 1rem;
    }

    .pagination-controls {
      width: 100%;
      justify-content: space-between;
    }

    .pagination-controls button {
      flex: 1;
    }
  }
</style>
