package it.olomedia.scai.modules.veicoli.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità Veicolo
 * Rappresenta l'anagrafica di un veicolo con targa, modello, tipo e alimentazione
 */
public class VeicoloDTO {

    private Long id;
    private String targa;
    private String modello;
    private String marca;
    private String colore;
    private Integer annoImmatricolazione;
    private Long veicoloTipoId;
    private Long veicoloAlimentazioneId;
    private String note;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private Short isNotDeleted;

    // Campi relazionali (per visualizzazione)
    private String tipoDescrizione;
    private String alimentazioneDescrizione;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTarga() {
        return targa;
    }

    public void setTarga(String targa) {
        this.targa = targa;
    }

    public String getModello() {
        return modello;
    }

    public void setModello(String modello) {
        this.modello = modello;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getColore() {
        return colore;
    }

    public void setColore(String colore) {
        this.colore = colore;
    }

    public Integer getAnnoImmatricolazione() {
        return annoImmatricolazione;
    }

    public void setAnnoImmatricolazione(Integer annoImmatricolazione) {
        this.annoImmatricolazione = annoImmatricolazione;
    }

    public Long getVeicoloTipoId() {
        return veicoloTipoId;
    }

    public void setVeicoloTipoId(Long veicoloTipoId) {
        this.veicoloTipoId = veicoloTipoId;
    }

    public Long getVeicoloAlimentazioneId() {
        return veicoloAlimentazioneId;
    }

    public void setVeicoloAlimentazioneId(Long veicoloAlimentazioneId) {
        this.veicoloAlimentazioneId = veicoloAlimentazioneId;
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

    public String getTipoDescrizione() {
        return tipoDescrizione;
    }

    public void setTipoDescrizione(String tipoDescrizione) {
        this.tipoDescrizione = tipoDescrizione;
    }

    public String getAlimentazioneDescrizione() {
        return alimentazioneDescrizione;
    }

    public void setAlimentazioneDescrizione(String alimentazioneDescrizione) {
        this.alimentazioneDescrizione = alimentazioneDescrizione;
    }
}
