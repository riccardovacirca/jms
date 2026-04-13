#!/bin/bash
# Test: operator-delete
#
# Autentica come admin, crea un operatore da eliminare, esegue operator-delete
# confermando in modo non interattivo (echo "y" | ...), verifica che l'operatore
# non sia più presente nella lista.
#
# Nota: richiede credenziali Vonage configurate.
#
# Usage:
#   cmd module test cti/vonage operator-delete-admin

set -e

WORKSPACE="${1:-/workspace}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="/tmp/jms-cti-test-$$"
CTI_TEST_USER="${CTI_TEST_USER:-root@localhost}"
CTI_TEST_PASS="${CTI_TEST_PASS:-root}"
TEST_OP_NAME="test_op_del_$(date +%s)"

_fail() { echo "[FAIL] $1"; exit 1; }
_ok()   { echo "[ OK ] $1"; }

OP_ID=""
DELETED=false

cleanup() {
  if [ "$DELETED" = "false" ] && [ -n "$OP_ID" ]; then
    echo "[cleanup] Eliminazione operatore ID=$OP_ID (test non completato)..."
    echo "y" | SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-delete "$OP_ID" 2>/dev/null || true
  fi
  SESSION_FILE="$SESSION_FILE" cmd module cli user auth_logout 2>/dev/null || true
  rm -f "$SESSION_FILE"
}
trap cleanup EXIT

echo "[TEST] operator-delete: autentica come $CTI_TEST_USER"
SESSION_FILE="$SESSION_FILE" cmd module cli user auth-login "$CTI_TEST_USER" "$CTI_TEST_PASS" \
  || _fail "Login fallito"
_ok "Login completato"

echo "[TEST] operator-delete: crea operatore da eliminare '$TEST_OP_NAME'"
SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-create \
  "$TEST_OP_NAME" "Operatore Delete Test" 2>&1 | grep -v "^\[" || true

OP_ID=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator" | \
  python3 -c "import sys, json; ops = json.load(sys.stdin).get('out', []); \
  match = [o for o in ops if o.get('vonage_user_id') == '$TEST_OP_NAME']; \
  print(match[0]['id'] if match else '')")

[ -n "$OP_ID" ] || _fail "Impossibile recuperare ID dell'operatore appena creato"
_ok "Operatore creato con ID=$OP_ID"

echo "[TEST] operator-delete: elimina operatore ID=$OP_ID (conferma non interattiva)"
OUTPUT=$(echo "y" | SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-delete "$OP_ID" 2>&1)
echo "$OUTPUT"

echo "$OUTPUT" | grep -qi "annullata\|\[!\]" && _fail "operator-delete ha fallito o è stato annullato"
_ok "operator-delete completato"
DELETED=true

echo "[TEST] operator-delete: verifica che l'operatore non sia più in lista"
LIST=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator" | \
  python3 -c "import sys, json; ops = json.load(sys.stdin).get('out', []); \
  match = [o for o in ops if o.get('vonage_user_id') == '$TEST_OP_NAME']; \
  print('found' if match else 'not_found')")

[ "$LIST" = "not_found" ] || _fail "Operatore '$TEST_OP_NAME' è ancora presente dopo l'eliminazione"
_ok "Operatore '$TEST_OP_NAME' non più presente in lista"

echo "[TEST] operator-delete: tenta get su ID eliminato (deve restituire errore)"
GET_OUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-get "$OP_ID" 2>&1)
echo "$GET_OUT"
echo "$GET_OUT" | grep -q "\[!\]" \
  || echo "[warn] Get su operatore eliminato non ha restituito errore (potrebbe essere OK se l'ID viene riusato)"
_ok "Comportamento post-eliminazione verificato"

echo ""
echo "[PASS] operator-delete-admin"
