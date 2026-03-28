# Configurazione

La configurazione dell'applicazione è divisa in due livelli con responsabilità distinte:

- **`.env`** — parametri di infrastruttura (Docker, porte, credenziali DB), letti dagli script shell (`install.sh`, `release.sh`, `bin/cmd`). Non viene mai letto dall'applicazione Java.
- **`config/application.properties`** — configurazione runtime dell'applicazione, letta da `Config.java` all'avvio.

---

## `Config.java`

Carica `application.properties` da `/app/config/application.properties` (path fisso, bind-montato da `./config/` sul host). Se il file non esiste, l'applicazione termina con exit code 1.

**Priorità**: le variabili d'ambiente hanno precedenza sulle chiavi del file. La conversione del nome avviene automaticamente: `db.host` → `DB_HOST`, `jwt.secret` → `JWT_SECRET` (uppercase + `.` → `_`).

```java
config.get("db.host", "localhost")     // String, con default
config.getInt("db.pool.size", 10)      // int, con default
```

Se la chiave non è presente né nel file né nell'ambiente, viene restituito il default. `getInt` ritorna il default anche se il valore non è un numero valido.

---

## `application.properties`

### Server

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `server.port` | `8080` | Porta HTTP Undertow |
| `app.base.url` | — | URL base pubblica dell'app (es. per link nelle email) |

### Database

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `db.host` | — | Hostname PostgreSQL (in sviluppo: nome container Docker) |
| `db.port` | `5432` | Porta PostgreSQL |
| `db.name` | — | Nome database |
| `db.user` | — | Utente database |
| `db.password` | — | Password database |
| `db.pool.size` | `10` | Dimensione pool HikariCP |

Se `db.host` è vuoto, `DB.init()` salta la configurazione e il DataSource non viene creato (Flyway e tutti i DAO vengono disabilitati).

### JWT

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `jwt.secret` | `dev-secret-change-in-production` | Chiave HMAC-SHA256 per la firma dei token |
| `jwt.access.expiry.seconds` | `900` | Scadenza access token in secondi (default: 15 minuti) |

`jwt.secret` deve essere una stringa lunga e casuale in produzione. Con il valore di default i token sono verificabili da chiunque conosca il default.

### Async handler

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `async.pool.size` | `20` | Thread pool per handler registrati con `router.async()` |
| `async.max.body.size` | `10485760` | Dimensione massima body per handler async (bytes, default: 10 MB) |

Linee guida per `async.pool.size`: minimo = numero CPU, raccomandato = `db.pool.size`, massimo = `2 × db.pool.size`.

### Mail (SMTP)

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `mail.enabled` | `false` | Abilita l'invio email. `false` disabilita completamente `Mail.send()` |
| `mail.host` | — | Hostname server SMTP |
| `mail.port` | `1025` | Porta SMTP |
| `mail.auth` | `false` | Abilita autenticazione SMTP |
| `mail.user` | — | Utente SMTP (se `mail.auth=true`) |
| `mail.password` | — | Password SMTP (se `mail.auth=true`) |
| `mail.from` | — | Indirizzo mittente |

In sviluppo usare Mailpit (`./install.sh --mailpit`): cattura le email senza spedirle e le espone in una web UI su `http://localhost:<MAILPIT_UI_PORT_HOST>`.

### Scheduler

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `scheduler.enabled` | `true` | Abilita JobRunr. `false` disabilita completamente lo scheduler |
| `scheduler.poll.interval.seconds` | `15` | Frequenza di polling del background job server |

### Moduli

I moduli installati possono aggiungere le proprie chiavi. Per convenzione usano il prefisso `<nome_modulo>.*`:

| Chiave | Default | Descrizione |
|--------|---------|-------------|
| `user.cookie.secure` | `false` | Flag `Secure` sui cookie JWT (richede HTTPS, abilitare in produzione) |
| `user.cookie.samesite` | `Lax` | Politica SameSite dei cookie (`Strict`, `Lax`, `None`) |
| `user.ratelimit.max.attempts` | `5` | Tentativi di login falliti prima del blocco |
| `user.ratelimit.window.seconds` | `300` | Finestra temporale del rate limiter (secondi) |

---

## `.env`

Contiene i parametri di infrastruttura usati da `install.sh`, `release.sh` e `bin/cmd`. **Non viene mai letto dall'applicazione Java.** Viene generato automaticamente da `install.sh` al primo avvio se non esiste, ed è escluso da git.

Parametri principali:

| Variabile | Descrizione |
|-----------|-------------|
| `PROJECT_NAME` | Nome del progetto — usato come nome container, rete Docker, database |
| `API_PORT_HOST` | Porta host per l'API (es. `2310` → `http://localhost:2310`) |
| `VITE_PORT_HOST` | Porta host per il dev server Vite |
| `DEBUG_PORT_HOST` | Porta host per JDWP (debug Java) |
| `PGSQL_HOST` | Hostname del container PostgreSQL |
| `PGSQL_PORT_HOST` | Porta host per PostgreSQL |
| `PGSQL_NAME`, `PGSQL_USER`, `PGSQL_PASSWORD` | Credenziali database |
| `MAILPIT_UI_PORT_HOST` | Porta host per la web UI di Mailpit |
| `JAVA_VERSION` | Versione Java per le immagini Docker (default: `21`) |
| `ARTIFACT_VERSION` | Versione dell'artefatto di release (default: `1.0.0`) |

---

## Relazione tra `.env` e `application.properties`

`install.sh` sostituisce i placeholder in `application.properties` con i valori di `.env` al momento dell'install:

```
{{PROJECT_NAME}}    → PROJECT_NAME
{{PGSQL_PASSWORD}}  → PGSQL_PASSWORD
{{MAILPIT_HOST}}    → MAILPIT_HOST
{{APP_BASE_URL}}    → http://localhost:<API_PORT_HOST>
db.host=postgres    → db.host=<PGSQL_HOST>
```

Dopo la sostituzione `application.properties` contiene i valori definitivi per l'ambiente di sviluppo. Le modifiche successive a `.env` non si propagano automaticamente — richiedono un nuovo `./install.sh`.

---

## Aggiungere configurazione in un modulo

Un modulo che necessita di configurazione riceve l'oggetto `Config` come secondo parametro di `Routes.register`:

```java
public static void register(Router router, Config config)
{
  String apiKey;
  apiKey = config.get("mymodule.api.key", "");
  // ...
}
```

Le chiavi del modulo vanno aggiunte a `config/application.properties` con prefisso `<nome_modulo>.*` e documentate nel file `module/config/` del modulo stesso.
