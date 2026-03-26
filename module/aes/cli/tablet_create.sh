#!/bin/bash
# POST /api/aes/tablets - Crea nuova configurazione tablet
#
# Usage:
#   ./tablet_create.sh USERNAME PASSWORD ACCOUNT_ID TABLET_ID TABLET_NAME TABLET_APP TABLET_DEPT PROVIDER ENDPOINT API_USER API_PASS
#
# Example:
#   ./tablet_create.sh admin@example.com password123 1 "tab_001" "Tablet Vendite" "sales" "commercial" "savino" "https://api.conserva.cloud/api/v1" "tablet1" "secret"

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"
ACCOUNT_ID="${3:?Errore: ACCOUNT_ID richiesto}"
TABLET_ID="${4:?Errore: TABLET_ID richiesto}"
TABLET_NAME="${5:?Errore: TABLET_NAME richiesto}"
TABLET_APP="${6:?Errore: TABLET_APP richiesto}"
TABLET_DEPT="${7:?Errore: TABLET_DEPT richiesto}"
PROVIDER="${8:?Errore: PROVIDER richiesto (savino|namirial)}"
ENDPOINT="${9:?Errore: ENDPOINT richiesto}"
API_USER="${10:?Errore: API_USER richiesto}"
API_PASS="${11:?Errore: API_PASS richiesta}"

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

# Crea tablet con parametri
echo "[*] Creazione configurazione tablet..."

REQUEST_JSON=$(cat <<EOF
{
  "accountId": $ACCOUNT_ID,
  "tabletId": "$TABLET_ID",
  "tabletName": "$TABLET_NAME",
  "tabletApp": "$TABLET_APP",
  "tabletDepartment": "$TABLET_DEPT",
  "provider": "$PROVIDER",
  "endpoint": "$ENDPOINT",
  "username": "$API_USER",
  "password": "$API_PASS"
}
EOF
)

RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/aes/tablets" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Tablet creato con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nella creazione:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
