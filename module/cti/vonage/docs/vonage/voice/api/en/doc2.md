# Make an outbound call

This code snippet makes an outbound call and plays a text-to-speech message when the call is answered.

## Example

Replace the following variables in the example code:

| Key | Description |
| --- | --- |
| `VONAGE_VIRTUAL_NUMBER` | Your Vonage Number. E.g. 447700900000 |
| `VOICE_TO_NUMBER` | The recipient number to call, e.g. 447700900002. |
| `VOICE_ANSWER_URL` | The answer URL. For example https://raw.githubusercontent.com/nexmo-community/ncco-examples/gh-pages/text-to-speech.json. |

### cURL

#### Generate your JWT

Execute the following command at your terminal prompt to create the [JWT](https://developer.vonage.com/en/concepts/guides/authentication#json-web-tokens-jwt) for authentication:

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

#### Install dependencies

```bash
npm install @vonage/server-sdk
```

#### Initialize your dependencies

Create a file named `make-an-outbound-call.js` and add the following code:

[View full source](https://github.com/Vonage/vonage-node-code-snippets/blob/3164e3fd94822aec9ae926c1771d58636e01c4a7/voice/make-an-outbound-call.js#L9-L14)

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

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk-kotlin:2.1.1'
```

#### Initialize your dependencies

Create a file named `OutboundTextToSpeechCall` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-kotlin-code-snippets/blob/1a41b5234a23ab2e937f5963d0d698a8934ca25d/src/main/kotlin/com/vonage/quickstart/kt/voice/OutboundTextToSpeechCall.kt#L28-L31)

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

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk:9.3.1'
```

#### Initialize your dependencies

Create a file named `OutboundTextToSpeech` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-java-code-snippets/blob/223b17f76d061456a202218b0c05011274bd5550/src/main/java/com/vonage/quickstart/voice/OutboundTextToSpeech.java#L30-L33)

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

#### Install dependencies

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

#### Install dependencies

```bash
composer require vonage/client
```

```php
require_once __DIR__ . '/../config.php';
require_once __DIR__ . '/../vendor/autoload.php';

// Building Blocks
// 1. Make a Phone Call
// 2. Play Text-to-Speech

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

#### Install dependencies

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

#### Install dependencies

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

## Try it out

When you run the code the `TO_NUMBER` will be called and a text-to-speech message will be heard if the call is answered.

## Further Reading

*   [Voice Notifications](https://developer.vonage.com/en/voice/voice-api/guides/voice-notifications) - In this guide, you will learn how to contact a list of people by phone, convey a message, and see who confirmed that they had received the message. These voice-based critical alerts are more persistent than a text message, making your message more likely to be noticed. Additionally, with the recipient confirmation, you can be sure that your message made it through.
*   [Conference Calling](https://developer.vonage.com/en/voice/voice-api/guides/conference-calling) - This guide explains the two concepts Vonage associates with a call, a leg and a conversation.
*   [Voice Bot with Google Dialogflow](https://developer.vonage.com/en/voice/voice-api/guides/voice-bot-dialogflow) - This guide will help you to start with an example Dialogflow bot and interact with it from phone calls using provided sample reference codes using Vonage Voice API.