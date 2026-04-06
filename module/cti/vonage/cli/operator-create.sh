#!/bin/bash
# POST /api/cti/vonage/admin/operator - Crea utente Vonage e registra operatore CTI
#
# Richiede sessione ADMIN attiva. Esegui prima: cmd module cli user auth-login
#
# Usage:
#   ./operator-create.sh NAME [DISPLAY_NAME]
#   ./operator-create.sh --help
#
# Example:
#   ./operator-create.sh operatore_01 "Operatore 01"
#   ./operator-create.sh operatore_02

set -e

# Help
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  cat << HELP
Usage: cmd module cli cti/vonage operator-create NAME [DISPLAY_NAME]

Crea un nuovo operatore CTI Vonage.

Argomenti:
  NAME           Nome univoco dell'operatore (es. operatore_01)
  DISPLAY_NAME   Nome visualizzato (opzionale, default: NAME)

Prerequisiti:
  - Sessione ADMIN attiva (esegui prima: cmd module cli user auth-login)

Esempi:
  cmd module cli cti/vonage operator-create operatore_01
  cmd module cli cti/vonage operator-create operatore_01 "Operatore 01"

Variabili ambiente:
  API_BASE      Base URL API (default: http://localhost:8080)
  SESSION_FILE  File sessione (default: /tmp/jms-session)
HELP
  exit 0
fi

NAME="${1:?Errore: NAME richiesto. Usa --help per maggiori informazioni}"
DISPLAY_NAME="${2:-}"

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

# Build JSON body
if [ -n "$DISPLAY_NAME" ]; then
  REQUEST_JSON="{\"name\":\"$NAME\",\"displayName\":\"$DISPLAY_NAME\"}"
else
  REQUEST_JSON="{\"name\":\"$NAME\"}"
fi

RESPONSE=$(curl -s -b "$SESSION_FILE" -X POST "$API_BASE/api/cti/vonage/admin/operator" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# Verifica errori
if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi

# Output conciso
echo "out:"
echo "$RESPONSE" | python3 -c "import sys, json; print(json.dumps(json.load(sys.stdin)['out'], indent=2))"
