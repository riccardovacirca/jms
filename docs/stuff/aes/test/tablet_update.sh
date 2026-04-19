#!/bin/bash
#
# Test: Tablet Update - Aggiornamento configurazione tablet
# Funzionalità: PUT /api/aes/tablets/{id}
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Aggiorna configurazione tablet esistente
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
ID="${TEST_ID:?Errore: TEST_ID richiesto}"
TABLET_NAME="${TEST_TABLET_NAME:-Updated Tablet Name}"
TABLET_APP="${TEST_TABLET_APP:-sales}"
TABLET_DEPT="${TEST_TABLET_DEPT:-commercial}"
ENDPOINT="${TEST_ENDPOINT:-https://api.conserva.cloud/api/v1}"
API_USER="${TEST_API_USER:-tablet1}"
API_PASS="${TEST_API_PASS:-newsecret}"

echo "=========================================="
echo "Test: Tablet Update"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  ID: $ID"
echo "  Tablet Name: $TABLET_NAME"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/tablet_update.sh" "$USERNAME" "$PASSWORD" "$ID" "$TABLET_NAME" "$TABLET_APP" "$TABLET_DEPT" "$ENDPOINT" "$API_USER" "$API_PASS"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
