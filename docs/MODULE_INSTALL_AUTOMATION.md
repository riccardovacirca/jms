# Module Installation Automation - Piano di Implementazione

> Documento di pianificazione per l'automazione del processo di installazione moduli

## Stato Attuale

Attualmente l'installazione di un modulo richiede questi passaggi manuali:

1. **Estrazione archivio**: `cmd module import <file.tar.gz>` (già automatizzato)
2. **Copia file Java**: manuale
3. **Sostituzione package**: manuale con `find` + `sed`
4. **Copia file GUI**: manuale
5. **Copia migration SQL**: manuale
6. **Aggiunta dipendenze pom.xml**: manuale (editing XML)
7. **Aggiunta configurazioni properties**: manuale (copy-paste)
8. **Aggiunta import in App.java**: manuale (editing Java)
9. **Aggiunta route in App.java**: manuale (editing Java)
10. **Aggiunta configurazione config.js**: manuale (editing JavaScript)
11. **Build**: manuale (`cmd gui build && cmd app build`)

## Obiettivo

Automatizzare tutti i passaggi che **non richiedono parsing complesso** dei file sorgente, riducendo il lavoro manuale dell'80%.

---

## Passaggi Automatizzabili (Semplici)

### ✅ 1. Estrazione e Preparazione

**Stato**: Già automatizzato via `cmd module import`

**Azione**: Nessuna modifica richiesta

**Output**: Directory `modules/<nome>/` con struttura:
```
modules/<nome>/
├── README.md
├── config/
│   └── application.properties
├── gui/
├── java/
└── migration/
```

---

### ✅ 2. Copia File Java

**Complessità**: Bassa (copia diretta)

**Script**:
```bash
# Rileva package dall'applicazione
APP_PACKAGE=$(grep -oP '<groupId>\K[^<]+' pom.xml | head -1)
APP_PACKAGE_PATH=$(echo "$APP_PACKAGE" | tr '.' '/')

# Copia sorgenti Java
MODULE_NAME="auth"
cp -r "modules/$MODULE_NAME/java/$MODULE_NAME" \
      "src/main/java/$APP_PACKAGE_PATH/$MODULE_NAME/"
```

**Prerequisiti**:
- `pom.xml` esiste e contiene `<groupId>`
- Struttura standard del modulo

**Rischi**: Nessuno (copia filesystem)

---

### ✅ 3. Sostituzione Package Java

**Complessità**: Bassa (sed su pattern fisso)

**Script**:
```bash
# Sostituisci com.example con package applicazione
find "src/main/java/$APP_PACKAGE_PATH/$MODULE_NAME" -name '*.java' \
     -exec sed -i "s|com\.example|$APP_PACKAGE|g" {} +
```

**Prerequisiti**:
- I file Java sono già copiati
- Tutti i template usano `com.example` (convenzione)

**Rischi**: Nessuno se la convenzione è rispettata

---

### ✅ 4. Copia File GUI (Frontend)

**Complessità**: Bassa (copia diretta)

**Script**:
```bash
cp -r "modules/$MODULE_NAME/gui/$MODULE_NAME" \
      "vite/src/modules/$MODULE_NAME/"
```

**Prerequisiti**:
- Directory `vite/src/modules/` esiste

**Rischi**: Nessuno

---

### ✅ 5. Copia e Rinomina Migration SQL

**Complessità**: Bassa (copia + rename timestamp)

**Script**:
```bash
# Se il modulo ha migration/
if [ -d "modules/$MODULE_NAME/migration" ]; then
    # Genera timestamp fresco
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)

    # Copia e rinomina ogni file
    for sql_file in modules/$MODULE_NAME/migration/*.sql; do
        BASENAME=$(basename "$sql_file")
        # Estrai descrizione dal nome (dopo __)
        DESCRIPTION=$(echo "$BASENAME" | sed 's/^V[0-9_]*__//')
        # Nuovo nome con timestamp fresco
        NEW_NAME="V${TIMESTAMP}__${DESCRIPTION}"

        cp "$sql_file" "src/main/resources/db/migration/$NEW_NAME"
    done
fi
```

**Prerequisiti**:
- Directory `src/main/resources/db/migration/` esiste
- Migration seguono pattern `V<timestamp>__<description>.sql`

