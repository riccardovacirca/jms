#!/bin/bash
# POST /api/user/auth/login - Login e salvataggio sessione
#
# Salva il cookie di sessione in SESSION_FILE per essere riusato dagli altri comandi CLI.
# Esegui questo comando prima di qualsiasi altro che richieda autenticazione.
#
# Usage:
#   ./auth_login.sh USERNAME PASSWORD
#
# Example:
#   ./auth_login.sh admin@example.com password123

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

echo "[*] Login in corso..."
RESPONSE=$(curl -s -c "$SESSION_FILE" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Login riuscito. Sessione salvata in $SESSION_FILE"
else
  rm -f "$SESSION_FILE"
  echo "[!] Login fallito:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "Errore sconosciuto"
  exit 1
fi
