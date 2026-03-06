package it.olomedia.scai.modules.policy.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità PolicyDefaultSede
 * Rappresenta una policy di default associata a una sede
 */
public class PolicyDefaultSedeDTO {

    private Long id;
    private String codicePolicy;
    private String codiceRepertorio;
    private String codEnte;
    private String codSede;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Short isNotDeleted;

    // Campi relazionali (per visualizzazione)
    private String descrizioneEnte;
    private String descrizioneSede;
    private String descrizioneRepertorio;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodicePolicy() {
        return codicePolicy;
    }

    public void setCodicePolicy(String codicePolicy) {
        this.codicePolicy = codicePolicy;
    }

    public String getCodiceRepertorio() {
        return codiceRepertorio;
    }

    public void setCodiceRepertorio(String codiceRepertorio) {
        this.codiceRepertorio = codiceRepertorio;
    }

    public String getCodEnte() {
        return codEnte;
    }

    public void setCodEnte(String codEnte) {
        this.codEnte = codEnte;
    }

    public String getCodSede() {
        return codSede;
    }

    public void setCodSede(String codSede) {
        this.codSede = codSede;
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

    public String getDescrizioneEnte() {
        return descrizioneEnte;
    }

    public void setDescrizioneEnte(String descrizioneEnte) {
        this.descrizioneEnte = descrizioneEnte;
    }

    public String getDescrizioneSede() {
        return descrizioneSede;
    }

    public void setDescrizioneSede(String descrizioneSede) {
        this.descrizioneSede = descrizioneSede;
    }

    public String getDescrizioneRepertorio() {
        return descrizioneRepertorio;
    }

    public void setDescrizioneRepertorio(String descrizioneRepertorio) {
        this.descrizioneRepertorio = descrizioneRepertorio;
    }
}