**Rischi**: Nessuno (Flyway accetta nuovi timestamp)

---

### ✅ 6. Append Configurazioni Properties

**Complessità**: Bassa (cat append)

**Script**:
```bash
# Se il modulo ha config/application.properties
if [ -f "modules/$MODULE_NAME/config/application.properties" ]; then
    # Verifica se ha configurazioni (non solo commenti)
    if grep -qv '^#' "modules/$MODULE_NAME/config/application.properties" | grep -q '.'; then
        # Append al file di configurazione app
        echo "" >> config/application.properties
        cat "modules/$MODULE_NAME/config/application.properties" >> config/application.properties

        echo "✓ Configurazioni aggiunte a config/application.properties"
        echo "  IMPORTANTE: Rivedi e personalizza i valori (es. jwt.secret, mail.*)"
    fi
fi
```

**Prerequisiti**:
- `config/application.properties` esiste
- Ogni modulo ha `config/application.properties` (anche vuoto)

**Rischi**: Minimi
- Possibili duplicazioni se modulo già installato → richiede controllo manuale

**Miglioramento possibile**:
```bash
# Check se configurazione già presente
if ! grep -q "# Module: $MODULE_NAME" config/application.properties; then
    # ... append
else
    echo "⚠ Configurazione per $MODULE_NAME già presente, skip"
fi
```

---

### ✅ 7. Build Automatico

**Complessità**: Bassa (comandi fissi)

**Script**:
```bash
cmd gui build
cmd app build
```

**Prerequisiti**: Nessuno

**Rischi**: Nessuno (build potrebbe fallire per errori precedenti, ma è informativo)

---

## Passaggi NON Automatizzabili (Complessi)

### ❌ 8. Aggiunta Dipendenze pom.xml

**Motivo**: Richiede parsing XML, gestione indentazione, trovare tag `<dependencies>`

**Soluzione proposta**: **Output istruzioni manuali**

**Script**:
```bash
# Estrai sezione dipendenze dal README
if grep -q "pom.xml.*dependencies" "modules/$MODULE_NAME/README.md"; then
    echo ""
    echo "=========================================="
    echo "AZIONE MANUALE RICHIESTA: pom.xml"
    echo "=========================================="
    echo ""
    echo "Aggiungi le seguenti dipendenze al file pom.xml:"
    echo ""

    # Estrai blocco XML dal README
    sed -n '/```xml/,/```/p' "modules/$MODULE_NAME/README.md" | \
        grep -v '```'

    echo ""
fi
```

**Stima riduzione lavoro**: Da 5-10 minuti a 30 secondi (copy-paste guidato)

---

### ❌ 9. Aggiunta Import in App.java

**Motivo**: Richiede parsing Java, trovare sezione import, gestire ordinamento

**Soluzione proposta**: **Output istruzioni manuali con copy-paste ready**

**Script**:
```bash
echo ""
echo "=========================================="
echo "AZIONE MANUALE RICHIESTA: App.java imports"
echo "=========================================="
echo ""
echo "Aggiungi questi import all'inizio di App.java:"
echo ""

# Estrai blocco import dal README
sed -n '/import.*'$MODULE_NAME'/p' "modules/$MODULE_NAME/README.md"

echo ""
```

**Stima riduzione lavoro**: Da 3-5 minuti a 20 secondi

---

### ❌ 10. Aggiunta Route in App.java

**Motivo**: Richiede parsing Java, trovare blocco PathTemplateHandler

**Soluzione proposta**: **Output istruzioni con copy-paste ready**

**Script**:
```bash
echo "=========================================="
echo "AZIONE MANUALE RICHIESTA: App.java routes"
echo "=========================================="
echo ""
echo "Aggiungi queste route nel PathTemplateHandler di App.java:"
echo ""

# Estrai blocco routes dal README
sed -n '/paths\.add.*'$MODULE_NAME'/p' "modules/$MODULE_NAME/README.md"

echo ""
```

**Stima riduzione lavoro**: Da 5 minuti a 30 secondi

---

### ⚠️ 11. Aggiunta Configurazione config.js

**Motivo**: Richiede parsing JavaScript, gestire ordinamento moduli (header deve essere primo)

**Soluzione proposta**: **Output istruzioni con blocco JavaScript pronto**

**Script**:
```bash
echo "=========================================="
echo "AZIONE MANUALE RICHIESTA: config.js"
echo "=========================================="
echo ""
echo "Aggiungi questa configurazione in vite/src/config.js:"
echo ""

