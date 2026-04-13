import { dashboardItems } from '../../../store.js';

/**
 * Procedura di inizializzazione del modulo CTI.
 * Registra nel dashboard un gruppo "CTI" con la voce "Operatori" come sotto-voce.
 */
async function init()
{
  dashboardItems.set([...dashboardItems.get(),
    {
      key:           'cti',
      label:         'CTI',
      icon:          'bi-headset',
      group:         true,
      minRuoloLevel: 2,
    },
    {
      key:           'cti-operators',
      label:         'Operatori',
      icon:          'bi-people',
      tag:           'cti-admin-operators',
      import:        () => import('./admin/Operators.js'),
      parent:        'cti',
      minRuoloLevel: 2,
    },
  ]);
}

export default init;
