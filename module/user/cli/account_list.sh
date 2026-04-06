#!/bin/bash
# GET /api/user/accounts - Lista tutti gli account
#
# Usage:
#   ./account_list.sh
#
# Example:
#   ./account_list.sh

set -e

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

echo "[*] Recupero lista account..."
RESPONSE=$(curl -s -b "$SESSION_FILE" -X GET "$API_BASE/api/user/accounts" \
  -H "Content-Type: application/json")

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Lista account recuperata:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nel recupero lista:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
