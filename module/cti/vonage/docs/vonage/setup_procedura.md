# Setup Vonage — Procedura completa con CLI

Questa guida descrive i passi necessari per configurare una Vonage Application
e effettuare la prima chiamata, usando esclusivamente la **Vonage CLI**.

---

## Prerequisiti

- Node.js e npm installati
- Account Vonage creato su [dashboard.nexmo.com](https://dashboard.nexmo.com)
- API Key e API Secret: disponibili nel dashboard dopo il login, nella sezione **"API Settings"**
  - Vai su [dashboard.nexmo.com](https://dashboard.nexmo.com)
  - Trovi le credenziali nella sezione "API Settings" o nella home del dashboard

---

## 1. Installare Vonage CLI

### Con npm

```bash
npm install -g @vonage/cli
```

### Con yarn

```bash
yarn global add @vonage/cli
```

### Verifica installazione

```bash
vonage --version
```

**Riferimento locale:** `voice/cli/doc1.md` (Installation)

---

## 2. Configurare le credenziali

### Recupera le credenziali dal Dashboard

1. Vai su [dashboard.nexmo.com](https://dashboard.nexmo.com)
2. Dopo il login, trovi **API Key** e **API Secret** nella sezione **"API Settings"**
   (o direttamente nella home del dashboard)
3. Copia entrambi i valori

### Configura la CLI

```bash
vonage auth set \
  --apiKey='YOUR_API_KEY' \
  --apiSecret='YOUR_API_SECRET'
```

Sostituisci `YOUR_API_KEY` e `YOUR_API_SECRET` con i valori copiati dal dashboard.

**Output:**

```
API Key: YOUR_API_KEY
API Secret: YOUR-**************

✅ Checking API Key Secret
```

**Nota:** Le credenziali vengono salvate nel file globale `$HOME/.vonage/config.json`.
Puoi anche creare un file locale `.vonagerc` nella directory del progetto per
configurazioni specifiche per progetto.

### Verifica configurazione

```bash
vonage auth check
```

**Output:**

```
Global credentials found at: /Users/yourname/.vonage/config.json

API Key: YOUR_API_KEY
API Secret: YOUR-**************

✅ Checking API Key Secret
```

**Riferimento locale:** `voice/cli/doc1.md` (Authentication)

---

## 3. Creare l'applicazione Vonage

```bash
vonage apps create 'CRM Voice Application'
```

**Output:**

```
✅ Creating Application
Saving private key ... Done!
Application created

Name: CRM Voice Application
Application ID: 00000000-0000-0000-0000-000000000000
Private/Public Key: Set

Capabilities:
  None Enabled
```

**Importante:**
- Annota l'**Application ID** (serve in `application.properties`)
- Il file `private.key` viene salvato automaticamente nella directory corrente
- Questo file è necessario per generare i JWT

### Aggiorna configurazione CLI con app ID e private key

Dopo aver creato l'applicazione, aggiorna la configurazione CLI fornendo tutti i parametri:

```bash
vonage auth set \
  --apiKey='YOUR_API_KEY' \
  --apiSecret='YOUR_API_SECRET' \
  --appId='00000000-0000-0000-0000-000000000000' \
  --privateKey=./private.key
```

Sostituisci:
- `YOUR_API_KEY` e `YOUR_API_SECRET` con le credenziali configurate al passo 2
- `00000000-0000-0000-0000-000000000000` con l'Application ID ottenuto dal comando precedente

**Output:**

```
API Key: YOUR_API_KEY
API Secret: YOUR-**************
App ID: 00000000-0000-0000-0000-000000000000
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

**Riferimento locale:** `voice/client_sdk/en/doc1.md` (Create a Vonage Application), `voice/cli/doc1.md` (Set Authentication)

---

## 4. Configurare la capability Voice

Questo comando abilita le chiamate vocali PSTN (operatorType: "phone").

### Setup minimo (per la nostra app)

La nostra implementazione usa **NCCO inline**, quindi serve solo l'Event URL:

```bash
vonage apps capabilities update <APPLICATION_ID> voice \
  --voice-event-url='https://crm.example.com/api/voice/webhook/event'
```

Sostituisci:
- `<APPLICATION_ID>` con l'ID del passo 3
- `https://crm.example.com` con il dominio pubblico della tua app

**Output:**

```
✅ Adding voice capability to application <APPLICATION_ID>

Capabilities:
 VOICE:
    Event URL: [POST] https://crm.example.com/api/voice/webhook/event
```

### Setup completo (opzionale, se servono chiamate in ingresso)

Se vuoi gestire chiamate **in ingresso** verso un numero Vonage, configura
anche Answer URL e Fallback URL:

```bash
vonage apps capabilities update <APPLICATION_ID> voice \
  --voice-answer-url='https://crm.example.com/api/voice/webhook/answer' \
  --voice-event-url='https://crm.example.com/api/voice/webhook/event' \
  --voice-fallback-url='https://crm.example.com/api/voice/webhook/fallback'
```

**Nota:** La nostra app attualmente non implementa l'Answer URL perché genera
chiamate **outbound** con NCCO inline.

**Riferimento locale:**
- `voice_webhooks.md` (Answer URL, Event URL, Fallback URL)
- `voice/api/en/doc1.md` (Voice Capability Configuration)

---

## 5. Configurare la capability RTC (per WebRTC)

Questo comando abilita le chiamate WebRTC (operatorType: "app") nel browser.

**Importante**: Questo step è **aggiuntivo** al precedente, non alternativo.
Per supportare sia chiamate PSTN che WebRTC, esegui sia lo step 4 che lo step 5.

```bash
vonage apps capabilities update <APPLICATION_ID> rtc \
  --rtc-event-url='https://crm.example.com/api/voice/webhook/event'
```

**Output:**

```
✅ Adding rtc capability to application <APPLICATION_ID>

Capabilities:
 VOICE: ...
 RTC:
    Event URL: [POST] https://crm.example.com/api/voice/webhook/event
```

**Riferimento locale:**
- `voice/voice_flow.md` (WebRTC nel browser)
- `voice/client_sdk/en/doc1.md` (RTC Capability)

### Riepilogo capabilities

Quale combinazione serve per il tuo scenario:

| Scenario | voice (step 4) | rtc (step 5) |
|----------|----------------|--------------|
| Solo chiamate PSTN (operatorType: "phone") | ✅ Obbligatorio | ❌ Non serve |
| Solo chiamate WebRTC (operatorType: "app") | ✅ Obbligatorio | ✅ Obbligatorio |
| Entrambi (PSTN + WebRTC) | ✅ Obbligatorio | ✅ Obbligatorio |

**Nota**: La capability voice è sempre necessaria. La capability rtc si aggiunge
solo se vuoi supportare operatori WebRTC nel browser.

---

## 6. Creare utenti per operatori WebRTC

Ogni operatore che riceverà chiamate via WebRTC deve essere registrato come
**user** su Vonage. Il campo `name` diventa il claim `sub` nel JWT SDK.

```bash
vonage users create --name='operatore_01'
```

**Output:**

```
✅ Creating User

User ID: USR-00000000-0000-0000-0000-000000000000
Name: operatore_01
```

Ripeti per ogni operatore:

```bash
vonage users create --name='operatore_02'
vonage users create --name='operatore_03'
```

**Importante:**
- Il `name` deve corrispondere esattamente al valore passato in
  `POST /api/voice/calls` quando `operatorType` è `"app"`.
- Esempio: se chiami con `{ operatorId: "operatore_01" }`, l'utente deve esistere
  con quel nome esatto.

**Riferimento locale:**
- `voice/voice_flow.md` (WebRTC nel browser - User registration)
- `voice/client_sdk/en/doc1.md` (Create a User)
- `voice/client_sdk/en/doc6.md` (Backend Server - createUser API)

---

## 7. (Opzionale) Acquistare un numero Vonage

Se vuoi ricevere chiamate **in ingresso** su un numero Vonage, devi acquistarne uno.

### Cercare numeri disponibili

```bash
vonage numbers search IT
```

**Output:**

```
✅ Searching for numbers

Number         Type      Features      Monthly Cost  Setup Cost
-------------  --------  ------------  ------------  ----------
+39021234567   Landline  VOICE         €1.00         €0.00
```

### Acquistare il numero

```bash
vonage numbers buy IT +39021234567
```

Conferma con `y` quando richiesto.

**Output:**

```
✅ Purchasing number
Number +39021234567 purchased

Monthly Cost: €1.00
Setup Cost: €0.00
Linked Application ID: Not linked to any application
```

### Collegare il numero all'applicazione

Attualmente la Vonage CLI non supporta il linking diretto. Devi collegare il
numero all'applicazione tramite il [Dashboard Vonage](https://dashboard.nexmo.com):

1. Vai a **Numbers** → **Your numbers**
2. Trova il numero acquistato
3. Clicca su **Edit**
4. Nella sezione **Voice**, seleziona **Forward to Application**
5. Scegli l'applicazione creata al passo 3
6. Salva

**Nota:** Questo passo serve solo se vuoi gestire chiamate in ingresso. Per le
chiamate **outbound** (il nostro caso d'uso principale) non è necessario.

**Riferimento locale:** `voice/api/en/doc1.md` (Phone Number Management)

---

## 8. Configurare l'app CRM

Copia i valori ottenuti nel file `application.properties`:

```properties
# Application ID dal passo 3
voice.applicationId=00000000-0000-0000-0000-000000000000

# Path al file private.key salvato al passo 3
voice.privateKey=file:///path/to/private.key

# Oppure incolla il contenuto completo della chiave:
# voice.privateKey=-----BEGIN PRIVATE KEY-----\nMIIE...==\n-----END PRIVATE KEY-----

# Numero Vonage mittente (se acquistato al passo 7)
voice.fromNumber=+39021234567

# URL pubblico per ricevere eventi (deve corrispondere al passo 4)
voice.eventUrl=https://crm.example.com/api/voice/webhook/event
```

**Riferimento locale:**
- `voice/voice_flow.md` (Configurazione section)
- `src/main/resources/application.properties` (implementation file)

---

## 9. Verificare la configurazione

### Verifica configurazione CLI

```bash
vonage auth show
```

**Output atteso:**

```
Global credentials found at: /Users/yourname/.vonage/config.json

API Key: YOUR_API_KEY
API Secret: YOUR-**************
App ID: 00000000-0000-0000-0000-000000000000
Private Key: Is Set

✅ Checking API Key Secret
✅ Checking App ID and Private Key
```

### Verifica file locali (opzionale)

Alcune versioni precedenti della CLI creano un file `vonage_app.json` locale:

```bash
cat vonage_app.json
```

**Output (se presente):**

```json
{
  "application_id": "00000000-0000-0000-0000-000000000000",
  "private_key": "./private.key"
}
```

**Riferimento locale:** `voice/cli/doc1.md` (Show Authentication), `voice/client_sdk/en/doc7.md` (Troubleshooting - Vonage Application setup)

---

## 10. Test della prima chiamata

### Test con endpoint backend

Avvia l'app CRM e prova il test call:

```bash
curl -X POST http://localhost:8080/api/voice/test \
  -H "Content-Type: application/json"
```

Questo dovrebbe:
1. Generare un JWT dal `private.key`
2. Chiamare `POST /v1/calls` su Vonage
3. Vonage chiama il `testNumber` e riproduce un messaggio TTS
4. Gli eventi vengono inviati a `/api/voice/webhook/event`

### Test con chiamata reale

```bash
curl -X POST http://localhost:8080/api/voice/calls \
  -H "Content-Type: application/json" \
  -d '{
    "toNumber": "+39XXXXXXXXXX",
    "operatorType": "phone",
    "operatorId": "+39YYYYYYYYYY"
  }'
```

### Test con operatore WebRTC

```bash
curl -X POST http://localhost:8080/api/voice/calls \
  -H "Content-Type: application/json" \
  -d '{
    "toNumber": "+39XXXXXXXXXX",
    "operatorType": "app",
    "operatorId": "operatore_01"
  }'
```

**Prerequisito:** L'operatore deve avere una sessione Client SDK attiva nel browser.

**Riferimento locale:**
- `voice/voice_flow.md` (Flusso 1 — Chiamata di Test, Flusso 2 — Chiamata Reale)
- `voice/outbound_call_ncco.md` (Making an outbound call with NCCO)
- `gui/src/module/voice/SdkSessionComponent.svelte` (WebRTC component implementation)

---

## 11. Sviluppo locale con Prism

In ambiente di sviluppo, l'app usa **Prism** come mock server invece di Vonage:

```properties
# application-dev.properties
voice.baseUrl=http://crm-prism:4010
voice.token=mock-jwt-token
```

Prism accetta le stesse richieste ma **non esegue** chiamate reali né invia
webhook. Per testare gli eventi:

```bash
curl -X POST http://localhost:8080/api/voice/webhook/event \
  -H "Content-Type: application/json" \
  -d '{
    "uuid": "test-uuid-123",
    "status": "answered",
    "from": "+39XXXXXXXXXX",
    "to": "+39YYYYYYYYYY",
    "timestamp": "2026-02-06T12:00:00.000Z"
  }'
```

**Riferimento locale:**
- `voice/voice_flow.md` (Dev: Prism mock server)
- `voice/prism.md` (Prism configuration and usage)

---

## Troubleshooting

### JWT invalido

Verifica che:
- `application_id` in `application.properties` corrisponda all'app creata
- `private.key` sia il file generato al passo 3
- Il formato della chiave sia corretto (PEM con `-----BEGIN PRIVATE KEY-----`)

**Riferimento locale:** `voice/client_sdk/en/doc7.md` (Troubleshooting - JWT, Session, Push)

### Webhook non ricevuto

Verifica che:
- L'URL configurato al passo 4 sia **pubblico** (usa ngrok in sviluppo)
- Il server sia raggiungibile da Vonage
- L'endpoint ritorni `200 OK`

### Utente non trovato (WebRTC)

Verifica che:
- L'utente sia stato creato al passo 6 con il nome esatto
- Il claim `sub` nel JWT corrisponda al nome utente
- L'applicazione abbia la capability RTC abilitata (passo 5)

---

## Comandi utili

```bash
# Autenticazione
vonage auth show                    # Mostra configurazione corrente
vonage auth check                   # Verifica che le credenziali siano valide
vonage auth check --local          # Verifica configurazione locale (.vonagerc)

# Lista applicazioni
vonage apps list

# Mostra dettagli applicazione
vonage apps show <APPLICATION_ID>

# Lista utenti
vonage users list

# Lista numeri acquistati
vonage numbers list

# Genera JWT di test (CLI)
vonage jwt create \
  --appId='<APPLICATION_ID>' \
  --privateKey=./private.key \
  --sub='operatore_01' \
  --acl='{"paths":{"/*/users/**":{},"/*/conversations/**":{},"/*/sessions/**":{},"/*/devices/**":{},"/*/push/**":{},"/*/knocking/**":{},"/*/legs/**":{}}}'
```

---

## Riferimenti

### Documentazione locale

- **CLI**: `voice/cli/doc1.md` (Getting Started, Authentication, Commands)
- Flusso implementazione: `voice/voice_flow.md`
- Webhook URLs: `voice_webhooks.md`
- NCCO reference: `voice/ncco.md` (se presente) o `voice/outbound_call_ncco.md`
- Voice API reference: `voice/api/en/doc1.md`, `voice/api/en/doc2.md`, `voice/api/en/doc3.md`
- Client SDK setup: `voice/client_sdk/en/doc1.md` (Create Application & Users)
- Client SDK usage: `voice/client_sdk/en/doc2.md` (Overview)
- Session management: `voice/client_sdk/en/doc4.md` (Sessions)
- Backend JWT/User: `voice/client_sdk/en/doc6.md` (Backend Server)
- Troubleshooting: `voice/client_sdk/en/doc7.md` (Troubleshooting)
- SDK migration: `voice/client_sdk/en/doc8.md` (Nexmo → Vonage)
- Prism mock: `voice/prism.md`

### Documentazione esterna

- Vonage CLI: [developer.vonage.com/en/getting-started/tools/cli](https://developer.vonage.com/en/getting-started/tools/cli)
- Vonage Dashboard: [dashboard.nexmo.com](https://dashboard.nexmo.com)
