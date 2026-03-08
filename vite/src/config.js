/**
 * Configurazione dei moduli dell'applicazione.
 *
 * Ogni modulo dichiara tutti gli attributi seguenti:
 *
 * route         → stringa (es. '/home') o null (modulo non navigabile via URL)
 * path          → nome della cartella sotto vite/src/modules/ (es. 'home')
 * container     → ID dell'elemento DOM in cui montare il modulo (es. 'main', 'header', 'footer')
 * authorization → null (pubblico) o { redirectTo: '/route' } (protetto, reindirizza se non autorizzato)
 * persistent    → true (sempre montato, non smontato) o false (montato/smontato durante la navigazione)
 * priority      → numero (solo per persistent: true): più basso = carica per primo (default: 999)
 * init          → null o funzione asincrona eseguita all'avvio dell'app prima del routing
 */
export const MODULE_CONFIG = {
  status: {
    route: '/',
    path: 'status',
    container: 'main',
    authorization: null,
    persistent: false,
    priority: 999,
    init: null
  }
};

/**
 * Modulo di default caricato quando l'hash è assente.
 */
export const DEFAULT_MODULE = 'status';
