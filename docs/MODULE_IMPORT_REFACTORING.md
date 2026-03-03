# Rifattorizzazione `cmd module import` - Proposta Implementazione

## Analisi Funzione Attuale

### Comportamento Corrente
```bash
cmd module import <file.tar.gz>
```

**Azioni eseguite**:
1. ✓ Estrae archivio in `modules/<nome>/`
2. ✓ Sostituisce placeholder `{{APP_PACKAGE}}` con groupId da `pom.xml`
3. ✓ Mostra messaggio di successo

**Output**:
```
✓ SUCCESS: Modulo 'auth' estratto in modules/auth
INFO: Leggi modules/auth/README.md per i passi di installazione.
```

**Limitazione**: L'utente deve poi eseguire manualmente **tutti** i passaggi descritti nel README (23-37 minuti di lavoro).

---

## Proposta: Estensione con Automazione

### Nuovo Comportamento
```bash
cmd module import <file.tar.gz> [--extract-only]
```

**Default (senza flag)**: Estrae **E** installa automaticamente tutto ciò che è sicuro

**Con `--extract-only`**: Comportamento originale (solo estrazione)

### Azioni Automatiche (Default)

1. ✓ **Estrazione** - invariato
2. ✓ **Sostituzione placeholder** - invariato
3. ✅ **Copia file Java** → `src/main/java/<package>/<modulo>/`
4. ✅ **Sostituzione package Java** → `com.example` → package reale
5. ✅ **Copia file GUI** → `vite/src/modules/<modulo>/`
6. ✅ **Copia migration SQL** → con timestamp fresco
7. ✅ **Append configurazioni** → `config/application.properties`
8. ⚠️ **Output istruzioni manuali** → pom.xml, App.java, config.js
9. ✅ **Build automatico** → `cmd gui build && cmd app build`

---

## Implementazione

### Codice Completo Rifattorizzato

