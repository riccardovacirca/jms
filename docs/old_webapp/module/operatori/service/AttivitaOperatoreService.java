package dev.crm.module.operatori.service;

import dev.crm.module.operatori.dao.AttivitaOperatoreDao;
import dev.crm.module.operatori.dto.AttivitaOperatoreDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AttivitaOperatoreService
{

  private final AttivitaOperatoreDao dao;

  public AttivitaOperatoreService(AttivitaOperatoreDao dao)
  {
    this.dao = dao;
  }

  public void registra(Long operatoreId, String azione, String descrizione) throws Exception
  {
    dao.insert(operatoreId, azione, descrizione);
  }

  public List<AttivitaOperatoreDto> findByOperatoreId(Long operatoreId, Integer limit)
      throws Exception
  {
    return dao.findByOperatoreId(operatoreId, limit);
  }

  public List<AttivitaOperatoreDto> findAll(Integer limit) throws Exception
  {
    return dao.findAll(limit);
  }
}
