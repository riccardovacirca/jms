# user

Gestione completa di autenticazione e profilo utente. Copre tre entità: account (credenziali e CRUD amministrativo), auth (login, logout, 2FA, reset/cambio password, sessione JWT) e profilo utente con impostazioni chiave/valore.

## Endpoint

### Account

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/user/accounts` | Lista account con paginazione e filtri |
| POST | `/api/user/accounts` | Registrazione nuovo account |
| GET | `/api/user/accounts/sid` | Account dell'utente autenticato |
| PUT | `/api/user/accounts/sid` | Aggiornamento account dell'utente autenticato |
| DELETE | `/api/user/accounts/sid` | Eliminazione account dell'utente autenticato |
| GET | `/api/user/accounts/{id}` | Account per ID |

### Auth

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/user/auth/session` | Verifica sessione corrente |
| GET | `/api/user/auth/generate-password` | Genera una password casuale sicura |
| POST | `/api/user/auth/login` | Login con username e password |
| POST | `/api/user/auth/logout` | Logout e revoca refresh token |
| POST | `/api/user/auth/refresh` | Rinnovo access token tramite refresh token |
| POST | `/api/user/auth/2fa` | Verifica PIN two-factor |
| POST | `/api/user/auth/forgot-password` | Invio link di reset password via email |
| POST | `/api/user/auth/reset-password` | Reset password tramite token |
| PUT | `/api/user/auth/change-password` | Cambio password autenticato |

### Profile

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/user/users/sid` | Profilo dell'utente autenticato |
| PUT | `/api/user/users/sid` | Aggiornamento profilo |
| GET | `/api/user/users/sid/settings` | Lista impostazioni utente |
| POST | `/api/user/users/sid/settings` | Crea o aggiorna impostazione |
| GET | `/api/user/users/sid/settings/{key}` | Singola impostazione per chiave |
| DELETE | `/api/user/users/sid/settings/{key}` | Eliminazione impostazione |

## Import

```bash
cmd module import user
```

## Benchmark

```bash
cmd bench -c 10 -r 50 -f modules/user/bench.txt
```
