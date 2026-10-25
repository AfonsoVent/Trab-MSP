package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;

import pt.unl.fct.msp.elderlyhealth.repository.Entidade;

//Alerta queda
public class AlertaQueda implements Entidade {

    private Long id;
    private Long idosoId;
    private String sensorId;
    private EstadoQueda estado;
    private String quarto;
    private boolean escaladoPorTimeout;
    private String enfermeiro;
    private String relatorio;
    private Instant detetadoEm;
    private Instant enfermeiroAlertadoEm;
    private Instant registadoEm;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdosoId() {
        return idosoId;
    }

    public void setIdosoId(Long idosoId) {
        this.idosoId = idosoId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public EstadoQueda getEstado() {
        return estado;
    }

    public void setEstado(EstadoQueda estado) {
        this.estado = estado;
    }

    public String getQuarto() {
        return quarto;
    }

    public void setQuarto(String quarto) {
        this.quarto = quarto;
    }

    public boolean isEscaladoPorTimeout() {
        return escaladoPorTimeout;
    }

    public void setEscaladoPorTimeout(boolean escaladoPorTimeout) {
        this.escaladoPorTimeout = escaladoPorTimeout;
    }

    public String getEnfermeiro() {
        return enfermeiro;
    }

    public void setEnfermeiro(String enfermeiro) {
        this.enfermeiro = enfermeiro;
    }

    public String getRelatorio() {
        return relatorio;
    }

    public void setRelatorio(String relatorio) {
        this.relatorio = relatorio;
    }

    public Instant getDetetadoEm() {
        return detetadoEm;
    }

    public void setDetetadoEm(Instant detetadoEm) {
        this.detetadoEm = detetadoEm;
    }

    public Instant getEnfermeiroAlertadoEm() {
        return enfermeiroAlertadoEm;
    }

    public void setEnfermeiroAlertadoEm(Instant enfermeiroAlertadoEm) {
        this.enfermeiroAlertadoEm = enfermeiroAlertadoEm;
    }

    public Instant getRegistadoEm() {
        return registadoEm;
    }

    public void setRegistadoEm(Instant registadoEm) {
        this.registadoEm = registadoEm;
    }
}
