<script>
  import { currentView } from './store.js'
  import ListeLayout from './Liste/Layout.svelte'
  import ContattiLayout from './Contatti/Layout.svelte'
</script>

<div class="liste-module">
  {#if $currentView === 'liste'}
    <ListeLayout />
  {:else if $currentView === 'contatti'}
    <ContattiLayout />
  {/if}
</div>

<style>
  .liste-module {
    width: 100%;
    height: 100%;
    background: #f5f7fa;
  }
</style>
