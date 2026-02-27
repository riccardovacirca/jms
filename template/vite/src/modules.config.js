/**
 * Configurazione dei moduli disponibili nell'applicazione.
 * Ogni modulo ha:
 * - context: 'public' | 'private' | 'auth'
 * - path: URL hash path (es. /home)
 * - title: Titolo della pagina
 */
export const MODULE_CONFIG = {
  // Aggiungere qui i moduli installati, esempio:
  // home: {
  //   context: 'public',
  //   path: '/home',
  //   title: 'Home'
  // }
};

/**
 * Configurazione dei contesti.
 * Definisce il comportamento di ogni contesto:
 * - requiresAuth: Se richiede autenticazione
 * - redirectTo: Dove reindirizzare se requisiti non soddisfatti
 * - fallbackTitle: Titolo mostrato se nessun modulo è associato
 * - fallbackMessage: Messaggio mostrato se nessun modulo è associato
 */
export const CONTEXT_CONFIG = {
  public: {
    requiresAuth: false,
    fallbackTitle: 'Pagina Pubblica',
    fallbackMessage: 'Nessun modulo configurato per questo contesto'
  },
  private: {
    requiresAuth: true,
    redirectTo: '/auth',
    fallbackTitle: 'Area Riservata',
    fallbackMessage: 'Nessun modulo configurato per questo contesto'
  },
  auth: {
    requiresAuth: false,
    fallbackTitle: 'Autenticazione',
    fallbackMessage: 'Nessun modulo di autenticazione configurato'
  }
};

/**
 * Route di default dell'applicazione
 */
export const DEFAULT_ROUTES = {
  root: '/',                  // Dove andare quando si accede a / (modificare dopo aver installato moduli)
  unauthorized: '/auth',      // Dove andare se non autenticato
  afterLogin: '/'             // Dove andare dopo login (modificare dopo aver installato moduli)
};
