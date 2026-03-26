#!/bin/bash
# POST /api/aes/savino/relate - Crea relazione tra due documenti su tablet Savino
#
# Usage:
#   ./savino_relate.sh USERNAME PASSWORD TABLET_ID DOC_ID RELATED_ID
#
# Example:
#   ./savino_relate.sh admin@example.com password123 "tab_001" "doc-123" "doc-456"

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"
TABLET_ID="${3:?Errore: TABLET_ID richiesto}"
DOC_ID="${4:?Errore: DOC_ID richiesto}"
RELATED_ID="${5:?Errore: RELATED_ID richiesto}"

API_BASE="${API_BASE:-http://localhost:8080}"
TMP_DIR="/tmp/aes-cli-$$"
mkdir -p "$TMP_DIR"

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

# Crea relazione
echo "[*] Creazione relazione tra documenti..."

REQUEST_JSON=$(cat <<EOF
{
  "tabletId": "$TABLET_ID",
  "docId": "$DOC_ID",
  "relatedId": "$RELATED_ID"
}
EOF
)

RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/aes/savino/relate" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Relazione creata con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nella creazione relazione:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
