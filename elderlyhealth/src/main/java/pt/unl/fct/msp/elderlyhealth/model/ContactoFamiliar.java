package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;

import pt.unl.fct.msp.elderlyhealth.repository.Entidade;

/** Contacto familiar confirmado e guardado. */
public class ContactoFamiliar implements Entidade {

    private Long id;
    private Long idosoId;
    private String nome;
    private String telefone;
    private String email;
    private String parentesco;
    private Instant adicionadoEm;

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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getParentesco() {
        return parentesco;
    }

    public void setParentesco(String parentesco) {
        this.parentesco = parentesco;
    }

    public Instant getAdicionadoEm() {
        return adicionadoEm;
    }

    public void setAdicionadoEm(Instant adicionadoEm) {
        this.adicionadoEm = adicionadoEm;
    }
}
