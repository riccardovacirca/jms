#!/bin/bash
# POST /api/cti/vonage/admin/operator - Crea utente Vonage e registra operatore CTI
#
# Richiede sessione ADMIN attiva. Esegui prima: cmd module cli user auth_login
#
# Usage:
#   ./operator_create.sh NAME [DISPLAY_NAME]
#
# Example:
#   ./operator_create.sh operatore_01 "Operatore 01"
#   ./operator_create.sh operatore_02

set -e

NAME="${1:?Errore: NAME richiesto (es. operatore_01)}"
DISPLAY_NAME="${2:-}"

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth_login"
  exit 1
fi

# Build JSON body
if [ -n "$DISPLAY_NAME" ]; then
  REQUEST_JSON="{\"name\":\"$NAME\",\"displayName\":\"$DISPLAY_NAME\"}"
else
  REQUEST_JSON="{\"name\":\"$NAME\"}"
fi

echo "[*] Creazione operatore Vonage: $NAME..."
RESPONSE=$(curl -s -b "$SESSION_FILE" -X POST "$API_BASE/api/cti/vonage/admin/operator" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Operatore creato:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nella creazione:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
