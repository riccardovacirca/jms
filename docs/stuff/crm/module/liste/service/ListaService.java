package dev.crm.module.liste.service;

import dev.crm.module.liste.dao.ListaDao;
import dev.crm.module.liste.dto.ListaContattoDto;
import dev.crm.module.liste.dto.ListaDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ListaService
{

  private final ListaDao dao;

  public ListaService(ListaDao dao)
  {
    this.dao = dao;
  }

  public ListaDto create(ListaDto dto) throws Exception
  {
    // Verifica nome duplicato
    if (dto.nome != null && dao.existsByNome(dto.nome, null)) {
      throw new Exception("Esiste già una lista con il nome \"" + dto.nome + "\"");
    }

    long id = dao.insert(dto);
    return dao.findById(id).orElseThrow(() -> new Exception("Lista non trovata dopo creazione"));
  }

  public ListaDto update(ListaDto dto) throws Exception
  {
    // Verifica nome duplicato (esclusa la lista corrente)
    if (dto.nome != null && dao.existsByNome(dto.nome, dto.id)) {
      throw new Exception("Esiste già una lista con il nome \"" + dto.nome + "\"");
    }

    dao.update(dto);
    return dao.findById(dto.id).orElseThrow(() -> new Exception("Lista non trovata"));
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }

  public Optional<ListaDto> findById(Long id) throws Exception
  {
    return dao.findById(id);
  }

  public List<ListaDto> findAll() throws Exception
  {
    return dao.findAll();
  }

  public List<ListaDto> findAll(int limit, int offset) throws Exception
  {
    return dao.findAll(limit, offset);
  }

  public int count() throws Exception
  {
    return dao.count();
  }

  public List<ListaDto> search(String query, int limit, int offset) throws Exception
  {
    return dao.search(query, limit, offset);
  }

  public int countSearch(String query) throws Exception
  {
    return dao.countSearch(query);
  }

  public ListaDto updateStato(Long id, Integer stato) throws Exception
  {
    dao.updateStato(id, stato);
    return dao.findById(id).orElseThrow(() -> new Exception("Lista non trovata"));
  }

  public ListaDto updateScadenza(Long id, LocalDate scadenza) throws Exception
  {
    dao.updateScadenza(id, scadenza);
    return dao.findById(id).orElseThrow(() -> new Exception("Lista non trovata"));
  }

  public void addContatto(Long listaId, Long contattoId) throws Exception
  {
    dao.addContatto(listaId, contattoId);
  }

  public void removeContatto(Long listaId, Long contattoId) throws Exception
  {
    dao.removeContatto(listaId, contattoId);
  }

  public List<ListaContattoDto> findContattiByListaId(Long listaId) throws Exception
  {
    return dao.findContattiByListaId(listaId);
  }
}
