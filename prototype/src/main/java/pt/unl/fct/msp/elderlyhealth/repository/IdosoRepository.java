package pt.unl.fct.msp.elderlyhealth.repository;

import org.springframework.stereotype.Repository;

import pt.unl.fct.msp.elderlyhealth.model.Idoso;

@Repository
public class IdosoRepository extends InMemoryRepository<Idoso> {

    public boolean existeNif(String nif) {
        return todos().stream().anyMatch(i -> i.getNif().equals(nif));
    }
}
