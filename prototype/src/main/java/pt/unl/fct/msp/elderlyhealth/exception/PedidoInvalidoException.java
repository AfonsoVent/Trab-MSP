package pt.unl.fct.msp.elderlyhealth.exception;

//Pedido inválido
public class PedidoInvalidoException extends RuntimeException {
    public PedidoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
