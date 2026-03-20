# Configura il tuo Server Backend

Quando lavori con il Vonage Client SDK, il tuo server backend dovrà gestire alcuni componenti chiave, principalmente rispondere ai webhook provenienti da Vonage, creare [Utenti](https://developer.vonage.com/en/conversation/concepts/user) e generare [JWT](https://jwt.io/) per tali utenti. Puoi saperne di più su questi diversi componenti e su come funzionano insieme nella guida [Crea la tua Applicazione](https://developer.vonage.com/en/vonage-client-sdk/create-your-application).

Per vedere un'implementazione predefinita dei concetti discussi nelle guide, puoi modificare o distribuire il [modello Client SDK Sample Server](https://developer.vonage.com/en/cloud-runtime/6b041470-0b95-4d90-bbd7-ac18946edd6a_client-sdk-voice-sample-server) con Code Hub.

Questa guida utilizzerà Node.JS, il [Vonage JWT SDK](https://github.com/Vonage/vonage-node-sdk/blob/3.x/packages/jwt/README.md) e il [Vonage Server SDK](https://github.com/Vonage/vonage-node-sdk). Ma il tuo server backend per il Client SDK può essere scritto in qualsiasi linguaggio. Puoi installare gli SDK utilizzando NPM:

```text
npm install @vonage/jwt @vonage/server-sdk
```

## Creazione di Utenti

Ora che hai installato gli SDK, puoi creare gli Utenti:

```javascript
import { Vonage } from "@vonage/server-sdk";
import { tokenGenerate } from '@vonage/jwt';

const vonage = new Vonage(
    {
      applicationId: process.env.API_APPLICATION_ID,
      privateKey: process.env.PRIVATE_KEY
    }
);

async function createUser(username) {
    try {
        const userResponse = await vonage.users.createUser({
            name: username,
            displayName: username
        });
    } catch (e) {
        console.log(e);
    }
}
```

Puoi leggere di più sui parametri di richiesta e le risposte per l'entità Utenti nella [specifica dell'API](https://developer.vonage.com/en/api/application.v2#User).

## Genera JWT per il Client SDK

Con gli utenti creati, ora puoi creare un JWT per quell'utente. Questo è il JWT che il Client SDK si aspetta quando chiami `createSession`. Creare un JWT per un utente richiederà percorsi ACL e una rivendicazione `sub`. I percorsi ACL vengono utilizzati per limitare gli endpoint a cui il JWT può accedere.

> NOTA: Gli ACL controllano quali azioni l'utente può eseguire. Puoi modificare i percorsi ACL per limitare i permessi.

```javascript
function generateUserJWT(username) {
    const aclPaths =  {
        "/*/users/**": {},
        "/*/conversations/**": {},
        "/*/sessions/**": {},
        "/*/devices/**": {},
        "/*/image/**": {},
        "/*/media/**": {},
        "/*/push/**": {},
        "/*/knocking/**": {},
        "/*/legs/**": {}
    };

    return tokenGenerate(applicationId, privateKey, {
        subject: username,
        acl: aclPaths
    });
}
```

Puoi esporre un endpoint sul tuo server che restituirà questo token generato alle tue applicazioni Android, iOS e Web Client SDK:

```javascript
app.post('/getJWT', async (req, res) => {
    const username = req.body.username;
    try {
        // Esegui autorizzazione e validazione
        res.json(
            {
                jwt: generateUserJWT(username)
            }
        );
    } catch(e) {
        console.log(e);
    }
});
```

## Gestione dei Webhook in Entrata

Quando la funzione `serverCall` viene utilizzata in un'applicazione Android, iOS e Web Client SDK, l'URL che è stato impostato come "URL di risposta" (answer URL) sulla tua Applicazione Vonage riceverà una richiesta in entrata. Puoi saperne di più sul processo nella guida ["Effettua una Chiamata"](https://developer.vonage.com/en/vonage-client-sdk/in-app-voice/guides/make-call).

Il tuo server dovrebbe rispondere con un [Oggetto di Controllo Chiamata](https://developer.vonage.com/en/voice/voice-api/ncco-reference) (Call Control Object) per controllare il flusso della chiamata. Ad esempio, per connettere una chiamata in entrata a un utente chiamato Alice, dovresti restituire un Oggetto di Controllo Chiamata simile a quello riportato di seguito.

```json
[
  {
    "action": "connect",
    "from": "447700900000",
    "endpoint": [
      {
        "type": "app",
        "user": "Alice"
      }
    ]
  }
]
```

## Hai Domande?

Se dovessi avere ulteriori domande, problemi o feedback, contattaci all'indirizzo `devrel@vonage.com` o su [Vonage Developer Community Slack](https://developer.vonage.com/en/community/slack).