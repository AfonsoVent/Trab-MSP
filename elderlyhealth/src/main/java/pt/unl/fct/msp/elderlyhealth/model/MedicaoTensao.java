package pt.unl.fct.msp.elderlyhealth.model;

import java.time.Instant;
import java.util.List;

import pt.unl.fct.msp.elderlyhealth.repository.Entidade;

/** Medição enviada pelo dispositivo e classificada pelo CPS. */
public class MedicaoTensao implements Entidade {

    private Long id;
    private Long idosoId;
    private String dispositivoId;
    private int sistolica;
    private int diastolica;
    private Integer pulso;
    private ClassificacaoTensao classificacao;
    private List<String> recomendacoes;
    private Instant medidoEm;

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

    public String getDispositivoId() {
        return dispositivoId;
    }

    public void setDispositivoId(String dispositivoId) {
        this.dispositivoId = dispositivoId;
    }

    public int getSistolica() {
        return sistolica;
    }

    public void setSistolica(int sistolica) {
        this.sistolica = sistolica;
    }

    public int getDiastolica() {
        return diastolica;
    }

    public void setDiastolica(int diastolica) {
        this.diastolica = diastolica;
    }

    public Integer getPulso() {
        return pulso;
    }

    public void setPulso(Integer pulso) {
        this.pulso = pulso;
    }

    public ClassificacaoTensao getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(ClassificacaoTensao classificacao) {
        this.classificacao = classificacao;
    }

    public List<String> getRecomendacoes() {
        return recomendacoes;
    }

    public void setRecomendacoes(List<String> recomendacoes) {
        this.recomendacoes = recomendacoes;
    }

    public Instant getMedidoEm() {
        return medidoEm;
    }

    public void setMedidoEm(Instant medidoEm) {
        this.medidoEm = medidoEm;
    }
}
