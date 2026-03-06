package it.olomedia.scai.modules.badge.dto;

import java.time.LocalDateTime;

/**
 * DTO per l'entità Badge
 * Rappresenta un badge di accesso associato a un dipendente
 *
 * ⚠️ ATTENZIONE: Usa idEnte (Long) invece di codEnte (String)
 * Questa è un'inconsistenza con altri moduli che usano natural key cod_ente.
 * Vedere migration per dettagli.
 */
public class BadgeDTO {

    private Long id;
    private Integer seqId;
    private Long idEnte;                    // ⚠️ BIGINT invece di VARCHAR!
    private Long idTipoTessera;
    private String numero;
    private String matricola;
    private String codFis;
    private String cognome;
    private String nome;
    private String tecnologia;
    private LocalDateTime dataInizioValidita;
    private LocalDateTime dataFineValidita;
    private LocalDateTime dataProduzione;
    private LocalDateTime dataRitiro;
    private Short attivo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Short isNotDeleted;

    // Campi relazionali (per visualizzazione)
    private String descrizioneEnte;
    private String descrizioneTipoBadge;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSeqId() {
        return seqId;
    }

    public void setSeqId(Integer seqId) {
        this.seqId = seqId;
    }

    public Long getIdEnte() {
        return idEnte;
    }

    public void setIdEnte(Long idEnte) {
        this.idEnte = idEnte;
    }

    public Long getIdTipoTessera() {
        return idTipoTessera;
    }

    public void setIdTipoTessera(Long idTipoTessera) {
        this.idTipoTessera = idTipoTessera;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
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

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTecnologia() {
        return tecnologia;
    }

    public void setTecnologia(String tecnologia) {
        this.tecnologia = tecnologia;
    }

    public LocalDateTime getDataInizioValidita() {
        return dataInizioValidita;
    }

    public void setDataInizioValidita(LocalDateTime dataInizioValidita) {
        this.dataInizioValidita = dataInizioValidita;
    }

    public LocalDateTime getDataFineValidita() {
        return dataFineValidita;
    }

    public void setDataFineValidita(LocalDateTime dataFineValidita) {
        this.dataFineValidita = dataFineValidita;
    }

    public LocalDateTime getDataProduzione() {
        return dataProduzione;
    }

    public void setDataProduzione(LocalDateTime dataProduzione) {
        this.dataProduzione = dataProduzione;
    }

    public LocalDateTime getDataRitiro() {
        return dataRitiro;
    }

    public void setDataRitiro(LocalDateTime dataRitiro) {
        this.dataRitiro = dataRitiro;
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

    public String getDescrizioneEnte() {
        return descrizioneEnte;
    }

    public void setDescrizioneEnte(String descrizioneEnte) {
        this.descrizioneEnte = descrizioneEnte;
    }

    public String getDescrizioneTipoBadge() {
        return descrizioneTipoBadge;
    }

    public void setDescrizioneTipoBadge(String descrizioneTipoBadge) {
        this.descrizioneTipoBadge = descrizioneTipoBadge;
    }
}
