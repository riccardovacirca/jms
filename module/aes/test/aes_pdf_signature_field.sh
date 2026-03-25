#!/bin/bash
#
# Test: PDF Signature Field - Inserimento campo firma in PDF
# Funzionalità: POST /api/aes/pdf/signature/field
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Carica un PDF base64, inserisce campo firma con placeholder
#              e restituisce l'hash del documento salvato in documents/yyyy/mm/dd/partner/hash.pdf
#

API_BASE="${API_BASE:-http://localhost:8080}"
USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TMP_DIR="/tmp/aes-cli-test"

mkdir -p "$TMP_DIR"

echo "=========================================="
echo "Test: PDF Signature Field"
echo "=========================================="
echo ""

# Step 1: Login e ottieni JWT
echo "[1/3] Login e ottenimento token JWT..."
LOGIN_RESPONSE=$(curl -s -c "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

# Estrai access_token dal file cookies
TOKEN=$(grep -oP 'access_token\s+\K[^\s]+' "$TMP_DIR/cookies.txt" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ Errore: Login fallito o token non trovato"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "✓ Token ottenuto: ${TOKEN:0:20}..."
echo ""

# Step 2: Chiamata POST /api/aes/pdf/signature/field
echo "[2/3] Test inserimento campo firma in PDF..."

# PDF minimale base64 (PDF vuoto 1 pagina)
PDF_BASE64="JVBERi0xLjQKJcOkw7zDtsOfCjIgMCBvYmoKPDwvTGVuZ3RoIDMgMCBSL0ZpbHRlci9GbGF0ZURlY29kZT4+CnN0cmVhbQp4nDPQM1Qo5ypUMABCMyBkBA0AGpMBlgplbmRzdHJlYW0KZW5kb2JqCgozIDAgb2JqCjI4CmVuZG9iagoKNSAwIG9iago8PC9MZW5ndGggNiAwIFIvRmlsdGVyL0ZsYXRlRGVjb2RlPj4Kc3RyZWFtCnicK+QK5AIAApwA3AplbmRzdHJlYW0KZW5kb2JqCgo2IDAgb2JqCjkKZW5kb2JqCgo3IDAgb2JqCjw8L1R5cGUvUGFnZS9QYXJlbnQgNCAwIFIvQ29udGVudHMgNSAwIFI+PgplbmRvYmoKCjQgMCBvYmoKPDwvVHlwZS9QYWdlcy9Db3VudCAxL0tpZHNbNyAwIFJdPj4KZW5kb2JqCgoxIDAgb2JqCjw8L1R5cGUvQ2F0YWxvZy9QYWdlcyA0IDAgUj4+CmVuZG9iagoKeHJlZgowIDgKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMzE1IDAwMDAwIG4gCjAwMDAwMDAwMTkgMDAwMDAgbiAKMDAwMDAwMDEwOSAwMDAwMCBuIAowMDAwMDAwMjYzIDAwMDAwIG4gCjAwMDAwMDAxMjggMDAwMDAgbiAKMDAwMDAwMDIxMCAwMDAwMCBuIAowMDAwMDAwMjI4IDAwMDAwIG4gCnRyYWlsZXIKPDwvU2l6ZSA4L1Jvb3QgMSAwIFI+PgpzdGFydHhyZWYKMzY0CiUlRU9G"

REQUEST_JSON=$(cat <<REQUEST_EOF
{
  "pdfBase64": "$PDF_BASE64",
  "placeholder": "FIRMA",
  "width": 150,
  "height": 50,
  "partner": "savino"
}
REQUEST_EOF
)

RESPONSE=$(curl -s -X POST "$API_BASE/api/aes/pdf/signature/field" \
  -H "Content-Type: application/json" \
  -b "$TMP_DIR/cookies.txt" \
  -d "$REQUEST_JSON")

echo "$RESPONSE" | grep -q '"err":false'
if [ $? -eq 0 ]; then
  echo "✓ Inserimento campo firma completato con successo"
  echo ""
  HASH=$(echo "$RESPONSE" | grep -oP '"hash":"\K[^"]+')
  REPLACED=$(echo "$RESPONSE" | grep -oP '"replaced":\K[0-9]+')
  echo "Hash documento: $HASH"
  echo "Placeholder sostituiti: $REPLACED"
  echo ""

  # Salva hash per altri test
  echo "$HASH" > "$TMP_DIR/last-pdf-hash.txt"
else
  echo "❌ Errore nell'inserimento campo firma"
  echo "Response: $RESPONSE"
  exit 1
fi

# Step 3: Verifica esistenza file
echo "[3/3] Verifica file salvato..."
YEAR=$(date +%Y)
MONTH=$(date +%m)
DAY=$(date +%d)
EXPECTED_PATH="/workspace/storage/aes/documents/$YEAR/$MONTH/$DAY/savino/$HASH.pdf"

if [ -f "$EXPECTED_PATH" ]; then
  echo "✓ File salvato correttamente in: $EXPECTED_PATH"
  FILE_SIZE=$(stat -f%z "$EXPECTED_PATH" 2>/dev/null || stat -c%s "$EXPECTED_PATH" 2>/dev/null)
  echo "Dimensione: $FILE_SIZE bytes"
else
  echo "❌ File non trovato in: $EXPECTED_PATH"
  exit 1
fi

echo ""
echo "=========================================="
echo "Test completato con successo"
echo "=========================================="
