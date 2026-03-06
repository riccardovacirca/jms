<script>
  import { onMount, onDestroy } from 'svelte'
  import {
    campagneAttive, campagnaSelezionata,
    listeAttive, listaSelezionata,
    contattoCorrente, contatti,
    loading, error, inChiamata, autoMode,
    chiamateNumeri,
    caricaCampagneAttive,
    selezionaCampagna, selezionaLista,
    avviaChiamata, avviaChiamataAutomatica,
    fermaChiamate, prosegui
  } from './store.js'

  let dropdownOpen = $state(false)
  let contattiRimanenti = $state(0)
  let totalContatti = $state(0)

  onMount(() => {
    caricaCampagneAttive()
  })

  onDestroy(() => {
    fermaChiamate()
  })

  $effect(() => {
    totalContatti = $contatti.length
    contattiRimanenti = $contatti.filter(c =>
      c.telefono && !c.blacklist && !$chiamateNumeri.has(c.telefono)
    ).length
  })

  function displayName(c) {
    if (c.ragioneSociale) return c.ragioneSociale
    return [c.nome, c.cognome].filter(Boolean).join(' ') || `Contatto #${c.id}`
  }

  function formatDate(dateStr) {
    if (!dateStr) return ''
    const [y, m, d] = dateStr.split('-')
    const mesi = ['gen', 'feb', 'mar', 'apr', 'mag', 'giu', 'lug', 'ago', 'set', 'ott', 'nov', 'dic']
    return `${d} ${mesi[parseInt(m) - 1]} ${y}`
  }

  async function handleManuale() {
    if (!$contattoCorrente || $inChiamata) return
    dropdownOpen = false
    await avviaChiamata($contattoCorrente)
  }

  async function handleAutomatica() {
    if (!$contattoCorrente || $inChiamata) return
    dropdownOpen = false
    avviaChiamataAutomatica()
  }

  function handleClickOutside(event) {
    if (dropdownOpen && !event.target.closest('.split-btn-group')) {
      dropdownOpen = false
    }
  }

  function handleCampagnaChange(e) {
    const id = Number(e.target.value)
    const campagna = $campagneAttive.find(c => Number(c.id) === id)
    if (campagna) selezionaCampagna(campagna)
  }

  function handleListaChange(e) {
    const id = Number(e.target.value)
    const lista = $listeAttive.find(l => Number(l.id) === id)
    if (lista) selezionaLista(lista)
  }
</script>

<svelte:window onclick={handleClickOutside} />

