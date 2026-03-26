#\!/bin/bash
# GET /api/user/accounts - Lista tutti gli account
#
# Usage:
#   ./account_list.sh ADMIN_USER ADMIN_PASS
#
# Example:
#   ./account_list.sh admin@example.com admin123

set -e

ADMIN_USER="${1:?Errore: ADMIN_USER richiesto}"
ADMIN_PASS="${2:?Errore: ADMIN_PASS richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"
TMP_DIR="/tmp/user-cli-$$"
mkdir -p "$TMP_DIR"

# Login
echo "[*] Autenticazione admin..."
LOGIN_RESPONSE=$(curl -s -c "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")

if \! echo "$LOGIN_RESPONSE" | grep -q "\"err\":false"; then
  echo "[\!] Login fallito:"
  echo "$LOGIN_RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "Errore sconosciuto"
  rm -rf "$TMP_DIR"
  exit 1
fi

echo "[✓] Login riuscito"

# List accounts
echo "[*] Recupero lista account..."
RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X GET "$API_BASE/api/user/accounts" \
  -H "Content-Type: application/json")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Lista account recuperata:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[\!] Errore nel recupero lista:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
