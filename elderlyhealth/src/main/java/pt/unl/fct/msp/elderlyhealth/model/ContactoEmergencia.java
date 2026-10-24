package pt.unl.fct.msp.elderlyhealth.model;

import jakarta.validation.constraints.NotBlank;

public record ContactoEmergencia(@NotBlank String nome, @NotBlank String telefone, String parentesco) {
}
