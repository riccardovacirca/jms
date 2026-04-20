import { UIRegistry } from '../../store.js';

/**
 * Procedura di inizializzazione del modulo dashboard.
 * Registra la voce Stats nella sidebar per tornare alla pagina iniziale del dashboard.
 */
async function init() {
  UIRegistry.sidebarNav.set([{
    key:           'dashboard-stats',
    label:         'Stats',
    icon:          'bi-grid',
    tag:           'dashboard-stats',
    import:        () => import('./Stats.js'),
    minRuoloLevel: 2,
  }, ...UIRegistry.sidebarNav.get()]);
}

export default init;