# Estrai blocco JavaScript dal README
sed -n '/MODULE_CONFIG/,/};/p' "modules/$MODULE_NAME/README.md" | \
    grep -A 10 "  $MODULE_NAME:"

echo ""
echo "ATTENZIONE: Il modulo 'header' deve essere dichiarato PRIMA degli altri"
echo ""
```

**Stima riduzione lavoro**: Da 3 minuti a 20 secondi

---

## Implementazione Proposta

### Nuovo Comando: `cmd module install <nome>`

**Sintassi**:
```bash
cmd module install auth
cmd module install home
cmd module install header
cmd module install contatti
```

**Logica**:

```bash
#!/bin/bash
MODULE_NAME="$1"

# Validazione
if [ -z "$MODULE_NAME" ]; then
    echo "Uso: cmd module install <nome-modulo>"
    exit 1
fi

if [ ! -d "modules/$MODULE_NAME" ]; then
    echo "Errore: Modulo 'modules/$MODULE_NAME' non trovato"
    echo "Esegui prima: cmd module import <file.tar.gz>"
    exit 1
fi

# Rileva package applicazione
APP_PACKAGE=$(grep -oP '<groupId>\K[^<]+' pom.xml | head -1)
APP_PACKAGE_PATH=$(echo "$APP_PACKAGE" | tr '.' '/')

echo "=========================================="
echo "Installazione modulo: $MODULE_NAME"
echo "Package applicazione: $APP_PACKAGE"
echo "=========================================="
echo ""

# ✅ 1. Copia file Java
if [ -d "modules/$MODULE_NAME/java/$MODULE_NAME" ]; then
    echo "→ Copia file Java..."
    mkdir -p "src/main/java/$APP_PACKAGE_PATH"
    cp -r "modules/$MODULE_NAME/java/$MODULE_NAME" \
          "src/main/java/$APP_PACKAGE_PATH/"

    # ✅ 2. Sostituzione package
    echo "→ Sostituzione package (com.example → $APP_PACKAGE)..."
    find "src/main/java/$APP_PACKAGE_PATH/$MODULE_NAME" -name '*.java' \
         -exec sed -i "s|com\.example|$APP_PACKAGE|g" {} +

    echo "  ✓ File Java installati: src/main/java/$APP_PACKAGE_PATH/$MODULE_NAME/"
fi

# ✅ 3. Copia file GUI
if [ -d "modules/$MODULE_NAME/gui/$MODULE_NAME" ]; then
    echo "→ Copia file GUI..."
    mkdir -p "vite/src/modules"
    cp -r "modules/$MODULE_NAME/gui/$MODULE_NAME" \
          "vite/src/modules/"

    echo "  ✓ File GUI installati: vite/src/modules/$MODULE_NAME/"
fi

