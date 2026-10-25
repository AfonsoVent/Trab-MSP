package pt.unl.fct.msp.elderlyhealth.exception;

//Timer excedido
public class PedidoExpiradoException extends RuntimeException {
    public PedidoExpiradoException(String mensagem) {
        super(mensagem);
    }
}
