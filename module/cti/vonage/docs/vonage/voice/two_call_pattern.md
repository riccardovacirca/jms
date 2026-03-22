# Two-Call Pattern — Progressive Dialer

## Problema Risolto

**Flusso precedente (problematico):**
```
1. Sistema chiama cliente
2. Cliente risponde e attende
3. Vonage chiama operatore
4. Operatore risponde
5. Bridge attivo → cliente ha già aspettato
```

**Flusso attuale (ottimizzato):**
```
1. Sistema chiama operatore
2. Operatore risponde immediatamente
3. Sistema chiama cliente
4. Cliente risponde
5. Bridge attivo → nessuna attesa per il cliente
```

---

## Modifiche al Database

**Migration:** `V20260208_082122__voice_two_call_pattern.sql`

Campi aggiunti a `voice_calls`:
- `parent_call_id` INTEGER — FK alla chiamata parent (operator leg)
- `call_leg_type` VARCHAR(20) — "operator" o "customer"
- `customer_number` VARCHAR(20) — numero cliente (salvato nella operator leg)

**Relazione:**
```
voice_calls (operator leg)
  id: 1
  call_leg_type: "operator"
  customer_number: "+39XXXXXXXXXX"
  conversation_uuid: "CON-abc123"
       │
       └─► voice_calls (customer leg)
             id: 2
             parent_call_id: 1
             call_leg_type: "customer"
             conversation_uuid: "CON-abc123" (stesso!)
```

---

## Procedura di Chiamata — Step by Step

### Step 1: Frontend richiede chiamata

```javascript
POST /api/voice/calls
{
  "toNumber": "+39XXXXXXXXXX",      // numero cliente
  "operatorType": "app",             // o "phone"
  "operatorId": "operatore_01"       // user SDK o numero telefono
}
```

### Step 2: Backend chiama operatore

**VoiceService.createCall():**

```java
// 1. Genera conversation name univoco
conversationName = "call-<uuid>"

// 2. Costruisce NCCO conversation
ncco = [{
  "action": "conversation",
  "name": conversationName
}]

// 3. Chiama operatore
POST /v1/calls (Vonage)
{
  "to": [{ "type": "app", "user": "operatore_01" }],
  "from": { "type": "phone", "number": fromNumber },
  "ncco": [...]
}

// 4. Salva operator call nel DB
voice_calls:
  uuid: "CALL-op-123"
  call_leg_type: "operator"
  customer_number: "+39XXXXXXXXXX"  // salvato per step 4
  conversation_uuid: "CON-abc123"
  status: "started"
```

### Step 3: Operatore risponde

**Nel browser (SdkSessionComponent.svelte):**

```javascript
// Evento: callInvite
client.on('callInvite', (callId, from) => {
  // Auto-accept
  client.answer(callId)
  // Mostra UI "In chiamata con..."
  incomingCall = { callId, from }
})
```

**Vonage invia webhook:**

```
POST /api/voice/webhook/event
{
  "uuid": "CALL-op-123",
  "status": "answered",
  "conversation_uuid": "CON-abc123"
}
```

### Step 4: Backend chiama cliente

**VoiceService.handleWebhookEvent():**

```java
// 1. Aggiorna status operator call
updateStatus("CALL-op-123", "answered")

// 2. Verifica: status="answered" && call_leg_type="operator"
if (answered && operator) {

  // 3. Recupera customerNumber dalla operator call
  customerNumber = operatorCall.customerNumber

  // 4. Costruisce NCCO conversation (stesso name!)
  ncco = [{
    "action": "conversation",
    "name": conversationName  // stesso della operator call
  }]

  // 5. Chiama cliente
  POST /v1/calls (Vonage)
  {
    "to": [{ "type": "phone", "number": customerNumber }],
    "from": { "type": "phone", "number": fromNumber },
    "ncco": [...]
  }

  // 6. Salva customer call nel DB
  voice_calls:
    uuid: "CALL-cust-456"
    parent_call_id: 1  // ID della operator call
    call_leg_type: "customer"
    conversation_uuid: "CON-abc123"  // stesso!
    status: "started"
}
```

### Step 5: Cliente risponde

**Vonage invia webhook:**

```
POST /api/voice/webhook/event
{
  "uuid": "CALL-cust-456",
  "status": "answered",
  "conversation_uuid": "CON-abc123"
}
```

