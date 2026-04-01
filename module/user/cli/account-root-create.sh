#!/bin/bash
# POST /api/user/root - Crea l'account root (bootstrap iniziale)
#
# Endpoint di bootstrap senza autenticazione. Può essere invocato una sola volta.
# Restituisce errore se l'account root esiste già.
#
# Usage:
#   ./account-root-create.sh EMAIL PASSWORD
#   ./account-root-create.sh --help
#
# Example:
#   ./account-root-create.sh root@example.com rootpass123

set -e

# Help
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  cat << HELP
Usage: cmd module cli user account-root-create EMAIL PASSWORD

Crea l'account root iniziale (bootstrap del sistema).

Argomenti:
  EMAIL      Email dell'account root
  PASSWORD   Password dell'account root

Note:
  - Non richiede autenticazione (endpoint di bootstrap)
  - Può essere eseguito una sola volta
  - Restituisce errore se l'account root esiste già
  - L'account creato ha ruolo ROOT (massimi privilegi)

Esempi:
  cmd module cli user account-root-create root@example.com rootpass123
  cmd module cli user account-root-create admin@app.com SecureP@ss2024

Variabili ambiente:
  API_BASE  Base URL API (default: http://localhost:8080)
HELP
  exit 0
fi

EMAIL="${1:?Errore: EMAIL richiesta. Usa --help per maggiori informazioni}"
PASSWORD="${2:?Errore: PASSWORD richiesta. Usa --help per maggiori informazioni}"

API_BASE="${API_BASE:-http://localhost:8080}"

RESPONSE=$(curl -s -X POST "$API_BASE/api/user/root" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

# Verifica errori
if echo "$RESPONSE" | grep -q "\"err\":true"; then
  echo "$RESPONSE" | grep -oP "\"log\":\"\\K[^\"]+" || echo "$RESPONSE"
  exit 1
fi

# Output conciso
echo "out:"
echo "$RESPONSE" | python3 -c "import sys, json; r = json.load(sys.stdin); print('Account root creato con successo' if r.get('err') == False else r.get('log', 'Errore sconosciuto'))"
