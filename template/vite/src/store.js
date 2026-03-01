import { atom } from 'nanostores';

// Stato di autorizzazione generico.
// Impostato a true dal modulo che gestisce l'autenticazione.
// Il router legge questo valore per consentire o negare le rotte protette.
export const authorized = atom(false);

// Dati dell'utente corrente. Impostato dal modulo auth al login/session check.
// null quando non autorizzato.
export const user = atom(null);

