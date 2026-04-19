#!/bin/bash
#
# Test: Tablet Delete - Disabilitazione configurazione tablet
# Funzionalità: DELETE /api/aes/tablets/{id}
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Disabilita configurazione tablet (soft delete)
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
ID="${TEST_ID:?Errore: TEST_ID richiesto}"

echo "=========================================="
echo "Test: Tablet Delete"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  ID: $ID"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/tablet_delete.sh" "$USERNAME" "$PASSWORD" "$ID"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
