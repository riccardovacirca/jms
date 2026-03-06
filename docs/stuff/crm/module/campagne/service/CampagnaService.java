package dev.crm.module.campagne.service;

import dev.crm.module.campagne.dao.CampagnaDao;
import dev.crm.module.campagne.entity.CampagnaEntity;
import dev.crm.module.campagne.entity.CampagnaListaEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CampagnaService
{
  private final CampagnaDao dao;

  public CampagnaService(CampagnaDao dao)
  {
    this.dao = dao;
  }

  public Map<String, Object> findAll(Integer limit, Integer offset) throws Exception
  {
    List<CampagnaEntity> items;
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

  public Map<String, Object> findListe(Long campagnaId,
                                        Integer limit,
                                        Integer offset) throws Exception
  {
    List<CampagnaListaEntity> items;
    int total;
    int off;
    Map<String, Object> result;
    off = offset != null ? offset : 0;
    if (limit != null) {
      items = dao.findListeByCampagnaId(campagnaId, limit, off);
      total = dao.countListeByCampagnaId(campagnaId);
    } else {
      items = dao.findListeByCampagnaId(campagnaId);
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

  public CampagnaEntity create(CampagnaEntity entity) throws Exception
  {
    long id;
    CampagnaEntity result;
    id = dao.insert(entity);
    result = dao.findById(id);
    if (result == null) {
      throw new Exception("Campagna non trovata dopo creazione");
    }
    return result;
  }

  public CampagnaEntity update(CampagnaEntity entity) throws Exception
  {
    CampagnaEntity result;
    dao.update(entity);
    result = dao.findById(entity.id);
    if (result == null) {
      throw new Exception("Campagna non trovata");
    }
    return result;
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }

  public CampagnaEntity findById(Long id) throws Exception
  {
    return dao.findById(id);
  }

  public CampagnaEntity updateStato(Long id, Integer stato) throws Exception
  {
    CampagnaEntity result;
    dao.updateStato(id, stato);
    result = dao.findById(id);
    if (result == null) {
      throw new Exception("Campagna non trovata");
    }
    return result;
  }

  public void addLista(Long campagnaId, Long listaId) throws Exception
  {
    dao.addLista(campagnaId, listaId);
  }

  public void removeLista(Long campagnaId, Long listaId) throws Exception
  {
    dao.removeLista(campagnaId, listaId);
  }
}
