package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pt.unl.fct.msp.elderlyhealth.config.AuthInterceptor;
import pt.unl.fct.msp.elderlyhealth.exception.AcessoNegadoException;
import pt.unl.fct.msp.elderlyhealth.model.CanalRegisto;
import pt.unl.fct.msp.elderlyhealth.model.ContactoEmergencia;
import pt.unl.fct.msp.elderlyhealth.model.DadosIdoso;
import pt.unl.fct.msp.elderlyhealth.model.Papel;
import pt.unl.fct.msp.elderlyhealth.model.PedidoRegisto;
import pt.unl.fct.msp.elderlyhealth.model.Preferencias;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.RegistoContaService;

/** Feature básica 1 — Register elder's account ("Inscrever idoso na plataforma" no main.bpmn). */
@RestController
@RequestMapping("/api/registos")
public class RegistoContaController {

    private final RegistoContaService servico;

    public RegistoContaController(RegistoContaService servico) {
        this.servico = servico;
    }

    /** O canal vem da conta: equipa = recepcionista, familiar = familiar, idoso = online. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoRegisto iniciar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u) {
        CanalRegisto canal = u.getPapel().isEquipa() ? CanalRegisto.RECEPCIONISTA
                : u.getPapel() == Papel.IDOSO ? CanalRegisto.ONLINE : CanalRegisto.FAMILIAR;
        return servico.iniciar(canal, u.getId());
    }

    @PutMapping("/{id}/dados")
    public PedidoRegisto submeterDados(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id, @Valid @RequestBody DadosIdoso dados) {
        exigirDono(u, id);
        return servico.submeterDados(id, dados);
    }

    @PutMapping("/{id}/preferencias")
    public PedidoRegisto submeterPreferencias(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id, @RequestBody Preferencias preferencias) {
        exigirDono(u, id);
        return servico.submeterPreferencias(id, preferencias);
    }

    @PutMapping("/{id}/contacto-emergencia")
    public PedidoRegisto submeterContactoEmergencia(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id,
                                                    @Valid @RequestBody ContactoEmergencia contacto) {
        exigirDono(u, id);
        return servico.submeterContactoEmergencia(id, contacto);
    }

    @GetMapping("/{id}")
    public PedidoRegisto obter(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id) {
        exigirDono(u, id);
        return servico.obter(id);
    }

    @GetMapping
    public List<PedidoRegisto> listar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u) {
        return servico.listar().stream()
                .filter(p -> u.getPapel().isEquipa() || u.getId().equals(p.getUtilizadorId()))
                .toList();
    }

    private void exigirDono(Utilizador u, Long pedidoId) {
        PedidoRegisto p = servico.obter(pedidoId);
        if (!u.getPapel().isEquipa() && !u.getId().equals(p.getUtilizadorId())) {
            throw new AcessoNegadoException("Este pedido de registo pertence a outra conta");
        }
    }
}
