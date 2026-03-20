# Configure your Data Center

You may need to configure the Client SDK to connect to your nearest data center. You can use this guide to help determine your best configuration.

> NOTE: This is an advanced optional step. You only need to do this if you determine your network performance needs to be enhanced. For most users this configuration is not required. This step can be done after adding the SDK to your application.

## Why Configure your Data Centers?

You only need to do this if you believe your application performance could be improved by connecting to a more local data center. By default the Client SDK connects to a US data center.

## Configuration with Regions

The Client SDK Config has an initializer which takes a region enum (`US`, `EU`, `AP`):

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
val config = VGClientConfig(ClientConfigRegion.US) // Other options are .AP & .EU

val client = VonageClient(this.application.applicationContext)
client.setConfig(config)
```

### Swift

```swift
let config = VGClientConfig(region: .US) // Other options are .AP & .EU

let client = VGVonageClient()
client.setConfig(config)
```

> NOTE: Users are specific to regions, if change the Client SDK's data center you need to create users in the same data center. The data center URLs as detailed below.

## URLs

If you want more control than setting the region, it is possible to configure the following URLs:

1.  `apiUrl`: the Conversation API URL.
2.  `websocketUrl`: the websocket URL.

### apiUrl

This is the Conversation API URL. This is the URL used when the Client SDK calls the API.

The default value is `https://api-us-3.vonage.com`.

| Data Center | URL |
| --- | --- |
| `Virginia` | `https://api-us-3.vonage.com` |
| `Oregon` | `https://api-us-4.vonage.com` |
| `Dublin` | `https://api-eu-3.vonage.com` |
| `Frankfurt` | `https://api-eu-4.vonage.com` |
| `Singapore` | `https://api-ap-3.vonage.com` |
| `Sydney` | `https://api-ap-4.vonage.com` |

### websocketUrl

This is the websocket URL: the URL that receives realtime events.

The default value is `wss://ws-us-3.vonage.com`.

| Data Center | URL |
| --- | --- |
| `Virginia` | `wss://ws-us-3.vonage.com` |
| `Oregon` | `wss://ws-us-4.vonage.com` |
| `Dublin` | `wss://ws-eu-3.vonage.com` |
| `Frankfurt` | `wss://ws-eu-4.vonage.com` |
| `Singapore` | `wss://ws-ap-3.vonage.com` |
| `Sydney` | `wss://ws-ap-4.vonage.com` |

### Configuration with URLs

You can specify your preferred URLs when you create the Client SDK `ClientConfig` object:

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

## See also

*   [Add the SDK to your application](https://developer.vonage.com/en/client-sdk/setup/add-sdk-to-your-app/android)