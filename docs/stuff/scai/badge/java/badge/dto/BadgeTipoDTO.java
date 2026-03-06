package it.olomedia.scai.modules.badge.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità BadgeTipo
 * Rappresenta una tipologia di badge (lookup)
 */
public class BadgeTipoDTO {

    private Long id;
    private String codTipoBadge;
    private String descrizioneTipoBadge;
    private Short isNotDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodTipoBadge() {
        return codTipoBadge;
    }

    public void setCodTipoBadge(String codTipoBadge) {
        this.codTipoBadge = codTipoBadge;
    }

    public String getDescrizioneTipoBadge() {
        return descrizioneTipoBadge;
    }

    public void setDescrizioneTipoBadge(String descrizioneTipoBadge) {
        this.descrizioneTipoBadge = descrizioneTipoBadge;
    }

    public Short getIsNotDeleted() {
        return isNotDeleted;
    }

    public void setIsNotDeleted(Short isNotDeleted) {
        this.isNotDeleted = isNotDeleted;
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
