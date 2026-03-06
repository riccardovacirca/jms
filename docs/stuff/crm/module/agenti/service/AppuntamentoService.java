package dev.crm.module.agenti.service;

import dev.crm.module.agenti.dao.AppuntamentoDao;
import dev.crm.module.agenti.entity.AppuntamentoEntity;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AppuntamentoService
{
  private final AppuntamentoDao dao;

  public AppuntamentoService(AppuntamentoDao dao)
  {
    this.dao = dao;
  }

  public Map<String, Object> findAll(Integer limit, Integer offset) throws Exception
  {
    List<AppuntamentoEntity> items;
    int total;
    int off;
    Map<String, Object> result;
    off = offset != null ? offset : 0;
    if (limit != null) {
      items = dao.findAll(limit, off);
      total = dao.count();
    } else {
      items = dao.findAll();
      total = items.size();
    }
    result = new HashMap<>();
    result.put("items", items);
    result.put("offset", off);
    result.put("limit", limit);
    result.put("total", total);
    result.put("hasNext", limit != null && limit > 0 && (off + limit) < total);
    return result;
  }

  public Map<String, Object> findByAgente(Long agenteId,
                                           Integer limit,
                                           Integer offset,
                                           String dataInizio,
                                           String dataFine) throws Exception
  {
    List<AppuntamentoEntity> items;
    int total;
    int off;
    Map<String, Object> result;
    off = offset != null ? offset : 0;
    if (dataInizio != null && dataFine != null) {
      LocalDateTime inizio;
      LocalDateTime fine;
      inizio = LocalDateTime.parse(dataInizio);
      fine = LocalDateTime.parse(dataFine);
      if (limit != null) {
        items = dao.findByAgenteIdAndData(agenteId, inizio, fine, limit, off);
        total = dao.countByAgenteIdAndData(agenteId, inizio, fine);
      } else {
        items = dao.findByAgenteIdAndData(agenteId, inizio, fine);
        total = items.size();
      }
    } else {
      if (limit != null) {
        items = dao.findByAgenteId(agenteId, limit, off);
        total = dao.countByAgenteId(agenteId);
      } else {
        items = dao.findByAgenteId(agenteId);
        total = items.size();
      }
    }
    result = new HashMap<>();
    result.put("items", items);
    result.put("offset", off);
    result.put("limit", limit);
    result.put("total", total);
    result.put("hasNext", limit != null && limit > 0 && (off + limit) < total);
    return result;
  }

  public AppuntamentoEntity create(AppuntamentoEntity entity) throws Exception
  {
    long id;
    AppuntamentoEntity result;
    id = dao.insert(entity);
    result = dao.findById(id);
    if (result == null) {
      throw new Exception("Appuntamento non trovato dopo creazione");
    }
    return result;
  }

  public AppuntamentoEntity update(AppuntamentoEntity entity) throws Exception
  {
    AppuntamentoEntity result;
    dao.update(entity);
    result = dao.findById(entity.id);
    if (result == null) {
      throw new Exception("Appuntamento non trovato");
    }
    return result;
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }

  public AppuntamentoEntity findById(Long id) throws Exception
  {
    return dao.findById(id);
  }
}
