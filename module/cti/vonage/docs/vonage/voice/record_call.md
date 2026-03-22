# Record a call

A code snippets that shows how to answer an incoming call and set it up to record, then connect the call. When the call is completed, the `eventUrl` you specify in the `record` action of the NCCO will receive a webhook including the URL of the recording for download.

## Example

Replace the following variables in the example code:

| Key | Description |
| --- | --- |
| `VONAGE_VIRTUAL_NUMBER` | Your Vonage Number. E.g. 447700900000 |
| `VOICE_TO_NUMBER` | The recipient number to call, e.g. 447700900002. |

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
      action: 'record',
      eventUrl: [`${request.protocol}://${request.get('host')}/webhooks/recordings`],
    },
    {
      action: 'connect',
      from: VONAGE_VIRTUAL_NUMBER,
      endpoint: [
        {
          type: 'phone',
          number: VOICE_TO_NUMBER,
        },
      ],
    },
  ];
  response.json(ncco);
};

const onRecording = (request, response) => {
  const recording_url = request.body.recording_url;
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
$ node record-a-call.js
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
                    recordAction {
                        eventUrl(call.request.path().replace("answer", "recordings"))
                    },
                    connectToPstn(VOICE_TO_NUMBER) {
                        from(VONAGE_VIRTUAL_NUMBER)
                    }
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
$ gradle run -Pmain=com.vonage.quickstart.kt.voice.RecordCall
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

    RecordAction record = RecordAction.builder().eventUrl(recordingUrl).build();

    ConnectAction connect = ConnectAction.builder(PhoneEndpoint.builder(VOICE_TO_NUMBER).build())
            .from(VONAGE_VIRTUAL_NUMBER).build();

    res.type("application/json");

    return new Ncco(record, connect).toJson();
};

/*
 * Route which prints out the recording URL it is given to stdout.
 */
Route recordingRoute = (req, res) -> {
    System.out.println(EventWebhook.fromJson(req.body()).getRecordingUrl());

    res.status(204);
    return "";
};

Spark.port(3000);
Spark.get("/webhooks/answer", answerRoute);
Spark.post("/webhooks/recordings", recordingRoute);
```

```groovy
apply plugin: 'application'
mainClassName = project.hasProperty('main') ? project.getProperty('main') : ''
```

```bash
$ gradle run -Pmain=com.vonage.quickstart.voice.RecordCall
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
    var VOICE_TO_NUMBER = Environment.GetEnvironmentVariable("VOICE_TO_NUMBER") ?? "VOICE_TO_NUMBER";
    var VONAGE_VIRTUAL_NUMBER = Environment.GetEnvironmentVariable("VONAGE_VIRTUAL_NUMBER") ?? "VONAGE_VIRTUAL_NUMBER";
    var host = Request.Host.ToString();
    //Uncomment the next line if using ngrok with --host-header option
    //host = Request.Headers["X-Original-Host"];
    var sitebase = $"{Request.Scheme}://{host}";

    var recordAction = new RecordAction()
    {
        EventUrl = new string[] { $"{sitebase}/recordcall/webhooks/recording" },
        EventMethod = "POST"
    };

    var connectAction = new ConnectAction() { From = VONAGE_VIRTUAL_NUMBER, Endpoint = new[] { new PhoneEndpoint{ Number = VOICE_TO_NUMBER } } };

    var ncco = new Ncco(recordAction, connectAction);            
    return Ok(ncco.ToString());
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

define('TO_NUMBER', getenv('TO_NUMBER'));
define('VONAGE_NUMBER', getenv('VONAGE_NUMBER'));

$app = new \Slim\App();

$app->get('/webhooks/answer', function (Request $request, Response $response) {
    //Get our public URL for this route
    $uri = $request->getUri();
    $url = $uri->getScheme() . '://'.$uri->getHost() . ($uri->getPort() ? ':'.$uri->getPort() : '') . '/webhooks/recording';

    $record = new \Vonage\Voice\NCCO\Action\Record();
    $record->setEventWebhook(new \Vonage\Voice\Webhook($url));

    $connect = new \Vonage\Voice\NCCO\Action\Connect(new \Vonage\Voice\Endpoint\Phone(TO_NUMBER));
    $connect->setFrom(VONAGE_NUMBER);

    $ncco = new \Vonage\Voice\NCCO\NCCO();
    $ncco->addAction($connect);
    $ncco->addAction($record);

    return new JsonResponse($ncco);
});

$app->post('/webhooks/recording', function (Request $request, Response $response) {
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
from vonage_voice import Connect, NccoAction, PhoneEndpoint, Record

dotenv_path = join(dirname(__file__), '../.env')
load_dotenv(dotenv_path)

VONAGE_VIRTUAL_NUMBER = os.environ.get('VONAGE_VIRTUAL_NUMBER')
VOICE_TO_NUMBER = os.environ.get('VOICE_TO_NUMBER')

app = FastAPI()


@app.get('/webhooks/answer')
async def inbound_call():
    ncco: list[NccoAction] = [
        Record(eventUrl=['https://demo.ngrok.io/webhooks/recordings']),
        Connect(
            from_=VONAGE_VIRTUAL_NUMBER, endpoint=[PhoneEndpoint(number=VOICE_TO_NUMBER)]
        ),
    ]

    return [action.model_dump(by_alias=True, exclude_none=True) for action in ncco]


@app.post('/webhooks/recordings')
async def recordings(data: dict = Body(...)):
    pprint(data)
    return {'message': 'webhook received'}
```

```bash
$ fastapi dev voice/record-a-call.py
```

### Ruby

#### Install dependencies

```bash
gem install sinatra sinatra-contrib
```

```ruby
require 'dotenv/load'
require 'sinatra'
require 'sinatra/multi_route'
require 'json'

VOICE_TO_NUMBER = ENV['VOICE_TO_NUMBER']
VONAGE_VIRTUAL_NUMBER = ENV['VONAGE_VIRTUAL_NUMBER']

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
      "action": "record",
      "eventUrl": ["#{request.base_url}/webhooks/recordings"]
    },
    {
      "action": "connect",
      "from": VONAGE_VIRTUAL_NUMBER,
      "endpoint": [
        {
          "type": "phone",
          "number": VOICE_TO_NUMBER
        }
      ]
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
$ ruby record_a_call.rb
```

## Try it out

You will need to:

1.  Answer and record the call (this code snippet).
2.  Download the recording. See the [Download a recording](https://developer.vonage.com/en/voice/voice-api/code-snippets/recording-calls/download-a-recording) code snippet for how to do this.

## Further Reading

*   [Call Recording](https://developer.vonage.com/en/voice/voice-api/concepts/recording) - Recording audio input from a caller or recording the conversation between two callers.