<script>
  import { onMount } from 'svelte'
  import { executeImport, loading, uploadedFile } from '../store.js'
  import { caricaListe, listeAttive } from '../../liste/store.js'

  let { onBack = () => {}, onReset = () => {} } = $props()

  let mode = $state('new')
  let newListName = $state('')
  let selectedListaId = $state(null)
  let importResult = $state(null)
  let isExecuting = $state(false)

  onMount(async () => {
    // Load available lists
    await caricaListe(1000, 0)

    // Auto-fill new list name from filename
    let file;
    uploadedFile.subscribe(value => {
      file = value
    })()

    if (file && file.name) {
      const baseName = file.name.replace(/\.[^/.]+$/, '')
      newListName = baseName.substring(0, 100)
    }
  })

  async function handleExecute() {
    if (mode === 'new') {
      if (!newListName || newListName.trim().length === 0) {
        alert('Inserisci un nome per la nuova lista')
        return
      }
    } else {
      if (!selectedListaId) {
        alert('Seleziona una lista esistente')
        return
      }
    }

    const confirmed = confirm(
      `Confermi l'importazione dei contatti nella lista?\n\n` +
      `Lista: ${mode === 'new' ? newListName : 'Lista esistente'}\n\n` +
      'Questa operazione non può essere annullata.'
    )

    if (!confirmed) return

    isExecuting = true

    try {
      const result = await executeImport(
        mode === 'existing' ? selectedListaId : null,
        mode === 'new' ? newListName.trim() : null
      )

      importResult = result
    } catch (err) {
      console.error('Errore importazione:', err)
    } finally {
      isExecuting = false
    }
  }

  function handleNewImport() {
    importResult = null
    onReset()
  }
</script>

<div class="step4-container">
  {#if importResult}
    <!-- Success Screen -->
    <div class="success-screen">
      <div class="success-icon">
        <i class="bi bi-check-circle-fill"></i>
      </div>
      <h2>Importazione Completata!</h2>
      <p class="success-message">
        <strong>{importResult.rowsImported}</strong> contatti importati con successo
        nella lista <strong>{importResult.listaName}</strong>
      </p>

      <div class="success-actions">
        <button class="btn btn-primary" onclick={handleNewImport}>
          <i class="bi bi-arrow-clockwise me-2"></i>
          Nuova Importazione
        </button>
        <a href="/liste" class="btn btn-outline-primary">
          <i class="bi bi-list-ul me-2"></i>
          Visualizza Liste
        </a>
      </div>
    </div>
  {:else}
    <!-- Execute Form -->
    <h2 class="step-title">
      <i class="bi bi-play-circle me-2"></i>
      Esegui Importazione
    </h2>
    <p class="step-description">
      Seleziona la lista di destinazione ed esegui l'importazione
    </p>

    <div class="mode-selector">
      <div class="form-check">
        <input
          type="radio"
          id="mode-new"
          class="form-check-input"
          bind:group={mode}
          value="new"
        />
        <label for="mode-new" class="form-check-label">
          <strong>Crea Nuova Lista</strong>
          <small class="d-block text-muted">I contatti verranno aggiunti a una nuova lista</small>
        </label>
      </div>

      <div class="form-check">
        <input
          type="radio"
          id="mode-existing"
          class="form-check-input"
          bind:group={mode}
          value="existing"
        />
        <label for="mode-existing" class="form-check-label">
          <strong>Aggiungi a Lista Esistente</strong>
          <small class="d-block text-muted">I contatti verranno aggiunti a una lista esistente</small>
        </label>
      </div>
    </div>

    {#if mode === 'new'}
      <div class="list-form">
        <label for="listName" class="form-label">
          Nome Nuova Lista <span class="text-danger">*</span>
        </label>
        <input
          type="text"
          id="listName"
          class="form-control"
          bind:value={newListName}
          maxlength="100"
          placeholder="Es: Clienti Q1 2026"
        />
        <div class="form-text">
          {newListName.length}/100 caratteri
        </div>
      </div>
    {:else}
      <div class="list-form">
        <label for="listSelect" class="form-label">
          Seleziona Lista <span class="text-danger">*</span>
        </label>
        {#if $listeAttive.length === 0}
          <div class="alert alert-warning">
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            Nessuna lista attiva disponibile. Crea una nuova lista invece.
          </div>
        {:else}
          <select
            id="listSelect"
            class="form-select"
            bind:value={selectedListaId}
          >
            <option value={null}>-- Seleziona una lista --</option>
            {#each $listeAttive as lista}
              <option value={lista.id}>
                {lista.nome}
                {lista.descrizione ? ` - ${lista.descrizione}` : ''}
                ({lista.contattiCount || 0} contatti)
              </option>
            {/each}
          </select>
        {/if}
      </div>
    {/if}

    <div class="step-actions">
      <button class="btn btn-secondary" onclick={onBack} disabled={isExecuting}>
        <i class="bi bi-arrow-left me-2"></i>
        Indietro
      </button>
      <button
        class="btn btn-success"
        onclick={handleExecute}
        disabled={isExecuting || $loading}
      >
        {#if isExecuting || $loading}
          <span class="spinner-border spinner-border-sm me-2"></span>
          Importazione in corso...
        {:else}
          <i class="bi bi-upload me-2"></i>
          Esegui Importazione
        {/if}
      </button>
    </div>
  {/if}
</div>

<style>
  .step4-container {
    max-width: 700px;
    margin: 0 auto;
  }

  .step-title {
    font-size: 1.5rem;
    font-weight: 600;
    color: #2c3e50;
    margin-bottom: 0.5rem;
  }

  .step-description {
    color: #7f8c8d;
    margin-bottom: 2rem;
  }

  .mode-selector {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    margin-bottom: 2rem;
    padding: 1.5rem;
    background: #f8f9fa;
    border-radius: 8px;
  }

  .form-check {
    padding: 1rem;
    border: 2px solid #dee2e6;
    border-radius: 8px;
    transition: all 0.2s;
  }

  .form-check:has(input:checked) {
    border-color: #667eea;
    background: #f0f4ff;
  }

  .form-check-label {
    cursor: pointer;
    margin-left: 0.5rem;
  }

  .list-form {
    margin-bottom: 2rem;
  }

  .step-actions {
    display: flex;
    justify-content: space-between;
    padding-top: 1.5rem;
    border-top: 1px solid #dee2e6;
  }

  /* Success Screen */
  .success-screen {
    text-align: center;
    padding: 3rem 2rem;
  }

  .success-icon {
    font-size: 5rem;
    color: #28a745;
    margin-bottom: 1.5rem;
    animation: scaleIn 0.5s ease-out;
  }

  @keyframes scaleIn {
    from {
      transform: scale(0);
    }
    to {
      transform: scale(1);
    }
  }

  .success-screen h2 {
    font-size: 2rem;
    font-weight: 700;
    color: #2c3e50;
    margin-bottom: 1rem;
  }

  .success-message {
    font-size: 1.125rem;
    color: #7f8c8d;
    margin-bottom: 2rem;
  }

  .success-actions {
    display: flex;
    justify-content: center;
    gap: 1rem;
    flex-wrap: wrap;
  }
</style>
