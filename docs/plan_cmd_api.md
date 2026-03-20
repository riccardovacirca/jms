# Piano: `cmd api` — interfaccia CLI per i moduli

## Obiettivo

Aggiungere al comando `cmd` un subcommand `api` che permette di invocare gli
endpoint di un modulo dalla riga di comando, senza conoscere i dettagli HTTP.
Ogni modulo può opzionalmente esporre un insieme di comandi CLI; quando viene
importato, questi comandi diventano automaticamente disponibili sotto `cmd api`.

```bash
cmd api user create --mail mario@azienda.it --name "Mario Rossi" --type operator
cmd api user list
cmd api cti chiamate
```

---

## Principio di funzionamento

`cmd api` scopre i comandi disponibili leggendo i tracker installati:

```
src/main/resources/modules/<nome>/module.json
```

Ogni tracker è scritto da `cmd module import` e rimosso da `cmd module remove`.
Se un `module.json` contiene una chiave `"cli"`, i suoi comandi vengono inclusi
nell'albero di `cmd api`. Nessuna registrazione aggiuntiva è necessaria: il
ciclo di vita è interamente delegato al sistema di import/remove già esistente.

---

## Estensione di `module.json`

Si aggiunge una chiave opzionale `"cli"` allo schema esistente. La sua assenza
significa che il modulo non espone comandi CLI.

### Schema completo

```json
{
  "name": "user",
  "version": "1.0.0",
  "dependencies": {},
  "api": { "routes": "...", "config": {} },
  "gui": { "config": { ... } },
  "cli": {
    "auth": {
      "endpoint": "POST /api/user/auth/login",
      "credentials": {
        "mail":     { "env": "CLI_USER_MAIL",     "prompt": "Email amministratore" },
        "password": { "env": "CLI_USER_PASSWORD", "prompt": "Password", "secret": true }
      },
      "cookie": "access_token"
    },
    "commands": [
      {
        "name": "create",
        "description": "Crea un nuovo utente",
        "endpoint": "POST /api/user/admin/users",
        "args": [
          { "flag": "--mail",     "field": "mail",     "required": true,  "description": "Email utente" },
          { "flag": "--name",     "field": "name",     "required": true,  "description": "Nome visualizzato" },
          { "flag": "--type",     "field": "type",     "required": true,  "description": "Tipo utente",
            "values": ["operator", "admin"] }
        ]
      },
      {
        "name": "list",
        "description": "Lista utenti",
        "endpoint": "GET /api/user/admin/users"
      },
      {
        "name": "delete",
        "description": "Elimina un utente",
        "endpoint": "DELETE /api/user/admin/users/{id}",
        "args": [
          { "flag": "--id", "field": "id", "required": true, "path": true, "description": "ID utente" }
        ]
      }
    ]
  },
  "install_notice": null
}
```

### Campi della chiave `"cli"`

**`auth`** — descrive come autenticarsi prima di eseguire i comandi:

| Campo | Descrizione |
|-------|-------------|
| `endpoint` | Metodo e path dell'endpoint di autenticazione (`METODO /path`) |
| `credentials` | Mappa dei campi richiesti dal body di autenticazione |
| `credentials.<field>.env` | Variabile d'ambiente da cui leggere il valore |
| `credentials.<field>.prompt` | Testo mostrato all'utente se la variabile è assente |
| `credentials.<field>.secret` | Se `true`, il valore non viene mostrato in input |
| `cookie` | Nome del cookie ricevuto nella risposta che verrà usato nelle chiamate successive |

Se `auth` è assente, il comando viene eseguito senza autenticazione preventiva
(es. endpoint pubblici o webhook).

**`commands`** — array dei comandi disponibili:

| Campo | Descrizione |
|-------|-------------|
| `name` | Nome del sottocomando (`cmd api <modulo> <name>`) |
| `description` | Testo mostrato in `--help` |
| `endpoint` | Metodo e path dell'endpoint da invocare |
| `args` | Array di argomenti accettati (opzionale) |

**Argomento singolo** (`args[i]`):

| Campo | Descrizione |
|-------|-------------|
| `flag` | Flag CLI (es. `--mail`) |
| `field` | Nome del campo nel body JSON (o nel path se `path: true`) |
| `required` | Se `true`, errore se assente |
| `description` | Testo mostrato in `--help` |
| `values` | Array di valori ammessi (validazione) |
| `path` | Se `true`, il valore viene sostituito nel path dell'endpoint (es. `{id}`) |

---

## Comportamento di `cmd api`

