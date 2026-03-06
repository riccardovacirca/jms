<script>
  import { onMount } from 'svelte'
  import { validateData, validationResult, loading } from '../store.js'

  let { onNext = () => {}, onBack = () => {} } = $props()

  let hasValidated = $state(false)

  onMount(async () => {
    await performValidation()
  })

  async function performValidation() {
    try {
      await validateData()
      hasValidated = true
    } catch (err) {
      console.error('Errore validazione:', err)
    }
  }

  function handleNext() {
    if (!hasValidated) {
      alert('Attendi il completamento della validazione')
      return
    }

    if ($validationResult && $validationResult.errorRows > 0) {
      const confirmed = confirm(
        `Ci sono ${$validationResult.errorRows} righe con errori che NON verranno importate.\n\n` +
        `Verranno importate solo ${$validationResult.validRows} righe valide.\n\n` +
        'Vuoi continuare?'
      )

      if (!confirmed) return
    }

    onNext()
  }

  function getSeverityClass(severity) {
    const classes = {
      error: 'bg-danger',
      warning: 'bg-warning',
      info: 'bg-info'
    }
    return classes[severity] || 'bg-secondary'
  }

  function getSeverityIcon(severity) {
    const icons = {
      error: 'bi-x-circle-fill',
      warning: 'bi-exclamation-triangle-fill',
      info: 'bi-info-circle-fill'
    }
    return icons[severity] || 'bi-circle-fill'
  }
</script>

<div class="step3-container">
  <h2 class="step-title">
    <i class="bi bi-check-circle me-2"></i>
    Validazione Dati
  </h2>
  <p class="step-description">
    Verifica dei dati prima dell'importazione
  </p>

  {#if $loading}
    <div class="text-center py-5">
      <div class="spinner-border text-primary mb-3" role="status">
        <span class="visually-hidden">Validazione in corso...</span>
      </div>
      <p class="text-muted">Validazione dei dati in corso...</p>
    </div>
  {:else if $validationResult}
    <!-- Statistics Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon bg-primary-subtle">
          <i class="bi bi-file-earmark-text text-primary"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{$validationResult.totalRows}</div>
          <div class="stat-label">Totale Righe</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon bg-success-subtle">
          <i class="bi bi-check-circle text-success"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{$validationResult.validRows}</div>
          <div class="stat-label">Righe Valide</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon bg-warning-subtle">
          <i class="bi bi-exclamation-triangle text-warning"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{$validationResult.warningRows}</div>
          <div class="stat-label">Con Avvisi</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon bg-danger-subtle">
          <i class="bi bi-x-circle text-danger"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{$validationResult.errorRows}</div>
          <div class="stat-label">Con Errori</div>
        </div>
      </div>
    </div>

    <!-- Overall Status -->
    {#if $validationResult.errorRows > 0}
      <div class="alert alert-warning">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <strong>Attenzione:</strong> {$validationResult.errorRows} righe contengono errori e non verranno importate.
        Verranno importate solo le {$validationResult.validRows} righe valide.
      </div>
    {:else if $validationResult.warningRows > 0}
      <div class="alert alert-info">
        <i class="bi bi-info-circle-fill me-2"></i>
        <strong>Info:</strong> {$validationResult.warningRows} righe contengono avvisi ma verranno importate.
      </div>
    {:else}
      <div class="alert alert-success">
        <i class="bi bi-check-circle-fill me-2"></i>
        <strong>Perfetto!</strong> Tutti i dati sono validi e pronti per l'importazione.
      </div>
    {/if}

    <!-- Issues List -->
    {#if $validationResult.issues && $validationResult.issues.length > 0}
      <div class="issues-section">
        <h3 class="issues-title">
          Problemi Rilevati ({$validationResult.issues.length})
        </h3>
        <div class="issues-list">
          {#each $validationResult.issues.slice(0, 10) as issue}
            <div class="issue-item">
              <div class="issue-header">
                <span class="badge {getSeverityClass(issue.severity)}">
                  <i class="{getSeverityIcon(issue.severity)} me-1"></i>
                  {issue.severity.toUpperCase()}
                </span>
                <span class="issue-row">Riga {issue.rowNumber}</span>
              </div>
              <div class="issue-message">{issue.message}</div>
              {#if issue.rowData}
                <div class="issue-data">
                  <small class="text-muted">{JSON.stringify(issue.rowData)}</small>
                </div>
              {/if}
            </div>
          {/each}
          {#if $validationResult.issues.length > 10}
            <div class="text-center text-muted mt-3">
              ... e altri {$validationResult.issues.length - 10} problemi
            </div>
          {/if}
        </div>
      </div>
    {/if}
  {/if}

  <div class="step-actions">
    <button class="btn btn-secondary" onclick={onBack}>
      <i class="bi bi-arrow-left me-2"></i>
      Indietro
    </button>
    <button
      class="btn btn-primary"
      onclick={handleNext}
      disabled={$loading || !hasValidated}
    >
      Continua Importazione
      <i class="bi bi-arrow-right ms-2"></i>
    </button>
  </div>
</div>

<style>
  .step3-container {
    max-width: 900px;
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

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
    margin-bottom: 2rem;
  }

  .stat-card {
    background: white;
    border: 1px solid #dee2e6;
    border-radius: 8px;
    padding: 1.5rem;
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .stat-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.5rem;
  }

  .stat-value {
    font-size: 1.75rem;
    font-weight: 700;
    color: #2c3e50;
  }

  .stat-label {
    font-size: 0.875rem;
    color: #7f8c8d;
  }

  .issues-section {
    margin-top: 2rem;
  }

  .issues-title {
    font-size: 1.125rem;
    font-weight: 600;
    color: #2c3e50;
    margin-bottom: 1rem;
  }

  .issues-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .issue-item {
    background: white;
    border: 1px solid #dee2e6;
    border-radius: 8px;
    padding: 1rem;
  }

  .issue-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;
  }

  .issue-row {
    font-size: 0.875rem;
    color: #6c757d;
  }

  .issue-message {
    color: #2c3e50;
    margin-bottom: 0.5rem;
  }

  .issue-data {
    padding: 0.5rem;
    background: #f8f9fa;
    border-radius: 4px;
    overflow-x: auto;
  }

  .step-actions {
    display: flex;
    justify-content: space-between;
    padding-top: 1.5rem;
    border-top: 1px solid #dee2e6;
    margin-top: 2rem;
  }
</style>
