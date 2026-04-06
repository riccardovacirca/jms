#!/bin/bash
# PUT /api/cti/vonage/admin/operator/{id} - Aggiorna operatore
#
# Usage:
#   cmd module cli cti/vonage operator-update <id> [--nome <nome>] [--attivo true|false]
#
# Esempi:
#   cmd module cli cti/vonage operator-update 1 --nome "Operatore Uno"
#   cmd module cli cti/vonage operator-update 1 --attivo false
#   cmd module cli cti/vonage operator-update 1 --nome "Operatore Uno" --attivo true

set -e

ID="${1:?Errore: ID richiesto. Usage: cmd module cli cti/vonage operator-update <id> [--nome <n>] [--attivo true|false]}"
shift

NOME=""
ATTIVO=""

while [ $# -gt 0 ]; do
  case "$1" in
    --nome)   NOME="$2";   shift 2 ;;
    --attivo) ATTIVO="$2"; shift 2 ;;
    *) echo "Opzione sconosciuta: $1"; exit 1 ;;
  esac
done

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

REQUEST_JSON=$(python3 -c "
import json, sys
body = {}
nome = sys.argv[1]
attivo = sys.argv[2]
if nome: body['nome'] = nome
if attivo: body['attivo'] = (attivo.lower() == 'true')
print(json.dumps(body))
" "$NOME" "$ATTIVO")

RESPONSE=$(curl -s -b "$SESSION_FILE" -X PUT "$API_BASE/api/cti/vonage/admin/operator/$ID" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('log', 'Errore sconosciuto'))" 2>/dev/null || echo "$RESPONSE"
  exit 1
fi

echo "$RESPONSE" | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin)['out'], indent=2))"
