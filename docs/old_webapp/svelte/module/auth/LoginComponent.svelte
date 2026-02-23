<script>
    import { login, loading, error } from './store.js';
    import { checkAuth, currentModule } from '../../store.js';

    let username = $state('');
    let password = $state('');

    async function handleSubmit() {
        if (!username.trim() || !password) {
            alert('Inserire username e password');
            return;
        }

        try {
            await login(username.trim(), password);
            const isAuth = await checkAuth();
            if (isAuth) {
                currentModule.navigate('status');
            }
        } catch (err) {
            // Errore già gestito nello store
        }
    }

    function handleKeydown(event) {
        if (event.key === 'Enter') {
            handleSubmit();
        }
    }
</script>

<div class="login-container">
    <div class="login-box">
        <h2>Login</h2>

        <div class="form-group">
            <label for="username">Username</label>
            <input
                id="username"
                type="text"
                bind:value={username}
                onkeydown={handleKeydown}
                disabled={$loading}
                placeholder="Inserisci username"
            />
        </div>

        <div class="form-group">
            <label for="password">Password</label>
            <input
                id="password"
                type="password"
                bind:value={password}
                onkeydown={handleKeydown}
                disabled={$loading}
                placeholder="Inserisci password"
            />
        </div>

        {#if $error}
            <div class="error">{$error}</div>
        {/if}

        <button onclick={handleSubmit} disabled={$loading || !username.trim() || !password}>
            {$loading ? 'Accesso in corso...' : 'Accedi'}
        </button>

        <div class="hint">
            <p>Utenti demo:</p>
            <ul>
                <li><strong>admin</strong> / admin (Amministratore)</li>
                <li><strong>operatore</strong> / operatore (Operatore)</li>
            </ul>
        </div>
    </div>
</div>

<style>
    .login-container {
        display: flex;
        justify-content: center;
        align-items: center;
        min-height: 400px;
    }
    .login-box {
        width: 100%;
        max-width: 400px;
        padding: 2rem;
        border: 1px solid #ddd;
        background: white;
    }
    .login-box h2 {
        margin: 0 0 1.5rem 0;
        text-align: center;
    }
    .form-group {
        margin-bottom: 1rem;
    }
    .form-group label {
        display: block;
        margin-bottom: 0.25rem;
        font-weight: bold;
    }
    .form-group input {
        width: 100%;
        padding: 0.75rem;
        border: 1px solid #ccc;
        box-sizing: border-box;
        font-size: 1rem;
    }
    .error {
        background: #f8d7da;
        color: #721c24;
        padding: 0.5rem;
        margin-bottom: 1rem;
        text-align: center;
    }
    button {
        width: 100%;
        padding: 0.75rem;
        background: #007bff;
        color: white;
        border: none;
        font-size: 1rem;
        cursor: pointer;
    }
    button:disabled {
        background: #ccc;
        cursor: not-allowed;
    }
    .hint {
        margin-top: 1.5rem;
        padding-top: 1rem;
        border-top: 1px solid #eee;
        font-size: 0.85rem;
        color: #666;
    }
    .hint p { margin: 0 0 0.5rem 0; }
    .hint ul { margin: 0; padding-left: 1.25rem; }
    .hint li { margin: 0.25rem 0; }
</style>
