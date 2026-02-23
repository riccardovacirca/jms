package dev.crm.module.operatori.service;

import dev.crm.module.operatori.dao.AttivitaOperatoreDao;
import dev.crm.module.operatori.dao.OperatoreDao;
import dev.crm.module.operatori.dao.StatoOperatoreDao;
import dev.crm.module.operatori.dto.OperatoreDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OperatoreService
{

  private final OperatoreDao operatoreDao;
  private final StatoOperatoreDao statoDao;
  private final AttivitaOperatoreDao attivitaDao;

  public OperatoreService(
      OperatoreDao operatoreDao, StatoOperatoreDao statoDao, AttivitaOperatoreDao attivitaDao)
  {
    this.operatoreDao = operatoreDao;
    this.statoDao = statoDao;
    this.attivitaDao = attivitaDao;
  }

  public OperatoreDto create(OperatoreDto dto) throws Exception
  {
    long id = operatoreDao.insert(dto);
    attivitaDao.insert(id, "CREAZIONE", "Operatore creato");
    return operatoreDao
        .findById(id)
        .orElseThrow(() -> new Exception("Operatore non trovato dopo creazione"));
  }

  public OperatoreDto update(OperatoreDto dto) throws Exception
  {
    operatoreDao.update(dto);
    attivitaDao.insert(dto.id, "MODIFICA", "Dati operatore aggiornati");
    return operatoreDao.findById(dto.id).orElseThrow(() -> new Exception("Operatore non trovato"));
  }

  public void delete(Long id) throws Exception
  {
    attivitaDao.insert(id, "ELIMINAZIONE", "Operatore eliminato");
    operatoreDao.delete(id);
  }

  public Optional<OperatoreDto> findById(Long id) throws Exception
  {
    return operatoreDao.findById(id);
  }

  public List<OperatoreDto> findAll() throws Exception
  {
    return operatoreDao.findAll();
  }

  public List<OperatoreDto> findByStato(String stato) throws Exception
  {
    return operatoreDao.findByStato(stato);
  }

  public OperatoreDto updateStato(Long id, String stato) throws Exception
  {
    operatoreDao.updateStatoAttuale(id, stato);
    statoDao.insert(id, stato);
    attivitaDao.insert(id, "CAMBIO_STATO", "Stato cambiato in: " + stato);
    return operatoreDao.findById(id).orElseThrow(() -> new Exception("Operatore non trovato"));
  }
}
