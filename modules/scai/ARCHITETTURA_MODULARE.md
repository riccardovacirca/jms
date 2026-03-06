# Architettura Modulare - Sistema SCAI

## Introduzione

Il sistema SCAI (Sistema di Controllo Accessi Integrato) è stato progettato seguendo un'architettura modulare che garantisce:

- **Modularità**: Ogni modulo è autonomo e può essere installato/disinstallato indipendentemente
- **Zero Accoppiamento**: Nessuna dipendenza circolare tra moduli
- **Riusabilità**: I moduli possono essere utilizzati in diversi contesti
- **Manutenibilità**: Modifiche isolate per dominio funzionale
- **Scalabilità**: Possibile aggiungere/rimuovere moduli senza impatti

## Principio Architetturale Fondamentale

```
UN MODULO = UNA ENTITÀ + SUE DIPENDENZE DIRETTE
```

Ogni modulo gestisce una singola entità principale e le sue dipendenze 1:1 dirette. Le relazioni cross-modulo sono gestite tramite **chiavi naturali** senza introdurre accoppiamento.

## Tipologie di Moduli

### 1. Moduli LOOKUP
Moduli che forniscono dati di riferimento per altri moduli. Non hanno dipendenze.

**Caratteristiche**:
- Installation Order basso (1-5)
- Nessuna dipendenza
- Utilizzati da moduli ENTITY

**Esempi**: `ente`, `sede`

### 2. Moduli ENTITY
Moduli che gestiscono entità di business con relazioni verso moduli LOOKUP.

**Caratteristiche**:
- Installation Order medio-alto (10+)
- Dipendenze verso moduli LOOKUP
- Possono avere dipendenze opzionali verso altri moduli ENTITY

**Esempi**: `rapporti`, `badge`, `policy`, `veicoli`

## Struttura di un Modulo

Ogni modulo segue questa struttura standardizzata:

```
modules/{nome_modulo}/
├── module.json                 # Metadati del modulo
├── README.md                   # Documentazione
├── config/
│   └── application.properties  # Configurazione
├── migration/
│   └── V{timestamp}__{name}.sql  # Migration SQL
├── java/{nome_modulo}/
│   ├── dto/                    # Data Transfer Objects
│   ├── dao/                    # Data Access Objects
│   ├── adapter/                # Adapter layer
│   └── handler/                # Request handlers
└── {nome_modulo}/
    ├── index.js                # Entry point
    ├── component.js            # Componente Lit principale
    └── module-component.js     # Wrapper modulo
```

## File module.json

Ogni modulo dichiara solo le informazioni essenziali nel file `module.json`:

```json
{
  "name": "nome_modulo",
  "version": "1.0.0",
  "dependencies": {
    "modulo_richiesto": "^1.0.0"
  }
}
```

**Esempi**:

Modulo senza dipendenze (lookup):
```json
{
  "name": "ente",
  "version": "1.0.0",
  "dependencies": {}
}
```

Modulo con dipendenze (entity):
```json
{
  "name": "rapporti",
  "version": "1.0.0",
  "dependencies": {
    "ente": "^1.0.0",
    "sede": "^1.0.0"
  }
}
```

## Gestione delle Dipendenze

### Chiavi Naturali

Le relazioni cross-modulo utilizzano **chiavi naturali** invece di foreign key fisiche:

```sql
-- Modulo rapporti
scai_rapporto (
  cod_ente VARCHAR(15),   -- chiave naturale verso modulo ente
  matricola VARCHAR(15)
)

-- Modulo badge (riferimento a rapporti)
scai_badge (
  cod_ente VARCHAR(15),
  matricola VARCHAR(15),
  FOREIGN KEY (cod_ente, matricola) REFERENCES scai_rapporto(cod_ente, matricola)
)
```

### Vantaggi delle Chiavi Naturali

