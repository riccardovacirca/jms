import { atom } from 'nanostores';

// Stato di autorizzazione generico.
// Impostato a true dal modulo che gestisce l'autenticazione.
// Il router legge questo valore per consentire o negare le rotte protette.
const authorized = atom(false);

// Dati dell'utente corrente. Impostato dal modulo auth al login/session check.
// null quando non autorizzato.
const user = atom(null);

// Voci di navigazione area "link" (sinistra, dopo il logo).
// Ogni modulo può aggiungere al massimo un elemento con la forma:
// { id: string, label: string, href: string, auth: boolean }
//   oppure con sottovoci per un dropdown:
// { id: string, label: string, auth: boolean, items: Array<{ label, href }> }
// auth: true = visibile solo se authorized; false = sempre visibile.
const headerNavItems = atom([]);

// Slot area "user" (destra, prima del tema).
// Un solo modulo alla volta può occupare questo slot registrando un
// tag di custom element già definito: { tag: string }
// Esempio: { tag: 'user-menu' }
// null = nessun modulo registrato.
const headerUserSlot = atom(null);

// Voci della sidebar del modulo dashboard.
// Ogni modulo con interfaccia amministrativa registra al massimo una voce:
// { key: string, label: string, icon: string, tag: string, import: () => Promise, minRuoloLevel: number }
// minRuoloLevel: 1=user, 2=admin, 3=root; default 0 = sempre visibile.
const dashboardItems = atom([]);

export { authorized, user, headerNavItems, headerUserSlot, dashboardItems };
