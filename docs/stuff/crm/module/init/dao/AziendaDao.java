package dev.crm.module.init.dao;

import dev.crm.module.init.dto.AziendaDto;
import dev.springtools.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sql.DataSource;

public class AziendaDao
{
  private final DataSource dataSource;

  public AziendaDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public AziendaDto findFirst() throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    AziendaDto dto;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select("SELECT * FROM azienda LIMIT 1");
      if (rs.size() == 0) {
        return null;
      }
      dto = mapToDto(rs.get(0));
      return dto;
    } finally {
      db.release();
    }
  }

  public Long insert(AziendaDto dto) throws Exception
  {
    DB db;
    Long id;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.begin();

      db.query(
          "INSERT INTO azienda (ragione_sociale, forma_giuridica, partita_iva, codice_fiscale, "
              + "codice_sdi, pec, numero_rea, capitale_sociale, sede_legale_indirizzo, "
              + "sede_legale_cap, sede_legale_citta, sede_legale_provincia, sede_legale_nazione, "
              + "telefono_generale, email_generale, sito_web, referente_commerciale, "
              + "referente_tecnico, intestatario_fatturazione, indirizzo_fatturazione, iban, "
              + "modalita_pagamento, regime_iva) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          dto.ragioneSociale,
          dto.formaGiuridica,
          dto.partitaIva,
          dto.codiceFiscale,
          dto.codiceSdi,
          dto.pec,
          dto.numeroRea,
          dto.capitaleSociale,
          dto.sedeLegaleIndirizzo,
          dto.sedeLegaleCap,
          dto.sedeLegaleCitta,
          dto.sedeLegaleProvincia,
          dto.sedeLegaleNazione,
          dto.telefonoGenerale,
          dto.emailGenerale,
          dto.sitoWeb,
          dto.referenteCommerciale,
          dto.referenteTecnico,
          dto.intestatarioFatturazione,
          dto.indirizzoFatturazione,
          dto.iban,
          dto.modalitaPagamento,
          dto.regimeIva);

      id = db.lastInsertId();
      db.commit();
      return id;
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.release();
    }
  }

  private AziendaDto mapToDto(java.util.Map<String, Object> row)
  {
    AziendaDto dto;
    Object idObj;

    dto = new AziendaDto();
    idObj = row.get("id");
    dto.id = idObj instanceof Integer ? ((Integer) idObj).longValue() : (Long) idObj;
    dto.ragioneSociale = (String) row.get("ragione_sociale");
    dto.formaGiuridica = (String) row.get("forma_giuridica");
    dto.partitaIva = (String) row.get("partita_iva");
    dto.codiceFiscale = (String) row.get("codice_fiscale");
    dto.codiceSdi = (String) row.get("codice_sdi");
    dto.pec = (String) row.get("pec");
    dto.numeroRea = (String) row.get("numero_rea");
    dto.capitaleSociale = (String) row.get("capitale_sociale");
    dto.sedeLegaleIndirizzo = (String) row.get("sede_legale_indirizzo");
    dto.sedeLegaleCap = (String) row.get("sede_legale_cap");
    dto.sedeLegaleCitta = (String) row.get("sede_legale_citta");
    dto.sedeLegaleProvincia = (String) row.get("sede_legale_provincia");
    dto.sedeLegaleNazione = (String) row.get("sede_legale_nazione");
    dto.telefonoGenerale = (String) row.get("telefono_generale");
    dto.emailGenerale = (String) row.get("email_generale");
    dto.sitoWeb = (String) row.get("sito_web");
    dto.referenteCommerciale = (String) row.get("referente_commerciale");
    dto.referenteTecnico = (String) row.get("referente_tecnico");
    dto.intestatarioFatturazione = (String) row.get("intestatario_fatturazione");
    dto.indirizzoFatturazione = (String) row.get("indirizzo_fatturazione");
    dto.iban = (String) row.get("iban");
    dto.modalitaPagamento = (String) row.get("modalita_pagamento");
    dto.regimeIva = (String) row.get("regime_iva");
    return dto;
  }
}
