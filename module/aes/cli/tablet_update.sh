#!/bin/bash
# PUT /api/aes/tablets/{id} - Aggiorna configurazione tablet
#
# Usage:
#   ./tablet_update.sh USERNAME PASSWORD ID TABLET_NAME TABLET_APP TABLET_DEPT ENDPOINT API_USER API_PASS
#
# Example:
#   ./tablet_update.sh admin@example.com password123 1 "Tablet Vendite Updated" "sales" "commercial" "https://api.conserva.cloud/api/v1" "tablet1" "newsecret"

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"
ID="${3:?Errore: ID richiesto}"
TABLET_NAME="${4:?Errore: TABLET_NAME richiesto}"
TABLET_APP="${5:?Errore: TABLET_APP richiesto}"
TABLET_DEPT="${6:?Errore: TABLET_DEPT richiesto}"
ENDPOINT="${7:?Errore: ENDPOINT richiesto}"
API_USER="${8:?Errore: API_USER richiesto}"
API_PASS="${9:?Errore: API_PASS richiesta}"

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

# Aggiorna tablet
echo "[*] Aggiornamento configurazione tablet..."

REQUEST_JSON=$(cat <<EOF
{
  "tabletName": "$TABLET_NAME",
  "tabletApp": "$TABLET_APP",
  "tabletDepartment": "$TABLET_DEPT",
  "endpoint": "$ENDPOINT",
  "username": "$API_USER",
  "password": "$API_PASS"
}
EOF
)

RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X PUT "$API_BASE/api/aes/tablets/$ID" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Tablet aggiornato con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nell'aggiornamento:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
