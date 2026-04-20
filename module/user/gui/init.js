import { authorized, user, UIRegistry } from '../../store.js';
import './UserMenu.js';

/**
 * Procedura di inizializzazione del modulo user.
 * Ripristina la sessione utente interrogando il server, registra
 * il componente UserMenu nell'area "user" dell'header e aggiunge
 * la voce di gestione utenti al dashboard (visibile solo per admin+).
 */
async function init() {
  UIRegistry.headerUser.set({ tag: 'user-menu' });
  UIRegistry.sidebarNav.set([...UIRegistry.sidebarNav.get(), {
    key:           'user-admin',
    label:         'Utenti',
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
