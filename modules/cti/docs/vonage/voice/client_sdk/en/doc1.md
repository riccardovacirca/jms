# Create your Application, Users, and Tokens

In order to use the Client SDK, there are three things you need to set up before getting started:

*   [Vonage Application](https://developer.vonage.com/en/application/overview) - an Application which contains configuration for the app that you are building.
*   [Users](https://developer.vonage.com/en/conversation/concepts/user) - Users who are associated with the Vonage Application. It is expected that Users will have a one-to-one mapping with your own authentication system.
*   [JSON Web Tokens, JWTs](https://jwt.io/) - Client SDK uses JWTs for authentication. In order for a user to log in and use the SDK functionality, you need to provide a JWT per user. JWTs contain all the information the Vonage platform needs to authenticate requests, as well as information such as the associated Applications, Users and permissions.

All of these may be [created by your backend](https://developer.vonage.com/en/vonage-client-sdk/backend). If you wish to get started and experience using the SDK straightaway you can also deploy a backend application for use with the Client SDK with the [Client SDK Sample Server template](https://developer.vonage.com/en/cloud-runtime/6b041470-0b95-4d90-bbd7-ac18946edd6a_client-sdk-voice-sample-server). Otherwise, this tutorial will show you how to do so, using the [Vonage CLI](https://github.com/vonage/vonage-cli).

## Prerequisites

Make sure you have the following:

*   A Vonage account - [sign up](https://ui.idp.vonage.com/ui/auth/registration?icid=tryitfree_adpdocs_nexmodashbdfreetrialsignup_inpagelink)
*   [Node.JS](https://nodejs.org/en/download/) and NPM installed
*   Install the [Vonage CLI](https://developer.vonage.com/en/getting-started/tools/cli).

## Create a Vonage Application

You now need to create a Vonage application. In this example you create an application capable of handling in-app Voice use case.

1.  First create your project directory if you've not already done so.
2.  Change into the project directory you've now created.
3.  Use the following commands to create and configure a Vonage application with Voice and WebRTC capabilities. Replace the webhook URLs with your own. If your platform restricts the inbound traffic it can receive using IP address-ranges you'll need to add the [Vonage IP addresses](https://api.support.vonage.com/hc/en-us/articles/360035471331) to your allow list. The IP addresses can be fetched programmatically by sending a GET request to `https://api.nexmo.com/ips-v4`.

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

Add Voice Capabilities:

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

Add RTC Capabilities

### Powershell (Windows)

```powershell
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 messages `
  --messages-inbound-url='https://example.com/messages/inboud' `
  --messages-status-url='https://example.com/messages/status' `
  --messages-version='v1' `
  --no-messages-authenticate-media
  
✅ Fetching Application
✅ Adding messages capability to application 00000000-0000-0000-0000-000000000000

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
  MESSAGES:
    Authenticate Inbound Media: Off
    Webhook Version: v1
    Status URL: [POST] https://example.com/messages/status
    Inbound URL: [POST] https://example.com/messages/inboud
```

### CMD (Windows)

```cmd
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 messages ^
  --messages-inbound-url='https://example.com/messages/inboud' ^
  --messages-status-url='https://example.com/messages/status' ^
  --messages-version='v1' ^
  --no-messages-authenticate-media
  
✅ Fetching Application
✅ Adding messages capability to application 00000000-0000-0000-0000-000000000000

Name: Your application
Application ID: 00000000-0000-0000-0000-000000000000
Improve AI: Off
Private/Public Key: Set

Capabilities:
  MESSAGES:
    Authenticate Inbound Media: Off
    Webhook Version: v1
    Status URL: [POST] https://example.com/messages/status
    Inbound URL: [POST] https://example.com/messages/inboud
```

### Bash

```bash
$ vonage apps capabilities update 00000000-0000-0000-0000-000000000000 rtc \
--rtc-event-url='https://example.com/webhooks/rtc/fallback'
$ ✅ Fetching Application
$ ✅ Adding rtc capability to application 00000000-0000-0000-0000-000000000000
$ Name: Your application
$ Application ID: 00000000-0000-0000-0000-000000000000
$ Improve AI: Off
$ Private/Public Key: Set
$ Capabilities:
$ RTC:
$ Event URL: [POST] https://example.com/webhooks/voice/rtc
$ Uses Signed callbacks: Off
```

The application is then created. A private key file `private.key` is also created.

Creating an application and application capabilities are covered in detail in the [application guide](https://developer.vonage.com/en/application/overview).

## Create a User

Create a User who will log in to Vonage Client and participate in the SDK functionality: Conversations, Calls and so on.

Run the following command in your terminal to create a user named Alice:

### Powershell (Windows)

```powershell
vonage users create `
  --name='Alice'
  
✅ Creating User

User ID: USR-00000000-0000-0000-0000-000000000000
Name: Alice
Display Name: Not Set
Image URL: Not Set
Time to Live: Not Set

Channels:
  None Set
```

### CMD (Windows)

```cmd
vonage users create ^
  --name='Alice'
  
✅ Creating User

User ID: USR-00000000-0000-0000-0000-000000000000
Name: Alice
Display Name: Not Set
Image URL: Not Set
Time to Live: Not Set

Channels:
  None Set
```

### Bash

```bash
$ vonage users create \
--name='Alice'
$ ✅ Creating User
$ User ID: USR-00000000-0000-0000-0000-000000000000
$ Name: Alice
$ Display Name: Not Set
$ Image URL: Not Set
$ Time to Live: Not Set
$ Channels:
$ None Set
```

The user ID is used to perform tasks by the SDK, such as login, starting a call and more.

## Generate a User JWT

[JWTs](https://jwt.io) are used to authenticate a user into the Client SDK.

To generate a JWT for Alice run the following command (replacing with your information):

### Powershell (Windows)

```powershell
# A command with parameters
vonage jwt create `
--app-id='00000000-0000-0000-0000-000000000000' `
--private-key=./private.key `
--sub='Alice' `
--acl='{\"paths\":{\"\/*\/users\/**\":{},\"\/*\/conversations\/**\":{},\"\/*\/sessions\/**\":{},\"\/*\/devices\/**\":{},\"\/*\/push\/**\":{},\"\/*\/knocking\/**\":{},\"\/*\/legs\/**\":{}}}'

# Will produce a token
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2wiOnsicGF0aHMiOnsiLyovcnRjLyoqIjp7fSwiLyovdXNlcnMvKioiOnt9LCIvKi9jb252ZXJzYXRpb25zLyoqIjp7fSwiLyovc2Vzc2lvbnMvKioiOnt9LCIvKi9kZXZpY2VzLyoqIjp7fSwiLyovcHVzaC8qKiI6e30sIi8qL2tub2NraW5nLyoqIjp7fSwiLyovbGVncy8qKiI6e319fSwiZXhwIjoxNzQxMTgyMzA3LCJzdWIiOiJBbGljZSIsImp0aSI6Ijg1MTViNzk2LTA1YjktNGFkMS04MTRkLTE1NWZjZTQzZWM1YiIsImlhdCI6MTc0MTE4MTQwNywiYXBwbGljYXRpb25faWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAifQ.BscMdDXZ1-nuLtKyPJvw9tE8E8ZjJvTPJPMT9y0TjPz4Q7qqNaqxcjglc5QPtYEjh2YpZH6btSKbUF4XTClI026Hl5_QOBlnayYo7jXwhba16fa5PeyzSf30QFGFrHbANwrQJFVCjd329SZUpwK4GxgB1gf230NhbfmkhegKezqicru2WTGCKm8kQncYliFwIEYUlcRAb2c8xcaVrn_6QNNahyeJRwGFfWpIkX0Oe-S4RDlPjoq47_gYWac9MmaetB4Dd3Yp531AuniGV5JiIShkaEwuY4Zyov4Hcmajm4Lm_UFY119la7vzHis0P7cT9pPUDe5cyPj7eT8-VhitfQ
```

### CMD (Windows)

```cmd
REM A command with parameters
vonage jwt create ^
--app-id='00000000-0000-0000-0000-000000000000' ^
--private-key=./private.key ^
--sub='Alice' ^
--acl="{\"paths\":{\"\/*\/users\/**\":{},\"\/*\/conversations\/**\":{},\"\/*\/sessions\/**\":{},\"\/*\/devices\/**\":{},\"\/*\/push\/**\":{},\"\/*\/knocking\/**\":{},\"\/*\/legs\/**\":{}}}"

REM Will produce a token
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2wiOnsicGF0aHMiOnsiLyovcnRjLyoqIjp7fSwiLyovdXNlcnMvKioiOnt9LCIvKi9jb252ZXJzYXRpb25zLyoqIjp7fSwiLyovc2Vzc2lvbnMvKioiOnt9LCIvKi9kZXZpY2VzLyoqIjp7fSwiLyovcHVzaC8qKiI6e30sIi8qL2tub2NraW5nLyoqIjp7fSwiLyovbGVncy8qKiI6e319fSwiZXhwIjoxNzQxMTgyMzA3LCJzdWIiOiJBbGljZSIsImp0aSI6Ijg1MTViNzk2LTA1YjktNGFkMS04MTRkLTE1NWZjZTQzZWM1YiIsImlhdCI6MTc0MTE4MTQwNywiYXBwbGljYXRpb25faWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAifQ.BscMdDXZ1-nuLtKyPJvw9tE8E8ZjJvTPJPMT9y0TjPz4Q7qqNaqxcjglc5QPtYEjh2YpZH6btSKbUF4XTClI026Hl5_QOBlnayYo7jXwhba16fa5PeyzSf30QFGFrHbANwrQJFVCjd329SZUpwK4GxgB1gf230NhbfmkhegKezqicru2WTGCKm8kQncYliFwIEYUlcRAb2c8xcaVrn_6QNNahyeJRwGFfWpIkX0Oe-S4RDlPjoq47_gYWac9MmaetB4Dd3Yp531AuniGV5JiIShkaEwuY4Zyov4Hcmajm4Lm_UFY119la7vzHis0P7cT9pPUDe5cyPj7eT8-VhitfQ
```

### Bash

```bash
# A command with parameters
$ vonage jwt create \
--app-id='00000000-0000-0000-0000-000000000000' \
--private-key=./private.key \
--sub='Alice' \
--acl='{"paths":{"/*/rtc/**":{},"/*/users/**":{},"/*/conversations/**":{},"/*/sessions/**":{},"/*/devices/**":{},"/*/push/**":{},"/*/knocking/**":{},"/*/legs/**":{}}}'
# Will produce a token
$ eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2wiOnsicGF0aHMiOnsiLyovcnRjLyoqIjp7fSwiLyovdXNlcnMvKioiOnt9LCIvKi9jb252ZXJzYXRpb25zLyoqIjp7fSwiLyovc2Vzc2lvbnMvKioiOnt9LCIvKi9kZXZpY2VzLyoqIjp7fSwiLyovcHVzaC8qKiI6e30sIi8qL2tub2NraW5nLyoqIjp7fSwiLyovbGVncy8qKiI6e319fSwiZXhwIjoxNzQxMTgyMzA3LCJzdWIiOiJBbGljZSIsImp0aSI6Ijg1MTViNzk2LTA1YjktNGFkMS04MTRkLTE1NWZjZTQzZWM1YiIsImlhdCI6MTc0MTE4MTQwNywiYXBwbGljYXRpb25faWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAifQ.BscMdDXZ1-nuLtKyPJvw9tE8E8ZjJvTPJPMT9y0TjPz4Q7qqNaqxcjglc5QPtYEjh2YpZH6btSKbUF4XTClI026Hl5_QOBlnayYo7jXwhba16fa5PeyzSf30QFGFrHbANwrQJFVCjd329SZUpwK4GxgB1gf230NhbfmkhegKezqicru2WTGCKm8kQncYliFwIEYUlcRAb2c8xcaVrn_6QNNahyeJRwGFfWpIkX0Oe-S4RDlPjoq47_gYWac9MmaetB4Dd3Yp531AuniGV5JiIShkaEwuY4Zyov4Hcmajm4Lm_UFY119la7vzHis0P7cT9pPUDe5cyPj7eT8-VhitfQ
```

The above command defaults the expiry of the JWT to 24 hours. You may change the expiration to a shortened amount of time using the `--exp` flag.

> NOTE: In production apps, it is expected that your backend will expose an endpoint that generates JWT per your client request.

## Further information

*   [More about JWTs and ACLs](https://developer.vonage.com/en/getting-started/concepts/authentication#json-web-tokens)
*   [In-app Voice tutorial](https://developer.vonage.com/en/client-sdk/tutorials/app-to-phone/introduction)
*   [In-app Messaging tutorial](https://developer.vonage.com/en/client-sdk/tutorials/in-app-messaging/introduction)