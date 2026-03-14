import { authorized, user } from '../../store.js';

/**
 * Procedura di inizializzazione del modulo auth.
 * Chiamata dal router all'avvio, prima del primo routing.
 *
 * Ripristina la sessione utente interrogando il server: se esiste un
 * access token valido nel cookie, imposta authorized e user nello store
 * in modo che il router parta già con lo stato di autenticazione corretto.
 */
async function init() {
  try {
    const res  = await fetch('/api/auth/session');
    const data = await res.json();
    if (res.ok && !data.err) {
      authorized.set(true);
      user.set(data.out);
    }
  } catch (_) {}
}

export default init;