# ✅ 4. Copia migration SQL
if [ -d "modules/$MODULE_NAME/migration" ] && [ -n "$(ls -A modules/$MODULE_NAME/migration/*.sql 2>/dev/null)" ]; then
    echo "→ Copia migration SQL..."
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)

    for sql_file in modules/$MODULE_NAME/migration/*.sql; do
        BASENAME=$(basename "$sql_file")
        DESCRIPTION=$(echo "$BASENAME" | sed 's/^V[0-9_]*__//')
        NEW_NAME="V${TIMESTAMP}__${DESCRIPTION}"

        cp "$sql_file" "src/main/resources/db/migration/$NEW_NAME"
        echo "  ✓ Migration copiata: $NEW_NAME"

        # Incrementa timestamp di 1 secondo per evitare duplicati
        sleep 1
        TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    done
fi

# ✅ 5. Append configurazioni properties
if [ -f "modules/$MODULE_NAME/config/application.properties" ]; then
    # Verifica se ha configurazioni reali (non solo commenti/vuoto)
    if grep -qv '^#' "modules/$MODULE_NAME/config/application.properties" | grep -qv '^$'; then
        # Check duplicazione
        if ! grep -q "# Module: $MODULE_NAME" config/application.properties 2>/dev/null; then
            echo "→ Aggiunta configurazioni..."
            echo "" >> config/application.properties
            cat "modules/$MODULE_NAME/config/application.properties" >> config/application.properties

            echo "  ✓ Configurazioni aggiunte a config/application.properties"
            echo "  ⚠  IMPORTANTE: Rivedi e personalizza i valori di configurazione"
        else
            echo "  ⚠  Configurazione già presente, skip"
        fi
    fi
fi

echo ""
echo "=========================================="
echo "PASSAGGI AUTOMATICI COMPLETATI"
echo "=========================================="
echo ""

# ❌ Output istruzioni manuali

echo "=========================================="
echo "AZIONI MANUALI RICHIESTE"
echo "=========================================="
echo ""

# Controlla dipendenze pom.xml
if grep -q "pom.xml" "modules/$MODULE_NAME/README.md"; then
    echo "1. DIPENDENZE POM.XML"
    echo "   Apri: pom.xml"
    echo "   Aggiungi nel blocco <dependencies>:"
    echo ""
    sed -n '/```xml/,/```/p' "modules/$MODULE_NAME/README.md" | grep -v '```' | sed 's/^/   /'
    echo ""
fi

# Import App.java
if grep -q "import.*$MODULE_NAME" "modules/$MODULE_NAME/README.md"; then
    echo "2. IMPORT IN APP.JAVA"
    echo "   Apri: src/main/java/$APP_PACKAGE_PATH/App.java"
    echo "   Aggiungi questi import:"
    echo ""
    grep "import.*$MODULE_NAME" "modules/$MODULE_NAME/README.md" | sed 's/^/   /'
    echo ""
fi

# Route App.java
if grep -q "paths.add" "modules/$MODULE_NAME/README.md"; then
    echo "3. ROUTE IN APP.JAVA"
    echo "   Apri: src/main/java/$APP_PACKAGE_PATH/App.java"
    echo "   Aggiungi queste route nel PathTemplateHandler:"
    echo ""
    grep "paths.add" "modules/$MODULE_NAME/README.md" | sed 's/^/   /'
    echo ""
fi

# Config.js
if grep -q "MODULE_CONFIG" "modules/$MODULE_NAME/README.md"; then
    echo "4. CONFIGURAZIONE CONFIG.JS"
    echo "   Apri: vite/src/config.js"
    echo "   Aggiungi nel MODULE_CONFIG:"
    echo ""
    sed -n "/$MODULE_NAME:/,/}/p" "modules/$MODULE_NAME/README.md" | head -n -1 | sed 's/^/   /'
    echo "   }"
    echo ""

    if [ "$MODULE_NAME" = "header" ]; then
        echo "   ⚠  IMPORTANTE: 'header' deve essere il PRIMO modulo in MODULE_CONFIG"
        echo ""
    fi
fi

echo "=========================================="
echo "PROSSIMI PASSI"
echo "=========================================="
echo ""
echo "1. Completa le azioni manuali sopra elencate"
echo "2. Rivedi config/application.properties e personalizza i valori"
echo "3. Esegui build:"
echo ""
echo "   cmd gui build"
echo "   cmd app build"
echo ""
echo "4. Testa l'applicazione"
echo ""
echo "Per dettagli completi: modules/$MODULE_NAME/README.md"
echo ""
```

---

## Stima Riduzione Tempo

### Tempo Attuale (Installazione Manuale)
- Lettura README: 2-3 minuti
- Copia Java + sostituzione package: 3-5 minuti
- Copia GUI: 1 minuto
- Copia migration: 2 minuti (rename timestamp)
- Configurazione properties: 2-3 minuti (estrai, copia, incolla)
- Dipendenze pom.xml: 3-5 minuti
- Import/route App.java: 5-8 minuti
- Config.js: 3-5 minuti
- Build: 2 minuti

**Totale: 23-37 minuti per modulo**

### Tempo con Automazione Proposta
- Esecuzione script automatico: 10-20 secondi
- Lettura output istruzioni: 1 minuto
- Dipendenze pom.xml: 30 secondi (copy-paste)
- Import/route App.java: 1 minuto (copy-paste)
- Config.js: 30 secondi (copy-paste)
- Revisione config: 2-3 minuti
- Build: 2 minuti

**Totale: 7-10 minuti per modulo**

**Riduzione: ~70-75% del tempo**

---

## Benefici

### ✅ Riduzione Errori
- Package sempre corretto (rilevato da pom.xml)
- Migration timestamp sempre validi
- Nessun file dimenticato

### ✅ Consistenza
- Procedura standardizzata
- Tutti i moduli installati allo stesso modo
- Tracciabilità (log output dello script)

### ✅ Onboarding Sviluppatori
- Processo chiaro e documentato
- Output guida passo-passo
- Riduce curva di apprendimento

### ✅ Manutenibilità
- Modifiche future alla struttura moduli richiedono solo update script
- Convention-over-configuration

---

## Implementazione Tecnica

### File da Modificare

1. **`bin/cmd`** - Aggiungere comando `module install`

```bash
module)
    case "$2" in
        import)
            module_import "$3"
            ;;
        install)
            module_install "$3"
            ;;
        export)
            shift 2
            module_export "$@"
            ;;
        *)
            echo "Uso: cmd module [import|install|export]"
            ;;
    esac
    ;;
```

2. **Nuova funzione `module_install()` in `bin/cmd`**

(Vedi script completo sopra)

---

## Testing

### Test Cases

1. **Modulo semplice senza dipendenze** (home, header)
   - ✓ Copia file
   - ✓ Build funziona

2. **Modulo con dipendenze** (auth)
   - ✓ Copia file
   - ✓ Configurazioni properties aggiunte
   - ✓ Istruzioni manuali mostrate correttamente
   - ✓ Build funziona dopo completamento manuale

3. **Modulo con migration** (auth, contatti)
   - ✓ Migration copiate con timestamp fresco
   - ✓ Flyway accetta migration

4. **Reinstallazione modulo**
   - ✓ Rileva duplicazione configurazione
   - ✓ Skip append properties

### Procedure di Test

```bash
# Clean state
rm -rf modules/auth/
rm -rf src/main/java/*/auth
rm -rf vite/src/modules/auth

