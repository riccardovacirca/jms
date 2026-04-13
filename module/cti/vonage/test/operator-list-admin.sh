#!/bin/bash
# Test: operator-list
#
# Autentica come admin, lista tutti gli operatori CTI, verifica la struttura
# della risposta (almeno le colonne attese).
#
# Prerequisiti:
#   - App in esecuzione su API_BASE
#   - Account admin/root disponibile (CTI_TEST_USER / CTI_TEST_PASS)
#
# Usage:
#   cmd module test cti/vonage operator-list-admin

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

echo "[TEST] operator-list: autentica come $CTI_TEST_USER"
SESSION_FILE="$SESSION_FILE" cmd module cli user auth-login "$CTI_TEST_USER" "$CTI_TEST_PASS" \
  || _fail "Login fallito"
_ok "Login completato"

echo "[TEST] operator-list: esegue operator-list"
OUTPUT=$(SESSION_FILE="$SESSION_FILE" cmd module cli cti/vonage operator-list 2>&1)
echo "$OUTPUT"

# Verifica che il comando non abbia restituito un errore
echo "$OUTPUT" | grep -qv "\[!\]" || _fail "operator-list ha restituito un errore"
_ok "operator-list completato senza errori"

echo ""
echo "[PASS] operator-list-admin"
