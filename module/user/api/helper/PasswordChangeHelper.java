package dev.jms.app.user.helper;

import dev.jms.app.user.dao.AccountDAO;
import dev.jms.app.user.dto.AccountAuthDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.ValidationException;

/**
 * Helper per la verifica della password corrente e l'aggiornamento con la nuova.
 */
public class PasswordChangeHelper
{
  /**
   * Verifica la password corrente e aggiorna l'hash. Lancia ValidationException in caso di errore.
   *
   * @param db         connessione DB
   * @param accountId  id dell'account autenticato
   * @param currentPw  password corrente in chiaro
   * @param newPw      nuova password in chiaro (già validata dalla policy)
   */
  public static void changePassword(DB db, int accountId, String currentPw, String newPw) throws Exception
  {
    AccountDAO accountDAO;
    AccountAuthDTO account;
    String newHash;

    accountDAO = new AccountDAO(db);
    account    = accountDAO.findForPasswordChange(accountId);
    if (account == null || !Auth.verifyPassword(currentPw, account.passwordHash())) {
      throw new ValidationException("Password corrente non valida");
    }
    newHash = Auth.hashPassword(newPw);
    accountDAO.updatePassword(accountId, newHash, false);
  }
}
