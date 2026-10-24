package pt.unl.fct.msp.elderlyhealth.exception;

/** Timer do BPMN excedido (HTTP 410). */
public class PedidoExpiradoException extends RuntimeException {
    public PedidoExpiradoException(String mensagem) {
        super(mensagem);
    }
}
