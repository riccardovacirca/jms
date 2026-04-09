import { authorized, user, headerUserSlot, dashboardItems } from '../../store.js';
import './UserMenu.js';

/**
 * Procedura di inizializzazione del modulo user.
 * Ripristina la sessione utente interrogando il server, registra
 * il componente UserMenu nell'area "user" dell'header e aggiunge
 * la voce di gestione utenti al dashboard (visibile solo per admin+).
 */
async function init() {
  headerUserSlot.set({ tag: 'user-menu' });
  dashboardItems.set([...dashboardItems.get(), {
    key:           'user-admin',
    label:         'Gestione utenti',
    icon:          'bi-people',
    tag:           'user-admin-users',
    import:        () => import('./admin/Users.js'),
    minRuoloLevel: 2,
  }]);
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
