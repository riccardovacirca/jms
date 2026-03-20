# Riferimento NCCO

Un Call Control Object (NCCO) è rappresentato da un array JSON. Puoi usarlo per controllare il flusso di una chiamata dell'API Voce. Affinché il tuo NCCO venga eseguito correttamente, gli oggetti JSON devono essere validi.

Durante lo sviluppo e il test degli NCCO, puoi utilizzare il Voice Playground per provare gli NCCO in modo interattivo. Puoi [leggere di più a riguardo nella Panoramica dell'API Voce](https://developer.vonage.com/en/voice/voice-api/technical-details#voice-playground) o [andare direttamente al Voice Playground nella Dashboard](https://dashboard.nexmo.com/voice/playground).

## Azioni NCCO

L'ordine delle azioni nell'NCCO controlla il flusso della Chiamata. Le azioni che devono completarsi prima che l'azione successiva possa essere eseguita sono *sincrone*. Altre azioni sono *asincrone*. Cioè, dovrebbero continuare oltre le azioni successive fino a quando non viene soddisfatta una condizione. Ad esempio, un'azione `record` termina quando viene soddisfatta l'opzione `endOnSilence`. Quando tutte le azioni nell'NCCO sono complete, la Chiamata termina.

Le azioni NCCO e le opzioni e i tipi per ciascuna azione sono:

| Azione | Descrizione | Sincrona |
| --- | --- | --- |
| [record](https://developer.vonage.com/en/voice/voice-api/ncco-reference#record) | Tutta o parte di una Chiamata | No |
| [conversation](https://developer.vonage.com/en/voice/voice-api/ncco-reference#conversation) | Crea o unisciti a una [Conversazione](https://developer.vonage.com/en/conversation/concepts/conversation) esistente | Sì |
| [connect](https://developer.vonage.com/en/voice/voice-api/ncco-reference#connect) | A un endpoint collegabile come un numero di telefono o un'estensione VBC. | Sì |
| [talk](https://developer.vonage.com/en/voice/voice-api/ncco-reference#talk) | Invia sintesi vocale a una Conversazione. | Sì, a meno che bargeIn=true |
| [stream](https://developer.vonage.com/en/voice/voice-api/ncco-reference#stream) | Invia file audio a una Conversazione. | Sì, a meno che bargeIn=true |
| [input](https://developer.vonage.com/en/voice/voice-api/ncco-reference#input) | Raccogli cifre o cattura input vocale dalla persona che stai chiamando. | Sì |
| [notify](https://developer.vonage.com/en/voice/voice-api/ncco-reference#notify) | Invia una richiesta alla tua applicazione per tracciare l'avanzamento attraverso un NCCO | Sì |
| [wait](https://developer.vonage.com/en/voice/voice-api/ncco-reference#wait) | Sospende l'esecuzione per un numero specificato di secondi | Sì |
| [transfer](https://developer.vonage.com/en/voice/voice-api/ncco-reference#transfer) | Sposta le "gambe" (legs) della chiamata da una conversazione corrente a un'altra conversazione esistente | Sì |

> Nota: "Connect an inbound call" fornisce un esempio di come servire i tuoi NCCO a Vonage dopo che una Chiamata o una Conferenza è stata avviata.

**Nota che in tutte le azioni, il parametro `eventUrl` DEVE essere un array, anche se contiene un solo valore.**

## Record

Usa l'azione `record` per registrare una Chiamata o parte di una Chiamata:

```json
[
  {
    "action": "record",
    "eventUrl": ["https://example.com/recordings"]
  },
  {
    "action": "connect",
    "eventUrl": ["https://example.com/events"],
    "from":"447700900000",
    "endpoint": [
      {
        "type": "phone",
        "number": "447700900001"
      }
    ]
  }
]
```

L'azione record è asincrona. Puoi definire una condizione sincrona - `endOnSilence`, `timeOut` o `endOnKey` - per terminare la registrazione quando viene soddisfatta. Se non viene impostata alcuna condizione, la registrazione funzionerà in modo asincrono e passerà istantaneamente all'azione successiva continuando a registrare la chiamata. La registrazione terminerà e invierà l'evento rilevante solo quando la chiamata sarà terminata. Questo è usato per scenari simili al monitoraggio delle chiamate.

Puoi trascrivere una registrazione utilizzando l'[opzione](https://developer.vonage.com/en/voice/voice-api/ncco-reference#transcription-settings) `transcription`. Una volta completata la trascrizione della registrazione, un callback verrà inviato a un `eventUrl`. Utilizzando le impostazioni di trascrizione puoi specificare un `eventUrl` personalizzato e una `language` per le tue trascrizioni. Si prega di notare, questa è una funzionalità a pagamento; le tariffe esatte si trovano nella pagina [Voice API Pricing](https://www.vonage.co.uk/communications-apis/voice/pricing/) sotto 'Programmable Features'.

Per informazioni sul flusso di lavoro da seguire, vedi [Recording](https://developer.vonage.com/en/voice/voice-api/guides/recording).

Puoi utilizzare le seguenti opzioni per controllare un'azione `record`:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `format` | Registra la Chiamata in un formato specifico. Le opzioni sono: mp3, wav, ogg. Il valore predefinito è `mp3`, o `wav` quando si registrano più di 2 canali. | No |
| `split` | Registra l'audio inviato e ricevuto in canali separati di una registrazione stereo—imposta su `conversation` per abilitare questo. | No |
| `channels` | Il numero di canali da registrare (massimo `32`). Se il numero di partecipanti supera `channels`, eventuali partecipanti aggiuntivi verranno aggiunti all'ultimo canale nel file. Anche split `conversation` deve essere abilitato. | No |
| `endOnSilence` | Interrompe la registrazione dopo n secondi di silenzio. Una volta che la registrazione è interrotta, i dati della registrazione vengono inviati a `event_url`. L'intervallo di valori possibili è 3<=`endOnSilence`<=10. | No |
| `endOnKey` | Interrompe la registrazione quando viene premuto un tasto sul telefono. I valori possibili sono: `*`, `#` o qualsiasi singola cifra es. `9`. | No |
| `timeOut` | La lunghezza massima di una registrazione in secondi. Una volta che la registrazione è interrotta, i dati della registrazione vengono inviati a `event_url`. L'intervallo di valori possibili è tra `3` secondi e `7200` secondi (2 ore). | No |
| `beepStart` | Imposta su `true` per riprodurre un segnale acustico quando inizia una registrazione. | No |
| `eventUrl` | L'URL dell'endpoint webhook che viene chiamato in modo asincrono quando una registrazione è terminata. Se la registrazione del messaggio è ospitata da Vonage, questo webhook contiene [l'URL di cui hai bisogno per scaricare la registrazione e altri metadati](https://developer.vonage.com/en/voice/voice-api/ncco-reference#record-return-parameters). | No |
| `eventMethod` | Il metodo HTTP utilizzato per effettuare la richiesta a `eventUrl`. Il valore predefinito è `POST`. | No |
| `transcription` [Beta] | Imposta su un oggetto vuoto, `{}`, per utilizzare i valori predefiniti o personalizza con [Impostazioni di Trascrizione](https://developer.vonage.com/en/voice/voice-api/ncco-reference#transcription-settings) | No |

### Impostazioni di Trascrizione

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `language` | La lingua ([formato BCP-47](https://tools.ietf.org/html/bcp47)) per la registrazione che stai trascrivendo. Attualmente supporta le stesse lingue di Automatic Speech Recording, e un elenco è disponibile [qui](https://developer.vonage.com/en/voice/voice-api/guides/asr#language). | No |
| `eventUrl` | L'URL dell'endpoint webhook che viene chiamato in modo asincrono quando una trascrizione è terminata. | No |
| `eventMethod` | Il metodo HTTP utilizzato da Vonage per effettuare la richiesta a eventUrl. Il valore predefinito è `POST`. | No |
| `sentimentAnalysis` [[Developer Preview]](https://developer.vonage.com/en/product-lifecycle/dev-preview) | Esegui l'analisi del sentimento sui segmenti di trascrizione della registrazione della chiamata. Restituirà un valore tra -1 (sentimento negativo) e 1 (sentimento positivo) per ogni segmento. Il valore predefinito è `false`. | No |

> Nota: c'è un limite massimo di 2 ore per la trascrizione delle chiamate vocali.

### Parametri Restituiti da Record

Vedi il [Riferimento Webhook](https://developer.vonage.com/en/voice/voice-api/webhook-reference#record) per i parametri di record o trascrizione che vengono restituiti all'`eventUrl`.

## Conversation

Puoi usare l'azione `conversation` per creare conferenze standard o moderate, preservando il contesto di comunicazione. Usare `conversation` con lo stesso `name` riutilizza la stessa [Conversazione](https://developer.vonage.com/en/conversation/concepts/conversation) persistente. La prima persona che chiama il numero virtuale assegnato alla conversazione la crea. Questa azione è sincrona.

> Nota: puoi invitare fino a 200 persone nella tua Conversazione.

I seguenti esempi NCCO mostrano come configurare diversi tipi di Conversazione. Puoi utilizzare i parametri della richiesta GET del webhook `answer_url` per assicurarti di consegnare un NCCO ai partecipanti e un altro al moderatore.

### Conferenza standard

```json
[
  {
    "action": "conversation",
    "name": "nexmo-conference-standard",
    "record": true,
    "transcription": {
      "eventUrl": [ "https://example.com/transcription" ],
      "eventMethod": "POST",
      "language": "he-IL"
    }
  }
]
```

### Controlli Audio Selettivi

```javascript
// Poiché il cliente è la prima persona a unirsi, non c'è una voce canHear/canSpeak
// L'ID della "gamba" (leg) del cliente è 6a4d6af0-55a6-4667-be90-8614e4c8e83c
[
  {
    "action": "conversation",
    "name": "selective-audio-demo",
    "startOnEnter": false,
    "musicOnHoldUrl": ["https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3"],
  }
]

// L'agente si unisce e può sia sentire che parlare con il cliente
// L'ID della "gamba" dell'agente è 533c0874-f43d-446c-a153-f35bf30783fa
[
  {
    "action": "conversation",
    "name": "selective-audio-demo",
    "startOnEnter": true,
    "record": true,
    "canHear": ["6a4d6af0-55a6-4667-be90-8614e4c8e83c"], // ID "gamba" cliente
    "canSpeak": ["6a4d6af0-55a6-4667-be90-8614e4c8e83c"] // ID "gamba" cliente
  }
]

// Infine, il supervisore si unisce alla conversazione. Può sentire sia il cliente
// che l'agente, ma può parlare solo con l'agente
// L'ID della "gamba" del supervisore è e2833e43-db39-4c1a-b689-d17ad2cf3529
[
  {
    "action": "conversation",
    "name": "selective-audio-demo",
    "startOnEnter": true,
    "record": true,
    "canHear": ["6a4d6af0-55a6-4667-be90-8614e4c8e83c", "533c0874-f43d-446c-a153-f35bf30783fa"] // ID "gamba" cliente, ID "gamba" agente
    "canSpeak": ["533c0874-f43d-446c-a153-f35bf30783fa"] // ID "gamba" agente
  }
]
```

### Conferenza moderata, moderatore

```json
[
  {
    "action": "conversation",
    "name": "nexmo-conference-moderated",
    "record": true,
    "startOnEnter": true
  }
]
```

### Conferenza moderata, partecipante

```json
[
  {
    "action": "talk",
    "text": "Benvenuto a una conferenza moderata Vonage. Ti collegheremo quando un agente sarà disponibile"
  },
  {
    "action": "conversation",
    "name": "nexmo-conference-moderated",
    "startOnEnter": false,
    "musicOnHoldUrl": ["https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3"]
  }
]
```

Puoi utilizzare le seguenti opzioni per controllare un'azione *conversation*:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `name` | Il nome della stanza Conversazione. I nomi sono organizzati per spazio dei nomi a livello di applicazione e [regione](https://developer.vonage.com/en/voice/voice-api/concepts/regions?source=voice). | Sì |
| `musicOnHoldUrl` | Un URL del file mp3 da riprodurre in streaming ai partecipanti finché la conversazione non inizia. Per impostazione predefinita la conversazione inizia quando la prima persona chiama il numero virtuale associato alla tua app Voce. Per riprodurre in streaming questo mp3 prima che il moderatore si unisca alla conversazione, imposta startOnEnter su false per tutti gli utenti diversi dal moderatore. | No |
| `startOnEnter` | Il valore predefinito true garantisce che la conversazione inizi quando questo chiamante si unisce alla conversazione `name`. Imposta su false per i partecipanti in una conversazione moderata. | No |
| `endOnExit` | Specifica se una conversazione moderata termina quando il moderatore riaggancia. Questo è impostato su false per impostazione predefinita, il che significa che la conversazione termina solo quando l'ultimo partecipante rimasto riaggancia, indipendentemente dal fatto che il moderatore sia ancora in chiamata. Imposta `endOnExit` su true per terminare la conversazione quando il moderatore riaggancia. | No |
| `record` | Imposta su true per registrare questa conversazione. Per conversazioni standard, le registrazioni iniziano quando uno o più partecipanti si connettono alla conversazione. Per conversazioni moderate, le registrazioni iniziano quando il moderatore si unisce. Cioè, quando un NCCO viene eseguito per la conversazione nominata dove startOnEnter è impostato su true. Quando la registrazione è terminata, l'URL da cui scaricare la registrazione viene inviato all'URL dell'evento. Puoi sostituire l'URL dell'evento di registrazione predefinito e il metodo HTTP predefinito fornendo opzioni personalizzate `eventUrl` e `eventMethod` nella definizione dell'azione `conversation`. Per impostazione predefinita l'audio viene registrato in formato MP3. Vedi la guida [recording](https://developer.vonage.com/en/voice/voice-api/guides/recording#file-formats) per maggiori dettagli. | No |
| `canSpeak` | Un elenco di UUID di "gambe" che questo partecipante può far sentire. Se non fornito, il partecipante può essere sentito da tutti. Se viene fornito un elenco vuoto, il partecipante non sarà sentito da nessuno | No |
| `canHear` | Un elenco di UUID di "gambe" che questo partecipante può sentire. Se non fornito, il partecipante può sentire tutti. Se viene fornito un elenco vuoto, il partecipante non sentirà nessun altro partecipante | No |
| `mute` | Imposta su true per silenziare il partecipante. L'audio dal partecipante non verrà riprodotto nella conversazione e non verrà registrato. Quando si usa `canSpeak`, il parametro `mute` non è supportato. | No |
| `transcription` | Impostazioni di trascrizione. Se presentato (anche come oggetto vuoto), la trascrizione è attivata. Il parametro record deve essere impostato su true. Vedi [Impostazioni di Trascrizione](https://developer.vonage.com/en/voice/voice-api/ncco-reference#transcription-settings) sopra per maggiori dettagli. | No |

## Connect

Puoi usare l'azione `connect` per collegare una chiamata a endpoint come numeri di telefono o un'estensione VBC.

Questa azione è sincrona, dopo un *connect* viene processata l'azione successiva nello stack NCCO. Un'azione connect termina quando l'endpoint che stai chiamando è occupato o non disponibile. Puoi far squillare gli endpoint in sequenza annidando le azioni connect.

I seguenti esempi NCCO mostrano come configurare diversi tipi di connessioni.

### Endpoint telefono

```json
[
  {
    "action": "talk",
    "text": "Attendi mentre ti colleghiamo"
  },
  {
    "action": "connect",
    "eventUrl": ["https://example.com/events"],
    "timeout": "45",
    "from": "447700900000",
    "endpoint": [
      {
        "type": "phone",
        "number": "447700900001",
        "dtmfAnswer": "2p02p"
      }
    ]
  }
]
```

### Endpoint WebSocket

```json
[
  {
    "action": "talk",
    "text": "Attendi mentre ti colleghiamo"
  },
  {
    "action": "connect",
    "eventType": "synchronous",
    "eventUrl": [
      "https://example.com/events"
    ],
    "from": "447700900000",
    "endpoint": [
      {
        "type": "websocket",
        "uri": "ws://example.com/socket",
        "content-type": "audio/l16;rate=16000",
        "headers": {
            "name": "J Doe",
            "age": 40,
            "address": {
                "line_1": "Appartamento 14",
                "line_2": "123 Example Street",
                "city": "New York City"
            },
            "system_roles": [183493, 1038492, 22],
            "enable_auditing": false
        }
      }
    ]
  }
]
```

### Endpoint App

```json
[
  {
    "action": "talk",
    "text": "Attendi mentre ti colleghiamo"
  },
  {
    "action": "connect",
    "eventUrl": [
      "https://example.com/events"
    ],
    "from": "447700900000",
    "endpoint": [
      {
        "type": "app",
        "user": "jamie"
      }
    ]
  }
]
```

### Endpoint SIP

```json
[
  {
    "action": "talk",
    "text": "Attendi mentre ti colleghiamo"
  },
  {
    "action": "connect",
    "eventUrl": [
      "https://example.com/events"
    ],
    "from": "447700900000",
    "endpoint": [
      {
        "type": "sip",
        "uri": "sip:rebekka@sip.mcrussell.com",
        "headers": { "location": "New York City", "occupation": "developer" }
      }
    ]
  }
]
```

### NCCO di Fallback

```json
[
  {
    "action": "connect",
    "from": "447700900000",
    "timeout": 5,
    "eventType": "synchronous",
    "eventUrl": [
      "https://example.com/event-fallback"
    ],
    "endpoint": [
      {
        "type": "phone",
        "number": "447700900001"
      }
    ]
  }
]
```

### Chiamata proxy registrata

```json
[
  {
    "action": "record",
    "eventUrl": ["https://example.com/recordings"]
  },
  {
    "action": "connect",
    "eventUrl": ["https://example.com/events"],
    "from": "447700900000",
    "endpoint": [
      {
        "type": "phone",
        "number": "447700900001"
      }
    ]
  }
]
```

### Estensione VBC

```json
[
  {
    "action": "talk",
    "voiceName": "Russell",
    "text": "Grazie per la chiamata. Ti stiamo collegando all'estensione."
  },
  {
    "action": "connect",
    "endpoint": [
      {
        "type": "vbc",
        "extension": "111"
      }
    ]
  }
]
```

### Endpoint SIP personalizzato

```json
[
    {
      "action": "talk",
      "text": "Attendi mentre ti colleghiamo"
    },
    {
        "action": "connect",
        "eventUrl": [
          "https://example.com/events"
        ],
        "from": "447700900000",
        "endpoint": [
            {
              "type": "sip",
              "user": "john",
              "domain": "vonage-developer",
              "headers":
                {
                  "location": "New York City",
                  "occupation": "developer"
                }
            }
        ]
    }
]
```

Puoi utilizzare le seguenti opzioni per controllare un'azione `connect`:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `endpoint` | Array di oggetti `endpoint` a cui collegarsi. Attualmente supporta un massimo di un oggetto `endpoint`. [Tipi di endpoint disponibili](https://developer.vonage.com/en/voice/voice-api/ncco-reference#endpoint-types-and-values). | Sì |
| `from` | Un numero in formato [E.164](https://en.wikipedia.org/wiki/E.164) che identifica il chiamante. Questo deve essere uno dei tuoi numeri virtuali Vonage se ti stai collegando a un telefono reale, altrimenti la chiamata non si collegherà. | No |
| `randomFromNumber` | Imposta su `true` per usare un numero di telefono casuale come `from`. Il numero verrà selezionato dall'elenco dei numeri assegnati all'applicazione corrente. L'applicazione proverà a usare numeri dello stesso paese della destinazione (se disponibili). `randomFromNumber: true` non può essere usato insieme a `from`. Il valore predefinito è `false`. | No |
| `eventType` | Imposta su `synchronous` per: rendere l'azione connect sincrona, abilitare eventUrl per restituire un NCCO che sostituisce l'NCCO corrente quando una chiamata passa a stati specifici. | No |
| `timeout` | Se la chiamata non viene risposta, imposta il numero in secondi prima che Vonage smetta di far squillare `endpoint`. Deve essere un intero tra `1` e `120`. Il valore predefinito è `60`. | No |
| `limit` | Lunghezza massima della chiamata in secondi. Il valore predefinito e massimo è `7200` secondi (2 ore). | No |
| `machineDetection` | Configura il comportamento quando Vonage rileva che una destinazione è una segreteria telefonica. Imposta su uno dei seguenti: continue - Vonage invia una richiesta HTTP a event_url con l'evento di chiamata machine, hangup - termina la Chiamata | No |
| `advancedMachineDetection` | Configura il comportamento del rilevamento avanzato della macchina di Vonage. Sostituisce `machineDetection` se entrambi sono impostati. Vedi il [riferimento API](https://developer.vonage.com/en/api/voice#createCall) per i dettagli dei parametri. Questa funzionalità è a pagamento; le tariffe esatte si trovano nella pagina [Voice API Pricing](https://www.vonage.co.uk/communications-apis/voice/pricing/) sotto 'Programmable Features'. | No |
| `eventUrl` | Imposta l'endpoint webhook che Vonage chiama in modo asincrono su ciascuno dei possibili [Stati della Chiamata](https://developer.vonage.com/en/voice/voice-api/guides/call-flow#call-states). Se `eventType` è impostato su `synchronous`, l'`eventUrl` può restituire un NCCO che sostituisce l'NCCO corrente quando si verifica un timeout. | No |
| `eventMethod` | Il metodo HTTP utilizzato da Vonage per effettuare la richiesta a eventUrl. Il valore predefinito è `POST`. | No |
| `ringbackTone` | Un valore URL che punta a un `ringbackTone` da riprodurre in loop al chiamante, in modo che non senta il silenzio. Il `ringbackTone` smetterà automaticamente di suonare quando la chiamata sarà completamente connessa. Non è consigliato usare questo parametro quando ci si collega a un endpoint telefonico, poiché il vettore fornirà il proprio `ringbackTone`. Esempio: `"ringbackTone": "http://example.com/ringbackTone.wav"`. | No |

### Tipi e Valori degli Endpoint

#### Telefono (PSTN) - numeri di telefono in formato E.164

| Valore | Descrizione |
| --- | --- |
| `type` | Il tipo di endpoint: `phone` per un endpoint PSTN. |
| `number` | Il numero di telefono a cui collegarsi in formato [E.164](https://en.wikipedia.org/wiki/E.164). |
| `dtmfAnswer` | Imposta le cifre che vengono inviate all'utente non appena la Chiamata viene risposta. Le cifre `*` e `#` sono rispettate. Puoi creare pause usando `p`. Ogni pausa è di 500ms. |
| `onAnswer` | Un oggetto JSON contenente una chiave `url` obbligatoria. L'URL serve un NCCO da eseguire nel numero a cui ci si sta collegando, prima che quella chiamata venga unita alla tua conversazione esistente. Facoltativamente, può essere specificata la chiave `ringbackTone` con un valore URL che punta a un `ringbackTone` da riprodurre in loop al chiamante, in modo che non senta solo il silenzio. Il `ringbackTone` smetterà automaticamente di suonare quando la chiamata sarà completamente connessa. Esempio: `{"url":"https://example.com/answer", "ringbackTone":"http://example.com/ringbackTone.wav" }`. Si prega di notare, la chiave `ringback` è ancora supportata. |
| `shaken` | Per i clienti Vonage che sono tenuti dalla FCC a firmare le proprie chiamate verso gli USA, offriamo l'opzione di effettuare chiamate Voice API utilizzando la propria firma. Questa funzionalità è disponibile solo su richiesta. Le chiamate con una firma non valida verranno rifiutate. Contattaci per ulteriori informazioni. Quando si utilizza questa opzione, devi fornire il contenuto dell'Intestazione di Identità STIR/SHAKEN che Vonage deve utilizzare per questa chiamata. Il formato previsto consiste in: un JWT con intestazione, payload e firma, un parametro info con un link al certificato, un parametro alg (algoritmo) che indica il tipo di crittografia utilizzato, un parametro ppt (passport type) che dovrebbe essere shaken. Fai riferimento all'esempio fornito sotto la tabella. |

Esempio dell'opzione `shaken`:

```text
eyJhbGciOiJFUzI1NiIsInBwdCI6InNoYWtlbiIsInR5cCI6InBhc3Nwb3J0IiwieDV1IjoiaHR0cHM6Ly9jZXJ0LmV4YW1wbGUuY29tL3Bhc3Nwb3J0LnBlbSJ9.eyJhdHRlc3QiOiJBIiwiZGVzdCI6eyJ0biI6WyIxMjEyNTU1MTIxMiJdfSwiaWF0IjoxNjk0ODcwNDAwLCJvcmlnIjp7InRuIjoiMTQxNTU1NTEyMzQifSwib3JpZ2lkIjoiMTIzZTQ1NjctZTg5Yi0xMmQzLWE0NTYtNDI2NjE0MTc0MDAwIn0.MEUCIQCrfKeMtvn9I6zXjE2VfGEcdjC2sm5M6cPqBvFyV9XkpQIgLxlvLNmC8DJEKexXZqTZ;info=<https://stir-provider.example.net/cert.cer>;alg=ES256;ppt="shaken"
```

#### App - Collega la chiamata a un'applicazione compatibile con RTC

| Valore | Descrizione |
| --- | --- |
| `type` | Il tipo di endpoint: `app` per un'[applicazione](https://developer.vonage.com/en/client-sdk/setup/create-your-application). |
| `user` | Il nome utente dell'utente a cui collegarsi. Questo nome utente deve essere stato [aggiunto come utente](https://developer.vonage.com/en/api/conversation#createUser). |

#### WebSocket - Il WebSocket a cui collegarsi

| Valore | Descrizione |
| --- | --- |
| `type` | Il tipo di endpoint: `websocket` per un WebSocket. |
| `uri` | L'URI del websocket a cui stai effettuando lo streaming. |
| `content-type` | Il tipo di media internet per l'audio che stai trasmettendo in streaming. I valori possibili sono: `audio/l16;rate=16000` o `audio/l16;rate=8000`. |
| `headers` | Un oggetto JSON contenente qualsiasi metadato desideri. Vedi [connecting to a websocket](https://developer.vonage.com/en/voice/voice-api/guides/websockets#connecting-to-a-websocket) per esempi di intestazioni. |

#### SIP - L'endpoint SIP a cui collegarsi

| Valore | Descrizione |
| --- | --- |
| `type` | Il tipo di endpoint: `sip` per SIP. |
| `uri` | L'URI SIP dell'endpoint a cui ti stai collegando nel formato `sip:rebekka@sip.example.com`. Per usare [TLS e/o SRTP](https://developer.vonage.com/en/voice/sip/overview#protocols), includi rispettivamente `transport=tls` o `media=srtp` nell'URL con il punto e virgola `;` come delimitatore, per esempio: `sip:rebekka@sip.example.com;transport=tls;media=srtp`. Nota che questa proprietà è mutualmente esclusiva con `user` e `domain`. |
| `user` | Il componente `user` dell'URI. Sarà utilizzato insieme alla proprietà `domain` per creare l'URI SIP completo. Se imposti questa proprietà, devi anche impostare `domain` e lasciare `uri` non impostato. |
| `domain` | L'identificatore per un trunk creato utilizzando la dashboard. Questo deve essere un dominio provisionato con successo utilizzando la [dashboard SIP Trunking](https://dashboard.nexmo.com/sip-trunking) o la [Programmable SIP API](https://developer.vonage.com/en/api/psip#createDomain). Gli URI provisionati nel trunk verranno utilizzati insieme alla proprietà `user` per creare l'URI SIP completo. Quindi, ad esempio, se l'URI nel trunk è: `sip.example.com` e `user` è `example_user`, Vonage invierà la chiamata a `example_user@sip.example.com`. Se imposti questa proprietà, devi lasciare `uri` non impostato. Nota che questa proprietà si riferisce al nome di dominio, non all'URI del dominio. |
| `headers` | Coppie di stringhe `chiave` => `valore` contenenti qualsiasi metadato di cui hai bisogno es. `{ "location": "New York City", "occupation": "developer" }`. Le intestazioni vengono trasmesse come parte dell'INVITE SIP come intestazioni `X-chiave: valore`. Quindi nell'esempio, queste intestazioni vengono inviate: `X-location: New York City` e `X-occupation: developer`. |
| `standardHeaders` | Un oggetto JSON contenente una singola chiave `User-to-User`. Questo è usato per trasmettere informazioni da utente a utente se supportato dal fornitore, come da [RFC 7433](https://datatracker.ietf.org/doc/html/rfc7433). A differenza di `headers`, la chiave non verrà prefissata con `X-`, poiché è standardizzata. Per esempio: `{ "User-to-User": "342342ef34;encoding=hex" }`. Vonage non convaliderà il contenuto dell'intestazione User-to-User, se non assicurandosi che utilizzi caratteri validi e che il contenuto sia entro il numero massimo di caratteri consentito (256). |

> Per capire come la tua applicazione può ricevere e gestire le Intestazioni SIP Personalizzate, consulta la seguente pagina su Programmable SIP. Se vuoi sapere come la tua applicazione può inviare Intestazioni SIP, vai alla Guida di riferimento dei Webhook dell'API Voce.

#### VBC - L'estensione Vonage Business Cloud (VBC) a cui collegarsi

| Valore | Descrizione |
| --- | --- |
| `type` | Il tipo di endpoint: `vbc` per un'estensione VBC. |
| `extension` | l'estensione VBC a cui collegare la chiamata. |

## Talk

L'azione `talk` invia sintesi vocale a una Conversazione.

Il testo fornito nell'azione talk può essere semplice o formattato utilizzando [SSML](https://developer.vonage.com/en/voice/voice-api/concepts/customizing-tts). I tag SSML forniscono ulteriori istruzioni al sintetizzatore di testo in voce che ti consentono di impostare tono, pronuncia e combinare testo in più lingue. I tag SSML sono basati su XML e vengono inviati in linea nella stringa JSON.

Per impostazione predefinita, l'azione talk è sincrona. Tuttavia, se imposti *bargeIn* su *true* devi impostare un'azione *input* successivamente nello stack NCCO. I seguenti esempi NCCO mostrano come inviare un messaggio di sintesi vocale a una Conversazione o Chiamata:

### Sincrona

```json
[
  {
    "action": "talk",
    "text": "Stai ascoltando una Chiamata effettuata con Voice API"
  }
]
```

### Asincrona

```json
[
  {
    "action": "talk",
    "text": "Benvenuto in un I V R Voice API. ",
    "language": "en-GB",
    "bargeIn": false
  },
  {
    "action": "talk",
    "text": "Premi 1 forse e 2 per non sicuro seguito dal tasto cancelletto",
    "language": "en-GB",
    "bargeIn": true
  },
  {
    "action": "input",
    "submitOnHash": true,
    "eventUrl": ["https://example.com/ivr"]
  }
]
```

### Sincrona (con SSML)

```json
[
  {
    "action": "talk",
    "text": "<speak><prosody rate='fast'>Posso parlare veloce.</prosody></speak>"
  }
]
```

Puoi utilizzare le seguenti opzioni per controllare un'azione *talk*:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `text` | Una stringa fino a 1.500 caratteri (esclusi i tag SSML) contenente il messaggio da sintetizzare nella Chiamata o Conversazione. Una singola virgola in `text` aggiunge una breve pausa alla sintesi vocale. Per aggiungere una pausa più lunga è necessario usare un tag `break` in SSML. Per usare i tag [SSML](https://developer.vonage.com/en/voice/voice-api/concepts/customizing-tts), devi racchiudere il testo in un elemento `speak`. | Sì |
| `bargeIn` | Imposta su `true` in modo che questa azione termini quando l'utente interagisce con l'applicazione tramite input DTMF o vocale ASR. Usa questa funzione per consentire agli utenti di scegliere un'opzione senza dover ascoltare l'intero messaggio nel tuo [Interactive Voice Response (IVR)](https://developer.vonage.com/voice/voice-api/guides/interactive-voice-response). Se imposti `bargeIn` su `true`, la prossima azione non-talk nello stack NCCO deve essere un'azione `input`. Il valore predefinito è `false`. Una volta che `bargeIn` è impostato su `true` rimarrà `true` (anche se `bargeIn: false` è impostato in un'azione successiva) fino a quando non viene incontrata un'azione `input` | No |
| `loop` | Il numero di volte che `text` viene ripetuto prima che la Chiamata venga chiusa. Il valore predefinito è 1. Imposta su 0 per ripetere infinitamente. | No |
| `level` | Il livello di volume a cui viene riprodotto il discorso. Può essere qualsiasi valore tra `-1` e `1` con `0` come predefinito. | No |
| `language` | La lingua ([formato BCP-47](https://tools.ietf.org/html/bcp47)) per il messaggio che stai inviando. Predefinito: `en-US`. I valori possibili sono elencati nella guida [Text-To-Speech](https://developer.vonage.com/en/voice/voice-api/guides/text-to-speech#supported-languages). | No |
| `style` | Lo stile vocale (estensione vocale, tessitura e timbro). Predefinito: `0`. I valori possibili sono elencati nella guida [Text-To-Speech](https://developer.vonage.com/en/voice/voice-api/guides/text-to-speech#supported-languages). | No |
| `premium` | Imposta su `true` per usare la versione premium dello stile specificato se disponibile, altrimenti verrà usata la versione standard. Il valore predefinito è `false`. Puoi trovare maggiori informazioni sulle Voci Premium nella guida [Text-To-Speech](https://developer.vonage.com/en/voice/voice-api/guides/text-to-speech#premium-voices). | No |

### Parametri Restituiti da Talk

Vedi [Riferimento Webhook](https://developer.vonage.com/en/voice/voice-api/webhook-reference#talk--stream) per i parametri che vengono restituiti all'`eventUrl`.

## Stream

L'azione `stream` ti consente di inviare uno stream audio a una Conversazione

Per impostazione predefinita, l'azione stream è sincrona. Tuttavia, se imposti *bargeIn* su *true* devi impostare un'azione *input* successivamente nello stack NCCO.

Il seguente esempio NCCO mostra come inviare uno stream audio a una Conversazione o Chiamata:

### Sincrona

```json
[
  {
    "action": "stream",
    "streamUrl": ["https://acme.com/streams/music.mp3"]
  }
]
```

### Asincrona

```json
[
  {
    "action": "stream",
    "streamUrl": ["https://acme.com/streams/announcement.mp3"],
    "bargeIn": "true"
  },
  {
    "action": "input",
    "submitOnHash": "true",
    "eventUrl": ["https://example.com/ivr"]
  }
]
```

Puoi utilizzare le seguenti opzioni per controllare un'azione *stream*:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `streamUrl` | Un array contenente un singolo URL a un file audio mp3 o wav (16-bit) da riprodurre in streaming alla Chiamata o Conversazione. | Sì |
| `level` | Imposta il livello audio dello stream nell'intervallo -1 >=level<=1 con una precisione di 0.1. Il valore predefinito è 0. | No |
| `bargeIn` | Imposta su `true` in modo che questa azione termini quando l'utente interagisce con l'applicazione tramite input DTMF o vocale ASR. Usa questa funzione per consentire agli utenti di scegliere un'opzione senza dover ascoltare l'intero messaggio nel tuo [Interactive Voice Response (IVR)](https://developer.vonage.com/en/voice/guides/interactive-voice-response). Se imposti `bargeIn` su `true` su una o più azioni Stream, la prossima azione non-stream nello stack NCCO deve essere un'azione `input`. Il valore predefinito è `false`. Una volta che `bargeIn` è impostato su `true` rimarrà `true` (anche se `bargeIn: false` è impostato in un'azione successiva) fino a quando non viene incontrata un'azione `input`. | No |
| `loop` | Il numero di volte che `audio` viene ripetuto prima che la Chiamata venga chiusa. Il valore predefinito è `1`. Imposta su `0` per ripetere infinitamente. | No |

Lo stream audio a cui si fa riferimento dovrebbe essere un file in formato MP3 o WAV. Se hai problemi con la riproduzione del file, codificalo secondo le seguenti specifiche tecniche: [What kind of prerecorded audio files can I use?](https://api.support.vonage.com/hc/en-us/articles/115007447567)

> Se riproduci lo stesso file audio più volte, ad esempio utilizzando la stessa registrazione in molte chiamate, considera l'aggiunta di un'intestazione Cache-Control alla risposta URL con valori appropriati: Cache-Control: public, max-age=360000
Questo consente a Vonage di memorizzare nella cache il tuo file audio invece di scaricarlo ogni volta, il che può migliorare significativamente le prestazioni e l'esperienza utente. La memorizzazione nella cache è supportata sia per URL HTTP che HTTPS.

### Parametri Restituiti da Stream

Vedi [Riferimento Webhook](https://developer.vonage.com/en/voice/voice-api/webhook-reference#talk--stream) per i parametri che vengono restituiti all'`eventUrl`.

## Input

Puoi usare l'azione `input` per raccogliere cifre o input vocale dalla persona che stai chiamando. Questa azione è sincrona, Vonage elabora l'input e lo inoltra nei [parametri](https://developer.vonage.com/en/voice/voice-api/ncco-reference#input-return-parameters) inviati all'endpoint webhook `eventUrl` che configuri nella tua richiesta. Il tuo endpoint webhook dovrebbe restituire un altro NCCO che sostituisce l'NCCO esistente e controlla la Chiamata in base all'input dell'utente. Potresti usare questa funzionalità per creare una Risposta Vocale Interattiva (IVR). Ad esempio, se il tuo utente preme *4* o dice "Vendite", restituisci un NCCO [connect](https://developer.vonage.com/en/voice/voice-api/ncco-reference#connect) che inoltra la chiamata al tuo reparto vendite.

Il seguente esempio NCCO mostra come configurare un endpoint IVR:

```json
[
  {
    "action": "talk",
    "text": "Inserisci una cifra o di qualcosa"
  },
  {
    "action": "input",
    "eventUrl": [
      "https://example.com/ivr"
    ],
    "type": [ "dtmf", "speech" ],
    "dtmf": {
      "maxDigits": 1
    },
    "speech": {
      "context": [ "sales", "support" ]
    }
  }
]
```

Il seguente esempio NCCO mostra come usare `bargeIn` per consentire a un utente di interrompere un'azione `talk`. Nota che un'azione `input` **deve** seguire qualsiasi azione che abbia una proprietà `bargeIn` (es. `talk` o `stream`).

```json
[
  {
    "action": "talk",
    "text": "Inserisci una cifra o di qualcosa",
    "bargeIn": true
  },
  {
    "action": "input",
    "eventUrl": [
      "https://example.com/ivr"
    ],
    "type": [ "dtmf", "speech" ],
    "dtmf": {
      "maxDigits": 1
    },
    "speech": {
      "context": [ "sales", "support" ]
    }
  }
]
```

Le seguenti opzioni possono essere utilizzate per controllare un'azione `input`:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `type` | Tipo di input accettabile, può essere impostato come `[ "dtmf" ]` per solo input DTMF, `[ "speech" ]` per solo ASR, o `[ "dtmf", "speech" ]` per entrambi. | Sì |
| `dtmf` | [Impostazioni DTMF](https://developer.vonage.com/en/voice/voice-api/ncco-reference#dtmf-input-settings). | No |
| `speech` | [Impostazioni di riconoscimento vocale](https://developer.vonage.com/en/voice/voice-api/ncco-reference#speech-recognition-settings). | No |
| `mode` | Modalità di elaborazione dell'input, attualmente applicabile solo a DTMF. Valori validi sono `synchronous` (predefinito) e `asynchronous`. Se impostato su `asynchronous`, tutte le [impostazioni DTMF](https://developer.vonage.com/en/voice/voice-api/ncco-reference#dtmf-input-settings) devono essere lasciate vuote. In modalità asincrona, le cifre vengono inviate una alla volta al webhook dell'evento in tempo reale. Nella modalità `synchronous` predefinita, questo è controllato invece dalle impostazioni DTMF e gli input vengono inviati in batch. | No |
| `eventUrl` | Vonage invia le cifre premute dal chiamato a questo URL 1) dopo una pausa di `timeOut` nell'attività o quando viene premuto # per DTMF o 2) dopo che l'utente smette di parlare o dopo `30` secondi di discorso per l'input vocale. | No |
| `eventMethod` | Il metodo HTTP utilizzato per inviare informazioni sull'evento a `event_url`. Il valore predefinito è `POST`. | No |

#### Impostazioni Input DTMF

*Nota:* Queste impostazioni non si applicano se la `mode` è impostata su `asynchronous`.

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `timeOut` | Il risultato dell'attività del chiamato viene inviato all'endpoint webhook `eventUrl` `timeOut` secondi dopo l'ultima azione. Il valore predefinito è 3. Il massimo è 10. | No |
| `maxDigits` | Il numero di cifre che l'utente può premere. Il valore massimo è `20`, il predefinito è `4` cifre. | No |
| `submitOnHash` | Imposta su `true` in modo che l'attività del chiamato venga inviata al tuo endpoint webhook a `eventUrl` dopo che preme #. Se # non viene premuto, il risultato viene inviato dopo `timeOut` secondi. Il valore predefinito è `false`. Cioè, il risultato viene inviato al tuo endpoint webhook dopo `timeOut` secondi. | No |

#### Impostazioni di Riconoscimento Vocale

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `uuid` | L'ID univoco della "gamba" (leg) della chiamata per l'utente di cui catturare il parlato, definito come array con un singolo elemento. La prima "gamba" unitasi alla chiamata per impostazione predefinita. | No |
| `endOnSilence` | Controlla per quanto tempo il sistema attenderà dopo che l'utente smette di parlare per decidere che l'input è completato. Il valore predefinito è `2.0` (secondi). L'intervallo di valori possibili è tra `0.4` secondi e `10.0` secondi. | No |
| `language` | Lingua attesa del parlato dell'utente. Formato: BCP-47. Predefinito: `en-US`. [Elenco delle lingue supportate](https://developer.vonage.com/en/voice/voice-api/guides/asr#language). | No |
| `context` | Array di suggerimenti (stringhe) per migliorare la qualità del riconoscimento se sono attese determinate parole dall'utente. | No |
| `startTimeout` | Controlla per quanto tempo il sistema attenderà che l'utente inizi a parlare. L'intervallo di valori possibili è tra 1 secondo e 60 secondi. Il valore predefinito è `10`. | No |
| `maxDuration` | Controlla la durata massima del parlato (dal momento in cui l'utente inizia a parlare). Il valore predefinito è 60 (secondi). L'intervallo di valori possibili è tra 1 e 60 secondi. | No |
| `saveAudio` | Imposta su `true` in modo che la registrazione dell'input vocale (`recording_url`) venga inviata al tuo endpoint webhook a `eventUrl`. Il valore predefinito è `false`. | No |
| `sensitivity` | Sensibilità audio utilizzata per differenziare il rumore dal parlato. Un valore intero dove 10 rappresenta bassa sensibilità e 100 massima sensibilità. Il valore predefinito è 90. | No |

Il seguente esempio mostra i parametri inviati al webhook `eventUrl` per input DTMF:

```json
{
  "speech": { "results": [ ] },
  "dtmf": {
    "digits": "1234",
    "timed_out": true
  },
  "from": "15551234567",
  "to": "15557654321",
  "uuid": "aaaaaaaa-bbbb-cccc-dddd-0123456789ab",
  "conversation_uuid": "bbbbbbbb-cccc-dddd-eeee-0123456789ab",
  "timestamp": "2020-01-01T14:00:00.000Z"
}
```

Il seguente esempio mostra i parametri restituiti al webhook `eventUrl` per input vocale:

```json
{
  "speech": {
    "recording_url": "https://api-us.nexmo.com/v1/files/eeeeeee-ffff-0123-4567-0123456789ab",
    "timeout_reason": "end_on_silence_timeout",
    "results": [
      {
        "confidence": "0.9405097",
        "text": "sales"
      },
      {
        "confidence": "0.70543784",
        "text": "sails"
      },
      {
        "confidence": "0.5949854",
        "text": "sale"
      }
    ]
  },
  "dtmf": {
    "digits": null,
    "timed_out": false
  },
  "from": "15551234567",
  "to": "15557654321",
  "uuid": "aaaaaaaa-bbbb-cccc-dddd-0123456789ab",
  "conversation_uuid": "bbbbbbbb-cccc-dddd-eeee-0123456789ab",
  "timestamp": "2020-01-01T14:00:00.000Z"
}
```

### Parametri Restituiti da Input

Vedi [Riferimento Webhook](https://developer.vonage.com/en/voice/voice-api/webhook-reference#input) per i parametri di input che vengono restituiti all'`eventUrl`.

## Notify

Usa l'azione `notify` per inviare un payload personalizzato al tuo URL di evento. Il tuo endpoint webhook può restituire un altro NCCO che sostituisce l'NCCO esistente o restituire un payload vuoto, il che significa che l'NCCO esistente continuerà a essere eseguito.

```json
[
  {
    "action": "notify",
    "payload": {
      "foo": "bar"
    },
    "eventUrl": [
      "https://example.com/webhooks/event"
    ],
    "eventMethod": "POST"
  }
]
```

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `payload` | Il corpo JSON da inviare al tuo URL di evento. | Sì |
| `eventUrl` | L'URL a cui inviare gli eventi. Se restituisci un NCCO quando ricevi una notifica, sostituirà l'NCCO corrente. | Sì |
| `eventMethod` | Il metodo HTTP da utilizzare quando si invia `payload` al tuo `eventUrl`. | No |

### Impostazioni Vocali dei Prompt

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `language` | La lingua ([formato BCP-47](https://tools.ietf.org/html/bcp47)) per i prompt. Predefinito: `en-US`. I valori possibili sono elencati nella guida [Text-To-Speech](https://developer.vonage.com/en/voice/voice-api/guides/text-to-speech#supported-languages). | No |
| `style` | Lo stile vocale (estensione vocale, tessitura e timbro). Predefinito: `0`. I valori possibili sono elencati nella guida [Text-To-Speech](https://developer.vonage.com/en/voice/voice-api/guides/text-to-speech#supported-languages). | No |

## Wait

Puoi usare l'azione `wait` per aggiungere un periodo di attesa e mettere in pausa l'esecuzione dell'NCCO in esecuzione per un numero specificato di secondi.

L'azione è sincrona. Il periodo di attesa inizia quando l'azione wait viene eseguita nell'NCCO e termina dopo il timeout fornito o predefinito. A questo punto, l'esecuzione dell'NCCO riprende. Il parametro `timeout` è un float. I valori validi vanno da 0.1 secondi a 7200 secondi. I valori inferiori a 0.1 predefiniti a 0.1 secondi, e i valori superiori a 7200 predefiniti a 7200 secondi. Se non specificato, predefinito a 10 secondi.

> Nota: se hai bisogno di un callback che informi che l'azione wait è terminata, aggiungi un'azione notify dopo l'azione wait.

Il seguente esempio NCCO mostra come eseguire l'azione wait:

```json
[
  {
    "action": "talk",
    "text": "Benvenuto a una conferenza moderata Vonage"
  },
  {
    "action": "wait",
    "timeout": 0.5
  },
  {
    "action": "talk",
    "text": "Ti collegheremo quando un agente sarà disponibile"
  }
]
```

Puoi utilizzare le seguenti opzioni per controllare un'azione `wait`:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `timeout` | Controlla la durata del periodo di attesa prima di eseguire l'azione successiva nell'NCCO. Questo parametro è un float. I valori validi vanno da 0.1 secondi a 7200 secondi. I valori inferiori a 0.1 predefiniti a 0.1 secondi, e i valori superiori a 7200 predefiniti a 7200 secondi. Il valore predefinito è `10`. | No |

## Transfer

L'azione `transfer` è sincrona. Puoi usarla per spostare le "gambe" della chiamata da una conversazione corrente a un'altra conversazione esistente. L'azione `transfer` è terminale per la conversazione corrente, e l'NCCO della conversazione target continua a controllare il comportamento della conversazione target. Tutte le "gambe" della conversazione corrente vengono trasferite nella conversazione target, rispettando le impostazioni audio (`canHear`, `canSpeak`, `mute`) se fornite.

Il seguente esempio NCCO mostra come eseguire l'azione transfer:

```json
[
   ...
   {
     "action": "transfer",
     "conversationId": "CON-f972836a-550f-45fa-956c-12a2ab5b7d22",
     "canHear": [ "9c132730-8c22-4760-a4dc-40502f05b444" ]
   }
   ...
]
```

Puoi utilizzare le seguenti opzioni per controllare un'azione transfer:

| Opzione | Descrizione | Obbligatoria |
| --- | --- | --- |
| `conversation_id` | ID della conversazione target, definito come stringa. | Sì |
| `canHear` | Un elenco di UUID di "gambe" che questo partecipante può sentire, definito come un array di stringhe. Se non fornito, il partecipante può sentire tutti. Se viene fornito un elenco vuoto, il partecipante non sentirà nessun altro partecipante. | No |
| `canSpeak` | Un elenco di UUID di "gambe" che questo partecipante può far sentire, definito come un array di stringhe. Se non fornito, il partecipante può essere sentito da tutti. Se viene fornito un elenco vuoto, il partecipante non sarà sentito da nessuno. | No |
| `mute` | Imposta su true per silenziare il partecipante. L'audio dal partecipante non verrà riprodotto nella conversazione e non verrà registrato. Quando si usa `canSpeak`, il parametro `mute` non è supportato. | No |