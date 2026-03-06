# Modulo Agenti

Gestione di agenti commerciali con disponibilità settimanale, appuntamenti e calcolo slot liberi.

---

## Entità principali

### Agente
Anagrafica dell'agente con flag `attivo`. Esposto via `AgenteController` su `/api/agenti`.

### Disponibilità (`DisponibilitaEntity`)
Fascia oraria settimanale ricorrente di un agente:
- `giornoSettimana` — intero 1–7 (1 = lunedì, 7 = domenica)
- `oraInizio` / `oraFine` — `LocalTime`, fascia di lavoro nel giorno

Un agente può avere più disponibilità per lo stesso giorno (es. mattina + pomeriggio).
Esposto via `DisponibilitaController` su `/api/agenti/{agenteId}/disponibilita`.

### Appuntamento (`AppuntamentoEntity`)
Appuntamento concreto assegnato a un agente in una data/ora specifica. Occupa una finestra temporale e blocca gli slot corrispondenti nella disponibilità settimanale.
Esposto via `AppuntamentoController` su `/api/agenti/appuntamenti`.

### Slot disponibile (`SlotDisponibileEntity`)
Entità calcolata (non persistita). Rappresenta una finestra temporale libera derivata incrociando la disponibilità settimanale con gli appuntamenti esistenti:
- `agenteId`, `nomeAgente`
- `dataOra` — `LocalDateTime` dello slot
- `durataMinuti` — durata dello slot

---

## Algoritmo di calcolo degli slot (`SlotDisponibileService`)

Il servizio calcola i prossimi slot disponibili per uno o tutti gli agenti, con i seguenti passi:

1. **Orizzonte temporale**: cerca fino a 30 giorni in avanti dalla data corrente.
2. **Espansione della disponibilità**: per ogni giorno nell'orizzonte, se il `giornoSettimana` corrisponde a una `DisponibilitaEntity` dell'agente, genera slot a partire da `oraInizio` fino a `oraFine`, avanzando di `durataMinuti` per volta.
3. **Verifica conflitti**: ogni slot generato viene confrontato con gli appuntamenti esistenti dell'agente. Uno slot è libero se nessun appuntamento ne occupa l'intervallo.
4. **Risultato**: lista ordinata di `SlotDisponibileEntity` liberi.

Endpoint esposti via `SlotController` su `/api/agenti`:
- `GET /{agenteId}/slot` — slot liberi per un agente specifico
- `GET /slot` — slot liberi per tutti gli agenti attivi
- `GET /{agenteId}/slots` — variante con parametri aggiuntivi

---

## Paginazione opzionale

Tutti i metodi GET che restituiscono liste accettano `limit` e `offset` come parametri opzionali (`required = false`).

- Se `limit` è assente → restituzione completa senza LIMIT/OFFSET
- Se `limit` è presente e `offset` è assente → `offset` defaulta a `0`
- La logica decisionale risiede interamente nel service, non nel controller

Struttura della response per le liste paginate:
```json
{
  "items": [...],
  "total": 42,
  "limit": 20,
  "offset": 0,
  "hasNext": true
}
```

---

## Struttura del modulo

```
agenti/
  controller/
    AgenteController.java          # CRUD agenti
    AppuntamentoController.java    # CRUD appuntamenti
    DisponibilitaController.java   # Gestione disponibilità settimanali
    SlotController.java            # Calcolo slot liberi
  service/
    AgenteService.java
    AppuntamentoService.java       # Parsing date, logica paginazione con filtro data
    DisponibilitaService.java
    SlotDisponibileService.java    # Algoritmo slot (logica in-memory, no DB)
  dao/
    AgenteDao.java
    AppuntamentoDao.java
    DisponibilitaDao.java
  entity/
    AgenteEntity.java
    AppuntamentoEntity.java
    DisponibilitaEntity.java
    SlotDisponibileEntity.java     # Entità calcolata, non persistita
```
