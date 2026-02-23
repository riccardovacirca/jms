<script>
  export let state = { currentView: 'config' };
  export let onViewChange = () => {};
</script>

<div class="voice-sidebar">
  <div class="sidebar-title">Voice</div>
  <nav class="sidebar-nav">
    <button
      class:active={state.currentView === 'config'}
      onclick={() => onViewChange('config')}
    >
      Configurazione
    </button>
    <button
      class:active={state.currentView === 'test'}
      onclick={() => onViewChange('test')}
    >
      Test WebRTC
    </button>
  </nav>
</div>

<style>
  .voice-sidebar {
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
