package it.olomedia.scai.modules.veicoli.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità VeicoloAlimentazione
 * Rappresenta un tipo di alimentazione (Benzina, Diesel, Elettrico, etc.)
 */
public class VeicoloAlimentazioneDTO {

    private Long id;
    private String codiceAlimentazione;
    private String descrizione;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Short isNotDeleted;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodiceAlimentazione() {
        return codiceAlimentazione;
    }

    public void setCodiceAlimentazione(String codiceAlimentazione) {
        this.codiceAlimentazione = codiceAlimentazione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Short getIsNotDeleted() {
        return isNotDeleted;
    }

    public void setIsNotDeleted(Short isNotDeleted) {
        this.isNotDeleted = isNotDeleted;
    }
}
