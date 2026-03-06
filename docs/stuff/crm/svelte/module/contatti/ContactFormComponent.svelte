<script>
  export let contatto = null;
  export let onSave = () => {};
  export let onCancel = () => {};

  let formData;
  let saving;
  let formError;

  saving = false;
  formError = null;

  // Inizializza form data
  if (contatto) {
    // Modalità modifica
    formData = {
      id: contatto.id,
      nome: contatto.nome || '',
      cognome: contatto.cognome || '',
      ragioneSociale: contatto.ragioneSociale || '',
      telefono: contatto.telefono || '',
      email: contatto.email || '',
      indirizzo: contatto.indirizzo || '',
      citta: contatto.citta || '',
      cap: contatto.cap || '',
      provincia: contatto.provincia || '',
      note: contatto.note || '',
      stato: contatto.stato !== undefined ? contatto.stato : 1,
      consenso: contatto.consenso || false,
      blacklist: contatto.blacklist || false
    };
  } else {
    // Modalità creazione
    formData = {
      nome: '',
      cognome: '',
      ragioneSociale: '',
      telefono: '',
      email: '',
      indirizzo: '',
      citta: '',
      cap: '',
      provincia: '',
      note: '',
      stato: 1,
      consenso: false,
      blacklist: false
    };
  }

  async function handleSubmit(event) {
    let response;
    let data;
    let method;
    let url;

    event.preventDefault();

    formError = null;
    saving = true;

    try {
      if (formData.id) {
        // Update
        method = 'PUT';
        url = `/api/liste/contatti/${formData.id}`;
      } else {
        // Create
        method = 'POST';
        url = '/api/liste/contatti';
      }

      response = await fetch(url, {
        method: method,
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      data = await response.json();

      if (data.err) {
        throw new Error(data.log || 'Errore durante il salvataggio');
      }

      onSave(data.out);
    } catch (e) {
      formError = e.message;
    } finally {
      saving = false;
    }
  }

  function handleCancel() {
    onCancel();
  }
</script>

<div class="contact-form-container">
  <div class="form-header">
    <h2>{contatto ? 'Modifica Contatto' : 'Nuovo Contatto'}</h2>
    <p>Inserisci i dati del contatto</p>
  </div>

  {#if formError}
    <div class="alert-error">
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
        <circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="2"/>
        <path d="M10 6v4M10 13h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <span>{formError}</span>
    </div>
  {/if}

  <form onsubmit={handleSubmit}>
    <!-- Informazioni Anagrafiche -->
    <div class="form-section">
      <h3 class="section-title">Informazioni Anagrafiche</h3>

      <div class="form-row">
        <div class="form-group">
          <label for="cognome">Cognome</label>
          <input
            type="text"
            id="cognome"
            bind:value={formData.cognome}
            class="form-input"
            placeholder="Rossi"
          />
        </div>

        <div class="form-group">
          <label for="nome">Nome</label>
          <input
            type="text"
            id="nome"
            bind:value={formData.nome}
            class="form-input"
            placeholder="Mario"
          />
        </div>
      </div>

      <div class="form-group">
        <label for="ragioneSociale">Ragione Sociale</label>
        <input
          type="text"
          id="ragioneSociale"
          bind:value={formData.ragioneSociale}
          class="form-input"
          placeholder="Azienda S.r.l."
        />
        <small class="form-hint">Lascia vuoto se si tratta di una persona fisica</small>
      </div>
    </div>

    <!-- Contatti -->
    <div class="form-section">
      <h3 class="section-title">Contatti</h3>

      <div class="form-row">
        <div class="form-group">
          <label for="telefono">Telefono</label>
          <input
            type="tel"
            id="telefono"
            bind:value={formData.telefono}
            class="form-input"
            placeholder="+39 123 456 7890"
          />
        </div>

        <div class="form-group">
          <label for="email">Email</label>
          <input
            type="email"
            id="email"
            bind:value={formData.email}
            class="form-input"
            placeholder="mario.rossi@esempio.it"
          />
        </div>
      </div>
    </div>

    <!-- Indirizzo -->
    <div class="form-section">
      <h3 class="section-title">Indirizzo</h3>

      <div class="form-group">
        <label for="indirizzo">Via/Piazza</label>
        <input
          type="text"
          id="indirizzo"
          bind:value={formData.indirizzo}
          class="form-input"
          placeholder="Via Roma, 123"
        />
      </div>

      <div class="form-row">
        <div class="form-group">
          <label for="citta">Città</label>
          <input
            type="text"
            id="citta"
            bind:value={formData.citta}
            class="form-input"
            placeholder="Milano"
          />
        </div>

        <div class="form-group">
          <label for="cap">CAP</label>
          <input
            type="text"
            id="cap"
            bind:value={formData.cap}
            class="form-input"
            placeholder="20100"
            maxlength="5"
          />
        </div>

        <div class="form-group">
          <label for="provincia">Provincia</label>
          <input
            type="text"
            id="provincia"
            bind:value={formData.provincia}
            class="form-input"
            placeholder="MI"
            maxlength="2"
          />
        </div>
      </div>
    </div>

    <!-- Stato e Opzioni -->
    <div class="form-section">
      <h3 class="section-title">Stato e Opzioni</h3>

      <div class="form-group">
        <label for="stato">Stato</label>
        <select id="stato" bind:value={formData.stato} class="form-input">
          <option value={1}>Attivo</option>
          <option value={0}>Inattivo</option>
          <option value={2}>Sospeso</option>
        </select>
      </div>

      <div class="form-checkboxes">
        <label class="checkbox-label">
          <input type="checkbox" bind:checked={formData.consenso} />
          <span>Consenso al trattamento dati</span>
        </label>

        <label class="checkbox-label">
          <input type="checkbox" bind:checked={formData.blacklist} />
          <span>Blacklist</span>
        </label>
      </div>
    </div>

    <!-- Note -->
    <div class="form-section">
      <h3 class="section-title">Note</h3>

      <div class="form-group">
        <label for="note">Note aggiuntive</label>
        <textarea
          id="note"
          bind:value={formData.note}
          class="form-textarea"
          placeholder="Inserisci eventuali note..."
          rows="4"
        ></textarea>
      </div>
    </div>

    <!-- Actions -->
    <div class="form-actions">
      <button type="button" class="btn btn-cancel" onclick={handleCancel} disabled={saving}>
        Annulla
      </button>
      <button type="submit" class="btn btn-save" disabled={saving}>
        {#if saving}
          <span class="spinner-small"></span>
          Salvataggio...
        {:else}
          {contatto ? 'Salva Modifiche' : 'Crea Contatto'}
        {/if}
      </button>
    </div>
  </form>
</div>

<style>
  .contact-form-container {
    max-width: 900px;
    margin: 0 auto;
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    padding: 2rem;
  }

  .form-header {
    margin-bottom: 2rem;
    padding-bottom: 1.5rem;
    border-bottom: 2px solid #e0e6ed;
  }

  .form-header h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.75rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .form-header p {
    margin: 0;
    color: #7f8c8d;
    font-size: 1rem;
  }

  .alert-error {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 1rem;
    margin-bottom: 1.5rem;
    background: #fee;
    border: 1px solid #fcc;
    border-radius: 8px;
    color: #c33;
    font-size: 0.9rem;
  }

  .form-section {
    margin-bottom: 2rem;
  }

  .section-title {
    margin: 0 0 1rem 0;
    font-size: 1.1rem;
    color: #2c3e50;
    font-weight: 600;
    padding-bottom: 0.5rem;
    border-bottom: 1px solid #e0e6ed;
  }

  .form-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-size: 0.9rem;
    font-weight: 500;
    color: #2c3e50;
  }

  .form-input,
  .form-textarea {
    width: 100%;
    padding: 0.75rem;
    border: 2px solid #e0e6ed;
    border-radius: 8px;
    font-size: 0.95rem;
    font-family: inherit;
    transition: all 0.2s;
  }

  .form-input:focus,
  .form-textarea:focus {
    outline: none;
    border-color: #3498db;
    box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
  }

  .form-textarea {
    resize: vertical;
    min-height: 100px;
  }

  .form-hint {
    display: block;
    margin-top: 0.25rem;
    font-size: 0.85rem;
    color: #7f8c8d;
  }

  .form-checkboxes {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    cursor: pointer;
    font-size: 0.95rem;
    color: #2c3e50;
  }

  .checkbox-label input[type="checkbox"] {
    width: 18px;
    height: 18px;
    cursor: pointer;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 2px solid #e0e6ed;
  }

  .btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    padding: 0.875rem 2rem;
    border: none;
    border-radius: 8px;
    font-size: 0.95rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    min-width: 150px;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-cancel {
    background: white;
    color: #2c3e50;
    border: 2px solid #e0e6ed;
  }

  .btn-cancel:hover:not(:disabled) {
    background: #f8f9fa;
    border-color: #cbd5e0;
  }

  .btn-save {
    background: #3498db;
    color: white;
  }

  .btn-save:hover:not(:disabled) {
    background: #2980b9;
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(52, 152, 219, 0.3);
  }

  .spinner-small {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top-color: white;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  @media (max-width: 768px) {
    .contact-form-container {
      padding: 1.5rem;
    }

    .form-row {
      grid-template-columns: 1fr;
    }

    .form-actions {
      flex-direction: column-reverse;
    }

    .btn {
      width: 100%;
    }
  }
</style>