**Bridge automatico:**
```
Operatore (browser WebRTC) ◄──────► Vonage Conversation ◄──────► Cliente (PSTN)
                                    "CON-abc123"
```

### Step 6: Operatore riaggancia

**Nel browser:**

```javascript
// Click su "Riaggancia"
PUT /api/voice/calls/CALL-op-123/hangup
```

**Backend:**

```java
PUT /v1/calls/CALL-op-123 (Vonage)
{
  "action": "hangup"
}
```

**Vonage termina entrambe le legs:**
- Operator leg: hangup
- Customer leg: hangup (automatico quando conversation è vuota)

---

## Componenti Modificati

### Backend

**VoiceService.java:**
- `createCall()` — ora chiama operatore invece del cliente
- `submitOperatorCall()` — salva operator leg con customerNumber
- `handleWebhookEvent()` — trigger customer call quando operator answered
- `triggerCustomerCall()` — crea seconda chiamata con stesso conversation
- `hangupCall()` — PUT /v1/calls/{uuid} con action=hangup

**VoiceController.java:**
- `PUT /calls/{uuid}/hangup` — endpoint per riagganciare

**CallDto.java:**
- `parentCallId`, `callLegType`, `customerNumber`

**CallDao.java:**
- Insert/update con nuovi campi

### Frontend

**SdkSessionComponent.svelte:**
- Auto-accept mantenuto
- UI "In chiamata con..." + pulsante "Riaggancia"
- `hangupCall()` — chiama API backend

---

## Vantaggi del Pattern

1. **Zero attesa per il cliente** — viene chiamato solo quando operatore è pronto
2. **Tracciabilità completa** — due record in DB con relazione parent/child
3. **Debugging semplice** — ogni leg ha il suo uuid e eventi separati
4. **Gestione errori** — se cliente non risponde, operatore resta in conversation
5. **Standard del settore** — pattern usato da tutti i contact center professionali

---

## Testing

### Test completo

```bash
# 1. Avvia app
bin/cmd app run

# 2. Avvia GUI (altro terminale)
bin/cmd gui run

# 3. Apri browser su http://localhost:2350/voice
# Login operatore con userId: operatore_01

# 4. Avvia chiamata (da API o GUI)
curl -X POST http://localhost:8080/api/voice/calls \
  -H "Content-Type: application/json" \
  -d '{
    "toNumber": "+39XXXXXXXXXX",
    "operatorType": "app",
    "operatorId": "operatore_01"
  }'

# 5. Verifica sequenza:
# - Browser: riceve callInvite, auto-accept
# - Browser: mostra "In chiamata con..."
# - Sistema: chiama cliente (dopo 1-2 secondi)
# - Cliente: risponde
# - Bridge attivo

# 6. Verifica DB
bin/cmd db
SELECT id, uuid, call_leg_type, parent_call_id, customer_number, status
FROM voice_calls
ORDER BY created_at DESC
LIMIT 2;

# Output atteso:
# id | uuid          | call_leg_type | parent_call_id | customer_number | status
# 2  | CALL-cust-456 | customer      | 1              | NULL            | answered
# 1  | CALL-op-123   | operator      | NULL           | +39XXXXXXXXXX   | answered
```

### Test riagganciare

```bash
# Nel browser: click su "Riaggancia"
# Verifica webhook:
# - operator leg: status=completed
# - customer leg: status=completed
```

---

## Troubleshooting

### Cliente non viene chiamato

Verifica:
1. Webhook `answered` ricevuto per operator call
2. `call_leg_type` = "operator" nel DB
3. `customer_number` non null nella operator call
4. Logs backend per errori in `triggerCustomerCall()`

### Operatore non riceve chiamata

Verifica:
1. Sessione SDK attiva: `sessionActive = true` in SdkSessionComponent
2. User registrato su Vonage: `vonage users list`
3. Capability RTC abilitata: `vonage apps show <appId>`

### Bridge non funziona

Verifica:
1. Stesso `conversation_uuid` per entrambe le legs
2. Stesso `conversation name` in entrambi gli NCCO
3. Stato "answered" per entrambe le legs

---

## Riferimenti

- **Voice API**: `docs/vonage/voice/api/en/doc1.md`
- **NCCO conversation**: `docs/vonage/voice/api/en/doc3.md` (sezione Conversation)
- **Webhooks**: `docs/vonage/voice_webhooks.md`
- **Pattern originale**: `docs/vonage/voice/voice_flow.md` (flusso precedente)
