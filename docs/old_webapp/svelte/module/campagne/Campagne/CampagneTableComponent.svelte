<script>
  import { campagne, campagneFilters, eliminaCampagna, aggiornaStatoCampagna, showListeCampagna } from '../store.js'

  let { onEdit = () => {} } = $props()

  // Campagne filtrate client-side
  let campagneFiltrate = $state([])

  $effect(() => {
    let filters;
    campagneFilters.subscribe(value => { filters = value })()

    let allCampagne;
    campagne.subscribe(value => { allCampagne = value })()

    campagneFiltrate = allCampagne.filter(c => {
      if (filters.search) {
        const s = filters.search.toLowerCase()
        if (!c.nome?.toLowerCase().includes(s) && !c.descrizione?.toLowerCase().includes(s)) {
          return false
        }
      }
      if (filters.stato !== 'all' && String(c.stato) !== filters.stato) return false
      if (filters.tipo !== 'all' && c.tipo !== filters.tipo) return false
      return true
    })
  })

  async function handleDelete(campagna) {
    if (!confirm(`Sei sicuro di voler eliminare la campagna "${campagna.nome}"?\n\nQuesta operazione non può essere annullata.`)) {
      return
    }
    try {
      await eliminaCampagna(campagna.id)
    } catch (err) {
      alert(`Errore durante l'eliminazione: ${err.message}`)
    }
  }

  async function handleStatoChange(campagna, event) {
    const nuovoStato = Number(event.target.value)
    try {
      await aggiornaStatoCampagna(campagna.id, nuovoStato)
    } catch (err) {
      alert(`Errore aggiornamento stato: ${err.message}`)
    }
  }

  function formatDate(dateString) {
    if (!dateString) return '-'
    const date = new Date(dateString)
    return date.toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' })
  }

  function getStatoBadgeClass(stato) {
    return { 0: 'bg-secondary', 1: 'bg-success', 2: 'bg-primary', 3: 'bg-warning' }[stato] || 'bg-secondary'
  }

  function getStatoLabel(stato) {
    return { 0: 'Bozza', 1: 'Attiva', 2: 'Completata', 3: 'Archiviata' }[stato] || 'Sconosciuto'
  }

  function getTipoBadgeClass(tipo) {
    return tipo === 'inbound' ? 'bg-info' : 'bg-primary'
  }
</script>

<div class="table-card">
  {#if campagneFiltrate.length === 0}
    <div class="empty-state">
      <i class="bi bi-megaphone"></i>
      <h3>Nessuna campagna trovata</h3>
      <p>Crea la tua prima campagna per iniziare a gestire le liste di contatti</p>
    </div>
  {:else}
    <div class="table-responsive">
      <table class="table table-hover">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Tipo</th>
            <th class="text-center">Stato</th>
            <th class="text-center">Inizio</th>
            <th class="text-center">Fine</th>
            <th class="text-center">Azioni</th>
          </tr>
        </thead>
        <tbody>
          {#each campagneFiltrate as campagna (campagna.id)}
            <tr>
              <td>
                <button
                  class="btn btn-link p-0 text-start text-decoration-none fw-medium"
                  onclick={() => showListeCampagna(campagna)}
                >
                  {campagna.nome}
                </button>
                {#if campagna.descrizione}
                  <div class="small text-muted">{campagna.descrizione}</div>
                {/if}
              </td>
              <td>
                <span class="badge {getTipoBadgeClass(campagna.tipo)}">
                  {campagna.tipo?.toUpperCase() || 'OUTBOUND'}
                </span>
              </td>
              <td class="text-center">
                <select
                  class="form-select form-select-sm w-auto mx-auto"
                  value={campagna.stato}
                  onchange={(e) => handleStatoChange(campagna, e)}
                >
                  <option value={0}>Bozza</option>
                  <option value={1}>Attiva</option>
                  <option value={2}>Completata</option>
                  <option value={3}>Archiviata</option>
                </select>
              </td>
              <td class="text-center">{formatDate(campagna.dataInizio)}</td>
              <td class="text-center">{formatDate(campagna.dataFine)}</td>
              <td class="text-center">
                <div class="btn-group btn-group-sm">
                  <button
                    type="button"
                    class="btn btn-outline-primary"
                    title="Gestisci liste"
                    onclick={() => showListeCampagna(campagna)}
                  >
                    <i class="bi bi-list-ul"></i>
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-secondary"
                    title="Modifica"
                    onclick={() => onEdit(campagna)}
                  >
                    <i class="bi bi-pencil-fill"></i>
                  </button>
                  <button
                    type="button"
                    class="btn btn-outline-danger"
                    title="Elimina"
                    onclick={() => handleDelete(campagna)}
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
