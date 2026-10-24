package pt.unl.fct.msp.elderlyhealth.service;

import java.util.List;

import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.exception.RecursoNaoEncontradoException;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.repository.IdosoRepository;

@Service
public class IdosoService {

    private final IdosoRepository idosos;

    public IdosoService(IdosoRepository idosos) {
        this.idosos = idosos;
    }

    public Idoso obter(Long id) {
        return idosos.procurar(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Idoso " + id + " não existe"));
    }

    public List<Idoso> listar() {
        return idosos.todos();
    }
}
