<script>
  import { onMount } from 'svelte';
  import { selectedOperatore, showCampagneModal, campagneOperatore, caricaCampagneOperatore, associaCampagna, rimuoviCampagna } from './store.js';

  let tutteCampagne = [];
  let loadingCampagne = false;
  let showAddForm = false;
  let selectedCampagnaId = null;

  $: if ($showCampagneModal && $selectedOperatore) {
    loadData();
  }

  async function loadData() {
    await caricaCampagneOperatore($selectedOperatore.id);
    await loadAllCampagne();
  }

  async function loadAllCampagne() {
    loadingCampagne = true;
    try {
      const response = await fetch('/api/campagne', {
        credentials: 'include'
      });
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      tutteCampagne = data.out || [];
    } catch (e) {
      console.error('Errore caricamento campagne:', e);
    } finally {
      loadingCampagne = false;
    }
  }

  function closeModal() {
    showCampagneModal.set(false);
    showAddForm = false;
    selectedCampagnaId = null;
  }

  function handleAddCampagna() {
    showAddForm = true;
  }

  async function handleAssociaCampagna() {
    if (!selectedCampagnaId) {
      return;
    }

    const success = await associaCampagna($selectedOperatore.id, selectedCampagnaId);
    if (success) {
      showAddForm = false;
      selectedCampagnaId = null;
    }
  }

  async function handleRimuoviCampagna(campagnaId) {
    if (confirm('Sei sicuro di voler rimuovere questa campagna dall\'operatore?')) {
      await rimuoviCampagna($selectedOperatore.id, campagnaId);
    }
  }

  function getCampagnaNome(campagnaId) {
    const campagna = tutteCampagne.find(c => c.id === campagnaId);
    return campagna?.nome || `Campagna #${campagnaId}`;
  }

  function getCampagneDisponibili() {
    const campagneAssegnateIds = $campagneOperatore.map(c => c.campagnaId);
    return tutteCampagne.filter(c => !campagneAssegnateIds.includes(c.id));
  }

  function handleOverlayKeydown(event) {
    if (event.key === 'Escape') {
      closeModal();
    }
  }

  function handleContentKeydown(event) {
    // Prevent propagation to allow content interaction
    event.stopPropagation();
  }
</script>

{#if $showCampagneModal && $selectedOperatore}
  <div class="modal-overlay" on:click={closeModal} on:keydown={handleOverlayKeydown} role="button" tabindex="-1">
    <div class="modal-content" on:click|stopPropagation on:keydown={handleContentKeydown} role="dialog" aria-labelledby="modal-title" aria-modal="true" tabindex="0">
      <div class="modal-header">
        <h2 id="modal-title">Campagne di {$selectedOperatore.nome} {$selectedOperatore.cognome}</h2>
        <button class="btn-close" on:click={closeModal} aria-label="Chiudi modale">✕</button>
      </div>

      <div class="modal-body">
        {#if $campagneOperatore.length === 0}
          <div class="empty-state">
            <p>Nessuna campagna assegnata</p>
          </div>
        {:else}
          <div class="campagne-list">
            {#each $campagneOperatore as campagna}
              <div class="campagna-item">
                <div class="campagna-info">
                  <strong>{getCampagnaNome(campagna.campagnaId)}</strong>
                  <span class="campagna-date">
                    Assegnata il {new Date(campagna.createdAt).toLocaleDateString('it-IT')}
                  </span>
                </div>
                <button
                  class="btn-remove"
                  on:click={() => handleRimuoviCampagna(campagna.campagnaId)}
                  title="Rimuovi campagna"
                >
                  🗑️
                </button>
              </div>
            {/each}
          </div>
        {/if}

        {#if showAddForm}
          <div class="add-form">
            <h3>Aggiungi Campagna</h3>
            <div class="form-group">
              <label for="campagna-select">Seleziona campagna</label>
              <select id="campagna-select" bind:value={selectedCampagnaId}>
                <option value={null}>-- Seleziona una campagna --</option>
                {#each getCampagneDisponibili() as campagna}
                  <option value={campagna.id}>{campagna.nome}</option>
                {/each}
              </select>
            </div>
            <div class="form-actions">
              <button class="btn-secondary" on:click={() => { showAddForm = false; selectedCampagnaId = null; }}>
                Annulla
              </button>
              <button class="btn-primary" on:click={handleAssociaCampagna} disabled={!selectedCampagnaId}>
                Aggiungi
              </button>
            </div>
          </div>
        {:else}
          <button class="btn-add" on:click={handleAddCampagna}>
            + Aggiungi Campagna
          </button>
        {/if}
      </div>
    </div>
  </div>
{/if}

<style>
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  .modal-content {
    background: white;
    border-radius: 8px;
    width: 90%;
    max-width: 600px;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e9ecef;
  }

  .modal-header h2 {
    margin: 0;
    font-size: 1.5rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .btn-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: #7f8c8d;
    padding: 0;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .btn-close:hover {
    color: #2c3e50;
  }

  .modal-body {
    padding: 1.5rem;
    overflow-y: auto;
  }

  .empty-state {
    text-align: center;
    padding: 2rem;
    color: #7f8c8d;
  }

  .campagne-list {
    margin-bottom: 1.5rem;
  }

  .campagna-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    background: #f8f9fa;
    border-radius: 4px;
    margin-bottom: 0.75rem;
  }

  .campagna-info {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .campagna-info strong {
    color: #2c3e50;
  }

  .campagna-date {
    font-size: 0.875rem;
    color: #7f8c8d;
  }

  .btn-remove {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 1.2rem;
    padding: 0.5rem;
    transition: transform 0.2s;
  }

  .btn-remove:hover {
    transform: scale(1.2);
  }

  .add-form {
    background: #f8f9fa;
    padding: 1.5rem;
    border-radius: 4px;
    margin-top: 1rem;
  }

  .add-form h3 {
    margin: 0 0 1rem 0;
    font-size: 1.25rem;
    color: #2c3e50;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #2c3e50;
  }

  .form-group select {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #ced4da;
    border-radius: 4px;
    font-size: 1rem;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
  }

  .btn-add, .btn-primary, .btn-secondary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-add {
    background: #3498db;
    color: white;
    width: 100%;
  }

  .btn-add:hover {
    background: #2980b9;
  }

  .btn-primary {
    background: #3498db;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2980b9;
  }

  .btn-primary:disabled {
    background: #bdc3c7;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: #95a5a6;
    color: white;
  }

  .btn-secondary:hover {
    background: #7f8c8d;
  }
</style>