### Discovery

All'avvio, `cmd api` scansiona:

```
src/main/resources/modules/*/module.json
```

e costruisce in memoria l'albero dei comandi disponibili. I moduli senza chiave
`"cli"` vengono ignorati.

### Invocazione

```bash
cmd api                              # mostra help: moduli e comandi disponibili
cmd api <modulo>                     # mostra help del modulo
cmd api <modulo> <comando> [args]    # esegue il comando
```

### Esecuzione di un comando

1. **Validazione argomenti**: verifica che tutti i `required` siano presenti e
   che i `values` (se definiti) siano rispettati.

2. **Autenticazione** (se `auth` presente nel modulo):
   - Legge le credenziali dalla variabile d'ambiente (`env`) oppure chiede
     all'utente via prompt.
   - Esegue una `curl` verso l'endpoint di autenticazione con body JSON.
   - Estrae il cookie dalla risposta (`Set-Cookie: <cookie>=...`).

3. **Esecuzione della chiamata**:
   - Sostituisce i path param (es. `{id}` → valore di `--id`).
   - Costruisce il body JSON dagli argomenti non-path.
   - Esegue `curl` con il cookie di sessione.

4. **Output**: stampa la risposta JSON formattata (via `python3 -m json.tool`
   o `jq` se disponibile).

### Flusso internamente (bash pseudocode)

```bash
# 1. Autenticazione
AUTH_BODY=$(build_json_from_credentials "$AUTH_CREDENTIALS")
COOKIE=$(curl -s -c - -X POST "$BASE_URL$AUTH_ENDPOINT" \
  -H "Content-Type: application/json" -d "$AUTH_BODY" \
  | extract_cookie "$COOKIE_NAME")

# 2. Chiamata
ENDPOINT=$(substitute_path_params "$CMD_ENDPOINT" "$ARGS")
BODY=$(build_json_from_args "$CMD_ARGS" "$ARGS")
curl -s -b "$COOKIE_NAME=$COOKIE" -X "$METHOD" "$BASE_URL$ENDPOINT" \
  -H "Content-Type: application/json" ${BODY:+-d "$BODY"} \
  | format_json
```

---

## Risoluzione del `BASE_URL`

`cmd api` costruisce il base URL leggendo la configurazione già presente in `.env`:

```bash
BASE_URL="http://localhost:${API_PORT_HOST:-8080}"
```

Non è necessaria nessuna configurazione aggiuntiva per ambienti di sviluppo.
Per ambienti remoti, si può aggiungere `CLI_BASE_URL` come override.

---

## Autenticazione: due pattern supportati

### Pattern 1 — Login con credenziali (modulo `user`)

Il modulo emette un JWT via `POST /api/user/auth/login`. Il cookie `access_token`
viene usato in tutte le chiamate successive.

```json
"auth": {
  "endpoint": "POST /api/user/auth/login",
  "credentials": {
    "mail":     { "env": "CLI_USER_MAIL",     "prompt": "Email" },
    "password": { "env": "CLI_USER_PASSWORD", "prompt": "Password", "secret": true }
  },
  "cookie": "access_token"
}
```

### Pattern 2 — API key (modulo `cti`)

Il modulo accetta una API key via `POST /api/cti/auth`. Il cookie `access_token`
viene emesso allo stesso modo.

```json
"auth": {
  "endpoint": "POST /api/cti/auth",
  "credentials": {
    "apiKey": { "env": "CLI_CTI_API_KEY", "prompt": "CTI API Key", "secret": true }
  },
  "cookie": "access_token"
}
```

L'utente non percepisce differenza tra i due pattern: `cmd api` gestisce
l'autenticazione in modo trasparente.

---

## Integrazione con `cmd module import` e `cmd module remove`

Non è richiesta nessuna modifica alla logica di import/remove. Il tracker
`src/main/resources/modules/<nome>/module.json` contiene già tutto il necessario.
Se il modulo ha la chiave `"cli"`, i comandi diventano disponibili
automaticamente dopo l'import; spariscono automaticamente dopo il remove.

L'unica aggiunta a `module_install()` è copiare il `cli/` folder se presente:

```bash
# Nella funzione module_install(), dopo la copia della GUI:
if [ -d "$MODULE_DIR/cli" ]; then
    info "[module] Copia script CLI ausiliari..."
    mkdir -p "$WORKSPACE/src/main/resources/modules/$MODULE/cli"
    cp -r "$MODULE_DIR/cli/." "$WORKSPACE/src/main/resources/modules/$MODULE/cli/"
    success "  Script CLI installati"
fi
```

