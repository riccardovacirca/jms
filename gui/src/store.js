import { atom } from 'nanostores';

// Stato di autorizzazione generico.
// Impostato a true dal modulo che gestisce l'autenticazione.
// Il router legge questo valore per consentire o negare le rotte protette.
const authorized = atom(false);

// Dati dell'utente corrente. Impostato dal modulo auth al login/session check.
// null quando non autorizzato.
const user = atom(null);

/**
 * Registro centralizzato dei componenti dinamici dell'interfaccia.
 *
 * Ogni slot è un atom nanostores indipendente: i consumer sottoscrivono
 * solo lo slot di proprio interesse senza ricevere notifiche dagli altri.
 * I moduli scrivono sugli slot nella propria init.js senza importare
 * nulla dal consumer che li ospita.
 *
 * Slot disponibili:
 *
 *   headerNav    — Array di voci nav dell'header.
 *                  Forma: { id, label, href, auth } oppure { id, label, auth, items: [{label, href}] }
 *                  auth: true = visibile solo se authorized.
 *
 *   headerUser   — Singleton: tag del custom element da montare nell'area user dell'header.
 *                  Forma: { tag: string } — un solo modulo alla volta.
 *
 *   sidebarNav   — Array di voci della sidebar del dashboard.
 *                  Forma voce normale:  { key, label, icon, tag, import, minRuoloLevel }
 *                  Forma gruppo:        { key, label, icon, group: true, minRuoloLevel }
 *                  Forma voce figlia:   { key, label, icon, tag, import, parent, minRuoloLevel }
 *                  minRuoloLevel: 0=sempre, 1=user, 2=admin, 3=root.
 *
 *   sidebarStats — Array di schede statistiche nella pagina di default del dashboard.
 *                  Forma: { key, label, icon, color, value: string | (() => Promise<string>), minRuoloLevel }
 */
class UIRegistry {
  static headerNav    = atom([]);
  static headerUser   = atom(null);
  static sidebarNav   = atom([]);
  static sidebarStats = atom([]);

  /**
   * Slot notifiche — array di notifiche da mostrare all'utente.
   * Forma: { id: string, message: string, type: 'info'|'success'|'warning'|'danger' }
   * Scritto da qualsiasi modulo tramite UIRegistry.notify().
   * Letto dal componente header-notifications (modulo header).
   */
  static notifications = atom([]);

  /**
   * Accoda una notifica visibile all'utente.
   *
   * @param {string} message - testo della notifica
   * @param {'info'|'success'|'warning'|'danger'} [type='info'] - stile Bootstrap alert
   */
  static notify(message, type = 'info') {
    const id = crypto.randomUUID();
    UIRegistry.notifications.set([...UIRegistry.notifications.get(), { id, message, type }]);
  }
}

export { authorized, user, UIRegistry };
