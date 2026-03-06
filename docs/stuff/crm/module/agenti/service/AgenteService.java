package dev.crm.module.agenti.service;

import dev.crm.module.agenti.dao.AgenteDao;
import dev.crm.module.agenti.entity.AgenteEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AgenteService
{
  private final AgenteDao dao;

  public AgenteService(AgenteDao dao)
  {
    this.dao = dao;
  }

  public Map<String, Object> findAll(Integer limit, Integer offset, Integer attivo) throws Exception
  {
    List<AgenteEntity> items;
    int total;
    int off;
    Map<String, Object> result;
    off = offset != null ? offset : 0;
    if (attivo != null) {
      if (limit != null) {
        items = dao.findByAttivo(attivo, limit, off);
        total = dao.countByAttivo(attivo);
      } else {
        items = dao.findByAttivo(attivo);
        total = items.size();
      }
    } else {
      if (limit != null) {
        items = dao.findAll(limit, off);
        total = dao.count();
      } else {
        items = dao.findAll();
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

  public AgenteEntity create(AgenteEntity entity) throws Exception
  {
    long id;
    AgenteEntity result;
    id = dao.insert(entity);
    result = dao.findById(id);
    if (result == null) {
      throw new Exception("Agente non trovato dopo creazione");
    }
    return result;
  }

  public AgenteEntity update(AgenteEntity entity) throws Exception
  {
    AgenteEntity result;
    dao.update(entity);
    result = dao.findById(entity.id);
    if (result == null) {
      throw new Exception("Agente non trovato");
    }
    return result;
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }

  public AgenteEntity findById(Long id) throws Exception
  {
    return dao.findById(id);
  }
}
