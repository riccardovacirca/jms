#!/bin/bash
#
# Test: Savino Require Signature - Invio documento alla firma
# Funzionalità: POST /api/aes/savino/require-signature
# Requisiti: Autenticazione JWT (modulo user), configurazione tablet Savino
# Descrizione: Invia documento alla firma remota su tablet Savino
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
PDF_FILE="${TEST_PDF_FILE:-/tmp/test-document.pdf}"
FILENAME="${TEST_FILENAME:-test-document.pdf}"
DOCTYPE_ID="${TEST_DOCTYPE_ID:-CONTRATTO}"
TABLET_ID="${TEST_TABLET_ID:?Errore: TEST_TABLET_ID richiesto}"
METADATA_JSON="${TEST_METADATA_JSON:-{}}"

echo "=========================================="
echo "Test: Savino Require Signature"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  PDF File: $PDF_FILE"
echo "  Filename: $FILENAME"
echo "  Doc Type: $DOCTYPE_ID"
echo "  Tablet ID: $TABLET_ID"
echo ""

# Crea PDF di test se non esiste
if [ ! -f "$PDF_FILE" ]; then
  echo "[*] Creazione PDF di test..."
  mkdir -p "$(dirname "$PDF_FILE")"
  echo "Test PDF content - $(date)" > "${PDF_FILE%.pdf}.txt"
  # Nota: questo crea un txt, non un vero PDF. Per un test reale serve un PDF valido.
fi

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/savino_require_signature.sh" "$USERNAME" "$PASSWORD" "$PDF_FILE" "$FILENAME" "$DOCTYPE_ID" "$TABLET_ID" "$METADATA_JSON"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
