# Transizione al Vonage Client SDK

Se hai familiarità con o utilizzi il Nexmo Client SDK nella tua applicazione, ci sono alcune modifiche da considerare quando passi al Vonage Client SDK.

## Istanziazione del Client

Per Android e iOS, il Client SDK non è più un singleton.

### JavaScript

```javascript
// Nexmo SDK
let client = new NexmoClient()

// Vonage SDK

// Se caricato con un tag <script>:
const client = new vonageClientSDK.VonageClient();

// Se caricato via import:
const client = new VonageClient();
```

### Kotlin

```kotlin
// Nexmo SDK
client = NexmoClient.Builder().build(this)

// Vonage SDK
client = VoiceClient(this)
```

### Swift

```swift
// Nexmo SDK
let client = NXMClient.shared

// Vonage SDK
let client = VGVoiceClient()
```

## Gestione della Sessione

Precedentemente, nel Nexmo Client SDK su Android e iOS, chiamavi `login`/`createSession` e ricevevi aggiornamenti sulla creazione della tua sessione iniziale tramite un listener.

Nel Vonage Client SDK per JavaScript, non riceverai più un oggetto app, ma invece otterrai un ID sessione.

Il Vonage Client SDK ha una callback per permetterti di sapere se la creazione iniziale della sessione è avvenuta con successo o meno. Ulteriori aggiornamenti della sessione, inclusa la riconnessione, sono disponibili sui listener/metodi delegati.

### JavaScript

```javascript
// Nexmo SDK
client.createSession(token)
  .then(app => {
    ...
  })
  .catch(error => {
    ...
  });

// Vonage SDK
client.createSession(token)
  .then(sessionId => {
    ...
  })
  .catch(error => {
    ...
  });
```

### Kotlin

```kotlin
// Nexmo SDK
client.login(token)

client.setConnectionListener { connectionStatus, _ ->
    when (connectionStatus) {
        ConnectionStatus.CONNECTED ->
        ConnectionStatus.DISCONNECTED ->
        ConnectionStatus.CONNECTING ->
        ConnectionStatus.UNKNOWN ->
    }
}


// Vonage SDK
client.createSession(token) { err, sessionId ->
    when(err) {
        null ->  // Sessione creata 🎉
        else ->  // Gestisci errore
    }
}

client.setSessionErrorListener { err -> }

client.setReconnectingListener {}

client.setReconnectionListener {}
```

### Swift

```swift
// NexmoSDK
client.login(withAuthToken: token)

class ExampleClientDelegate: NSObject, NXMClientDelegate {
    func client(_ client: NXMClient, didChange status: NXMConnectionStatus, reason: NXMConnectionStatusReason) {
        switch status {
        case .connected:
        case .disconnected:
        case .connecting:
    }
}

// Vonage SDK
client.createSession(token) { error, sessionId in
    if (error != nil) {
        // Gestisci errore
    } else {
        // Sessione creata 🎉
    }
}

class ExampleClientDelegate: NSObject, VGClientDelegate {
    func client(_ client: VGBaseClient, didReceiveSessionErrorWith reason: VGSessionErrorReason) {}

    func clientWillReconnect(_ client: VGBaseClient) {}

    func clientDidReconnect(_ client: VGBaseClient) {}
}
```

## Chiamate

Nel Vonage Client SDK, le chiamate server sono l'unico tipo di chiamate che puoi effettuare; il precedente tipo di chiamata `inApp` è stato deprecato. Ciò significa che un server webhook NCCO è ora obbligatorio per tutti i flussi di chiamata.

I parametri per effettuare una chiamata server sono cambiati; il precedente campo `to` e `context` sono ora un unico parametro. Per compatibilità con i webhook NCCO esistenti, puoi specificare `to` come parte dell'oggetto context, che verrà inoltrato al tuo webhook come prima.

Altrimenti, utilizza il parametro context per inviare dati personalizzati al tuo webhook `answer_url`.

### JavaScript

```javascript
// Nexmo SDK
application.callServer("NUMERO_DI_TELEFONO")
    .then(nxmCall => {
        ...
    })
    .catch(error => {
        ...
    });

// Vonage SDK
client.serverCall({ to: "NUMERO_DI_TELEFONO" })
    .then(callId => {
        ...
    })
    .catch(error => {
        ...
    });
```

### Kotlin

```kotlin
// NexmoSDK
client.serverCall("NUMERO_DI_TELEFONO", null, object : NexmoRequestListener<NexmoCall> {
    override fun onSuccess(call: NexmoCall?) {
        // Gestisci chiamata
    }

    override fun onError(apiError: NexmoApiError) {
        // Gestisci errore
    }
})

// VonageSDK
client.serverCall(mapOf("to" to "NUMERO_DI_TELEFONO")) { err, voiceCall ->
    when(err) {
        null -> {
            // Gestisci chiamata
        }
        else -> // Gestisci errore
    }
}
```

### Swift

```swift
// Nexmo SDK
client.serverCall(withCallee: "NUMERO_DI_TELEFONO", customData: nil) { (error, call) in
    // Gestisci errore/chiamata
}

// Vonage SDK
client.serverCall(["to":"NUMERO_DI_TELEFONO"]) { (error, call) in
    // Gestisci errore/chiamata
}
```

## Azioni di Chiamata/Chat

Il Vonage Client SDK inoltre non ha più metodi sugli oggetti per eseguire azioni come terminare una chiamata, mettere in muto una chiamata o inviare un messaggio. Tutte queste azioni sono disponibili come metodi sul client. Per eseguire queste azioni, dovrai memorizzare l'ID della chiamata o della conversazione su cui desideri eseguire un'azione.

Ad esempio, per mettere in muto una chiamata:

### JavaScript

```javascript
// Nexmo SDK
conversation.me.mute(true);

// Vonage SDK
client.mute(callId)
    .then(() => {
        // Muto applicato con successo
    })
    .catch(error => {
        // Gestisci errore del muto
    });
```

### Kotlin

```kotlin
// Nexmo SDK
callMember?.enableMute(muteListener)

// Vonage SDK
client.mute(callId) { 
    error ->
    when {
        error != null -> {
            // Gestisci errore del muto
        }
    }
}
```

### Swift

```swift
// Nexmo SDK
callMember.enableMute()

// Vonage SDK
client.mute(callId) { error in
    if error != nil {
        // Gestisci errore del muto
    }
}
```

Oppure per inviare un messaggio:

### JavaScript

```javascript
// Nexmo SDK
conversation.sendMessage({
    "message_type": "text",
    "text": "Hello world!"
}).then((event) => {
    ...
}).catch((error)=>{
    ...
});


// Vonage SDK
client.sendMessageTextEvent("CONV_ID", "Hello world!")
    .then(timestamp => {
        ...
    }).catch(error => {
        ...
    });
```

### Kotlin

```kotlin
// Nexmo SDK
conversation.sendMessage("Hello world!", object : NexmoRequestListener<Void> {
   ...
})

// Vonage SDK
client.sendMessageTextEvent("CONV_ID", "Hello world!") { error, timestamp ->
    ...
}
```

### Swift

```swift
// Nexmo SDK
conversation.sendMessage("Hello world!") { error in
   ...
}

// Vonage SDK
client.sendMessageTextEvent("CONV_ID", text: "Hello world!") { error, timestamp in
    ...
}
```
