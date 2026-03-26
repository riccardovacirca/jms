#!/bin/bash
#
# Test: auth logout - Auto-generated test
# Funzionalità: See CLI script for details
# Requisiti: Autenticazione (tranne generate_password)
# Descrizione: Invoca script CLI con parametri da variabili d'ambiente

USERNAME="${TEST_USERNAME:-test}"
PASSWORD="${TEST_PASSWORD:-test}"

echo "=========================================="
echo "Test: auth_logout"
echo "=========================================="
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"

case "auth_logout" in
  auth_generate_password)
    "$CLI_DIR/auth_logout.sh"
    ;;
  auth_change_password)
    NEW_PASSWORD="${TEST_NEW_PASSWORD:-newpass}"
    "$CLI_DIR/auth_logout.sh" "$USERNAME" "$PASSWORD" "$NEW_PASSWORD"
    ;;
  account_register)
    NEW_USER="${TEST_NEW_USER:-newuser}"
    NEW_EMAIL="${TEST_NEW_EMAIL:-new@example.com}"
    NEW_PASS="${TEST_NEW_PASS:-pass123}"
    RUOLO="${TEST_RUOLO:-user}"
    "$CLI_DIR/auth_logout.sh" "$USERNAME" "$PASSWORD" "$NEW_USER" "$NEW_EMAIL" "$NEW_PASS" "$RUOLO"
    ;;
  *)
    "$CLI_DIR/auth_logout.sh" "$USERNAME" "$PASSWORD"
    ;;
esac

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
