import { dashboardItems } from '../../store.js';

/**
 * Procedura di inizializzazione del modulo CRM.
 * Registra nel dashboard il gruppo "CRM" con le voci Contatti (utenti) e Liste (admin+).
 */
async function init() {
  dashboardItems.set([...dashboardItems.get(),
    {
      key:           'crm',
      label:         'CRM',
      icon:          'bi-people-fill',
      group:         true,
      minRuoloLevel: 1,
    },
    {
      key:           'crm-contatti',
      label:         'Contatti',
      icon:          'bi-person-lines-fill',
      tag:           'contatti-layout',
      import:        () => import('./Contatti.js'),
      parent:        'crm',
      minRuoloLevel: 1,
    },
    {
      key:           'crm-liste',
      label:         'Liste',
      icon:          'bi-list-ul',
      tag:           'crm-admin-liste',
      import:        () => import('./admin/Liste.js'),
      parent:        'crm',
      minRuoloLevel: 2,
    },
    {
      key:           'crm-campagne',
      label:         'Campagne',
      icon:          'bi-megaphone',
      tag:           'crm-admin-campagne',
      import:        () => import('./admin/Campagne.js'),
      parent:        'crm',
      minRuoloLevel: 2,
    },
  ]);
}

export default init;
