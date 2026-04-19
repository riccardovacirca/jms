#!/bin/bash
# GET /api/aes/savino/documents?tabletId=... - Elenca documenti disponibili su tablet Savino
#
# Usage:
#   ./savino_list_documents.sh USERNAME PASSWORD TABLET_ID
#
# Example:
#   ./savino_list_documents.sh admin@example.com password123 "tab_001"

set -e

USERNAME="${1:?Errore: USERNAME richiesto}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"
TABLET_ID="${3:?Errore: TABLET_ID richiesto}"

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

# Lista documenti
echo "[*] Recupero lista documenti..."
RESPONSE=$(curl -s -b "$TMP_DIR/cookies.txt" -X GET "$API_BASE/api/aes/savino/documents?tabletId=$TABLET_ID" \
  -H "Content-Type: application/json")

# Cleanup
rm -rf "$TMP_DIR"

# Output
if echo "$RESPONSE" | grep -q '"err":false'; then
  echo "[✓] Lista documenti recuperata con successo:"
  echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
else
  echo "[!] Errore nel recupero:"
  echo "$RESPONSE" | grep -oP '"log":"\K[^"]+' || echo "$RESPONSE"
  exit 1
fi