# Import + Install
cmd module import jms/modules/auth-1.0.1.tar.gz
cmd module install auth

# Verifica output
# Completa azioni manuali
# Build e test
```

---

## Milestone

### Phase 1: Implementazione Base ✅
- [x] Creazione file `config/application.properties` per ogni modulo
- [ ] Implementazione funzione `module_install` in `bin/cmd`
- [ ] Test con modulo home (semplice)

### Phase 2: Gestione Casi Complessi
- [ ] Test con modulo auth (dipendenze + config + migration)
- [ ] Test con modulo header (ordinamento config.js)
- [ ] Test reinstallazione (check duplicati)

### Phase 3: Documentazione e Refinement
- [ ] Update CLAUDE.md con nuova procedura
- [ ] Aggiunta esempi in docs/
- [ ] Feedback sviluppatori e iterazione

---

## Limitazioni Accettate

1. **Editing pom.xml**: Richiesto manuale (XML parsing complesso)
2. **Editing App.java**: Richiesto manuale (Java parsing complesso)
3. **Editing config.js**: Richiesto manuale (ordinamento moduli dinamico)

**Rationale**: Questi 3 file sono centrali e modificarli automaticamente introdurrebbe rischi. Il trade-off (70% automazione vs. 100% sicurezza) è favorevole.

---

## Estensioni Future (Opzionali)

### A. Comando `cmd module uninstall <nome>`
- Rimuove file Java, GUI, configurazioni
- Utile per testing e cleanup

### B. Comando `cmd module list`
- Mostra moduli disponibili
- Mostra moduli installati (rilevando presenza file)

### C. Validazione pre-installazione
- Check se modulo già installato
- Check compatibilità versioni

### D. Interactive mode
- Prompt conferma prima di sovrascrivere
- Scelta dipendenze opzionali

---

## Conclusione

L'automazione proposta:
- ✅ Elimina **70-75% del lavoro manuale**
- ✅ Riduce errori umani
- ✅ Standardizza il processo
- ✅ Mantiene sicurezza (no parsing complesso)
- ✅ Implementabile in **~2-3 ore di sviluppo**
- ✅ Testabile e manutenibile

**Prossimo step**: Implementazione `module_install` in `bin/cmd`
