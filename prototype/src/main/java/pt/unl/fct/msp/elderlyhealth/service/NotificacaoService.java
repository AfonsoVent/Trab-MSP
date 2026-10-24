package pt.unl.fct.msp.elderlyhealth.service;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.Notificacao;
import pt.unl.fct.msp.elderlyhealth.repository.NotificacaoRepository;

/**
 * Representa os message flows do CPS para os participantes "black box"
 * (idoso, família, enfermeiro). No protótipo ficam guardadas e podem ser lidas
 * por GET /api/notificacoes; mais tarde podem ir por push/SMS/e-mail.
 */
@Service
public class NotificacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoRepository repositorio;
    private final Clock clock;

    public NotificacaoService(NotificacaoRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    public Notificacao enviar(Destinatario destinatario, Long idosoId, String referencia, String mensagem) {
        Notificacao n = new Notificacao();
        n.setDestinatario(destinatario);
        n.setIdosoId(idosoId);
        n.setReferencia(referencia);
        n.setMensagem(mensagem);
        n.setCriadaEm(clock.instant());
        repositorio.guardar(n);
        LOG.info("[NOTIFICAÇÃO -> {}] ({}) {}", destinatario, referencia, mensagem);
        return n;
    }

    public List<Notificacao> listar(Destinatario destinatario, Long idosoId) {
        return repositorio.filtrar(n -> (destinatario == null || n.getDestinatario() == destinatario)
                && (idosoId == null || idosoId.equals(n.getIdosoId())));
    }
}
