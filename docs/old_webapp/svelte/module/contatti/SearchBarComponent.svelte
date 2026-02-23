<script>
  import { searchQuery, contatti, loading, error, pagination, showNew } from './store.js';

  let searchInput;
  let searchTimeout;

  searchInput = '';

  async function performSearch() {
    let query;
    let response;
    let data;

    query = searchInput.trim();

    if (query.length === 0) {
      loadContatti();
      return;
    }

    if (query.length < 2) {
      return;
    }

    loading.set(true);
    error.set(null);

    try {
      response = await fetch(`/api/liste/contatti/search?q=${encodeURIComponent(query)}&limit=50`, {
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      data = await response.json();

      contatti.set(data.out || []);
      searchQuery.set(query);

      pagination.set({
        offset: 0,
        limit: 50,
        total: (data.out || []).length,
        hasNext: false
      });
    } catch (e) {
      error.set(e.message);
      contatti.set([]);
    } finally {
      loading.set(false);
    }
  }

  async function loadContatti() {
    let response;
    let data;

    loading.set(true);
    error.set(null);
    searchQuery.set('');

    try {
      response = await fetch('/api/liste/contatti?limit=50&offset=0');

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      data = await response.json();

      contatti.set(data.out.items || []);
      pagination.set({
        offset: data.out.offset || 0,
        limit: data.out.limit || 50,
        total: data.out.total || 0,
        hasNext: data.out.hasNext || false
      });
    } catch (e) {
      error.set(e.message);
      contatti.set([]);
    } finally {
      loading.set(false);
    }
  }

  function handleSearchInput() {
    clearTimeout(searchTimeout);

    searchTimeout = setTimeout(() => {
      performSearch();
    }, 500);
  }

  function handleClearSearch() {
    searchInput = '';
    loadContatti();
  }

  function handleNuovo() {
    showNew();
  }

  function handleImporta() {
    alert('Funzionalità "Importa Contatti" da implementare');
  }
</script>

<div class="search-bar">
  <div class="search-input-wrapper">
    <svg class="search-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
      <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="2"/>
      <path d="M12.5 12.5L17 17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
    </svg>
    <input
      type="text"
      bind:value={searchInput}
      oninput={handleSearchInput}
      placeholder="Cerca per nome, cognome, email, telefono..."
      class="search-input"
    />
    {#if searchInput.length > 0}
      <button class="clear-btn" onclick={handleClearSearch} type="button" aria-label="Cancella ricerca">
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
    {/if}
  </div>

  <div class="action-buttons">
    <button class="btn btn-primary" onclick={handleNuovo}>
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M8 3V13M3 8H13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      Nuovo
    </button>
    <button class="btn btn-secondary" onclick={handleImporta}>
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M14 10v3a1 1 0 01-1 1H3a1 1 0 01-1-1v-3M8 2v9M8 11l-3-3M8 11l3-3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      Importa
    </button>
  </div>
</div>

<style>
  .search-bar {
    display: flex;
    gap: 1rem;
    margin-bottom: 1.5rem;
    align-items: center;
  }

  .search-input-wrapper {
    position: relative;
    flex: 1;
    max-width: 600px;
  }

  .search-icon {
    position: absolute;
    left: 1rem;
    top: 50%;
    transform: translateY(-50%);
    color: #7f8c8d;
    pointer-events: none;
  }

  .search-input {
    width: 100%;
    padding: 0.875rem 1rem 0.875rem 3rem;
    border: 2px solid #e0e6ed;
    border-radius: 8px;
    font-size: 0.95rem;
    background: white;
    transition: all 0.2s;
  }

  .search-input:focus {
    outline: none;
    border-color: #3498db;
    box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
  }

  .clear-btn {
    position: absolute;
    right: 0.75rem;
    top: 50%;
    transform: translateY(-50%);
    background: #ecf0f1;
    border: none;
    border-radius: 4px;
    padding: 0.375rem;
    cursor: pointer;
    color: #7f8c8d;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
  }

  .clear-btn:hover {
    background: #dfe6e9;
    color: #2c3e50;
  }

  .action-buttons {
    display: flex;
    gap: 0.75rem;
  }

  .btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.875rem 1.5rem;
    border: none;
    border-radius: 8px;
    font-size: 0.95rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
  }

  .btn-primary {
    background: #3498db;
    color: white;
  }

  .btn-primary:hover {
    background: #2980b9;
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(52, 152, 219, 0.3);
  }

  .btn-secondary {
    background: white;
    color: #2c3e50;
    border: 2px solid #e0e6ed;
  }

  .btn-secondary:hover {
    background: #f8f9fa;
    border-color: #cbd5e0;
    transform: translateY(-1px);
  }

  @media (max-width: 768px) {
    .search-bar {
      flex-direction: column;
      align-items: stretch;
    }

    .search-input-wrapper {
      max-width: none;
    }

    .action-buttons {
      justify-content: stretch;
    }

    .btn {
      flex: 1;
      justify-content: center;
    }
  }
</style>
