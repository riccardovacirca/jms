#\!/bin/bash
# GET /api/user/auth/session - Ottieni sessione corrente
#
# Usage:
#   ./auth_session.sh USERNAME PASSWORD
#
# Example:
#   ./auth_session.sh admin@example.com password123

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"
TMP_DIR="/tmp/user-cli-$$"
mkdir -p "$TMP_DIR"

# Login
echo "[*] Autenticazione in corso..."
LOGIN_RESPONSE=$(curl -s -c "$TMP_DIR/cookies.txt" -X POST "$API_BASE/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")

if \! echo "$LOGIN_RESPONSE" | grep -q "\"err\":false"; then
  echo "[\!] Login fallito:"
  echo "$LOGIN_RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "Errore sconosciuto"
  rm -rf "$TMP_DIR"
  exit 1
fi

echo "[✓] Login riuscito"

# Get session
echo "[*] Recupero sessione..."
RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X GET "$API_BASE/api/user/auth/session" \
  -H "Content-Type: application/json")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Sessione recuperata:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[\!] Errore nel recupero sessione:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