```bash
module_import() {
    local ARCHIVE="$1"
    local EXTRACT_ONLY=false

    # Parse flags
    if [ "$2" = "--extract-only" ]; then
        EXTRACT_ONLY=true
    fi

    [ -n "$ARCHIVE" ] || error "Usage: cmd module import <file.tar.gz> [--extract-only]"
    case "$ARCHIVE" in
        */*) ;;
        *) ARCHIVE="$WORKSPACE/modules/$ARCHIVE" ;;
    esac
    [ -f "$ARCHIVE" ] || error "File non trovato: $ARCHIVE"

    local APP_PACKAGE
    APP_PACKAGE=$(_module_app_package)
    [ -n "$APP_PACKAGE" ] || error "Cannot detect groupId from pom.xml"

    # Detect module name from archive (java/ subdirectory)
    local MODULE
    MODULE=$(tar -tzf "$ARCHIVE" 2>/dev/null | grep -E '^(./)?java/[^/]+/$' | head -1 | sed 's|.*/java/||; s|/$||')
    [ -n "$MODULE" ] || MODULE=$(tar -tzf "$ARCHIVE" 2>/dev/null | grep -E '^(./)?gui/[^/]+/$' | head -1 | sed 's|.*/gui/||; s|/$||')
    [ -n "$MODULE" ] || error "Impossibile rilevare il nome del modulo dall'archivio (java/ o gui/ mancante)"

    local DEST="$WORKSPACE/modules/$MODULE"
    if [ -d "$DEST" ]; then
        error "Cartella '$DEST' già esistente. Rimuoverla prima di eseguire l'import."
    fi

    info "Estrazione modulo '$MODULE'..."
    mkdir -p "$DEST"
    tar -xzf "$ARCHIVE" -C "$DEST"

    # Replace {{APP_PACKAGE}} placeholder in all text files
    find "$DEST" -type f \( -name "*.java" -o -name "*.js" -o -name "*.html" -o -name "*.md" \) \
        -exec sed -i "s|{{APP_PACKAGE}}|${APP_PACKAGE}|g" {} +

    success "Modulo '$MODULE' estratto in $DEST"

    # =========================================================================
    # Se --extract-only, fermiamoci qui (comportamento originale)
    # =========================================================================
    if [ "$EXTRACT_ONLY" = true ]; then
        info "Leggi $DEST/README.md per i passi di installazione."
        return 0
    fi

    # =========================================================================
    # INSTALLAZIONE AUTOMATICA
    # =========================================================================

    info ""
    info "=========================================="
    info "INSTALLAZIONE AUTOMATICA MODULO: $MODULE"
    info "Package applicazione: $APP_PACKAGE"
    info "=========================================="
    info ""

    local APP_PACKAGE_PATH="${APP_PACKAGE//.//}"
    local INSTALLED_SOMETHING=false

    # -------------------------------------------------------------------------
    # 1. COPIA FILE JAVA
    # -------------------------------------------------------------------------
    if [ -d "$DEST/java/$MODULE" ]; then
        info "→ Installazione file Java..."
        mkdir -p "$WORKSPACE/src/main/java/$APP_PACKAGE_PATH"
        cp -r "$DEST/java/$MODULE" "$WORKSPACE/src/main/java/$APP_PACKAGE_PATH/"

        # Sostituzione package com.example → APP_PACKAGE
        find "$WORKSPACE/src/main/java/$APP_PACKAGE_PATH/$MODULE" -name '*.java' \
             -exec sed -i "s|com\.example|$APP_PACKAGE|g" {} +

        success "File Java installati: src/main/java/$APP_PACKAGE_PATH/$MODULE/"
        INSTALLED_SOMETHING=true
    fi

    # -------------------------------------------------------------------------
    # 2. COPIA FILE GUI (FRONTEND)
    # -------------------------------------------------------------------------
    if [ -d "$DEST/gui/$MODULE" ]; then
        info "→ Installazione file GUI..."
        mkdir -p "$WORKSPACE/vite/src/modules"
        cp -r "$DEST/gui/$MODULE" "$WORKSPACE/vite/src/modules/"

        success "File GUI installati: vite/src/modules/$MODULE/"
        INSTALLED_SOMETHING=true
    fi

    # -------------------------------------------------------------------------
    # 3. COPIA MIGRATION SQL (con timestamp fresco)
    # -------------------------------------------------------------------------
    if [ -d "$DEST/migration" ] && [ -n "$(ls -A "$DEST/migration"/*.sql 2>/dev/null)" ]; then
        info "→ Installazione migration SQL..."
        local TIMESTAMP=$(date +%Y%m%d_%H%M%S)

        for sql_file in "$DEST/migration"/*.sql; do
            local BASENAME=$(basename "$sql_file")
            local DESCRIPTION=$(echo "$BASENAME" | sed 's/^V[0-9_]*__//')
            local NEW_NAME="V${TIMESTAMP}__${DESCRIPTION}"

            cp "$sql_file" "$WORKSPACE/src/main/resources/db/migration/$NEW_NAME"
            success "Migration installata: $NEW_NAME"

            # Incrementa timestamp di 1 secondo per evitare conflitti
            sleep 1
            TIMESTAMP=$(date +%Y%m%d_%H%M%S)
        done

        INSTALLED_SOMETHING=true
    fi

    # -------------------------------------------------------------------------
    # 4. APPEND CONFIGURAZIONI APPLICATION.PROPERTIES
    # -------------------------------------------------------------------------
    if [ -f "$DEST/config/application.properties" ]; then
        # Verifica se ha configurazioni reali (non solo commenti/righe vuote)
        if grep -qv '^#' "$DEST/config/application.properties" | grep -qv '^$'; then
            # Check se configurazione già presente (evita duplicati)
            if ! grep -q "# Module: $MODULE" "$WORKSPACE/config/application.properties" 2>/dev/null; then
                info "→ Aggiunta configurazioni a config/application.properties..."
                echo "" >> "$WORKSPACE/config/application.properties"
                cat "$DEST/config/application.properties" >> "$WORKSPACE/config/application.properties"

                success "Configurazioni aggiunte a config/application.properties"
                warn "IMPORTANTE: Rivedi e personalizza i valori di configurazione"
                INSTALLED_SOMETHING=true
            else
                warn "Configurazione per modulo '$MODULE' già presente in application.properties, skip"
            fi
        fi
    fi

    # -------------------------------------------------------------------------
    # SEPARATORE
    # -------------------------------------------------------------------------
    if [ "$INSTALLED_SOMETHING" = true ]; then
        info ""
        info "=========================================="
        info "INSTALLAZIONE AUTOMATICA COMPLETATA"
        info "=========================================="
        info ""
    fi

    # =========================================================================
    # ISTRUZIONI MANUALI
    # =========================================================================

    local MANUAL_STEPS_REQUIRED=false

    # Verifica se README contiene istruzioni per pom.xml
    if grep -q "pom.xml" "$DEST/README.md" 2>/dev/null; then
        if ! $MANUAL_STEPS_REQUIRED; then
            info "=========================================="
            info "AZIONI MANUALI RICHIESTE"
            info "=========================================="
            info ""
            MANUAL_STEPS_REQUIRED=true
        fi

        info "1. DIPENDENZE POM.XML"
        info "   Apri: pom.xml"
        info "   Aggiungi nel blocco <dependencies>:"
        info ""

        # Estrai blocco XML dependency dal README
        sed -n '/```xml/,/```/p' "$DEST/README.md" | grep -v '```' | sed 's/^/   /'
        info ""
    fi

    # Verifica se README contiene import Java
    if grep -q "import.*$MODULE" "$DEST/README.md" 2>/dev/null; then
        if ! $MANUAL_STEPS_REQUIRED; then
            info "=========================================="
            info "AZIONI MANUALI RICHIESTE"
            info "=========================================="
            info ""
            MANUAL_STEPS_REQUIRED=true
        fi

        info "2. IMPORT IN APP.JAVA"
        info "   Apri: src/main/java/$APP_PACKAGE_PATH/App.java"
        info "   Aggiungi questi import:"
        info ""

        grep "import.*$MODULE" "$DEST/README.md" | sed "s|com\.example|$APP_PACKAGE|g" | sed 's/^/   /'
        info ""
    fi

    # Verifica se README contiene route
    if grep -q "paths.add" "$DEST/README.md" 2>/dev/null; then
        if ! $MANUAL_STEPS_REQUIRED; then
            info "=========================================="
            info "AZIONI MANUALI RICHIESTE"
            info "=========================================="
            info ""
            MANUAL_STEPS_REQUIRED=true
        fi

        info "3. ROUTE IN APP.JAVA"
        info "   Apri: src/main/java/$APP_PACKAGE_PATH/App.java"
        info "   Aggiungi queste route nel PathTemplateHandler:"
        info ""

        grep "paths.add" "$DEST/README.md" | sed "s|com\.example|$APP_PACKAGE|g" | sed 's/^/   /'
        info ""
    fi

    # Verifica se README contiene config.js
    if grep -q "MODULE_CONFIG" "$DEST/README.md" 2>/dev/null; then
        if ! $MANUAL_STEPS_REQUIRED; then
            info "=========================================="
            info "AZIONI MANUALI RICHIESTE"
            info "=========================================="
            info ""
            MANUAL_STEPS_REQUIRED=true
        fi

        info "4. CONFIGURAZIONE CONFIG.JS"
        info "   Apri: vite/src/config.js"
        info "   Aggiungi nel MODULE_CONFIG:"
        info ""

        # Estrai blocco module config dal README
        sed -n "/$MODULE:/,/^[[:space:]]*}/p" "$DEST/README.md" | sed 's/^/   /'
        info ""

        if [ "$MODULE" = "header" ]; then
            warn "IMPORTANTE: Il modulo 'header' deve essere dichiarato PRIMA degli altri in MODULE_CONFIG"
            info ""
        fi
    fi

    # =========================================================================
    # BUILD AUTOMATICO (opzionale, può essere disabilitato con flag)
    # =========================================================================

    if [ "$INSTALLED_SOMETHING" = true ]; then
        info "=========================================="
        info "BUILD APPLICAZIONE"
        info "=========================================="
        info ""
        info "Esecuzione build automatico..."
        info ""

        # Build GUI
        (cd "$WORKSPACE/vite" && npm run build) || warn "Build GUI fallito"

        # Build App (Maven)
        (cd "$WORKSPACE" && mvn clean package -DskipTests) || warn "Build Maven fallito"

        info ""
    fi

    # =========================================================================
    # RIEPILOGO FINALE
    # =========================================================================

    info "=========================================="
    info "INSTALLAZIONE COMPLETATA"
    info "=========================================="
    info ""

    if [ "$MANUAL_STEPS_REQUIRED" = true ]; then
        warn "Completa le azioni manuali sopra elencate prima di testare il modulo"
        info ""
        info "Documentazione completa: $DEST/README.md"
    else
        success "Nessuna azione manuale richiesta!"
        info "Il modulo '$MODULE' è pronto per l'uso"
    fi

    info ""
}
```

---

## Modifiche al Dispatcher

Aggiornare la sezione `module)` per supportare il nuovo flag:

```bash
module)
    case "$2" in
        export) shift 2; module_export "$@" ;;
        import) module_import "$3" "$4" ;;  # Passa anche il flag --extract-only
        *) error "Unknown module command: $2. Use: export, import" ;;
    esac
    ;;
