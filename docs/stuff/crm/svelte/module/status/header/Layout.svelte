<script>
  export let state = { currentView: 'health' };
  export let onViewChange = () => {};
  export let isDropdown = false;
  export let onClose = () => {};

  function handleItemClick(view) {
    onViewChange(view);
    onClose();
  }
</script>

{#if isDropdown}
  <!-- Menu dropdown con voci -->
  <div class="status-header-dropdown">
    <button
      class="dropdown-item"
      class:active={state.currentView === 'health'}
      onclick={() => handleItemClick('health')}
    >
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      Health Check
    </button>
    <button
      class="dropdown-item"
      class:active={state.currentView === 'logs'}
      onclick={() => handleItemClick('logs')}
    >
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M2 2h12M2 6h12M2 10h12M2 14h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      System Logs
    </button>
  </div>
{:else}
  <!-- Solo il titolo nel trigger -->
  <span class="status-header-title">Status</span>
{/if}

<style>
  .status-header-title {
    font-weight: 500;
    color: #2c3e50;
  }

  .status-header-dropdown {
    display: flex;
    flex-direction: column;
    padding: 0.25rem 0;
  }

  .dropdown-item {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.75rem 1rem;
    background: none;
    border: none;
    text-align: left;
    cursor: pointer;
    font-size: 0.9rem;
    color: #495057;
    transition: background 0.15s;
  }

  .dropdown-item:hover {
    background: #f8f9fa;
  }

  .dropdown-item.active {
    background: #e3f2fd;
    color: #007bff;
  }

  .dropdown-item svg {
    flex-shrink: 0;
    opacity: 0.7;
  }

  .dropdown-item.active svg {
    opacity: 1;
  }
</style>
