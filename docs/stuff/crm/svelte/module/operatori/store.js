import { writable } from 'svelte/store';

// Store del modulo operatori
export const operatori = writable([]);
export const loading = writable(false);
export const error = writable(null);

// Operatore selezionato
export const selectedOperatore = writable(null);

// Vista corrente: 'list', 'new', 'edit'
export const currentView = writable('list');

// Modal campagne
export const showCampagneModal = writable(false);
export const campagneOperatore = writable([]);

// Funzioni di navigazione
export function showList() {
  currentView.set('list');
  selectedOperatore.set(null);
}

export function showNew() {
  currentView.set('new');
  selectedOperatore.set(null);
}

export function showEdit(operatore) {
  currentView.set('edit');
  selectedOperatore.set(operatore);
}

// Funzioni API
export async function caricaOperatori() {
  loading.set(true);
  error.set(null);

  try {
    const response = await fetch('/api/operatori', {
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    operatori.set(data.out || []);
  } catch (e) {
    error.set(e.message);
    operatori.set([]);
  } finally {
    loading.set(false);
  }
}

export async function salvaOperatore(dto) {
  loading.set(true);
  error.set(null);

  try {
    const isUpdate = dto.id != null;
    const url = isUpdate ? `/api/operatori/${dto.id}` : '/api/operatori';
    const method = isUpdate ? 'PUT' : 'POST';

    const response = await fetch(url, {
      method: method,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(dto)
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    await caricaOperatori();
    return true;
  } catch (e) {
    error.set(e.message);
    return false;
  } finally {
    loading.set(false);
  }
}

export async function eliminaOperatore(id) {
  loading.set(true);
  error.set(null);

  try {
    const response = await fetch(`/api/operatori/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    await caricaOperatori();
    return true;
  } catch (e) {
    error.set(e.message);
    return false;
  } finally {
    loading.set(false);
  }
}

export async function caricaCampagneOperatore(operatoreId) {
  loading.set(true);
  error.set(null);

  try {
    const response = await fetch(`/api/operatori/${operatoreId}/campagne`, {
      credentials: 'include'
    });
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    campagneOperatore.set(data.out || []);
  } catch (e) {
    error.set(e.message);
    campagneOperatore.set([]);
  } finally {
    loading.set(false);
  }
}

export async function associaCampagna(operatoreId, campagnaId) {
  loading.set(true);
  error.set(null);

  try {
    const response = await fetch(`/api/operatori/${operatoreId}/campagne/${campagnaId}`, {
      method: 'POST'
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    await caricaCampagneOperatore(operatoreId);
    return true;
  } catch (e) {
    error.set(e.message);
    return false;
  } finally {
    loading.set(false);
  }
}

export async function rimuoviCampagna(operatoreId, campagnaId) {
  loading.set(true);
  error.set(null);

  try {
    const response = await fetch(`/api/operatori/${operatoreId}/campagne/${campagnaId}`, {
      method: 'DELETE'
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    await caricaCampagneOperatore(operatoreId);
    return true;
  } catch (e) {
    error.set(e.message);
    return false;
  } finally {
    loading.set(false);
  }
}
