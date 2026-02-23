<script>
  import { campagneFilters, campagne, caricaCampagne } from '../store.js'

  let search = $state('')
  let stato = $state('all')
  let tipo = $state('all')

  function handleSearchChange(event) {
    search = event.target.value
    campagneFilters.update(f => ({ ...f, search }))
  }

  function handleStatoChange(event) {
    stato = event.target.value
    campagneFilters.update(f => ({ ...f, stato }))
  }

  function handleTipoChange(event) {
    tipo = event.target.value
    campagneFilters.update(f => ({ ...f, tipo }))
  }

  function handleReset() {
    search = ''
    stato = 'all'
    tipo = 'all'
    campagneFilters.set({ search: '', stato: 'all', tipo: 'all' })
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
      <select id="stato" class="form-select" value={stato} onchange={handleStatoChange}>
        <option value="all">Tutti gli stati</option>
        <option value="0">Bozza</option>
        <option value="1">Attiva</option>
        <option value="2">Completata</option>
        <option value="3">Archiviata</option>
      </select>
    </div>

    <div class="filter-item">
      <label for="tipo" class="form-label">
        <i class="bi bi-telephone me-1"></i>
        Tipo
      </label>
      <select id="tipo" class="form-select" value={tipo} onchange={handleTipoChange}>
        <option value="all">Tutti i tipi</option>
        <option value="outbound">Outbound</option>
        <option value="inbound">Inbound</option>
      </select>
    </div>

    <div class="filter-actions">
      <button type="button" class="btn btn-outline-secondary w-100" onclick={handleReset}>
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
