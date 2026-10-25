package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;
import java.time.LocalDate;

import pt.unl.fct.msp.elderlyhealth.repository.Entidade;

//Conta idoso
public class Idoso implements Entidade {

    private Long id;
    private String nome;
    private String nif;
    private LocalDate dataNascimento;
    private String informacaoMedica;
    private String quarto;
    private Preferencias preferencias;
    private ContactoEmergencia contactoEmergencia;
    private Instant registadoEm;

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

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getInformacaoMedica() {
        return informacaoMedica;
    }

    public void setInformacaoMedica(String informacaoMedica) {
        this.informacaoMedica = informacaoMedica;
    }

    public String getQuarto() {
        return quarto;
    }

    public void setQuarto(String quarto) {
        this.quarto = quarto;
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

    public Instant getRegistadoEm() {
        return registadoEm;
    }

    public void setRegistadoEm(Instant registadoEm) {
        this.registadoEm = registadoEm;
    }
}
