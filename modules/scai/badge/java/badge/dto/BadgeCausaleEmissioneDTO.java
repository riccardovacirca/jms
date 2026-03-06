package it.olomedia.scai.modules.badge.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità BadgeCausaleEmissione
 * Rappresenta una causale di emissione badge (lookup)
 */
public class BadgeCausaleEmissioneDTO {

    private Long id;
    private String codCausaleEmissione;
    private String descCausaleEmissione;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodCausaleEmissione() {
        return codCausaleEmissione;
    }

    public void setCodCausaleEmissione(String codCausaleEmissione) {
        this.codCausaleEmissione = codCausaleEmissione;
    }

    public String getDescCausaleEmissione() {
        return descCausaleEmissione;
    }

    public void setDescCausaleEmissione(String descCausaleEmissione) {
        this.descCausaleEmissione = descCausaleEmissione;
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
}
