- Le migrazioni dei moduli devono avere il prefisso jms_<nome_modulo>_

- [FIXED] BUG - il post-install script dei moduli chiama l'API prima che l'app
  venga ricompilata e riavviata con il nuovo modulo

- Se l'installazione di un modulo prevede una installazione di un pacchetto npm
  deve essere verificata la presenza di node e di npm e in caso di assenza deve
  essere dato un messaggio amichevola all'utente

- [OK] Verificare che il modulo header consenta a ogni modulo di aggiungere alla
  barra header un proprio elemento ad esempio uno o più link o un menu dropdown.
  Rimuovi il link contatti hardcoded. Non è obbligatorio per un modulo
  aggiungere voci al menu. se lo fa devo potere scegliere tra diverse aree dello
  header. la gui del modulo header dovrebbe prevedere a partire da sinistra le
  seguenti aree: logo, link, tema, user. le aree link e user sono vuote. l'area
  logo dovrebbe essere localmente configurabile. l'area tema contiene il
  pulsante light/dark, l'area link può essere usata da tutti i moduli per
  aggiungere un proprio menu dropdown o un singolo link (1 link o menu per
  modulo). l'area user può essere usata da un solo modulo ovvero allo stato
  attuale dal modulo user che dovrebbe essere il contesto in cui il menu
  dropdown dell'utente è definito e da cui è importato. ovviamente il modulo
  user non è privilegiato altri moduli possono usare la area user ma non due
  moduli contemporaneamente.

- [FIXED] Verificare a cosa è collegato l'attuale link Contatti

- Ogni modulo potrebbe avere un file config.js con parametri di configurazione
  custom importabili quando il modulo viene caricato.

- Il titolo visualizzato alla estrema sinistra della barra header dovrebbe
  essere configurabile e potrebbe star enella config.js del modulo header

- La barra CTI dovebbe avere un padding minimale max 2px.

- Attualmente esiste una azione nel menu di contesto della barra CTI per
  importare tutti gli operatori vonage disponibili e associarli a operatori CTI.
  Vorrei capire il contesto ghenerale di questo comportamento.
  Quando un utente esegue il login se il modulo cti è installato viene mostrata
  la barra CTI. Un utente corrisponde a un operatore CTI e un operatore CTI
  corrisponde a un operatore vonage. Il modulo CTI dovrebbe eseguire questo
  controllo e creare questa associazione quando si clicca sul pulsante
  Connetti... al termine della sessione di lavoro o se l'utente perde lo stato
  di autenticazione dovrebbe avvenire una disconnessione anche dall'operatore
  CTI e qundi Vonage. Il link elenco operatori visibile all'utente admin o root
  dovrebbe mostrare lo stato di associazione tra operatore cti e operatore
  vonage relativo a tutti gli operatori che hanno cliccato su Connetti...
  pertanto lo stato di connessione dovrebbe essere persistente. Un utente admin
  o root dovrebbe anche avere un proprio operatore associato sia CTI che vonage.
  La voce elenco operatori non è presente nel menu di contesto di un operatore.
  Al suo posto dovrebbe essere presente una voce 'Stato operatore' che mostra
  statistiche di connessione dell'operatore corrente. Se per qualche ragione si
  perde la connessione con l'operatore vonage bisogna interrompere anche lo
  stato di connessione. La differenza tra operatore vonage e operatore CTI
  consiste nel mantenimento della storia. Uno stesso operatore CTI potrebbe
  essere connesso a operatori vonage diversi ad ogni sessione ma deve conservare
  una storia relativa alla propria attività.

- [OK/PARTIAL] Attualmente il click sul pulsante chiama della barra CTI esegue una
  chaimata a un endpoint del crm che fornisce il prossimo contatto da chiamare.
  Voglio modificare questo comportamento. Il CTI dovrebbe avere una coda di
  chiamata contenente contatti. La coda deve essere persistente. Il formato di
  un contatto nella coda è lo stesso formato che attualmente viene inviato dal
  crm al cti quando si esegue una chiamata. il cti espone un endpoint per
  scrivere nella coda. Il crm usa l'endpoint per scrivere nella coda.
  Il crm può scrivere anche in modo massivo sulla coda.
  Il click sul pulsante chiama estrae il prossimo contatto dalla coda e lo mette
  nella sessione dell'operatore, quindi il contatto viene letto dalla sessione
  per eseguire la chiamata.
  Il cti ha una funzione di chiamata alternativa al pulsante chiama ovvero una
  chiamata continua che potrebbe essere implementata in una sezione
  della barra cti con i simboli Play/Pause per prelevare dalla coda un contatto,
  metterlo in sessione e avviare la chiamata a intervalli regolari ad esempio
  uno ogni 20 secondi. La coda a livello di database è condivisa tra gli
  operatori. Un contatto può essare messo nella sessione di qualsiasi operatore.

- Tutte le operazioni del CTI che riguardano una chiamata dovrebbero generare
  eventi. La politica generale è che se un modulo vuole accedere agli eventi
  generati dal modulo CTI deve sottoscriversi come observer. il gestore di
  eventi del modulo CTI notifica a tutti gli observer gli eventi generati
  durante la propria operatività.

- Deve esistere una tabella settings del cti e una tabella setting del crm che
  sono gestite da admin e root per definire tutti gli aspetti configurabili
  dinamicamente (ad esempio il tempo di attesa tra una chiamata e l'altra nella
  funzione di chiamata continua). A questa tabella deve corrispondere una pagina
  di gestione del crm e una pagina di gestione del cti.

- Che cosa succede se è stata avviata una sessione cti e viene ricaricata
  la pagina? Cosa succede se l'operatore è in conversazione e viene ricaricata
  la pagina? Quale dovrebbe essere il comportamento più stabile e conservativo?

- La cartella /workspace/jms/docs/core contiene un insieme di file che
  documentano tutti i workflow che è possibile isolare dal flusso applicativo
  della classe /workspace/jms/src/main/java/dev/jms/app/App.java. Per ciascuno è
  definito anche un diagramma di sequenza UML con la sintassi Mermaid.
  Usa questi file come modello per documentare tutti i workflow che è possibile
  isolare dal flusso applicativo del modulo CTI. Il livello di granularità di
  questa analisi deve essere ragionevole ovvero non troppo grossolano ne troppo
  fine. Tutti i workflow vanno numerati in modo da rendere esplicita la
  timeline dell'intero processo.

- [OK] Supponiamo di avere due installazioni jms su due macchine diverse. Queste
  installazioni potrebbero avere nomi di progetto diversi esempio hello e hola.
  supponiamo che nel progetto hello io esegua delle modifiche alle classi della
  libreria /workspace/src/main/java/dev/jms/util e che propaghi queste modifiche
  anche in /workspace/jms. Se eseguo il push di jms da hello con cmd git push -r
  hello ed eseguo il pull di jms in hola con cmd git pull -r hello posso copiare
  i file modificati, anche nel src del progetto, senza dover eseguire una nuova
  installazione? Per quali file o cartelle di jms è possibile eseguire la stessa
  procedura in sicurezza? Questa procedura di copia e sovrascrittura può essere
  eseguita anche per i moduli da jms/module a src/main/java/dev/jms/app/module?
  Credo che per App.java, una semplice copia in entrambe le direzioni, non sia
  possibile perchè App.java, viene modificato dai moduli. Il mio dubbio riguarda
  il fatto che una modifica a una libreria di util potrebbe implicare anche una
  modifica delle dipendenze del file pom.xml. fai un quadro di questa situazione
  [RISPOSTA]: Situazione frammentata. Sovrascrittura sicura solo per alcuni file