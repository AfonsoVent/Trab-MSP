package pt.unl.fct.msp.elderlyhealth.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import pt.unl.fct.msp.elderlyhealth.model.Utilizador;

@Repository
public class UtilizadorRepository extends InMemoryRepository<Utilizador> {

    public Optional<Utilizador> procurarPorEmail(String email) {
        return todos().stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }
}
