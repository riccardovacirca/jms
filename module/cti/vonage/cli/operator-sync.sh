#!/bin/bash
# POST /api/cti/vonage/admin/operator/sync - Allinea operatori locali agli utenti Vonage
#
# Per ogni utente presente su Vonage ma assente localmente, crea il record in DB.
# Gli operatori locali senza corrispondente su Vonage non vengono toccati.
#
# Usage:
#   cmd module cli cti/vonage operator-sync

set -e

API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="${SESSION_FILE:-/tmp/jms-session}"

if [ ! -f "$SESSION_FILE" ]; then
  echo "[!] Sessione non trovata ($SESSION_FILE). Esegui prima: cmd module cli user auth-login"
  exit 1
fi

RESPONSE=$(curl -s -b "$SESSION_FILE" -X POST "$API_BASE/api/cti/vonage/admin/operator/sync" \
  -H "Content-Type: application/json" -d '{}')

if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('log', 'Errore sconosciuto'))" 2>/dev/null || echo "$RESPONSE"
  exit 1
fi

echo "$RESPONSE" | python3 -c "
import sys, json
data = json.load(sys.stdin)
created = data.get('out', [])
if not created:
    print('Nessun nuovo operatore da sincronizzare. Tutti gli utenti Vonage sono già presenti localmente.')
    sys.exit(0)
print(f'Operatori creati: {len(created)}')
print()
print(f\"{'ID':<6}  {'VONAGE USER ID':<30}  {'NOME'}\")
print('-' * 70)
for op in created:
    print(f\"{str(op.get('id','')):<6}  {str(op.get('vonageUserId','')):<30}  {str(op.get('nome') or '')}\")
"
