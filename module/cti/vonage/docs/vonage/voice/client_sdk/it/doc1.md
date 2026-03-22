# Crea la tua Applicazione, Utenti e Token

Per utilizzare il Client SDK, ci sono tre cose che devi configurare prima di iniziare:

*   [Applicazione Vonage](https://developer.vonage.com/en/application/overview) - un'Applicazione che contiene la configurazione per l'app che stai costruendo.
*   [Utenti](https://developer.vonage.com/en/conversation/concepts/user) - Utenti associati all'Applicazione Vonage. Si prevede che gli Utenti avranno una mappatura uno-a-uno con il tuo sistema di autenticazione.
*   [JSON Web Tokens, JWT](https://jwt.io/) - Il Client SDK utilizza JWT per l'autenticazione. Affinché un utente possa accedere e utilizzare le funzionalità dell'SDK, devi fornire un JWT per utente. I JWT contengono tutte le informazioni di cui la piattaforma Vonage ha bisogno per autenticare le richieste, oltre a informazioni come le Applicazioni associate, gli Utenti e i permessi.

Tutti questi elementi possono essere [creati dal tuo backend](https://developer.vonage.com/en/vonage-client-sdk/backend). Se desideri iniziare subito e sperimentare l'uso dell'SDK, puoi anche distribuire un'applicazione backend da utilizzare con il Client SDK utilizzando il [modello Client SDK Sample Server](https://developer.vonage.com/en/cloud-runtime/6b041470-0b95-4d90-bbd7-ac18946edd6a_client-sdk-voice-sample-server). In alternativa, questo tutorial ti mostrerà come farlo, utilizzando la [Vonage CLI](https://github.com/vonage/vonage-cli).

## Prerequisiti

Assicurati di avere quanto segue:

*   Un account Vonage - [registrati](https://ui.idp.vonage.com/ui/auth/registration?icid=tryitfree_adpdocs_nexmodashbdfreetrialsignup_inpagelink)
*   [Node.JS](https://nodejs.org/en/download/) e NPM installati
*   Installa la [Vonage CLI](https://developer.vonage.com/en/getting-started/tools/cli).

## Crea un'Applicazione Vonage

Ora devi creare un'applicazione Vonage. In questo esempio crei un'applicazione in grado di gestire un caso d'uso Voice (voce) all'interno dell'app.

1.  Innanzitutto crea la directory del tuo progetto, se non l'hai già fatto.
2.  Entra nella directory del progetto che hai appena creato.
3.  Utilizza i seguenti comandi per creare e configurare un'applicazione Vonage con capacità Voice e WebRTC. Sostituisci gli URL dei webhook con i tuoi. Se la tua piattaforma limita il traffico in entrata che può ricevere utilizzando intervalli di indirizzi IP, dovrai aggiungere gli [indirizzi IP Vonage](https://api.support.vonage.com/hc/en-us/articles/360035471331) alla tua lista consentita. Gli indirizzi IP possono essere recuperati in modo programmatico inviando una richiesta GET a `https://api.nexmo.com/ips-v4`.

### Powershell (Windows)

```powershell
vonage apps create 'Nome tua applicazione'

✅ Creazione Applicazione
Salvataggio chiave privata ... Fatto!
Applicazione creata

Nome: Nome tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Migliora AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  Nessuna abilitata
```

### CMD (Windows)

```cmd
vonage apps create 'Nome tua applicazione'

✅ Creazione Applicazione
Salvataggio chiave privata ... Fatto!
Applicazione creata

Nome: Nome tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Migliora AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  Nessuna abilitata
```

### Bash

```bash
$ vonage apps create 'Nome tua applicazione'
$ ✅ Creazione Applicazione
$ Salvataggio chiave privata ... Fatto!
$ Applicazione creata
$ Nome: Nome tua applicazione
$ ID Applicazione: 00000000-0000-0000-0000-000000000000
$ Migliora AI: Disattivato
$ Chiave Privata/Pubblica: Impostata
$ Capacità:
$ Nessuna abilitata
```

Aggiungi Capacità Voice:

### Powershell (Windows)

```powershell
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice `
  --voice-answer-url='https://example.com/webhooks/voice/answer' `
  --voice-event-url='https://example.com/webhooks/voice/event' `
  --voice-fallback-url='https://example.com/webhooks/voice/fallback'
  
✅ Recupero Applicazione
✅ Aggiunta capacità voice all'applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Migliora AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
 VOICE:
    Utilizza Callback Firma: Attivato
    Durata Conversazione: 41 ore
    Tempo Persistenza Linea: 6 giorni
    URL Evento: [POST] https://example.com/webhooks/voice/event
    URL Risposta: [POST] https://example.com/webhooks/voice/answer
    URL Fallback: [POST] https://example.com/webhooks/voice/fallback
```

### CMD (Windows)

```cmd
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice ^
  --voice-answer-url='https://example.com/webhooks/voice/answer' ^
  --voice-event-url='https://example.com/webhooks/voice/event' ^
  --voice-fallback-url='https://example.com/webhooks/voice/fallback'
  
✅ Recupero Applicazione
✅ Aggiunta capacità voice all'applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Migliora AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
 VOICE:
    Utilizza Callback Firma: Attivato
    Durata Conversazione: 41 ore
    Tempo Persistenza Linea: 6 giorni
    URL Evento: [POST] https://example.com/webhooks/voice/event
    URL Risposta: [POST] https://example.com/webhooks/voice/answer
    URL Fallback: [POST] https://example.com/webhooks/voice/fallback
```

### Bash

```bash
$ vonage apps capabilities update 00000000-0000-0000-0000-000000000000 voice \
--voice-answer-url='https://example.com/webhooks/voice/answer' \
--voice-event-url='https://example.com/webhooks/voice/event' \
--voice-fallback-url='https://example.com/webhooks/voice/fallback'
$ ✅ Recupero Applicazione
$ ✅ Aggiunta capacità voice all'applicazione 00000000-0000-0000-0000-000000000000
$ Nome: Nome tua applicazione
$ ID Applicazione: 00000000-0000-0000-0000-000000000000
$ Migliora AI: Disattivato
$ Chiave Privata/Pubblica: Impostata
$ Capacità:
$ VOICE:
$    Utilizza Callback Firma: Attivato
$    Durata Conversazione: 41 ore
$    Tempo Persistenza Linea: 6 giorni
$    URL Evento: [POST] https://example.com/webhooks/voice/event
$    URL Risposta: [POST] https://example.com/webhooks/voice/answer
$    URL Fallback: [POST] https://example.com/webhooks/voice/fallback
```

Aggiungi Capacità RTC (Real-Time Communication)

### Powershell (Windows)

```powershell
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 messages `
  --messages-inbound-url='https://example.com/messages/inboud' `
  --messages-status-url='https://example.com/messages/status' `
  --messages-version='v1' `
  --no-messages-authenticate-media
  
✅ Recupero Applicazione
✅ Aggiunta capacità messages all'applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Migliora AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  MESSAGES:
    Autentica Media in Entrata: Disattivato
    Versione Webhook: v1
    URL Stato: [POST] https://example.com/messages/status
    URL in Entrata: [POST] https://example.com/messages/inboud
```

### CMD (Windows)

```cmd
vonage apps capabilities update 00000000-0000-0000-0000-000000000000 messages ^
  --messages-inbound-url='https://example.com/messages/inboud' ^
  --messages-status-url='https://example.com/messages/status' ^
  --messages-version='v1' ^
  --no-messages-authenticate-media
  
✅ Recupero Applicazione
✅ Aggiunta capacità messages all'applicazione 00000000-0000-0000-0000-000000000000

Nome: Nome tua applicazione
ID Applicazione: 00000000-0000-0000-0000-000000000000
Migliora AI: Disattivato
Chiave Privata/Pubblica: Impostata

Capacità:
  MESSAGES:
    Autentica Media in Entrata: Disattivato
    Versione Webhook: v1
    URL Stato: [POST] https://example.com/messages/status
    URL in Entrata: [POST] https://example.com/messages/inboud
```

### Bash

```bash
$ vonage apps capabilities update 00000000-0000-0000-0000-000000000000 rtc \
--rtc-event-url='https://example.com/webhooks/rtc/fallback'
$ ✅ Recupero Applicazione
$ ✅ Aggiunta capacità rtc all'applicazione 00000000-0000-0000-0000-000000000000
$ Nome: Nome tua applicazione
$ ID Applicazione: 00000000-0000-0000-0000-000000000000
$ Migliora AI: Disattivato
$ Chiave Privata/Pubblica: Impostata
$ Capacità:
$ RTC:
$    URL Evento: [POST] https://example.com/webhooks/voice/rtc
$    Utilizza Callback Firma: Disattivato
```

L'applicazione viene quindi creata. Viene creato anche un file di chiave privata `private.key`.

La creazione di un'applicazione e delle sue capacità è trattata in dettaglio nella [guida alle applicazioni](https://developer.vonage.com/en/application/overview).

## Crea un Utente

Crea un Utente che accederà al Client Vonage e parteciperà alle funzionalità dell'SDK: Conversazioni, Chiamate e così via.

Esegui il seguente comando nel tuo terminale per creare un utente di nome Alice:

### Powershell (Windows)

```powershell
vonage users create `
  --name='Alice'
  
✅ Creazione Utente

ID Utente: USR-00000000-0000-0000-0000-000000000000
Nome: Alice
Nome Visualizzato: Non Impostato
URL Immagine: Non Impostato
Tempo di Vita: Non Impostato

Canali:
  Nessuno Impostato
```

### CMD (Windows)

```cmd
vonage users create ^
  --name='Alice'
  
✅ Creazione Utente

ID Utente: USR-00000000-0000-0000-0000-000000000000
Nome: Alice
Nome Visualizzato: Non Impostato
URL Immagine: Non Impostato
Tempo di Vita: Non Impostato

Canali:
  Nessuno Impostato
```

### Bash

```bash
$ vonage users create \
--name='Alice'
$ ✅ Creazione Utente
$ ID Utente: USR-00000000-0000-0000-0000-000000000000
$ Nome: Alice
$ Nome Visualizzato: Non Impostato
$ URL Immagine: Non Impostato
$ Tempo di Vita: Non Impostato
$ Canali:
$ Nessuno Impostato
```

L'ID utente viene utilizzato dall'SDK per eseguire operazioni come l'accesso, l'inizio di una chiamata e altro.

## Genera un JWT per l'Utente

I [JWT](https://jwt.io) vengono utilizzati per autenticare un utente nel Client SDK.

Per generare un JWT per Alice esegui il seguente comando (sostituendo con le tue informazioni):

### Powershell (Windows)

```powershell
# Un comando con parametri
vonage jwt create `
--app-id='00000000-0000-0000-0000-000000000000' `
--private-key=./private.key `
--sub='Alice' `
--acl='{\"paths\":{\"\/*\/users\/**\":{},\"\/*\/conversations\/**\":{},\"\/*\/sessions\/**\":{},\"\/*\/devices\/**\":{},\"\/*\/push\/**\":{},\"\/*\/knocking\/**\":{},\"\/*\/legs\/**\":{}}}'

# Produrrà un token
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2wiOnsicGF0aHMiOnsiLyovcnRjLyoqIjp7fSwiLyovdXNlcnMvKioiOnt9LCIvKi9jb252ZXJzYXRpb25zLyoqIjp7fSwiLyovc2Vzc2lvbnMvKioiOnt9LCIvKi9kZXZpY2VzLyoqIjp7fSwiLyovcHVzaC8qKiI6e30sIi8qL2tub2NraW5nLyoqIjp7fSwiLyovbGVncy8qKiI6e319fSwiZXhwIjoxNzQxMTgyMzA3LCJzdWIiOiJBbGljZSIsImp0aSI6Ijg1MTViNzk2LTA1YjktNGFkMS04MTRkLTE1NWZjZTQzZWM1YiIsImlhdCI6MTc0MTE4MTQwNywiYXBwbGljYXRpb25faWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAifQ.BscMdDXZ1-nuLtKyPJvw9tE8E8ZjJvTPJPMT9y0TjPz4Q7qqNaqxcjglc5QPtYEjh2YpZH6btSKbUF4XTClI026Hl5_QOBlnayYo7jXwhba16fa5PeyzSf30QFGFrHbANwrQJFVCjd329SZUpwK4GxgB1gf230NhbfmkhegKezqicru2WTGCKm8kQncYliFwIEYUlcRAb2c8xcaVrn_6QNNahyeJRwGFfWpIkX0Oe-S4RDlPjoq47_gYWac9MmaetB4Dd3Yp531AuniGV5JiIShkaEwuY4Zyov4Hcmajm4Lm_UFY119la7vzHis0P7cT9pPUDe5cyPj7eT8-VhitfQ
```

### CMD (Windows)

```cmd
REM Un comando con parametri
vonage jwt create ^
--app-id='00000000-0000-0000-0000-000000000000' ^
--private-key=./private.key ^
--sub='Alice' ^
--acl="{\"paths\":{\"\/*\/users\/**\":{},\"\/*\/conversations\/**\":{},\"\/*\/sessions\/**\":{},\"\/*\/devices\/**\":{},\"\/*\/push\/**\":{},\"\/*\/knocking\/**\":{},\"\/*\/legs\/**\":{}}}"

REM Produrrà un token
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2wiOnsicGF0aHMiOnsiLyovcnRjLyoqIjp7fSwiLyovdXNlcnMvKioiOnt9LCIvKi9jb252ZXJzYXRpb25zLyoqIjp7fSwiLyovc2Vzc2lvbnMvKioiOnt9LCIvKi9kZXZpY2VzLyoqIjp7fSwiLyovcHVzaC8qKiI6e30sIi8qL2tub2NraW5nLyoqIjp7fSwiLyovbGVncy8qKiI6e319fSwiZXhwIjoxNzQxMTgyMzA3LCJzdWIiOiJBbGljZSIsImp0aSI6Ijg1MTViNzk2LTA1YjktNGFkMS04MTRkLTE1NWZjZTQzZWM1YiIsImlhdCI6MTc0MTE4MTQwNywiYXBwbGljYXRpb25faWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAifQ.BscMdDXZ1-nuLtKyPJvw9tE8E8ZjJvTPJPMT9y0TjPz4Q7qqNaqxcjglc5QPtYEjh2YpZH6btSKbUF4XTClI026Hl5_QOBlnayYo7jXwhba16fa5PeyzSf30QFGFrHbANwrQJFVCjd329SZUpwK4GxgB1gf230NhbfmkhegKezqicru2WTGCKm8kQncYliFwIEYUlcRAb2c8xcaVrn_6QNNahyeJRwGFfWpIkX0Oe-S4RDlPjoq47_gYWac9MmaetB4Dd3Yp531AuniGV5JiIShkaEwuY4Zyov4Hcmajm4Lm_UFY119la7vzHis0P7cT9pPUDe5cyPj7eT8-VhitfQ
```

### Bash

```bash
# Un comando con parametri
$ vonage jwt create \
--app-id='00000000-0000-0000-0000-000000000000' \
--private-key=./private.key \
--sub='Alice' \
--acl='{"paths":{"/*/rtc/**":{},"/*/users/**":{},"/*/conversations/**":{},"/*/sessions/**":{},"/*/devices/**":{},"/*/push/**":{},"/*/knocking/**":{},"/*/legs/**":{}}}'
# Produrrà un token
$ eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2wiOnsicGF0aHMiOnsiLyovcnRjLyoqIjp7fSwiLyovdXNlcnMvKioiOnt9LCIvKi9jb252ZXJzYXRpb25zLyoqIjp7fSwiLyovc2Vzc2lvbnMvKioiOnt9LCIvKi9kZXZpY2VzLyoqIjp7fSwiLyovcHVzaC8qKiI6e30sIi8qL2tub2NraW5nLyoqIjp7fSwiLyovbGVncy8qKiI6e319fSwiZXhwIjoxNzQxMTgyMzA3LCJzdWIiOiJBbGljZSIsImp0aSI6Ijg1MTViNzk2LTA1YjktNGFkMS04MTRkLTE1NWZjZTQzZWM1YiIsImlhdCI6MTc0MTE4MTQwNywiYXBwbGljYXRpb25faWQiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDAifQ.BscMdDXZ1-nuLtKyPJvw9tE8E8ZjJvTPJPMT9y0TjPz4Q7qqNaqxcjglc5QPtYEjh2YpZH6btSKbUF4XTClI026Hl5_QOBlnayYo7jXwhba16fa5PeyzSf30QFGFrHbANwrQJFVCjd329SZUpwK4GxgB1gf230NhbfmkhegKezqicru2WTGCKm8kQncYliFwIEYUlcRAb2c8xcaVrn_6QNNahyeJRwGFfWpIkX0Oe-S4RDlPjoq47_gYWac9MmaetB4Dd3Yp531AuniGV5JiIShkaEwuY4Zyov4Hcmajm4Lm_UFY119la7vzHis0P7cT9pPUDe5cyPj7eT8-VhitfQ
```

Il comando sopra indicato imposta la scadenza predefinita del JWT a 24 ore. Puoi modificare la scadenza in un periodo di tempo più breve utilizzando il flag `--exp`.

> NOTA: Nelle app di produzione, ci si aspetta che il tuo backend esponga un endpoint che genera un JWT per ogni richiesta del client.

## Ulteriori informazioni

*   [Maggiori informazioni su JWT e ACL](https://developer.vonage.com/en/getting-started/concepts/authentication#json-web-tokens)
*   [Tutorial Voice nell'app](https://developer.vonage.com/en/client-sdk/tutorials/app-to-phone/introduction)
*   [Tutorial Messaggistica nell'app](https://developer.vonage.com/en/client-sdk/tutorials/in-app-messaging/introduction)