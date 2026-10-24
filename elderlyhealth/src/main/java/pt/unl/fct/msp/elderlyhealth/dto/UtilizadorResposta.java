package pt.unl.fct.msp.elderlyhealth.dto;

import java.util.List;

import pt.unl.fct.msp.elderlyhealth.model.Papel;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;

/** O que se devolve sobre um utilizador (nunca inclui o hash da palavra-passe). */
public record UtilizadorResposta(Long id, String nome, String email, Papel papel, List<Long> idosoIds) {

    public static UtilizadorResposta de(Utilizador u) {
        return new UtilizadorResposta(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), List.copyOf(u.getIdosoIds()));
    }
}
