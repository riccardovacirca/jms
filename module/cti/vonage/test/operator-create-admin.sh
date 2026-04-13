#!/bin/bash
# Test: operator-create
#
# Autentica come admin, crea un operatore CTI con nome e display name completi,
# verifica la risposta, poi elimina l'operatore creato (cleanup).
#
# Nota: richiede credenziali Vonage configurate in application.properties
#   (cti.vonage.application_id, cti.vonage.private_key_path).
#   Se Vonage non è raggiungibile il test fallisce al passo di creazione.
#
# Prerequisiti:
#   - App in esecuzione su API_BASE con Vonage configurato
#   - Account admin/root disponibile (CTI_TEST_USER / CTI_TEST_PASS)
#
# Usage:
#   cmd module test cti/vonage operator-create-admin

set -e

WORKSPACE="${1:-/workspace}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="/tmp/jms-cti-test-$$"
CTI_TEST_USER="${CTI_TEST_USER:-root@localhost}"
CTI_TEST_PASS="${CTI_TEST_PASS:-root}"
TEST_OP_NAME="test_op_$(date +%s)"
TEST_OP_DISPLAY="Operatore di Test $(date '+%Y-%m-%d %H:%M')"

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

echo "[TEST] operator-create: autentica come $CTI_TEST_USER"
SESSION_FILE="$SESSION_FILE" cmd module cli user auth-login "$CTI_TEST_USER" "$CTI_TEST_PASS" \
  || _fail "Login fallito"
_ok "Login completato"

echo "[TEST] operator-create: crea operatore '$TEST_OP_NAME' con display name completo"
OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-create \
  "$TEST_OP_NAME" "$TEST_OP_DISPLAY" 2>&1)
echo "$OUTPUT"

echo "$OUTPUT" | grep -q "\[!\]" && _fail "operator-create ha restituito un errore"
_ok "operator-create completato senza errori"

echo "[TEST] operator-create: verifica presenza del nuovo operatore nella lista"
LIST=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-list 2>&1)
echo "$LIST" | grep -q "$TEST_OP_NAME" || _fail "Operatore '$TEST_OP_NAME' non trovato nella lista"
_ok "Operatore '$TEST_OP_NAME' presente in lista"

OP_ID=$(SESSION_FILE="$SESSION_FILE" curl -s -b "$SESSION_FILE" \
  "$API_BASE/api/cti/vonage/admin/operator" | \
  python3 -c "import sys, json; ops = json.load(sys.stdin).get('out', []); \
  match = [o for o in ops if o.get('vonage_user_id') == '$TEST_OP_NAME']; \
  print(match[0]['id'] if match else '')")

[ -n "$OP_ID" ] && _ok "ID operatore recuperato: $OP_ID" \
  || echo "[warn] Impossibile recuperare ID per cleanup automatico"

echo ""
echo "[PASS] operator-create-admin"
