<script>
  import { createEventDispatcher } from 'svelte'
  import { liste, listePagination, eliminaLista, showContatti } from '../store.js'

  let { onEdit = () => {}, onPageChange = () => {} } = $props()

  const dispatch = createEventDispatcher()

  function handleEdit(lista) {
    onEdit(lista)
  }

  function handleViewContatti(lista) {
    showContatti(lista)
  }

  async function handleDelete(lista) {
    const confirmMessage = `Sei sicuro di voler eliminare la lista "${lista.nome}"?\n\nQuesta operazione non può essere annullata.`

    if (!confirm(confirmMessage)) {
      return
    }

    try {
      await eliminaLista(lista.id)
    } catch (err) {
      alert(`Errore durante l'eliminazione: ${err.message}`)
    }
  }

  function formatDate(dateString) {
    if (!dateString) return '-'

    const date = new Date(dateString)

    return date.toLocaleDateString('it-IT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    })
  }

  function getStatoBadgeClass(stato) {
    const classes = {
      0: 'bg-secondary',
      1: 'bg-success',
      2: 'bg-primary',
      3: 'bg-warning'
    }

    return classes[stato] || 'bg-secondary'
  }

  function getStatoLabel(stato) {
    const labels = {
      0: 'Bozza',
      1: 'Attiva',
      2: 'Completata',
      3: 'Archiviata'
    }

    return labels[stato] || 'Sconosciuto'
  }

  function handlePreviousPage() {
    let pagination;

    listePagination.subscribe(value => {
      pagination = value
    })()

    if (pagination.offset > 0) {
      const newOffset = Math.max(0, pagination.offset - pagination.limit)

      dispatch('pageChange', { offset: newOffset })
    }
  }

  function handleNextPage() {
    let pagination;

    listePagination.subscribe(value => {
      pagination = value
    })()

    if (pagination.offset + pagination.limit < pagination.total) {
      const newOffset = pagination.offset + pagination.limit

      dispatch('pageChange', { offset: newOffset })
    }
  }
</script>

<div class="table-card">
  {#if $liste.length === 0}
    <div class="empty-state">
      <i class="bi bi-list-ul"></i>
      <h3>Nessuna lista trovata</h3>
      <p>Crea la tua prima lista per iniziare a organizzare i contatti</p>
    </div>
  {:else}
    <div class="table-responsive">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>Nome</th>
            <th class="text-center">Stato</th>
            <th class="text-center">Consenso</th>
            <th class="text-center">Contatti</th>
            <th class="text-center">Scadenza</th>
            <th class="text-center">Azioni</th>
          </tr>
        </thead>
        <tbody>
          {#each $liste as lista (lista.id)}
            <tr>
              <td class="fw-medium">
                <button
                  class="btn btn-link p-0 text-start text-decoration-none"
                  onclick={() => handleViewContatti(lista)}
                >
                  {lista.nome}
                </button>
              </td>
              <td class="text-center">
                <span class="badge {getStatoBadgeClass(lista.stato)}">
                  {getStatoLabel(lista.stato)}
                </span>
              </td>
              <td class="text-center">
                {#if lista.consenso}
                  <span class="badge bg-success-subtle text-success">
                    <i class="bi bi-check-circle-fill me-1"></i>
                    Sì
                  </span>
                {:else}
                  <span class="badge bg-secondary-subtle text-secondary">
                    <i class="bi bi-x-circle-fill me-1"></i>
                    No
                  </span>
                {/if}
              </td>
              <td class="text-center">
                <span class="badge bg-primary-subtle text-primary">
                  {lista.contattiCount || 0}
                </span>
              </td>
              <td class="text-center">{formatDate(lista.scadenza)}</td>
              <td class="text-center">
                <div class="btn-group btn-group-sm">
                  <button
                    type="button"
                    class="btn btn-outline-primary"
                    title="Visualizza contatti"
                    onclick={() => handleViewContatti(lista)}
                  >
                    <i class="bi bi-people-fill"></i>
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-secondary"
                    title="Modifica"
                    onclick={() => handleEdit(lista)}
                  >
                    <i class="bi bi-pencil-fill"></i>
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-danger"
                    title="Elimina"
                    onclick={() => handleDelete(lista)}
                  >
                    <i class="bi bi-trash-fill"></i>
                  </button>
                </div>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>

    <div class="pagination-footer">
      <div class="pagination-info">
        Visualizzazione {$listePagination.offset + 1} - {Math.min($listePagination.offset + $listePagination.limit, $listePagination.total)} di {$listePagination.total}
      </div>
      <div class="pagination-controls">
        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          disabled={$listePagination.offset === 0}
          onclick={handlePreviousPage}
        >
          <i class="bi bi-chevron-left"></i>
          Precedente
        </button>
        <button
          type="button"
          class="btn btn-outline-secondary btn-sm"
          disabled={$listePagination.offset + $listePagination.limit >= $listePagination.total}
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
