package pt.unl.fct.msp.elderlyhealth.model;

/** Subpastas dentro da pasta de cada idoso. */
public enum CategoriaDocumento {
    HISTORICO_CLINICO("historico-clinico"),
    EXAMES("exames"),
    RECEITAS("receitas"),
    RELATORIOS("relatorios"),
    OUTROS("outros");

    private final String pasta;

    CategoriaDocumento(String pasta) {
        this.pasta = pasta;
    }

    public String getPasta() {
        return pasta;
    }
}
