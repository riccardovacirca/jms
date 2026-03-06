package dev.crm.module.contatti.service;

import dev.crm.module.contatti.dao.ContattoDao;
import dev.crm.module.contatti.entity.ContattoEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ContattoService
{
  private final ContattoDao dao;

  public ContattoService(ContattoDao dao)
  {
    this.dao = dao;
  }

  public Map<String, Object> findAll(Integer limit, Integer offset, Long listaId) throws Exception
  {
    List<ContattoEntity> items;
    int total;
    int off;
    Map<String, Object> result;
    off = offset != null ? offset : 0;
    if (listaId != null) {
      if (limit != null) {
        items = dao.findByListaId(listaId, limit, off);
        total = dao.countByListaId(listaId);
      } else {
        items = dao.findByListaId(listaId);
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

  public ContattoEntity create(ContattoEntity entity) throws Exception
  {
    long id;
    ContattoEntity result;
    id = dao.insert(entity);
    result = dao.findById(id);
    if (result == null) {
      throw new Exception("Contatto non trovato dopo creazione");
    }
    return result;
  }

  public ContattoEntity update(ContattoEntity entity) throws Exception
  {
    ContattoEntity result;
    dao.update(entity);
    result = dao.findById(entity.id);
    if (result == null) {
      throw new Exception("Contatto non trovato");
    }
    return result;
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }

  public ContattoEntity findById(Long id) throws Exception
  {
    return dao.findById(id);
  }

  public List<ContattoEntity> search(String query, int limit) throws Exception
  {
    return dao.search(query, limit);
  }

  public ContattoEntity updateStato(Long id, Integer stato) throws Exception
  {
    ContattoEntity result;
    dao.updateStato(id, stato);
    result = dao.findById(id);
    if (result == null) {
      throw new Exception("Contatto non trovato");
    }
    return result;
  }

  public ContattoEntity setBlacklist(Long id, Boolean blacklist) throws Exception
  {
    ContattoEntity result;
    dao.setBlacklist(id, blacklist);
    result = dao.findById(id);
    if (result == null) {
      throw new Exception("Contatto non trovato");
    }
    return result;
  }
}
