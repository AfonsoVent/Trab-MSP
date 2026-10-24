package pt.unl.fct.msp.elderlyhealth.dto;

import java.time.Instant;

public record SessaoIniciada(String token, Instant expiraEm, UtilizadorResposta utilizador) {
}
