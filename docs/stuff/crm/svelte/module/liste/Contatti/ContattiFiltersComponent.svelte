<script>
  import { contattiFilters } from '../store.js'

  let search;
  let stato;
  let blacklist;

  search = ''
  stato = 'all'
  blacklist = 'all'

  contattiFilters.subscribe(value => {
    search = value.search
    stato = value.stato
    blacklist = value.blacklist
  })

  function handleSearchChange(event) {
    search = event.target.value

    contattiFilters.update(f => ({
      ...f,
      search
    }))
  }

  function handleStatoChange(event) {
    stato = event.target.value

    contattiFilters.update(f => ({
      ...f,
      stato
    }))
  }

  function handleBlacklistChange(event) {
    blacklist = event.target.value

    contattiFilters.update(f => ({
      ...f,
      blacklist
    }))
  }

  function handleReset() {
    search = ''
    stato = 'all'
    blacklist = 'all'

    contattiFilters.set({
      search: '',
      stato: 'all',
      blacklist: 'all'
    })
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
        placeholder="Nome, telefono, email..."
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
        <option value="new">Nuovo</option>
        <option value="contacted">Contattato</option>
        <option value="qualified">Qualificato</option>
        <option value="converted">Convertito</option>
      </select>
    </div>

    <div class="filter-item">
      <label for="blacklist" class="form-label">
        <i class="bi bi-shield-x me-1"></i>
        Blacklist
      </label>
      <select
        id="blacklist"
        class="form-select"
        value={blacklist}
        onchange={handleBlacklistChange}
      >
        <option value="all">Tutti</option>
        <option value="false">Escludi blacklist</option>
        <option value="true">Solo blacklist</option>
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
