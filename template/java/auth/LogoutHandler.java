package {{APP_PACKAGE}}.auth;

import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;

public class LogoutHandler implements Handler
{
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String refreshToken;

    refreshToken = req.getCookie("refresh_token");

    if (refreshToken != null) {
      db.query("DELETE FROM refresh_tokens WHERE token = ?", refreshToken);
    }

    res.status(200)
       .contentType("application/json")
       .cookie("access_token", "", 0)
       .cookie("refresh_token", "", 0)
       .err(false).log(null).out(null)
       .send();
  }
}
