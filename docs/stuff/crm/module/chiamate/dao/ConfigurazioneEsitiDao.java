package dev.crm.module.chiamate.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.chiamate.dto.ConfigurazioneEsitiDto;
import java.util.ArrayList;
import java.util.HashMap;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class ConfigurazioneEsitiDao
{
  private final Map<String, ConfigurazioneEsitiDto> config = new ConcurrentHashMap<>();

  @PostConstruct
  public void init()
  {
    // Esiti predefiniti
    salva(crea("VENDITA", "Vendita conclusa", "POSITIVO", false, 60, true));
    salva(crea("APPUNTAMENTO", "Appuntamento fissato", "POSITIVO", false, 30, true));
    salva(crea("RICHIAMO", "Richiamo generico", "RICHIAMO", true, 0, true));
    salva(crea("RICHIAMO_APP", "Richiamo con appuntamento", "RICHIAMO", true, 0, true));
    salva(crea("NON_INTERESSATO", "Non interessato", "NEGATIVO", false, 10, true));
    salva(crea("NON_RISPONDE", "Non risponde", "GENERICO", true, 0, true));
    salva(crea("OCCUPATO", "Linea occupata", "GENERICO", true, 0, true));
    salva(crea("NUMERO_ERRATO", "Numero errato/inesistente", "NEGATIVO", false, 0, true));
    salva(crea("RIFIUTO", "Rifiuto immediato", "NEGATIVO", false, 0, true));
  }

  private ConfigurazioneEsitiDto crea(
      String codice, String desc, String tipo, boolean richiamo, int durata, boolean attivo)
  {
    ConfigurazioneEsitiDto dto = new ConfigurazioneEsitiDto();
    dto.codice = codice;
    dto.descrizione = desc;
    dto.tipoEsito = tipo;
    dto.abilitaRichiamo = richiamo;
    dto.durataMinima = durata;
    dto.attivo = attivo;
    return dto;
  }

  public ConfigurazioneEsitiDto salva(ConfigurazioneEsitiDto dto)
  {
    config.put(dto.codice, dto);
    return dto;
  }

  public Optional<ConfigurazioneEsitiDto> find(String codice)
  {
    return Optional.ofNullable(config.get(codice));
  }

  public List<ConfigurazioneEsitiDto> findAll()
  {
    return new ArrayList<>(config.values());
  }

  public List<ConfigurazioneEsitiDto> findAttivi()
  {
    List<ConfigurazioneEsitiDto> result = new ArrayList<>();
    for (ConfigurazioneEsitiDto c : config.values()) {
      if (c.attivo) {
        result.add(c);
      }
    }
    return result;
  }

  public void delete(String codice)
  {
    config.remove(codice);
  }
}
