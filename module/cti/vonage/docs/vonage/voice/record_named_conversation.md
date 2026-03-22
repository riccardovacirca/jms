# Record a named conversation

A code snippet that shows how to record a conversation. Answer an incoming call and return an NCCO that joins the caller to a named conversation. By setting `record` to true, the conversation is recorded and when the call is complete, a webhook is sent to the `eventUrl` you specify. The webhook includes the URL of the recording.

## Example

### Node.js

#### Install dependencies

```bash
npm install express body-parser
```

```javascript
const Express = require('express');
const bodyParser = require('body-parser');

const app = new Express();
app.use(bodyParser.json());

const onInboundCall = (request, response) => {
  const ncco = [
    {
      'action': 'conversation',
      'name': VOICE_CONF_NAME,
      'record': 'true',
      'eventMethod': 'POST',
      'eventUrl': [`${request.protocol}://${request.get('host')}/webhooks/recordings`],
    },
  ];

  response.json(ncco);
};

const onRecording = (request, response) => {
  const recording_url = request.body?.recording_url;
  console.log(`Recording URL = ${recording_url}`);

  response.status(204).send();
};

app
  .get('/webhooks/answer', onInboundCall)
  .post('/webhooks/recordings', onRecording);

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`);
});
```

```bash
$ node record-a-conversation.js
```

### Kotlin

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk-kotlin:2.1.1'
implementation 'io.ktor:ktor-server-netty'
implementation 'io.ktor:ktor-serialization-jackson'
```

```kotlin
embeddedServer(Netty, port = 8000) {
    routing {
        get("/webhooks/answer") {
            call.response.header("Content-Type", "application/json")
            call.respond(
                Ncco(
                    conversationAction(VOICE_CONFERENCE_NAME) {
                        record(true)
                        eventMethod(EventMethod.POST)
                        eventUrl(call.request.path().replace("answer", "recordings"))
                    },
                ).toJson()
            )
        }
        post("/webhooks/recordings") {
            val event = EventWebhook.fromJson(call.receive())
            println("Recording URL: ${event.recordingUrl}")
            call.respond(204)
        }
    }
}.start(wait = true)
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.kt.voice.RecordConversation
```

### Java

#### Install dependencies

Add the following to `build.gradle`:

```groovy
implementation 'com.vonage:server-sdk:9.3.1'
implementation 'com.sparkjava:spark-core:2.9.4'
```

```java
/*
 * Route to answer and connect incoming calls with recording.
 */
Route answerRoute = (req, res) -> {
    String recordingUrl = String.format("%s://%s/webhooks/recordings", req.scheme(), req.host());

    ConversationAction conversation = ConversationAction.builder(VOICE_CONFERENCE_NAME)
            .record(true)
            .eventMethod(EventMethod.POST)
            .eventUrl(recordingUrl)
            .build();

    res.type("application/json");

    return new Ncco(conversation).toJson();
};

/*
 * Route which prints out the recording URL it is given to stdout.
 */
Route recordingWebhookRoute = (req, res) -> {
    System.out.println(EventWebhook.fromJson(req.body()).getRecordingUrl());

    res.status(204);
    return "";
};

Spark.port(3000);
Spark.get("/webhooks/answer", answerRoute);
Spark.post("/webhooks/recordings", recordingWebhookRoute);
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.voice.RecordConversation
```

### .NET

#### Install dependencies

```bash
Install-Package Vonage
```

```csharp
[HttpGet("webhooks/answer")]
public IActionResult Answer()
{
    var VOICE_CONFERENCE_NAME = Environment.GetEnvironmentVariable("VOICE_CONFERENCE_NAME") ?? "VOICE_CONFERENCE_NAME";
    var host = Request.Host.ToString();
    //Uncomment the next line if using ngrok with --host-header option
    //host = Request.Headers["X-Original-Host"];
    var sitebase = $"{Request.Scheme}://{host}";

    var conversationAction = new ConversationAction
    { 
        Name = VOICE_CONFERENCE_NAME, Record = true, 
        EventMethod = "POST",
        EventUrl = new [] { $"{sitebase}/recordconversation/webhooks/recording" }
    };
    var ncco = new Ncco(conversationAction);
    var json = ncco.ToString();
    return Ok(json);
}

