# Panoramica

In questa guida imparerai come aggiungere il Client SDK alla tua app JavaScript lato client.

## Prerequisiti

Il Client SDK richiede [Node.js](https://nodejs.org) e [NPM](https://www.npmjs.com/).

## Per aggiungere il Client SDK al tuo progetto

### Naviga alla tua app

Apri il tuo terminale. Se hai un'app esistente, vai alla sua radice. In caso contrario, crea una nuova directory per il tuo progetto.

### Aggiungi il Client SDK al tuo progetto

### Utilizzando npm

```javascript
import { VonageClient } from "@vonage/client-sdk";
```

```html
<script src="./node_modules/@vonage/client-sdk/dist/vonageClientSDK.min.js"></script>
```

```bash
$ npm init -y
```

```bash
$ npm install @vonage/client-sdk -s
```

### Utilizzando CDN

```html
<!-- ******* Carica vonageClientSDK da un CDN ****** -->
<script src="https://cdn.jsdelivr.net/npm/@vonage/client-sdk@latest/dist/vonageClientSDK.min.js"></script>
```

```javascript
//********* Ottieni un riferimento a vonageClientSDK **********
const vonageClientSDK = window.vonageClientSDK;
```

## Utilizzare il Client SDK nella tua app

### Creazione di Utenti e JWT

Un JSON Web Token (JWT) è necessario per accedere alla tua Applicazione Vonage. Il Client SDK non può gestire gli utenti né generare JWT, quindi devi scegliere un metodo per gestirlo sul backend:

*   Per scopi di onboarding o test, puoi far funzionare la tua app lato client prima di configurare un backend [generando un JWT di test dalla riga di comando](https://developer.vonage.com/en/vonage-client-sdk/create-your-application) e inserendolo in modo statico nel tuo JavaScript lato client.
    
*   Per un utilizzo reale, puoi fornire JWT dal server utilizzando gli [SDK backend](https://developer.vonage.com/en/tools) Node o PHP, e impostare la variabile `jwt` nel tuo codice recuperando quei dati:
    
    ```javascript
    fetch("/getJwt")
      .then(results => results.json())
      .then(data => {
        jwt = data.token;
      })
      .catch(err => console.error(err));
    ```
    
*   Leggi di più sulla generazione di JWT [in questo articolo](https://developer.vonage.com/en/getting-started/concepts/authentication#json-web-tokens)
    

### Instanziare VonageClient e creare una sessione

L'istanziazione del `VonageClient` varia a seconda di come carichi il Vonage Client SDK.

Se caricato con un tag `<script>`:

```javascript
const client = new vonageClientSDK.VonageClient();
```

Se caricato via `import`:

```javascript
const client = new VonageClient();
```

Per creare una sessione, passa il tuo JWT come argomento a `createSession()`.

```javascript
client.createSession(jwt)
  .then(sessionId => {
    console.log("ID della sessione creata: ", sessionId);
  })
  .catch(error => {
    console.error("Errore durante la creazione della sessione: ", error);
  });
```

### Stato della Sessione

Se ci sono errori con la sessione dopo che è stata creata con successo, li riceverai tramite il listener dell'evento `sessionError` sul `VonageClient` istanziato.

```javascript
// Dopo aver creato una sessione
client.on("sessionError", (reason) => {
  console.error("Motivo dell'errore della sessione: ", reason);
});
```

## Conclusione

Hai aggiunto il Client SDK alla tua app JavaScript lato client e hai creato una sessione. Ora puoi utilizzare il client `VonageClient` nella tua app e sfruttare le funzionalità del Client SDK.

## Vedi anche

*   [Configurazione del Centro Dati](https://developer.vonage.com/en/vonage-client-sdk/configure-data-center) - questa è una configurazione avanzata opzionale che puoi eseguire dopo aver aggiunto l'SDK alla tua applicazione.
