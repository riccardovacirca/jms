#!/bin/bash
# POST /api/user/accounts - Registra nuovo account
#
# Usage:
#   ./account_register.sh USERNAME EMAIL PASSWORD RUOLO
#
# Example:
#   ./account_register.sh newuser new@example.com pass123 user

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
EMAIL="${2:?Errore: EMAIL richiesta}"
PASSWORD="${3:?Errore: PASSWORD richiesta}"
RUOLO="${4:?Errore: RUOLO richiesto}"

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

echo "[*] Registrazione nuovo account..."

REQUEST_JSON=$(cat <<EOF_JSON
{
  "username": "$USERNAME",
  "email": "$EMAIL",
  "password": "$PASSWORD",
  "ruolo": "$RUOLO"
}
EOF_JSON
)

RESPONSE=$(curl -s -b "$SESSION_FILE" -X POST "$API_BASE/api/user/accounts" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Account registrato con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nella registrazione:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
