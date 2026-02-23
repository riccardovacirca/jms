<script>
  import { listeCampagna, campagnaCorrente, rimuoviListaCampagna } from '../store.js'

  async function handleRemove(lista) {
    let campagna;
    campagnaCorrente.subscribe(value => { campagna = value })()

    if (!confirm(`Sei sicuro di voler rimuovere la lista "${lista.nome}" dalla campagna?\n\nLa lista non verrà eliminata dal sistema.`)) {
      return
    }

    try {
      await rimuoviListaCampagna(campagna.id, lista.listaId)
    } catch (err) {
      alert(`Errore durante la rimozione: ${err.message}`)
    }
  }

  function getStatoBadgeClass(stato) {
    return { 0: 'bg-secondary', 1: 'bg-success', 2: 'bg-primary', 3: 'bg-warning' }[stato] || 'bg-secondary'
  }

  function getStatoLabel(stato) {
    return { 0: 'Bozza', 1: 'Attiva', 2: 'Completata', 3: 'Archiviata' }[stato] || 'Sconosciuto'
  }

  function formatDate(dateString) {
    if (!dateString) return '-'
    const date = new Date(dateString)
    return date.toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' })
  }
</script>

<div class="table-card">
  {#if $listeCampagna.length === 0}
    <div class="empty-state">
      <i class="bi bi-list-ul"></i>
      <h3>Nessuna lista associata</h3>
      <p>Aggiungi una lista per iniziare a popolare questa campagna</p>
    </div>
  {:else}
    <div class="table-responsive">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>Nome Lista</th>
            <th>Descrizione</th>
            <th class="text-center">Stato</th>
            <th class="text-center">Contatti</th>
            <th class="text-center">Aggiunta il</th>
            <th class="text-end">Azioni</th>
          </tr>
        </thead>
        <tbody>
          {#each $listeCampagna as lista (lista.id)}
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
              <td class="text-center text-muted small">{formatDate(lista.createdAt)}</td>
              <td class="text-end">
                <button
                  type="button"
                  class="btn btn-outline-danger btn-sm"
                  title="Rimuovi dalla campagna"
                  onclick={() => handleRemove(lista)}
                >
                  <i class="bi bi-x-lg me-1"></i>
                  Rimuovi
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
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
