package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;

import pt.unl.fct.msp.elderlyhealth.repository.Entidade;

/** Instância do subprocesso 'Adicionar contacto familiar'. */
public class PedidoContactoFamiliar implements Entidade {

    private Long id;
    private Long idosoId;
    private DadosContactoFamiliar dados;
    private EstadoPedidoContacto estado;
    private Long contactoId;
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

    public Long getIdosoId() {
        return idosoId;
    }

    public void setIdosoId(Long idosoId) {
        this.idosoId = idosoId;
    }

    public DadosContactoFamiliar getDados() {
        return dados;
    }

    public void setDados(DadosContactoFamiliar dados) {
        this.dados = dados;
    }

    public EstadoPedidoContacto getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedidoContacto estado) {
        this.estado = estado;
    }

    public Long getContactoId() {
        return contactoId;
    }

    public void setContactoId(Long contactoId) {
        this.contactoId = contactoId;
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
