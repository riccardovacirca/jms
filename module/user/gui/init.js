import { authorized, user } from '../../store.js';

/**
 * Procedura di inizializzazione del modulo user.
 * Ripristina la sessione utente interrogando il server.
 */
async function init() {
  try {
    const res  = await fetch('/api/user/auth/session');
    const data = await res.json();
    if (res.ok && !data.err) {
      authorized.set(true);
      user.set(data.out);
    }
  } catch (_) {}
}

export default init;