1. **Zero Accoppiamento**: I moduli non devono conoscersi tra loro
2. **FK Opzionali**: Le foreign key sono gestite a livello applicativo (commentate nelle migration)
3. **Deploy Separato**: Possibile deployare moduli separatamente
4. **Integrazione Facilitata**: Facile integrazione con sistemi esterni

## Mappa dei Moduli

### Moduli Implementati

#### 1. ente (Lookup - Order: 1)
**Descrizione**: Anagrafica enti regionali

**Tabelle**:
- `scai_ente`

**Dipendenze**: Nessuna

**Utilizzato da**: `rapporti`, `badge`, `policy`, `veicoli`

**Endpoints**:
- `GET /api/scai/ente`
- `GET /api/scai/ente/{id}`
- `POST /api/scai/ente`
- `PUT /api/scai/ente/{id}`
- `DELETE /api/scai/ente/{id}`

---

#### 2. sede (Lookup - Order: 2)
**Descrizione**: Anagrafica sedi di lavoro

**Tabelle**:
- `scai_sede`

**Dipendenze**: Nessuna

**Utilizzato da**: `rapporti`

**Endpoints**:
- `GET /api/scai/sede`
- `GET /api/scai/sede/{id}`
- `POST /api/scai/sede`
- `PUT /api/scai/sede/{id}`
- `DELETE /api/scai/sede/{id}`

---

#### 3. rapporti (Entity - Order: 10)
**Descrizione**: Gestione rapporti di lavoro dipendenti

**Tabelle**:
- `scai_rapporto`
- `scai_rapporto_foto`

**Dipendenze**:
- `ente` (required)
- `sede` (required)

**Dipendenze Opzionali**:
- `badge`
- `policy`
- `veicoli`

**Utilizzato da**: `badge`, `policy`, `veicoli`

**Endpoints**:
- `GET /api/scai/rapporto`
- `GET /api/scai/rapporto/{id}`
- `GET /api/scai/rapporto/create`
- `GET /api/scai/rapporto/foto/{id}`

---

### Moduli Da Implementare

#### 4. badge (Entity - Order: 20)
**Descrizione**: Gestione badge dipendenti

**Tabelle Previste**:
- `scai_badge`
- `scai_badge_tipo`
- `scai_badge_causale`
- `scai_badge_request`

**Dipendenze**:
- `ente` (required)
- `rapporti` (required)

---

#### 5. policy (Entity - Order: 20)
**Descrizione**: Gestione policy rapporti di lavoro

**Tabelle Previste**:
- `scai_policy_rapporto`
- `scai_policy_rapporto_validity`
- `scai_policy_default`

**Dipendenze**:
- `ente` (required)
- `rapporti` (required)
- `sdc` (required)
- `repertorio` (required)

---

#### 6. veicoli (Entity - Order: 20)
**Descrizione**: Gestione veicoli e convenzioni

**Tabelle Previste**:
- `scai_veicolo`
- `scai_veicolo_tipo`
- `scai_veicolo_alimentazione`
- `scai_veicolo_convenzione`

**Dipendenze**:
- `ente` (required)
- `rapporti` (required)

---

#### 7. sdc (Lookup - Order: 3)
**Descrizione**: Sistemi di Campo

**Tabelle Previste**:
- `scai_sistemi_di_campo`

**Dipendenze**: Nessuna

**Utilizzato da**: `policy`

---

#### 8. repertorio (Lookup - Order: 4)
**Descrizione**: Repertorio

**Tabelle Previste**:
- `scai_repertorio`

**Dipendenze**: Nessuna

**Utilizzato da**: `policy`

---

#### 9. varco (Lookup - Order: 5)
**Descrizione**: Varchi di accesso

**Tabelle Previste**:
- `scai_varchi`

**Dipendenze**: Nessuna

**Utilizzato da**: Altri moduli se necessario

---

## Grafo delle Dipendenze

```
Ordine Installazione:

[1] ente (lookup)
     ↓
[2] sede (lookup)
     ↓
[3] sdc (lookup)
     ↓
[4] repertorio (lookup)
     ↓
[5] varco (lookup)
     ↓
[10] rapporti (entity)
     ├──→ dipende da: ente, sede
     └──→ utilizzato da: badge, policy, veicoli
     ↓
[20] badge (entity)
     ├──→ dipende da: ente, rapporti
     │
[20] policy (entity)
     ├──→ dipende da: ente, rapporti, sdc, repertorio
     │
[20] veicoli (entity)
     └──→ dipende da: ente, rapporti
```

## Pattern Architetturali

### 1. Soft Delete
Tutti i moduli utilizzano il pattern soft delete:

```sql
is_not_deleted SMALLINT DEFAULT 1
-- 1 = record attivo
-- NULL = record cancellato
```

### 2. Audit Trail
Tutti i moduli tracciano le modifiche:

```sql
created_at TIMESTAMP NOT NULL DEFAULT NOW(),
created_by BIGINT,
updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
updated_by BIGINT
```

### 3. Trigger Auto-Update
Tutte le tabelle hanno trigger per aggiornare `updated_at`:

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### 4. Unique Key con Soft Delete
Chiavi univoche che rispettano il soft delete:

```sql
CONSTRAINT uq_ente_cod_ente
    UNIQUE (cod_ente, is_not_deleted)
```

## Frontend Architecture

### Stack Tecnologico
- **Lit** - Web Components framework
- **Bootstrap 5** - UI framework
- **Nanostores** - State management (se necessario)
- **VanillaJS** - Core JavaScript

### Pattern Componenti

Ogni modulo espone due componenti:

1. **{modulo}-module**: Wrapper principale
2. **{modulo}-component**: Logica CRUD con tre viste:
   - Lista (con filtri e paginazione)
   - Dettaglio (visualizzazione read-only)
   - Form (creazione/modifica)

### Disabilitazione Shadow DOM

Per compatibilità con Bootstrap:

```javascript
createRenderRoot() {
  return this; // Disable Shadow DOM
}
```

### Debounced Search

Ricerca con delay per ottimizzare le chiamate API:

```javascript
_onFilterChange() {
  clearTimeout(this._searchTimer);
  this._searchTimer = setTimeout(() => {
    this._page = 0;
    this._loadList();
  }, 400);
}
```

## Backend Architecture

### Laravel + PostgreSQL

Backend attuale basato su:
- Laravel 9+
- PostgreSQL
- Eloquent ORM
- DataTableHelper per paginazione server-side

### Migration Pattern

Naming convention PostgreSQL:

```
V{YYYYMMDD}_{NNNNNN}__{description}.sql
```

Esempio:
```
V20260304_000001__ente.sql
V20260304_000002__sede.sql
V20260304_000010__rapporti.sql
```

### Future Java/Spring Boot

Ogni modulo contiene stub Java per futura migrazione:

```
java/{modulo}/
├── dto/        # Record classes
├── dao/        # JPA Repositories
├── adapter/    # Adapter pattern
└── handler/    # Controllers
```

## Installation Order

I moduli devono essere installati in ordine crescente di `installation.order`:

1. **Order 1-5**: Moduli LOOKUP (nessuna dipendenza)
2. **Order 10+**: Moduli ENTITY (con dipendenze)

Il sistema di installazione verifica automaticamente le dipendenze dichiarate in `module.json` prima di procedere.

## Validazione Dipendenze

Prima dell'installazione, il sistema:

1. Legge `module.json` del modulo target
2. Verifica che tutti i moduli in `dependencies` siano installati
3. Controlla la versione compatibile (semver)
4. Blocca l'installazione se mancano dipendenze richieste

## Convenzioni di Naming

### Database
- Tabelle: `scai_{entita}` (es: `scai_rapporto`)
- Colonne: `snake_case` (es: `cod_ente`, `data_assunzione`)
- Indici: `idx_{tabella}_{colonna}` (es: `idx_rapporto_cod_ente`)
- Constraint: `uq_{tabella}_{descrizione}` (es: `uq_rapporto_matricola_ente`)

