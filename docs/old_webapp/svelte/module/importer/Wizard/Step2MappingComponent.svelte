<script>
  import { onMount } from 'svelte'
  import {
    fileHeaders,
    filePreview,
    fileWarnings,
    columnMapping,
    availableFields,
    loadAvailableFields,
    saveMapping,
    loading
  } from '../store.js'

  let { onNext = () => {}, onBack = () => {} } = $props()

  let mapping = $state({})
  let fields = $state([])

  onMount(async () => {
    await loadAvailableFields()

    availableFields.subscribe(value => {
      fields = value
    })()

    // Try intelligent matching
    performIntelligentMatching()
  })

  function performIntelligentMatching() {
    let headers;

    fileHeaders.subscribe(value => {
      headers = value
    })()

    const newMapping = {}

    headers.forEach((header, index) => {
      const normalized = header.toLowerCase().trim()

      // Exact match on field name
      const exactMatch = fields.find(f => f.nomeCampo.toLowerCase() === normalized)
      if (exactMatch) {
        newMapping[header] = exactMatch.nomeCampo
        return
      }

      // Exact match on label
      const labelMatch = fields.find(f => f.etichetta.toLowerCase() === normalized)
      if (labelMatch) {
        newMapping[header] = labelMatch.nomeCampo
        return
      }

      // Pattern matching
      const patterns = {
        nome: ['nome', 'name', 'first'],
        cognome: ['cognome', 'surname', 'last'],
        ragioneSociale: ['ragione', 'azienda', 'company', 'societa'],
        telefono: ['telefono', 'phone', 'tel', 'cellulare', 'mobile'],
        email: ['email', 'mail', 'posta'],
        indirizzo: ['indirizzo', 'address', 'via'],
        citta: ['citta', 'city', 'comune'],
        cap: ['cap', 'zip', 'postal'],
        provincia: ['provincia', 'prov', 'province'],
        note: ['note', 'notes', 'commenti']
      }

      for (const [fieldName, keywords] of Object.entries(patterns)) {
        if (keywords.some(keyword => normalized.includes(keyword))) {
          const field = fields.find(f => f.nomeCampo === fieldName)
          if (field) {
            newMapping[header] = fieldName
            break
          }
        }
      }
    })

    mapping = newMapping
  }

  function handleMappingChange(header, value) {
    if (value === '') {
      delete mapping[header]
      mapping = { ...mapping }
    } else {
      mapping = { ...mapping, [header]: value }
    }
  }

  async function handleNext() {
    // Validate: at least one column must be mapped
    if (Object.keys(mapping).length === 0) {
      alert('Devi mappare almeno una colonna')
      return
    }

    // Check if at least one identifier is mapped (nome, cognome, or ragioneSociale)
    const identifiers = ['nome', 'cognome', 'ragioneSociale']
    const hasIdentifier = Object.values(mapping).some(field => identifiers.includes(field))

    if (!hasIdentifier) {
      const confirmed = confirm(
        'Attenzione: nessun identificatore mappato (nome, cognome o ragione sociale).\n\n' +
        'I contatti potrebbero essere difficili da identificare.\n\n' +
        'Vuoi continuare comunque?'
      )

      if (!confirmed) return
    }

    try {
      await saveMapping(mapping)
      onNext()
    } catch (err) {
      console.error('Errore salvataggio mapping:', err)
    }
  }

  function getMappedCount() {
    return Object.keys(mapping).length
  }

  function getFieldInfo(fieldName) {
    return fields.find(f => f.nomeCampo === fieldName)
  }
</script>

<div class="step2-container">
  <div class="step-header">
    <div>
      <h2 class="step-title">
        <i class="bi bi-diagram-3 me-2"></i>
        Mapping Colonne
      </h2>
      <p class="step-description">
        Associa le colonne del file ai campi del sistema
      </p>
    </div>
    <div class="mapping-summary">
      <span class="badge bg-primary">
        {getMappedCount()} / {$fileHeaders.length} colonne mappate
      </span>
    </div>
  </div>

  {#if $fileWarnings && $fileWarnings.length > 0}
    <div class="alert alert-warning">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      <strong>Attenzione:</strong>
      <ul class="mb-0 mt-2">
        {#each $fileWarnings as warning}
          <li>{warning}</li>
        {/each}
      </ul>
    </div>
  {/if}

  {#if $fileHeaders.length > 0}
    <div class="mapping-table-container">
      <table class="table">
        <thead>
          <tr>
            <th>Colonna File</th>
            <th>Campo Sistema</th>
            <th>Anteprima Dati</th>
          </tr>
        </thead>
        <tbody>
          {#each $fileHeaders as header, index}
            <tr>
              <td class="column-name">
                <strong>{header}</strong>
              </td>
              <td class="mapping-select">
                <select
                  class="form-select"
                  value={mapping[header] || ''}
                  onchange={(e) => handleMappingChange(header, e.target.value)}
                >
                  <option value="">-- Ignora --</option>
                  {#each fields as field}
                    <option value={field.nomeCampo}>
                      {field.etichetta}
                      {field.obbligatorio ? ' *' : ''}
                    </option>
                  {/each}
                </select>
                {#if mapping[header]}
                  {@const fieldInfo = getFieldInfo(mapping[header])}
                  {#if fieldInfo?.descrizione}
                    <small class="text-muted d-block mt-1">{fieldInfo.descrizione}</small>
                  {/if}
                {/if}
              </td>
              <td class="preview-data">
                {#if $filePreview.length > 0}
                  <div class="preview-values">
                    {#each $filePreview.slice(0, 3) as row}
                      <div class="preview-value">{row[header] || '-'}</div>
                    {/each}
                  </div>
                {/if}
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}

  <div class="step-actions">
    <button class="btn btn-secondary" onclick={onBack}>
      <i class="bi bi-arrow-left me-2"></i>
      Indietro
    </button>
    <button class="btn btn-primary" onclick={handleNext} disabled={$loading}>
      {#if $loading}
        <span class="spinner-border spinner-border-sm me-2"></span>
      {/if}
      Avanti
      <i class="bi bi-arrow-right ms-2"></i>
    </button>
  </div>
</div>

<style>
  .step2-container {
    max-width: 1000px;
    margin: 0 auto;
  }

  .step-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 2rem;
  }

  .step-title {
    font-size: 1.5rem;
    font-weight: 600;
    color: #2c3e50;
    margin-bottom: 0.5rem;
  }

  .step-description {
    color: #7f8c8d;
    margin: 0;
  }

  .mapping-summary {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .mapping-table-container {
    background: white;
    border-radius: 8px;
    border: 1px solid #dee2e6;
    margin-bottom: 2rem;
    overflow-x: auto;
  }

  .table {
    margin-bottom: 0;
  }

  .table thead th {
    background: #f8f9fa;
    border-bottom: 2px solid #dee2e6;
    font-weight: 600;
    font-size: 0.875rem;
    color: #495057;
    padding: 1rem;
  }

  .table tbody td {
    padding: 1rem;
    vertical-align: top;
  }

  .column-name {
    min-width: 150px;
  }

  .mapping-select {
    min-width: 250px;
  }

  .preview-data {
    min-width: 200px;
  }

  .preview-values {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .preview-value {
    font-size: 0.875rem;
    color: #6c757d;
    padding: 0.25rem 0.5rem;
    background: #f8f9fa;
    border-radius: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .step-actions {
    display: flex;
    justify-content: space-between;
    padding-top: 1.5rem;
    border-top: 1px solid #dee2e6;
  }
</style>
