#!/bin/bash
# GET /api/cti/vonage/admin/operator/{id} - Dettaglio operatore
#
# Usage:
#   cmd module cli cti/vonage operator-get <id>

set -e

ID="${1:?Errore: ID richiesto. Usage: cmd module cli cti/vonage operator-get <id>}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

RESPONSE=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator/$ID")

if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('log', 'Errore sconosciuto'))" 2>/dev/null || echo "$RESPONSE"
  exit 1
fi

echo "$RESPONSE" | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin)['out'], indent=2))"
