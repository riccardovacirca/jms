# Modulo CTI — Vonage

Un sistema CTI (Computer Telephony Integration) integra la telefonia con l'applicazione software, consentendo di avviare, gestire e tracciare chiamate telefoniche direttamente dall'interfaccia applicativa. In questo modulo il CTI è implementato tramite le API vocali di Vonage e il Vonage Client SDK, che permette all'operatore di effettuare chiamate outbound verso clienti PSTN attraverso una connessione WebRTC nel browser.

---

## 1. Attori del sistema

### Operatore

Utente umano autenticato nell'applicazione. Utilizza il frontend CTI nel browser per avviare chiamate outbound. Lato Vonage è rappresentato da un **Vonage User** (identificato da un `name` univoco, es. `operatore_01`). Gli operatori vengono gestiti tramite l'interfaccia amministrativa della barra CTI (accessibile agli utenti con ruolo ADMIN o superiore): è possibile creare nuovi operatori Vonage direttamente dall'applicazione, oppure importare quelli già esistenti su Vonage tramite la funzione di sincronizzazione. A ogni sessione operativa viene assegnato dinamicamente un operatore libero dal pool tramite claim atomico. Interagisce con la piattaforma Vonage tramite il **Vonage Client SDK** usando un JWT RS256 emesso dal backend.

### Cliente

Utente esterno raggiunto tramite rete telefonica pubblica (PSTN). Non interagisce con l'applicazione: riceve una chiamata outbound sul proprio numero telefonico. Lato Vonage è una leg di tipo `phone`.

### Backend (applicazione Java)

Componente server che orchestra l'intero flusso CTI. Responsabile di:
- assegnare un operatore dal pool e generare il JWT SDK
- rispondere al webhook Vonage `answer` con l'NCCO operatore
- avviare la chiamata verso il cliente tramite Vonage Voice API
- gestire il hangup simultaneo di entrambe le legs
- ricevere e processare gli eventi Vonage (`answered`, `completed`, errori)
- persistere i record delle chiamate sul database con dati di billing
- gestire il CRUD degli operatori e la sincronizzazione con Vonage

### Frontend (browser)

Componente web eseguito nel browser dell'operatore. Utilizza il **Vonage Client SDK** (`@vonage/client-sdk`) per stabilire la connessione WebRTC con la piattaforma Vonage. Richiede il JWT SDK al backend, avvia la chiamata tramite `client.serverCall()` e gestisce gli eventi della chiamata. È implementato come web component Lit persistente (`<cti-bar>`) montato nel container `footer`, visibile su tutte le pagine. Si nasconde automaticamente quando l'utente non è autenticato.

### Vonage Platform