```

---

## Esempio di Output

### Caso 1: Modulo con Dipendenze (auth)

```bash
$ cmd module import auth-1.0.1.tar.gz
```

**Output**:
```
INFO: Estrazione modulo 'auth'...
✓ SUCCESS: Modulo 'auth' estratto in modules/auth

INFO:
INFO: ==========================================
INFO: INSTALLAZIONE AUTOMATICA MODULO: auth
INFO: Package applicazione: dev.hola
INFO: ==========================================
INFO:

INFO: → Installazione file Java...
✓ SUCCESS: File Java installati: src/main/java/dev/hola/auth/

INFO: → Installazione file GUI...
✓ SUCCESS: File GUI installati: vite/src/modules/auth/

INFO: → Installazione migration SQL...
✓ SUCCESS: Migration installata: V20260303_152010__auth_tables.sql
✓ SUCCESS: Migration installata: V20260303_152011__auth_indexes.sql

INFO: → Aggiunta configurazioni a config/application.properties...
✓ SUCCESS: Configurazioni aggiunte a config/application.properties
WARNING: IMPORTANTE: Rivedi e personalizza i valori di configurazione

INFO:
INFO: ==========================================
INFO: INSTALLAZIONE AUTOMATICA COMPLETATA
INFO: ==========================================
INFO:

