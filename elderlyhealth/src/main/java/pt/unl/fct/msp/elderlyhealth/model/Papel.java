package pt.unl.fct.msp.elderlyhealth.model;

public enum Papel {
    FAMILIAR,
    IDOSO,
    ENFERMEIRO,
    ADMIN;

    /** Equipa do hospital: vê todos os idosos. */
    public boolean isEquipa() {
        return this == ENFERMEIRO || this == ADMIN;
    }
}
