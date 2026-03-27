#!/bin/bash
# Crea l'account root (username=root, ruolo=root).
# Chiama POST /api/user/root — endpoint di bootstrap senza autenticazione.
# Può essere invocato una sola volta: restituisce errore se l'account root esiste già.
#
# Usage:
#   ./account_root.sh EMAIL PASSWORD
#
# Example:
#   ./account_root.sh root@example.com rootpass

set -e

EMAIL="${1:?Errore: EMAIL richiesta}"
PASSWORD="${2:?Errore: PASSWORD richiesta}"

API_BASE="${API_BASE:-http://localhost:8080}"

echo "[*] Creazione account root..."

RESPONSE=$(curl -s -X POST "$API_BASE/api/user/root" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

if echo "$RESPONSE" | grep -q "\"err\":false"; then
  echo "[✓] Account root creato con successo"
else
  echo "[!] Errore:"
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi
