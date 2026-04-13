# Fix critici

Raccolta di fix particolarmente critici o insidiosi, documentati per riferimento futuro.

---

## 2026-04-11 — `DB.rollback()` fallisce con "Cannot rollback when autoCommit is enabled"

### Sintomo

Errore di sistema su un endpoint che usa `db.begin()` / `db.rollback()`:

```
org.postgresql.util.PSQLException: Cannot rollback when autoCommit is enabled.
    at dev.jms.util.DB.rollback(DB.java:208)
    at ...SomeDAO.someMethod(SomeDAO.java:NNN)
```

L'eccezione originale (quella che ha causato l'ingresso nel `catch`) viene persa: `db.rollback()` lancia a sua volta, e `throw e` non viene mai raggiunto. `HandlerAdapter` vede e logga solo la `PSQLException` del rollback.

### Causa

Due problemi combinati:

**1. `db.select()` usato per una query DML (UPDATE … RETURNING)**

`db.select()` chiama internamente `stmt.executeQuery()`. Il driver PostgreSQL JDBC in alcune versioni considera `executeQuery()` valido solo per statement che restituiscono un `ResultSet` diretto (SELECT). Per un UPDATE con RETURNING il comportamento può essere imprevedibile — in certi casi l'esecuzione fallisce o lascia la connessione in uno stato inconsistente.

**2. `DB.rollback()` non difensivo**

```java
// PRIMA (bug)
public void rollback() throws Exception {
    Connection c = requireConnection();
    c.rollback();            // lancia se autoCommit=true
    c.setAutoCommit(true);
}
```

Se per qualsiasi motivo `autoCommit` risulta già `true` quando il `catch` chiama `db.rollback()`, il metodo lancia una seconda eccezione che nasconde completamente l'errore originale, rendendo il debug molto difficile.

**3. `catch` block che non protegge il rollback**

```java
// PRIMA (bug)
} catch (Exception e) {
    db.rollback();   // se lancia, throw e non viene raggiunto
    throw e;
}
```

### Fix

**`DB.rollback()` — controllo difensivo su autoCommit** (`dev.jms.util.DB`):

```java
// DOPO (fix)
public void rollback() throws Exception {
    Connection c = requireConnection();
    if (!c.getAutoCommit()) {
        c.rollback();
        c.setAutoCommit(true);
    }
}
```

Se la transazione non è attiva (autoCommit già true), il rollback è un no-op silenzioso. L'eccezione originale non viene mai nascosta.

**DAO — separare UPDATE da SELECT, proteggere il rollback nei catch**:

Usare `db.query()` per le DML e `db.select()` solo per query che restituiscono righe:

```java
// PRIMA (bug)
sql = "UPDATE ... WHERE id = ? RETURNING *";
rows = db.select(sql, operatoreId, id);   // executeQuery() su DML — problematico
db.commit();

// DOPO (fix)
sql = "UPDATE ... WHERE id = ?";
db.query(sql, operatoreId, id);           // executeUpdate() su DML — corretto

sql = "SELECT * FROM ... WHERE id = ?";
rows = db.select(sql, id);                // executeQuery() su SELECT — corretto
db.commit();
```

Proteggere il rollback nel catch per non perdere l'eccezione originale:

```java
} catch (Exception e) {
    try {
        db.rollback();
    } catch (Exception ignored) {}
    throw e;   // l'eccezione originale viene sempre propagata
}
```

---

## 2026-04-11 — `ClassCastException: PGobject cannot be cast to String` su colonne JSONB

### Sintomo

```
java.lang.ClassCastException: class org.postgresql.util.PGobject cannot be cast to class java.lang.String
    at ...SomeDAO.mapRow(SomeDAO.java:NNN)
```

### Causa

Il driver PostgreSQL JDBC restituisce le colonne di tipo `JSONB` come `PGobject`, non come `String`. Un cast diretto `(String) row.get("colonna_jsonb")` fallisce a runtime.

### Fix

Sostituire il cast diretto con `DB.toString()`, che chiama `value.toString()` — su `PGobject` questo restituisce correttamente il valore JSON:

```java
// PRIMA (bug)
(String) row.get("contatto_json")

// DOPO (fix)
DB.toString(row.get("contatto_json"))
```

### Regola generale

Non usare mai cast diretto a `String` in `mapRow()` per colonne di tipo `JSONB`, `JSON`, o altri tipi PostgreSQL-specifici. Usare sempre `DB.toString()`.

---

### Regola generale

- **`db.select()`** — solo per SELECT. Usa `executeQuery()` internamente.
- **`db.query()`** — per INSERT, UPDATE, DELETE senza RETURNING. Usa `executeUpdate()` internamente.
- Per INSERT/UPDATE con RETURNING: usare `db.query()` + `db.select()` separati all'interno della stessa transazione.
- Nei `catch` block che gestiscono transazioni, wrappare sempre `db.rollback()` in un try-catch per preservare l'eccezione originale.
