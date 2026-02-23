import { writable } from 'svelte/store';

export const user = writable(null);
export const loading = writable(false);
export const error = writable(null);

export async function login(username, password) {
  loading.set(true);
  error.set(null);

  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username, password })
    });

    const data = await response.json();
    if (data.err) throw new Error(data.log);

    user.set({
      id: data.out.userId,
      username: data.out.username,
      ruolo: data.out.ruolo
    });
  } catch (err) {
    error.set(err.message);
    throw err;
  } finally {
    loading.set(false);
  }
}

export async function logout() {
  await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include'
  });
  user.set(null);
}

export async function checkSession() {
  try {
    const response = await fetch('/api/auth/session', {
      credentials: 'include'
    });

    if (!response.ok) return false;

    const data = await response.json();
    if (data.err) return false;

    user.set({
      id: data.out.userId,
      ruolo: data.out.ruolo
    });
    return true;
  } catch {
    return false;
  }
}

export function isAdmin() {
  let currentUser;
  user.subscribe(u => currentUser = u)();
  return currentUser?.ruolo === 'ADMIN';
}
