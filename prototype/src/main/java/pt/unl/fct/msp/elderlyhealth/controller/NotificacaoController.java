package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.Notificacao;
import pt.unl.fct.msp.elderlyhealth.service.NotificacaoService;

/** Caixa de mensagens dos participantes (os message flows que saem do CPS). */
@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService servico;

    public NotificacaoController(NotificacaoService servico) {
        this.servico = servico;
    }

    /** Ex.: GET /api/notificacoes?destinatario=ENFERMEIRO */
    @GetMapping
    public List<Notificacao> listar(@RequestParam(required = false) Destinatario destinatario,
                                    @RequestParam(required = false) Long idosoId) {
        return servico.listar(destinatario, idosoId);
    }
}