[HttpPost("webhooks/recording")]
public async Task<IActionResult> Recording()
{
    var record = await WebhookParser.ParseWebhookAsync<Record>(Request.Body, Request.ContentType);
    Console.WriteLine($"Record event received on webhook - URL: {record?.RecordingUrl}");
    return StatusCode(204);
}
```

### PHP

#### Install dependencies

```bash
composer require slim/slim:^3.8 vonage/client
```

```php
require 'vendor/autoload.php';

$dotenv = Dotenv::createImmutable(__DIR__);
$dotenv->load();

define('CONF_NAME', getenv('CONF_NAME'));

$app = new \Slim\App();

$app->map(['GET', 'POST'], '/webhooks/event', function($request, $response) {
    error_log(print_r($_REQUEST, true));
});

$app->get('/webhooks/answer', function (Request $request, Response $response) {
    //Get our public URL for this route
    $uri = $request->getUri();
    $url = $uri->getScheme() . '://'.$uri->getHost() . ($uri->getPort() ? ':'.$uri->getPort() : '') . '/webhooks/recordings';

    $conversation = new \Vonage\Voice\NCCO\Action\Conversation(CONF_NAME);
    $conversation->setRecord(true);
    $conversation->setEventWebhook(new \Vonage\Voice\Webhook($url));

    $ncco = new \Vonage\Voice\NCCO\NCCO();
    $ncco->addAction($conversation);

    return new JsonResponse($ncco);
});

$app->post('/webhooks/recordings', function (Request $request, Response $response) {
    /** @var \Vonage\Voice\Webhook\Record */
    $recording = \Vonage\Voice\Webhook\Factory::createFromRequest($request);
    error_log($recording->getRecordingUrl());

    return $response->withStatus(204);
});

$app->run();
```

```bash
$ php index.php
```

### Python

#### Install dependencies

```bash
pip install vonage python-dotenv fastapi[standard]
```

```python
import os
from os.path import dirname, join
from pprint import pprint

from dotenv import load_dotenv
from fastapi import Body, FastAPI
from vonage_voice import Conversation

dotenv_path = join(dirname(__file__), '../.env')
load_dotenv(dotenv_path)

VOICE_CONFERENCE_NAME = os.environ.get('VOICE_CONFERENCE_NAME')

app = FastAPI()


@app.get('/webhooks/answer')
async def answer_call():
    ncco = [
        Conversation(
            name=VOICE_CONFERENCE_NAME,
            record=True,
            eventMethod='POST',
            eventUrl=['https://demo.ngrok.io/webhooks/recordings'],
        )
    ]

    return ncco


@app.post('/webhooks/recordings')
async def recordings(data: dict = Body(...)):
    pprint(data)
    return {'message': 'webhook received'}
```

```bash
$ fastapi dev voice/record-a-conversation.py
```

### Ruby

#### Install dependencies

```bash
gem install sinatra sinatra-contrib
```

```ruby
require 'sinatra'
require 'sinatra/multi_route'
require 'json'

VOICE_CONFERENCE_NAME = ENV['VOICE_CONFERENCE_NAME']

before do
  content_type :json
end

helpers do
  def parsed_body
    JSON.parse(request.body.read)
  end
end


route :get, :post, '/webhooks/answer' do
  [
    {
      action: "conversation",
      name: VOICE_CONFERENCE_NAME,
      record: "true",
      eventMethod: "POST", 
      eventUrl: ["#{request.base_url}/webhooks/recordings"]
    }
  ].to_json
end

route :get, :post, '/webhooks/recordings' do
  recording_url = params['recording_url'] || parsed_body['recording_url']
  puts "Recording URL = #{recording_url}"

  halt 204
end

set :port, 3000
```

```bash
$ ruby record-a-conversation.rb
```

## Try it out

You will need to:

1.  Record a conversation by dialling your Vonage Number (this code snippet).
2.  Download the recording. See the [Download a recording](https://developer.vonage.com/en/voice/voice-api/code-snippets/recording-calls/download-a-recording) code snippet for how to do this.

## Further Reading

*   [Call Recording](https://developer.vonage.com/en/voice/voice-api/concepts/recording) - Recording audio input from a caller or recording the conversation between two callers.