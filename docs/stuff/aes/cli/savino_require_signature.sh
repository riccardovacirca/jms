#!/bin/bash
# POST /api/aes/savino/require-signature - Invia documento alla firma remota su tablet Savino
#
# Usage:
#   ./savino_require_signature.sh USERNAME PASSWORD PDF_FILE FILENAME DOCTYPE_ID TABLET_ID [METADATA_JSON]
#
# Example:
#   ./savino_require_signature.sh admin@example.com password123 contratto.pdf "contratto.pdf" "CONTRATTO" "tab_001" '{"id":"123","customer":"ACME"}'
#   ./savino_require_signature.sh admin@example.com password123 doc.pdf "doc.pdf" "CONTRATTO" "tab_001"

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"
PDF_FILE="${3:?Errore: PDF_FILE richiesto}"
FILENAME="${4:?Errore: FILENAME richiesto}"
DOCTYPE_ID="${5:?Errore: DOCTYPE_ID richiesto}"
TABLET_ID="${6:?Errore: TABLET_ID richiesto}"
METADATA_JSON="${7:-{}}"

API_BASE="${API_BASE:-http://localhost:8080}"
TMP_DIR="/tmp/aes-cli-$$"
mkdir -p "$TMP_DIR"

# Verifica esistenza file PDF
if [ ! -f "$PDF_FILE" ]; then
  echo "[!] File non trovato: $PDF_FILE"
  exit 1
fi

# Login
echo "[*] Autenticazione in corso..."
LOGIN_RESPONSE=$(curl -s -c "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

if ! echo "$LOGIN_RESPONSE" | grep -q '"err":false'; then
  echo "[!] Login fallito:"
  echo "$LOGIN_RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "Errore sconosciuto"
  rm -rf "$TMP_DIR"
  exit 1
fi

echo "[✓] Login riuscito"

# Codifica PDF in base64
echo "[*] Codifica PDF in base64..."
PDF_BASE64=$(base64 -w 0 "$PDF_FILE")

# Richiedi firma
echo "[*] Invio documento alla firma..."

REQUEST_JSON=$(cat <<EOF
{
  "pdfBase64": "$PDF_BASE64",
  "filename": "$FILENAME",
  "docTypeId": "$DOCTYPE_ID",
  "tabletId": "$TABLET_ID",
  "metadata": $METADATA_JSON
}
EOF
)

RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/aes/savino/require-signature" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Documento inviato alla firma con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nell'invio:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
