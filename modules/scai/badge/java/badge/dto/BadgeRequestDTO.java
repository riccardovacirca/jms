package it.olomedia.scai.modules.badge.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità BadgeRequest
 * Rappresenta una richiesta di assegnazione badge
 *
 * ⚠️ NOTA: Usa codEnte (String) - inconsistenza con BadgeDTO che usa idEnte (Long)
 */
public class BadgeRequestDTO {

    private Long id;
    private String codEnte;             // ⚠️ VARCHAR natural key (diverso da BadgeDTO!)
    private String matricola;
    private String codFis;
    private LocalDateTime dataInizioValidita;
    private Long instanceId;
    private Short isNotDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Campi relazionali (per visualizzazione)
    private String descrizioneEnte;
    private String nomeRapporto;
    private String cognomeRapporto;

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

    public String getCodFis() {
        return codFis;
    }

    public void setCodFis(String codFis) {
        this.codFis = codFis;
    }

    public LocalDateTime getDataInizioValidita() {
        return dataInizioValidita;
    }

    public void setDataInizioValidita(LocalDateTime dataInizioValidita) {
        this.dataInizioValidita = dataInizioValidita;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
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

    public String getDescrizioneEnte() {
        return descrizioneEnte;
    }

    public void setDescrizioneEnte(String descrizioneEnte) {
        this.descrizioneEnte = descrizioneEnte;
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
}
