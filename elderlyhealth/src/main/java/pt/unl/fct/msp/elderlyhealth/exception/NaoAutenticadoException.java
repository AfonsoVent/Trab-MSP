package pt.unl.fct.msp.elderlyhealth.exception;

/** Sem sessão válida ou credenciais erradas (HTTP 401). */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
