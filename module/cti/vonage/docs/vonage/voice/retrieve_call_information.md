# Retrieve information for a call

A code snippet that shows how to retrieve information for a call. The call to retrieve information for is identified via a UUID.

## Example

Replace the following variables in the example code:

| Key | Description |
| --- | --- |
| `VOICE_CALL_ID` | The UUID of the call leg. |

### cURL

#### Generate your JWT

Execute the following command at your terminal prompt to create the [JWT](https://developer.vonage.com/en/concepts/guides/authentication#json-web-tokens-jwt) for authentication:

```bash
export JWT=$(nexmo jwt:generate $PATH_TO_PRIVATE_KEY application_id=$NEXMO_APPLICATION_ID)
```

```sh
curl "https://api.nexmo.com/v1/calls/"$VOICE_CALL_ID \
  -H "Authorization: Bearer $JWT" \
```

```bash
$ bash retrieve-info-for-a-call.sh
```

### Node.js

#### Install dependencies

```bash
npm install @vonage/server-sdk
```

#### Initialize your dependencies

Create a file named `retrieve-info-for-a-call.js` and add the following code:

[View full source](https://github.com/Vonage/vonage-node-code-snippets/blob/3164e3fd94822aec9ae926c1771d58636e01c4a7/voice/retrieve-info-for-a-call.js#L7-L12)

```javascript
const { Vonage } = require('@vonage/server-sdk');

const vonage = new Vonage({
  applicationId: VONAGE_APPLICATION_ID,
  privateKey: VONAGE_PRIVATE_KEY,
});
```

```javascript
vonage.voice.getCall(VOICE_CALL_ID)
  .then((call) => console.log(call))
  .catch((error) => console.error(error));
```

```bash
$ node retrieve-info-for-a-call.js
```

### Kotlin

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk-kotlin:2.1.1'
```

#### Initialize your dependencies

Create a file named `RetrieveCallInfo` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-kotlin-code-snippets/blob/1a41b5234a23ab2e937f5963d0d698a8934ca25d/src/main/kotlin/com/vonage/quickstart/kt/voice/RetrieveCallInfo.kt#L28-L31)

```kotlin
val client = Vonage {
    applicationId(VONAGE_APPLICATION_ID)
    privateKeyPath(VONAGE_PRIVATE_KEY_PATH)
}
```

```kotlin
val callDetails = client.voice.call(VOICE_CALL_ID).info()
println(callDetails)
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.kt.voice.RetrieveCallInfo
```

### Java

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk:9.3.1'
```

#### Initialize your dependencies

Create a file named `RetrieveCallInfo` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-java-code-snippets/blob/223b17f76d061456a202218b0c05011274bd5550/src/main/java/com/vonage/quickstart/voice/RetrieveCallInfo.java#L30-L33)

```java
VonageClient client = VonageClient.builder()
        .applicationId(VONAGE_APPLICATION_ID)
        .privateKeyPath(VONAGE_PRIVATE_KEY_PATH)
        .build();
```

```java
CallInfo details = client.getVoiceClient().getCallDetails(VOICE_CALL_ID);
System.out.println(details);
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.voice.RetrieveCallInfo
```

### .NET

#### Install dependencies

```bash
Install-Package Vonage
```

```csharp
var credentials = Credentials.FromAppIdAndPrivateKeyPath(VONAGE_APPLICATION_ID, VONAGE_PRIVATE_KEY_PATH);
var client = new VonageClient(credentials);

var response = await client.VoiceClient.GetCallAsync(VOICE_CALL_ID);
```

### PHP

#### Install dependencies

```bash
composer require vonage/client
```

```php
require_once __DIR__ . '/../../vendor/autoload.php';

$keypair = new \Vonage\Client\Credentials\Keypair(file_get_contents(VONAGE_APPLICATION_PRIVATE_KEY_PATH), VONAGE_APPLICATION_ID);
$client = new \Vonage\Client($keypair);

$call = $client->voice()->get(VONAGE_CALL_UUID);
echo json_encode($call->toArray());
```

```bash
$ php index.php
```

### Python

#### Install dependencies

```bash
pip install vonage python-dotenv
```

```python
from vonage import Auth, Vonage
from vonage_voice import CallInfo

client = Vonage(
    Auth(
        application_id=VONAGE_APPLICATION_ID,
        private_key=VONAGE_PRIVATE_KEY,
    )
)

response: CallInfo = client.voice.get_call(VOICE_CALL_ID)
pprint(response)
```

```bash
$ python voice/retrieve-info-for-a-call.py
```

### Ruby

#### Install dependencies

```bash
gem install vonage
```

```ruby
client = Vonage::Client.new(
  application_id: VONAGE_APPLICATION_ID,
  private_key: VONAGE_PRIVATE_KEY
)

response = client.voice.get(VOICE_CALL_ID)
```

```bash
$ ruby retrieve-info-for-a-call.rb
```

## Try it out

You will need to:

1.  Set up a call and obtain the call UUID. You could use the 'connect an inbound call' code snippet to do this.
2.  Retrieve information for the call (this code snippet).