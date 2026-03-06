<script>
  import { currentModule, currentModuleName, currentSubPath } from "../../store.js"
  import { contextSidebar, contextSidebarProps } from "./store.js"

  const menuItems = [
    { id: 'status', label: 'Status', icon: '📊' },
    { id: 'contatti', label: 'Contatti', icon: '👥' },
    { id: 'liste', label: 'Liste', icon: '📋' },
    { id: 'campagne', label: 'Campagne', icon: '📣' },
    { id: 'operatori', label: 'Operatori', icon: '👨‍💼' },
    { id: 'importer', label: 'Importer', icon: '📥' },
    { id: 'voice', label: 'Voice', icon: '📞' },
    { id: 'voice2', label: 'Voice 2.0', icon: '🎵' }
  ]

  function handleNavigate(moduleId, subPath = '') {
    currentModule.navigate(moduleId, subPath)
    // Chiudi offcanvas su mobile dopo il click
    const offcanvas = document.getElementById('sidebarOffcanvas')
    if (offcanvas) {
      const bsOffcanvas = bootstrap.Offcanvas.getInstance(offcanvas)
      if (bsOffcanvas) bsOffcanvas.hide()
    }
  }

  function handleSubNavigate(subPath) {
    currentModule.navigate($currentModuleName, subPath)
    const offcanvas = document.getElementById('sidebarOffcanvas')
    if (offcanvas) {
      const bsOffcanvas = bootstrap.Offcanvas.getInstance(offcanvas)
      if (bsOffcanvas) bsOffcanvas.hide()
    }
  }
</script>

<div class="d-flex flex-column h-100">
  <!-- Brand -->
  <div class="p-3 border-bottom border-secondary">
    <h5 class="mb-0">CRM</h5>
  </div>

  <!-- Navigation Statica: Menu Principale -->
  <nav class="sidebar-static py-3 border-bottom border-secondary">
    <div class="px-3 mb-2">
      <small class="text-muted text-uppercase fw-bold">Menu Principale</small>
    </div>
    {#each menuItems as item}
      <button
        class="nav-item w-100 text-start px-3 py-2 border-0 d-flex align-items-center gap-2"
        class:active={$currentModuleName === item.id}
        onclick={() => handleNavigate(item.id)}
      >
        <span class="fs-5">{item.icon}</span>
        <span>{item.label}</span>
      </button>
    {/each}
  </nav>

  <!-- Area Dinamica: Menu Contestuale del Modulo -->
  <div class="sidebar-dynamic flex-grow-1 overflow-auto">
    {#key $contextSidebar}
      {#if $contextSidebar}
        {@const SidebarComponent = $contextSidebar}
        <SidebarComponent
          state={$contextSidebarProps.state}
          onViewChange={$contextSidebarProps.onViewChange}
        />
      {/if}
    {/key}
  </div>
</div>

<style>
  .sidebar-static {
  }

  .nav-item {
    background: transparent;
    color: rgba(255, 255, 255, 0.7);
    border-left: 3px solid transparent;
    transition: all 0.2s;
  }

  .nav-item:hover {
    background: rgba(255, 255, 255, 0.1);
    color: white;
  }

  .nav-item.active {
    background: rgba(255, 255, 255, 0.15);
    color: white;
    border-left-color: #0d6efd;
  }

  .sidebar-dynamic {
    color: rgba(255, 255, 255, 0.8);
  }

  .sidebar-dynamic :global(.btn-outline-light) {
    border-color: rgba(255, 255, 255, 0.2);
    color: rgba(255, 255, 255, 0.7);
  }

  .sidebar-dynamic :global(.btn-outline-light:hover) {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.3);
    color: white;
  }
</style>
