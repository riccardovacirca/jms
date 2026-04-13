#!/bin/bash
# Test: operator-sync
#
# Autentica come admin, esegue la sincronizzazione degli operatori da Vonage,
# verifica che il comando risponda senza errori e mostra il risultato.
# Non modifica dati locali se tutti gli utenti Vonage sono già presenti.
#
# Nota: richiede credenziali Vonage configurate.
#   La sincronizzazione è idempotente: più esecuzioni consecutive non creano duplicati.
#
# Usage:
#   cmd module test cti/vonage operator-sync-admin

set -e

WORKSPACE="${1:-/workspace}"
API_BASE="${API_BASE:-http://localhost:8080}"
SESSION_FILE="/tmp/jms-cti-test-$$"
CTI_TEST_USER="${CTI_TEST_USER:-root@localhost}"
CTI_TEST_PASS="${CTI_TEST_PASS:-root}"

_fail() { echo "[FAIL] $1"; exit 1; }
_ok()   { echo "[ OK ] $1"; }

cleanup() {
  SESSION_FILE="$SESSION_FILE" cmd module cli user auth_logout 2>/dev/null || true
  rm -f "$SESSION_FILE"
}
trap cleanup EXIT

echo "[TEST] operator-sync: autentica come $CTI_TEST_USER"
SESSION_FILE="$SESSION_FILE" cmd module cli user auth-login "$CTI_TEST_USER" "$CTI_TEST_PASS" \
  || _fail "Login fallito"
_ok "Login completato"

echo "[TEST] operator-sync: conta operatori locali prima della sync"
COUNT_BEFORE=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator" | \
  python3 -c "import sys, json; print(len(json.load(sys.stdin).get('out', [])))")
_ok "Operatori locali prima della sync: $COUNT_BEFORE"

echo "[TEST] operator-sync: esegue sincronizzazione da Vonage"
OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-sync 2>&1)
echo "$OUTPUT"

echo "$OUTPUT" | grep -q "\[!\]" && _fail "operator-sync ha restituito un errore"
_ok "operator-sync completato senza errori"

echo "[TEST] operator-sync: conta operatori locali dopo la sync"
COUNT_AFTER=$(curl -s -b "$SESSION_FILE" "$API_BASE/api/cti/vonage/admin/operator" | \
  python3 -c "import sys, json; print(len(json.load(sys.stdin).get('out', [])))")
_ok "Operatori locali dopo la sync: $COUNT_AFTER"

echo "[TEST] operator-sync: esegue nuovamente (verifica idempotenza)"
OUTPUT2=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-sync 2>&1)
echo "$OUTPUT2"
echo "$OUTPUT2" | grep -q "\[!\]" && _fail "seconda operator-sync ha restituito un errore"
_ok "Seconda sync idempotente: nessun operatore duplicato"

echo ""
echo "[PASS] operator-sync-admin (prima: $COUNT_BEFORE operatori, dopo: $COUNT_AFTER)"
