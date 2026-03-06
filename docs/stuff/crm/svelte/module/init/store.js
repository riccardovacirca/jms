import { writable } from 'svelte/store'

export const currentStep = writable(1)
export const loading = writable(false)
export const error = writable(null)

// Dati wizard
export const aziendaData = writable({
  ragioneSociale: '',
  formaGiuridica: '',
  partitaIva: '',
  codiceFiscale: '',
  codiceSdi: '',
  pec: '',
  numeroRea: '',
  capitaleSociale: '',
  sedeLegaleIndirizzo: '',
  sedeLegaleCap: '',
  sedeLegaleCitta: '',
  sedeLegaleProvincia: '',
  sedeLegaleNazione: 'Italia',
  telefonoGenerale: '',
  emailGenerale: '',
  sitoWeb: '',
  referenteCommerciale: '',
  referenteTecnico: '',
  intestatarioFatturazione: '',
  indirizzoFatturazione: '',
  iban: '',
  modalitaPagamento: '',
  regimeIva: ''
})

export const sediData = writable([])

export const ownerData = writable({
  username: '',
  password: '',
  nome: '',
  cognome: '',
  email: '',
  telefono: '',
  twoFactorEnabled: 0
})

export const adminAccountsData = writable([])

export const configurazioniData = writable([])

// Navigazione wizard
export function goToStep(step) {
  currentStep.set(step)
  error.set(null)
}

export function nextStep() {
  currentStep.update(s => s + 1)
  error.set(null)
}

export function prevStep() {
  currentStep.update(s => Math.max(1, s - 1))
  error.set(null)
}

export function reset() {
  currentStep.set(1)
  loading.set(false)
  error.set(null)
  aziendaData.set({
    ragioneSociale: '',
    formaGiuridica: '',
    partitaIva: '',
    codiceFiscale: '',
    codiceSdi: '',
    pec: '',
    numeroRea: '',
    capitaleSociale: '',
    sedeLegaleIndirizzo: '',
    sedeLegaleCap: '',
    sedeLegaleCitta: '',
    sedeLegaleProvincia: '',
    sedeLegaleNazione: 'Italia',
    telefonoGenerale: '',
    emailGenerale: '',
    sitoWeb: '',
    referenteCommerciale: '',
    referenteTecnico: '',
    intestatarioFatturazione: '',
    indirizzoFatturazione: '',
    iban: '',
    modalitaPagamento: '',
    regimeIva: ''
  })
  sediData.set([])
  ownerData.set({
    username: '',
    password: '',
    nome: '',
    cognome: '',
    email: '',
    telefono: '',
    twoFactorEnabled: 0
  })
  adminAccountsData.set([])
  configurazioniData.set([])
}
