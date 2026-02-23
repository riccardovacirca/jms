package dev.crm.module.sedi.service;

import dev.crm.module.sedi.dao.SedeCampagnaDao;
import dev.crm.module.sedi.dao.SedeDao;
import dev.crm.module.sedi.dao.SedeOperatoreDao;
import dev.crm.module.sedi.dto.SedeCampagnaDto;
import dev.crm.module.sedi.dto.SedeDto;
import dev.crm.module.sedi.dto.SedeOperatoreDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SedeService
{

  private final SedeDao dao;
  private final SedeOperatoreDao operatoreDao;
  private final SedeCampagnaDao campagnaDao;

  public SedeService(SedeDao dao, SedeOperatoreDao operatoreDao, SedeCampagnaDao campagnaDao)
  {
    this.dao = dao;
    this.operatoreDao = operatoreDao;
    this.campagnaDao = campagnaDao;
  }

  public SedeDto create(SedeDto dto) throws Exception
  {
    long id = dao.insert(dto);
    return dao.findById(id).orElseThrow(() -> new Exception("Sede non trovata dopo creazione"));
  }

  public SedeDto update(SedeDto dto) throws Exception
  {
    dao.update(dto);
    return dao.findById(dto.id).orElseThrow(() -> new Exception("Sede non trovata"));
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }

  public Optional<SedeDto> findById(Long id) throws Exception
  {
    return dao.findById(id);
  }

  public List<SedeDto> findAll() throws Exception
  {
    return dao.findAll();
  }

  // Gestione operatori
  public void associaOperatore(Long sedeId, Long operatoreId) throws Exception
  {
    operatoreDao.associa(sedeId, operatoreId);
  }

  public void rimuoviOperatore(Long sedeId, Long operatoreId) throws Exception
  {
    operatoreDao.rimuovi(sedeId, operatoreId);
  }

  public List<SedeOperatoreDto> findOperatoriBySedeId(Long sedeId) throws Exception
  {
    return operatoreDao.findBySedeId(sedeId);
  }

  // Gestione campagne
  public void associaCampagna(Long sedeId, Long campagnaId) throws Exception
  {
    campagnaDao.associa(sedeId, campagnaId);
  }

  public void rimuoviCampagna(Long sedeId, Long campagnaId) throws Exception
  {
    campagnaDao.rimuovi(sedeId, campagnaId);
  }

  public List<SedeCampagnaDto> findCampagneBySedeId(Long sedeId) throws Exception
  {
    return campagnaDao.findBySedeId(sedeId);
  }
}
