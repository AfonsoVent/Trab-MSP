package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import pt.unl.fct.msp.elderlyhealth.repository.Entidade;

/** Conta de acesso à aplicação (familiar, idoso, enfermeiro ou administrador). */
public class Utilizador implements Entidade {

    private Long id;
    private String nome;
    private String email;
    private String passwordHash;
    private Papel papel;
    private Set<Long> idosoIds = new LinkedHashSet<>();
    private Instant criadoEm;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public Set<Long> getIdosoIds() {
        return idosoIds;
    }

    public void setIdosoIds(Set<Long> idosoIds) {
        this.idosoIds = idosoIds;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
