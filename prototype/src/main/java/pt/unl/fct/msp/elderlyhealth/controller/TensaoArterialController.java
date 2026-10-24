package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.unl.fct.msp.elderlyhealth.dto.MedicaoTensaoRequest;
import pt.unl.fct.msp.elderlyhealth.model.MedicaoTensao;
import pt.unl.fct.msp.elderlyhealth.service.TensaoArterialService;

/** Feature básica 4 — Monitor blood pressure. */
@RestController
@RequestMapping("/api/tensao-arterial")
public class TensaoArterialController {

    private final TensaoArterialService servico;

    public TensaoArterialController(TensaoArterialService servico) {
        this.servico = servico;
    }

    /** Chamado pelo dispositivo: medição -> analisar -> gateway "Estado da pressão?". */
    @PostMapping("/medicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicaoTensao registarMedicao(@Valid @RequestBody MedicaoTensaoRequest pedido) {
        return servico.registarMedicao(pedido.idosoId(), pedido.dispositivoId(),
                pedido.sistolica(), pedido.diastolica(), pedido.pulso());
    }

    @GetMapping("/idosos/{idosoId}")
    public List<MedicaoTensao> historico(@PathVariable Long idosoId) {
        return servico.historico(idosoId);
    }
}
