<script>
    import { user, logout, loading } from './store.js';

    async function handleLogout() {
        await logout();
    }
</script>

<div class="session-container">
    <div class="user-info">
        <h2>Benvenuto, {$user?.username || 'Utente'}</h2>
        <p>Ruolo: <strong>{$user?.ruolo}</strong></p>
        <p>ID Utente: {$user?.id}</p>

        <button onclick={handleLogout} disabled={$loading}>
            {$loading ? 'Uscita...' : 'Logout'}
        </button>
    </div>
</div>

<style>
    .session-container { padding: 1rem; }
    .user-info {
        background: #f5f5f5;
        padding: 1.5rem;
        margin-bottom: 2rem;
        border-radius: 8px;
    }
    .user-info h2 { margin: 0 0 0.5rem 0; }
    .user-info p { margin: 0.25rem 0; }
    .user-info button {
        margin-top: 1rem;
        padding: 0.5rem 1rem;
        background: #dc3545;
        color: white;
        border: none;
        cursor: pointer;
        border-radius: 4px;
    }
    .user-info button:hover {
        background: #c82333;
    }
    .user-info button:disabled {
        background: #ccc;
        cursor: not-allowed;
    }
</style>
