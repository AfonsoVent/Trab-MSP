package pt.unl.fct.msp.elderlyhealth.exception;

/** Passo do processo fora de ordem (HTTP 409). */
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
