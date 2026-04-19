#!/bin/bash
#
# Test: Savino Move Signed - Spostamento documenti firmati
# Funzionalità: POST /api/aes/savino/move-signed
# Requisiti: Autenticazione JWT (modulo user), configurazione tablet Savino
# Descrizione: Sposta documenti firmati dal folder firma al folder firmati
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TABLET_ID="${TEST_TABLET_ID:?Errore: TEST_TABLET_ID richiesto}"

echo "=========================================="
echo "Test: Savino Move Signed"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  Tablet ID: $TABLET_ID"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/savino_move_signed.sh" "$USERNAME" "$PASSWORD" "$TABLET_ID"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
