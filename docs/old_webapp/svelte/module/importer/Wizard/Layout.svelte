<script>
  import { onMount } from 'svelte'
  import Step1UploadComponent from './Step1UploadComponent.svelte'
  import Step2MappingComponent from './Step2MappingComponent.svelte'
  import Step3ValidationComponent from './Step3ValidationComponent.svelte'
  import Step4ExecuteComponent from './Step4ExecuteComponent.svelte'
  import { currentStep, resetWizard, error } from '../store.js'

  function handleStepChange(step) {
    if (step >= 1 && step <= 4) {
      currentStep.set(step)
    }
  }

  function handleReset() {
    resetWizard()
  }

  onMount(() => {
    // Reset wizard on mount
    resetWizard()
  })
</script>

<div class="wizard-container">
  <div class="wizard-header">
    <h1>Importa Contatti</h1>
    <p>Carica un file Excel/CSV e importa i contatti in una lista</p>
  </div>

  {#if $error}
    <div class="alert alert-danger alert-dismissible fade show" role="alert">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      {$error}
      <button type="button" class="btn-close" onclick={() => error.set(null)} aria-label="Chiudi"></button>
    </div>
  {/if}

  <!-- Step Indicator -->
  <div class="step-indicator">
    <div class="step {$currentStep === 1 ? 'active' : $currentStep > 1 ? 'completed' : ''}">
      <div class="step-circle">
        {#if $currentStep > 1}
          <i class="bi bi-check-lg"></i>
        {:else}
          1
        {/if}
      </div>
      <div class="step-label">Upload File</div>
    </div>
    <div class="step-line {$currentStep > 1 ? 'completed' : ''}"></div>
    <div class="step {$currentStep === 2 ? 'active' : $currentStep > 2 ? 'completed' : ''}">
      <div class="step-circle">
        {#if $currentStep > 2}
          <i class="bi bi-check-lg"></i>
        {:else}
          2
        {/if}
      </div>
      <div class="step-label">Mapping</div>
    </div>
    <div class="step-line {$currentStep > 2 ? 'completed' : ''}"></div>
    <div class="step {$currentStep === 3 ? 'active' : $currentStep > 3 ? 'completed' : ''}">
      <div class="step-circle">
        {#if $currentStep > 3}
          <i class="bi bi-check-lg"></i>
        {:else}
          3
        {/if}
      </div>
      <div class="step-label">Validazione</div>
    </div>
    <div class="step-line {$currentStep > 3 ? 'completed' : ''}"></div>
    <div class="step {$currentStep === 4 ? 'active' : $currentStep > 4 ? 'completed' : ''}">
      <div class="step-circle">4</div>
      <div class="step-label">Esecuzione</div>
    </div>
  </div>

  <!-- Step Content -->
  <div class="wizard-content">
    {#if $currentStep === 1}
      <Step1UploadComponent onNext={() => handleStepChange(2)} />
    {:else if $currentStep === 2}
      <Step2MappingComponent
        onNext={() => handleStepChange(3)}
        onBack={() => handleStepChange(1)}
      />
    {:else if $currentStep === 3}
      <Step3ValidationComponent
        onNext={() => handleStepChange(4)}
        onBack={() => handleStepChange(2)}
      />
    {:else if $currentStep === 4}
      <Step4ExecuteComponent
        onBack={() => handleStepChange(3)}
        onReset={handleReset}
      />
    {/if}
  </div>
</div>

<style>
  .wizard-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
  }

  .wizard-header {
    margin-bottom: 3rem;
    text-align: center;
  }

  .wizard-header h1 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    color: #2c3e50;
    font-weight: 600;
  }

  .wizard-header p {
    margin: 0;
    color: #7f8c8d;
    font-size: 1rem;
  }

  .step-indicator {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 3rem;
    padding: 0 2rem;
  }

  .step {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
  }

  .step-circle {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: #e9ecef;
    border: 3px solid #e9ecef;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    color: #6c757d;
    transition: all 0.3s;
  }

  .step.active .step-circle {
    background: #667eea;
    border-color: #667eea;
    color: white;
    box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.2);
  }

  .step.completed .step-circle {
    background: #28a745;
    border-color: #28a745;
    color: white;
  }

  .step-label {
    font-size: 0.875rem;
    font-weight: 500;
    color: #6c757d;
    white-space: nowrap;
  }

  .step.active .step-label {
    color: #667eea;
    font-weight: 600;
  }

  .step.completed .step-label {
    color: #28a745;
  }

  .step-line {
    width: 80px;
    height: 3px;
    background: #e9ecef;
    transition: all 0.3s;
  }

  .step-line.completed {
    background: #28a745;
  }

  .wizard-content {
    background: white;
    border-radius: 12px;
    padding: 2rem;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  @media (max-width: 768px) {
    .step-indicator {
      padding: 0;
    }

    .step-line {
      width: 40px;
    }

    .step-label {
      font-size: 0.75rem;
    }

    .step-circle {
      width: 40px;
      height: 40px;
    }
  }
</style>
