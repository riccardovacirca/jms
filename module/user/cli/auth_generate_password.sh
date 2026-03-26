#\!/bin/bash
# GET /api/user/auth/generate-password - Genera password casuale
#
# Usage:
#   ./auth_generate_password.sh
#
# Example:
#   ./auth_generate_password.sh

set -e

API_BASE="${API_BASE:-http://localhost:8080}"

echo "[*] Generazione password..."
RESPONSE=$(curl -s -X GET "$API_BASE/api/user/auth/generate-password" \
  -H "Content-Type: application/json")

# Output
if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Password generata:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[\!] Errore nella generazione:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
