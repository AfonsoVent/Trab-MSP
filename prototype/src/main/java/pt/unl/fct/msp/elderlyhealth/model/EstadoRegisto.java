package pt.unl.fct.msp.elderlyhealth.model;

public enum EstadoRegisto {
    AGUARDA_DADOS,
    AGUARDA_PREFERENCIAS_E_CONTACTO,
    REGISTADO,
    NEGADO,
    EXPIRADO;

    public boolean isFinal() {
        return this == REGISTADO || this == NEGADO || this == EXPIRADO;
    }
}
