# Documento di Pianificazione

## Architettura ibrida Blocking / Non-Blocking con `@Async`

---

## 1. Obiettivo

Introdurre un modello ibrido su base Undertow che:

* Mantenga compatibilità con handler blocking esistenti
* Permetta handler contrassegnati `@Async`
* Supporti:

  * Lettura body non-blocking
  * Scrittura risposta non-blocking
  * Delegazione operazioni lente a executor dedicato
* Continui a usare JDBC + HikariCP

---

## 2. Architettura Target

```
IO Thread
   ↓
AsyncHandlerAdapter
   ├─ Handler blocking → worker thread + startBlocking()
   └─ Handler @Async → requestReceiver → dispatch(executor) → responseSender
```

---

## 3. Componenti da Introdurre

### 3.1 Annotation

* `@Async`
* Retention runtime
* Target TYPE

Scopo: marcare handler da gestire in modalità non-blocking.

---

### 3.2 Evoluzione HandlerAdapter

Nuova logica:

| Caso                   | Comportamento                    |
| ---------------------- | -------------------------------- |
| Handler senza `@Async` | comportamento attuale (blocking) |
| Handler con `@Async`   | pipeline async                   |

Pipeline async:

1. `exchange.getRequestReceiver().receiveFullBytes(...)`
2. `dispatch(asyncExecutor, ...)`
3. Apertura DB
4. Invocazione handler
5. Scrittura risposta con `getResponseSender()`

---

### 3.3 Executor dedicato

Creare:

```java
ExecutorService asyncExecutor
```

Linee guida:

* Separato dal worker Undertow
* Dimensionato in base a:

  * query lente attese
  * dimensione pool DB
* Monitorabile

---

## 4. Evoluzione HttpRequest

### Nuove funzionalità

* Costruttore con `byte[] body`
* `getBody()` non bloccante (già disponibile)
* Nessun uso di `InputStream` per handler async

Comportamento:

| Modalità | Lettura Body                |
| -------- | --------------------------- |
| Blocking | startBlocking + InputStream |
| Async    | body già letto dal receiver |

---

## 5. Evoluzione HttpResponse

Requisiti:

* Supporto `send(String)` non-bloccante
* Impostazione header prima della scrittura
* No `OutputStream`

Metodo raccomandato:

```java
exchange.getResponseSender().send(payload);
```

Garantire:

* Idempotenza `send()`
* Gestione errori se responseStarted

---

## 6. Flusso Async Completo

1. IO thread riceve richiesta
2. `HandlerAdapter` rileva `@Async`
3. Lettura body non-blocking
4. Dispatch su `asyncExecutor`
5. Operazione DB (bloccante ma isolata)
6. Scrittura risposta non-blocking
7. Fine exchange

---

## 7. Compatibilità

* Nessuna modifica agli handler blocking esistenti
* Nessuna modifica a DB layer
* Routing invariato
* Nessuna rottura API pubblica

---

## 8. Linee Guida di Utilizzo

Usare `@Async` solo per:

* Query lente
* Operazioni lunghe
* Endpoint ad alta concorrenza

Non necessario per:

* CRUD veloci
* Query leggere
* Endpoint amministrativi

---

## 9. Rischi e Mitigazioni

| Rischio                       | Mitigazione                     |
| ----------------------------- | ------------------------------- |
| Saturazione executor          | dimensionamento corretto        |
| Saturazione pool DB           | allineare pool DB a executor    |
| Errori di doppia risposta     | controllo `isResponseStarted()` |
| Memory pressure (body grandi) | limite dimensione body          |

---

## 10. Fasi di Implementazione

### Fase 1

* Creazione annotation `@Async`

### Fase 2

* Refactor `HandlerAdapter`

### Fase 3

* Aggiornamento `HttpRequest`

### Fase 4

* Aggiornamento `HttpResponse`

### Fase 5

* Test carico:

  * blocking puro
  * async puro
  * mix

### Fase 6

* Monitoraggio thread pool e connessioni DB

---

## 11. Risultato Atteso

Sistema:

* Ibrido
* Retrocompatibile
* IO thread sempre liberi
* Controllo fine su endpoint lenti
* Nessun cambio paradigma (no reactive DB)
