import { writable } from 'svelte/store'
import { fetchWithRefresh } from '../../util/fetchWithRefresh.js'

// Store principale del modulo campagne
export const campagne = writable([])
export const campagnaCorrente = writable(null)
export const listeCampagna = writable([])
export const tuteliste = writable([])
export const loading = writable(false)
export const error = writable(null)

// Vista corrente: 'campagne' | 'liste'
export const currentView = writable('campagne')

// Filtri campagne
export const campagneFilters = writable({
  search: '',
  stato: 'all',
  tipo: 'all'
})

// Helper per gestire risposte API
async function handleResponse(response) {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({ message: 'Errore sconosciuto' }))
    throw new Error(errorData.message || `HTTP ${response.status}`)
  }

  return response.json()
}

// === NAVIGAZIONE ===

export function showCampagne() {
  currentView.set('campagne')
  campagnaCorrente.set(null)
  listeCampagna.set([])
}

export function showListeCampagna(campagna) {
  campagnaCorrente.set(campagna)
  currentView.set('liste')
}

// === API CAMPAGNE ===

export async function caricaCampagne() {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh('/api/campagne')

    const data = await handleResponse(response)

    campagne.set(data.out || [])
    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function salvaCampagna(campagnaDto) {
  loading.set(true)
  error.set(null)

  try {
    const isNew = !campagnaDto.id
    const url = isNew ? '/api/campagne' : `/api/campagne/${campagnaDto.id}`
    const method = isNew ? 'POST' : 'PUT'

    const response = await fetchWithRefresh(url, {
      method,
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(campagnaDto)
    })

    const data = await handleResponse(response)

    await caricaCampagne()
    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function eliminaCampagna(id) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/campagne/${id}`, {
      method: 'DELETE'
    })

    await handleResponse(response)
    await caricaCampagne()
    return true
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function aggiornaStatoCampagna(id, stato) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/campagne/${id}/stato?stato=${stato}`, {
      method: 'PUT'
    })

    const data = await handleResponse(response)

    await caricaCampagne()
    return data.out
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

// === API LISTE CAMPAGNA ===

export async function caricaListeCampagna(campagnaId) {
  loading.set(true)
  error.set(null)

  try {
    // Recupera le associazioni campagna-lista
    const response = await fetchWithRefresh(`/api/campagne/${campagnaId}/liste`)
    const data = await handleResponse(response)
    const associazioni = data.out || []

    // Recupera anche tutte le liste per ottenere nome e dettagli
    const listeResponse = await fetchWithRefresh('/api/liste')
    const listeData = await handleResponse(listeResponse)
    const allListe = listeData.out?.items || []

    // Mappa le associazioni con i dettagli della lista
    const listeWithDetails = associazioni.map(assoc => {
      const listaDetail = allListe.find(l => l.id === assoc.listaId)
      return {
        ...assoc,
        nome: listaDetail?.nome || `Lista #${assoc.listaId}`,
        descrizione: listaDetail?.descrizione || '',
        stato: listaDetail?.stato,
        contattiCount: listaDetail?.contattiCount || 0
      }
    })

    listeCampagna.set(listeWithDetails)
    tuteliste.set(allListe)
    return listeWithDetails
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function aggiungiListaCampagna(campagnaId, listaId) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/campagne/${campagnaId}/liste/${listaId}`, {
      method: 'POST'
    })

    await handleResponse(response)
    await caricaListeCampagna(campagnaId)
    return true
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}

export async function rimuoviListaCampagna(campagnaId, listaId) {
  loading.set(true)
  error.set(null)

  try {
    const response = await fetchWithRefresh(`/api/campagne/${campagnaId}/liste/${listaId}`, {
      method: 'DELETE'
    })

    await handleResponse(response)
    await caricaListeCampagna(campagnaId)
    return true
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    loading.set(false)
  }
}
