package it.olomedia.scai.modules.veicoli.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO per l'entità VeicoloConvenzione
 * Rappresenta l'associazione tra veicoli e dipendenti con stato autorizzazione
 */
public class VeicoloConvenzioneDTO {

    private Long id;
    private String codEnte;
    private String matricola;
    private String targa;
    private LocalDate dataInizio;
    private LocalDate dataFine;
    private String status;
    private String note;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Short isNotDeleted;

    // Campi relazionali (per visualizzazione)
    private String nomeRapporto;
    private String cognomeRapporto;
    private String veicoloModello;
    private String veicoloMarca;

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

    public String getTarga() {
        return targa;
    }

    public void setTarga(String targa) {
        this.targa = targa;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
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

    public String getVeicoloModello() {
        return veicoloModello;
    }

    public void setVeicoloModello(String veicoloModello) {
        this.veicoloModello = veicoloModello;
    }

    public String getVeicoloMarca() {
        return veicoloMarca;
    }

    public void setVeicoloMarca(String veicoloMarca) {
        this.veicoloMarca = veicoloMarca;
    }
}
