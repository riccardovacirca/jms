#!/bin/bash
# GET /api/user/auth/session - Ottieni sessione corrente
#
# Usage:
#   ./auth_session.sh
#
# Example:
#   ./auth_session.sh

set -e

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth_login"
  exit 1
fi

echo "[*] Recupero sessione..."
RESPONSE=$(curl -s -b "$SESSION_FILE" -X GET "$API_BASE/api/user/auth/session" \
  -H "Content-Type: application/json")

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Sessione recuperata:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nel recupero sessione:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
