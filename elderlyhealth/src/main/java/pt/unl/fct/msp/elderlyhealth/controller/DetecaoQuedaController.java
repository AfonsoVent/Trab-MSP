package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.unl.fct.msp.elderlyhealth.config.AuthInterceptor;
import pt.unl.fct.msp.elderlyhealth.dto.QuedaDetetadaRequest;
import pt.unl.fct.msp.elderlyhealth.dto.RelatorioAtendimentoRequest;
import pt.unl.fct.msp.elderlyhealth.dto.RespostaIdosoRequest;
import pt.unl.fct.msp.elderlyhealth.model.AlertaQueda;
import pt.unl.fct.msp.elderlyhealth.model.EstadoQueda;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.DetecaoQuedaService;

/** Feature básica 3 — Detect fall. */
@RestController
@RequestMapping("/api/quedas")
public class DetecaoQuedaController {

    private final DetecaoQuedaService servico;

    public DetecaoQuedaController(DetecaoQuedaService servico) {
        this.servico = servico;
    }

    /** Sensor (cabeçalho X-Device-Key) ou simulador na aplicação (sessão com acesso ao idoso). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertaQueda registarQueda(@RequestAttribute(name = AuthInterceptor.ATRIBUTO_UTILIZADOR, required = false) Utilizador u,
                                     @Valid @RequestBody QuedaDetetadaRequest pedido) {
        if (u != null) {
            AuthService.exigirAcessoIdoso(u, pedido.idosoId());
        }
        return servico.registarQueda(pedido.idosoId(), pedido.sensorId());
    }

    @PostMapping("/{id}/resposta")
    public AlertaQueda responder(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id, @Valid @RequestBody RespostaIdosoRequest resposta) {
        AuthService.exigirAcessoIdoso(u, servico.obter(id).getIdosoId());
        return servico.responder(id, resposta.precisaAjuda());
    }

    /** Só a equipa de enfermagem regista o relatório do atendimento. */
    @PostMapping("/{id}/relatorio")
    public AlertaQueda registarRelatorio(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id,
                                         @Valid @RequestBody RelatorioAtendimentoRequest relatorio) {
        AuthService.exigirEquipa(u);
        return servico.registarRelatorio(id, relatorio.enfermeiro(), relatorio.relatorio());
    }

    @GetMapping("/{id}")
    public AlertaQueda obter(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id) {
        AlertaQueda a = servico.obter(id);
        AuthService.exigirAcessoIdoso(u, a.getIdosoId());
        return a;
    }

    @GetMapping
    public List<AlertaQueda> listar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @RequestParam(required = false) EstadoQueda estado,
                                    @RequestParam(required = false) Long idosoId) {
        if (idosoId != null) {
            AuthService.exigirAcessoIdoso(u, idosoId);
        }
        return servico.listar(estado).stream()
                .filter(a -> idosoId == null || a.getIdosoId().equals(idosoId))
                .filter(a -> AuthService.podeVer(u, a.getIdosoId()))
                .toList();
    }
}
