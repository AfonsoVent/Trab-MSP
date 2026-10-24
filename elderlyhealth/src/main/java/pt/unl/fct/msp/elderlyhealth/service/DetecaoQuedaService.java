package pt.unl.fct.msp.elderlyhealth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.exception.EstadoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.RecursoNaoEncontradoException;
import pt.unl.fct.msp.elderlyhealth.model.AlertaQueda;
import pt.unl.fct.msp.elderlyhealth.model.CategoriaDocumento;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.EstadoQueda;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.repository.AlertaQuedaRepository;

/**
 * Subprocesso BPMN "Detetar queda":
 * queda detetada pelo sensor -> pedir confirmação ao idoso -> event-based gateway
 * (resposta | 1 minuto sem resposta) -> obter quarto -> alertar enfermeiro
 * -> receber relatório -> registar incidente.
 */
@Service
public class DetecaoQuedaService {

    private final AlertaQuedaRepository alertas;
    private final IdosoService idosos;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Europe/Lisbon"));

    private final NotificacaoService notificacoes;
    private final DocumentoService documentos;
    private final Clock clock;
    private final Duration timeoutResposta;

    public DetecaoQuedaService(AlertaQuedaRepository alertas, IdosoService idosos,
                               NotificacaoService notificacoes, DocumentoService documentos, Clock clock,
                               @Value("${elderlyhealth.queda.timeout-resposta:PT1M}") Duration timeoutResposta) {
        this.alertas = alertas;
        this.idosos = idosos;
        this.notificacoes = notificacoes;
        this.documentos = documentos;
        this.clock = clock;
        this.timeoutResposta = timeoutResposta;
    }

    /** Start event "Queda detetada pelo sensor" + send task "Pedir confirmação de ajuda ao idoso". */
    public synchronized AlertaQueda registarQueda(Long idosoId, String sensorId) {
        Idoso idoso = idosos.obter(idosoId);
        AlertaQueda a = new AlertaQueda();
        a.setIdosoId(idosoId);
        a.setSensorId(sensorId);
        a.setQuarto(idoso.getQuarto());
        a.setEstado(EstadoQueda.AGUARDA_RESPOSTA_IDOSO);
        a.setDetetadoEm(clock.instant());
        alertas.guardar(a);
        notificacoes.enviar(Destinatario.IDOSO, idosoId, ref(a),
                "Detetámos uma possível queda. Precisa de ajuda? Sem resposta em "
                        + timeoutResposta.toSeconds() + " segundos, a enfermagem será alertada.");
        return a;
    }

    /** Message catch "Resposta do idoso" -> gateway "Ajuda necessária?". */
    public synchronized AlertaQueda responder(Long alertaId, boolean precisaAjuda) {
        AlertaQueda a = procurar(alertaId);
        escalarSeExpirou(a);
        if (a.getEstado() != EstadoQueda.AGUARDA_RESPOSTA_IDOSO) {
            throw new EstadoInvalidoException("O alerta " + alertaId + " já está em " + a.getEstado());
        }
        if (!precisaAjuda) {
            a.setEstado(EstadoQueda.CANCELADO);
            alertas.guardar(a);
            notificacoes.enviar(Destinatario.IDOSO, a.getIdosoId(), ref(a), "Alerta cancelado. Ainda bem que está bem!");
            return a;
        }
        return alertarEnfermeiro(a, false);
    }

    /** Receive task "Receber relatório de atendimento" + service task "Registar incidente". */
    public synchronized AlertaQueda registarRelatorio(Long alertaId, String enfermeiro, String relatorio) {
        AlertaQueda a = procurar(alertaId);
        escalarSeExpirou(a);
        if (a.getEstado() != EstadoQueda.ENFERMEIRO_ALERTADO) {
            throw new EstadoInvalidoException("O alerta " + alertaId + " está em " + a.getEstado()
                    + " — só é possível registar o relatório depois de o enfermeiro ser alertado");
        }
        a.setEnfermeiro(enfermeiro);
        a.setRelatorio(relatorio);
        a.setEstado(EstadoQueda.INCIDENTE_REGISTADO);
        a.setRegistadoEm(clock.instant());
        alertas.guardar(a);
        arquivarRelatorio(a);
        return a;
    }

    public synchronized AlertaQueda obter(Long alertaId) {
        AlertaQueda a = procurar(alertaId);
        escalarSeExpirou(a);
        return a;
    }

    public synchronized List<AlertaQueda> listar(EstadoQueda estado) {
        alertas.todos().forEach(this::escalarSeExpirou);
        return alertas.filtrar(a -> estado == null || a.getEstado() == estado);
    }

    /** Timer "1 minuto sem resposta" (chamado periodicamente pelo TimeoutScheduler). */
    public synchronized void escalarSemResposta() {
        alertas.todos().forEach(this::escalarSeExpirou);
    }

    private void escalarSeExpirou(AlertaQueda a) {
        if (a.getEstado() == EstadoQueda.AGUARDA_RESPOSTA_IDOSO
                && clock.instant().isAfter(a.getDetetadoEm().plus(timeoutResposta))) {
            alertarEnfermeiro(a, true);
        }
    }

    /** "Obter quarto e dados do idoso" + send task "Alertar enfermeiro". */
    private AlertaQueda alertarEnfermeiro(AlertaQueda a, boolean porTimeout) {
        Idoso idoso = idosos.obter(a.getIdosoId());
        a.setQuarto(idoso.getQuarto());
        a.setEscaladoPorTimeout(porTimeout);
        a.setEstado(EstadoQueda.ENFERMEIRO_ALERTADO);
        a.setEnfermeiroAlertadoEm(clock.instant());
        alertas.guardar(a);
        notificacoes.enviar(Destinatario.ENFERMEIRO, a.getIdosoId(), ref(a),
                "QUEDA: " + idoso.getNome() + " — quarto " + (idoso.getQuarto() == null ? "desconhecido" : idoso.getQuarto())
                        + (porTimeout ? " (sem resposta do idoso)" : " (idoso pediu ajuda)")
                        + ". Info médica: " + idoso.getInformacaoMedica());
        return a;
    }

    /** O incidente fica no histórico do idoso: pasta relatorios/ da pasta do idoso. */
    private void arquivarRelatorio(AlertaQueda a) {
        Idoso idoso = idosos.obter(a.getIdosoId());
        String texto = "RELATÓRIO DE INCIDENTE — QUEDA #" + a.getId() + "\n\n"
                + "Idoso: " + idoso.getNome() + " (NIF " + idoso.getNif() + ")\n"
                + "Quarto: " + a.getQuarto() + "\n"
                + "Sensor: " + (a.getSensorId() == null ? "-" : a.getSensorId()) + "\n"
                + "Queda detetada: " + DATA.format(a.getDetetadoEm()) + "\n"
                + "Enfermeiro alertado: " + DATA.format(a.getEnfermeiroAlertadoEm())
                + (a.isEscaladoPorTimeout() ? " (sem resposta do idoso)" : " (idoso pediu ajuda)") + "\n"
                + "Atendido por: " + a.getEnfermeiro() + "\n"
                + "Registado: " + DATA.format(a.getRegistadoEm()) + "\n\n"
                + "Relatório:\n" + a.getRelatorio() + "\n";
        documentos.guardarTexto(a.getIdosoId(), CategoriaDocumento.RELATORIOS,
                "incidente-queda-" + a.getId() + ".txt", texto);
    }

    private AlertaQueda procurar(Long id) {
        return alertas.procurar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta de queda " + id + " não existe"));
    }

    private static String ref(AlertaQueda a) {
        return "QUEDA#" + a.getId();
    }
}
