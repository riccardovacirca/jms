#!/bin/bash
# PUT /api/user/accounts/{id}/password - Cambia password (self)
#
# Usage:
#   ./auth_change_password.sh CURRENT_PASSWORD NEW_PASSWORD
#
# Example:
#   ./auth_change_password.sh oldpass123 newpass123

set -e

CURRENT_PASSWORD="${1:?Errore: CURRENT_PASSWORD richiesta}"
NEW_PASSWORD="${2:?Errore: NEW_PASSWORD richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

ACCOUNT=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/user/accounts/sid")
ACCOUNT_ID=$(echo "$ACCOUNT" | grep -oP '"id":\K[0-9]+' | head -1)

if [ -z "$ACCOUNT_ID" ]; then
  echo "[!] Impossibile ottenere l'id dell'account. Verifica la sessione."
  exit 1
fi

echo "[*] Cambio password..."

REQUEST_JSON=$(cat <<EOF_JSON
{
  "current_password": "$CURRENT_PASSWORD",
  "new_password": "$NEW_PASSWORD"
}
EOF_JSON
)

RESPONSE=$(curl -s -b "$SESSION_FILE" -X PUT "$API_BASE/api/user/accounts/$ACCOUNT_ID/password" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Password cambiata con successo"
else
  echo "[!] Errore nel cambio password:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
