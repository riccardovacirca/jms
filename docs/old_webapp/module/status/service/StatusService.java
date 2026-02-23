package dev.crm.module.status.service;

import dev.crm.module.agenti.dao.AgenteDao;
import dev.crm.module.campagne.dao.CampagnaDao;
import dev.crm.module.chiamate.dao.ChiamataDao;
import dev.crm.module.contatti.dao.ContattoDao;
import dev.crm.module.liste.dao.ListaDao;
import dev.crm.module.operatori.dao.OperatoreDao;
import dev.crm.module.sedi.dao.SedeDao;
import dev.crm.module.status.dao.StatusDao;
import dev.crm.module.status.dto.StatusHealthDto;
import dev.crm.module.status.dto.StatusLogDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StatusService
{

  private final StatusDao dao;
  private final CampagnaDao campagnaDao;
  private final ListaDao listaDao;
  private final ContattoDao contattoDao;
  private final OperatoreDao operatoreDao;
  private final AgenteDao agenteDao;
  private final SedeDao sedeDao;
  private final ChiamataDao chiamataDao;

  public StatusService(
      StatusDao dao,
      CampagnaDao campagnaDao,
      ListaDao listaDao,
      ContattoDao contattoDao,
      OperatoreDao operatoreDao,
      AgenteDao agenteDao,
      SedeDao sedeDao,
      ChiamataDao chiamataDao)
  {
    this.dao = dao;
    this.campagnaDao = campagnaDao;
    this.listaDao = listaDao;
    this.contattoDao = contattoDao;
    this.operatoreDao = operatoreDao;
    this.agenteDao = agenteDao;
    this.sedeDao = sedeDao;
    this.chiamataDao = chiamataDao;
  }

  public StatusHealthDto getHealth()
  {
    try {
      int campagne;
      int liste;
      int contatti;
      int operatori;
      int agenti;
      int sedi;
      int chiamate;
      StatusHealthDto result;

      campagne = campagnaDao.count();
      liste = listaDao.count();
      contatti = contattoDao.count();
      operatori = operatoreDao.count();
      agenti = agenteDao.count();
      sedi = sedeDao.count();
      chiamate = chiamataDao.count();

      result = new StatusHealthDto(
          "UP", campagne, liste, contatti, operatori, agenti, sedi, chiamate);

      return result;
    } catch (Exception e) {
      StatusHealthDto fallback;

      fallback = new StatusHealthDto("UP", 0, 0, 0, 0, 0, 0, 0);

      return fallback;
    }
  }

  public StatusLogDto log(String message) throws Exception
  {
    long id;
    StatusLogDto result;

    id = dao.insertLog(message);
    result = new StatusLogDto(id, message, java.time.LocalDateTime.now());

    return result;
  }

  public List<StatusLogDto> getLogs(int limit, int offset) throws Exception
  {
    return dao.findLogs(limit, offset);
  }
}
