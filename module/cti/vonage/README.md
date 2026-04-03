Modulo CTI - Integrazione Vonage Voice API

Implementa il pattern operator-first progressive dialer tramite Vonage Voice API e Vonage Client SDK.
L'operatore e gia in linea prima che il cliente risponda.


CONFIGURAZIONE

Aggiungere in config/application.properties:

    cti.vonage.application_id=<APPLICATION_ID>
    cti.vonage.private_key=/app/config/private.key
    cti.vonage.from_number=12345678901
    cti.vonage.event_url=https://your-domain.com/api/cti/vonage/answer
    cti.vonage.music_on_hold_url=https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3

Copiare la chiave privata Vonage in /app/config/private.key.


DIPENDENZA FRONTEND

Installata automaticamente dallo script di post-install (install.sh) durante cmd module import.
Per reinstallare manualmente dalla cartella gui/:

    npm install @vonage/client-sdk


WEBHOOK NEL VONAGE DASHBOARD

Answer URL:  https://your-domain.com/api/cti/vonage/answer
Event URL:   https://your-domain.com/api/cti/vonage/answer

L'Answer URL e obbligatoria: Vonage la chiama quando l'operatore avvia serverCall() dal browser.


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
      --voice-event-url='https://your-domain.com/api/cti/vonage/answer'

Passo 6 - Abilita la capability RTC (per operatori WebRTC nel browser):

    vonage apps capabilities update <APPLICATION_ID> rtc \
      --rtc-event-url='https://your-domain.com/api/cti/vonage/answer'

Passo 7 - (Opzionale) Acquista un numero Vonage, necessario solo per operatori PSTN:

    vonage numbers search IT
    vonage numbers buy IT 12345678901

Il collegamento numero-applicazione si fa dal Dashboard: Numbers > Your numbers > Edit > Forward to Application.

Passo 8 - Crea utenti Vonage per operatori WebRTC, necessario solo per operatorType "app":

    vonage users create --name='operatore_01'

Il name deve corrispondere all'userId passato al frontend.


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

Usare l'URL https generato come base per cti.vonage.event_url e per i webhook nel Dashboard.


RIFERIMENTI

Architettura e flusso chiamata: docs/cti.md
Setup Vonage dettagliato: docs/vonage/setup_procedura.md
Pattern operator-first: docs/vonage/voice2_operator_first_pattern.md
Voice API reference: docs/vonage/voice/api/
Client SDK reference: docs/vonage/voice/client_sdk/
