package pt.unl.fct.msp.elderlyhealth.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.model.ClassificacaoTensao;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.model.MedicaoTensao;
import pt.unl.fct.msp.elderlyhealth.repository.MedicaoTensaoRepository;

/**
 * Subprocesso BPMN "Monitorizar tensão arterial":
 * receber medição do dispositivo -> guardar -> analisar -> gateway "Estado da pressão?"
 * (OK -> notificar idoso | Alerta não grave -> gerar relatório de cuidados | Alerta grave -> notificar enfermeiro).
 */
@Service
public class TensaoArterialService {

    // Limiares do protótipo (mmHg) — a validar com a equipa / requisitos
    static final int SISTOLICA_GRAVE_ALTA = 180;
    static final int DIASTOLICA_GRAVE_ALTA = 120;
    static final int SISTOLICA_GRAVE_BAIXA = 80;
    static final int SISTOLICA_ALTA = 140;
    static final int DIASTOLICA_ALTA = 90;
    static final int SISTOLICA_BAIXA = 90;
    static final int DIASTOLICA_BAIXA = 60;

    private final MedicaoTensaoRepository medicoes;
    private final IdosoService idosos;
    private final NotificacaoService notificacoes;
    private final Clock clock;

    public TensaoArterialService(MedicaoTensaoRepository medicoes, IdosoService idosos,
                                 NotificacaoService notificacoes, Clock clock) {
        this.medicoes = medicoes;
        this.idosos = idosos;
        this.notificacoes = notificacoes;
        this.clock = clock;
    }

    public static ClassificacaoTensao classificar(int sistolica, int diastolica) {
        if (sistolica >= SISTOLICA_GRAVE_ALTA || diastolica >= DIASTOLICA_GRAVE_ALTA || sistolica < SISTOLICA_GRAVE_BAIXA) {
            return ClassificacaoTensao.ALERTA_GRAVE;
        }
        if (sistolica >= SISTOLICA_ALTA || diastolica >= DIASTOLICA_ALTA
                || sistolica < SISTOLICA_BAIXA || diastolica < DIASTOLICA_BAIXA) {
            return ClassificacaoTensao.ALERTA_NAO_GRAVE;
        }
        return ClassificacaoTensao.NORMAL;
    }

    public synchronized MedicaoTensao registarMedicao(Long idosoId, String dispositivoId,
                                                      int sistolica, int diastolica, Integer pulso) {
        Idoso idoso = idosos.obter(idosoId);
        if (sistolica <= diastolica) {
            throw new PedidoInvalidoException("A tensão sistólica tem de ser maior do que a diastólica");
        }

        MedicaoTensao m = new MedicaoTensao();
        m.setIdosoId(idosoId);
        m.setDispositivoId(dispositivoId);
        m.setSistolica(sistolica);
        m.setDiastolica(diastolica);
        m.setPulso(pulso);
        m.setMedidoEm(clock.instant());
        m.setClassificacao(classificar(sistolica, diastolica));
        m.setRecomendacoes(List.of());
        medicoes.guardar(m);

        String ref = "TENSAO#" + m.getId();
        String valores = sistolica + "/" + diastolica + " mmHg";
        switch (m.getClassificacao()) {
            case NORMAL -> notificacoes.enviar(Destinatario.IDOSO, idosoId, ref,
                    "Tensão arterial normal (" + valores + ")");
            case ALERTA_NAO_GRAVE -> {
                m.setRecomendacoes(gerarRelatorioCuidados(sistolica, diastolica));
                notificacoes.enviar(Destinatario.IDOSO, idosoId, ref,
                        "Tensão fora do normal (" + valores + "). Recomendações: " + String.join("; ", m.getRecomendacoes()));
            }
            case ALERTA_GRAVE -> {
                notificacoes.enviar(Destinatario.ENFERMEIRO, idosoId, ref,
                        "URGENTE: tensão arterial de " + idoso.getNome() + " em " + valores
                                + " — quarto " + idoso.getQuarto());
                notificacoes.enviar(Destinatario.IDOSO, idosoId, ref,
                        "A sua tensão está em " + valores + ". A equipa de enfermagem foi alertada e vai ter consigo.");
            }
        }
        return m;
    }

    public List<MedicaoTensao> historico(Long idosoId) {
        idosos.obter(idosoId);
        return medicoes.filtrar(m -> m.getIdosoId().equals(idosoId));
    }

    /** Service task "Gerar relatório de cuidados". */
    private static List<String> gerarRelatorioCuidados(int sistolica, int diastolica) {
        if (sistolica < SISTOLICA_BAIXA || diastolica < DIASTOLICA_BAIXA) {
            return List.of("Beber água", "Levantar-se devagar", "Repetir a medição dentro de 30 minutos");
        }
        return List.of("Descansar 15 minutos sentado", "Evitar sal e cafeína",
                "Confirmar que tomou a medicação prescrita", "Repetir a medição dentro de 30 minutos");
    }
}
