# Make an outbound call with an NCCO

This code snippet makes an outbound call and plays a text-to-speech message when the call is answered. You don't need to run a server hosting an `answer_url` to run this code snippet, as you provide your NCCO as part of the request

## Example

Replace the following variables in the example code:

| Key | Description |
| --- | --- |
| `VONAGE_VIRTUAL_NUMBER` | Your Vonage Number. E.g. 447700900000 |
| `VOICE_TO_NUMBER` | The recipient number to call, e.g. 447700900002. |

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
      "ncco": [
        {
          "action": "talk",
          "text": "This is a text to speech call from Vonage"
        }
      ]}'
```

```bash
$ sh make-an-outbound-call-with-ncco.sh
```

### Node.js

#### Install dependencies

```bash
npm install @vonage/server-sdk @vonage/voice
```

#### Initialize your dependencies

Create a file named `make-an-outbound-call-with-ncco.js` and add the following code:

[View full source](https://github.com/Vonage/vonage-node-code-snippets/blob/3164e3fd94822aec9ae926c1771d58636e01c4a7/voice/make-an-outbound-call-with-ncco.js#L8-L14)

```javascript
const { Vonage } = require('@vonage/server-sdk');
const { NCCOBuilder, Talk } = require('@vonage/voice');

const vonage = new Vonage({
  applicationId: VONAGE_APPLICATION_ID,
  privateKey: VONAGE_PRIVATE_KEY,
});
```

```javascript
const builder = new NCCOBuilder();
builder.addAction(new Talk('This is a text to speech call from Vonage'));

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
  ncco: builder.build(),
})
  .then((result) => console.log(result))
  .catch((error) => console.error(error));
```

```bash
$ node make-an-outbound-call-with-ncco.js
```

### Kotlin

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk-kotlin:2.1.1'
```

#### Initialize your dependencies

Create a file named `OutboundTextToSpeechCallWithNcco` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-kotlin-code-snippets/blob/1a41b5234a23ab2e937f5963d0d698a8934ca25d/src/main/kotlin/com/vonage/quickstart/kt/voice/OutboundTextToSpeechCallWithNcco.kt#L28-L31)

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
    ncco(
        talkAction("This is a text to speech call from Vonage")
    )
}
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.kt.voice.OutboundTextToSpeechCallWithNcco
```

### Java

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk:9.3.1'
```

#### Initialize your dependencies

Create a file named `OutboundTextToSpeechWithNcco` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-java-code-snippets/blob/223b17f76d061456a202218b0c05011274bd5550/src/main/java/com/vonage/quickstart/voice/OutboundTextToSpeechWithNcco.java#L33-L36)

```java
VonageClient client = VonageClient.builder()
        .applicationId(VONAGE_APPLICATION_ID)
        .privateKeyPath(VONAGE_PRIVATE_KEY_PATH)
        .build();
```

```java
Ncco ncco = new Ncco(TalkAction.builder("This is a text to speech call from Vonage").build());

client.getVoiceClient().createCall(new Call(VOICE_TO_NUMBER, VONAGE_VIRTUAL_NUMBER, ncco.getActions()));
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.voice.OutboundTextToSpeechWithNcco
```

### .NET

#### Install dependencies

```bash
Install-Package Vonage
```

```csharp
var client = new VonageClient(creds);

var toEndpoint = new PhoneEndpoint() { Number = VOICE_TO_NUMBER };
var fromEndpoint = new PhoneEndpoint() { Number = VONAGE_VIRTUAL_NUMBER };
var extraText = "";
for (var i = 0; i < 50; i++)
    extraText += $"{i} ";
var talkAction = new TalkAction() { Text = "This is a text to speech call from Vonage " + extraText };
var ncco = new Ncco(talkAction);

var command = new CallCommand() { To = new Endpoint[] { toEndpoint }, From = fromEndpoint, Ncco = ncco };
var response = await client.VoiceClient.CreateCallAsync(command);
```

### PHP

#### Install dependencies

```bash
composer require vonage/client
```

```php
$keypair = new \Vonage\Client\Credentials\Keypair(
    file_get_contents(VONAGE_APPLICATION_PRIVATE_KEY_PATH),
    VONAGE_APPLICATION_ID
);
$client = new \Vonage\Client($keypair);

$outboundCall = new \Vonage\Voice\OutboundCall(
    new \Vonage\Voice\Endpoint\Phone(TO_NUMBER),
    new \Vonage\Voice\Endpoint\Phone(VONAGE_NUMBER)
);
$ncco = new NCCO();
$ncco->addAction(new \Vonage\Voice\NCCO\Action\Talk('This is a text to speech call from Nexmo'));
$outboundCall->setNCCO($ncco);

$response = $client->voice()->createOutboundCall($outboundCall);

var_dump($response);
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
from vonage_voice import CreateCallRequest, Phone, Talk, ToPhone

client = Vonage(
    Auth(
        application_id=VONAGE_APPLICATION_ID,
        private_key=VONAGE_PRIVATE_KEY,
    )
)

response = client.voice.create_call(
    CreateCallRequest(
        ncco=[Talk(text='This is a text to speech call from Vonage.')],
        to=[ToPhone(number=VOICE_TO_NUMBER)],
        from_=Phone(number=VONAGE_VIRTUAL_NUMBER),
    )
)

pprint(response)
```

```bash
$ python voice/make-outbound-call-ncco.py
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

client.voice.create(
  to: [{
    type: 'phone',
    number: VOICE_TO_NUMBER
  }],
  from: {
    type: 'phone',
    number: VONAGE_VIRTUAL_NUMBER
  },
  ncco: [
    {
      'action' => 'talk',
      'text' => 'This is a text to speech call from Vonage'
    }
  ]
)
```

```bash
$ ruby make-outbound-call-with-ncco.rb
```

## Try it out

When you run the code the `VOICE_TO_NUMBER` will be called and a text-to-speech message will be heard if the call is answered.

## Further Reading

*   [Voice Notifications](https://developer.vonage.com/en/voice/voice-api/guides/voice-notifications) - In this guide, you will learn how to contact a list of people by phone, convey a message, and see who confirmed that they had received the message. These voice-based critical alerts are more persistent than a text message, making your message more likely to be noticed. Additionally, with the recipient confirmation, you can be sure that your message made it through.
*   [Conference Calling](https://developer.vonage.com/en/voice/voice-api/guides/conference-calling) - This guide explains the two concepts Vonage associates with a call, a leg and a conversation.
*   [Voice Bot with Google Dialogflow](https://developer.vonage.com/en/voice/voice-api/guides/voice-bot-dialogflow) - This guide will help you to start with an example Dialogflow bot and interact with it from phone calls using provided sample reference codes using Vonage Voice API.