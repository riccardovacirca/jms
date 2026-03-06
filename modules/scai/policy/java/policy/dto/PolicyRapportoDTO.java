package it.olomedia.scai.modules.policy.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità PolicyRapporto
 * Rappresenta una policy di accesso associata a un rapporto dipendente
 */
public class PolicyRapportoDTO {

    private Long id;
    private String codEnte;
    private String matricola;
    private String slugSdc;
    private String codiceRepertorio;
    private String codicePolicy;
    private LocalDateTime dataInizioValidita;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private Short isNotDeleted;

    // Campi relazionali (per visualizzazione)
    private String nomeRapporto;
    private String cognomeRapporto;
    private String descrizioneSdc;
    private String descrizioneRepertorio;

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

    public String getCodiceRepertorio() {
        return codiceRepertorio;
    }

    public void setCodiceRepertorio(String codiceRepertorio) {
        this.codiceRepertorio = codiceRepertorio;
    }

    public String getCodicePolicy() {
        return codicePolicy;
    }

    public void setCodicePolicy(String codicePolicy) {
        this.codicePolicy = codicePolicy;
    }

    public LocalDateTime getDataInizioValidita() {
        return dataInizioValidita;
    }

    public void setDataInizioValidita(LocalDateTime dataInizioValidita) {
        this.dataInizioValidita = dataInizioValidita;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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

    public String getDescrizioneRepertorio() {
        return descrizioneRepertorio;
    }

    public void setDescrizioneRepertorio(String descrizioneRepertorio) {
        this.descrizioneRepertorio = descrizioneRepertorio;
    }
}
