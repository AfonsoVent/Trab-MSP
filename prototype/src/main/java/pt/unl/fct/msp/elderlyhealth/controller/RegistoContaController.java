package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.unl.fct.msp.elderlyhealth.dto.IniciarRegistoRequest;
import pt.unl.fct.msp.elderlyhealth.model.ContactoEmergencia;
import pt.unl.fct.msp.elderlyhealth.model.DadosIdoso;
import pt.unl.fct.msp.elderlyhealth.model.PedidoRegisto;
import pt.unl.fct.msp.elderlyhealth.model.Preferencias;
import pt.unl.fct.msp.elderlyhealth.service.RegistoContaService;

/** Feature básica 1 — Register elder's account. */
@RestController
@RequestMapping("/api/registos")
public class RegistoContaController {

    private final RegistoContaService servico;

    public RegistoContaController(RegistoContaService servico) {
        this.servico = servico;
    }

    /** Início: "Pedido de registo" (corpo opcional: {"canal": "ONLINE|FAMILIAR|RECEPCIONISTA"}). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoRegisto iniciar(@RequestBody(required = false) IniciarRegistoRequest pedido) {
        return servico.iniciar(pedido == null ? null : pedido.canal());
    }

    /** "Receber os dados requisitados" -> validar NIF. */
    @PutMapping("/{id}/dados")
    public PedidoRegisto submeterDados(@PathVariable Long id, @Valid @RequestBody DadosIdoso dados) {
        return servico.submeterDados(id, dados);
    }

    /** "Receber os dados de preferências". */
    @PutMapping("/{id}/preferencias")
    public PedidoRegisto submeterPreferencias(@PathVariable Long id, @RequestBody Preferencias preferencias) {
        return servico.submeterPreferencias(id, preferencias);
    }

    /** "Receber o contacto de emergência" -> validar. */
    @PutMapping("/{id}/contacto-emergencia")
    public PedidoRegisto submeterContactoEmergencia(@PathVariable Long id,
                                                    @Valid @RequestBody ContactoEmergencia contacto) {
        return servico.submeterContactoEmergencia(id, contacto);
    }

    @GetMapping("/{id}")
    public PedidoRegisto obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    @GetMapping
    public List<PedidoRegisto> listar() {
        return servico.listar();
    }
}
