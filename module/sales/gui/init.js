import { dashboardItems, dashboardStats } from '../../store.js';

/**
 * Procedura di inizializzazione del modulo sales.
 * Registra nel dashboard il gruppo "Sales" con le voci Contatti (utenti), Liste e Campagne (admin+).
 * Registra in dashboardStats le schede statistiche per contatti, liste e campagne.
 */
async function init() {
  dashboardItems.set([...dashboardItems.get(),
    {
      key:           'sales',
      label:         'Sales',
      icon:          'bi-people-fill',
      group:         true,
      minRuoloLevel: 1,
    },
    {
      key:           'sales-contatti',
      label:         'Contatti',
      icon:          'bi-person-lines-fill',
      tag:           'contatti-layout',
      import:        () => import('./Contatti.js'),
      parent:        'sales',
      minRuoloLevel: 1,
    },
    {
      key:           'sales-liste',
      label:         'Liste',
      icon:          'bi-list-ul',
      tag:           'sales-admin-liste',
      import:        () => import('./admin/Liste.js'),
      parent:        'sales',
      minRuoloLevel: 2,
    },
    {
      key:           'sales-campagne',
      label:         'Campagne',
      icon:          'bi-megaphone',
      tag:           'sales-admin-campagne',
      import:        () => import('./admin/Campagne.js'),
      parent:        'sales',
      minRuoloLevel: 2,
    },
  ]);

  // Shared promise: all three value() calls within the same render tick
  // share a single HTTP request to /api/sales/stats.
  const statsState = { promise: null };
  const fetchStats = () => {
    if (!statsState.promise) {
      statsState.promise = fetch('/api/sales/stats')
        .then(r => r.json())
        .then(d => d.out ?? {});
      statsState.promise.then(() => setTimeout(() => { statsState.promise = null; }, 0));
    }
    return statsState.promise;
  };

  dashboardStats.set([...dashboardStats.get(),
    {
      key:           'sales-contatti',
      label:         'Contatti',
      icon:          'bi-person-lines-fill',
      color:         'primary',
      minRuoloLevel: 1,
      value:         async () => {
        const out = await fetchStats();
        return String(out.contatti ?? '—');
      },
    },
    {
      key:           'sales-liste',
      label:         'Liste',
      icon:          'bi-list-ul',
      color:         'success',
      minRuoloLevel: 2,
      value:         async () => {
        const out = await fetchStats();
        return String(out.liste ?? '—');
      },
    },
    {
      key:           'sales-campagne',
      label:         'Campagne',
      icon:          'bi-megaphone',
      color:         'warning',
      minRuoloLevel: 2,
      value:         async () => {
        const out = await fetchStats();
        return String(out.campagne ?? '—');
      },
    },
  ]);
}

export default init;
