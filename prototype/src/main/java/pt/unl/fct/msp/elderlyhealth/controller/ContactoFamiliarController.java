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
import pt.unl.fct.msp.elderlyhealth.dto.ConfirmacaoRequest;
import pt.unl.fct.msp.elderlyhealth.model.ContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.DadosContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.PedidoContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.service.ContactoFamiliarService;

/** Feature básica 2 — Add family contacts. */
@RestController
@RequestMapping("/api/idosos/{idosoId}/contactos-familiares")
public class ContactoFamiliarController {

    private final ContactoFamiliarService servico;

    public ContactoFamiliarController(ContactoFamiliarService servico) {
        this.servico = servico;
    }

    /** "Contacto familiar" recebido -> validar -> pedir confirmação ao utilizador (202: fica à espera). */
    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PedidoContactoFamiliar pedirAdicao(@PathVariable Long idosoId,
                                              @Valid @RequestBody DadosContactoFamiliar dados) {
        return servico.pedirAdicao(idosoId, dados);
    }

    /** "Receber confirmação" (tem de chegar antes do timer de 1 minuto). */
    @PostMapping("/pedidos/{pedidoId}/confirmacao")
    public PedidoContactoFamiliar confirmar(@PathVariable Long idosoId, @PathVariable Long pedidoId,
                                            @Valid @RequestBody ConfirmacaoRequest confirmacao) {
        return servico.confirmar(idosoId, pedidoId, confirmacao.confirmado());
    }

    @GetMapping("/pedidos/{pedidoId}")
    public PedidoContactoFamiliar obterPedido(@PathVariable Long idosoId, @PathVariable Long pedidoId) {
        return servico.obterPedido(idosoId, pedidoId);
    }

    @GetMapping
    public List<ContactoFamiliar> listar(@PathVariable Long idosoId) {
        return servico.listar(idosoId);
    }
}
