package dev.crm.module.chiamate.controller;

import dev.crm.module.chiamate.dao.LogChiamataDao;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chiamate/log")
public class LogChiamataController
{
  private final LogChiamataDao logDao;

  public LogChiamataController(LogChiamataDao logDao)
  {
    this.logDao = logDao;
  }

  @GetMapping
  public ApiPayload all()
  {
    List<LogChiamataDao.LogEntry> data = logDao.all();
    return ApiResponse.create().out(data).build();
  }

  @GetMapping("/{chiamataId}")
  public ApiPayload byChiamata(@PathVariable Long chiamataId)
  {
    List<LogChiamataDao.LogEntry> data = logDao.byChiamata(chiamataId);
    return ApiResponse.create().out(data).build();
  }
}
