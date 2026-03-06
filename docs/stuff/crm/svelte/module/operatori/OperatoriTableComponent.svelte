<script>
  import { onMount } from 'svelte';
  import { operatori, loading, error, showNew, showEdit, caricaOperatori, eliminaOperatore, selectedOperatore, showCampagneModal } from './store.js';
  import CampagneModalComponent from './CampagneModalComponent.svelte';

  onMount(() => {
    caricaOperatori();
  });

  function handleNew() {
    showNew();
  }

  function handleEdit(operatore) {
    showEdit(operatore);
  }

  async function handleDelete(operatore) {
    if (confirm(`Sei sicuro di voler eliminare l'operatore ${operatore.nome} ${operatore.cognome}?`)) {
      await eliminaOperatore(operatore.id);
    }
  }

  function handleGestisciCampagne(operatore) {
    selectedOperatore.set(operatore);
    showCampagneModal.set(true);
  }

  function getStatoLabel(statoAttuale) {
    return statoAttuale || 'OFFLINE';
  }

  function getStatoClass(statoAttuale) {
    switch(statoAttuale) {
      case 'ONLINE': return 'status-online';
      case 'PAUSA': return 'status-pause';
      case 'OFFLINE': return 'status-offline';
      default: return 'status-offline';
    }
  }
</script>

<div class="operatori-table-container">
  <div class="table-header">
    <button class="btn-primary" on:click={handleNew}>
      + Nuovo Operatore
    </button>
  </div>

  {#if $loading}
    <div class="loading">Caricamento in corso...</div>
  {:else if $error}
    <div class="error">Errore: {$error}</div>
  {:else if $operatori.length === 0}
    <div class="empty-state">
      <p>Nessun operatore presente</p>
      <button class="btn-primary" on:click={handleNew}>
        Crea il primo operatore
      </button>
    </div>
  {:else}
    <div class="table-wrapper">
      <table class="operatori-table">
        <thead>
          <tr>
            <th>Nome</th>
            <th>Cognome</th>
            <th>Email</th>
            <th>Username</th>
            <th>Telefono</th>
            <th>Stato</th>
            <th>Azioni</th>
          </tr>
        </thead>
        <tbody>
          {#each $operatori as operatore}
            <tr>
              <td>{operatore.nome}</td>
              <td>{operatore.cognome}</td>
              <td>{operatore.email}</td>
              <td>{operatore.username}</td>
              <td>{operatore.telefono || '-'}</td>
              <td>
                <span class="status-badge {getStatoClass(operatore.statoAttuale)}">
                  {getStatoLabel(operatore.statoAttuale)}
                </span>
              </td>
              <td class="actions-cell">
                <button class="btn-icon" on:click={() => handleEdit(operatore)} title="Modifica">
                  ✏️
                </button>
                <button class="btn-icon" on:click={() => handleGestisciCampagne(operatore)} title="Gestisci Campagne">
                  📋
                </button>
                <button class="btn-icon btn-danger" on:click={() => handleDelete(operatore)} title="Elimina">
                  🗑️
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

<CampagneModalComponent />

<style>
  .operatori-table-container {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    padding: 1.5rem;
  }

  .table-header {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 1.5rem;
  }

  .btn-primary {
    padding: 0.75rem 1.5rem;
    background: #3498db;
    color: white;
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-primary:hover {
    background: #2980b9;
  }

  .table-wrapper {
    overflow-x: auto;
  }

  .operatori-table {
    width: 100%;
    border-collapse: collapse;
  }

  .operatori-table thead {
    background: #f8f9fa;
  }

  .operatori-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    color: #2c3e50;
    border-bottom: 2px solid #e9ecef;
  }

  .operatori-table td {
    padding: 1rem;
    border-bottom: 1px solid #e9ecef;
    color: #495057;
  }

  .operatori-table tbody tr:hover {
    background: #f8f9fa;
  }

  .actions-cell {
    display: flex;
    gap: 0.5rem;
  }

  .btn-icon {
    padding: 0.5rem;
    background: transparent;
    border: none;
    cursor: pointer;
    font-size: 1.2rem;
    transition: transform 0.2s;
  }

  .btn-icon:hover {
    transform: scale(1.2);
  }

  .btn-danger:hover {
    filter: brightness(1.2);
  }

  .status-badge {
    padding: 0.25rem 0.75rem;
    border-radius: 12px;
    font-size: 0.875rem;
    font-weight: 500;
  }

  .status-online {
    background: #d4edda;
    color: #155724;
  }

  .status-pause {
    background: #fff3cd;
    color: #856404;
  }

  .status-offline {
    background: #f8d7da;
    color: #721c24;
  }

  .loading, .error, .empty-state {
    text-align: center;
    padding: 3rem;
    color: #7f8c8d;
  }

  .error {
    color: #e74c3c;
  }

  .empty-state p {
    margin-bottom: 1rem;
    font-size: 1.1rem;
  }
</style>
