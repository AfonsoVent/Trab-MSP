package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.unl.fct.msp.elderlyhealth.config.AuthInterceptor;
import pt.unl.fct.msp.elderlyhealth.dto.IdosoResumo;
import pt.unl.fct.msp.elderlyhealth.model.EstadoQueda;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.model.MedicaoTensao;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.ContactoFamiliarService;
import pt.unl.fct.msp.elderlyhealth.service.DetecaoQuedaService;
import pt.unl.fct.msp.elderlyhealth.service.IdosoService;
import pt.unl.fct.msp.elderlyhealth.service.TensaoArterialService;

/** Idosos visíveis para a conta: família vê os seus, a equipa vê todos. */
@RestController
@RequestMapping("/api/idosos")
public class IdosoController {

    private final IdosoService idosos;
    private final TensaoArterialService tensao;
    private final DetecaoQuedaService quedas;
    private final ContactoFamiliarService contactos;

    public IdosoController(IdosoService idosos, TensaoArterialService tensao, DetecaoQuedaService quedas,
                           ContactoFamiliarService contactos) {
        this.idosos = idosos;
        this.tensao = tensao;
        this.quedas = quedas;
        this.contactos = contactos;
    }

    @GetMapping
    public List<IdosoResumo> listar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u) {
        return idosos.listar().stream()
                .filter(i -> AuthService.podeVer(u, i.getId()))
                .map(this::resumo)
                .toList();
    }

    @GetMapping("/{id}")
    public IdosoResumo obter(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u, @PathVariable Long id) {
        AuthService.exigirAcessoIdoso(u, id);
        return resumo(idosos.obter(id));
    }

    private IdosoResumo resumo(Idoso i) {
        List<MedicaoTensao> medicoes = tensao.historico(i.getId());
        MedicaoTensao ultima = medicoes.isEmpty() ? null : medicoes.get(medicoes.size() - 1);
        long ativas = quedas.listar(null).stream()
                .filter(a -> a.getIdosoId().equals(i.getId()))
                .filter(a -> a.getEstado() == EstadoQueda.AGUARDA_RESPOSTA_IDOSO
                        || a.getEstado() == EstadoQueda.ENFERMEIRO_ALERTADO)
                .count();
        return new IdosoResumo(i, ultima, ativas, contactos.listar(i.getId()).size());
    }
}
