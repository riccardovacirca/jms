# Download a recording

In this code snippet you see how to download a recording.

## Example

Replace the following variables in the example code:

| Key | Description |
| --- | --- |
| `VOICE_RECORDING_URL` | The URL of the recording to download. You typically get this from the JSON response received on the /webhooks/recordings endpoint when the record action is used. |

### cURL

#### Generate your JWT

Execute the following command at your terminal prompt to create the [JWT](https://developer.vonage.com/en/concepts/guides/authentication#json-web-tokens-jwt) for authentication:

```bash
export JWT=$(nexmo jwt:generate $PATH_TO_PRIVATE_KEY application_id=$NEXMO_APPLICATION_ID)
```

```sh
curl $VOICE_RECORDING_URL \
  -H "Authorization: Bearer $JWT" \
  --output recording.mp3
```

```bash
$ bash download-a-recording.sh
```

### Node.js

#### Install dependencies

```bash
npm install @vonage/server-client
```

#### Initialize your dependencies

Create a file named `download-a-recording.js` and add the following code:

[View full source](https://github.com/Vonage/vonage-node-code-snippets/blob/3164e3fd94822aec9ae926c1771d58636e01c4a7/voice/download-a-recording.js#L8-L13)

```javascript
const { FileClient } = require('@vonage/server-client');

const fileClient = new FileClient({
  applicationId: VONAGE_APPLICATION_ID,
  privateKey: VONAGE_PRIVATE_KEY,
});
```

```javascript
fileClient.downloadFile(
  VOICE_RECORDING_URL,
  VOICE_RECORDING_DESTINATION,
)
  .then(() => console.log(`File Downloaded to ${VOICE_RECORDING_DESTINATION}`))
  .catch((error) => console.error(error));
```

```bash
$ node download-a-recording.js
```

### Kotlin

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk-kotlin:2.1.1'
```

#### Initialize your dependencies

Create a file named `DownloadRecording` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-kotlin-code-snippets/blob/1a41b5234a23ab2e937f5963d0d698a8934ca25d/src/main/kotlin/com/vonage/quickstart/kt/voice/DownloadRecording.kt#L29-L32)

```kotlin
val client = Vonage {
    applicationId(VONAGE_APPLICATION_ID)
    privateKeyPath(VONAGE_PRIVATE_KEY_PATH)
}
```

```kotlin
val destination = Paths.get("/Users/me123/Downloads")
client.voice.downloadRecording(VOICE_RECORDING_URL, destination)
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.kt.voice.DownloadRecording
```

### Java

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk:9.3.1'
```

#### Initialize your dependencies

Create a file named `DownloadRecording` and add the following code to the `main` method:

[View full source](https://github.com/Vonage/vonage-java-code-snippets/blob/223b17f76d061456a202218b0c05011274bd5550/src/main/java/com/vonage/quickstart/voice/DownloadRecording.java#L35-L38)

```java
VonageClient client = VonageClient.builder()
        .applicationId(VONAGE_APPLICATION_ID)
        .privateKeyPath(VONAGE_PRIVATE_KEY_PATH)
        .build();
```

```java
/*
 * A recording webhook endpoint which automatically downloads the specified recording to a file in the
 * current working directory, called "downloaded_recording.mp3"
 */
Route downloadRoute = (req, res) -> {
    EventWebhook event = EventWebhook.fromJson(req.body());
    String recordingUrl = event.getRecordingUrl().toString();
    Path recordingFile = Paths.get("downloaded_recording.mp3");
    System.out.println("Downloading from " + recordingUrl);
    client.getVoiceClient().saveRecording(recordingUrl, recordingFile);
    return "OK";
};

Spark.port(3000);
Spark.post("/recording", downloadRoute);
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.voice.DownloadRecording
```

### .NET

#### Install dependencies

```bash
Install-Package Vonage
```

```csharp
var credentials = Credentials.FromAppIdAndPrivateKeyPath(VONAGE_APPLICATION_ID, VONAGE_PRIVATE_KEY_PATH);
var client = new VonageClient(credentials);

var response = await client.VoiceClient.GetRecordingAsync(VOICE_RECORDING_URL);
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

$recordingUrl = 'https://api.nexmo.com/v1/files/'.VONAGE_RECORDING_ID;
$data = $client->voice()->getRecording($recordingUrl);
file_put_contents(VONAGE_RECORDING_ID .'.mp3', $data->getContents());
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

client = Vonage(
    Auth(
        application_id=VONAGE_APPLICATION_ID,
        private_key=VONAGE_PRIVATE_KEY,
    )
)

client.voice.download_recording(VOICE_RECORDING_URL, 'recording.mp3')
```

```bash
$ python voice/get-recording.py
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

response = client.files.save(VOICE_RECORDING_URL, 'recording.mp3')
```

```bash
$ ruby download-a-recording.rb
```

## Try it out

You will need a Recording URL from which to download the recording file. You typically get this from the JSON response received on the `/webhooks/recordings` endpoint when the `record` action is used when [recording a call](https://developer.vonage.com/en/voice/voice-api/code-snippets/recording-calls/record-a-call), connecting another call and so on. A typical JSON response will resemble the following:

```text
{'conversation_uuid': 'CON-ddddaaaa-bbbb-cccc-dddd-0123456789de',
 'end_time': '2018-08-10T11:19:31Z',
 'recording_url': 'https://api.nexmo.com/v1/files/aaaaaaaa-bbbb-cccc-dddd-0123456789ab',
 'recording_uuid': 'ccccaaaa-dddd-cccc-dddd-0123456789ab',
 'size': 162558,
 'start_time': '2018-08-10T11:18:51Z',
 'timestamp': '2018-08-10T11:19:31.744Z'}
1.2.3.4 - - [10/Aug/2018 11:19:31] "POST /webhooks/recordings HTTP/1.1" 200 -
```

When you run the script, the recording located at the recording URL will be downloaded. You can then listen to the recording.

## Further Reading

*   [Transcription](https://developer.vonage.com/en/voice/voice-api/guides/trancribe-amazon-api) - This guide shows you how to use the Amazon Transcribe API to transcribe a phone conversation recorded with the Vonage Voice API.