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

RESPONSE=$(curl -s -X GET "$API_BASE/api/home/hello" \
  -H "Content-Type: application/json")

# Verifica errori
if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi

# Estrae e stampa solo il campo "out"
echo "out:"
echo "$RESPONSE" | grep -oP "\"out\":\"\\K[^\"]+"
