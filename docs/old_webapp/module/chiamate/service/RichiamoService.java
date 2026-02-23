package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.PromemoriaDao;
import dev.crm.module.chiamate.dao.RichiamoDao;
import dev.crm.module.chiamate.dao.StoricoRichiamoDao;
import dev.crm.module.chiamate.dto.PromemoriaDto;
import dev.crm.module.chiamate.dto.RichiamoDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RichiamoService
{

  private final RichiamoDao richiamoDao;
  private final PromemoriaDao promemoriaDao;
  private final StoricoRichiamoDao storicoDao;

  public RichiamoService(
      RichiamoDao richiamoDao, PromemoriaDao promemoriaDao, StoricoRichiamoDao storicoDao)
  {
    this.richiamoDao = richiamoDao;
    this.promemoriaDao = promemoriaDao;
    this.storicoDao = storicoDao;
  }

  public RichiamoDto create(RichiamoDto dto) throws Exception
  {
    long id = richiamoDao.insert(dto);

    // Crea promemoria automatico (2 minuti prima)
    if (dto.dataOra != null) {
      PromemoriaDto promemoria = new PromemoriaDto();
      promemoria.richiamoId = id;
      promemoria.minutiAnticipo = 2;
      promemoria.triggerAt = dto.dataOra.minusMinutes(2);
      promemoria.inviato = 0;
      promemoriaDao.insert(promemoria);
    }

    storicoDao.insert(id, dto.operatoreId, "CREAZIONE", "Richiamo creato");

    return richiamoDao
        .findById(id)
        .orElseThrow(() -> new Exception("Richiamo non trovato dopo creazione"));
  }

  public RichiamoDto update(RichiamoDto dto) throws Exception
  {
    richiamoDao.update(dto);
    storicoDao.insert(dto.id, dto.operatoreId, "MODIFICA", "Richiamo aggiornato");
    return richiamoDao.findById(dto.id).orElseThrow(() -> new Exception("Richiamo non trovato"));
  }

  public void delete(Long id) throws Exception
  {
    RichiamoDto richiamo = richiamoDao.findById(id).orElseThrow(() -> new Exception("Richiamo non trovato"));

    // Elimina promemoria associati
    promemoriaDao.deleteByRichiamoId(id);

    storicoDao.insert(id, richiamo.operatoreId, "ELIMINAZIONE", "Richiamo eliminato");
    richiamoDao.delete(id);
  }

  public Optional<RichiamoDto> findById(Long id) throws Exception
  {
    return richiamoDao.findById(id);
  }

  public List<RichiamoDto> findByOperatoreId(Long operatoreId) throws Exception
  {
    return richiamoDao.findByOperatoreId(operatoreId);
  }

  public List<RichiamoDto> findByOperatoreIdAndStato(Long operatoreId, String stato)
      throws Exception
  {
    return richiamoDao.findByOperatoreIdAndStato(operatoreId, stato);
  }

  public List<RichiamoDto> findImminenti(Long operatoreId) throws Exception
  {
    LocalDateTime limite = LocalDateTime.now().plusHours(1);
    return richiamoDao.findImminenti(operatoreId, limite);
  }

  public List<RichiamoDto> findAll() throws Exception
  {
    return richiamoDao.findAll();
  }

  public RichiamoDto posticipa(Long id, LocalDateTime nuovaDataOra) throws Exception
  {
    RichiamoDto richiamo = richiamoDao.findById(id).orElseThrow(() -> new Exception("Richiamo non trovato"));

    richiamoDao.updateDataOra(id, nuovaDataOra);

    // Aggiorna promemoria
    promemoriaDao.deleteByRichiamoId(id);
    PromemoriaDto promemoria = new PromemoriaDto();
    promemoria.richiamoId = id;
    promemoria.minutiAnticipo = 2;
    promemoria.triggerAt = nuovaDataOra.minusMinutes(2);
    promemoria.inviato = 0;
    promemoriaDao.insert(promemoria);

    storicoDao.insert(
        id, richiamo.operatoreId, "POSTICIPO", "Richiamo posticipato a: " + nuovaDataOra);

    return richiamoDao.findById(id).orElseThrow(() -> new Exception("Richiamo non trovato"));
  }

  public RichiamoDto annulla(Long id, String motivo) throws Exception
  {
    RichiamoDto richiamo = richiamoDao.findById(id).orElseThrow(() -> new Exception("Richiamo non trovato"));

    richiamoDao.updateStato(id, "ANNULLATO");

    // Elimina promemoria
    promemoriaDao.deleteByRichiamoId(id);

    storicoDao.insert(
        id,
        richiamo.operatoreId,
        "ANNULLAMENTO",
        "Motivo: " + (motivo != null ? motivo : "Non specificato"));

    return richiamoDao.findById(id).orElseThrow(() -> new Exception("Richiamo non trovato"));
  }

  public RichiamoDto completa(Long id) throws Exception
  {
    RichiamoDto richiamo = richiamoDao.findById(id).orElseThrow(() -> new Exception("Richiamo non trovato"));

    richiamoDao.updateStato(id, "COMPLETATO");
    storicoDao.insert(id, richiamo.operatoreId, "COMPLETAMENTO", "Richiamo completato");

    return richiamoDao.findById(id).orElseThrow(() -> new Exception("Richiamo non trovato"));
  }
}
