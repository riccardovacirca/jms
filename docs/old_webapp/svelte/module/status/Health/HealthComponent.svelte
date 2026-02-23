<script>
  import { onMount } from "svelte";

  let healthData = null;
  let loading = true;
  let error = null;

  async function loadHealth() {
    loading = true;
    error = null;
    try {
      const response = await fetch("/api/status/health");
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      healthData = await response.json();
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    loadHealth();
  });
</script>

<div class="health-dashboard">
  <!-- Header con pulsante refresh -->
  <div class="dashboard-header">
    <div class="header-info">
      <h2>Statistiche Applicazione</h2>
      <p>Riepilogo delle entità del sistema CRM</p>
    </div>
    <button class="refresh-btn" onclick={loadHealth} disabled={loading}>
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" class:spinning={loading}>
        <path d="M14 8c0-3.3-2.7-6-6-6-1.5 0-2.9.6-4 1.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M2 8c0 3.3 2.7 6 6 6 1.5 0 2.9-.6 4-1.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        <path d="M4 3L4 6 1 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      Refresh
    </button>
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
  {:else if loading}
    <div class="loading-card">
      <div class="spinner"></div>
      <p>Caricamento statistiche...</p>
    </div>
  {:else if healthData}
    <!-- Status principale -->
    <div class="status-card main-status" class:status-up={healthData.status === 'UP'}>
      <div class="status-icon">
        <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
          <circle cx="24" cy="24" r="20" stroke="currentColor" stroke-width="3"/>
          <path d="M16 24l6 6 10-12" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="status-info">
        <div class="status-label">Stato Applicazione</div>
        <div class="status-value">{healthData.status}</div>
        <div class="status-time">Ultimo aggiornamento: {new Date().toLocaleTimeString('it-IT')}</div>
      </div>
    </div>

    <!-- Griglia metriche -->
    <div class="metrics-grid">
      <!-- Campagne -->
      <div class="metric-card">
        <div class="metric-icon campagne">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">Campagne</div>
          <div class="metric-value">{healthData.campagne || 0}</div>
        </div>
      </div>

      <!-- Liste -->
      <div class="metric-card">
        <div class="metric-icon liste">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">Liste</div>
          <div class="metric-value">{healthData.liste || 0}</div>
        </div>
      </div>

      <!-- Contatti -->
      <div class="metric-card">
        <div class="metric-icon contatti">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">Contatti</div>
          <div class="metric-value">{healthData.contatti || 0}</div>
        </div>
      </div>

      <!-- Operatori -->
      <div class="metric-card">
        <div class="metric-icon operatori">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M12.5 7a4 4 0 11-8 0 4 4 0 018 0zM20 8v6M23 11h-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">Operatori</div>
          <div class="metric-value">{healthData.operatori || 0}</div>
        </div>
      </div>

      <!-- Agenti -->
      <div class="metric-card">
        <div class="metric-icon agenti">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2M12 11a4 4 0 100-8 4 4 0 000 8z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">Agenti</div>
          <div class="metric-value">{healthData.agenti || 0}</div>
        </div>
      </div>

      <!-- Sedi -->
      <div class="metric-card">
        <div class="metric-icon sedi">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 22V12h6v10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">Sedi</div>
          <div class="metric-value">{healthData.sedi || 0}</div>
        </div>
      </div>

      <!-- Chiamate -->
      <div class="metric-card">
        <div class="metric-icon chiamate">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <div class="metric-content">
          <div class="metric-label">Chiamate</div>
          <div class="metric-value">{healthData.chiamate || 0}</div>
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  .health-dashboard {
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

  .main-status {
    display: flex;
    align-items: center;
    gap: 2rem;
    padding: 2rem;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    color: white;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }

  .main-status.status-up {
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    box-shadow: 0 4px 12px rgba(56, 239, 125, 0.3);
  }

  .status-icon {
    flex-shrink: 0;
  }

  .status-info {
    flex: 1;
  }

  .status-label {
    font-size: 0.9rem;
    opacity: 0.9;
    margin-bottom: 0.5rem;
  }

  .status-value {
    font-size: 2rem;
    font-weight: 700;
    margin-bottom: 0.25rem;
  }

  .status-time {
    font-size: 0.85rem;
    opacity: 0.8;
  }

  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1.5rem;
  }

  .metric-card {
    display: flex;
    gap: 1rem;
    padding: 1.5rem;
    background: white;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s, box-shadow 0.2s;
  }

  .metric-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .metric-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 12px;
    flex-shrink: 0;
  }

  .metric-icon.campagne {
    background: #e3f2fd;
    color: #2196f3;
  }

  .metric-icon.liste {
    background: #f3e5f5;
    color: #9c27b0;
  }

  .metric-icon.contatti {
    background: #fff3e0;
    color: #ff9800;
  }

  .metric-icon.operatori {
    background: #e8f5e9;
    color: #4caf50;
  }

  .metric-icon.agenti {
    background: #fce4ec;
    color: #e91e63;
  }

  .metric-icon.sedi {
    background: #e0f2f1;
    color: #009688;
  }

  .metric-icon.chiamate {
    background: #fff9c4;
    color: #f57c00;
  }

  .metric-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .metric-label {
    font-size: 0.85rem;
    color: #7f8c8d;
    margin-bottom: 0.5rem;
  }

  .metric-value {
    font-size: 1.5rem;
    font-weight: 600;
    color: #2c3e50;
  }
</style>
