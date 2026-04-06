/**
 * Store del modulo CTI.
 *
 * Gestisce lo stato della chiamata in corso nel flusso operator-first:
 *   1. Operatore si connette via WebRTC (sessione SDK)
 *   2. Il modulo dialer (esterno) fornisce il prossimo contatto da chiamare
 *   3. Il contatto viene mostrato in un dialog di conferma
 *   4. Operatore conferma → sistema chiama il cliente in modo asincrono (delay 1s)
 *   5. Chiamata connessa quando il cliente risponde
 *   6. Operatore può riagganciare
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

/**
 * Numero del cliente da chiamare.
 * Impostato internamente dalla CTI bar prima di avviare la chiamata.
 * Può essere letto da moduli ospitanti per mostrare il numero in corso.
 *
 * @type {import('nanostores').WritableAtom<string>}
 */
const targetNumber = atom('');

/**
 * Contatto corrente in lavorazione.
 * Impostato dalla CTI bar quando il dialer restituisce il prossimo contatto.
 * Leggibile dai moduli ospitanti per reagire al cambio di contatto.
 *
 * @type {import('nanostores').WritableAtom<object|null>}
 */
const currentContact = atom(null);

/**
 * URL dell'endpoint del modulo dialer esterno.
 * Impostato dal modulo ospitante prima dell'uso della CTI bar.
 * Se null, la CTI bar usa dati mock al posto della chiamata all'endpoint.
 *
 * Esempio: dialerEndpoint.set('/api/crm/contatti/dialer/next');
 *
 * @type {import('nanostores').WritableAtom<string|null>}
 */
const dialerEndpoint = atom(null);

export { callState, targetNumber, currentContact, dialerEndpoint };
