/**
 * Store del modulo CTI.
 *
 * Gestisce lo stato della chiamata in corso nel flusso operator-first:
 *   1. Operatore si connette via WebRTC (sessione SDK)
 *   2. Inserisce il numero cliente e avvia la chiamata (prepare + serverCall)
 *   3. Sistema chiama il cliente in modo asincrono (delay 1s)
 *   4. Chiamata connessa quando il cliente risponde
 *   5. Operatore può riagganciare
 */
import { atom } from 'nanostores';

/**
 * Stato della chiamata in corso.
 * @type {import('nanostores').WritableAtom<{
 *   active: boolean,
 *   callId: string|null,
 *   customerNumber: string|null,
 *   status: 'idle'|'waiting_customer'|'connected'
 * }>}
 */
const callState = atom({
  active: false,
  callId: null,
  customerNumber: null,
  status: 'idle'
});

export { callState };
