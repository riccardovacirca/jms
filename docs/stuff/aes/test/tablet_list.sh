#!/bin/bash
#
# Test: Tablet List - Lista configurazioni tablet
# Funzionalità: GET /api/aes/tablets
# Requisiti: Autenticazione JWT (modulo user)
# Descrizione: Recupera lista configurazioni tablet con filtri opzionali (accountId, provider)
#

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"
ACCOUNT_ID="${TEST_ACCOUNT_ID:-}"
PROVIDER="${TEST_PROVIDER:-}"

echo "=========================================="
echo "Test: Tablet List"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo "  Account ID: ${ACCOUNT_ID:-<tutti>}"
echo "  Provider: ${PROVIDER:-<tutti>}"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/tablet_list.sh" "$USERNAME" "$PASSWORD" "$ACCOUNT_ID" "$PROVIDER"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
