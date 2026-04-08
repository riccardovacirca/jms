import { authorized, user, headerUserSlot } from '../../store.js';
import './UserMenu.js';

/**
 * Procedura di inizializzazione del modulo user.
 * Ripristina la sessione utente interrogando il server e registra
 * il componente UserMenu nell'area "user" dell'header.
 */
async function init() {
  headerUserSlot.set({ tag: 'user-menu' });
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
