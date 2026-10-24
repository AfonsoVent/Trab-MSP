package pt.unl.fct.msp.elderlyhealth.exception;

/** Sem permissão para este recurso (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
