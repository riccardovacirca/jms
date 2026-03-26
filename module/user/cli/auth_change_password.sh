#!/bin/bash
# PUT /api/user/auth/change-password - Cambia password
#
# Usage:
#   ./auth_change_password.sh USERNAME OLD_PASSWORD NEW_PASSWORD
#
# Example:
#   ./auth_change_password.sh admin@example.com oldpass123 newpass123

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
OLD_PASSWORD="${2:?Errore: OLD_PASSWORD richiesta}"
NEW_PASSWORD="${3:?Errore: NEW_PASSWORD richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"
TMP_DIR="/tmp/user-cli-$$"
mkdir -p "$TMP_DIR"

# Login
echo "[*] Autenticazione in corso..."
LOGIN_RESPONSE=$(curl -s -c "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$OLD_PASSWORD\"}")

if ! echo "$LOGIN_RESPONSE" | grep -q "\"err\":false"; then
  echo "[!] Login fallito:"
  echo "$LOGIN_RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "Errore sconosciuto"
  rm -rf "$TMP_DIR"
  exit 1
fi

echo "[✓] Login riuscito"

# Change password
echo "[*] Cambio password..."

REQUEST_JSON=$(cat <<EOF_JSON
{
  "oldPassword": "$OLD_PASSWORD",
  "newPassword": "$NEW_PASSWORD"
}
EOF_JSON
)

RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X PUT "$API_BASE/api/user/auth/change-password" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Password cambiata con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nel cambio password:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
