#!/bin/bash
#
# Test: Tablet Get - Recupero dettaglio tablet
# Funzionalità: GET /api/aes/tablets/{tabletId}
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Recupera dettaglio configurazione tablet per ID tablet
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TABLET_ID="${TEST_TABLET_ID:?Errore: TEST_TABLET_ID richiesto}"

echo "=========================================="
echo "Test: Tablet Get"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  Tablet ID: $TABLET_ID"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/tablet_get.sh" "$USERNAME" "$PASSWORD" "$TABLET_ID"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
