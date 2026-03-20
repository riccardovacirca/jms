# Voice2 Module - Operator-First Progressive Dialer Pattern

## Overview

Il modulo `voice2` implementa il pattern **operator-first progressive dialer** dove l'operatore WebRTC si connette per primo e aspetta con musica di attesa prima che il cliente venga chiamato.

Questo pattern risolve il problema del "customer wait time" presente nel flusso tradizionale dove il cliente risponde prima che l'operatore sia pronto.

## Differenze con Voice Module

| Aspetto | Voice Module | Voice2 Module |
|---------|-------------|---------------|
| **Pattern** | Customer-first | Operator-first |
| **Chiamata operatore** | Connect action (inbound) | serverCall() (outbound from browser) |
| **Attesa** | Cliente aspetta operatore | Operatore aspetta cliente |
| **NCCO conversation** | Standard (default startOnEnter) | startOnEnter: false + musicOnHoldUrl |
| **Trigger chiamata** | POST /api/voice/calls | Automatico da answer webhook |

## Architecture

### Components

**Backend:**
- `VoiceController2.java` - REST endpoints + answer webhook
- `VoiceService2.java` - Business logic, NCCO generation, Vonage API calls
- Riusa: `VoiceConfig`, `CallDao`, `CallDto` dal modulo voice

**Frontend:**
- `Layout.svelte` - Module entry point
- `SdkSessionComponent.svelte` - Operator WebRTC session + progressive dialer UI
- `store.js` - Module state management

## Call Flow - Step by Step

### 1. Operator Setup

Operatore apre GUI voice2, inserisce userId e attiva sessione WebRTC:

```javascript
// Frontend: SdkSessionComponent.svelte
const token = await fetch(`/api/voice2/sdk-token?userId=${userId}`)
await client.createSession(token)
// Sessione WebRTC attiva ✓
```

### 2. Operator Initiates Call

Operatore inserisce numero cliente e clicca "Pronto per chiamata":

```javascript
// Frontend
const callId = await client.serverCall({ customerNumber: "+39XXXXXXXXXX" })
```

**Cosa succede:**
- Client SDK fa chiamata outbound verso backend
- Vonage chiama answer_url webhook configurato sull'applicazione
- Parametro `customerNumber` viene passato al webhook come query parameter

### 3. Answer Webhook - Operator Joins Conversation

Backend riceve webhook e restituisce NCCO:

```java
// Backend: VoiceController2.handleAnswerWebhook()
POST /api/voice2/answer?customerNumber=+39XXXXXXXXXX

// Response NCCO:
[
  {
    "action": "conversation",
    "name": "call-<uuid>",
    "startOnEnter": false,
    "musicOnHoldUrl": ["https://...hold.mp3"]
  }
]
```

**Cosa succede:**
- Operatore entra in conversation "call-<uuid>"
- **startOnEnter: false** → conversation NON inizia ancora
- Operatore sente `musicOnHoldUrl` (hold music)
- Operatore è in attesa, pronto per il cliente

### 4. Backend Triggers Customer Call

Dopo aver restituito NCCO, backend triggera async chiamata cliente:

```java
// Backend: handleAnswerWebhook() - async thread
Thread.sleep(1000);  // Small delay to ensure operator NCCO processed
voiceService.callCustomer(customerNumber, conversationName);
```

```java
// VoiceService2.callCustomer()
POST /v1/calls (Vonage API)
{
  "to": [{"type": "phone", "number": "+39XXXXXXXXXX"}],
  "from": {"type": "phone", "number": "YOUR_VONAGE_NUMBER"},
  "ncco": [
    {
      "action": "conversation",
      "name": "call-<uuid>"  // stesso conversation name!
    }
  ]
}
```

**Cosa succede:**
- Sistema chiama cliente via PSTN
- Cliente sente squillo
- Cliente NON sente attesa perché operatore è già pronto

### 5. Customer Answers - Conversation Starts

Cliente risponde alla chiamata:

```
Customer answers
  ↓
Enters conversation "call-<uuid>"
  ↓
Operator startOnEnter: false → conversation STARTS (first participant with startOnEnter: true)
  ↓
Hold music stops
  ↓
Bridge attivo: Operator ↔ Customer
```

**Conversation flow:**

```
Operator (WebRTC) ───┐
                     ├──→ Vonage Conversation "call-<uuid>" ←──┐
Customer (PSTN)  ────┘                                          │
                                                                │
Hold music stops when customer joins (startOnEnter: true)  ────┘
```

### 6. Call Active

Entrambi in chiamata attiva:

- Frontend mostra: "📞 In chiamata con +39XXXXXXXXXX"
- Operatore può riagganciare con pulsante "Riaggancia"
- Pulsante chiama: `PUT /api/voice2/calls/{uuid}/hangup`

## Configuration

### 1. Vonage Application Setup

L'applicazione Vonage deve avere configurato:

```bash
# Answer URL (IMPORTANTE!)
https://YOUR_DOMAIN/api/voice2/answer

# Event URL
https://YOUR_DOMAIN/api/voice/webhook/event  # può riusare quello del voice module

# Capabilities
- Voice: enabled
- RTC: enabled (per endpoint "app")
```

**ATTENZIONE:** L'answer URL deve puntare a `/api/voice2/answer`, NON `/api/voice/answer`!

### 2. Create Operator User

Operatore deve essere registrato su Vonage:

```bash
vonage users create --name=operatore_01
```

### 3. Application Properties

