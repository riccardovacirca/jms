package it.olomedia.scai.modules.policy.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità PolicyRapportoValidity
 * Rappresenta la validità temporale di una policy associata a un rapporto
 */
public class PolicyRapportoValidityDTO {

    private Long id;
    private String codEnte;
    private String matricola;
    private String slugSdc;
    private LocalDateTime dataInizio;
    private LocalDateTime dataFine;
    private LocalDateTime dataCessazione;
    private Short attivo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Short isNotDeleted;

    // Campi relazionali (per visualizzazione)
    private String nomeRapporto;
    private String cognomeRapporto;
    private String descrizioneSdc;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodEnte() {
        return codEnte;
    }

    public void setCodEnte(String codEnte) {
        this.codEnte = codEnte;
    }

    public String getMatricola() {
        return matricola;
    }

    public void setMatricola(String matricola) {
        this.matricola = matricola;
    }

    public String getSlugSdc() {
        return slugSdc;
    }

    public void setSlugSdc(String slugSdc) {
        this.slugSdc = slugSdc;
    }

    public LocalDateTime getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDateTime dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDateTime getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDateTime dataFine) {
        this.dataFine = dataFine;
    }

    public LocalDateTime getDataCessazione() {
        return dataCessazione;
    }

    public void setDataCessazione(LocalDateTime dataCessazione) {
        this.dataCessazione = dataCessazione;
    }

    public Short getAttivo() {
        return attivo;
    }

    public void setAttivo(Short attivo) {
        this.attivo = attivo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Short getIsNotDeleted() {
        return isNotDeleted;
    }

    public void setIsNotDeleted(Short isNotDeleted) {
        this.isNotDeleted = isNotDeleted;
    }

    public String getNomeRapporto() {
        return nomeRapporto;
    }

    public void setNomeRapporto(String nomeRapporto) {
        this.nomeRapporto = nomeRapporto;
    }

    public String getCognomeRapporto() {
        return cognomeRapporto;
    }

    public void setCognomeRapporto(String cognomeRapporto) {
        this.cognomeRapporto = cognomeRapporto;
    }

    public String getDescrizioneSdc() {
        return descrizioneSdc;
    }

    public void setDescrizioneSdc(String descrizioneSdc) {
        this.descrizioneSdc = descrizioneSdc;
    }
}
