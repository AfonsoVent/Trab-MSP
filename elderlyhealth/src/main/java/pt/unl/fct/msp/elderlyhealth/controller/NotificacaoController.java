package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.unl.fct.msp.elderlyhealth.config.AuthInterceptor;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.Notificacao;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.NotificacaoService;

/** Caixa de mensagens dos participantes (os message flows que saem do CPS), mais recentes primeiro. */
@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService servico;

    public NotificacaoController(NotificacaoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Notificacao> listar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u,
                                    @RequestParam(required = false) Destinatario destinatario,
                                    @RequestParam(required = false) Long idosoId) {
        if (idosoId != null) {
            AuthService.exigirAcessoIdoso(u, idosoId);
        }
        return servico.listar(destinatario, idosoId).stream()
                .filter(n -> u.getPapel().isEquipa() || (n.getIdosoId() != null && AuthService.podeVer(u, n.getIdosoId())))
                .sorted(Comparator.comparing(Notificacao::getId).reversed())
                .toList();
    }
}
