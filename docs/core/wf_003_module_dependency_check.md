# WF-003-MODULE-DEPENDENCY-CHECK

### Check delle dipendenze dei moduli

### Obiettivo

Verificare che tutte le dipendenze dei moduli installati siano presenti e loggare eventuali mancanze.

### Attori

* Applicazione (`App.main`)
* Loader risorse (`ClassLoader`)
* Mapper JSON (`ObjectMapper`)
* File moduli installati (`installed.json`)
* Logger console (`Console`)

### Precondizioni

* File `installed.json` presente nel classpath (opzionale)
* Moduli installati correttamente

---

### Flusso principale

1. `App` apre `installed.json` tramite `ClassLoader`
2. Se il file esiste:

   * `ObjectMapper` converte il JSON in mappe Java
   * Per ogni modulo, estrai le dipendenze

     * Se la dipendenza è presente → log ok
     * Se mancante → log warning
   * Se almeno una dipendenza è mancante → log warning generale
3. Se il file non esiste → check saltato e log

---

### Postcondizioni

* Tutte le dipendenze verificate
* Warning loggati per dipendenze mancanti o check saltato

---

### Diagramma di sequenza

```mermaid
sequenceDiagram
    participant App as App.main
    participant Loader as ClassLoader
    participant Mapper as ObjectMapper
    participant ModFile as installed.json
    participant Logger as Console

    App->>Loader: openResource("module/installed.json")
    alt file trovato
        Loader-->>App: InputStream
        App->>Mapper: readValue(InputStream)
        Mapper-->>App: Map<String, Map<String,Object>>

        loop per ogni modulo
            App->>App: estrai dipendenze
            alt dipendenza trovata
                App-->>Logger: ok
            else dipendenza mancante
                App-->>Logger: warn "Modulo X: dipendenza Y mancante"
            end
        end

        alt almeno una dipendenza mancante
            App-->>Logger: warn "Dipendenze moduli non soddisfatte"
        end
    else file non trovato
        App-->>Logger: skip check
    end
```
