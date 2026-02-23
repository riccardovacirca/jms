<script>
  import { onMount } from 'svelte'
  import {
    currentStep,
    loading,
    error,
    aziendaData,
    sediData,
    ownerData,
    goToStep,
    nextStep,
    prevStep,
    reset
  } from './store.js'
  import { wizardCompleted } from '../../store.js'

  let submitting = false

  const totalSteps = 4

  async function handleSubmit() {
    const payload = {
      azienda: $aziendaData,
      sedi: $sediData,
      ownerAccount: $ownerData,
      adminAccounts: [],
      configurazioni: []
    }

    submitting = true
    error.set(null)

    try {
      const response = await fetch('/api/init/complete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })

      const data = await response.json()

      if (data.err) {
        error.set(data.log || 'Errore durante il completamento del wizard')
        submitting = false
        return
      }

      // Wizard completato con successo
      wizardCompleted.set(true)
      reset()
      window.location.reload()
    } catch (e) {
      error.set('Errore di connessione')
      submitting = false
    }
  }

  function addSede() {
    sediData.update(sedi => [
      ...sedi,
      {
        nome: '',
        indirizzo: '',
        cap: '',
        citta: '',
        provincia: '',
        nazione: 'Italia',
        numeroPostazioni: null,
        responsabileNome: '',
        telefono: '',
        email: '',
        attiva: 1
      }
    ])
  }

  function removeSede(index) {
    sediData.update(sedi => sedi.filter((_, i) => i !== index))
  }

  function validateStep1() {
    if (!$aziendaData.ragioneSociale) {
      error.set('Ragione sociale obbligatoria')
      return false
    }
    return true
  }

  function validateStep3() {
    if (!$ownerData.username || !$ownerData.password || !$ownerData.email) {
      error.set('Username, password e email obbligatori per account owner')
      return false
    }
    if ($ownerData.password.length < 8) {
      error.set('La password deve essere di almeno 8 caratteri')
      return false
    }
    return true
  }

  function handleNext() {
    if ($currentStep === 1 && !validateStep1()) return
    if ($currentStep === 3 && !validateStep3()) return
    nextStep()
  }
</script>

<div class="container-fluid min-vh-100 d-flex align-items-center justify-content-center bg-light">
  <div class="card shadow-lg" style="max-width: 900px; width: 100%;">
    <div class="card-header bg-primary text-white">
      <h2 class="mb-0">
        <i class="bi bi-gear-fill me-2"></i>
        Configurazione Iniziale
      </h2>
      <p class="mb-0 mt-2">Step {$currentStep} di {totalSteps}</p>
    </div>

    <div class="card-body p-4">
      <!-- Progress Bar -->
      <div class="progress mb-4" style="height: 8px;">
        <div
          class="progress-bar bg-primary"
          role="progressbar"
          style="width: {($currentStep / totalSteps) * 100}%"
          aria-valuenow={$currentStep}
          aria-valuemin="0"
          aria-valuemax={totalSteps}
        ></div>
      </div>

      {#if $error}
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
          <i class="bi bi-exclamation-triangle-fill me-2"></i>
          {$error}
          <button
            type="button"
            class="btn-close"
            on:click={() => error.set(null)}
            aria-label="Close"
          ></button>
        </div>
      {/if}

      <!-- Step 1: Dati Azienda -->
      {#if $currentStep === 1}
        <h4 class="mb-4">
          <i class="bi bi-building me-2"></i>
          Dati Azienda
        </h4>

        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label" for="ragioneSociale">Ragione Sociale *</label>
            <input
              id="ragioneSociale"
              type="text"
              class="form-control"
              bind:value={$aziendaData.ragioneSociale}
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="formaGiuridica">Forma Giuridica</label>
            <input id="formaGiuridica" type="text" class="form-control" bind:value={$aziendaData.formaGiuridica} />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="partitaIva">Partita IVA</label>
            <input id="partitaIva" type="text" class="form-control" bind:value={$aziendaData.partitaIva} />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="codiceFiscale">Codice Fiscale</label>
            <input id="codiceFiscale" type="text" class="form-control" bind:value={$aziendaData.codiceFiscale} />
          </div>

          <div class="col-12">
            <hr class="my-4" />
            <h5>Sede Legale</h5>
          </div>

          <div class="col-12">
            <label class="form-label" for="sedeLegaleIndirizzo">Indirizzo</label>
            <input
              id="sedeLegaleIndirizzo"
              type="text"
              class="form-control"
              bind:value={$aziendaData.sedeLegaleIndirizzo}
            />
          </div>

          <div class="col-md-4">
            <label class="form-label" for="sedeLegaleCap">CAP</label>
            <input id="sedeLegaleCap" type="text" class="form-control" bind:value={$aziendaData.sedeLegaleCap} />
          </div>

          <div class="col-md-4">
            <label class="form-label" for="sedeLegaleCitta">Città</label>
            <input id="sedeLegaleCitta" type="text" class="form-control" bind:value={$aziendaData.sedeLegaleCitta} />
          </div>

          <div class="col-md-4">
            <label class="form-label" for="sedeLegaleProvincia">Provincia</label>
            <input
              id="sedeLegaleProvincia"
              type="text"
              class="form-control"
              bind:value={$aziendaData.sedeLegaleProvincia}
            />
          </div>

          <div class="col-12">
            <hr class="my-4" />
            <h5>Contatti</h5>
          </div>

          <div class="col-md-6">
            <label class="form-label" for="telefonoGenerale">Telefono</label>
            <input id="telefonoGenerale" type="tel" class="form-control" bind:value={$aziendaData.telefonoGenerale} />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="emailGenerale">Email</label>
            <input id="emailGenerale" type="email" class="form-control" bind:value={$aziendaData.emailGenerale} />
          </div>
        </div>
      {/if}

      <!-- Step 2: Sedi Operative -->
      {#if $currentStep === 2}
        <h4 class="mb-4">
          <i class="bi bi-geo-alt-fill me-2"></i>
          Sedi Operative (Opzionale)
        </h4>

        {#if $sediData.length === 0}
          <div class="alert alert-info">
            <i class="bi bi-info-circle me-2"></i>
            Nessuna sede operativa configurata. Puoi aggiungerne se necessario.
          </div>
        {/if}

        {#each $sediData as sede, i}
          <div class="card mb-3">
            <div class="card-body">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="mb-0">Sede #{i + 1}</h5>
                <button type="button" class="btn btn-sm btn-danger" on:click={() => removeSede(i)}>
                  <i class="bi bi-trash"></i>
                  Rimuovi
                </button>
              </div>

              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label" for="sede-nome-{i}">Nome Sede *</label>
                  <input id="sede-nome-{i}" type="text" class="form-control" bind:value={sede.nome} required />
                </div>

                <div class="col-md-6">
                  <label class="form-label" for="sede-citta-{i}">Città</label>
                  <input id="sede-citta-{i}" type="text" class="form-control" bind:value={sede.citta} />
                </div>

                <div class="col-md-6">
                  <label class="form-label" for="sede-responsabile-{i}">Responsabile</label>
                  <input id="sede-responsabile-{i}" type="text" class="form-control" bind:value={sede.responsabileNome} />
                </div>

                <div class="col-md-6">
                  <label class="form-label" for="sede-postazioni-{i}">Numero Postazioni</label>
                  <input
                    id="sede-postazioni-{i}"
                    type="number"
                    class="form-control"
                    bind:value={sede.numeroPostazioni}
                    min="1"
                  />
                </div>
              </div>
            </div>
          </div>
        {/each}

        <button type="button" class="btn btn-outline-primary" on:click={addSede}>
          <i class="bi bi-plus-circle me-2"></i>
          Aggiungi Sede
        </button>
      {/if}

      <!-- Step 3: Account Owner -->
      {#if $currentStep === 3}
        <h4 class="mb-4">
          <i class="bi bi-person-fill-gear me-2"></i>
          Account Amministratore Principale
        </h4>

        <div class="alert alert-warning">
          <i class="bi bi-exclamation-triangle me-2"></i>
          Questo account avrà accesso completo al sistema. Conserva le credenziali in modo sicuro.
        </div>

        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label" for="ownerNome">Nome *</label>
            <input id="ownerNome" type="text" class="form-control" bind:value={$ownerData.nome} required />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="ownerCognome">Cognome *</label>
            <input id="ownerCognome" type="text" class="form-control" bind:value={$ownerData.cognome} required />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="ownerUsername">Username *</label>
            <input id="ownerUsername" type="text" class="form-control" bind:value={$ownerData.username} required />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="ownerEmail">Email *</label>
            <input id="ownerEmail" type="email" class="form-control" bind:value={$ownerData.email} required />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="ownerPassword">Password * (min 8 caratteri)</label>
            <input
              id="ownerPassword"
              type="password"
              class="form-control"
              bind:value={$ownerData.password}
              required
              minlength="8"
            />
          </div>

          <div class="col-md-6">
            <label class="form-label" for="ownerTelefono">Telefono</label>
            <input id="ownerTelefono" type="tel" class="form-control" bind:value={$ownerData.telefono} />
          </div>
        </div>
      {/if}

      <!-- Step 4: Recap e Conferma -->
      {#if $currentStep === 4}
        <h4 class="mb-4">
          <i class="bi bi-check-circle me-2"></i>
          Riepilogo Configurazione
        </h4>

        <div class="card mb-3">
          <div class="card-header bg-light">
            <h5 class="mb-0">Azienda</h5>
          </div>
          <div class="card-body">
            <p class="mb-1">
              <strong>Ragione Sociale:</strong>
              {$aziendaData.ragioneSociale}
            </p>
            {#if $aziendaData.partitaIva}
              <p class="mb-1">
                <strong>P.IVA:</strong>
                {$aziendaData.partitaIva}
              </p>
            {/if}
            {#if $aziendaData.emailGenerale}
              <p class="mb-1">
                <strong>Email:</strong>
                {$aziendaData.emailGenerale}
              </p>
            {/if}
          </div>
        </div>

        {#if $sediData.length > 0}
          <div class="card mb-3">
            <div class="card-header bg-light">
              <h5 class="mb-0">Sedi Operative ({$sediData.length})</h5>
            </div>
            <div class="card-body">
              <ul class="list-unstyled mb-0">
                {#each $sediData as sede}
                  <li>
                    <i class="bi bi-geo-alt text-primary me-2"></i>
                    {sede.nome}
                    {#if sede.citta}- {sede.citta}{/if}
                  </li>
                {/each}
              </ul>
            </div>
          </div>
        {/if}

        <div class="card mb-3">
          <div class="card-header bg-light">
            <h5 class="mb-0">Account Owner</h5>
          </div>
          <div class="card-body">
            <p class="mb-1">
              <strong>Nome:</strong>
              {$ownerData.nome}
              {$ownerData.cognome}
            </p>
            <p class="mb-1">
              <strong>Username:</strong>
              {$ownerData.username}
            </p>
            <p class="mb-0">
              <strong>Email:</strong>
              {$ownerData.email}
            </p>
          </div>
        </div>

        <div class="alert alert-warning">
          <i class="bi bi-exclamation-triangle me-2"></i>
          <strong>Attenzione:</strong>
          Una volta confermato, il wizard non potrà più essere eseguito.
        </div>
      {/if}
    </div>

    <!-- Footer con bottoni navigazione -->
    <div class="card-footer bg-light">
      <div class="d-flex justify-content-between">
        <button
          type="button"
          class="btn btn-secondary"
          on:click={prevStep}
          disabled={$currentStep === 1 || submitting}
        >
          <i class="bi bi-arrow-left me-2"></i>
          Indietro
        </button>

        {#if $currentStep < totalSteps}
          <button type="button" class="btn btn-primary" on:click={handleNext} disabled={submitting}>
            Avanti
            <i class="bi bi-arrow-right ms-2"></i>
          </button>
        {:else}
          <button
            type="button"
            class="btn btn-success"
            on:click={handleSubmit}
            disabled={submitting}
          >
            {#if submitting}
              <span class="spinner-border spinner-border-sm me-2" role="status"></span>
              Salvataggio...
            {:else}
              <i class="bi bi-check-circle me-2"></i>
              Completa Configurazione
            {/if}
          </button>
        {/if}
      </div>
    </div>
  </div>
</div>
