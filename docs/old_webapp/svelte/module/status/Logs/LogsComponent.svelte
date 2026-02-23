<script>
  import { onMount } from "svelte";

  let logsData = [];
  let loading = true;
  let error = null;
  let num = 20;
  let off = 0;

  const logLevels = {
    ERROR: { color: '#e74c3c', bg: '#fee', icon: '⚠️' },
    WARN: { color: '#f39c12', bg: '#fff3cd', icon: '⚡' },
    INFO: { color: '#3498db', bg: '#e3f2fd', icon: 'ℹ️' },
    DEBUG: { color: '#95a5a6', bg: '#ecf0f1', icon: '🔍' }
  };

  async function loadLogs() {
    loading = true;
    error = null;
    try {
      const response = await fetch(`/api/status/logs?num=${num}&off=${off}`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      logsData = await response.json();
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  function getLogLevel(message) {
    if (!message) return 'INFO';
    const upperMsg = message.toUpperCase();
    if (upperMsg.includes('ERROR') || upperMsg.includes('EXCEPTION')) return 'ERROR';
    if (upperMsg.includes('WARN')) return 'WARN';
    if (upperMsg.includes('DEBUG')) return 'DEBUG';
    return 'INFO';
  }

  onMount(() => {
    loadLogs();
  });
</script>

<div class="logs-dashboard">
  <!-- Header -->
  <div class="dashboard-header">
    <div class="header-info">
      <h2>Application Logs</h2>
      <p>Real-time application logs and events</p>
    </div>
    <div class="header-actions">
      <select bind:value={num} onchange={loadLogs}>
        <option value={10}>10 logs</option>
        <option value={20}>20 logs</option>
        <option value={50}>50 logs</option>
        <option value={100}>100 logs</option>
      </select>
      <button class="refresh-btn" onclick={loadLogs} disabled={loading}>
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class:spinning={loading}>
          <path d="M14 8c0-3.3-2.7-6-6-6-1.5 0-2.9.6-4 1.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M2 8c0 3.3 2.7 6 6 6 1.5 0 2.9-.6 4-1.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          <path d="M4 3L4 6 1 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        Refresh
      </button>
    </div>
  </div>

  {#if error}
    <div class="error-card">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
        <path d="M12 8v4M12 16h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <div>
        <strong>Connection Error</strong>
        <p>{error}</p>
      </div>
    </div>
  {:else if loading && logsData.length === 0}
    <div class="loading-card">
      <div class="spinner"></div>
      <p>Loading logs...</p>
    </div>
  {:else if logsData && logsData.length > 0}
    <div class="logs-container">
      <div class="logs-list">
        {#each logsData as log, index}
          {@const level = getLogLevel(log.message)}
          {@const levelStyle = logLevels[level]}
          <div class="log-entry" style="border-left-color: {levelStyle.color}">
            <div class="log-badge" style="background: {levelStyle.bg}; color: {levelStyle.color}">
              <span class="log-icon">{levelStyle.icon}</span>
              <span class="log-level">{level}</span>
            </div>
            <div class="log-content">
              <div class="log-id">#{log.id}</div>
              <div class="log-message">{log.message}</div>
              <div class="log-meta">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                  <circle cx="6" cy="6" r="5" stroke="currentColor" stroke-width="1"/>
                  <path d="M6 3v3l2 2" stroke="currentColor" stroke-width="1" stroke-linecap="round"/>
                </svg>
                {new Date(log.createdAt).toLocaleString("it-IT")}
              </div>
            </div>
          </div>
        {/each}
      </div>
    </div>
  {:else}
    <div class="empty-state">
      <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
        <rect x="12" y="16" width="40" height="32" rx="2" stroke="#bdc3c7" stroke-width="2"/>
        <path d="M20 24h24M20 32h24M20 40h16" stroke="#bdc3c7" stroke-width="2" stroke-linecap="round"/>
      </svg>
      <p>No logs available</p>
    </div>
  {/if}
</div>

<style>
  .logs-dashboard {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .header-info h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.5rem;
    color: #2c3e50;
  }

  .header-info p {
    margin: 0;
    color: #7f8c8d;
    font-size: 0.9rem;
  }

  .header-actions {
    display: flex;
    gap: 0.75rem;
    align-items: center;
  }

  .header-actions select {
    padding: 0.75rem 1rem;
    border: 1px solid #ddd;
    border-radius: 8px;
    background: white;
    color: #2c3e50;
    font-size: 0.9rem;
    cursor: pointer;
    transition: border-color 0.2s;
  }

  .header-actions select:hover {
    border-color: #3498db;
  }

  .refresh-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1.25rem;
    background: #3498db;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: 500;
    transition: all 0.2s;
  }

  .refresh-btn:hover:not(:disabled) {
    background: #2980b9;
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(52, 152, 219, 0.3);
  }

  .refresh-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .spinning {
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }

  .error-card {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1.5rem;
    background: #fee;
    border: 1px solid #fcc;
    border-radius: 12px;
    color: #c33;
  }

  .error-card svg {
    flex-shrink: 0;
  }

  .error-card strong {
    display: block;
    margin-bottom: 0.25rem;
    font-size: 1rem;
  }

  .error-card p {
    margin: 0;
    font-size: 0.9rem;
  }

  .loading-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem;
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .spinner {
    width: 40px;
    height: 40px;
    border: 4px solid #ecf0f1;
    border-top-color: #3498db;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  .loading-card p {
    margin: 1rem 0 0 0;
    color: #7f8c8d;
  }

  .logs-container {
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }

  .logs-list {
    display: flex;
    flex-direction: column;
  }

  .log-entry {
    display: flex;
    gap: 1rem;
    padding: 1.25rem;
    border-left: 4px solid #3498db;
    border-bottom: 1px solid #ecf0f1;
    transition: background 0.15s;
  }

  .log-entry:last-child {
    border-bottom: none;
  }

  .log-entry:hover {
    background: #f8f9fa;
  }

  .log-badge {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    border-radius: 6px;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    white-space: nowrap;
    height: fit-content;
  }

  .log-icon {
    font-size: 1rem;
  }

  .log-content {
    flex: 1;
    min-width: 0;
  }

  .log-id {
    font-size: 0.75rem;
    color: #95a5a6;
    margin-bottom: 0.5rem;
    font-family: monospace;
  }

  .log-message {
    color: #2c3e50;
    font-size: 0.9rem;
    line-height: 1.5;
    margin-bottom: 0.5rem;
    word-break: break-word;
  }

  .log-meta {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.8rem;
    color: #7f8c8d;
  }

  .log-meta svg {
    flex-shrink: 0;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem 2rem;
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .empty-state svg {
    margin-bottom: 1.5rem;
  }

  .empty-state p {
    margin: 0;
    color: #95a5a6;
    font-size: 1rem;
  }
</style>
