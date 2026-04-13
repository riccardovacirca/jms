#!/bin/bash
#
# AES Module - Post-Install Script
# Eseguito automaticamente da 'cmd module import aes'
#
# 1. Crea la struttura directory per lo storage dei file del modulo AES
# 2. Crea account di test (username: test, password: test) per testing
#

set -e

WORKSPACE="${1:-/workspace}"
MODULE_KEY="${2:-aes}"

echo ""
echo "[aes] Post-install: creazione struttura storage..."

# Crea directory storage con sottocartelle
mkdir -p "$WORKSPACE/storage/aes"/{tmp,documents}

# Imposta permessi (777 per development, in produzione usare ownership appropriato)
chmod -R 777 "$WORKSPACE/storage/aes" 2>/dev/null || true

echo "  ✓ $WORKSPACE/storage/aes/tmp/"
echo "  ✓ $WORKSPACE/storage/aes/documents/"
echo ""

# Crea account di test per CLI testing
echo "[aes] Post-install: creazione account di test (test/test)..."

# Hash PBKDF2 per password "test" (salt:hash base64)
HASH="P/LYd1gx/5l+e126XEx1aQ==:06QFVZ9DAUfHzFSljvgOcUBc+qw19kT0RJ/+zFdkgkE="

# Inserisci account solo se non esiste già
"$WORKSPACE/bin/cmd" db -c "
INSERT INTO accounts (username, password_hash, ruolo, must_change_password, attivo)
VALUES ('test', '$HASH', 'admin', false, true)
ON CONFLICT (username) DO NOTHING;
" > /dev/null 2>&1 || true

echo "  ✓ Account test creato (username: test, password: test, ruolo: admin)"
echo ""
echo "[aes] Post-install completato"
echo ""
