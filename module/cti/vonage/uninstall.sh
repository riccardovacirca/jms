#!/bin/bash
# Pre-remove script per il modulo cti/vonage
# Argomenti: $1 = WORKSPACE, $2 = MODULE_KEY

WORKSPACE="$1"

echo "  Rimozione @vonage/client-sdk da gui/..."
cd "$WORKSPACE/gui" && npm uninstall @vonage/client-sdk || exit 1

echo "  Rimozione @vonage/cli globale..."
npm uninstall -g @vonage/cli || exit 1
