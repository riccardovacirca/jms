# Configure Logging Level

When debugging your application, you can set the logging level for the Client SDK which will change how much information the Client SDK prints out to the console. You must set your log level before the client is initialized. You can specify your log level in the client initializer:

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
// Available options are Verbose, Debug, Info, Warn, and Error
```

### Swift

```swift
let initConfig = VGClientInitConfig(loggingLevel: .verbose)
let client = VGVonageClient(initConfig)
// Available options are Verbose, Debug, Info, Warn, and Error
```