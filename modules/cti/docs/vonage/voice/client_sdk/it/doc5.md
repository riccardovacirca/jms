# Configura il Livello di Log

Quando stai eseguendo il debug della tua applicazione, puoi impostare il livello di log per il Client SDK, il quale modificherà la quantità di informazioni che il Client SDK stampa sulla console. È necessario impostare il livello di log prima che il client venga inizializzato. Puoi specificare il tuo livello di log nell'inizializzatore del client:

### JavaScript

```javascript
const client = new vonageClientSDK.VonageClient({
    loggingLevel: 'Verbose'
});
```

```javascript
import { VonageClient } from '@vonage/client-sdk';

const client = new VonageClient({
    loggingLevel: 'Verbose'
});
```

### Kotlin

```kotlin
val initConfig = VGClientInitConfig(LoggingLevel.Verbose)
val client = VoiceClient(this.application.applicationContext, initConfig)
// Le opzioni disponibili sono Verbose, Debug, Info, Warn ed Error
```

### Swift

```swift
let initConfig = VGClientInitConfig(loggingLevel: .verbose)
let client = VGVonageClient(initConfig)
// Le opzioni disponibili sono Verbose, Debug, Info, Warn ed Error
```