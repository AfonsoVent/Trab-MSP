package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;

import pt.unl.fct.msp.elderlyhealth.repository.Entidade;

/** Instância do subprocesso 'Registar conta do idoso'. */
public class PedidoRegisto implements Entidade {

    private Long id;
    private CanalRegisto canal;
    private EstadoRegisto estado;
    private DadosIdoso dados;
    private Preferencias preferencias;
    private ContactoEmergencia contactoEmergencia;
    private String motivo;
    private Long idosoId;
    private Instant criadoEm;
    private Instant atualizadoEm;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public CanalRegisto getCanal() {
        return canal;
    }

    public void setCanal(CanalRegisto canal) {
        this.canal = canal;
    }

    public EstadoRegisto getEstado() {
        return estado;
    }

    public void setEstado(EstadoRegisto estado) {
        this.estado = estado;
    }

    public DadosIdoso getDados() {
        return dados;
    }

    public void setDados(DadosIdoso dados) {
        this.dados = dados;
    }

    public Preferencias getPreferencias() {
        return preferencias;
    }

    public void setPreferencias(Preferencias preferencias) {
        this.preferencias = preferencias;
    }

    public ContactoEmergencia getContactoEmergencia() {
        return contactoEmergencia;
    }

    public void setContactoEmergencia(ContactoEmergencia contactoEmergencia) {
        this.contactoEmergencia = contactoEmergencia;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getIdosoId() {
        return idosoId;
    }

    public void setIdosoId(Long idosoId) {
        this.idosoId = idosoId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
