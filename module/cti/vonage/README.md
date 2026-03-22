# Modulo CTI

Integrazione telefonica (Computer Telephony Integration) basata su
**Vonage Voice API** e **Vonage Client SDK**.
Implementa il pattern **operator-first progressive dialer**: l'operatore è già
in linea prima che il cliente risponda.

---

## Configurazione

`config/application.properties`

```properties
cti.api.key=change-me-in-production
cti.vonage.application-id=<APPLICATION_ID>
cti.vonage.private-key=/app/config/private.key
cti.vonage.from-number=<VONAGE_NUMBER>
cti.vonage.event-url=https://your-domain.com/api/cti
cti.vonage.music-on-hold-url=https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3
```

---

## Webhook da configurare nel Vonage Dashboard

| Tipo | URL |
|------|-----|
| **Answer URL** | `https://your-domain.com/api/cti/answer` |
| **Event URL** (Voice) | `https://your-domain.com/api/cti` |
| **Event URL** (RTC) | `https://your-domain.com/api/cti` |

L'Answer URL è obbligatoria: Vonage la chiama quando l'operatore avvia `serverCall()` dal browser.

---

## Come ottenere le credenziali Vonage

### Prerequisiti

```bash
npm install -g @vonage/cli
```

Credenziali API Key / API Secret disponibili su [dashboard.nexmo.com](https://dashboard.nexmo.com) → **API Settings**.

### Step 1 — Configura la CLI

```bash
vonage auth set --apiKey='<API_KEY>' --apiSecret='<API_SECRET>'
```

### Step 2 — Verifica

```bash
vonage auth check
```

### Step 3 — Crea l'applicazione Vonage

```bash
vonage apps create 'Hello CTI'
```

Salva l'**Application ID** dall'output. Il file `private.key` viene creato nella directory corrente —
copiarlo in `./config/private.key` del progetto.

### Step 4 — Aggiorna la CLI con app ID e private key

```bash
vonage auth set \
  --apiKey='<API_KEY>' \
  --apiSecret='<API_SECRET>' \
  --appId='<APPLICATION_ID>' \
  --privateKey=./private.key
```

### Step 5 — Abilita capability Voice (obbligatoria)

```bash
vonage apps capabilities update <APPLICATION_ID> voice \
  --voice-answer-url='https://your-domain.com/api/cti/answer' \
  --voice-event-url='https://your-domain.com/api/cti'
```

### Step 6 — Abilita capability RTC (per operatori WebRTC nel browser)

```bash
vonage apps capabilities update <APPLICATION_ID> rtc \
  --rtc-event-url='https://your-domain.com/api/cti'
```

### Step 7 — (Opzionale) Acquista un numero Vonage

Necessario solo se si usano operatori PSTN (`operatorType: "phone"`).

```bash
vonage numbers search IT
vonage numbers buy IT +39XXXXXXXXXX
```

Il collegamento numero ↔ applicazione va fatto dal Dashboard: **Numbers → Your numbers → Edit → Forward to Application**.

### Step 8 — Crea utenti per operatori WebRTC

Necessario solo per `operatorType: "app"`. Il `name` deve corrispondere all'`userId` passato al frontend.

```bash
vonage users create --name='operatore_01'
vonage users create --name='operatore_02'
```

---

## Sviluppo locale

In sviluppo Vonage deve raggiungere il backend tramite URL pubblico. Usare [ngrok](https://ngrok.com):

```bash
ngrok http 8080
# → copia l'URL https://xxxx.ngrok-free.app
```

Aggiornare `cti.vonage.event-url` e riconfigurare i webhook nel Dashboard con il nuovo URL ngrok.

---

## Dipendenza frontend

```bash
cd gui/
npm install @vonage/client-sdk
```

---

## Riferimenti

- Architettura e flusso chiamata: `docs/cti.md`
- Setup Vonage dettagliato: `docs/vonage/setup_procedura.md`
- Pattern operator-first: `docs/vonage/voice2_operator_first_pattern.md`
- Voice API reference: `docs/vonage/voice/api/`
- Client SDK reference: `docs/vonage/voice/client_sdk/`
