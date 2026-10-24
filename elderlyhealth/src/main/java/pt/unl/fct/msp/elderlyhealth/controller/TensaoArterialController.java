package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.unl.fct.msp.elderlyhealth.config.AuthInterceptor;
import pt.unl.fct.msp.elderlyhealth.dto.MedicaoTensaoRequest;
import pt.unl.fct.msp.elderlyhealth.model.MedicaoTensao;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.TensaoArterialService;

/** Feature básica 4 — Monitor blood pressure. */
@RestController
@RequestMapping("/api/tensao-arterial")
public class TensaoArterialController {

    private final TensaoArterialService servico;

    public TensaoArterialController(TensaoArterialService servico) {
        this.servico = servico;
    }

    /** Tensiómetro (cabeçalho X-Device-Key) ou simulador na aplicação. */
    @PostMapping("/medicoes")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicaoTensao registarMedicao(@RequestAttribute(name = AuthInterceptor.ATRIBUTO_UTILIZADOR, required = false) Utilizador u,
                                         @Valid @RequestBody MedicaoTensaoRequest pedido) {
        if (u != null) {
            AuthService.exigirAcessoIdoso(u, pedido.idosoId());
        }
        return servico.registarMedicao(pedido.idosoId(), pedido.dispositivoId(),
                pedido.sistolica(), pedido.diastolica(), pedido.pulso());
    }

    @GetMapping("/idosos/{idosoId}")
    public List<MedicaoTensao> historico(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long idosoId) {
        AuthService.exigirAcessoIdoso(u, idosoId);
        return servico.historico(idosoId);
    }
}
