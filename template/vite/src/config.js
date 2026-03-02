/**
 * Configurazione dei moduli dell'applicazione.
 *
 * Ogni modulo dichiara tutti gli attributi seguenti:
 *
 * path          → stringa (es. '/home') o null (modulo non navigabile via URL)
 * container     → ID dell'elemento DOM in cui montare il modulo (es. 'main', 'header', 'footer')
 * authorization → null (pubblico) o { redirectTo: '/path' } (protetto, reindirizza se non autorizzato)
 * persistent    → true (sempre montato, non smontato) o false (montato/smontato durante la navigazione)
 * init          → null o funzione asincrona eseguita all'avvio dell'app prima del routing
 */
export const MODULE_CONFIG = {
  index: {
    path: '/',
    container: 'main',
    authorization: null,
    persistent: false,
    init: null
  }
};

/**
 * Rotta di default quando l'hash è assente.
 */
export const DEFAULT_ROUTE = '/';