Questo è necessario solo se il modulo include script bash ausiliari (vedi sezione
"Comandi avanzati"). Per comandi che si traducono direttamente in chiamate HTTP,
`module.json` è sufficiente.

---

## Struttura modulo con CLI

```
modules/user/
  api/          → sorgenti Java
  gui/          → sorgenti frontend
  migration/    → SQL
  config/       → properties
  cli/          → (opzionale) script bash ausiliari
    helpers.sh  → funzioni condivise tra comandi
  module.json   → include chiave "cli"
```

La cartella `cli/` è opzionale. La maggior parte dei moduli non ne ha bisogno:
la chiave `"cli"` in `module.json` è sufficiente per comandi che mappano
direttamente su endpoint REST.

---

## Comandi avanzati (script ausiliari)

Per comandi che richiedono logica non esprimibile in JSON (pipeline multi-step,
interazione con file locali, trasformazioni di dati), il modulo può fornire uno
script bash in `cli/`. Il descrittore rimanda allo script invece che a un endpoint:

```json
{
  "name": "import-csv",
  "description": "Importa utenti da file CSV",
  "script": "cli/import_csv.sh",
  "args": [
    { "flag": "--file", "field": "file", "required": true, "description": "Path al CSV" }
  ]
}
```

`cmd api` esegue lo script passando gli argomenti come variabili d'ambiente.
Lo script ha accesso alle funzioni di `cmd` (base URL, cookie di sessione) via
sourcing di un helper comune.

---

## Modifiche a `bin/cmd`

### 1. Aggiungere il subcommand `api` nel dispatcher principale

```bash
api)
    shift 1
    cmd_api "$@"
    ;;
```

### 2. Implementare `cmd_api()`

La funzione principale che:
- Senza argomenti: chiama `api_help`
- Con solo `<modulo>`: chiama `api_help_module <modulo>`
- Con `<modulo> <comando> [args]`: chiama `api_exec <modulo> <comando> [args]`

### 3. Implementare le funzioni ausiliarie

| Funzione | Responsabilità |
|----------|----------------|
| `api_discover` | Scansiona i tracker, costruisce l'albero comandi in memoria |
| `api_help` | Mostra tutti i moduli e i loro comandi |
| `api_help_module` | Mostra i comandi di un modulo con descrizione e argomenti |
| `api_exec` | Valida argomenti, autentica, esegue la chiamata HTTP |
| `api_authenticate` | Risolve credenziali (env/prompt), esegue curl di auth, restituisce cookie |
| `api_build_json` | Costruisce il body JSON dagli argomenti CLI |
| `api_substitute_path` | Sostituisce i path param nell'endpoint |
| `api_format_output` | Formatta la risposta JSON |

Tutte le funzioni usano `python3` per il parsing JSON dei `module.json`, in linea
con il pattern già usato in `module_import`.

### 4. Aggiornare `show_help`

Aggiungere `cmd api` alla sezione comandi mostrata da `cmd --help`.

---

## Esempio d'uso: modulo `user`

```bash
# Crea un operatore
cmd api user create --mail mario@azienda.it --name "Mario Rossi" --type operator

# Lista utenti
cmd api user list

# Elimina utente
cmd api user delete --id 42

# Help modulo
cmd api user
```

Output di `cmd api user`:

```
Comandi disponibili per il modulo 'user':

  create   Crea un nuovo utente
           --mail  <string>          Email utente (obbligatorio)
           --name  <string>          Nome visualizzato (obbligatorio)
           --type  operator|admin    Tipo utente (obbligatorio)

  list     Lista utenti

  delete   Elimina un utente
           --id    <string>          ID utente (obbligatorio)
```

---

## Esempio d'uso: modulo `cti`

```bash
# Lista chiamate
cmd api cti chiamate

# Help
cmd api cti
```

---

## Ordine di implementazione suggerito

1. Estendere lo schema di `module.json` (solo documentazione, nessun codice da
   cambiare — il sistema di import ignora chiavi sconosciute).
2. Implementare `cmd_api` in `bin/cmd` con discovery e help.
3. Implementare `api_authenticate` e `api_exec` per il pattern semplice
   (endpoint REST senza path param).
4. Aggiungere supporto path param.
5. Aggiungere supporto script ausiliari (`cli/` folder).
6. Aggiungere la chiave `"cli"` al `module.json` del modulo `user` come primo
   modulo di riferimento.
7. Aggiornare `module_install()` per copiare `cli/` se presente.