Sistema esterno che fornisce l'infrastruttura telefonica. Gestisce:
- le **Vonage Users** (identità WebRTC degli operatori)
- le **Conversations** (sessioni audio che raccolgono le legs)
- le **legs** (singole connessioni alla conversazione: una per l'operatore via WebRTC, una per il cliente via PSTN)
- i **webhook** verso il backend (`answer`, `event`)

### Database

Persiste il pool degli operatori (`jms_cti_operatori`) e lo storico delle chiamate (`jms_chiamate`). Gestisce l'assegnazione atomica degli operatori tramite `SELECT FOR UPDATE SKIP LOCKED` e traccia quale account ha in uso quale operatore tramite la colonna `sessione_account_id`.

---

## 2. Setup in ambiente di sviluppo

Dopo l'installazione del modulo CTI è necessario configurare l'ambiente di sviluppo per rendere il backend raggiungibile dalla piattaforma Vonage e registrare gli operatori.

### 2.1 Vonage CLI

La Vonage CLI è necessaria per creare l'applicazione Vonage e abilitare le capabilities. Va installata nel container di sviluppo:

```bash
npm install -g @vonage/cli
vonage auth set --apiKey='<API_KEY>' --apiSecret='<API_SECRET>'
```

Verifica:
```bash
vonage auth check
```

### 2.2 Vonage Client SDK

Il Client SDK è una dipendenza frontend usata dal browser dell'operatore per la connessione WebRTC. Va installata nella directory `gui/` del progetto:

```bash
cd /workspace/gui
npm install @vonage/client-sdk
```

### 2.3 ngrok

Vonage deve poter raggiungere i webhook del backend (`/api/cti/vonage/answer`, `/api/cti/vonage/event`) tramite URL pubblici. In sviluppo il backend è accessibile solo sulla rete locale, quindi è necessario esporre la porta tramite **ngrok**.

Installazione nel container:
```bash
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
  | tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
  && echo 'deb https://ngrok-agent.s3.amazonaws.com bookworm main' \
  | tee /etc/apt/sources.list.d/ngrok.list \
  && apt-get update -qq && apt-get install -y ngrok
```

Autenticazione (richiede account ngrok gratuito):
```bash
ngrok config add-authtoken <NGROK_TOKEN>
```

Avvio del tunnel sulla porta del backend:
```bash
ngrok http 8080
```

ngrok restituisce un URL pubblico del tipo `https://<id>.ngrok-free.app` che va configurato in `application.properties`:

```properties
cti.vonage.answer_url=https://<id>.ngrok-free.app/api/cti/vonage/answer
cti.vonage.event_url=https://<id>.ngrok-free.app/api/cti/vonage/event
```

Aggiornare anche i webhook nel Dashboard Vonage (capability Voice e RTC) con gli stessi URL.

> **Nota:** L'URL ngrok cambia a ogni riavvio del tunnel (piano gratuito). Ogni volta va aggiornato in `application.properties` e nella configurazione dell'applicazione Vonage.

### 2.4 Registrazione operatori

Gli operatori si gestiscono tramite l'interfaccia amministrativa: accedere con un account ADMIN, aprire il menu contestuale della barra CTI (icona ingranaggio) e selezionare **Lista operatori**.

**Creare un nuovo operatore direttamente dall'applicazione:**

Cliccare **Nuovo...** nel footer della modal. Compilare:
- **Nome utente Vonage** (obbligatorio): identificatore univoco su Vonage, es. `operatore_01`. Deve essere univoco nell'intera applicazione Vonage.
- **Nome visualizzato** (facoltativo): nome leggibile, es. `Mario Rossi`.

Il backend crea l'utente su Vonage e inserisce il record in `jms_cti_operatori` in un unico passaggio.

**Importare operatori già esistenti su Vonage:**

Se gli operatori sono stati creati in precedenza tramite CLI o Dashboard Vonage, cliccare **Sincronizza...** per importare automaticamente quelli non ancora presenti nel database locale. La sincronizzazione è unidirezionale (Vonage → locale) e non elimina né modifica operatori già presenti.

---

## 3. Flusso operativo

1. **Autenticazione CTI** — L'operatore apre il frontend CTI. Il browser richiede al backend un JWT SDK (`POST /api/cti/vonage/sdk/auth`). Il backend assegna dinamicamente un operatore libero dal pool e restituisce il token firmato RS256.

2. **Connessione WebRTC** — Il frontend usa il JWT SDK per inizializzare il Vonage Client SDK e stabilire una sessione WebRTC con la piattaforma Vonage. Da questo momento l'operatore è raggiungibile dalla piattaforma.

3. **Avvio chiamata** — L'operatore seleziona il numero del cliente e avvia la chiamata tramite `client.serverCall({ customerNumber })`. Il Client SDK trasmette la richiesta alla piattaforma Vonage.

4. **Webhook answer** — Vonage riceve la richiesta e chiama il backend sul webhook `POST /api/cti/vonage/answer`, passando l'identità dell'operatore (`from_user`), il suo UUID di chiamata (`uuid`) e il numero del cliente (`customerNumber` nel campo `custom_data`).

5. **NCCO operatore** — Il backend risponde immediatamente con un NCCO che fa entrare l'operatore in una nuova Conversation con `startOnEnter: false`. L'operatore sente la musica di attesa e la conversazione non è ancora attiva. Il record della chiamata viene inserito in `jms_chiamate` con stato `ringing`.

6. **Chiamata al cliente** — Con un ritardo di 1 secondo, il backend chiama il cliente tramite Vonage Voice API (`POST /v1/calls`), passando un NCCO che fa entrare il cliente nella stessa Conversation con `startOnEnter: true`.

7. **Conversazione attiva** — Il cliente risponde al telefono ed entra nella Conversation. Essendo il primo partecipante con `startOnEnter: true`, la conversazione si avvia: operatore e cliente si sentono in modo bidirezionale. Vonage invia l'evento `answered` al webhook event URL: il backend aggiorna `jms_chiamate` con `ora_inizio` e stato `answered`.

8. **Fine chiamata** — L'operatore termina la chiamata dal frontend (`PUT /api/cti/vonage/call/{uuid}/hangup`). Il backend invia `action: hangup` a entrambe le legs. Vonage invia l'evento `completed` con i dati di billing: il backend aggiorna `jms_chiamate` con `ora_fine`, `durata`, `tariffa`, `costo`, `rete` e stato `completed`.

9. **Rilascio operatore** — Al disconnect del componente frontend, il browser chiama `DELETE /api/cti/vonage/sdk/auth`. Il backend rilascia l'operatore riportando `sessione_account_id` a `NULL` e rendendolo disponibile per una nuova sessione.

---

## 4. Storico chiamate

Ogni chiamata viene tracciata nella tabella `jms_chiamate`. I campi principali:

| Campo | Descrizione |
|---|---|
| `uuid` | UUID Vonage della leg operatore |
| `conversazione_uuid` | Nome della Conversation Vonage |
| `stato` | `ringing` → `answered` → `completed` (o stati di errore) |
| `numero_destinatario` | Numero telefonico del cliente |
| `operatore_id` | FK verso `jms_cti_operatori` |
| `chiamante_account_id` | FK verso l'account applicativo che ha avviato la chiamata |
| `ora_inizio` | Timestamp risposta cliente (da evento `answered`) |
| `ora_fine` | Timestamp fine chiamata (da evento `completed`) |
| `durata` | Durata in secondi (da Vonage) |
| `tariffa` / `costo` | Dati di billing (da Vonage) |

Lo storico è accessibile dalla barra CTI (menu contestuale → **Storico chiamate**):
- Gli utenti con ruolo **ADMIN o superiore** vedono tutte le chiamate.
- Gli utenti con ruolo **USER** vedono solo le proprie chiamate (`chiamante_account_id`).

---

## 5. Gestione sessioni e cleanup

Il TTL della sessione operatore è di 30 minuti, rinnovato a ogni chiamata `claimOrRenew()`. Il frontend esegue un refresh automatico ogni 13 minuti per mantenere la sessione attiva.

Un job schedulato (`cti-session-cleanup`, ogni minuto) rilascia automaticamente le sessioni scadute nella tabella `jms_cti_operatori`, recuperando operatori da browser crashati o sessioni abbandonate entro 31 minuti massimi.

---

## 6. Accesso e autorizzazioni

La barra CTI è un componente **persistente** montato nel container `footer` all'avvio dell'applicazione. Si nasconde automaticamente quando l'utente non è autenticato, sottoscrivendosi allo store `authorized`.

| Funzione | Ruolo minimo |
|---|---|
| Connetti / avvia chiamata | USER |
| Storico chiamate (solo proprie) | USER |
| Storico chiamate (tutte) | ADMIN |
| Lista operatori | ADMIN |
| Crea / sincronizza operatori | ADMIN |
