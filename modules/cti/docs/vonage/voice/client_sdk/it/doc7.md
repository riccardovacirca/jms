# Risoluzione dei Problemi

## Lavorare con la Vonage CLI

### Configurazione dell'Applicazione Vonage

Poiché puoi creare più applicazioni Vonage, i comandi che esegui fanno riferimento all'applicazione configurata. Ad esempio, quando crei un utente, devi assicurarti di crearlo nell'applicazione desiderata.

*   Controlla a quale app fa riferimento la tua CLI eseguendo:

```bash
$ cat vonage_app.json
```

### Nessuna Risposta ai Comandi

Se esegui un comando e non ottieni una risposta:

*   Prova ad assicurarti che tutti gli oggetti JSON nel tuo comando siano oggetti chiusi e non manchino ad esempio `}` o `'`.

## JWT

> Ricorda che un JWT è per utente per Applicazione Vonage.

### Errore Token Non Valido

*   [Decodifica il tuo JWT](https://jwt.io/)
    
*   Assicurati che la rivendicazione `"application_id"` sia corretta.
    
*   Assicurati che `"sub"` sia corretto. Cioè, che esista un utente con questo nome utente nella tua Applicazione Vonage.
    
*   Assicurati che il JWT non sia scaduto:
    
    *   Puoi trovare la data di scadenza su `"exp"`, in tempo Unix, che è il numero di secondi dal 01 gennaio 1970 (UTC).
        
    *   Puoi [convertirlo in tempo leggibile](https://www.epochconverter.com/).
        
    *   Assicurati che l'orario di scadenza sia nel futuro, cioè che il JWT non sia ancora scaduto.
        

### Errore di Connessione o Timeout della Connessione

Ottieni un errore di connessione o un timeout della connessione durante il tentativo di accesso all'SDK:

*   Controlla la connessione Internet sul tuo dispositivo.
    
*   Il `JWT` potrebbe essere valido secondo gli standard JWT, tuttavia alcune rivendicazioni potrebbero essere non corrette secondo i requisiti di Vonage. Prova a generare un nuovo `JWT`, assicurandoti della correttezza delle rivendicazioni specifiche di Vonage.
    

### Errori durante la Generazione di un JWT

*   Assicurati che il file della chiave privata esista. Viene generato sulla macchina su cui hai creato l'applicazione.
    
*   Nella nostra documentazione, quando usi la CLI, suggeriamo di utilizzare il percorso `./private.key`.
    
*   Assicurati che la tua chiave privata esista sulla macchina con cui stai generando il JWT e che il percorso sia corretto.
    
*   Se hai bisogno di una nuova chiave privata:
    
    *   Puoi ottenerne una dal [Dashboard](https://dashboard.nexmo.com/voice/your-applications). Nel menu sul lato sinistro seleziona Voice → Your Applications → seleziona l'applicazione → Modifica. In fondo clicca su `Generate public / private key pair`. Ricorda di fare clic su `Save changes`.
        
    *   Salva il file sulla tua macchina e aggiorna il percorso ad esso quando generi il JWT.
        

## Notifiche Push

*   Assicurati di aver caricato il certificato sul server di Vonage. Devi avere un JWT amministrativo valido, cioè un JWT senza la rivendicazione `sub`. Puoi [Decodificare il tuo JWT](https://jwt.io/) per verificarlo.
    
*   Assicurati di aver abilitato le notifiche push e che il metodo `client.registerVoipToken()` sia andato a buon fine. Puoi anche inserire una chiamata di log o un punto di interruzione per assicurarti che la chiamata sia riuscita.
    

# Hai altre Domande?

Se dovessi avere ulteriori domande, problemi o feedback, contattaci all'indirizzo `devrel@vonage.com` o su [Vonage Developer Community Slack](https://developer.vonage.com/en/community/slack).