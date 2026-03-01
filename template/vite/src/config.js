/**
 * Configurazione delle rotte dell'applicazione.
 *
 * path          → hash della rotta (es. '/home' corrisponde a /#/home)
 * authorization → null: rotta libera
 *                 { redirectTo: '/path' }: rotta protetta; se non autorizzato
 *                 reindirizza a redirectTo (se registrata) o mostra 403
 * init          → (opzionale) procedura eseguita dal router all'avvio, prima
 *                 del primo routing; usata per ripristinare stato condiviso
 *                 senza precaricare il modulo intero
 */
export const MODULE_CONFIG = {
  index: {
    path: '/',
    authorization: null
  }
};

/**
 * Rotta di default quando l'hash è assente.
 */
export const DEFAULT_ROUTE = '/';
