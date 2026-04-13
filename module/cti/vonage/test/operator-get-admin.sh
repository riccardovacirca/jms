#!/bin/bash
# Test: operator-get
#
# Autentica come admin, crea un operatore, lo recupera per ID,
# verifica i campi della risposta, poi elimina l'operatore (cleanup).
#
# Nota: richiede credenziali Vonage configurate.
#
# Usage:
#   cmd module test cti/vonage operator-get-admin

set -e

WORKSPACE="${1:-/workspace}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="/tmp/jms-cti-test-$$"
CTI_TEST_USER="${CTI_TEST_USER:-root@localhost}"
CTI_TEST_PASS="${CTI_TEST_PASS:-root}"
TEST_OP_NAME="test_op_get_$(date +%s)"

_fail() { echo "[FAIL] $1"; exit 1; }
_ok()   { echo "[ OK ] $1"; }

OP_ID=""

cleanup() {
  if [ -n "$OP_ID" ]; then
    echo "[cleanup] Eliminazione operatore ID=$OP_ID..."
    echo "y" | SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-delete "$OP_ID" 2>/dev/null || true
  fi
  SESSION_FILE="$SESSION_FILE" cmd module cli user auth_logout 2>/dev/null || true
  rm -f "$SESSION_FILE"
}
trap cleanup EXIT

echo "[TEST] operator-get: autentica come $CTI_TEST_USER"
SESSION_FILE="$SESSION_FILE" cmd module cli user auth-login "$CTI_TEST_USER" "$CTI_TEST_PASS" \
  || _fail "Login fallito"
_ok "Login completato"

echo "[TEST] operator-get: crea operatore di test '$TEST_OP_NAME'"
SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-create \
  "$TEST_OP_NAME" "Operatore Get Test" 2>&1 | grep -v "^\[" || true

OP_ID=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator" | \
  python3 -c "import sys, json; ops = json.load(sys.stdin).get('out', []); \
  match = [o for o in ops if o.get('vonage_user_id') == '$TEST_OP_NAME']; \
  print(match[0]['id'] if match else '')")

[ -n "$OP_ID" ] || _fail "Impossibile recuperare ID dell'operatore appena creato"
_ok "Operatore creato con ID=$OP_ID"

echo "[TEST] operator-get: recupera operatore ID=$OP_ID"
OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-get "$OP_ID" 2>&1)
echo "$OUTPUT"

echo "$OUTPUT" | grep -q "\[!\]" && _fail "operator-get ha restituito un errore"
_ok "operator-get completato senza errori"

echo "[TEST] operator-get: verifica presenza campo vonageUserId nella risposta"
echo "$OUTPUT" | grep -qi "vonage" || _fail "Campo vonageUserId non presente nella risposta"
_ok "Campo vonageUserId presente"

echo "[TEST] operator-get: verifica che vonageUserId corrisponda a '$TEST_OP_NAME'"
echo "$OUTPUT" | grep -q "$TEST_OP_NAME" || _fail "vonageUserId '$TEST_OP_NAME' non trovato nella risposta"
_ok "vonageUserId corretto"

echo ""
echo "[PASS] operator-get-admin"
