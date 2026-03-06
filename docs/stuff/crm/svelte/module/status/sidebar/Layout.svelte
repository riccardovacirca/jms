<script>
  export let state = { currentView: 'health' };
  export let onViewChange = () => {};
</script>

<div class="status-sidebar">
  <div class="sidebar-title">Status</div>
  <nav class="sidebar-nav">
    <button
      class:active={state.currentView === 'health'}
      onclick={() => onViewChange('health')}
    >
      Health
    </button>
    <button
      class:active={state.currentView === 'logs'}
      onclick={() => onViewChange('logs')}
    >
      Logs
    </button>
  </nav>
</div>

<style>
  .status-sidebar {
    padding: 0;
  }

  .sidebar-title {
    padding: 0.5rem 1rem;
    font-size: 0.75rem;
    text-transform: uppercase;
    color: rgba(255, 255, 255, 0.5);
    letter-spacing: 0.05em;
  }

  .sidebar-nav {
    display: flex;
    flex-direction: column;
  }

  .sidebar-nav button {
    display: block;
    width: 100%;
    padding: 0.5rem 1rem 0.5rem 1.5rem;
    background: none;
    border: none;
    color: rgba(255, 255, 255, 0.6);
    text-align: left;
    cursor: pointer;
    font-size: 0.9rem;
  }

  .sidebar-nav button:hover {
    background: rgba(255, 255, 255, 0.05);
    color: white;
  }

  .sidebar-nav button.active {
    color: #3498db;
  }
</style>
