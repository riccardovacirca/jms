<script>
    import LoginComponent from './LoginComponent.svelte';
    import SessionComponent from './SessionComponent.svelte';
    import { auth } from '../../store.js';
</script>

<nav class="module-nav">
    <button class="active">Autenticazione</button>
</nav>

<main>
    {#if $auth.isAuthenticated && $auth.user}
        <SessionComponent />
    {:else}
        <LoginComponent />
    {/if}
</main>

<style>
    .module-nav {
        padding: 1rem;
        border-bottom: 1px solid #ddd;
    }
    .module-nav button {
        padding: 0.5rem 1rem;
        margin-right: 0.5rem;
        border: 1px solid #ddd;
        background: #007bff;
        color: white;
        border-color: #007bff;
        cursor: pointer;
    }
    main { padding: 1rem; }
</style>
