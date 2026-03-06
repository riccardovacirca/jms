import { writable } from 'svelte/store'
import { fetchWithRefresh } from '../../util/fetchWithRefresh.js'

// Store principale del modulo importer
export const currentView = writable('wizard')
export const loading = writable(false)
export const error = writable(null)

// Wizard state
export const currentStep = writable(1)
export const sessionId = writable(null)
export const uploadedFile = writable(null)

// Step 1: File analysis result
export const fileHeaders = writable([])
export const filePreview = writable([])
export const fileRowCount = writable(0)
export const fileWarnings = writable([])

// Step 2: Column mapping
export const columnMapping = writable({})
export const availableFields = writable([])

// Step 3: Validation results
export const validationResult = writable(null)

// Step 4: Target list
export const targetLista = writable(null)

// Helper per gestire risposte API
async function handleResponse(response) {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ message: 'Errore sconosciuto' }))
    throw new Error(errorData.message || `HTTP ${response.status}`)
  }

  return response.json()
}

// === NAVIGAZIONE ===

export function showWizard() {
  currentView.set('wizard')
}

export function resetWizard() {
  currentStep.set(1)
  sessionId.set(null)
  uploadedFile.set(null)
  fileHeaders.set([])
  filePreview.set([])
  fileRowCount.set(0)
  fileWarnings.set([])
  columnMapping.set({})
  validationResult.set(null)
  targetLista.set(null)
  error.set(null)
}

// === API STEP 1: ANALYZE FILE ===

export async function analyzeFile(file) {
  loading.set(true)
  error.set(null)

  try {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetchWithRefresh('/api/liste/import/analyze', {
      method: 'POST',
      body: formData
    })

    const data = await handleResponse(response)

    // Struttura risposta: { out: { sessionId, headers, previewRows, rowCount, filename, warnings } }
    if (data.out) {
      sessionId.set(data.out.sessionId)
      fileHeaders.set(data.out.headers || [])
      filePreview.set(data.out.previewRows || [])
      fileRowCount.set(data.out.rowCount || 0)
      fileWarnings.set(data.out.warnings || [])
      uploadedFile.set({ name: file.name, size: file.size })
    }

    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

// === API: GET AVAILABLE FIELDS ===

export async function loadAvailableFields() {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh('/api/liste/import/campi')

    const data = await handleResponse(response)

    // Struttura risposta: { out: [ { nomeCampo, etichetta, descrizione, tipoDato, obbligatorio } ] }
    availableFields.set(data.out || [])

    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

// === API STEP 2: SAVE MAPPING ===

export async function saveMapping(mapping) {
  loading.set(true)
  error.set(null)

  try {
    let sid;

    sessionId.subscribe(value => {
      sid = value
    })()

    const response = await fetchWithRefresh('/api/liste/import/mapping', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: sid,
        columnMapping: mapping
      })
    })

    const data = await handleResponse(response)

    columnMapping.set(mapping)

    return data
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

// === API STEP 3: VALIDATE DATA ===

export async function validateData() {
  loading.set(true)
  error.set(null)

  try {
    let sid;

    sessionId.subscribe(value => {
      sid = value
    })()

    const response = await fetchWithRefresh(`/api/liste/import/validate/${sid}`, {
      method: 'POST'
    })

    const data = await handleResponse(response)

    // Struttura risposta: { out: { totalRows, validRows, warningRows, errorRows, issues: [...] } }
    validationResult.set(data.out)

    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

// === API STEP 4: EXECUTE IMPORT ===

export async function executeImport(listaId, listaName) {
  loading.set(true)
  error.set(null)

  try {
    let sid;

    sessionId.subscribe(value => {
      sid = value
    })()

    const body = {}
    if (listaId) {
      body.listaId = listaId
    } else if (listaName) {
      body.listaName = listaName
    }

    const response = await fetchWithRefresh(`/api/liste/import/execute/${sid}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    })

    const data = await handleResponse(response)

    // Struttura risposta: { out: { rowsImported, listaName, listaId } }
    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}
