#\!/bin/bash
# POST /api/user/auth/login - Login utente
#
# Usage:
#   ./auth_login.sh USERNAME PASSWORD
#
# Example:
#   ./auth_login.sh admin@example.com password123

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"
TMP_DIR="/tmp/user-cli-$$"
mkdir -p "$TMP_DIR"

echo "[*] Login in corso..."
RESPONSE=$(curl -s -c "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Login riuscito:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[\!] Login fallito:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
