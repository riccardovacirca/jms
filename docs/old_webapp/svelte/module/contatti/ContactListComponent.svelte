<script>
  import { onMount } from 'svelte';
  import { contatti, loading, error, pagination, currentView, showView, showEdit } from './store.js';

  function getStatoLabel(stato) {
    let label;

    switch (stato) {
      case 1:
        label = 'Attivo';
        break;
      case 0:
        label = 'Inattivo';
        break;
      case 2:
        label = 'Sospeso';
        break;
      default:
        label = 'Non definito';
    }

    return label;
  }

  function getStatoClass(stato) {
    let className;

    switch (stato) {
      case 1:
        className = 'status-active';
        break;
      case 0:
        className = 'status-inactive';
        break;
      case 2:
        className = 'status-suspended';
        break;
      default:
        className = 'status-undefined';
    }

    return className;
  }

  function getDisplayName(contatto) {
    let name;

    if (contatto.ragioneSociale) {
      name = contatto.ragioneSociale;
      return name;
    }

    if (contatto.cognome && contatto.nome) {
      name = `${contatto.cognome} ${contatto.nome}`;
      return name;
    }

    if (contatto.cognome) {
      name = contatto.cognome;
      return name;
    }

    if (contatto.nome) {
      name = contatto.nome;
      return name;
    }

    name = `Contatto #${contatto.id}`;
    return name;
  }

  async function loadContatti() {
    let response;
    let data;

    loading.set(true);
    error.set(null);

    try {
      response = await fetch('/api/liste/contatti?limit=50&offset=0', {
        credentials: 'include'
      });

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

  async function loadPage(offset) {
    let currentPagination;
    let response;
    let data;

    currentPagination = $pagination;

    loading.set(true);
    error.set(null);

    try {
      response = await fetch(`/api/liste/contatti?limit=${currentPagination.limit}&offset=${offset}`);

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
    } finally {
      loading.set(false);
    }
  }

  async function handleDelete(id) {
    let confirmDelete;
    let response;

    confirmDelete = confirm('Sei sicuro di voler eliminare questo contatto?');

    if (!confirmDelete) {
      return;
    }

    try {
      response = await fetch(`/api/liste/contatti/${id}`, {
        method: 'DELETE'
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      loadContatti();
    } catch (e) {
      alert(`Errore durante l'eliminazione: ${e.message}`);
    }
  }

  async function handleView(id) {
    let response;
    let data;
    let contatto;

    try {
      response = await fetch(`/api/liste/contatti/${id}`);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      data = await response.json();

      if (data.err) {
        throw new Error(data.log || 'Errore durante il caricamento');
      }

      contatto = data.out;
      showEdit(contatto);
    } catch (e) {
      alert(`Errore: ${e.message}`);
    }
  }

  function handlePrevPage() {
    let currentPagination;
    let newOffset;

    currentPagination = $pagination;

    if (currentPagination.offset > 0) {
      newOffset = Math.max(0, currentPagination.offset - currentPagination.limit);
      loadPage(newOffset);
    }
  }

  function handleNextPage() {
    let currentPagination;
    let newOffset;

    currentPagination = $pagination;

    if (currentPagination.hasNext) {
      newOffset = currentPagination.offset + currentPagination.limit;
      loadPage(newOffset);
    }
  }

  onMount(() => {
    loadContatti();
  });

  // Ricarica lista quando si torna alla vista lista
  $: if ($currentView === 'list') {
    loadContatti();
  }
</script>

<div class="contact-list-container">
  {#if $error}
    <div class="error-card">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
        <path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <div>
        <strong>Errore</strong>
        <p>{$error}</p>
      </div>
    </div>
  {:else if $loading}
    <div class="loading-card">
      <div class="spinner"></div>
      <p>Caricamento contatti...</p>
    </div>
  {:else if $contatti.length === 0}
    <div class="empty-state">
      <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
        <circle cx="32" cy="32" r="30" stroke="currentColor" stroke-width="2" opacity="0.2"/>
        <path d="M32 20v24M20 32h24" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.2"/>
      </svg>
      <h3>Nessun contatto trovato</h3>
      <p>Inizia creando un nuovo contatto o importandone alcuni</p>
    </div>
  {:else}
    <div class="contact-table-wrapper">
      <table class="contact-table">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Email</th>
            <th>Telefono</th>
            <th>Città</th>
            <th class="text-center">Stato</th>
            <th class="text-center">Liste</th>
            <th class="text-center">Azioni</th>
          </tr>
        </thead>
        <tbody>
          {#each $contatti as contatto (contatto.id)}
            <tr>
              <td class="name-cell">
                <div class="name-wrapper">
                  <span class="name">{getDisplayName(contatto)}</span>
                  {#if contatto.blacklist}
                    <span class="blacklist-badge" title="Blacklist">
                      <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                        <path d="M7 1L9 5L13 5.5L10 8.5L10.5 13L7 11L3.5 13L4 8.5L1 5.5L5 5L7 1Z" fill="currentColor"/>
                      </svg>
                    </span>
                  {/if}
                </div>
              </td>
              <td>{contatto.email || '-'}</td>
              <td>{contatto.telefono || '-'}</td>
              <td>{contatto.citta || '-'}</td>
              <td class="text-center">
                <span class="status-badge {getStatoClass(contatto.stato)}">
                  {getStatoLabel(contatto.stato)}
                </span>
              </td>
              <td class="text-center">
                {#if contatto.listeCount}
                  <span class="badge-count">{contatto.listeCount}</span>
                {:else}
                  -
                {/if}
              </td>
              <td class="text-center">
                <button class="action-btn view-btn" onclick={() => handleView(contatto.id)} title="Visualizza">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path d="M8 3C4.5 3 1.7 5.3 1 8c.7 2.7 3.5 5 7 5s6.3-2.3 7-5c-.7-2.7-3.5-5-7-5z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    <circle cx="8" cy="8" r="2" stroke="currentColor" stroke-width="1.5"/>
                  </svg>
                </button>
                <button class="action-btn delete-btn" onclick={() => handleDelete(contatto.id)} title="Elimina">
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path d="M2 4h12M5.5 4V2.5a1 1 0 011-1h3a1 1 0 011 1V4M7 7v4M9 7v4M3.5 4l.5 9a1 1 0 001 1h6a1 1 0 001-1l.5-9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>

    <!-- Paginazione -->
    <div class="pagination">
      <div class="pagination-info">
        Visualizzati {$pagination.offset + 1}-{Math.min($pagination.offset + $pagination.limit, $pagination.total)} di {$pagination.total} contatti
      </div>
      <div class="pagination-controls">
        <button
          class="pagination-btn"
          onclick={handlePrevPage}
          disabled={$pagination.offset === 0 || $loading}
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M10 12L6 8l4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          Precedente
        </button>
        <button
          class="pagination-btn"
          onclick={handleNextPage}
          disabled={!$pagination.hasNext || $loading}
        >
          Successiva
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 12l4-4-4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>
  {/if}
</div>

<style>
  .contact-list-container {
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }

  .error-card {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 2rem;
    background: #fee;
    border: 1px solid #fcc;
    color: #c33;
  }

  .error-card svg {
    flex-shrink: 0;
  }

  .error-card strong {
    display: block;
    margin-bottom: 0.25rem;
  }

  .error-card p {
    margin: 0;
    font-size: 0.9rem;
  }

  .loading-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem;
  }

  .spinner {
    width: 40px;
    height: 40px;
    border: 4px solid #ecf0f1;
    border-top-color: #3498db;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }

  .loading-card p {
    margin: 1rem 0 0 0;
    color: #7f8c8d;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem;
    color: #95a5a6;
  }

  .empty-state h3 {
    margin: 1rem 0 0.5rem 0;
    font-size: 1.25rem;
    color: #7f8c8d;
  }

  .empty-state p {
    margin: 0;
    font-size: 0.95rem;
  }

  .contact-table-wrapper {
    overflow-x: auto;
  }

  .contact-table {
    width: 100%;
    border-collapse: collapse;
  }

  .contact-table thead {
    background: #f8f9fa;
    border-bottom: 2px solid #e0e6ed;
  }

  .contact-table th {
    padding: 1rem;
    text-align: left;
    font-size: 0.85rem;
    font-weight: 600;
    color: #5a6c7d;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .text-center {
    text-align: center;
  }

  .contact-table tbody tr {
    border-bottom: 1px solid #f0f2f5;
    transition: background-color 0.2s;
  }

  .contact-table tbody tr:hover {
    background: #f8f9fa;
  }

  .contact-table td {
    padding: 1rem;
    font-size: 0.9rem;
    color: #2c3e50;
  }

  .name-cell {
    font-weight: 500;
  }

  .name-wrapper {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .name {
    color: #2c3e50;
  }

  .blacklist-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: #e74c3c;
  }

  .status-badge {
    display: inline-block;
    padding: 0.375rem 0.75rem;
    border-radius: 12px;
    font-size: 0.8rem;
    font-weight: 500;
    white-space: nowrap;
  }

  .status-active {
    background: #d4edda;
    color: #155724;
  }

  .status-inactive {
    background: #f8d7da;
    color: #721c24;
  }

  .status-suspended {
    background: #fff3cd;
    color: #856404;
  }

  .status-undefined {
    background: #e2e3e5;
    color: #383d41;
  }


  .badge-count {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 24px;
    height: 24px;
    padding: 0 0.5rem;
    background: #3498db;
    color: white;
    border-radius: 12px;
    font-size: 0.8rem;
    font-weight: 600;
  }


  .action-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;
    margin: 0 0.25rem;
  }

  .view-btn {
    background: #e3f2fd;
    color: #2196f3;
  }

  .view-btn:hover {
    background: #2196f3;
    color: white;
    transform: translateY(-1px);
  }

  .delete-btn {
    background: #ffebee;
    color: #f44336;
  }

  .delete-btn:hover {
    background: #f44336;
    color: white;
    transform: translateY(-1px);
  }

  .pagination {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-top: 1px solid #e0e6ed;
    background: #f8f9fa;
  }

  .pagination-info {
    font-size: 0.9rem;
    color: #5a6c7d;
  }

  .pagination-controls {
    display: flex;
    gap: 0.5rem;
  }

  .pagination-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.625rem 1rem;
    background: white;
    color: #2c3e50;
    border: 1px solid #cbd5e0;
    border-radius: 6px;
    font-size: 0.9rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .pagination-btn:hover:not(:disabled) {
    background: #3498db;
    color: white;
    border-color: #3498db;
    transform: translateY(-1px);
  }

  .pagination-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  @media (max-width: 768px) {
    .contact-table th,
    .contact-table td {
      padding: 0.75rem 0.5rem;
      font-size: 0.85rem;
    }

    .pagination {
      flex-direction: column;
      gap: 1rem;
      align-items: stretch;
    }

    .pagination-controls {
      justify-content: stretch;
    }

    .pagination-btn {
      flex: 1;
      justify-content: center;
    }
  }
</style>
