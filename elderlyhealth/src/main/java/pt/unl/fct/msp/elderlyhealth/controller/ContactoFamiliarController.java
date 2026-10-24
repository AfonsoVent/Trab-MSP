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
import pt.unl.fct.msp.elderlyhealth.dto.ConfirmacaoRequest;
import pt.unl.fct.msp.elderlyhealth.model.ContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.DadosContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.PedidoContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.ContactoFamiliarService;

/** Feature básica 2 — Add family contacts. */
@RestController
@RequestMapping("/api/idosos/{idosoId}/contactos-familiares")
public class ContactoFamiliarController {

    private final ContactoFamiliarService servico;

    public ContactoFamiliarController(ContactoFamiliarService servico) {
        this.servico = servico;
    }

    @PostMapping("/pedidos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PedidoContactoFamiliar pedirAdicao(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long idosoId,
                                              @Valid @RequestBody DadosContactoFamiliar dados) {
        AuthService.exigirAcessoIdoso(u, idosoId);
        return servico.pedirAdicao(idosoId, dados);
    }

    @PostMapping("/pedidos/{pedidoId}/confirmacao")
    public PedidoContactoFamiliar confirmar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long idosoId, @PathVariable Long pedidoId,
                                            @Valid @RequestBody ConfirmacaoRequest confirmacao) {
        AuthService.exigirAcessoIdoso(u, idosoId);
        return servico.confirmar(idosoId, pedidoId, confirmacao.confirmado());
    }

    @GetMapping("/pedidos/{pedidoId}")
    public PedidoContactoFamiliar obterPedido(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long idosoId, @PathVariable Long pedidoId) {
        AuthService.exigirAcessoIdoso(u, idosoId);
        return servico.obterPedido(idosoId, pedidoId);
    }

    @GetMapping
    public List<ContactoFamiliar> listar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long idosoId) {
        AuthService.exigirAcessoIdoso(u, idosoId);
        return servico.listar(idosoId);
    }
}