### Frontend
- Componenti: `{modulo}-component` (es: `ente-component`)
- Moduli: `{modulo}-module` (es: `ente-module`)
- File: `kebab-case` (es: `module-component.js`)

### Backend (Java)
- Package: `{app}.{modulo}.{layer}` (es: `com.scai.ente.dto`)
- DTO: `{Entita}DTO` (es: `EnteDTO`)
- Campi: `camelCase` (es: `codEnte`, `descrizioneEnte`)

## Vantaggi dell'Architettura

### 1. Modularità
Ogni modulo può essere sviluppato, testato e deployato indipendentemente.

### 2. Riusabilità
I moduli LOOKUP possono essere riutilizzati in progetti diversi senza modifiche.

### 3. Manutenibilità
Modifiche a un modulo non impattano altri moduli (eccetto breaking changes nelle API).

### 4. Scalabilità
Possibile aggiungere nuovi moduli senza refactoring dell'architettura esistente.

### 5. Testabilità
Ogni modulo può essere testato in isolamento con mock delle dipendenze.

### 6. Team Autonomi
Team diversi possono lavorare su moduli diversi senza conflitti.

## Antipattern da Evitare

### ❌ Includere tabelle di altri moduli
```sql
-- SBAGLIATO: rapporti migration include tabelle badge
CREATE TABLE scai_badge (...);  -- Appartiene al modulo badge!
```

### ❌ Dipendenze circolari
```json
// SBAGLIATO
// modulo A dipende da B, B dipende da A
{
  "name": "moduleA",
  "dependencies": { "moduleB": "^1.0.0" }
}
{
  "name": "moduleB",
  "dependencies": { "moduleA": "^1.0.0" }
}
```

### ❌ Foreign key fisiche cross-modulo nel backend Laravel
```sql
-- SBAGLIATO: FK fisica invece di gestione applicativa
ALTER TABLE scai_badge
ADD CONSTRAINT fk_badge_rapporto
FOREIGN KEY (matricola) REFERENCES scai_rapporto(matricola);
```

## Best Practices

### ✅ Un modulo = Una entità principale
Ogni modulo gestisce una sola entità principale e le sue dipendenze 1:1.

### ✅ Chiavi naturali per relazioni cross-modulo
Usare `(cod_ente, matricola)` invece di ID surrogate.

### ✅ FK commentate nelle migration
Le FK cross-modulo sono commentate e gestite a livello applicativo.

### ✅ Documentazione completa
Ogni modulo ha un README.md completo con esempi.

### ✅ Semantic versioning
Usare semantic versioning per le dipendenze (^1.0.0).

## Roadmap

### Fase 1 (Completata)
- [x] Definizione architettura modulare
- [x] Formato `module.json`
- [x] Modulo `ente` (lookup)
- [x] Modulo `sede` (lookup)
- [x] Modulo `rapporti` (entity)

### Fase 2 (Da Implementare)
- [ ] Modulo `sdc` (lookup)
- [ ] Modulo `repertorio` (lookup)
- [ ] Modulo `varco` (lookup)
- [ ] Modulo `badge` (entity)
- [ ] Modulo `policy` (entity)
- [ ] Modulo `veicoli` (entity)

### Fase 3 (Futuro)
- [ ] Sistema di installazione automatico
- [ ] Validazione dipendenze runtime
- [ ] Migrazione backend a Java/Spring Boot
- [ ] CLI per gestione moduli
- [ ] Registry moduli centralizzato

## Conclusioni

L'architettura modulare del sistema SCAI permette di costruire un sistema complesso mantenendo basso l'accoppiamento e alta la coesione. Ogni modulo è un'unità autonoma che può evolversi indipendentemente, facilitando la manutenzione e l'estensione del sistema nel tempo.

---

**Autore**: SCAI Team
**Versione Documento**: 1.0.0
**Data**: 2026-03-04
**Licenza**: Proprietary
