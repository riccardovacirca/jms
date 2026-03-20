# Effettuare una chiamata in uscita

Questo frammento di codice effettua una chiamata in uscita e riproduce un messaggio di sintesi vocale quando la chiamata viene risposta.

## Esempio

Sostituisci le seguenti variabili nel codice di esempio:

| Chiave                    | Descrizione                                                                                                |
| ------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `VONAGE_VIRTUAL_NUMBER`   | Il tuo numero Vonage. Es. 447700900000                                                                    |
| `VOICE_TO_NUMBER`         | Il numero del destinatario da chiamare, es. 447700900002.                                                 |
| `VOICE_ANSWER_URL`        | L'URL di risposta. Ad esempio https://raw.githubusercontent.com/nexmo-community/ncco-examples/gh-pages/text-to-speech.json. |

### cURL

#### Genera il tuo JWT

Esegui il seguente comando nel prompt del terminale per creare il [JWT](https://developer.vonage.com/en/concepts/guides/authentication#json-web-tokens-jwt) per l'autenticazione:

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

#### Inizializza le dipendenze

Crea un file chiamato `make-an-outbound-call.js` e aggiungi il seguente codice:

[Vedi il codice sorgente completo](https://github.com/Vonage/vonage-node-code-snippets/blob/3164e3fd94822aec9ae926c1771d58636e01c4a7/voice/make-an-outbound-call.js#9-L14)

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

#### Inizializza le dipendenze

Crea un file chiamato `OutboundTextToSpeechCall` e aggiungi il seguente codice al metodo `main`:

[Vedi il codice sorgente completo](https://github.com/Vonage/vonage-kotlin-code-snippets/blob/1a41b5234a23ab2e937f5963d0d698a8934ca25d/src/main/kotlin/com/vonage/quickstart/kt/voice/OutboundTextToSpeechCall.kt#L28-L31)

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

#### Inizializza le dipendenze

Crea un file chiamato `OutboundTextToSpeech` e aggiungi il seguente codice al metodo `main`:

[Vedi il codice sorgente completo](https://github.com/Vonage/vonage-java-code-snippets/blob/223b17f76d061456a202218b0c05011274bd5550/src/main/java/com/vonage/quickstart/voice/OutboundTextToSpeech.java#L30-L33)

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

// Blocchi di Costruzione
// 1. Effettua una chiamata telefonica
// 2. Riproduci un messaggio di sintesi vocale

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

## Provalo

Quando esegui il codice, il numero `TO_NUMBER` verrà chiamato e, se la chiamata viene risposta, verrà ascoltato un messaggio di sintesi vocale.

## Letture Consigliate

*   [Notifiche Vocali](https://developer.vonage.com/en/voice/voice-api/guides/voice-notifications) - In questa guida imparerai come contattare una lista di persone per telefono, comunicare un messaggio e vedere chi ha confermato di averlo ricevuto. Questi avvisi critici basati sulla voce sono più persistenti di un messaggio di testo, rendendo il tuo messaggio più probabile che venga notato. Inoltre, con la conferma del destinatario, puoi essere certo che il tuo messaggio sia arrivato.
*   [Conferenza Telefonica](https://developer.vonage.com/en/voice/voice-api/guides/conference-calling) - Questa guida spiega i due concetti che Vonage associa a una chiamata: una "gamba" (leg) e una "conversazione".
*   [Bot Vocale con Google Dialogflow](https://developer.vonage.com/en/voice/voice-api/guides/voice-bot-dialogflow) - Questa guida ti aiuterà a iniziare con un esempio di bot Dialogflow e a interagire con esso da chiamate telefoniche utilizzando i codici di esempio forniti tramite l'API Voce di Vonage.
