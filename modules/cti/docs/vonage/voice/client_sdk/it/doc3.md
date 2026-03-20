# Configura il tuo Data Center

Potrebbe essere necessario configurare il Client SDK per connettersi al data center più vicino a te. Puoi utilizzare questa guida per aiutarti a determinare la configurazione migliore.

> NOTA: Questo è un passaggio avanzato opzionale. È necessario eseguirlo solo se si determina che le prestazioni della rete devono essere migliorate. Per la maggior parte degli utenti questa configurazione non è richiesta. Questo passaggio può essere eseguito dopo aver aggiunto l'SDK alla tua applicazione.

## Perché Configurare i tuoi Data Center?

È necessario farlo solo se si ritiene che le prestazioni dell'applicazione possano essere migliorate connettendosi a un data center più locale. Per impostazione predefinita, il Client SDK si connette a un data center negli Stati Uniti.

## Configurazione con Regioni

Il Config del Client SDK ha un inizializzatore che accetta un'enumerazione della regione (`US`, `EU`, `AP`):

### JavaScript

```javascript
const client = new vonageClientSDK.VonageClient({
  region: "EU"
});
```

```javascript
import { VonageClient } from "@vonage/client-sdk";

const client = new VonageClient({
  region: "EU"
});
```

```javascript
client.setConfig({
  region: "AP"
});
```

### Kotlin

```kotlin
val config = VGClientConfig(ClientConfigRegion.US) // Altre opzioni sono .AP & .EU

val client = VonageClient(this.application.applicationContext)
client.setConfig(config)
```

### Swift

```swift
let config = VGClientConfig(region: .US) // Altre opzioni sono .AP & .EU

let client = VGVonageClient()
client.setConfig(config)
```

> NOTA: Gli utenti sono specifici per regione; se cambi il data center del Client SDK, devi creare gli utenti nello stesso data center. Gli URL dei data center sono dettagliati di seguito.

## URL

Se desideri un controllo maggiore rispetto all'impostazione della regione, è possibile configurare i seguenti URL:

1.  `apiUrl`: l'URL dell'API Conversazione.
2.  `websocketUrl`: l'URL del websocket.

### apiUrl

Questo è l'URL dell'API Conversazione. Questo è l'URL utilizzato quando il Client SDK chiama l'API.

Il valore predefinito è `https://api-us-3.vonage.com`.

| Data Center | URL |
| --- | --- |
| `Virginia` | `https://api-us-3.vonage.com` |
| `Oregon` | `https://api-us-4.vonage.com` |
| `Dublin` | `https://api-eu-3.vonage.com` |
| `Frankfurt` | `https://api-eu-4.vonage.com` |
| `Singapore` | `https://api-ap-3.vonage.com` |
| `Sydney` | `https://api-ap-4.vonage.com` |

### websocketUrl

Questo è l'URL del websocket: l'URL che riceve eventi in tempo reale.

Il valore predefinito è `wss://ws-us-3.vonage.com`.

| Data Center | URL |
| --- | --- |
| `Virginia` | `wss://ws-us-3.vonage.com` |
| `Oregon` | `wss://ws-us-4.vonage.com` |
| `Dublin` | `wss://ws-eu-3.vonage.com` |
| `Frankfurt` | `wss://ws-eu-4.vonage.com` |
| `Singapore` | `wss://ws-ap-3.vonage.com` |
| `Sydney` | `wss://ws-ap-4.vonage.com` |

### Configurazione con URL

Puoi specificare i tuoi URL preferiti quando crei l'oggetto `ClientConfig` del Client SDK:

### JavaScript

```javascript
const client = new vonageClientSDK.VonageClient({
  apiUrl: "https://api-us-3.vonage.com",
  websocketUrl: "wss://ws-us-3.vonage.com"
});
```

```javascript
import { VonageClient } from "@vonage/client-sdk";

const client = new VonageClient({
  apiUrl: "https://api-us-3.vonage.com",
  websocketUrl: "wss://ws-us-3.vonage.com"
});
```

```javascript
client.setConfig({
  apiUrl: "https://api-ap-3.vonage.com",
  websocketUrl: "wss://ws-ap-3.vonage.com"
});
```

### Kotlin

```kotlin
val config = VGClientConfig()
config.apiUrl = "https://api-us-3.vonage.com"
config.websocketUrl = "wss://ws-us-3.vonage.com"

val client = VonageClient(this.application.applicationContext)
client.setConfig(config)
```

### Swift

```swift
let config = VGClientConfig()
config.apiUrl = "https://api-us-3.vonage.com"
config.websocketUrl = "wss://ws-us-3.vonage.com"

let client = VGVonageClient()
client.setConfig(config)
```

## Vedi anche

*   [Aggiungi l'SDK alla tua applicazione](https://developer.vonage.com/en/client-sdk/setup/add-sdk-to-your-app/android)