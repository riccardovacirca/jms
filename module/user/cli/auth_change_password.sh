#!/bin/bash
# PUT /api/user/auth/change-password - Cambia password
#
# Usage:
#   ./auth_change_password.sh OLD_PASSWORD NEW_PASSWORD
#
# Example:
#   ./auth_change_password.sh oldpass123 newpass123

set -e

OLD_PASSWORD="${1:?Errore: OLD_PASSWORD richiesta}"
NEW_PASSWORD="${2:?Errore: NEW_PASSWORD richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

echo "[*] Cambio password..."

REQUEST_JSON=$(cat <<EOF_JSON
{
  "oldPassword": "$OLD_PASSWORD",
  "newPassword": "$NEW_PASSWORD"
}
EOF_JSON
)

RESPONSE=$(curl -s -b "$SESSION_FILE" -X PUT "$API_BASE/api/user/auth/change-password" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Password cambiata con successo"
else
  echo "[!] Errore nel cambio password:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
