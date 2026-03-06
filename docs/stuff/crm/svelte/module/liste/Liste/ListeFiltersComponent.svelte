<script>
  import { listeFilters } from '../store.js'

  let { onFilterChange = () => {} } = $props()

  let search = $state('')
  let stato = $state('all')
  let attiva = $state('all')

  $effect(() => {
    listeFilters.subscribe(value => {
      search = value.search
      stato = value.stato
      attiva = value.attiva
    })()
  })

  function handleSearchChange(event) {
    search = event.target.value

    listeFilters.update(f => ({
      ...f,
      search
    }))

    onFilterChange()
  }

  function handleStatoChange(event) {
    stato = event.target.value

    listeFilters.update(f => ({
      ...f,
      stato
    }))

    onFilterChange()
  }

  function handleAttivaChange(event) {
    attiva = event.target.value

    listeFilters.update(f => ({
      ...f,
      attiva
    }))

    onFilterChange()
  }

  function handleReset() {
    search = ''
    stato = 'all'
    attiva = 'all'

    listeFilters.set({
      search: '',
      stato: 'all',
      attiva: 'all'
    })

    onFilterChange()
  }
</script>

<div class="filters-card">
  <div class="filters-grid">
    <div class="filter-item">
      <label for="search" class="form-label">
        <i class="bi bi-search me-1"></i>
        Cerca
      </label>
      <input
        type="text"
        id="search"
        class="form-control"
        placeholder="Nome o descrizione..."
        value={search}
        oninput={handleSearchChange}
      />
    </div>

    <div class="filter-item">
      <label for="stato" class="form-label">
        <i class="bi bi-flag me-1"></i>
        Stato
      </label>
      <select
        id="stato"
        class="form-select"
        value={stato}
        onchange={handleStatoChange}
      >
        <option value="all">Tutti gli stati</option>
        <option value="0">Bozza</option>
        <option value="1">Attiva</option>
        <option value="2">Completata</option>
        <option value="3">Archiviata</option>
      </select>
    </div>

    <div class="filter-item">
      <label for="attiva" class="form-label">
        <i class="bi bi-toggle-on me-1"></i>
        Attiva
      </label>
      <select
        id="attiva"
        class="form-select"
        value={attiva}
        onchange={handleAttivaChange}
      >
        <option value="all">Tutte</option>
        <option value="true">Solo attive</option>
        <option value="false">Solo disattivate</option>
      </select>
    </div>

    <div class="filter-actions">
      <button
        type="button"
        class="btn btn-outline-secondary w-100"
        onclick={handleReset}
      >
        <i class="bi bi-arrow-clockwise me-1"></i>
        Reset
      </button>
    </div>
  </div>
</div>

<style>
  .filters-card {
    background: white;
    border-radius: 8px;
    padding: 1.5rem;
    margin-bottom: 1.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .filters-grid {
    display: grid;
    grid-template-columns: 2fr 1fr 1fr auto;
    gap: 1rem;
    align-items: end;
  }

  .filter-item label {
    font-size: 0.875rem;
    font-weight: 500;
    color: #495057;
    margin-bottom: 0.5rem;
  }

  .filter-actions {
    padding-bottom: 0.125rem;
  }

  @media (max-width: 1024px) {
    .filters-grid {
      grid-template-columns: 1fr 1fr;
    }

    .filter-item:first-child {
      grid-column: 1 / -1;
    }

    .filter-actions {
      grid-column: 1 / -1;
    }
  }

  @media (max-width: 640px) {
    .filters-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
