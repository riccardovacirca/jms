#\!/bin/bash
# GET /api/home/hello - Messaggio di benvenuto
#
# Usage:
#   ./hello.sh
#
# Example:
#   ./hello.sh

set -e

API_BASE="${API_BASE:-http://localhost:8080}"

echo "[*] Richiesta messaggio di benvenuto..."
RESPONSE=$(curl -s -X GET "$API_BASE/api/home/hello" \
  -H "Content-Type: application/json")

# Output formattato
if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Risposta ricevuta con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[\!] Errore nella richiesta:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
