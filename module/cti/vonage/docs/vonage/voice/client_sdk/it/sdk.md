# @vonage/client-sdk - v2.3.0
## SDK Client Vonage

Il Client SDK mira a fornire una soluzione pronta agli sviluppatori per costruire applicazioni di Conversazione Programmabile attraverso più Canali, tra cui: Messaggi, Voce, SIP, websocket e App.

```
⚠️ Attenzione: Funzionalità di Chat (Deprecata)

La funzionalità di chat nel nostro SDK è ora deprecata e la sua rimozione è pianificata per la fine di aprile 2026. Sebbene rimanga disponibile per il momento, si raccomanda di evitare di introdurre nuove dipendenze su questa funzionalità. I metodi esistenti potrebbero cambiare o cessare di essere supportati man mano che procediamo verso il suo abbandono. Se fai affidamento su questa funzionalità, inizia a pianificare la migrazione. I tuoi feedback rimangono benvenuti durante questo periodo di transizione.
```

## Installazione

L'SDK può essere installato utilizzando il comando `npm install`

```bash
npm i @vonage/client-sdk
```

## Configurazione dell'SDK
### Con bundler (Webpack, Vite, ecc.) e React

```javascript
import {
  VonageClient,
  ClientConfig,
  ConfigRegion,
  LoggingLevel
} from '@vonage/client-sdk';
import { useState, useEffect } from 'react';

function App() {
  const [config] = useState(() => new ClientConfig(ConfigRegion.AP));
  const [client] = useState(() => {
    // Inizializza il client con una configurazione opzionale (predefinito: log ERROR, regione US).
    const client = new VonageClient({
      loggingLevel: LoggingLevel.DEBUG,
      region: ConfigRegion.EU
    });
    // Oppure aggiorna alcune opzioni dopo l'inizializzazione.
    client.setConfig(config);
    return client;
  });
  const [session, setSession] = useState();
  const [user, setUser] = useState();
  const [error, setError] = useState();

  // Crea una Sessione non appena il client è disponibile
  useEffect(() => {
    if (!client) return;
    client
      .createSession('my-token')
      .then((session) => setSession(session))
      .catch((error) => setError(error));
  }, [client]);

  // Ottieni l'Utente non appena una sessione è disponibile
  useEffect(() => {
    if (!client || !session) return;
    client
      .getUser('me')
      .then((user) => setUser(user))
      .catch((error) => setError(error));
  }, [client, session]);

  if (error) return <pre>{JSON.stringify(error)}</pre>;

  if (!session || !user) return <div>Caricamento...</div>;

  return <div>Utente {user.displayName || user.name} connesso</div>;
}

export default App;
```

### Con tag script (UMD)

```html
<!-- <script src="./node_modules/@vonage/client-sdk/dist/vonageClientSDK.js"></script> -->
<!-- <script src="https://cdn.jsdelivr.net/npm/@vonage/client-sdk@1.0.0/dist/vonageClientSDK.min.js"></script> -->
<script src="./node_modules/@vonage/client-sdk/dist/vonageClientSDK.min.js"></script>
<script>
  const token = 'my-token';
  // Inizializza il client con una configurazione opzionale (predefinito: log ERROR, regione US).
  const client = new vonageClientSDK.VoiceClient({
    loggingLevel: LoggingLevel.DEBUG, 
    region: ConfigRegion.EU
  });
  // Oppure aggiorna alcune opzioni dopo l'inizializzazione.
  client.setConfig({
    region: ConfigRegion.AP
  });

  client.createSession(token).then((Session) => {});
</script>
```

### Con CDN (ES)

```javascript
import {
  VonageClient,
  ConfigRegion,
  LoggingLevel
} from 'https://cdn.jsdelivr.net/npm/@vonage/client-sdk@1.0.0/dist/vonageClientSDK.esm.min.js';

// Inizializza il client con una configurazione opzionale (predefinito: log ERROR, regione US).
const client = new VonageClient({
  loggingLevel: LoggingLevel.DEBUG,
  region: ConfigRegion.EU
});

// Oppure aggiorna alcune opzioni dopo l'inizializzazione.
client.setConfig({
  region: ConfigRegion.AP
});

(async () => {
  const token = 'my-token';
  try {
    // Crea una Sessione
    const sessionId = await client.createSession(token);
    // Ottieni l'Utente
    const user = await client.getUser('me');
    console.log(
      `Utente ${
        user.displayName || user.name
      } connesso con ID sessione: ${sessionId}`
    );
  } catch (error) {
    // Registra errori per createSession o getUser
    console.error(error);
  }
})();
```

## Esempi di Utilizzo

Di seguito sono riportati diversi scenari tipici in cui l'SDK è comunemente utilizzato.

### Effettuare una Chiamata in Uscita

```javascript
const context = {
  callee: 'user1'
};

const callId = await client.serverCall(context);
```

### Rispondere/Rifiutare una Chiamata in Entrata

