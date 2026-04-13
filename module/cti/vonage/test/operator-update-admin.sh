#!/bin/bash
# Test: operator-update
#
# Autentica come admin, crea un operatore, aggiorna nome e stato attivo,
# verifica che i campi siano stati aggiornati correttamente, poi esegue cleanup.
#
# Nota: richiede credenziali Vonage configurate.
#
# Usage:
#   cmd module test cti/vonage operator-update-admin

set -e

WORKSPACE="${1:-/workspace}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="/tmp/jms-cti-test-$$"
CTI_TEST_USER="${CTI_TEST_USER:-root@localhost}"
CTI_TEST_PASS="${CTI_TEST_PASS:-root}"
TEST_OP_NAME="test_op_upd_$(date +%s)"
TEST_DISPLAY_UPDATED="Operatore Aggiornato $(date '+%H:%M:%S')"

_fail() { echo "[FAIL] $1"; exit 1; }
_ok()   { echo "[ OK ] $1"; }

OP_ID=""

cleanup() {
  if [ -n "$OP_ID" ]; then
    echo "[cleanup] Ripristina attivo=true e elimina operatore ID=$OP_ID..."
    SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-update "$OP_ID" \
      --attivo true 2>/dev/null || true
    echo "y" | SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-delete "$OP_ID" 2>/dev/null || true
  fi
  SESSION_FILE="$SESSION_FILE" cmd module cli user auth_logout 2>/dev/null || true
  rm -f "$SESSION_FILE"
}
trap cleanup EXIT

echo "[TEST] operator-update: autentica come $CTI_TEST_USER"
SESSION_FILE="$SESSION_FILE" cmd module cli user auth-login "$CTI_TEST_USER" "$CTI_TEST_PASS" \
  || _fail "Login fallito"
_ok "Login completato"

echo "[TEST] operator-update: crea operatore di test '$TEST_OP_NAME'"
SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-create \
  "$TEST_OP_NAME" "Operatore Update Test" 2>&1 | grep -v "^\[" || true

OP_ID=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator" | \
  python3 -c "import sys, json; ops = json.load(sys.stdin).get('out', []); \
  match = [o for o in ops if o.get('vonage_user_id') == '$TEST_OP_NAME']; \
  print(match[0]['id'] if match else '')")

[ -n "$OP_ID" ] || _fail "Impossibile recuperare ID dell'operatore appena creato"
_ok "Operatore creato con ID=$OP_ID"

echo "[TEST] operator-update: aggiorna nome a '$TEST_DISPLAY_UPDATED'"
OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-update "$OP_ID" \
  --nome "$TEST_DISPLAY_UPDATED" 2>&1)
echo "$OUTPUT"
echo "$OUTPUT" | grep -q "\[!\]" && _fail "operator-update (--nome) ha restituito un errore"
_ok "Update nome completato"

echo "[TEST] operator-update: disabilita operatore (--attivo false)"
OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-update "$OP_ID" \
  --attivo false 2>&1)
echo "$OUTPUT"
echo "$OUTPUT" | grep -q "\[!\]" && _fail "operator-update (--attivo false) ha restituito un errore"
_ok "Update attivo=false completato"

echo "[TEST] operator-update: verifica stato via operator-get"
GET_OUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-get "$OP_ID" 2>&1)
echo "$GET_OUT"

echo "$GET_OUT" | grep -qi "false" || _fail "Campo attivo non risulta false dopo l'aggiornamento"
_ok "Campo attivo=false confermato"

echo "[TEST] operator-update: aggiorna nome e attivo in una sola chiamata"
OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-update "$OP_ID" \
  --nome "Operatore Finale" --attivo true 2>&1)
echo "$OUTPUT"
echo "$OUTPUT" | grep -q "\[!\]" && _fail "operator-update (nome+attivo) ha restituito un errore"
_ok "Update combinato nome+attivo completato"

echo ""
echo "[PASS] operator-update-admin"
