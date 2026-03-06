<script>
  import { onMount } from 'svelte'
  import { salvaLista, loading, error } from '../store.js'

  let { lista = null, onSave = () => {}, onCancel = () => {} } = $props()

  let formData = $state({
    nome: '',
    descrizione: '',
    consenso: false,
    stato: 1,
    scadenza: ''
  })

  let validationErrors = $state({})

  function initForm() {
    if (lista) {
      formData = {
        nome: lista.nome || '',
        descrizione: lista.descrizione || '',
        consenso: lista.consenso || false,
        stato: lista.stato !== undefined ? lista.stato : 1,
        scadenza: lista.scadenza || ''
      }
    }
  }

  function validateForm() {
    const errors = {}

    if (!formData.nome || formData.nome.trim().length === 0) {
      errors.nome = 'Il nome è obbligatorio'
    }

    if (formData.nome && formData.nome.length > 100) {
      errors.nome = 'Il nome non può superare i 100 caratteri'
    }

    if (formData.descrizione && formData.descrizione.length > 500) {
      errors.descrizione = 'La descrizione non può superare i 500 caratteri'
    }

    validationErrors = errors

    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()

    if (!validateForm()) {
      return
    }

    try {
      const listaDto = {
        ...formData,
        nome: formData.nome.trim(),
        descrizione: formData.descrizione ? formData.descrizione.trim() : null
      }

      if (lista && lista.id) {
        listaDto.id = lista.id
      }

      await salvaLista(listaDto)

      onSave()
    } catch (err) {
      console.error('Errore nel salvataggio della lista:', err)
    }
  }

  function handleCancel() {
    onCancel()
  }

  onMount(() => {
    initForm()
  })
</script>

<div class="form-view">
  <div class="form-header">
    <div>
      <h2>
        {#if lista}
          <i class="bi bi-pencil me-2"></i>
          Modifica Lista
        {:else}
          <i class="bi bi-plus-lg me-2"></i>
          Nuova Lista
        {/if}
      </h2>
      <p class="text-muted">Compila i campi per {lista ? 'modificare' : 'creare'} la lista</p>
    </div>
  </div>

  {#if $error}
    <div class="alert alert-danger">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      {$error}
    </div>
  {/if}

  <div class="form-card">
    <form onsubmit={handleSubmit}>
      <div class="row">
        <div class="col-md-6 mb-3">
          <label for="nome" class="form-label">
            Nome <span class="text-danger">*</span>
          </label>
          <input
            type="text"
            id="nome"
            class="form-control {validationErrors.nome ? 'is-invalid' : ''}"
            bind:value={formData.nome}
            maxlength="100"
            placeholder="Es: Clienti Q1 2026"
            disabled={$loading}
          />
          {#if validationErrors.nome}
            <div class="invalid-feedback">{validationErrors.nome}</div>
          {/if}
        </div>

        <div class="col-md-6 mb-3">
          <label for="stato" class="form-label">Stato</label>
          <select
            id="stato"
            class="form-select"
            bind:value={formData.stato}
            disabled={$loading}
          >
            <option value={0}>Bozza</option>
            <option value={1}>Attiva</option>
            <option value={2}>Completata</option>
            <option value={3}>Archiviata</option>
          </select>
        </div>
      </div>

      <div class="mb-3">
        <label for="descrizione" class="form-label">Descrizione</label>
        <textarea
          id="descrizione"
          class="form-control {validationErrors.descrizione ? 'is-invalid' : ''}"
          bind:value={formData.descrizione}
          rows="3"
          maxlength="500"
          placeholder="Descrizione opzionale della lista..."
          disabled={$loading}
        ></textarea>
        {#if validationErrors.descrizione}
          <div class="invalid-feedback">{validationErrors.descrizione}</div>
        {/if}
        <div class="form-text">
          {formData.descrizione.length}/500 caratteri
        </div>
      </div>

      <div class="row">
        <div class="col-md-6 mb-3">
          <label for="scadenza" class="form-label">Scadenza</label>
          <input
            type="date"
            id="scadenza"
            class="form-control"
            bind:value={formData.scadenza}
            disabled={$loading}
          />
        </div>

        <div class="col-md-6 mb-3">
          <label for="consenso" class="form-label d-block">Opzioni</label>
          <div class="form-check">
            <input
              type="checkbox"
              id="consenso"
              class="form-check-input"
              bind:checked={formData.consenso}
              disabled={$loading}
            />
            <label for="consenso" class="form-check-label">
              Consenso richiesto
            </label>
            <div class="form-text">
              Se attivo, verranno mostrati solo i contatti con consenso
            </div>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button
          type="button"
          class="btn btn-secondary"
          onclick={handleCancel}
          disabled={$loading}
        >
          Annulla
        </button>
        <button
          type="submit"
          class="btn btn-primary"
          disabled={$loading}
        >
          {#if $loading}
            <span class="spinner-border spinner-border-sm me-2"></span>
          {/if}
          {lista ? 'Salva Modifiche' : 'Crea Lista'}
        </button>
      </div>
    </form>
  </div>
</div>

<style>
  .form-view {
    max-width: 900px;
    margin: 0 auto;
  }

  .form-header {
    margin-bottom: 2rem;
  }

  .form-header h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.75rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .form-card {
    background: white;
    border-radius: 8px;
    padding: 2rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .form-label {
    font-weight: 500;
    color: #495057;
    font-size: 0.875rem;
  }

  .form-text {
    font-size: 0.75rem;
  }

  .text-danger {
    font-weight: 600;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 1px solid #dee2e6;
  }
</style>
