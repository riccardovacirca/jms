<script>
  import { auth, logout, currentModule, currentModuleName, currentSubPath } from '../../store.js'
  import { contextHeader, contextHeaderProps, contextTitle } from './store.js'

  let contextDropdownOpen = $state(false)

  function handleLogout() {
    logout()
  }

  function toggleContextDropdown() {
    contextDropdownOpen = !contextDropdownOpen
  }

  function closeContextDropdown() {
    contextDropdownOpen = false
  }

  function handleSubNavigate(subPath) {
    currentModule.navigate($currentModuleName, subPath)
    closeContextDropdown()
  }

  // Chiudi dropdown quando si clicca fuori
  function handleClickOutside(event) {
    if (contextDropdownOpen && !event.target.closest('.context-dropdown')) {
      closeContextDropdown()
    }
  }
</script>

<svelte:window onclick={handleClickOutside} />

<div class="container-fluid p-0">
  <div class="d-flex align-items-stretch h-100">
    <!-- Hamburger (solo mobile) -->
    <div class="d-flex align-items-center px-2">
      <button
        class="btn btn-link d-lg-none p-2"
        type="button"
        data-bs-toggle="offcanvas"
        data-bs-target="#sidebarOffcanvas"
        aria-label="Apri menu"
      >
        <svg width="24" height="24" fill="currentColor">
          <path d="M3 6h18M3 12h18M3 18h18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
    </div>

    <!-- Dropdown Contestuale del Modulo -->
    <div class="context-dropdown position-relative">
      {#if $contextHeader}
        {@const HeaderComponent = $contextHeader}
        <button
          class="btn btn-light d-flex align-items-center gap-2"
          onclick={toggleContextDropdown}
        >
          <!-- Render del trigger (isDropdown=false) -->
          <HeaderComponent
            state={$contextHeaderProps.state}
            onViewChange={$contextHeaderProps.onViewChange}
            isDropdown={false}
            onClose={closeContextDropdown}
          />
          <svg width="12" height="12" fill="currentColor" class:rotate={contextDropdownOpen}>
            <path d="M2 4L6 8L10 4" stroke="currentColor" stroke-width="2"/>
          </svg>
        </button>
        {#if contextDropdownOpen}
          <div class="dropdown-menu show position-absolute mt-2">
            <!-- Render del dropdown (isDropdown=true) -->
            <HeaderComponent
              state={$contextHeaderProps.state}
              onViewChange={$contextHeaderProps.onViewChange}
              isDropdown={true}
              onClose={closeContextDropdown}
            />
          </div>
        {/if}
      {/if}
    </div>

    <!-- Spacer -->
    <div class="flex-grow-1"></div>

    <!-- User Dropdown -->
    {#if $auth.user}
      <div class="dropdown d-flex align-items-center px-3">
        <button
          class="btn btn-link text-decoration-none d-flex align-items-center gap-2"
          type="button"
          data-bs-toggle="dropdown"
          aria-label="Menu utente"
        >
          <div class="rounded-circle bg-primary bg-opacity-10 p-2">
            <svg width="20" height="20" fill="currentColor">
              <circle cx="10" cy="7" r="3"/>
              <path d="M2 18c0-4 3.5-6 8-6s8 2 8 6"/>
            </svg>
          </div>
          <div class="d-none d-md-block text-start">
            <div class="small fw-semibold text-dark">{$auth.user.username}</div>
            <div class="small text-muted">{$auth.user.ruolo}</div>
          </div>
        </button>

        <ul class="dropdown-menu dropdown-menu-end">
          <li><h6 class="dropdown-header">Account</h6></li>
          <li><a class="dropdown-item" href="#profilo">Profilo</a></li>
          <li><a class="dropdown-item" href="#impostazioni">Impostazioni</a></li>
          <li><hr class="dropdown-divider"></li>
          <li>
            <button class="dropdown-item text-danger" onclick={handleLogout}>
              Logout
            </button>
          </li>
        </ul>
      </div>
    {/if}

    <!-- Separatore -->
    <div class="header-separator"></div>

    <!-- Pulsante Chiama (Verde, piena altezza) - Estrema destra -->
    <button
      class="btn-chiama d-flex align-items-center justify-content-center px-4 border-0"
      onclick={() => currentModule.navigate('chiamate')}
      class:active={$currentModuleName === 'chiamate'}
    >
      <svg width="20" height="20" fill="currentColor" class="me-2">
        <path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/>
      </svg>
      <span class="fw-semibold">Chiama</span>
    </button>
  </div>
</div>

<style>
  .rotate {
    transform: rotate(180deg);
    transition: transform 0.2s;
  }

  .context-dropdown .dropdown-menu {
    min-width: 200px;
  }

  /* Pulsante Chiama - Verde, piena altezza header */
  :global(.btn-chiama) {
    background: #28a745;
    color: white;
    cursor: pointer;
    transition: background 0.2s;
    height: 100%;
    white-space: nowrap;
  }

  :global(.btn-chiama:hover) {
    background: #218838;
  }

  :global(.btn-chiama.active) {
    background: #1e7e34;
    box-shadow: inset 0 3px 5px rgba(0,0,0,0.125);
  }

  :global(.header-separator) {
    width: 1px;
    height: 100%;
    background: rgba(0,0,0,0.1);
  }
</style>
