#!/bin/bash
#
# AES Module - Pre-Remove Script
# Eseguito automaticamente da 'cmd module remove --name aes'
#
# ATTENZIONE: Questo script NON rimuove automaticamente lo storage per evitare
# perdita accidentale di dati. L'amministratore deve decidere se:
# - Mantenere storage/aes/ per backup
# - Rimuovere manualmente storage/aes/ se non più necessario
#

set -e

WORKSPACE="${1:-/workspace}"
MODULE_KEY="${2:-aes}"

echo ""
echo "[aes] Pre-remove: verifica storage..."
echo ""

if [ -d "$WORKSPACE/storage/aes" ]; then
    FILE_COUNT=$(find "$WORKSPACE/storage/aes" -type f 2>/dev/null | wc -l)

    if [ "$FILE_COUNT" -gt 0 ]; then
        echo "  ⚠  ATTENZIONE: Trovati $FILE_COUNT file in storage/aes/"
        echo ""
        echo "  La directory storage/aes/ NON viene rimossa automaticamente."
        echo "  Se vuoi eliminarla manualmente:"
        echo ""
        echo "    rm -rf $WORKSPACE/storage/aes"
        echo ""
        echo "  Oppure spostala in backup:"
        echo ""
        echo "    mv $WORKSPACE/storage/aes $WORKSPACE/storage/aes.backup-\$(date +%Y%m%d)"
        echo ""
    else
        echo "  ℹ  Storage vuoto: $WORKSPACE/storage/aes/"
        echo "  Puoi rimuoverlo manualmente con:"
        echo ""
        echo "    rm -rf $WORKSPACE/storage/aes"
        echo ""
    fi
else
    echo "  ℹ  Storage non presente (niente da rimuovere)"
    echo ""
fi

echo "[aes] Pre-remove completato"
echo ""
