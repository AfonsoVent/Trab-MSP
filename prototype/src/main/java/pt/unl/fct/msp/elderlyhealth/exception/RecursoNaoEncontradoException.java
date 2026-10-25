package pt.unl.fct.msp.elderlyhealth.exception;

//Recurso inexistente
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
