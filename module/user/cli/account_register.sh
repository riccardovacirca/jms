#!/bin/bash
# POST /api/user/accounts - Registra nuovo account
#
# Usage:
#   ./account_register.sh ADMIN_USER ADMIN_PASS USERNAME EMAIL PASSWORD RUOLO
#
# Example:
#   ./account_register.sh admin@example.com admin123 newuser new@example.com pass123 user

set -e

ADMIN_USER="${1:?Errore: ADMIN_USER richiesto}"
ADMIN_PASS="${2:?Errore: ADMIN_PASS richiesta}"
USERNAME="${3:?Errore: USERNAME richiesto}"
EMAIL="${4:?Errore: EMAIL richiesta}"
PASSWORD="${5:?Errore: PASSWORD richiesta}"
RUOLO="${6:?Errore: RUOLO richiesto}"

API_BASE="${API_BASE:-http://localhost:8080}"
TMP_DIR="/tmp/user-cli-$$"
mkdir -p "$TMP_DIR"

# Login
echo "[*] Autenticazione admin..."
LOGIN_RESPONSE=$(curl -s -c "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")

if ! echo "$LOGIN_RESPONSE" | grep -q "\"err\":false"; then
  echo "[!] Login fallito:"
  echo "$LOGIN_RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "Errore sconosciuto"
  rm -rf "$TMP_DIR"
  exit 1
fi

echo "[✓] Login riuscito"

# Register new account
echo "[*] Registrazione nuovo account..."

REQUEST_JSON=$(cat <<EOF_JSON
{
  "username": "$USERNAME",
  "email": "$EMAIL",
  "password": "$PASSWORD",
  "ruolo": "$RUOLO"
}
EOF_JSON
)

RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/accounts" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_JSON")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Account registrato con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nella registrazione:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
