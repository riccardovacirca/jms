#!/bin/bash
# POST /api/user/auth/logout - Logout e rimozione sessione
#
# Usa la sessione salvata da auth_login.sh, chiama il logout e rimuove il file.
#
# Usage:
#   ./auth_logout.sh
#
# Example:
#   ./auth_logout.sh

set -e

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth_login"
  exit 1
fi

echo "[*] Logout in corso..."
RESPONSE=$(curl -s -b "$SESSION_FILE" -X POST "$API_BASE/api/user/auth/logout" \
  -H "Content-Type: application/json")

rm -f "$SESSION_FILE"

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Logout riuscito. Sessione rimossa."
else
  echo "[!] Logout fallito (sessione rimossa comunque):"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "Errore sconosciuto"
  exit 1
fi
