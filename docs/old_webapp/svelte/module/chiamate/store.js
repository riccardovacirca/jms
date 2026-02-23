import { writable } from 'svelte/store'
import { auth } from '../../store.js'
import { fetchWithRefresh } from '../../util/fetchWithRefresh.js'

export const campagneAttive = writable([])
export const campagnaSelezionata = writable(null)
export const listeAttive = writable([])
export const listaSelezionata = writable(null)
export const contattoCorrente = writable(null)
export const contatti = writable([])
export const chiamateNumeri = writable(new Set()) // numeri già chiamati nella sessione
export const loading = writable(false)
export const error = writable(null)
export const inChiamata = writable(false)
export const autoMode = writable(false)

let autoLoopRunning = false

function getAuthUser() {
  let user
  auth.subscribe(v => { user = v.user })()
  return user
}

function isAdmin() {
  const user = getAuthUser()
  return user?.ruolo === 'ADMIN'
}

async function handleResponse(response) {
  if (!response.ok) {
    const data = await response.json().catch(() => ({}))
    throw new Error(data.log || `HTTP ${response.status}`)
  }
  return response.json()
}

// === NAVIGAZIONE INTERNA ===

export async function selezionaCampagna(campagna) {
  fermaChiamate()
  campagnaSelezionata.set(campagna)
  listaSelezionata.set(null)
  contattoCorrente.set(null)
  contatti.set([])
  chiamateNumeri.set(new Set())
  await caricaListeAttive(campagna.id)
}

export async function selezionaLista(lista) {
  fermaChiamate()
  listaSelezionata.set(lista)
  contattoCorrente.set(null)
  chiamateNumeri.set(new Set())
  await caricaContattiLista(lista.id)
}

// === CARICAMENTO DATI ===

export async function caricaCampagneAttive() {
  loading.set(true)
  error.set(null)
  try {
    const user = getAuthUser()
    let response
    let data
    let campagne

    // Admin: vede tutte le campagne
    // Operatore: vede solo campagne assegnate
    if (isAdmin()) {
      response = await fetchWithRefresh('/api/campagne')
      data = await handleResponse(response)
      campagne = data.out || []
    } else {
      // Operatore: usa endpoint campagne assegnate con dettagli completi
      response = await fetchWithRefresh(`/api/operatori/${user.id}/campagne?dettagli=true`)
      data = await handleResponse(response)
      campagne = data.out || []
    }

    // Filtro stato=1 (Attiva), ordina per dataFine ASC (scadenza più vicina prima), nulls last
    const attive = campagne
      .filter(c => c.stato === 1)
      .sort((a, b) => {
        if (!a.dataFine && !b.dataFine) return 0
        if (!a.dataFine) return 1
        if (!b.dataFine) return -1
        return a.dataFine.localeCompare(b.dataFine)
      })

    campagneAttive.set(attive)

    if (attive.length > 0) {
      await selezionaCampagna(attive[0])
    }
  } catch (err) {
    error.set(err.message)
  } finally {
    loading.set(false)
  }
}

async function caricaListeAttive(campagnaId) {
  try {
    // Associazioni campagna-lista
    const assocRes = await fetchWithRefresh(`/api/campagne/${campagnaId}/liste`)
    const assocData = await handleResponse(assocRes)
    const associazioni = assocData.out || []

    // Dettagli tutte le liste
    const listeRes = await fetchWithRefresh('/api/liste')
    const listeData = await handleResponse(listeRes)
    const allListe = listeData.out?.items || []

    // Merge + filtro stato=1
    const attive = associazioni
      .map(assoc => allListe.find(l => Number(l.id) === Number(assoc.listaId)))
      .filter(l => l && l.stato === 1)

    listeAttive.set(attive)

    if (attive.length > 0) {
      await selezionaLista(attive[0])
    }
  } catch (err) {
    error.set(err.message)
  }
}

async function caricaContattiLista(listaId) {
  try {
    const res = await fetchWithRefresh(`/api/liste/${listaId}/contatti`)
    const data = await handleResponse(res)
    contatti.set(data.out || [])
    aggiornaContattoCorrente()
  } catch (err) {
    error.set(err.message)
  }
}

// === LOGICA CONTATTO CORRENTE ===

function aggiornaContattoCorrente() {
  let currentContatti, called
  contatti.subscribe(v => { currentContatti = v })()
  chiamateNumeri.subscribe(v => { called = v })()

  const next = currentContatti.find(c =>
    c.telefono &&
    !c.blacklist &&
    !called.has(c.telefono)
  )
  contattoCorrente.set(next || null)
}

function markAsCalled(telefono) {
  let called
  chiamateNumeri.subscribe(v => { called = v })()
  called.add(telefono)
  chiamateNumeri.set(new Set(called))
}

// === CHIAMATE ===

export async function avviaChiamata(contatto) {
  inChiamata.set(true)
  error.set(null)
  try {
    const response = await fetchWithRefresh('/api/voice/calls', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        to: [{ type: 'phone', number: contatto.telefono }],
        from: { type: 'phone', number: '+390000000000' },
        answer_url: [`${window.location.origin}/api/voice/webhook/answer`],
        event_url: [`${window.location.origin}/api/voice/webhook/event`]
      })
    })

    await handleResponse(response)
    markAsCalled(contatto.telefono)
    aggiornaContattoCorrente()
    return true
  } catch (err) {
    error.set(err.message)
    throw err
  } finally {
    inChiamata.set(false)
  }
}

export async function avviaChiamataAutomatica() {
  autoLoopRunning = true
  autoMode.set(true)

  while (autoLoopRunning) {
    let current
    contattoCorrente.subscribe(v => { current = v })()

    if (!current) break

    try {
      await avviaChiamata(current)
    } catch {
      break
    }

    // Attende prima della prossima chiamata
    await new Promise(r => setTimeout(r, 2000))
  }

  autoLoopRunning = false
  autoMode.set(false)
}

export function fermaChiamate() {
  autoLoopRunning = false
  autoMode.set(false)
}

// Salta il contatto corrente senza chiamarlo
export function prosegui() {
  let current
  contattoCorrente.subscribe(v => { current = v })()
  if (current?.telefono) markAsCalled(current.telefono)
  aggiornaContattoCorrente()
}
