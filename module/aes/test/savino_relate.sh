#!/bin/bash
#
# Test: Savino Relate - Creazione relazione documenti
# Funzionalità: POST /api/aes/savino/relate
# Requisiti: Autenticazione JWT (modulo user), configurazione tablet Savino
# Descrizione: Crea relazione tra due documenti su tablet Savino
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TABLET_ID="${TEST_TABLET_ID:?Errore: TEST_TABLET_ID richiesto}"
DOC_ID="${TEST_DOC_ID:?Errore: TEST_DOC_ID richiesto}"
RELATED_ID="${TEST_RELATED_ID:?Errore: TEST_RELATED_ID richiesto}"

echo "=========================================="
echo "Test: Savino Relate"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  Tablet ID: $TABLET_ID"
echo "  Doc ID: $DOC_ID"
echo "  Related ID: $RELATED_ID"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/savino_relate.sh" "$USERNAME" "$PASSWORD" "$TABLET_ID" "$DOC_ID" "$RELATED_ID"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
