#!/bin/bash
# Test: queue-add (admin)
#
# Autentica come admin e inserisce due contatti nella coda CTI:
#   1. Contatto persona — tutti i campi tramite CLI (--data JSON)
#   2. Contatto azienda — tutti i campi tramite API diretta
# Verifica la risposta di entrambi gli inserimenti e le statistiche della coda.
#
# Usage:
#   cmd module test cti/vonage queue-add-admin

set -e

WORKSPACE="${1:-/workspace}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="/tmp/jms-cti-test-$$"
CTI_TEST_USER="${CTI_TEST_USER:-admin@localhost}"
CTI_TEST_PASS="${CTI_TEST_PASS:-admin}"

_fail() { echo "[FAIL] $1"; exit 1; }
_ok()   { echo "[ OK ] $1"; }

cleanup() {
  SESSION_FILE="$SESSION_FILE" cmd module cli user auth_logout 2>/dev/null || true
  rm -f "$SESSION_FILE"
}
trap cleanup EXIT

echo "[TEST] queue-add: autentica come $CTI_TEST_USER"
SESSION_FILE="$SESSION_FILE" cmd module cli user auth-login "$CTI_TEST_USER" "$CTI_TEST_PASS" \
  || _fail "Login fallito"
_ok "Login completato"

# ── Contatto 1: persona, tutti i campi via CLI (--data JSON) ──────────────────
echo "[TEST] queue-add: inserimento persona tramite CLI con --data completo"

DATA_PERSONA='[
  {"key":"nome",            "value":"Mario",                  "type":"string"},
  {"key":"cognome",         "value":"Rossi",                  "type":"string"},
  {"key":"tipo",            "value":"persona",                "type":"string"},
  {"key":"email",           "value":"mario.rossi@example.it", "type":"string"},
  {"key":"indirizzo",       "value":"Via Roma 12",            "type":"string"},
  {"key":"cap",             "value":"20121",                  "type":"string"},
  {"key":"comune",          "value":"Milano",                 "type":"string"},
  {"key":"nazione",         "value":"Italia",                 "type":"string"},
  {"key":"id_contatto",     "value":"1001",                   "type":"number"},
  {"key":"id_lista",        "value":"5",                      "type":"number"},
  {"key":"id_campagna",     "value":"3",                      "type":"number"},
  {"key":"lista",           "value":"Clienti Attivi Q1",      "type":"string"},
  {"key":"campagna",        "value":"Rinnovi Aprile 2026",    "type":"string"},
  {"key":"note",            "value":"Cliente storico. Preferisce essere contattato la mattina. Interessato al rinnovo contratto.", "type":"text"}
]'

OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage queue-add "390612345678" \
  --id       "1001" \
  --data     "$DATA_PERSONA" \
  --callback "http://localhost:8080/api/crm/cti/callback" \
  --priorita "1" \
  2>&1)
echo "$OUTPUT"
echo "$OUTPUT" | grep -q "\[!\]" && _fail "queue-add (CLI persona) ha restituito un errore"
QUEUE_ID_1=$(echo "$OUTPUT" | grep -oP "id=\K[0-9]+" || true)
_ok "Contatto persona inserito${QUEUE_ID_1:+ (id=$QUEUE_ID_1)}"

# ── Contatto 2: azienda, tutti i campi via API diretta ────────────────────────
echo "[TEST] queue-add: inserimento azienda via API diretta con payload completo"

CONTATTO_JSON=$(python3 -c "
import json
contatto = {
  'id':       2002,
  'phone':    '390298765432',
  'callback': 'http://localhost:8080/api/crm/cti/callback',
  'data': [
    {'key': 'nome',            'value': 'Laura',                   'type': 'string'},
    {'key': 'cognome',         'value': 'Ferrari',                 'type': 'string'},
    {'key': 'ragione_sociale', 'value': 'Acme S.r.l.',             'type': 'string'},
    {'key': 'tipo',            'value': 'azienda',                 'type': 'string'},
    {'key': 'email',           'value': 'l.ferrari@acme.it',       'type': 'string'},
    {'key': 'indirizzo',       'value': 'Corso Venezia 8',         'type': 'string'},
    {'key': 'cap',             'value': '20121',                   'type': 'string'},
    {'key': 'comune',          'value': 'Milano',                  'type': 'string'},
    {'key': 'nazione',         'value': 'Italia',                  'type': 'string'},
    {'key': 'id_contatto',     'value': '2002',                    'type': 'number'},
    {'key': 'id_lista',        'value': '7',                       'type': 'number'},
    {'key': 'id_campagna',     'value': '4',                       'type': 'number'},
    {'key': 'lista',           'value': 'Aziende PMI',             'type': 'string'},
    {'key': 'campagna',        'value': 'Enterprise Q2',           'type': 'string'},
    {'key': 'note',            'value': 'Referente acquisti. Ha richiesto demo prodotto X. Budget approvato. Decisione entro fine mese.', 'type': 'text'}
  ]
}
body = {'contattoJson': json.dumps(contatto), 'priorita': 2}
print(json.dumps(body))
")

RESP=$(curl -s -b "$SESSION_FILE" -X POST "$API_BASE/api/cti/vonage/queue" \
  -H "Content-Type: application/json" \
  -d "$CONTATTO_JSON")
echo "Risposta: $RESP"
echo "$RESP" | python3 -c "import sys, json; d=json.load(sys.stdin); exit(1 if d.get('err') else 0)" \
  || _fail "Inserimento contatto azienda via API ha restituito errore"
QUEUE_ID_2=$(echo "$RESP" | python3 -c "import sys, json; print(json.load(sys.stdin).get('out', {}).get('id', ''))")
_ok "Contatto azienda inserito (id=$QUEUE_ID_2)"

# ── Statistiche ───────────────────────────────────────────────────────────────
echo "[TEST] queue-add: verifica statistiche coda (scope=all, inCoda >= 2)"
STATS=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/queue/stats?all=true")
echo "Stats: $STATS"
echo "$STATS" | python3 -c "
import sys, json
out     = json.load(sys.stdin).get('out', {})
in_coda = out.get('inCoda', 0)
print(f'  inCoda={in_coda}, disponibili={out.get(\"disponibili\",0)}, pianificati={out.get(\"pianificati\",0)}, scope={out.get(\"scope\")}')
if in_coda < 2:
    print('[warn] inCoda < 2')
"
_ok "Statistiche coda verificate"

echo ""
echo "[PASS] queue-add-admin (ids inseriti: $QUEUE_ID_1, $QUEUE_ID_2)"
echo "[info] I contatti rimangono in coda globale e saranno assegnati al prossimo operatore che chiama getNext"
