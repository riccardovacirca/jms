Modulo CTI - Integrazione Vonage Voice API

Implementa il pattern operator-first progressive dialer tramite Vonage Voice API e Vonage Client SDK.
L'operatore e gia in linea prima che il cliente risponda.


CONFIGURAZIONE

Aggiungere in config/application.properties:

    cti.vonage.application_id=<APPLICATION_ID>
    cti.vonage.private_key=/app/config/private.key
    cti.vonage.from_number=12345678901
    cti.vonage.answer_url=https://your-domain.com/api/cti/vonage/answer
    cti.vonage.event_url=https://your-domain.com/api/cti/vonage/event
    cti.vonage.music_on_hold_url=https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3

Copiare la chiave privata Vonage in /app/config/private.key.


DIPENDENZA FRONTEND

Installata automaticamente dallo script di post-install (install.sh) durante cmd module import.
Per reinstallare manualmente dalla cartella gui/:

    npm install @vonage/client-sdk


WEBHOOK NEL VONAGE DASHBOARD

Answer URL:  https://your-domain.com/api/cti/vonage/answer
Event URL:   https://your-domain.com/api/cti/vonage/event

Entrambi gli URL vanno configurati per Voice e RTC nel Dashboard Vonage.
L'Answer URL e obbligatoria: Vonage la chiama quando l'operatore avvia serverCall() dal browser.
L'Event URL riceve gli aggiornamenti di stato delle chiamate (answered, completed, errori).


CREDENZIALI VONAGE - PROCEDURA DI SETUP

Prerequisito: la Vonage CLI viene installata automaticamente dallo script di post-install.
Per reinstallarla manualmente:

    npm install -g @vonage/cli

Le credenziali API Key e API Secret sono disponibili su dashboard.nexmo.com alla sezione API Settings.

Passo 1 - Configura la CLI:

    vonage auth set --apiKey='<API_KEY>' --apiSecret='<API_SECRET>'

Passo 2 - Verifica:

    vonage auth check

Passo 3 - Crea l'applicazione Vonage:

    vonage apps create 'NomeApplicazione'

Salvare l'Application ID dall'output. Il file private.key viene creato nella directory corrente,
copiarlo in ./config/private.key del progetto.

Passo 4 - Aggiorna la CLI con app ID e private key:

    vonage auth set \
      --apiKey='<API_KEY>' \
      --apiSecret='<API_SECRET>' \
      --appId='<APPLICATION_ID>' \
      --privateKey=./private.key

Passo 5 - Abilita la capability Voice (obbligatoria):

    vonage apps capabilities update <APPLICATION_ID> voice \
      --voice-answer-url='https://your-domain.com/api/cti/vonage/answer' \
      --voice-event-url='https://your-domain.com/api/cti/vonage/event'

Passo 6 - Abilita la capability RTC (per operatori WebRTC nel browser):

    vonage apps capabilities update <APPLICATION_ID> rtc \
      --rtc-event-url='https://your-domain.com/api/cti/vonage/event'

Passo 7 - (Opzionale) Acquista un numero Vonage, necessario per chiamate outbound PSTN:

    vonage numbers search IT
    vonage numbers buy IT 12345678901

Il collegamento numero-applicazione si fa dal Dashboard: Numbers > Your numbers > Edit > Forward to Application.

Passo 8 - Crea operatori Vonage tramite l'interfaccia amministrativa:

Accedere all'applicazione con un account ADMIN, aprire il menu contestuale della barra CTI
e selezionare "Lista operatori". Da qui e possibile:

- Nuovo...: crea un utente Vonage e lo registra localmente in un unico passaggio.
  Il "Nome utente Vonage" (es. operatore_01) diventa l'identificatore univoco su Vonage.
  Il "Nome visualizzato" e facoltativo (es. Mario Rossi).

- Sincronizza...: importa dal Dashboard Vonage tutti gli utenti gia esistenti
  che non sono ancora presenti nel database locale. Utile dopo aver creato
  utenti manualmente via CLI o Dashboard.

In alternativa, gli operatori possono essere creati via CLI:

    vonage users create --name='operatore_01' --display_name='Mario Rossi'

Dopo la creazione via CLI, usare "Sincronizza..." dalla GUI per importarli localmente.


SVILUPPO LOCALE

Vonage deve raggiungere il backend tramite URL pubblico. Usare ngrok.

Installazione ngrok nel container:

    curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
      | tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
      && echo 'deb https://ngrok-agent.s3.amazonaws.com bookworm main' \
      | tee /etc/apt/sources.list.d/ngrok.list \
      && apt-get update -qq && apt-get install -y ngrok

Registrare il token (disponibile su dashboard.ngrok.com) e avviare il tunnel:

    ngrok config add-authtoken <NGROK_TOKEN>
    ngrok http 8080

Usare l'URL https generato come base per cti.vonage.answer_url, cti.vonage.event_url
e per i webhook nel Dashboard Vonage. L'URL cambia a ogni riavvio del tunnel (piano gratuito).


RIFERIMENTI

Architettura e flusso chiamata: docs/cti.md
Setup Vonage dettagliato: docs/vonage/setup_procedura.md
Pattern operator-first: docs/vonage/voice2_operator_first_pattern.md
Voice API reference: docs/vonage/voice/api/
Client SDK reference: docs/vonage/voice/client_sdk/