<div class="chiamate-panel container-lg py-4">
  {#if $error}
    <div class="alert alert-danger alert-dismissible" role="alert">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      {$error}
      <button type="button" class="btn-close" onclick={() => error.set(null)} aria-label="Chiudi"></button>
    </div>
  {/if}

  {#if $loading}
    <div class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Caricamento...</span>
      </div>
    </div>
  {:else}

    <!-- Selezionatori campagna / lista -->
    <div class="row g-3 mb-4">
      <div class="col-md-6">
        <label for="campagna-select" class="form-label section-label">
          <i class="bi bi-megaphone me-2"></i>
          <strong>Campagna</strong>
        </label>
        {#if $campagneAttive.length === 0}
          <select id="campagna-select" class="form-select" disabled>
            <option>Nessuna campagna attiva</option>
          </select>
        {:else}
          <select id="campagna-select" class="form-select" value={$campagnaSelezionata?.id} onchange={handleCampagnaChange}>
            {#each $campagneAttive as campagna}
              <option value={campagna.id}>
                {campagna.nome}{campagna.dataFine ? ` – Scad. ${formatDate(campagna.dataFine)}` : ''}
              </option>
            {/each}
          </select>
        {/if}
      </div>

      <div class="col-md-6">
        <label for="lista-select" class="form-label section-label">
          <i class="bi bi-list-ul me-2"></i>
          <strong>Lista</strong>
        </label>
        {#if $listeAttive.length === 0}
          <select id="lista-select" class="form-select" disabled>
            <option>Nessuna lista attiva</option>
          </select>
        {:else}
          <select id="lista-select" class="form-select" value={$listaSelezionata?.id} onchange={handleListaChange}>
            {#each $listeAttive as lista}
              <option value={lista.id}>
                {lista.nome} ({lista.contattiCount || 0} contatti)
              </option>
            {/each}
          </select>
        {/if}
      </div>
    </div>

    <!-- Contatto corrente + pulsanti -->
    <div class="contatto-section">
      {#if $contattoCorrente}
        <div class="card contatto-card">
          <div class="card-body p-4">
            <!-- Info contatto -->
            <div class="contatto-info mb-4">
              <h3 class="contatto-nome mb-1">{displayName($contattoCorrente)}</h3>
              <p class="contatto-telefono mb-1">
                <i class="bi bi-telephone-fill text-success me-2"></i>
                <strong class="fs-5">{$contattoCorrente.telefono}</strong>
              </p>
              {#if $contattoCorrente.email}
                <p class="text-muted mb-0">
                  <i class="bi bi-envelope me-2"></i>
                  {$contattoCorrente.email}
                </p>
              {/if}
              {#if $contattoCorrente.note}
                <p class="text-muted mt-2 mb-0 fst-italic">
                  <i class="bi bi-chat-dots me-2"></i>
                  {$contattoCorrente.note}
                </p>
              {/if}
            </div>

            <!-- Pulsanti chiamata -->
            <div class="call-actions">
              {#if $autoMode}
                <button class="btn btn-danger btn-lg" onclick={fermaChiamate}>
                  <i class="bi bi-stop-fill me-2"></i>
                  Ferma chiamate
                </button>
                {#if $inChiamata}
                  <span class="spinner-border spinner-border-sm text-danger ms-2" role="status"></span>
                {/if}
              {:else}
                <div class="split-btn-group btn-group position-relative">
                  <button
                    class="btn btn-success btn-lg btn-manuale"
                    onclick={handleManuale}
                    disabled={$inChiamata}
                  >
                    {#if $inChiamata}
                      <span class="spinner-border spinner-border-sm me-2" role="status"></span>
                      Avviando...
                    {:else}
                      <i class="bi bi-telephone-outgoing-fill me-2"></i>
                      Avvia chiamata manuale
                    {/if}
                  </button>
                  <button
                    class="btn btn-success btn-lg btn-split-toggle"
                    onclick={() => { dropdownOpen = !dropdownOpen }}
                    disabled={$inChiamata}
                    aria-label="Mostra opzioni"
                  >
                    <i class="bi bi-chevron-down"></i>
                  </button>
                  {#if dropdownOpen}
                    <div class="dropdown-menu show dropdown-end">
                      <button class="dropdown-item" onclick={handleAutomatica}>
                        <i class="bi bi-lightning-fill text-warning me-2"></i>
                        Avvia chiamata automatica
                      </button>
                    </div>
                  {/if}
                </div>
              {/if}
            </div>

            <!-- Salta contatto -->
            <div class="mt-3">
              <button
                class="btn btn-outline-secondary btn-sm"
                onclick={prosegui}
                disabled={$inChiamata || $autoMode}
              >
                <i class="bi bi-skip-end-fill me-1"></i>
                Salta contatto
              </button>
            </div>
          </div>
        </div>

      {:else if totalContatti > 0}
        <!-- Tutti chiamati -->
        <div class="card">
          <div class="card-body text-center py-5">
            <i class="bi bi-check-circle text-success" style="font-size: 3rem;"></i>
            <h5 class="mt-3">Tutti i contatti chiamati!</h5>
            <p class="text-muted mb-0">Seleziona un'altra lista per continuare.</p>
          </div>
        </div>

      {:else}
        <!-- Nessun contatto -->
        <div class="card">
          <div class="card-body text-center py-5">
            <i class="bi bi-telephone-slash text-muted" style="font-size: 3rem; opacity: 0.4;"></i>
            <h5 class="mt-3 text-muted">Nessun contatto disponibile</h5>
            <p class="text-muted mb-0">La lista selezionata non contiene contatti.</p>
          </div>
        </div>
      {/if}

      <!-- Progressione -->
      {#if totalContatti > 0}
        <div class="d-flex align-items-center justify-content-between mt-3">
          <div class="text-muted small">
            Contatti rimanenti: <strong class="text-dark">{contattiRimanenti}</strong> di {totalContatti}
          </div>
          <div class="progress progress-contatti">
            <div
              class="progress-bar bg-success"
              role="progressbar"
              style="width: {totalContatti > 0 ? Math.round((1 - contattiRimanenti / totalContatti) * 100) : 0}%"
            ></div>
          </div>
        </div>
      {/if}
    </div>
  {/if}
</div>

<style>
  .chiamate-panel {
    max-width: 860px;
  }

  .section-label {
    color: #495057;
  }

  /* Contatto card */
  .contatto-card {
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
    border-color: #dee2e6;
  }

  .contatto-nome {
    font-weight: 600;
    color: #2c3e50;
  }

  /* Split button */
  .btn-manuale {
    border-radius: 0.375rem 0 0 0.375rem !important;
  }

  .btn-split-toggle {
    border-left: 1px solid rgba(255, 255, 255, 0.3) !important;
    border-radius: 0 0.375rem 0.375rem 0 !important;
    padding: 0.5rem 0.75rem;
  }

  /* Progress */
  .progress-contatti {
    width: 180px;
    height: 8px;
    border-radius: 4px;
    background-color: #e9ecef;
  }
</style>
