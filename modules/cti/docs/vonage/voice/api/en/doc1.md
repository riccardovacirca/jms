# Getting Started with the Voice API

This page will talk you through all of the necessary steps to get up and running with the Vonage Voice API.

## Prerequisites

Before you begin, you will need the following:

*   [Create a Vonage account](https://developer.vonage.com/en/voice/voice-api/getting-started#create-a-vonage-account)
*   [Try the Voice API](https://developer.vonage.com/en/voice/voice-api/getting-started#try-the-voice-api)
*   [Create an Application](https://developer.vonage.com/en/voice/voice-api/getting-started#create-an-application)
*   [Rent a Number](https://developer.vonage.com/en/voice/voice-api/getting-started#rent-a-number)

### Create a Vonage account

To work with our APIs, you will need to [sign up for an account](https://developer.vonage.com/en/account/guides/dashboard-management#create-and-configure-a-vonage-account). This will give you an API key and secret that you can use to access our APIs.

> You can use the Voice API to make a voice call. Use the test number 123456789 as the caller ID, and call the number you originally provided during sign-up. Please note that this feature is only available for demo or trial accounts until you add credit to your account.

### Try the Voice API

After [signing up for a Vonage API account](https://ui.idp.vonage.com/ui/auth/registration?icid=tryitfree_adpdocs_nexmodashbdfreetrialsignup_inpagelink), access the [Developer Dashboard](https://dashboard.nexmo.com) and go to the [Make a Voice Call](https://dashboard.nexmo.com/make-a-voice-call) section. Here, you can make a test call to see the Voice API in action.

![Try Voice API Developer Dashboard view](https://developer.vonage.com/api/v1/developer/assets/images/voice-api/getting-started/try-voice-api.png)

Let’s now learn how to use the Voice API in your application.

### Create an Application

### Using the Vonage Dashboard

### Using the Vonage CLI

```powershell
vonage apps create 'Your application'

✅ Creating Application
Saving private key ... Done!
Application created

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
  None Enabled
```

```cmd
vonage apps create 'Your application'

✅ Creating Application
Saving private key ... Done!
Application created

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
  None Enabled
```

```powershell
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice `
  --voice-answer-url='https://example.com/webhooks/voice/answer' `
  --voice-event-url='https://example.com/webhooks/voice/event' `
  --voice-fallback-url='https://example.com/webhooks/voice/fallback'
  
✅ Fetching Application
✅ Adding voice capability to application 00000000-0000-0000-0000-000000000000

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
 VOICE:
    Uses Signed callbacks: On
    Conversation TTL: 41 hours
    Leg Persistence Time: 6 days
    Event URL: [POST] https://example.com/webhooks/voice/event
    Answer URL: [POST] https://example.com/webhooks/voice/answer
    Fallback URL: [POST] https://example.com/webhooks/voice/fallback
```

```cmd
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice ^
  --voice-answer-url='https://example.com/webhooks/voice/answer' ^
  --voice-event-url='https://example.com/webhooks/voice/event' ^
  --voice-fallback-url='https://example.com/webhooks/voice/fallback'
  
✅ Fetching Application
✅ Adding voice capability to application 00000000-0000-0000-0000-000000000000

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
 VOICE:
    Uses Signed callbacks: On
    Conversation TTL: 41 hours
    Leg Persistence Time: 6 days
    Event URL: [POST] https://example.com/webhooks/voice/event
    Answer URL: [POST] https://example.com/webhooks/voice/answer
    Fallback URL: [POST] https://example.com/webhooks/voice/fallback
```

```bash
$ vonage apps create 'Your application'
$ ✅ Creating Application
$ Saving private key ... Done!
$ Application created
$ Name: Your application
$ Application ID: 00000000-0000-0000-0000-000000000000
$ Improve AI: Off
$ Private/Public Key: Set
$ Capabilities:
$ None Enabled
```

```bash
$ vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice \
--voice-answer-url='https://example.com/webhooks/voice/answer' \
--voice-event-url='https://example.com/webhooks/voice/event' \
--voice-fallback-url='https://example.com/webhooks/voice/fallback'
$ ✅ Fetching Application
$ ✅ Adding voice capability to application 00000000-0000-0000-0000-000000000000
$ Name: Your application
$ Application ID: 00000000-0000-0000-0000-000000000000
$ Improve AI: Off
$ Private/Public Key: Set
$ Capabilities:
$ VOICE:
$ Uses Signed callbacks: On
$ Conversation TTL: 41 hours
$ Leg Persistence Time: 6 days
$ Event URL: [POST] https://example.com/webhooks/voice/event
$ Answer URL: [POST] https://example.com/webhooks/voice/answer
$ Fallback URL: [POST] https://example.com/webhooks/voice/fallback
```

### Powershell (Windows)

```powershell
vonage apps create 'Your application'

✅ Creating Application
Saving private key ... Done!
Application created

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
  None Enabled
```

### CMD (Windows)

```cmd
vonage apps create 'Your application'

✅ Creating Application
Saving private key ... Done!
Application created

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
  None Enabled
```

### Bash

```bash
$ vonage apps create 'Your application'
$ ✅ Creating Application
$ Saving private key ... Done!
$ Application created
$ Name: Your application
$ Application ID: 00000000-0000-0000-0000-000000000000
$ Improve AI: Off
$ Private/Public Key: Set
$ Capabilities:
$ None Enabled
```

### Powershell (Windows)

```powershell
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice `
  --voice-answer-url='https://example.com/webhooks/voice/answer' `
  --voice-event-url='https://example.com/webhooks/voice/event' `
  --voice-fallback-url='https://example.com/webhooks/voice/fallback'
  
✅ Fetching Application
✅ Adding voice capability to application 00000000-0000-0000-0000-000000000000

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
 VOICE:
    Uses Signed callbacks: On
    Conversation TTL: 41 hours
    Leg Persistence Time: 6 days
    Event URL: [POST] https://example.com/webhooks/voice/event
    Answer URL: [POST] https://example.com/webhooks/voice/answer
    Fallback URL: [POST] https://example.com/webhooks/voice/fallback
```

### CMD (Windows)

```cmd
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice ^
  --voice-answer-url='https://example.com/webhooks/voice/answer' ^
  --voice-event-url='https://example.com/webhooks/voice/event' ^
  --voice-fallback-url='https://example.com/webhooks/voice/fallback'
  
✅ Fetching Application
✅ Adding voice capability to application 00000000-0000-0000-0000-000000000000

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
 VOICE:
    Uses Signed callbacks: On
    Conversation TTL: 41 hours
    Leg Persistence Time: 6 days
    Event URL: [POST] https://example.com/webhooks/voice/event
    Answer URL: [POST] https://example.com/webhooks/voice/answer
    Fallback URL: [POST] https://example.com/webhooks/voice/fallback
```

### Bash

```bash
$ vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice \
--voice-answer-url='https://example.com/webhooks/voice/answer' \
--voice-event-url='https://example.com/webhooks/voice/event' \
--voice-fallback-url='https://example.com/webhooks/voice/fallback'
$ ✅ Fetching Application
$ ✅ Adding voice capability to application 00000000-0000-0000-0000-000000000000
$ Name: Your application
$ Application ID: 00000000-0000-0000-0000-000000000000
$ Improve AI: Off
$ Private/Public Key: Set
$ Capabilities:
$ VOICE:
$ Uses Signed callbacks: On
$ Conversation TTL: 41 hours
$ Leg Persistence Time: 6 days
$ Event URL: [POST] https://example.com/webhooks/voice/event
$ Answer URL: [POST] https://example.com/webhooks/voice/answer
$ Fallback URL: [POST] https://example.com/webhooks/voice/fallback
```

### Rent a Number

To rent a number, you must first [add credit to your account](https://developer.vonage.com/en/account/guides/payments#add-a-payment-method).

> You can skip this step if you want to use the test number 123456789 as a caller ID, and call the number you originally provided during sign-up.

### Using the Vonage Dashboard

### Using the Vonage CLI

```powershell
vonage numbers search US

✅ Searching for numbers

There is 1 number available for purchase in United States

Number       Type    Features         Monthly Cost  Setup Cost
-----------  ------  ---------------  ------------  ----------
16127779311  Mobile  MMS, SMS, VOICE  €0.90         €0.00

Use vonage numbers buy to purchase.
```

```cmd
vonage numbers search US

✅ Searching for numbers

There is 1 number available for purchase in United States

Number       Type    Features         Monthly Cost  Setup Cost
-----------  ------  ---------------  ------------  ----------
16127779311  Mobile  MMS, SMS, VOICE  €0.90         €0.00

Use vonage numbers buy to purchase.
```

```powershell
vonage numbers buy US 16127779311 
✅ Searching for numbers
Are you sure you want to purchase the number 16127779311 for €0.90? [y/n] y

✅ Purchasing number
Number 16127779311 purchased

Number: 16127779311 
Country: 🇺🇸 United States
Type: Mobile
Features: MMS, SMS, VOICE
Monthly Cost: €0.90
Setup Cost: €0.00
Linked Application ID: Not linked to any application
Voice Callback: Not Set
Voice Callback Value: Not Set
Voice Status Callback: Not Set
```

```cmd
vonage numbers buy US 16127779311 
✅ Searching for numbers
Are you sure you want to purchase the number 16127779311 for €0.90? [y/n] y

✅ Purchasing number
Number 16127779311 purchased

Number: 16127779311 
Country: 🇺🇸 United States
Type: Mobile
Features: MMS, SMS, VOICE
Monthly Cost: €0.90
Setup Cost: €0.00
Linked Application ID: Not linked to any application
Voice Callback: Not Set
Voice Callback Value: Not Set
Voice Status Callback: Not Set
```

```bash
$ vonage numbers search US
$ ✅ Searching for numbers
$ There is 1 number available for purchase in United States
$ Number Type Features Monthly Cost Setup Cost
$ ----------- ------ --------------- ------------ ----------
$ 16127779311 Mobile MMS, SMS, VOICE €0.90 €0.00
$ Use vonage numbers buy to purchase.
```

```bash
$ vonage numbers buy US 16127779311
$ ✅ Searching for numbers
$ Are you sure you want to purchase the number 16127779311 for €0.90? [y/n] y
$ ✅ Purchasing number
$ Number 16127779311 purchased
$ Number: 16127779311
$ Country: 🇺🇸 United States
$ Type: Mobile
$ Features: MMS, SMS, VOICE
$ Monthly Cost: €0.90
$ Setup Cost: €0.00
$ Linked Application ID: Not linked to any application
$ Voice Callback: Not Set
$ Voice Callback Value: Not Set
$ Voice Status Callback: Not Set
```

### Powershell (Windows)

```powershell
vonage numbers search US

✅ Searching for numbers

There is 1 number available for purchase in United States

Number       Type    Features         Monthly Cost  Setup Cost
-----------  ------  ---------------  ------------  ----------
16127779311  Mobile  MMS, SMS, VOICE  €0.90         €0.00

Use vonage numbers buy to purchase.
```

### CMD (Windows)

```cmd
vonage numbers search US

✅ Searching for numbers

There is 1 number available for purchase in United States

Number       Type    Features         Monthly Cost  Setup Cost
-----------  ------  ---------------  ------------  ----------
16127779311  Mobile  MMS, SMS, VOICE  €0.90         €0.00

Use vonage numbers buy to purchase.
```

### Bash

```bash
$ vonage numbers search US
$ ✅ Searching for numbers
$ There is 1 number available for purchase in United States
$ Number Type Features Monthly Cost Setup Cost
$ ----------- ------ --------------- ------------ ----------
$ 16127779311 Mobile MMS, SMS, VOICE €0.90 €0.00
$ Use vonage numbers buy to purchase.
```

### Powershell (Windows)

```powershell
vonage numbers buy US 16127779311 
✅ Searching for numbers
Are you sure you want to purchase the number 16127779311 for €0.90? [y/n] y

✅ Purchasing number
Number 16127779311 purchased

Number: 16127779311 
Country: 🇺🇸 United States
Type: Mobile
Features: MMS, SMS, VOICE
Monthly Cost: €0.90
Setup Cost: €0.00
Linked Application ID: Not linked to any application
Voice Callback: Not Set
Voice Callback Value: Not Set
Voice Status Callback: Not Set
```

### CMD (Windows)

```cmd
vonage numbers buy US 16127779311 
✅ Searching for numbers
Are you sure you want to purchase the number 16127779311 for €0.90? [y/n] y

✅ Purchasing number
Number 16127779311 purchased

Number: 16127779311 
Country: 🇺🇸 United States
Type: Mobile
Features: MMS, SMS, VOICE
Monthly Cost: €0.90
Setup Cost: €0.00
Linked Application ID: Not linked to any application
Voice Callback: Not Set
Voice Callback Value: Not Set
Voice Status Callback: Not Set
```

### Bash

```bash
$ vonage numbers buy US 16127779311
$ ✅ Searching for numbers
$ Are you sure you want to purchase the number 16127779311 for €0.90? [y/n] y
$ ✅ Purchasing number
$ Number 16127779311 purchased
$ Number: 16127779311
$ Country: 🇺🇸 United States
$ Type: Mobile
$ Features: MMS, SMS, VOICE
$ Monthly Cost: €0.90
$ Setup Cost: €0.00
$ Linked Application ID: Not linked to any application
$ Voice Callback: Not Set
$ Voice Callback Value: Not Set
$ Voice Status Callback: Not Set
```

## Making an Outbound Call

The primary way that you'll interact with the Vonage API voice platform is via the [public API](https://developer.vonage.com/en/voice/voice-api/technical-details). To place an outbound call, you make a `POST` request to `https://api.nexmo.com/v1/calls`.

To make your first call with the Voice API, choose your language below and replace the following variables in the example code:

| Key | Description |
| --- | --- |
| `VONAGE_NUMBER` | Your Vonage number that the call will be made from. For example `447700900000`. If you skipped the [Rent a Number](https://developer.vonage.com/en/voice/voice-api/getting-started#rent-a-number) step, use the test number “123456789”. |
| `TO_NUMBER` | The number you would like to call to in E.164 format. For example `447700900001`. If you skipped the [Rent a Number](https://developer.vonage.com/en/voice/voice-api/getting-started#rent-a-number) step, use the number you originally provided during sign-up. |

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

To make this easier, Vonage provides [Server SDKs](https://developer.vonage.com/en/tools) in various languages that take care of authentication and creating the correct request body for you.

## What Next?

Once you've made your first call, you're ready to try out other aspects of the Voice API. We recommend starting with the [Technical Details](https://developer.vonage.com/en/voice/voice-api/technical-details) page for a comprehensive overview of the Vonage Voice API. To understand various call flows, check out the [Call Flow](https://developer.vonage.com/en/voice/voice-api/concepts/call-flow?source=voice) guide. If you're interested in building a basic Voice Notification application, refer to the [Voice Notifications](https://developer.vonage.com/en/voice/voice-api/guides/voice-notifications?source=voice) How-to guide. For more information, please refer to our Voice API documentation.