package dev.jms.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Classe di utilità per l'invio di email via SMTP.
 * Wrappa Jakarta Mail (angus-mail). Supporta testo semplice e HTML.
 * Autenticazione opzionale: per test locali con Mailpit usare mail.auth=false.
 *
 * Inizializzare una volta all'avvio con Mail.init(), poi usare Mail.get()
 * ovunque nel codice.
 */
public class Mail
{
  private static Mail instance;

  private final Session session;
  private final String from;

  private Mail(String host, int port, boolean auth, String user, String password, String from)
  {
    Properties props;

    props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", String.valueOf(port));

    if (auth) {
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.starttls.enable", "true");
      this.session = Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
          return new PasswordAuthentication(user, password);
        }
      });
    } else {
      props.put("mail.smtp.auth", "false");
      this.session = Session.getInstance(props);
    }

    this.from = from;
  }

  /**
   * Da chiamare una volta in App.main() prima di avviare il server.
   * Non inizializza se mail.host o mail.from sono assenti.
   * Se mail.auth=true, richiede anche mail.user e mail.password.
   */
  public static void init(Config config)
  {
    String host;
    String from;
    int port;
    boolean auth;
    String user;
    String password;

    if (!config.get("mail.enabled", "false").equalsIgnoreCase("true")) {
      System.out.println("[info] Mail disabilitato (mail.enabled=false)");
      return;
    }

    host = config.get("mail.host", "");
    from = config.get("mail.from", "");

    if (host.isBlank() || from.isBlank()) {
      System.out.println("[info] Mail non configurato, invio email disabilitato");
      return;
    }

    port     = config.getInt("mail.port", 1025);
    auth     = config.get("mail.auth", "false").equalsIgnoreCase("true");
    user     = config.get("mail.user", "");
    password = config.get("mail.password", "");

    if (auth && (user.isBlank() || password.isBlank())) {
      System.out.println("[warn] Mail: mail.auth=true ma mail.user o mail.password mancanti, invio email disabilitato");
      return;
    }

    instance = new Mail(host, port, auth, user, password, from);
    System.out.println("[info] Mail inizializzato (" + host + ":" + port + ")");
  }

  public static boolean isConfigured()
  {
    return instance != null;
  }

  public static Mail get()
  {
    if (instance == null) {
      throw new IllegalStateException("Mail non inizializzato — chiamare Mail.init()");
    }
    return instance;
  }

  /** Invia un'email in testo semplice. */
  public void send(String to, String subject, String text) throws Exception
  {
    MimeMessage msg;

    msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(from));
    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
    msg.setSubject(subject, "UTF-8");
    msg.setText(text, "UTF-8");
    Transport.send(msg);
  }

  /** Invia un'email con corpo HTML. */
  public void sendHtml(String to, String subject, String html) throws Exception
  {
    MimeMessage msg;

    msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(from));
    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
    msg.setSubject(subject, "UTF-8");
    msg.setContent(html, "text/html; charset=UTF-8");
    Transport.send(msg);
  }
}
