package pt.unl.fct.msp.elderlyhealth.exception;

import java.time.Instant;

public record ErroResposta(Instant timestamp, int status, String erro, String mensagem) {
}
