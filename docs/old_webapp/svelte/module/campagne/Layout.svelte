<script>
  import { currentView } from './store.js'
  import CampagneLayout from './Campagne/Layout.svelte'
  import ListeLayout from './Liste/Layout.svelte'
</script>

<div class="campagne-module">
  {#if $currentView === 'campagne'}
    <CampagneLayout />
  {:else if $currentView === 'liste'}
    <ListeLayout />
  {/if}
</div>

<style>
  .campagne-module {
    width: 100%;
    height: 100%;
    background: #f5f7fa;
  }
</style>