Nessuna configurazione aggiuntiva necessaria. Il modulo voice2 riusa la configurazione esistente:

```properties
# In application.properties - configurazione condivisa
voice.applicationId=${VONAGE_APPLICATION_ID}
voice.privateKey=${VONAGE_PRIVATE_KEY_PATH}
voice.fromNumber=${VONAGE_FROM_NUMBER}
voice.eventUrl=${VONAGE_EVENT_URL}
```

## API Endpoints

### POST /api/voice2/answer

**Webhook answer_url** chiamato da Vonage quando operatore fa `serverCall()`.

**Query Parameters:**
- `customerNumber` (string) - numero cliente da chiamare

**Response:** NCCO array
```json
[
  {
    "action": "conversation",
    "name": "call-<uuid>",
    "startOnEnter": false,
    "musicOnHoldUrl": ["https://..."]
  }
]
```

**Side Effect:** Triggera async chiamata cliente dopo 1s delay.

### GET /api/voice2/sdk-token

Genera JWT per autenticazione Client SDK.

**Query Parameters:**
- `userId` (string) - nome utente operatore

**Response:**
```json
{
  "err": false,
  "out": {
    "token": "eyJ..."
  }
}
```

### PUT /api/voice2/calls/{uuid}/hangup

Termina chiamata attiva.

**Path Parameters:**
- `uuid` (string) - call UUID

**Response:**
```json
{
  "err": false,
  "out": {
    "message": "Call hangup triggered"
  }
}
```

## Testing

### 1. Start Application

```bash
bin/cmd app run
bin/cmd gui run
```

### 2. Access Voice2 Module

Apri browser: `http://localhost:2350/voice2`

### 3. Activate Operator Session

- Inserisci userId: `operatore_01`
- Click "Attiva Sessione"
- Verifica: "✓ Sessione attiva"

### 4. Initiate Call

- Inserisci numero cliente: `+39XXXXXXXXXX`
- Click "Pronto per Chiamata"
- Verifica: "🎵 In attesa cliente..."
- Senti musica di attesa nel browser

### 5. Customer Receives Call

- Cliente riceve chiamata sul proprio telefono
- Cliente risponde
- Musica si interrompe
- Bridge attivo: operatore ↔ cliente

### 6. End Call

- Click "Riaggancia" nel browser
- Chiamata termina per entrambi

## NCCO Details

### Operator NCCO (startOnEnter: false)

```json
{
  "action": "conversation",
  "name": "call-abc123",
  "startOnEnter": false,
  "musicOnHoldUrl": [
    "https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3"
  ]
}
```

**Parameters:**
- `name`: Conversation name univoco (generato: "call-<uuid>")
- `startOnEnter`: **false** → conversation non inizia finché qualcuno con true non entra
- `musicOnHoldUrl`: MP3 riprodotto mentre si aspetta

### Customer NCCO (startOnEnter: true by default)

```json
{
  "action": "conversation",
  "name": "call-abc123"
}
```

**Parameters:**
- `name`: Stesso conversation name dell'operatore
- `startOnEnter`: true (default) → fa partire la conversation

Quando cliente entra, conversation inizia e operatore smette di sentire musica.

## Troubleshooting

### Operatore non sente musica di attesa

**Verifica:**
- Answer URL configurato correttamente in Vonage dashboard
- Answer URL punta a `/api/voice2/answer`
- NCCO contiene `musicOnHoldUrl`
- URL musica è accessibile pubblicamente

### Cliente non viene chiamato

**Verifica:**
- Parametro `customerNumber` passato correttamente in `serverCall()`
- Backend logs: errori in async thread customer call
- Vonage credentials valide (applicationId, privateKey)
- Numero fromNumber configurato

### Conversation non inizia quando cliente risponde

**Verifica:**
- Stesso `conversation name` per operatore e cliente
- Operatore ha `startOnEnter: false`
- Cliente ha `startOnEnter: true` (o omesso, default true)

### Webhook answer non viene chiamato

**Verifica:**
- ngrok attivo: `bin/cmd ngrok run`
- Answer URL aggiornato in Vonage dashboard con URL ngrok
- Webhook logs in backend console
- Firewall/network non blocca Vonage IP

## Advanced: Moderated Conference

Il pattern può essere esteso per conference calls moderate:

```javascript
// Moderator (operator) NCCO
{
  "action": "conversation",
  "name": "conference-room",
  "startOnEnter": true,
  "endOnExit": true  // conference ends when moderator hangs up
}

// Attendees (customers) NCCO
{
  "action": "conversation",
  "name": "conference-room",
  "startOnEnter": false,
  "musicOnHoldUrl": ["..."]
}
```

Quando moderator entra, tutti gli attendees in attesa vengono connessi.

## References

- **Vonage Voice API NCCO**: `docs/vonage/voice/api/en/doc3.md`
- **Conversation Action**: startOnEnter, musicOnHoldUrl, endOnExit parameters
- **Client SDK serverCall**: `docs/vonage/voice/client_sdk/it/doc6.md`
- **Vonage AI Response**: Progressive dialer pattern confirmation

## Key Takeaways

✅ **Operator-first è possibile** con endpoint WebRTC usando `serverCall()`
✅ **startOnEnter: false** permette hold music senza iniziare conversation
✅ **Answer webhook** controlla il flusso e triggera customer call
✅ **Zero wait time** per il cliente - operatore è già pronto
✅ **Pattern standard** per contact center professionali
