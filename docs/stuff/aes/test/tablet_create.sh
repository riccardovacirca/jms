#!/bin/bash
#
# Test: Tablet Create - Creazione configurazione tablet
# Funzionalità: POST /api/aes/tablets
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Crea nuova configurazione tablet con credenziali Savino/Namirial
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
ACCOUNT_ID="${TEST_ACCOUNT_ID:-1}"
TABLET_ID="${TEST_TABLET_ID:-test-tablet-$(date +%s)}"
TABLET_NAME="${TEST_TABLET_NAME:-Test Tablet}"
TABLET_APP="${TEST_TABLET_APP:-sales}"
TABLET_DEPT="${TEST_TABLET_DEPT:-commercial}"
PROVIDER="${TEST_PROVIDER:-savino}"
ENDPOINT="${TEST_ENDPOINT:-https://api.conserva.cloud/api/v1}"
API_USER="${TEST_API_USER:-tablet1}"
API_PASS="${TEST_API_PASS:-secret}"

echo "=========================================="
echo "Test: Tablet Create"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  Account ID: $ACCOUNT_ID"
echo "  Tablet ID: $TABLET_ID"
echo "  Tablet Name: $TABLET_NAME"
echo "  Provider: $PROVIDER"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/tablet_create.sh" "$USERNAME" "$PASSWORD" "$ACCOUNT_ID" "$TABLET_ID" "$TABLET_NAME" "$TABLET_APP" "$TABLET_DEPT" "$PROVIDER" "$ENDPOINT" "$API_USER" "$API_PASS"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
