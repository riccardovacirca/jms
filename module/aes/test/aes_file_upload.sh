#\!/bin/bash
#
# Test: Upload File - Caricamento file temporaneo
# Funzionalità: POST /api/aes/file/upload
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Carica un file in storage temporaneo strutturato (YYYY/MM/subfolder)
#              con naming hash+timestamp, restituisce il path del file salvato
#

API_BASE="${API_BASE:-http://localhost:8080}"
USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TMP_DIR="/tmp/aes-cli-test"

mkdir -p "$TMP_DIR"

echo "=========================================="
echo "Test: Upload File"
echo "=========================================="
echo ""

# Step 1: Login e ottieni JWT
echo "[1/2] Login e ottenimento token JWT..."
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

# Step 2: Chiamata POST /api/aes/file/upload
echo "[2/2] Test upload file..."

# Crea un file di test
TEST_FILE="$TMP_DIR/test-document.txt"
echo "Contenuto di test - $(date)" > "$TEST_FILE"

UPLOAD_RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/aes/file/upload" \
  -F "file=@$TEST_FILE" \
  -F "subfolder=test-cli")

echo "$UPLOAD_RESPONSE" | grep -q '"err":false'
if [ $? -eq 0 ]; then
  echo "✓ Upload completato con successo"
  echo ""
  FILE_PATH=$(echo "$UPLOAD_RESPONSE" | grep -oP '"path":".*?"' | sed 's/"path":"\(.*\)"/\1/')
  echo "File salvato in: $FILE_PATH"
  echo ""
  echo "Response completo:"
  echo "$UPLOAD_RESPONSE" | jq . 2>/dev/null || echo "$UPLOAD_RESPONSE"

  # Salva il path per altri test
  echo "$FILE_PATH" > "$TMP_DIR/last-uploaded-file.txt"
else
  echo "❌ Errore nell'upload"
  echo "Response: $UPLOAD_RESPONSE"
  exit 1
fi

echo ""
echo "=========================================="
echo "Test completato con successo"
echo "=========================================="
