#!/bin/bash
#
# Test: Hello - Messaggio di benvenuto
# Funzionalità: GET /api/home/hello
# Requisiti: Nessuno (endpoint pubblico)
# Descrizione: Recupera messaggio di benvenuto dal server
#

echo "=========================================="
echo "Test: Hello"
echo "=========================================="
echo ""

# Esegui CLI script
CLI_DIR="$(dirname "$0")/../cli"
"$CLI_DIR/hello.sh"

echo ""
echo "=========================================="
echo "Test completato"
echo "=========================================="
