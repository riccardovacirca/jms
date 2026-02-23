import { writable, derived } from 'svelte/store'
import { fetchWithRefresh } from '../../util/fetchWithRefresh.js'

// Store principale del modulo liste
export const liste = writable([])
export const currentLista = writable(null)
export const contattiLista = writable([])
export const loading = writable(false)
export const error = writable(null)

// Vista corrente: 'liste' | 'contatti'
export const currentView = writable('liste')

// Filtri e paginazione per liste
export const listeFilters = writable({
  search: '',
  stato: 'all',
  attiva: 'all'
})

export const listePagination = writable({
  limit: 50,
  offset: 0,
  total: 0
})

// Filtri e paginazione per contatti
export const contattiFilters = writable({
  search: '',
  stato: 'all',
  blacklist: 'all'
})

export const contattiPagination = writable({
  limit: 50,
  offset: 0,
  total: 0
})

// Helper per gestire risposte API
async function handleResponse(response) {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ message: 'Errore sconosciuto' }))
    throw new Error(errorData.message || `HTTP ${response.status}`)
  }

  return response.json()
}

// === NAVIGAZIONE TRA VISTE ===

export function showListe() {
  currentView.set('liste')
  currentLista.set(null)
}

export function showContatti(lista) {
  currentLista.set(lista)
  currentView.set('contatti')
}

// === API LISTE ===

export async function caricaListe(limit = 50, offset = 0, filters = {}) {
  loading.set(true)
  error.set(null)

  try {
    const params = new URLSearchParams({
      limit: limit.toString(),
      offset: offset.toString()
    })

    if (filters.search) {
      params.append('search', filters.search)
    }

    const response = await fetchWithRefresh(`/api/liste?${params}`, {
    })

    const data = await handleResponse(response)

    liste.set(data.out?.items || [])
    listePagination.update(p => ({
      ...p,
      total: data.out?.total || 0,
      limit: data.out?.limit || limit,
      offset: data.out?.offset || offset
    }))

    return data
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function caricaLista(id) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/liste/${id}`, {
    })

    const data = await handleResponse(response)

    currentLista.set(data.out)
    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function salvaLista(listaDto) {
  loading.set(true)
  error.set(null)

  try {
    const isNew = !listaDto.id

    const url = isNew ? '/api/liste' : `/api/liste/${listaDto.id}`
    const method = isNew ? 'POST' : 'PUT'

    const response = await fetchWithRefresh(url, {
      method,
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(listaDto)
    })

    const data = await handleResponse(response)

    // Ricarica le liste
    await caricaListe()

    return data
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function eliminaLista(id) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/liste/${id}`, {
      method: 'DELETE',
    })

    await handleResponse(response)

    // Ricarica le liste
    await caricaListe()

    return true
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function aggiornaStatoLista(id, stato) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/liste/${id}/stato`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ stato })
    })

    const data = await handleResponse(response)

    // Ricarica le liste
    await caricaListe()

    return data
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function aggiornaScadenzaLista(id, scadenza) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/liste/${id}/scadenza`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ scadenza })
    })

    const data = await handleResponse(response)

    // Ricarica le liste
    await caricaListe()

    return data
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

// === API CONTATTI LISTA ===

export async function caricaContattiLista(listaId, limit = 50, offset = 0) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/liste/${listaId}/contatti`)

    const data = await handleResponse(response)

    const contattiArray = Array.isArray(data.out) ? data.out : []

    contattiLista.set(contattiArray)
    contattiPagination.update(p => ({
      ...p,
      total: contattiArray.length,
      limit: contattiArray.length,
      offset: 0
    }))

    return data
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function aggiungiContattoALista(listaId, contattoId) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/liste/${listaId}/contatti/${contattoId}`, {
      method: 'POST',
    })

    await handleResponse(response)

    // Ricarica i contatti della lista
    await caricaContattiLista(listaId)

    return true
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function rimuoviContattoDaLista(listaId, contattoId) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/liste/${listaId}/contatti/${contattoId}`, {
      method: 'DELETE',
    })

    await handleResponse(response)

    // Ricarica i contatti della lista
    await caricaContattiLista(listaId)

    return true
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

// Store derivati
export const listeAttive = derived(liste, $liste =>
  $liste.filter(l => l.attiva)
)

export const listeCount = derived(liste, $liste => $liste.length)

export const contattiListaCount = derived(contattiLista, $contatti => $contatti.length)
