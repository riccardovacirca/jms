# Introduzione all'API Voice

Questa pagina ti guiderà attraverso tutti i passaggi necessari per iniziare a utilizzare l'API Voice di Vonage.

## Prerequisiti

Prima di iniziare, avrai bisogno di:

*   [Creare un account Vonage](https://developer.vonage.com/en/voice/voice-api/getting-started#create-a-vonage-account)
*   [Provare l'API Voice](https://developer.vonage.com/en/voice/voice-api/getting-started#try-the-voice-api)
*   [Creare un'Applicazione](https://developer.vonage.com/en/voice/voice-api/getting-started#create-an-application)
*   [Acquistare un Numero](https://developer.vonage.com/en/voice/voice-api/getting-started#rent-a-number)

### Creare un account Vonage

Per lavorare con le nostre API, dovrai [iscriverti per creare un account](https://developer.vonage.com/en/account/guides/dashboard-management#create-and-configure-a-vonage-account). Questo ti fornirà una chiave API e un segreto che potrai usare per accedere alle nostre API.

> Puoi usare l'API Voice per effettuare una chiamata vocale. Usa il numero di prova 123456789 come caller ID, e chiama il numero che hai originariamente fornito durante l'iscrizione. Nota che questa funzionalità è disponibile solo per account demo o di prova finché non aggiungi credito al tuo account.

### Provare l'API Voice

Dopo [esserti iscritto per un account API Vonage](https://ui.idp.vonage.com/ui/auth/registration?icid=tryitfree_adpdocs_nexmodashbdfreetrialsignup_inpagelink), accedi al [Developer Dashboard](https://dashboard.nexmo.com) e vai alla sezione [Effettua una Chiamata Vocale](https://dashboard.nexmo.com/make-a-voice-call). Qui, puoi effettuare una chiamata di prova per vedere l'API Voice in azione.

![Vista del Developer Dashboard per provare l'API Voice](https://developer.vonage.com/api/v1/developer/assets/images/voice-api/getting-started/try-voice-api.png)

Ora vediamo come usare l'API Voice nella tua applicazione.

### Creare un'Applicazione

### Usando il Dashboard Vonage

### Usando la CLI Vonage

```powershell
vonage apps create 'Nome della tua applicazione'

✅ Creazione Applicazione
Salvataggio della chiave privata ... Fatto!
Applicazione creata

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  Nessuna Abilitata
```

```cmd
vonage apps create 'Nome della tua applicazione'

✅ Creazione Applicazione
Salvataggio della chiave privata ... Fatto!
Applicazione creata

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  Nessuna Abilitata
```

```powershell
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice `
  --voice-answer-url='https://esempio.com/webhooks/voice/answer' `
  --voice-event-url='https://esempio.com/webhooks/voice/event' `
  --voice-fallback-url='https://esempio.com/webhooks/voice/fallback'
  
✅ Recupero Applicazione
✅ Aggiunta della capacità voce alla applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
 VOCE:
    Usa Callback Firmati: Attivo
    TTL Conversazione: 41 ore
    Tempo Persistenza Gamba: 6 giorni
    URL Evento: [POST] https://esempio.com/webhooks/voice/event
    URL Risposta: [POST] https://esempio.com/webhooks/voice/answer
    URL Fallback: [POST] https://esempio.com/webhooks/voice/fallback
```

```cmd
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice ^
  --voice-answer-url='https://esempio.com/webhooks/voice/answer' ^
  --voice-event-url='https://esempio.com/webhooks/voice/event' ^
  --voice-fallback-url='https://esempio.com/webhooks/voice/fallback'
  
✅ Recupero Applicazione
✅ Aggiunta della capacità voce all'applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
 VOCE:
    Usa Callback Firmati: Attivo
    TTL Conversazione: 41 ore
    Tempo Persistenza Gamba: 6 giorni
    URL Evento: [POST] https://esempio.com/webhooks/voice/event
    URL Risposta: [POST] https://esempio.com/webhooks/voice/answer
    URL Fallback: [POST] https://esempio.com/webhooks/voice/fallback
```

```bash
$ vonage apps create 'Nome della tua applicazione'
$ ✅ Creazione Applicazione
$ Salvataggio della chiave privata ... Fatto!
$ Applicazione creata
$ Nome: Nome della tua applicazione
$ ID Applicazione: 00000000-0000-0000-0000-000000000000
$ Miglioramento AI: Disattivato
$ Chiave Privata/Pubblica: Impostata
$ Capacità:
$ Nessuna Abilitata
```

```bash
$ vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice \
--voice-answer-url='https://esempio.com/webhooks/voice/answer' \
--voice-event-url='https://esempio.com/webhooks/voice/event' \
--voice-fallback-url='https://esempio.com/webhooks/voice/fallback'
$ ✅ Recupero Applicazione
$ ✅ Aggiunta della capacità voce all'applicazione 00000000-0000-0000-0000-000000000000
$ Nome: Nome della tua applicazione
$ ID Applicazione: 00000000-0000-0000-0000-000000000000
$ Miglioramento AI: Disattivato
$ Chiave Privata/Pubblica: Impostata
$ Capacità:
$ VOCE:
$ Usa Callback Firmati: Attivo
$ TTL Conversazione: 41 ore
$ Tempo Persistenza Gamba: 6 giorni
$ URL Evento: [POST] https://esempio.com/webhooks/voice/event
$ URL Risposta: [POST] https://esempio.com/webhooks/voice/answer
$ URL Fallback: [POST] https://esempio.com/webhooks/voice/fallback
```

### Powershell (Windows)

```powershell
vonage apps create 'Nome della tua applicazione'

✅ Creazione Applicazione
Salvataggio della chiave privata ... Fatto!
Applicazione creata

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  Nessuna Abilitata
```

### CMD (Windows)

```cmd
vonage apps create 'Nome della tua applicazione'

✅ Creazione Applicazione
Salvataggio della chiave privata ... Fatto!
Applicazione creata

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  Nessuna Abilitata
```

### Bash

```bash
$ vonage apps create 'Nome della tua applicazione'
$ ✅ Creazione Applicazione
$ Salvataggio della chiave privata ... Fatto!
$ Applicazione creata
$ Nome: Nome della tua applicazione
$ ID Applicazione: 00000000-0000-0000-0000-000000000000
$ Miglioramento AI: Disattivato
$ Chiave Privata/Pubblica: Impostata
$ Capacità:
$ Nessuna Abilitata
```

### Powershell (Windows)

```powershell
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice `
  --voice-answer-url='https://esempio.com/webhooks/voice/answer' `
  --voice-event-url='https://esempio.com/webhooks/voice/event' `
  --voice-fallback-url='https://esempio.com/webhooks/voice/fallback'
  
✅ Recupero Applicazione
✅ Aggiunta della capacità voce all'applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
 VOCE:
    Usa Callback Firmati: Attivo
    TTL Conversazione: 41 ore
    Tempo Persistenza Gamba: 6 giorni
    URL Evento: [POST] https://esempio.com/webhooks/voice/event
    URL Risposta: [POST] https://esempio.com/webhooks/voice/answer
    URL Fallback: [POST] https://esempio.com/webhooks/voice/fallback
```

### CMD (Windows)

```cmd
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice ^
  --voice-answer-url='https://esempio.com/webhooks/voice/answer' ^
  --voice-event-url='https://esempio.com/webhooks/voice/event' ^
  --voice-fallback-url='https://esempio.com/webhooks/voice/fallback'
  
✅ Recupero Applicazione
✅ Aggiunta della capacità voce all'applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome della tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Miglioramento AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
 VOCE:
    Usa Callback Firmati: Attivo
    TTL Conversazione: 41 ore
    Tempo Persistenza Gamba: 6 giorni
    URL Evento: [POST] https://esempio.com/webhooks/voice/event
    URL Risposta: [POST] https://esempio.com/webhooks/voice/answer
    URL Fallback: [POST] https://esempio.com/webhooks/voice/fallback
```

### Bash

```bash
$ vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice \
--voice-answer-url='https://esempio.com/webhooks/voice/answer' \
--voice-event-url='https://esempio.com/webhooks/voice/event' \
--voice-fallback-url='https://esempio.com/webhooks/voice/fallback'
$ ✅ Recupero Applicazione
$ ✅ Aggiunta della capacità voce alla applicazione 00000000-0000-0000-0000-000000000000
$ Nome: Nome della tua applicazione
$ ID Applicazione: 00000000-0000-0000-0000-000000000000
$ Miglioramento AI: Disattivato
$ Chiave Privata/Pubblica: Impostata
$ Capacità:
$ VOCE:
$ Usa Callback Firmati: Attivo
$ TTL Conversazione: 41 ore
$ Tempo Persistenza Gamba: 6 giorni
$ URL Evento: [POST] https://esempio.com/webhooks/voice/event
$ URL Risposta: [POST] https://esempio.com/webhooks/voice/answer
$ URL Fallback: [POST] https://esempio.com/webhooks/voice/fallback
```

### Acquistare un Numero

Per acquistare un numero, devi prima [aggiungere credito al tuo account](https://developer.vonage.com/en/account/guides/payments#add-a-payment-method).

> Puoi saltare questo passaggio se vuoi usare il numero di prova 123456789 come caller ID, e chiamare il numero che hai originariamente fornito durante l'iscrizione.

### Usando il Dashboard Vonage

### Usando la CLI Vonage

```powershell
vonage numbers search US

✅ Ricerca numeri

C'è 1 numero disponibile per l'acquisto negli Stati Uniti

Numero       Tipo    Caratteristiche       Costo Mensile Costo Setup
-----------  ------  --------------------  ------------- -----------
16127779311  Mobile  MMS, SMS, VOICE       €0.90         €0.00

Usa vonage numbers buy per acquistare.
```

```cmd
vonage numbers search US

✅ Ricerca numeri

C'è 1 numero disponibile per l'acquisto negli Stati Uniti

Numero       Tipo    Caratteristiche       Costo Mensile Costo Setup
-----------  ------  --------------------  ------------- -----------
16127779311  Mobile  MMS, SMS, VOICE       €0.90         €0.00

Usa vonage numbers buy per acquistare.
```

```powershell
vonage numbers buy US 16127779311 
✅ Ricerca numeri
Sei sicuro di voler acquistare il numero 16127779311 per €0.90? [y/n] y

✅ Acquisto numero
Numero 16127779311 acquistato

Numero: 16127779311 
Paese: 🇺🇸 Stati Uniti
Tipo: Mobile
Caratteristiche: MMS, SMS, VOICE
Costo Mensile: €0.90
Costo Setup: €0.00
ID Applicazione Collegata: Non collegata ad alcuna applicazione
Callback Voce: Non Impostato
Valore Callback Voce: Non Impostato
Callback Stato Voce: Non Impostato
```

```cmd
vonage numbers buy US 16127779311 
✅ Ricerca numeri
Sei sicuro di voler acquistare il numero 16127779311 per €0.90? [y/n] y

✅ Acquisto numero
Numero 16127779311 acquistato

Numero: 16127779311 
Paese: 🇺🇸 Stati Uniti
Tipo: Mobile
Caratteristiche: MMS, SMS, VOICE
Costo Mensile: €0.90
Costo Setup: €0.00
ID Applicazione Collegata: Non collegata ad alcuna applicazione
Callback Voce: Non Impostato
Valore Callback Voce: Non Impostato
Callback Stato Voce: Non Impostato
```

```bash
$ vonage numbers search US
$ ✅ Ricerca numeri
$ C'è 1 numero disponibile per l'acquisto negli Stati Uniti
$ Numero Tipo Caratteristiche Costo Mensile Costo Setup
$ ----------- ------ -------------------- ------------- -----------
$ 16127779311 Mobile MMS, SMS, VOICE €0.90 €0.00
$ Usa vonage numbers buy per acquistare.
```

```bash
$ vonage numbers buy US 16127779311
$ ✅ Ricerca numeri
$ Sei sicuro di voler acquistare il numero 16127779311 per €0.90? [y/n] y
$ ✅ Acquisto numero
$ Numero 16127779311 acquistato
$ Numero: 16127779311
$ Paese: 🇺🇸 Stati Uniti
$ Tipo: Mobile
$ Caratteristiche: MMS, SMS, VOICE
$ Costo Mensile: €0.90
$ Costo Setup: €0.00
$ ID Applicazione Collegata: Non collegata ad alcuna applicazione
$ Callback Voce: Non Impostato
$ Valore Callback Voce: Non Impostato
$ Callback Stato Voce: Non Impostato
```

### Powershell (Windows)

```powershell
vonage numbers search US

✅ Ricerca numeri

C'è 1 numero disponibile per l'acquisto negli Stati Uniti

Numero       Tipo    Caratteristiche       Costo Mensile Costo Setup
-----------  ------  --------------------  ------------- -----------
16127779311  Mobile  MMS, SMS, VOICE       €0.90         €0.00

Usa vonage numbers buy per acquistare.
```

### CMD (Windows)

```cmd
vonage numbers search US

✅ Ricerca numeri

C'è 1 numero disponibile per l'acquisto negli Stati Uniti

Numero       Tipo    Caratteristiche       Costo Mensile Costo Setup
-----------  ------  --------------------  ------------- -----------
16127779311  Mobile  MMS, SMS, VOICE       €0.90         €0.00

Usa vonage numbers buy per acquistare.
```

### Bash

```bash
$ vonage numbers search US
$ ✅ Ricerca numeri
$ C'è 1 numero disponibile per l'acquisto negli Stati Uniti
$ Numero Tipo Caratteristiche Costo Mensile Costo Setup
$ ----------- ------ -------------------- ------------- -----------
$ 16127779311 Mobile MMS, SMS, VOICE €0.90 €0.00
$ Usa vonage numbers buy per acquistare.
```

### Powershell (Windows)

```powershell
vonage numbers buy US 16127779311 
✅ Ricerca numeri
Sei sicuro di voler acquistare il numero 16127779311 per €0.90? [y/n] y

✅ Acquisto numero
Numero 16127779311 acquistato

Numero: 16127779311 
Paese: 🇺🇸 Stati Uniti
Tipo: Mobile
Caratteristiche: MMS, SMS, VOICE
Costo Mensile: €0.90
Costo Setup: €0.00
ID Applicazione Collegata: Non collegata ad alcuna applicazione
Callback Voce: Non Impostato
Valore Callback Voce: Non Impostato
Callback Stato Voce: Non Impostato
```

### CMD (Windows)

```cmd
vonage numbers buy US 16127779311 
✅ Ricerca numeri
Sei sicuro di voler acquistare il numero 16127779311 per €0.90? [y/n] y

✅ Acquisto numero
Numero 16127779311 acquistato

Numero: 16127779311 
Paese: 🇺🇸 Stati Uniti
Tipo: Mobile
Caratteristiche: MMS, SMS, VOICE
Costo Mensile: €0.90
Costo Setup: €0.00
ID Applicazione Collegata: Non collegata ad alcuna applicazione
Callback Voce: Non Impostato
Valore Callback Voce: Non Impostato
Callback Stato Voce: Non Impostato
```

### Bash

```bash
$ vonage numbers buy US 16127779311
$ ✅ Ricerca numeri
$ Sei sicuro di voler acquistare il numero 16127779311 per €0.90? [y/n] y
$ ✅ Acquisto numero
$ Numero 16127779311 acquistato
$ Numero: 16127779311
$ Paese: 🇺🇸 Stati Uniti
$ Tipo: Mobile
$ Caratteristiche: MMS, SMS, VOICE
$ Costo Mensile: €0.90
$ Costo Setup: €0.00
$ ID Applicazione Collegata: Non collegata ad alcuna applicazione
$ Callback Voce: Non Impostato
$ Valore Callback Voce: Non Impostato
$ Callback Stato Voce: Non Impostato
```

## Effettuare una Chiamata in Uscita

Il modo principale per interagire con la piattaforma API voice di Vonage è tramite l'[API pubblica](https://developer.vonage.com/en/voice/voice-api/technical-details). Per effettuare una chiamata in uscita, devi fare una richiesta `POST` a `https://api.nexmo.com/v1/calls`.

Per effettuare la tua prima chiamata con l'API Voice, scegli il tuo linguaggio qui sotto e sostituisci le seguenti variabili nel codice di esempio:

| Chiave | Descrizione |
| --- | --- |
| `VONAGE_NUMBER` | Il tuo numero Vonage da cui verrà effettuata la chiamata. Ad esempio `447700900000`. Se hai saltato il passaggio [Acquistare un Numero](https://developer.vonage.com/en/voice/voice-api/getting-started#rent-a-number), usa il numero di prova "123456789". |
| `TO_NUMBER` | Il numero che desideri chiamare in formato E.164. Ad esempio `447700900001`. Se hai saltato il passaggio [Acquistare un Numero](https://developer.vonage.com/en/voice/voice-api/getting-started#rent-a-number), usa il numero che hai originariamente fornito durante l'iscrizione. |

### cURL

#### Genera il tuo JWT

Esegui il seguente comando nel tuo terminale per creare il [JWT](https://developer.vonage.com/en/concepts/guides/authentication#json-web-tokens-jwt) per l'autenticazione:

```bash
export JWT=$(nexmo jwt:generate $PATH_TO_PRIVATE_KEY application_id=$NEXMO_APPLICATION_ID)
```

```sh
curl -X POST https://api.nexmo.com/v1/calls\
  -H "Authorization: Bearer $JWT"\
  -H "Content-Type: application/json"\
  -d '{"to":[{"type": "phone","number": "'$VOICE_TO_NUMBER'"}],
      "from": {"type": "phone","number": "'$VONAGE_VIRTUAL_NUMBER'"},
      "answer_url":["'"$VOICE_ANSWER_URL"'"]}'
```

```bash
$ bash make-an-outbound-call.sh
```

### Node.js

#### Installa le dipendenze

```bash
npm install @vonage/server-sdk
```

#### Inizializza le tue dipendenze

Crea un file chiamato `make-an-outbound-call.js` e aggiungi il seguente codice:

[Visualizza codice sorgente completo](https://github.com/Vonage/vonage-node-code-snippets/blob/3164e3fd94822aec9ae926c1771d58636e01c4a7/voice/make-an-outbound-call.js#L9-L14)

```javascript
const { Vonage } = require('@vonage/server-sdk');

const vonage = new Vonage({
  applicationId: VONAGE_APPLICATION_ID,
  privateKey: VONAGE_PRIVATE_KEY,
});
```

```javascript
vonage.voice.createOutboundCall({
  to: [
    {
      type: 'phone',
      number: VOICE_TO_NUMBER,
    },
  ],
  from: {
    type: 'phone',
    number: VONAGE_VIRTUAL_NUMBER,
  },
  answer_url: [VOICE_ANSWER_URL],
})
  .then((resp) => console.log(resp))
  .catch((error) => console.error(error));
```

```bash
$ node make-an-outbound-call.js
```

### Kotlin

#### Installa le dipendenze

Aggiungi quanto segue a `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk-kotlin:2.1.1'
```

#### Inizializza le tue dipendenze

Crea un file chiamato `OutboundTextToSpeechCall` e aggiungi il seguente codice al metodo `main`:

[Visualizza codice sorgente completo](https://github.com/Vonage/vonage-kotlin-code-snippets/blob/1a41b5234a23ab2e937f5963d0d698a8934ca25d/src/main/kotlin/com/vonage/quickstart/kt/voice/OutboundTextToSpeechCall.kt#L28-L31)

```kotlin
val client = Vonage {
    applicationId(VONAGE_APPLICATION_ID)
    privateKeyPath(VONAGE_PRIVATE_KEY_PATH)
}
```

```kotlin
val callEvent = client.voice.createCall {
    toPstn(VOICE_TO_NUMBER)
    from(VONAGE_VIRTUAL_NUMBER)
    answerUrl(VOICE_ANSWER_URL)
}
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.kt.voice.OutboundTextToSpeechCall
```

### Java

#### Installa le dipendenze

Aggiungi quanto segue a `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk:9.3.1'
```

#### Inizializza le tue dipendenze

Crea un file chiamato `OutboundTextToSpeech` e aggiungi il seguente codice al metodo `main`:

[Visualizza codice sorgente completo](https://github.com/Vonage/vonage-java-code-snippets/blob/223b17f76d061456a202218b0c05011274bd5550/src/main/java/com/vonage/quickstart/voice/OutboundTextToSpeech.java#L30-L33)

```java
VonageClient client = VonageClient.builder()
        .applicationId(VONAGE_APPLICATION_ID)
        .privateKeyPath(VONAGE_PRIVATE_KEY_PATH)
        .build();
```

```java
client.getVoiceClient().createCall(new Call(VOICE_TO_NUMBER, VONAGE_VIRTUAL_NUMBER, VOICE_ANSWER_URL));
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.voice.OutboundTextToSpeech
```

### .NET

#### Installa le dipendenze

```bash
Install-Package Vonage
```

```csharp
var creds = Credentials.FromAppIdAndPrivateKeyPath(VONAGE_APPLICATION_ID, VONAGE_PRIVATE_KEY_PATH);
var client = new VonageClient(creds);

var answerUrl = "https://nexmo-community.github.io/ncco-examples/text-to-speech.json";
var toEndpoint = new PhoneEndpoint() { Number = VOICE_TO_NUMBER };
var fromEndpoint = new PhoneEndpoint() { Number = VONAGE_VIRTUAL_NUMBER };

var command = new CallCommand() { To = new Endpoint[] { toEndpoint }, From = fromEndpoint, AnswerUrl = new[] { answerUrl } };
var response = await client.VoiceClient.CreateCallAsync(command);
```

### PHP

#### Installa le dipendenze

```bash
composer require vonage/client
```

```php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../vendor/autoload.php';

// Building Blocks
// 1. Effettua una Chiamata Telefonica
// 2. Riproduci Sintesi Vocale

$keypair = new \Vonage\Client\Credentials\Keypair(
    file_get_contents(VONAGE_APPLICATION_PRIVATE_KEY_PATH),
    VONAGE_APPLICATION_ID
);
$client = new \Vonage\Client($keypair);

$outboundCall = new \Vonage\Voice\OutboundCall(
    new \Vonage\Voice\Endpoint\Phone(TO_NUMBER),
    new \Vonage\Voice\Endpoint\Phone(VONAGE_NUMBER)
);
$outboundCall->setAnswerWebhook(
    new \Vonage\Voice\Webhook(
        'https://raw.githubusercontent.com/nexmo-community/ncco-examples/gh-pages/text-to-speech.json',
        \Vonage\Voice\Webhook::METHOD_GET
    )
);
$response = $client->voice()->createOutboundCall($outboundCall);

var_dump($response);
```

```bash
$ php text-to-speech-outbound.php
```

### Python

#### Installa le dipendenze

```bash
pip install vonage python-dotenv
```

```python
from vonage import Auth, Vonage
from vonage_voice import CreateCallRequest, Phone, ToPhone

client = Vonage(
    Auth(
        application_id=VONAGE_APPLICATION_ID,
        private_key=VONAGE_PRIVATE_KEY,
    )
)

response = client.voice.create_call(
    CreateCallRequest(
        answer_url=[VOICE_ANSWER_URL],
        to=[ToPhone(number=VOICE_TO_NUMBER)],
        from_=Phone(number=VONAGE_VIRTUAL_NUMBER),
    )
)

pprint(response)
```

```bash
$ python voice/make-an-outbound-call.py
```

### Ruby

#### Installa le dipendenze

```bash
gem install vonage
```

```ruby
client = Vonage::Client.new(
  application_id: VONAGE_APPLICATION_ID,
  private_key: VONAGE_PRIVATE_KEY
)

response = client.voice.create(
  to: [{
    type: 'phone',
    number: VOICE_TO_NUMBER
  }],
  from: {
    type: 'phone',
    number: VONAGE_VIRTUAL_NUMBER
  },
  answer_url: [
    VOICE_ANSWER_URL
  ]
)
```

```bash
$ ruby outbound_tts_call.rb
```

Per semplificare, Vonage fornisce [Server SDK](https://developer.vonage.com/en/tools) in vari linguaggi che si occupano dell'autenticazione e della creazione del corpo della richiesta corretto per te.

## E Adesso?

Una volta effettuata la tua prima chiamata, sei pronto per provare altri aspetti dell'API Voice. Raccomandiamo di iniziare con la pagina [Dettagli Tecnici](https://developer.vonage.com/en/voice/voice-api/technical-details) per una panoramica completa dell'API Voice di Vonage. Per comprendere i vari flussi di chiamata, consulta la guida [Flusso di Chiamata](https://developer.vonage.com/en/voice/voice-api/concepts/call-flow?source=voice). Se sei interessato a costruire un'applicazione base di Notifiche Vocali, fai riferimento alla guida pratica [Notifiche Vocali](https://developer.vonage.com/en/voice/voice-api/guides/voice-notifications?source=voice). Per ulteriori informazioni, consulta la nostra documentazione dell'API Voice.