package dev.crm.module.agenti.service;

import dev.crm.module.agenti.dao.AgenteDao;
import dev.crm.module.agenti.dao.AppuntamentoDao;
import dev.crm.module.agenti.dao.DisponibilitaDao;
import dev.crm.module.agenti.entity.AgenteEntity;
import dev.crm.module.agenti.entity.AppuntamentoEntity;
import dev.crm.module.agenti.entity.DisponibilitaEntity;
import dev.crm.module.agenti.entity.SlotDisponibileEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SlotDisponibileService
{

  private final AgenteDao agenteDao;
  private final DisponibilitaDao disponibilitaDao;
  private final AppuntamentoDao appuntamentoDao;

  public SlotDisponibileService(
      AgenteDao agenteDao, DisponibilitaDao disponibilitaDao, AppuntamentoDao appuntamentoDao)
  {
    this.agenteDao = agenteDao;
    this.disponibilitaDao = disponibilitaDao;
    this.appuntamentoDao = appuntamentoDao;
  }

  /** Trova il prossimo slot disponibile per un agente specifico */
  public SlotDisponibileEntity findProssimoSlot(Long agenteId, Integer durataMinuti)
      throws Exception
  {
    AgenteEntity agente;
    List<DisponibilitaEntity> disponibilita;
    LocalDateTime now;
    LocalDate data;
    int durata;

    agente = agenteDao.findById(agenteId);
    if (agente == null || agente.attivo == 0) {
      return null;
    }

    disponibilita = disponibilitaDao.findByAgenteId(agenteId);
    if (disponibilita.isEmpty()) {
      return null;
    }

    now = LocalDateTime.now();
    data = now.toLocalDate();
    durata = durataMinuti != null ? durataMinuti : 30;

    // Cerca nei prossimi 30 giorni
    for (int i = 0; i < 30; i++) {
      LocalDate giornoCorrente = data.plusDays(i);
      int giornoSettimana = giornoCorrente.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

      for (DisponibilitaEntity disp : disponibilita) {
        if (disp.giornoSettimana.equals(giornoSettimana)) {
          LocalDateTime slotInizio = LocalDateTime.of(giornoCorrente, disp.oraInizio);
          LocalDateTime slotFine = LocalDateTime.of(giornoCorrente, disp.oraFine);
          List<AppuntamentoEntity> appuntamenti;

          // Verifica che lo slot sia nel futuro
          if (slotInizio.isBefore(now)) {
            continue;
          }

          // Verifica che ci sia abbastanza tempo
          if (slotInizio.plusMinutes(durata).isAfter(slotFine)) {
            continue;
          }

          // Verifica che non ci siano appuntamenti in conflitto
          appuntamenti = appuntamentoDao.findByAgenteIdAndData(
              agenteId, slotInizio, slotInizio.plusMinutes(durata));

          if (appuntamenti.isEmpty()) {
            return new SlotDisponibileEntity(
                agenteId, agente.nome + " " + agente.cognome, slotInizio, durata);
          }
        }
      }
    }

    return null;
  }

  /** Trova il prossimo slot disponibile tra tutti gli agenti attivi */
  public SlotDisponibileEntity findProssimoSlotTuttiAgenti(Integer durataMinuti)
      throws Exception
  {
    List<AgenteEntity> agentiAttivi;
    SlotDisponibileEntity slotPiuVicino;

    agentiAttivi = agenteDao.findByAttivo(1);
    slotPiuVicino = null;

    for (AgenteEntity agente : agentiAttivi) {
      SlotDisponibileEntity slot = findProssimoSlot(agente.id, durataMinuti);
      if (slot != null) {
        if (slotPiuVicino == null || slot.dataOra.isBefore(slotPiuVicino.dataOra)) {
          slotPiuVicino = slot;
        }
      }
    }

    return slotPiuVicino;
  }

  /** Trova tutti gli slot disponibili per un agente in un range di date */
  public List<SlotDisponibileEntity> findSlotDisponibili(
      Long agenteId, LocalDate dataInizio, LocalDate dataFine, Integer durataMinuti)
      throws Exception
  {
    List<SlotDisponibileEntity> slots = new ArrayList<>();

    AgenteEntity agente = agenteDao.findById(agenteId);
    if (agente == null || agente.attivo == 0) {
      return slots;
    }

    List<DisponibilitaEntity> disponibilita = disponibilitaDao.findByAgenteId(agenteId);
    if (disponibilita.isEmpty()) {
      return slots;
    }

    String nomeCompleto = agente.nome + " " + agente.cognome;
    LocalDate data = dataInizio;

    while (!data.isAfter(dataFine)) {
      int giornoSettimana = data.getDayOfWeek().getValue();

      for (DisponibilitaEntity disp : disponibilita) {
        if (disp.giornoSettimana.equals(giornoSettimana)) {
          LocalDateTime slotInizio = LocalDateTime.of(data, disp.oraInizio);
          LocalDateTime slotFine = LocalDateTime.of(data, disp.oraFine);

          // Genera slot ogni 30 minuti (o durata specificata)
          LocalDateTime currentSlot = slotInizio;
          int slotDuration = durataMinuti != null ? durataMinuti : 30;

          while (currentSlot.plusMinutes(slotDuration).isBefore(slotFine)
              || currentSlot.plusMinutes(slotDuration).isEqual(slotFine)) {

            // Verifica che sia nel futuro
            if (currentSlot.isAfter(LocalDateTime.now())) {
              // Verifica conflitti
              List<AppuntamentoEntity> appuntamenti = appuntamentoDao.findByAgenteIdAndData(
                  agenteId, currentSlot, currentSlot.plusMinutes(slotDuration));

              if (appuntamenti.isEmpty()) {
                slots.add(
                    new SlotDisponibileEntity(agenteId, nomeCompleto, currentSlot, slotDuration));
              }
            }

            currentSlot = currentSlot.plusMinutes(slotDuration);
          }
        }
      }

      data = data.plusDays(1);
    }

    return slots;
  }
}
