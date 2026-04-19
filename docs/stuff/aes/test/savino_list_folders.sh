#!/bin/bash
#
# Test: Savino List Folders - Lista folder tablet
# Funzionalità: GET /api/aes/savino/folders
# Requisiti: Autenticazione JWT (modulo user), configurazione tablet Savino
# Descrizione: Elenca folder disponibili su tablet Savino
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TABLET_ID="${TEST_TABLET_ID:?Errore: TEST_TABLET_ID richiesto}"

echo "=========================================="
echo "Test: Savino List Folders"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  Tablet ID: $TABLET_ID"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/savino_list_folders.sh" "$USERNAME" "$PASSWORD" "$TABLET_ID"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
