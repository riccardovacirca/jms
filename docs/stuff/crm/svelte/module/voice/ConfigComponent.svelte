<script>
  import { operatorUserId } from './store.js'

  let userId;
  let savedMessage;

  userId = ''
  savedMessage = ''

  function handleSave() {
    if (!userId || userId.trim() === '') {
      savedMessage = 'Inserisci un userId valido'
      return
    }

    operatorUserId.set(userId.trim())
    savedMessage = `Configurazione salvata: ${userId.trim()}`
  }
</script>

<div class="config-component">
  <div class="config-header">
    <h3>Configurazione Operatore</h3>
    <p class="config-description">
      Inserisci l'userId Vonage dell'operatore.<br>
      Deve corrispondere al nome dell'utente creato con: <code>vonage users create --name='...'</code>
    </p>
  </div>

  <div class="config-form">
    <div class="form-group">
      <label for="userId">Operator User ID</label>
      <input
        id="userId"
        type="text"
        bind:value={userId}
        placeholder="operatore_01"
      />
    </div>

    <button class="btn-save" onclick={handleSave}>
      Salva Configurazione
    </button>

    {#if savedMessage}
      <div class="config-message">
        {savedMessage}
      </div>
    {/if}
  </div>

  {#if $operatorUserId}
    <div class="config-current">
      <strong>Configurazione attuale:</strong>
      <code>{$operatorUserId}</code>
    </div>
  {/if}

  <div class="config-info">
    <strong>Nota:</strong>
    <p>
      L'userId deve essere registrato su Vonage prima di poter essere utilizzato.
      Verifica con <code>vonage users list</code> che l'utente esista.
    </p>
  </div>
</div>

<style>
  .config-component {
    background: #fff;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    padding: 1.5rem;
    max-width: 600px;
  }

  .config-header h3 {
    margin: 0 0 0.5rem;
    color: #2c3e50;
    font-size: 1.1rem;
  }

  .config-description {
    margin: 0 0 1.5rem;
    color: #555;
    font-size: 0.9rem;
    line-height: 1.5;
  }

  .config-description code {
    background: #f5f5f5;
    padding: 0.2rem 0.4rem;
    border-radius: 3px;
    font-family: monospace;
    font-size: 0.85rem;
  }

  .config-form {
    margin-bottom: 1.5rem;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #333;
    font-weight: 500;
    font-size: 0.9rem;
  }

  .form-group input {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 0.95rem;
    font-family: monospace;
  }

  .form-group input:focus {
    outline: none;
    border-color: #2196f3;
  }

  .btn-save {
    width: 100%;
    padding: 0.875rem;
    background: #4caf50;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-save:hover {
    background: #45a049;
  }

  .config-message {
    margin-top: 1rem;
    padding: 0.75rem;
    background: #e8f5e9;
    border-left: 4px solid #4caf50;
    color: #2e7d32;
    font-size: 0.9rem;
  }

  .config-current {
    padding: 1rem;
    background: #f5f5f5;
    border-radius: 6px;
    margin-bottom: 1.5rem;
    font-size: 0.9rem;
  }

  .config-current strong {
    display: block;
    margin-bottom: 0.5rem;
    color: #333;
  }

  .config-current code {
    background: #fff;
    padding: 0.3rem 0.5rem;
    border-radius: 3px;
    font-family: monospace;
    color: #1976d2;
  }

  .config-info {
    padding-top: 1.5rem;
    border-top: 1px solid #e0e0e0;
    font-size: 0.85rem;
    color: #666;
  }

  .config-info strong {
    display: block;
    margin-bottom: 0.5rem;
    color: #333;
  }

  .config-info p {
    margin: 0;
    line-height: 1.6;
  }

  .config-info code {
    background: #f5f5f5;
    padding: 0.2rem 0.4rem;
    border-radius: 3px;
    font-family: monospace;
    font-size: 0.85rem;
  }
</style>
