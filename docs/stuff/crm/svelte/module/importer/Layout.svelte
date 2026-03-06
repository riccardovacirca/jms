<script>
  import { currentView } from './store.js'
  import WizardLayout from './Wizard/Layout.svelte'
</script>

<div class="importer-module">
  {#if $currentView === 'wizard'}
    <WizardLayout />
  {/if}
</div>

<style>
  .importer-module {
    width: 100%;
    height: 100%;
    background: #f5f7fa;
  }
</style>