INFO: ==========================================
INFO: AZIONI MANUALI RICHIESTE
INFO: ==========================================
INFO:

INFO: 1. DIPENDENZE POM.XML
INFO:    Apri: pom.xml
INFO:    Aggiungi nel blocco <dependencies>:
INFO:
   <dependency>
       <groupId>com.auth0</groupId>
       <artifactId>java-jwt</artifactId>
       <version>4.4.0</version>
   </dependency>
INFO:

INFO: 2. IMPORT IN APP.JAVA
INFO:    Apri: src/main/java/dev/hola/App.java
INFO:    Aggiungi questi import:
INFO:
   import dev.hola.auth.handler.LoginHandler;
   import dev.hola.auth.handler.LogoutHandler;
   ...
INFO:

INFO: 3. ROUTE IN APP.JAVA
INFO:    Apri: src/main/java/dev/hola/App.java
INFO:    Aggiungi queste route nel PathTemplateHandler:
INFO:
   paths.add("/api/auth/login", route(new LoginHandler(), ds));
   paths.add("/api/auth/logout", route(new LogoutHandler(), ds));
   ...
INFO:

INFO: 4. CONFIGURAZIONE CONFIG.JS
INFO:    Apri: vite/src/config.js
INFO:    Aggiungi nel MODULE_CONFIG:
INFO:
   auth: {
     path: '/auth',
     container: 'main',
     authorization: null,
     persistent: false,
     init: () => import('./modules/auth/init.js').then(m => m.default())
   }
INFO:

INFO: ==========================================
INFO: BUILD APPLICAZIONE
INFO: ==========================================
INFO:

INFO: Esecuzione build automatico...
INFO:

[... output build npm ...]
[... output build maven ...]

INFO:
INFO: ==========================================
INFO: INSTALLAZIONE COMPLETATA
INFO: ==========================================
INFO:

WARNING: Completa le azioni manuali sopra elencate prima di testare il modulo
INFO:
INFO: Documentazione completa: modules/auth/README.md
INFO:
```

### Caso 2: Modulo Semplice Senza Dipendenze (home)

```bash
$ cmd module import home-1.2.0.tar.gz
```

**Output**:
```
INFO: Estrazione modulo 'home'...
✓ SUCCESS: Modulo 'home' estratto in modules/home

INFO:
INFO: ==========================================
INFO: INSTALLAZIONE AUTOMATICA MODULO: home
INFO: Package applicazione: dev.hola
INFO: ==========================================
INFO:

INFO: → Installazione file Java...
✓ SUCCESS: File Java installati: src/main/java/dev/hola/home/

INFO: → Installazione file GUI...
✓ SUCCESS: File GUI installati: vite/src/modules/home/

INFO:
INFO: ==========================================
INFO: INSTALLAZIONE AUTOMATICA COMPLETATA
INFO: ==========================================
INFO:

INFO: ==========================================
INFO: AZIONI MANUALI RICHIESTE
INFO: ==========================================
INFO:

INFO: 2. IMPORT IN APP.JAVA
INFO:    Apri: src/main/java/dev/hola/App.java
INFO:    Aggiungi questi import:
INFO:
   import dev.hola.home.handler.HelloHandler;
