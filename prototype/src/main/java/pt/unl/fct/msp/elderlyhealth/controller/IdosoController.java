package pt.unl.fct.msp.elderlyhealth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.service.IdosoService;

/** Consulta das contas criadas pelo registo (apoio ao front-end). */
@RestController
@RequestMapping("/api/idosos")
public class IdosoController {

    private final IdosoService servico;

    public IdosoController(IdosoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Idoso> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public Idoso obter(@PathVariable Long id) {
        return servico.obter(id);
    }
}
