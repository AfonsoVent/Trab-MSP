package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.unl.fct.msp.elderlyhealth.dto.QuedaDetetadaRequest;
import pt.unl.fct.msp.elderlyhealth.dto.RelatorioAtendimentoRequest;
import pt.unl.fct.msp.elderlyhealth.dto.RespostaIdosoRequest;
import pt.unl.fct.msp.elderlyhealth.model.AlertaQueda;
import pt.unl.fct.msp.elderlyhealth.model.EstadoQueda;
import pt.unl.fct.msp.elderlyhealth.service.DetecaoQuedaService;

/** Feature básica 3 — Detect fall. */
@RestController
@RequestMapping("/api/quedas")
public class DetecaoQuedaController {

    private final DetecaoQuedaService servico;

    public DetecaoQuedaController(DetecaoQuedaService servico) {
        this.servico = servico;
    }

    /** Chamado pelo sensor: "Queda detetada pelo sensor". */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertaQueda registarQueda(@Valid @RequestBody QuedaDetetadaRequest pedido) {
        return servico.registarQueda(pedido.idosoId(), pedido.sensorId());
    }

    /** "Resposta do idoso" (sim / não). */
    @PostMapping("/{id}/resposta")
    public AlertaQueda responder(@PathVariable Long id, @Valid @RequestBody RespostaIdosoRequest resposta) {
        return servico.responder(id, resposta.precisaAjuda());
    }

    /** "Receber relatório de atendimento" -> "Registar incidente". */
    @PostMapping("/{id}/relatorio")
    public AlertaQueda registarRelatorio(@PathVariable Long id,
                                         @Valid @RequestBody RelatorioAtendimentoRequest relatorio) {
        return servico.registarRelatorio(id, relatorio.enfermeiro(), relatorio.relatorio());
    }

    @GetMapping("/{id}")
    public AlertaQueda obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    /** Painel da enfermagem: GET /api/quedas?estado=ENFERMEIRO_ALERTADO */
    @GetMapping
    public List<AlertaQueda> listar(@RequestParam(required = false) EstadoQueda estado) {
        return servico.listar(estado);
    }
}
