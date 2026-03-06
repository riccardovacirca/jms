package dev.crm.module.operatori.service;

import dev.crm.module.campagne.dao.CampagnaDao;
import dev.crm.module.campagne.dto.CampagnaDto;
import dev.crm.module.operatori.dao.AttivitaOperatoreDao;
import dev.crm.module.operatori.dao.OperatoreCampagnaDao;
import dev.crm.module.operatori.dto.OperatoreCampagnaDto;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CampagnaAssegnataService
{

  private final OperatoreCampagnaDao dao;
  private final AttivitaOperatoreDao attivitaDao;
  private final CampagnaDao campagnaDao;

  public CampagnaAssegnataService(
      OperatoreCampagnaDao dao, AttivitaOperatoreDao attivitaDao, CampagnaDao campagnaDao)
  {
    this.dao = dao;
    this.attivitaDao = attivitaDao;
    this.campagnaDao = campagnaDao;
  }

  public void assegna(Long operatoreId, Long campagnaId) throws Exception
  {
    dao.insert(operatoreId, campagnaId);
    attivitaDao.insert(
        operatoreId, "ASSEGNAZIONE_CAMPAGNA", "Assegnata campagna ID: " + campagnaId);
  }

  public void rimuovi(Long operatoreId, Long campagnaId) throws Exception
  {
    dao.delete(operatoreId, campagnaId);
    attivitaDao.insert(operatoreId, "RIMOZIONE_CAMPAGNA", "Rimossa campagna ID: " + campagnaId);
  }

  public List<OperatoreCampagnaDto> findByOperatoreId(Long operatoreId) throws Exception
  {
    return dao.findByOperatoreId(operatoreId);
  }

  public List<OperatoreCampagnaDto> findByCampagnaId(Long campagnaId) throws Exception
  {
    return dao.findByCampagnaId(campagnaId);
  }

  /**
   * Restituisce i dettagli completi delle campagne assegnate a un operatore.
   */
  public List<CampagnaDto> findCampagneByOperatoreId(Long operatoreId) throws Exception
  {
    List<OperatoreCampagnaDto> associazioni;
    List<CampagnaDto> campagne;

    associazioni = dao.findByOperatoreId(operatoreId);

    campagne = associazioni.stream()
        .map(assoc -> {
          try {
            return campagnaDao.findById(assoc.campagnaId);
          } catch (Exception e) {
            return null;
          }
        })
        .filter(opt -> opt != null && opt.isPresent())
        .map(opt -> opt.get())
        .collect(Collectors.toList());

    return campagne;
  }
}
