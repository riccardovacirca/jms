#!/bin/bash
# GET /api/cti/vonage/admin/operator - Lista operatori locali
#
# Usage:
#   cmd module cli cti/vonage operator-list

set -e

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

RESPONSE=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator")

if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('log', 'Errore sconosciuto'))" 2>/dev/null || echo "$RESPONSE"
  exit 1
fi

echo "$RESPONSE" | python3 -c "
import sys, json
data = json.load(sys.stdin)
ops = data.get('out', [])
if not ops:
    print('Nessun operatore registrato.')
    sys.exit(0)
print(f\"{'ID':<6}  {'VONAGE USER ID':<30}  {'NOME':<25}  {'ATTIVO':<8}  {'ACCOUNT ID'}\")
print('-' * 90)
for op in ops:
    print(f\"{str(op.get('id','')):<6}  {str(op.get('vonageUserId','')):<30}  {str(op.get('nome') or ''):<25}  {str(op.get('attivo','')):<8}  {str(op.get('accountId') or '')}\")
"
