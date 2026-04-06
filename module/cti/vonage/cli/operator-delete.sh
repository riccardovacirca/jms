#!/bin/bash
# DELETE /api/cti/vonage/admin/operator/{id} - Elimina operatore locale e utente Vonage
#
# Usage:
#   cmd module cli cti/vonage operator-delete <id>

set -e

ID="${1:?Errore: ID richiesto. Usage: cmd module cli cti/vonage operator-delete <id>}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

echo "Eliminare operatore ID=$ID e il corrispondente utente Vonage? [y/N]"
read -r CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
  echo "Operazione annullata."
  exit 0
fi

RESPONSE=$(curl -s -b "$SESSION_FILE" -X DELETE "$API_BASE/api/cti/vonage/admin/operator/$ID")

if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('log', 'Errore sconosciuto'))" 2>/dev/null || echo "$RESPONSE"
  exit 1
fi

echo "Operatore $ID eliminato."
