#!/bin/bash
#
# Test: Auth Login - Login utente
# Funzionalità: POST /api/user/auth/login
# Requisiti: Nessuno
# Descrizione: Effettua login e ottiene token JWT

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"

echo "=========================================="
echo "Test: Auth Login"
echo "=========================================="
echo ""
echo "Parametri:"
echo "  Username: $USERNAME"
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/auth-login.sh" "$USERNAME" "$PASSWORD"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
