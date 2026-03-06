<script>
  export let operatore = null;
  export let onSave = () => {};
  export let onCancel = () => {};

  import { salvaOperatore, error } from './store.js';

  let isEditMode = operatore != null;

  let formData = {
    id: operatore?.id || null,
    nome: operatore?.nome || '',
    cognome: operatore?.cognome || '',
    email: operatore?.email || '',
    telefono: operatore?.telefono || '',
    username: operatore?.username || '',
    statoAttuale: operatore?.statoAttuale || 'OFFLINE'
  };

  let validationErrors = {};

  function validate() {
    validationErrors = {};

    if (!formData.nome.trim()) {
      validationErrors.nome = 'Il nome è obbligatorio';
    }

    if (!formData.cognome.trim()) {
      validationErrors.cognome = 'Il cognome è obbligatorio';
    }

    if (!formData.email.trim()) {
      validationErrors.email = 'L\'email è obbligatoria';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      validationErrors.email = 'Email non valida';
    }

    if (!formData.username.trim()) {
      validationErrors.username = 'Lo username è obbligatorio';
    }

    return Object.keys(validationErrors).length === 0;
  }

  async function handleSubmit() {
    if (!validate()) {
      return;
    }

    const success = await salvaOperatore(formData);
    if (success) {
      onSave();
    }
  }

  function handleCancelClick() {
    onCancel();
  }
</script>

<div class="form-container">
  <div class="form-header">
    <h2>{isEditMode ? 'Modifica Operatore' : 'Nuovo Operatore'}</h2>
  </div>

  <form on:submit|preventDefault={handleSubmit}>
    <div class="form-grid">
      <div class="form-group">
        <label for="nome">Nome *</label>
        <input
          id="nome"
          type="text"
          bind:value={formData.nome}
          class:error={validationErrors.nome}
          placeholder="Inserisci il nome"
        />
        {#if validationErrors.nome}
          <span class="error-message">{validationErrors.nome}</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="cognome">Cognome *</label>
        <input
          id="cognome"
          type="text"
          bind:value={formData.cognome}
          class:error={validationErrors.cognome}
          placeholder="Inserisci il cognome"
        />
        {#if validationErrors.cognome}
          <span class="error-message">{validationErrors.cognome}</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="email">Email *</label>
        <input
          id="email"
          type="email"
          bind:value={formData.email}
          class:error={validationErrors.email}
          placeholder="email@esempio.com"
        />
        {#if validationErrors.email}
          <span class="error-message">{validationErrors.email}</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="username">Username *</label>
        <input
          id="username"
          type="text"
          bind:value={formData.username}
          class:error={validationErrors.username}
          placeholder="username"
        />
        {#if validationErrors.username}
          <span class="error-message">{validationErrors.username}</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="telefono">Telefono</label>
        <input
          id="telefono"
          type="tel"
          bind:value={formData.telefono}
          placeholder="+39 123 456 7890"
        />
      </div>

      <div class="form-group">
        <label for="statoAttuale">Stato</label>
        <select id="statoAttuale" bind:value={formData.statoAttuale}>
          <option value="OFFLINE">OFFLINE</option>
          <option value="ONLINE">ONLINE</option>
          <option value="PAUSA">PAUSA</option>
        </select>
      </div>
    </div>

    {#if $error}
      <div class="form-error">
        Errore: {$error}
      </div>
    {/if}

    <div class="form-actions">
      <button type="button" class="btn-secondary" on:click={handleCancelClick}>
        Annulla
      </button>
      <button type="submit" class="btn-primary">
        {isEditMode ? 'Salva Modifiche' : 'Crea Operatore'}
      </button>
    </div>
  </form>
</div>

<style>
  .form-container {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    padding: 2rem;
    max-width: 800px;
  }

  .form-header {
    margin-bottom: 2rem;
  }

  .form-header h2 {
    margin: 0;
    font-size: 1.75rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .form-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 1.5rem;
    margin-bottom: 1.5rem;
  }

  .form-group {
    display: flex;
    flex-direction: column;
  }

  .form-group label {
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #2c3e50;
  }

  .form-group input,
  .form-group select {
    padding: 0.75rem;
    border: 1px solid #ced4da;
    border-radius: 4px;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .form-group input:focus,
  .form-group select:focus {
    outline: none;
    border-color: #3498db;
  }

  .form-group input.error {
    border-color: #e74c3c;
  }

  .error-message {
    color: #e74c3c;
    font-size: 0.875rem;
    margin-top: 0.25rem;
  }

  .form-error {
    padding: 1rem;
    background: #f8d7da;
    color: #721c24;
    border-radius: 4px;
    margin-bottom: 1.5rem;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
  }

  .btn-primary, .btn-secondary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-primary {
    background: #3498db;
    color: white;
  }

  .btn-primary:hover {
    background: #2980b9;
  }

  .btn-secondary {
    background: #95a5a6;
    color: white;
  }

  .btn-secondary:hover {
    background: #7f8c8d;
  }
</style>
