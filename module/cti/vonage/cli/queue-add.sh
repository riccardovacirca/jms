#!/bin/bash
# POST /api/cti/vonage/queue - Aggiunge un contatto alla coda CTI (inserimento singolo)
#
# Richiede sessione USER attiva. Esegui prima: cmd module cli user auth-login
#
# Usage:
#   ./queue-add.sh PHONE [--id N] [--data JSON] [--callback URL] [--priorita N]
#   ./queue-add.sh --help

set -e

if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  cat << HELP
Usage: cmd module cli cti/vonage queue-add PHONE [--id N] [--data JSON] [--callback URL] [--priorita N]

Inserisce un contatto nella coda CTI (inserimento singolo).

Argomenti:
  PHONE          Numero di telefono (es. 390612345678)

Opzioni:
  --id N         ID del contatto nel sistema sorgente (es. CRM)
  --data JSON    Array JSON con i dati del contatto.
                 Formato: '[{"key":"...","value":"...","type":"string|number|text"},...]'
                 Il CTI non interpreta il contenuto: lo memorizza e lo mostra as-is.
  --callback URL URL da notificare a fine chiamata
  --priorita N   Priorità numerica (default: 0)

Prerequisiti:
  - Sessione USER attiva (esegui prima: cmd module cli user auth-login)

Esempi:
  cmd module cli cti/vonage queue-add 390612345678

  cmd module cli cti/vonage queue-add 390612345678 \\
    --id 1001 \\
    --data '[{"key":"nome","value":"Mario","type":"string"},{"key":"cognome","value":"Rossi","type":"string"}]' \\
    --callback "http://localhost:8080/api/crm/cti/callback" \\
    --priorita 1

Variabili ambiente:
  API_BASE      Base URL API (default: http://localhost:8080)
  SESSION_FILE  File sessione (default: /tmp/jms-session)
HELP
  exit 0
fi

PHONE="${1:?Errore: PHONE richiesto. Usa --help per maggiori informazioni}"
shift

ID=""
DATA="[]"
CALLBACK=""
PRIORITA="0"

while [ $# -gt 0 ]; do
  case "$1" in
    --id)       ID="$2";       shift 2 ;;
    --data)     DATA="$2";     shift 2 ;;
    --callback) CALLBACK="$2"; shift 2 ;;
    --priorita) PRIORITA="$2"; shift 2 ;;
    *) echo "[!] Argomento non riconosciuto: $1. Usa --help per maggiori informazioni"; exit 1 ;;
  esac
done

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

export _PHONE="$PHONE" _ID="$ID" _DATA="$DATA" _CALLBACK="$CALLBACK" _PRIORITA="$PRIORITA"

REQUEST_JSON=$(python3 << 'PYEOF'
import json, os, sys

phone    = os.environ['_PHONE']
raw_id   = os.environ.get('_ID', '')
raw_data = os.environ.get('_DATA', '[]')
callback = os.environ.get('_CALLBACK', '') or None
priorita = int(os.environ.get('_PRIORITA', '0'))

try:
    data = json.loads(raw_data)
except json.JSONDecodeError as e:
    print(f'[!] --data non è un JSON valido: {e}', file=sys.stderr)
    sys.exit(1)

contatto_id = int(raw_id) if raw_id else None
contatto    = {'id': contatto_id, 'phone': phone, 'callback': callback, 'data': data}
body        = {'contattoJson': json.dumps(contatto), 'priorita': priorita}
print(json.dumps(body))
PYEOF
)

RESPONSE=$(curl -s -b "$SESSION_FILE" -X POST "$API_BASE/api/cti/vonage/queue" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

if echo "$RESPONSE" | grep -q '"err":true'; then
  echo "$RESPONSE" | python3 -c "import sys, json; d=json.load(sys.stdin); print('[!]', d.get('log', 'Errore sconosciuto'))"
  exit 1
fi

echo "$RESPONSE" | python3 -c "
import sys, json
out = json.load(sys.stdin).get('out', {})
print('Contatto aggiunto: id=' + str(out.get('id', '?')))
"
