package pt.unl.fct.msp.elderlyhealth.exception;

/** Dados recebidos inválidos (HTTP 400). */
public class PedidoInvalidoException extends RuntimeException {
    public PedidoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
