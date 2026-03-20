# Gestione delle Sessioni

Le Sessioni sono una parte integrante dell'utilizzo degli SDK Client Vonage. Una Sessione è un flusso di comunicazione in tempo reale tra il Client SDK e i server Vonage.

## Creazione di una Sessione

Per iniziare a effettuare o ricevere chiamate con il Vonage Client SDK è necessario creare una sessione. Un prerequisito per creare una sessione è avere un Utente e un JWT per quell'Utente. Puoi saperne di più su Utenti e JWT nella guida [Crea la tua Applicazione](https://developer.vonage.com/en/vonage-client-sdk/create-your-application#create-a-user). Una volta che hai un Utente e un JWT associato, puoi chiamare la funzione `createSession` sull'SDK. Gli esempi in questa guida saranno in JavaScript, ma i nomi delle funzioni sono gli stessi su tutte e tre le piattaforme.

```javascript
client.createSession(jwt)
 .then(sessionId => {
    console.log("ID della sessione creata: ", sessionId);
 })
 .catch(error => {
    console.error("Errore durante la creazione della sessione: ", error);
 });
```

Se ha successo, riceverai un ID Sessione. Ciò ti consente di riconnetterti a questa specifica Sessione in seguito, se necessario. Le sessioni hanno un TTL (Time To Live) di 15 minuti.

### Riconnessione a una Sessione

Se desideri riconnetterti a una sessione esistente, la funzione `createSession` accetta opzionalmente un ID Sessione come parametro.

```javascript
client.createSession(jwt, existingSessionId)
 .then(sessionId => {
    console.log("ID della sessione: ", sessionId);
 })
 .catch(error => {
    console.error("Errore: ", error);
 });
```

### Risoluzione dei Problemi nella Creazione della Sessione

Se non riesci a creare una Sessione con successo, il messaggio di errore restituito ti darà un'idea del problema. Ecco alcuni passaggi generali di risoluzione dei problemi:

1.  Verifica che l'Utente Vonage esista effettuando una richiesta GET all'[API Utenti](https://developer.vonage.com/en/api/application.v2#getUsers).
2.  Se l'Utente esiste, assicurati che l'Utente si trovi nella stessa [regione](https://developer.vonage.com/en/vonage-client-sdk/configure-data-center?source=vonage-client-sdk) a cui il Vonage Client SDK sta tentando di connettersi. L'oggetto `user._links` della chiamata API Utenti conterrà la regione dell'Utente.
3.  Inserisci il tuo JWT in [jwt.io](https://jwt.io) per assicurarti che il JWT abbia il nome utente corretto nella rivendicazione `sub` e non sia scaduto. Qui puoi anche verificare che i [percorsi ACL](https://developer.vonage.com/en/getting-started/concepts/authentication#access-control-list-acl) siano corretti e che l'oggetto abbia l'annidamento corretto.

## Aggiornamento di una Sessione

Se attualmente hai una sessione attiva e sai che il JWT utilizzato per creare la sessione sta per scadere presto, puoi aggiornare la sessione con un nuovo JWT.

```javascript
client.refreshSession(jwt)
 .then(() => {
    console.log("Sessione aggiornata");
 })
 .catch(error => {
    console.error("Errore durante l'aggiornamento della sessione: ", error);
 });
```

## Eliminazione di una Sessione

Puoi anche eliminare una sessione. Lo faresti, ad esempio, come parte di un flusso di disconnessione.

```javascript
client.deleteSession(jwt)
 .then(() => {
    console.log("Sessione eliminata");
 })
 .catch(error => {
    console.error("Errore durante l'eliminazione della sessione: ", error);
 });
```

## Hai Domande?

Se dovessi avere ulteriori domande, problemi o feedback, contattaci all'indirizzo `devrel@vonage.com` o su [Vonage Developer Community Slack](https://developer.vonage.com/en/community/slack).