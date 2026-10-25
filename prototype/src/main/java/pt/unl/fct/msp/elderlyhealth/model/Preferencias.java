package pt.unl.fct.msp.elderlyhealth.model;

import java.util.List;

public record Preferencias(String dieta, List<String> alergias, String observacoes) {
}
