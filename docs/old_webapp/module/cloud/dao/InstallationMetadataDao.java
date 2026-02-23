package dev.crm.module.cloud.dao;

import dev.crm.module.cloud.dto.InstallationMetadataDto;
import dev.springtools.util.DB;



import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InstallationMetadataDao
{
  private final DataSource dataSource;

  public InstallationMetadataDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public Optional<InstallationMetadataDto> findByInstallationId(String installationId) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    InstallationMetadataDto dto;

    db = new DB(dataSource);

    try {
      db.acquire();

      rs = db.select(
        "SELECT * FROM installation_metadata WHERE installation_id = ? AND is_active = 1",
        installationId
      );

      if (rs.size() == 0) {
        return Optional.empty();
      }

      dto = mapToDto(rs.get(0));

      return Optional.of(dto);
    }
    finally {
      db.release();
    }
  }

  public InstallationMetadataDto insert(InstallationMetadataDto dto) throws Exception
  {
    DB db;
    Long id;

    db = new DB(dataSource);

    try {
      db.acquire();
      db.begin();

      db.query(
        "INSERT INTO installation_metadata " +
        "(installation_id, installation_name, shared_secret, cloud_webhook_url, is_active, created_at, updated_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)",
        dto.installationId,
        dto.installationName,
        dto.sharedSecret,
        dto.cloudWebhookUrl,
        dto.isActive != null ? (dto.isActive ? 1 : 0) : 1,
        DB.toSqlTimestamp(LocalDateTime.now()),
        DB.toSqlTimestamp(LocalDateTime.now())
      );

      id = db.lastInsertId();

      db.commit();

      dto.id = id;

      return dto;
    }
    catch (Exception e) {
      db.rollback();
      throw e;
    }
    finally {
      db.release();
    }
  }

  public void update(InstallationMetadataDto dto) throws Exception
  {
    DB db;

    db = new DB(dataSource);

    try {
      db.acquire();
      db.begin();

      db.query(
        "UPDATE installation_metadata SET " +
        "installation_name = ?, " +
        "shared_secret = ?, " +
        "cloud_webhook_url = ?, " +
        "is_active = ?, " +
        "updated_at = ? " +
        "WHERE installation_id = ?",
        dto.installationName,
        dto.sharedSecret,
        dto.cloudWebhookUrl,
        dto.isActive != null ? (dto.isActive ? 1 : 0) : 1,
        DB.toSqlTimestamp(LocalDateTime.now()),
        dto.installationId
      );

      db.commit();
    }
    catch (Exception e) {
      db.rollback();
      throw e;
    }
    finally {
      db.release();
    }
  }

  private InstallationMetadataDto mapToDto(Map<String, Object> row)
  {
    InstallationMetadataDto dto;

    dto = new InstallationMetadataDto();

    dto.id = row.get("id") != null ? ((Number) row.get("id")).longValue() : null;
    dto.installationId = (String) row.get("installation_id");
    dto.installationName = (String) row.get("installation_name");
    dto.sharedSecret = (String) row.get("shared_secret");
    dto.cloudWebhookUrl = (String) row.get("cloud_webhook_url");

    Object isActiveObj;
    isActiveObj = row.get("is_active");

    if (isActiveObj != null) {
      if (isActiveObj instanceof Number) {
        dto.isActive = ((Number) isActiveObj).intValue() == 1;
      }
      else if (isActiveObj instanceof Boolean) {
        dto.isActive = (Boolean) isActiveObj;
      }
    }

    DateTimeFormatter formatter;
    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    dto.createdAt = row.get("created_at") != null
      ? LocalDateTime.parse(row.get("created_at").toString(), formatter)
      : null;

    dto.updatedAt = row.get("updated_at") != null
      ? LocalDateTime.parse(row.get("updated_at").toString(), formatter)
      : null;

    return dto;
  }
}
