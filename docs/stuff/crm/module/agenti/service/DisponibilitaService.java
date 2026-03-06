package dev.crm.module.agenti.service;

import dev.crm.module.agenti.dao.DisponibilitaDao;
import dev.crm.module.agenti.entity.DisponibilitaEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DisponibilitaService
{
  private final DisponibilitaDao dao;

  public DisponibilitaService(DisponibilitaDao dao)
  {
    this.dao = dao;
  }

  public Map<String, Object> findByAgente(Long agenteId,
                                           Integer limit,
                                           Integer offset) throws Exception
  {
    List<DisponibilitaEntity> items;
    int total;
    int off;
    Map<String, Object> result;
    off = offset != null ? offset : 0;
    if (limit != null) {
      items = dao.findByAgenteId(agenteId, limit, off);
      total = dao.countByAgenteId(agenteId);
    } else {
      items = dao.findByAgenteId(agenteId);
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

  public DisponibilitaEntity create(DisponibilitaEntity entity) throws Exception
  {
    long id;
    id = dao.insert(entity);
    entity.id = id;
    return entity;
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }
}
