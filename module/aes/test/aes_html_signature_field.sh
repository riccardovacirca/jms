#!/bin/bash
#
# Test: HTML Signature Field - Conversione HTML → PDF con campo firma
# Funzionalità: POST /api/aes/html/signature/field
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Converte HTML in PDF usando Flying Saucer, inserisce campo firma
#              e restituisce l'hash del documento salvato in documents/yyyy/mm/dd/partner/hash.pdf
#

API_BASE="${API_BASE:-http://localhost:8080}"
USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TMP_DIR="/tmp/aes-cli-test"

mkdir -p "$TMP_DIR"

echo "=========================================="
echo "Test: HTML Signature Field"
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

# Step 2: Chiamata POST /api/aes/html/signature/field
echo "[2/3] Test conversione HTML → PDF con firma..."

# Crea JSON file con HTML escaped
cat > "$TMP_DIR/request.json" << 'JSON_EOF'
{
  "html": "<!DOCTYPE html><html><head><meta charset=\"UTF-8\" /><style>body { font-family: Arial, sans-serif; padding: 20px; } h1 { color: #333; } .firma { margin-top: 50px; }</style></head><body><h1>Documento di Test</h1><p>Questo è un documento di test per la conversione HTML → PDF.</p><p>Il sistema inserirà un campo firma nel punto indicato dal placeholder.</p><div class=\"firma\"><p>Firma:</p><p>FIRMA</p></div></body></html>",
  "placeholder": "FIRMA",
  "width": 150,
  "height": 50,
  "partner": "savino"
}
JSON_EOF

RESPONSE=$(curl -s -X POST "$API_BASE/api/aes/html/signature/field" \
  -H "Content-Type: application/json" \
  -b "$TMP_DIR/cookies.txt" \
  -d @"$TMP_DIR/request.json")

echo "$RESPONSE" | grep -q '"err":false'
if [ $? -eq 0 ]; then
  echo "✓ Conversione HTML → PDF completata con successo"
  echo ""
  HASH=$(echo "$RESPONSE" | grep -oP '"hash":"\K[^"]+')
  REPLACED=$(echo "$RESPONSE" | grep -oP '"replaced":\K[0-9]+')
  echo "Hash documento: $HASH"
  echo "Placeholder sostituiti: $REPLACED"
  echo ""

  # Salva hash per altri test
  echo "$HASH" > "$TMP_DIR/last-html-hash.txt"
else
  echo "❌ Errore nella conversione HTML → PDF"
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
