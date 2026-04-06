#!/bin/bash
# Post-install script per il modulo user
# Crea gli account di bootstrap: root, admin, operator
# Argomenti: $1 = WORKSPACE, $2 = MODULE_KEY

set -e

echo "  Creazione account root (root@example.com)..."
cmd module cli user account-root-create root@example.com root123.

echo "  Login come root..."
cmd module cli user auth-login root root123.

echo "  Creazione account admin (admin@example.com)..."
cmd module cli user account_register admin admin@example.com admin123. admin

echo "  Creazione account operator (operator@example.com)..."
cmd module cli user account_register operator operator@example.com operator123. user

echo "  Bootstrap account completato."
