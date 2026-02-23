<script>
  import { analyzeFile, loading, fileHeaders, fileRowCount, uploadedFile } from '../store.js'

  let { onNext = () => {} } = $props()

  let isDragging = $state(false)
  let fileInput;

  function handleDragOver(event) {
    event.preventDefault()
    isDragging = true
  }

  function handleDragLeave() {
    isDragging = false
  }

  async function handleDrop(event) {
    event.preventDefault()
    isDragging = false

    const files = event.dataTransfer.files

    if (files.length > 0) {
      await processFile(files[0])
    }
  }

  function handleFileSelect(event) {
    const files = event.target.files

    if (files.length > 0) {
      processFile(files[0])
    }
  }

  async function processFile(file) {
    // Validate file type
    const validExtensions = ['.csv', '.xls', '.xlsx']
    const fileName = file.name.toLowerCase()
    const isValid = validExtensions.some(ext => fileName.endsWith(ext))

    if (!isValid) {
      alert('Tipo di file non supportato. Usa CSV, XLS o XLSX.')
      return
    }

    // Validate file size (max 10MB)
    const maxSize = 10 * 1024 * 1024
    if (file.size > maxSize) {
      alert('File troppo grande. Dimensione massima: 10MB')
      return
    }

    try {
      await analyzeFile(file)
      onNext()
    } catch (err) {
      console.error('Errore analisi file:', err)
      alert('Errore durante l\'analisi del file:\n\n' + err.message)
    }
  }

  function triggerFileInput() {
    fileInput.click()
  }

  function handleKeydown(event) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      triggerFileInput()
    }
  }
</script>

<div class="step1-container">
  <h2 class="step-title">
    <i class="bi bi-cloud-upload me-2"></i>
    Carica File
  </h2>
  <p class="step-description">
    Carica un file Excel (.xls, .xlsx) o CSV contenente i contatti da importare
  </p>

  <div
    class="drop-zone {isDragging ? 'dragging' : ''}"
    ondragover={handleDragOver}
    ondragleave={handleDragLeave}
    ondrop={handleDrop}
    onclick={triggerFileInput}
    onkeydown={handleKeydown}
    role="button"
    tabindex="0"
    aria-label="Carica file Excel o CSV"
  >
    <input
      type="file"
      bind:this={fileInput}
      onchange={handleFileSelect}
      accept=".csv,.xls,.xlsx"
      style="display: none;"
    />

    {#if $loading}
      <div class="loading-state">
        <div class="spinner-border text-primary mb-3" role="status">
          <span class="visually-hidden">Analisi in corso...</span>
        </div>
        <p>Analisi del file in corso...</p>
      </div>
    {:else}
      <div class="upload-content">
        <i class="bi bi-cloud-arrow-up upload-icon"></i>
        <h3>Trascina il file qui</h3>
        <p>oppure clicca per selezionare</p>
        <div class="supported-formats">
          <span class="badge bg-light text-dark">CSV</span>
          <span class="badge bg-light text-dark">XLS</span>
          <span class="badge bg-light text-dark">XLSX</span>
        </div>
        <small class="text-muted">Dimensione massima: 10MB</small>
      </div>
    {/if}
  </div>

  <div class="info-box">
    <i class="bi bi-info-circle me-2"></i>
    <div>
      <strong>Come funziona:</strong>
      <ol class="mb-0">
        <li>Carica un file Excel o CSV con i tuoi contatti</li>
        <li>Mappa le colonne del file ai campi del sistema</li>
        <li>Valida i dati per identificare eventuali problemi</li>
        <li>Seleziona una lista ed esegui l'importazione</li>
      </ol>
    </div>
  </div>
</div>

<style>
  .step1-container {
    max-width: 800px;
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

  .drop-zone {
    border: 3px dashed #cbd5e0;
    border-radius: 12px;
    padding: 4rem 2rem;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s;
    background: #f8f9fa;
  }

  .drop-zone:hover {
    border-color: #667eea;
    background: #f0f4ff;
  }

  .drop-zone.dragging {
    border-color: #667eea;
    background: #e6ecff;
    transform: scale(1.02);
  }

  .upload-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
  }

  .upload-icon {
    font-size: 4rem;
    color: #667eea;
    opacity: 0.7;
  }

  .upload-content h3 {
    font-size: 1.25rem;
    font-weight: 600;
    color: #2c3e50;
    margin: 0;
  }

  .upload-content p {
    margin: 0;
    color: #7f8c8d;
  }

  .supported-formats {
    display: flex;
    gap: 0.5rem;
    margin-top: 0.5rem;
  }

  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .loading-state p {
    color: #667eea;
    font-weight: 500;
  }

  .info-box {
    margin-top: 2rem;
    padding: 1.5rem;
    background: #e7f3ff;
    border-left: 4px solid #0066cc;
    border-radius: 8px;
    display: flex;
    gap: 1rem;
  }

  .info-box i {
    color: #0066cc;
    font-size: 1.5rem;
    flex-shrink: 0;
  }

  .info-box ol {
    padding-left: 1.25rem;
    margin-top: 0.5rem;
  }

  .info-box li {
    margin-bottom: 0.25rem;
  }
</style>