```javascript
client.on('callInvite', async (callId, from, channelType) => {
  console.log(`Ricevuto invito a chiamata da ${from} su ${channelType}`);

  // Accetta la chiamata
  await client.answer(callId);

  // Rifiuta la chiamata
  await client.reject(callId);
});

client.on('callInviteCancel', (callId, reason) => {
  if (reason === CancelReason.AnsweredElsewhere) {
    console.log(`Chiamata ${callId} è stata risposta altrove`);
  } else if (reason === CancelReason.RejectedElsewhere) {
    console.log(`Chiamata ${callId} è stata rifiutata altrove`);
  } else if (reason === CancelReason.RemoteCancel) {
    console.log(`Chiamata ${callId} è stata annullata dal chiamante`);
  } else if (reason === CancelReason.RemoteTimeout) {
    console.log(`Chiamata ${callId} è scaduta`);
  }
});
```

### Terminare una Chiamata e Raccogliere Statistiche

```javascript
client.on(
  'callHangup',
  (callId: string, callQuality: RTCQuality, reason: HangupReason) => {
    if (reason == 'LOCAL_HANGUP') {
      console.log(`Chiamata ${callId} terminata localmente`);
    } else if (reason == 'REMOTE_HANGUP') {
      console.log(`Chiamata ${callId} terminata in remoto`);
    } else if (reason == 'REMOTE_REJECT') {
      console.log(`Chiamata ${callId} rifiutata in remoto`);
    } else if (reason == 'REMOTE_NO_ANSWER_TIMEOUT') {
      console.log(`Chiamata ${callId} scaduta (nessuna risposta)`);
    } else if (reason == 'MEDIA_TIMEOUT') {
      console.log(`Chiamata ${callId} scaduta (media)`);
    } else {
      exhaustiveCheck(reason);
    }
  }
);
```

### Ottenere Conversazioni

```javascript
const { conversations, nextCursor, previousCursor } =
  await client.getConversations({
    order: PresentingOrder.DESC,
    pageSize: 10,
    cursor: undefined,
    includeCustomData: true,
    orderBy: OrderBy.CUSTOM_SORT_KEY
  });
```

### Inviare Messaggi di Testo

```javascript
const timestamp = await client.sendMessageTextEvent(
  'CONVERSATION_ID',
  'Ciao Mondo!'
);
```

### Ascoltare Eventi di Conversazione

```javascript
const eventHandler = (event: ConversationEvent) => {
  if (event.kind == 'member:invited') {
    console.log(`Membro invitato: ${event.body.memberId}`);
  } else if (event.kind == 'member:joined') {
    console.log(`Membro unito: ${event.body.memberId}`);
  } else if (event.kind == 'member:left') {
    console.log(`Membro uscito: ${event.body.memberId}`);
  } else if (event.kind == 'ephemeral') {
    console.log(`Evento effimero: ${event.body}`);
  } else if (event.kind == 'custom') {
    console.log(`Evento personalizzato: ${event.body}`);
  } else if (event.kind == 'message:text') {
    console.log(`Messaggio di testo: ${event.body.text}`);
  } else if (event.kind == 'message:custom') {
    console.log(`Messaggio personalizzato: ${event.body.customData}`);
  } else if (event.kind == 'message:image') {
    console.log(`Messaggio immagine: ${event.body.imageUrl}`);
  } else if (event.kind == 'message:video') {
    console.log(`Messaggio video: ${event.body.videoUrl}`);
  } else if (event.kind == 'message:audio') {
    console.log(`Messaggio audio: ${event.body.audioUrl}`);
  } else if (event.kind == 'message:file') {
    console.log(`Messaggio file: ${event.body.fileUrl}`);
  } else if (event.kind == 'message:vcard') {
    console.log(`Messaggio vCard: ${event.body.vcardUrl}`);
  } else if (event.kind == 'message:location') {
    console.log(`Messaggio posizione: ${event.body.location}`);
  } else if (event.kind == 'message:template') {
    console.log(`Messaggio modello: ${event.body.template}`);
  } else if (event.kind == 'event:delete') {
    console.log(`Messaggio modello: ${event.body}`);
  } else if (event.kind == 'message:seen') {
    console.log(`Messaggio modello: ${event.body}`);
  } else if (event.kind == 'message:delivered') {
    console.log(`Evento messaggio consegnato: ${event.body}`);
  } else if (event.kind == 'message:rejected') {
    console.log(`Evento messaggio rifiutato ${event.body}`);
  } else if (event.kind == 'message:submitted') {
    console.log(`Evento messaggio consegnato ${event.body}`);
  } else if (event.kind == 'message:undeliverable') {
    console.log(`Evento messaggio non consegnabile ${event.body}`);
  } else {
    exhaustiveCheck(event);
  }
};

const listener = client.on('conversationEvent', eventHandler);

client.off('conversationEvent', listener);
```

## Documentazione ed Esempi

Visita il sito web di Vonage: [https://developer.vonage.com/](https://developer.vonage.com/)

## Licenza

Copyright (c) 2023 Vonage, Inc. Tutti i diritti riservati. Concesso in licenza solo in base al Contratto di Licenza per Vonage Client SDK (la "Licenza") disponibile in LICENSE.

Scaricando o utilizzando in altro modo il nostro software o servizi, riconosci di aver letto, compreso e accettato di essere vincolato dal Contratto di Licenza per Vonage Client SDK e dall'Informativa sulla Privacy.

Non puoi utilizzare, esercitare alcun diritto relativo a o sfruttare questo SDK, o qualsiasi modifica o opera derivata dello stesso, se non in conformità alla Licenza.
ß