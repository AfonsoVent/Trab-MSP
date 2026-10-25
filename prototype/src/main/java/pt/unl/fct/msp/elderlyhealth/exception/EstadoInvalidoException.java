package pt.unl.fct.msp.elderlyhealth.exception;

//Estado inválido
public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
