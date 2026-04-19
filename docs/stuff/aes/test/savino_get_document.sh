#!/bin/bash
#
# Test: Savino Get Document - Recupero documento
# Funzionalità: GET /api/aes/savino/document/{docId}
# Requisiti: Autenticazione JWT (modulo user), configurazione tablet Savino
# Descrizione: Recupera documento dal tablet Savino
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
DOC_ID="${TEST_DOC_ID:?Errore: TEST_DOC_ID richiesto}"
TABLET_ID="${TEST_TABLET_ID:?Errore: TEST_TABLET_ID richiesto}"

echo "=========================================="
echo "Test: Savino Get Document"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  Doc ID: $DOC_ID"
echo "  Tablet ID: $TABLET_ID"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/savino_get_document.sh" "$USERNAME" "$PASSWORD" "$DOC_ID" "$TABLET_ID"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
