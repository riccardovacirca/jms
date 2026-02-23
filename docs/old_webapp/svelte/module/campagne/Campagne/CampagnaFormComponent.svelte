<script>
  import { onMount } from 'svelte'
  import { salvaCampagna, loading, error } from '../store.js'

  let { campagna = null, onSave = () => {}, onCancel = () => {} } = $props()

  let formData = $state({
    nome: '',
    descrizione: '',
    tipo: 'outbound',
    stato: 1,
    dataInizio: '',
    dataFine: ''
  })

  let validationErrors = $state({})

  onMount(() => {
    if (campagna) {
      formData = {
        nome: campagna.nome || '',
        descrizione: campagna.descrizione || '',
        tipo: campagna.tipo || 'outbound',
        stato: campagna.stato !== undefined ? campagna.stato : 1,
        dataInizio: campagna.dataInizio || '',
        dataFine: campagna.dataFine || ''
      }
    }
  })

  function validateForm() {
    const errors = {}
    if (!formData.nome || formData.nome.trim().length === 0) {
      errors.nome = 'Il nome è obbligatorio'
    }
    if (formData.nome && formData.nome.length > 100) {
      errors.nome = 'Il nome non può superare i 100 caratteri'
    }
    if (formData.dataInizio && formData.dataFine && formData.dataInizio > formData.dataFine) {
      errors.dataFine = 'La data di fine deve essere successiva alla data di inizio'
    }
    validationErrors = errors
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!validateForm()) return

    try {
      const campagnaDto = {
        nome: formData.nome.trim(),
        descrizione: formData.descrizione ? formData.descrizione.trim() : null,
        tipo: formData.tipo,
        stato: formData.stato,
        dataInizio: formData.dataInizio || null,
        dataFine: formData.dataFine || null
      }

      if (campagna && campagna.id) {
        campagnaDto.id = campagna.id
      }

      await salvaCampagna(campagnaDto)
      onSave()
    } catch (err) {
      console.error('Errore nel salvataggio della campagna:', err)
    }
  }
</script>

<div class="form-view">
  <div class="form-header">
    <div>
      <h2>
        {#if campagna}
          <i class="bi bi-pencil me-2"></i>
          Modifica Campagna
        {:else}
          <i class="bi bi-plus-lg me-2"></i>
          Nuova Campagna
        {/if}
      </h2>
      <p class="text-muted">Compila i campi per {campagna ? 'modificare' : 'creare'} la campagna</p>
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
            placeholder="Es: Campagna Q1 2026"
            disabled={$loading}
          />
          {#if validationErrors.nome}
            <div class="invalid-feedback">{validationErrors.nome}</div>
          {/if}
        </div>

        <div class="col-md-3 mb-3">
          <label for="tipo" class="form-label">Tipo</label>
          <select id="tipo" class="form-select" bind:value={formData.tipo} disabled={$loading}>
            <option value="outbound">Outbound</option>
            <option value="inbound">Inbound</option>
          </select>
        </div>

        <div class="col-md-3 mb-3">
          <label for="stato" class="form-label">Stato</label>
          <select id="stato" class="form-select" bind:value={formData.stato} disabled={$loading}>
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
          class="form-control"
          bind:value={formData.descrizione}
          rows="3"
          placeholder="Descrizione opzionale della campagna..."
          disabled={$loading}
        ></textarea>
      </div>

      <div class="row">
        <div class="col-md-6 mb-3">
          <label for="dataInizio" class="form-label">Data Inizio</label>
          <input
            type="date"
            id="dataInizio"
            class="form-control"
            bind:value={formData.dataInizio}
            disabled={$loading}
          />
        </div>

        <div class="col-md-6 mb-3">
          <label for="dataFine" class="form-label">Data Fine</label>
          <input
            type="date"
            id="dataFine"
            class="form-control {validationErrors.dataFine ? 'is-invalid' : ''}"
            bind:value={formData.dataFine}
            disabled={$loading}
          />
          {#if validationErrors.dataFine}
            <div class="invalid-feedback">{validationErrors.dataFine}</div>
          {/if}
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" onclick={onCancel} disabled={$loading}>
          Annulla
        </button>
        <button type="submit" class="btn btn-primary" disabled={$loading}>
          {#if $loading}
            <span class="spinner-border spinner-border-sm me-2"></span>
          {/if}
          {campagna ? 'Salva Modifiche' : 'Crea Campagna'}
        </button>
      </div>
    </form>
  </div>
</div>

<style>
  .form-view {
    padding: 2rem;
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

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 1px solid #dee2e6;
  }
</style>
