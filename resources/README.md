# Resources Directory

Questa directory contiene i file gestiti dai moduli dell'applicazione.

## Struttura

Ogni modulo che gestisce file (upload/download/storage) deve creare una sottodirectory
con il proprio nome:

```
resources/
├── README.md
├── <module-name>/
│   ├── tmp/         # File temporanei
│   ├── documents/   # Documenti permanenti
│   ├── uploads/     # Upload in ingresso
│   └── downloads/   # File preparati per download
```

## Configurazione

I path sono configurabili in `config/application.properties`:

```properties
# Esempio per modulo aes
aes.resources.base=/app/resources/aes
aes.resources.tmp=/app/resources/aes/tmp
aes.resources.documents=/app/resources/aes/documents
```

## Best Practices

1. **Separazione per modulo**: ogni modulo usa solo la propria sottodirectory
2. **Path configurabili**: non hardcodare path assoluti, usa Config
3. **Cleanup automatico**: implementa job schedulati per pulizia file temporanei
4. **Naming convention**: usa hash+timestamp per file temporanei
5. **Sicurezza**: valida sempre i path per prevenire directory traversal

## Note

- Questa directory NON è inclusa nel JAR (esterna all'applicazione)
- In produzione, configurare mount point separato per storage
- Backup regolari necessari per `documents/`, non per `tmp/`
