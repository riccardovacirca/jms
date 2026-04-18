/**
 * Logger frontend — invia messaggi di log al backend per integrazione
 * nel file di log del server, producendo un flusso misto frontend/backend.
 *
 * Il backend li scrive con il prefisso [FE] [username].
 * Gli errori di rete durante l'invio sono silenti: il logging non deve
 * mai bloccare o generare eccezioni nel flusso applicativo.
 *
 * Uso:
 *   import { Logger } from '../../../util/Logger.js';
 *   Logger.debug('Chiamata avviata', { callId, number });
 *   Logger.error('Errore SDK', { reason });
 */

/**
 * Invia un messaggio di log al backend endpoint POST /api/log.
 *
 * @param {string} level   - livello: debug|info|warn|error
 * @param {string} message - messaggio testuale
 * @param {object|string|null} context - dati aggiuntivi opzionali
 * @returns {Promise<void>}
 */
async function sendLog(level, message, context)
{
  let body;
  let contextStr;

  contextStr = null;
  if (context !== null && context !== undefined) {
    contextStr = typeof context === 'object' ? JSON.stringify(context) : String(context);
  }

  body = { level, message };
  if (contextStr !== null) {
    body.context = contextStr;
  }

  try {
    await fetch('/api/log', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  } catch (_) {
    // silente — il logging non deve mai bloccare il flusso applicativo
  }
}

const Logger = {
  /**
   * Log a livello debug.
   * @param {string} message
   * @param {object|string|null} [context]
   * @returns {Promise<void>}
   */
  debug(message, context = null) {
    return sendLog('debug', message, context);
  },

  /**
   * Log a livello info.
   * @param {string} message
   * @param {object|string|null} [context]
   * @returns {Promise<void>}
   */
  info(message, context = null) {
    return sendLog('info', message, context);
  },

  /**
   * Log a livello warn.
   * @param {string} message
   * @param {object|string|null} [context]
   * @returns {Promise<void>}
   */
  warn(message, context = null) {
    return sendLog('warn', message, context);
  },

  /**
   * Log a livello error.
   * @param {string} message
   * @param {object|string|null} [context]
   * @returns {Promise<void>}
   */
  error(message, context = null) {
    return sendLog('error', message, context);
  },
};

export { Logger };
