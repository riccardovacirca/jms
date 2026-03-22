# Transitioning to the Vonage Client SDK

If you are familiar with or using the Nexmo Client SDK in your application there are some changes to consider when moving to the Vonage Client SDK.

## Client Instantiation

For Android and iOS the SDK Client is no longer a singleton.

### JavaScript

```javascript
// Nexmo SDK
let client = new NexmoClient()

// Vonage SDK

// If loaded with a <script> tag:
const client = new vonageClientSDK.VonageClient();

// If loaded via import:
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

## Session Management

Previously, in the Nexmo Client SDK on Android and iOS you would call `login`/`createSession` and receive updates about creating your initial session via a listener.

In the JavaScript Vonage Client SDK you will no longer receive an app object, you will instead get a session ID.

The Vonage Client SDK has a callback to allow you to know if the initial session creation was successful or not. Further session updates, including reconnecting, are available on listeners/delegate methods.

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
        null ->  // Session created 🎉
        else ->  // Handle error
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
        // Handle error
    } else {
        // Session created 🎉
    }
}

class ExampleClientDelegate: NSObject, VGClientDelegate {
    func client(_ client: VGBaseClient, didReceiveSessionErrorWith reason: VGSessionErrorReason) {}

    func clientWillReconnect(_ client: VGBaseClient) {}

    func clientDidReconnect(_ client: VGBaseClient) {}
}
```

## Calls

In the Vonage Client SDK, server calls are the only type of calls you are able to make, the previous `inApp` call type has been deprecated. This means an NCCO webhook server is now mandatory for all call flows.

The parameters for making a server call have changed, the previous to and context field are now one parameter. For backwards compatibility with existing NCCO webhooks, you can specify `to` as apart of the context object, which will be forwarded to your webhook as before.

Otherwise, utilise the context param to send custom data to your `answer_url` webhook.

### JavaScript

```javascript
// Nexmo SDK
application.callServer("PHONE_NUMBER")
    .then(nxmCall => {
        ...
    })
    .catch(error => {
        ...
    });

// Vonage SDK
client.serverCall({ to: "PHONE_NUMBER" })
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
client.serverCall("PHONE_NUMBER", null, object : NexmoRequestListener<NexmoCall> {
    override fun onSuccess(call: NexmoCall?) {
        // Handle call
    }

    override fun onError(apiError: NexmoApiError) {
        // Handle error
    }
})

// VonageSDK
client.serverCall(mapOf("to" to "PHONE_NUMBER")) { err, voiceCall ->
    when(err) {
        null -> {
            // Handle call
        }
        else -> // Handle error
    }
}
```

### Swift

```swift
// Nexmo SDK
client.serverCall(withCallee: "PHONE_NUMBER", customData: nil) { (error, call) in
    // Handle error/call
}

// Vonage SDK
client.serverCall(["to":"PHONE_NUMBER"]) { (error, call) in
    // Handle error/call
}
```

## Call/Chat Actions

The Vonage Client SDK also no longer has methods on objects for performing action such and ending a call, muting a call, or sending a message. All these actions are available as method on the client. To perform these actions, you will need to store the call or conversation ID you would like to perform an action on.

For example to mute a call:

### JavaScript

```javascript
// Nexmo SDK
conversation.me.mute(true);

// Vonage SDK
client.mute(callId)
    .then(() => {
        // Mute successful
    })
    .catch(error => {
        // Handle muting error
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
            // Handle muting error
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
        // Handle muting error
    }
}
```

Or to sent a message:

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