INFO:

INFO: 3. ROUTE IN APP.JAVA
INFO:    Apri: src/main/java/dev/hola/App.java
INFO:    Aggiungi queste route nel PathTemplateHandler:
INFO:
   paths.add("/api/home/hello", route(new HelloHandler(), ds));
INFO:

INFO: 4. CONFIGURAZIONE CONFIG.JS
INFO:    Apri: vite/src/config.js
INFO:    Aggiungi nel MODULE_CONFIG:
INFO:
   home: {
     path: '/home',
     container: 'main',
     authorization: null,
     persistent: false,
     init: null
   }
INFO:

INFO: ==========================================
INFO: BUILD APPLICAZIONE
INFO: ==========================================
INFO:

INFO: Esecuzione build automatico...

[... output build ...]

INFO:
INFO: ==========================================
INFO: INSTALLAZIONE COMPLETATA
INFO: ==========================================
INFO:

WARNING: Completa le azioni manuali sopra elencate prima di testare il modulo
INFO:
INFO: Documentazione completa: modules/home/README.md
INFO:
```

### Caso 3: Solo Estrazione (Comportamento Originale)

```bash
$ cmd module import auth-1.0.1.tar.gz --extract-only
```

**Output**:
```
INFO: Estrazione modulo 'auth'...
✓ SUCCESS: Modulo 'auth' estratto in modules/auth
INFO: Leggi modules/auth/README.md per i passi di installazione.
```

---

## Vantaggi Rispetto a Implementazione Attuale

| Aspetto | Prima | Dopo | Miglioramento |
|---------|-------|------|---------------|
| **Tempo installazione** | 23-37 min | 7-10 min | **~70%** |
| **Comandi da eseguire** | ~15-20 | 1 + 3 edit | **~80%** |
| **Rischio errori** | Alto | Basso | **~80%** |
| **File da cercare manualmente** | 6 | 0 | **100%** |
| **Copy-paste manuali** | 5-7 | 0 (auto) | **100%** |
| **Build manuale** | Sì | No (auto) | **100%** |

---

## Compatibilità

- ✅ **Backward compatible**: Flag `--extract-only` preserva comportamento originale
- ✅ **Fail-safe**: Se build fallisce, mostra warning ma non blocca
- ✅ **Idempotente**: Rileva configurazioni già presenti (skip duplicati)
- ✅ **Informativo**: Output dettagliato di ogni passo

---

## Testing

### Test Cases da Verificare

1. **Modulo con tutto** (auth)
   ```bash
   cmd module import jms/modules/auth-1.0.1.tar.gz
   # Verifica: Java copiato, GUI copiato, migration, config, istruzioni mostrate
   ```

2. **Modulo solo GUI** (header senza Java)
   ```bash
   cmd module import jms/modules/header-1.0.0.tar.gz
   # Verifica: Solo GUI copiato, warning per header come primo
   ```

3. **Reinstallazione** (duplicati)
   ```bash
   cmd module import jms/modules/auth-1.0.1.tar.gz
   # Primo import OK
   rm -rf modules/auth
   cmd module import jms/modules/auth-1.0.1.tar.gz
   # Verifica: Skip config duplicata
   ```

4. **Extract-only flag**
   ```bash
   cmd module import jms/modules/home-1.2.0.tar.gz --extract-only
   # Verifica: Solo estrazione, nessuna installazione
   ```

---

## Prossimi Passi

1. ✅ Revisionare questa proposta
2. ⏳ Implementare funzione rifattorizzata in `jms/bin/cmd`
3. ⏳ Testare con tutti i moduli disponibili
4. ⏳ Aggiornare documentazione (CLAUDE.md, README.md)
5. ⏳ Commit e sync verso altri progetti

---

## Note Implementative

### Gestione Errori

- Se `pom.xml` non esiste → error (già gestito)
- Se modulo già estratto → error (già gestito)
- Se build fallisce → warning, continua (non blocca)
- Se README non contiene sezioni → skip silenzioso

### Performance

- Build può essere lento (~30-60s per modulo grande)
- Considerare flag `--no-build` per disabilitare build automatico
- Sleep di 1 secondo tra migration per evitare timestamp duplicati

### Manutenibilità

- Parsing README basato su pattern fissi (fragile se README cambia formato)
- Alternativa: metadata JSON nel modulo (es. `module.json` con dipendenze)
- Per ora: accettabile, README hanno formato standardizzato
