#\!/bin/bash
#
# Test: Download File - Recupero file temporaneo
# Funzionalità: GET /api/aes/file/download
# Requisiti: Autenticazione JWT (modulo user), file precedentemente caricato
# Descrizione: Scarica un file dallo storage temporaneo dato il path.
#              Se non viene fornito un path, tenta di usare l'ultimo file
#              caricato dal test di upload.
#

API_BASE="${API_BASE:-http://localhost:8080}"
USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
TMP_DIR="/tmp/aes-cli-test"
FILE_PATH="$1"

mkdir -p "$TMP_DIR"

echo "=========================================="
echo "Test: Download File"
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

# Determina il path del file
if [ -z "$FILE_PATH" ]; then
  if [ -f "$TMP_DIR/last-uploaded-file.txt" ]; then
    FILE_PATH=$(cat "$TMP_DIR/last-uploaded-file.txt")
    echo "ℹ Uso file dall'ultimo upload: $FILE_PATH"
  else
    echo "❌ Errore: Nessun path fornito e nessun file dall'ultimo upload"
    echo "Uso: $0 [path]"
    exit 1
  fi
else
  echo "ℹ File path fornito: $FILE_PATH"
fi
echo ""

# Step 2: Chiamata GET /api/aes/file/download
echo "[2/2] Test download file..."

DOWNLOAD_FILE="$TMP_DIR/downloaded-file"
HTTP_CODE=$(curl -s -w "%{http_code}" -o "$DOWNLOAD_FILE" \
  -b "$TMP_DIR/cookies.txt" \
  "$API_BASE/api/aes/file/download?path=$FILE_PATH")

if [ "$HTTP_CODE" = "200" ]; then
  echo "✓ Download completato con successo"
  echo ""
  echo "Contenuto del file:"
  echo "---"
  cat "$DOWNLOAD_FILE"
  echo ""
  echo "---"
else
  echo "❌ Errore nel download (HTTP $HTTP_CODE)"
  cat "$DOWNLOAD_FILE"
  exit 1
fi

echo ""
echo "=========================================="
echo "Test completato con successo"
echo "=========================================="
