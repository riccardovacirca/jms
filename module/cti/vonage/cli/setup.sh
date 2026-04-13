#!/bin/bash
# Post-install script per il modulo cti/vonage
# Argomenti: $1 = WORKSPACE, $2 = MODULE_KEY

WORKSPACE="$1"

echo "  Installazione @vonage/client-sdk in gui/..."
cd "$WORKSPACE/gui" && npm install @vonage/client-sdk || exit 1

echo "  Installazione @vonage/cli globale..."
npm install -g @vonage/cli || exit 1
