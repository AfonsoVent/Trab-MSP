package pt.unl.fct.msp.elderlyhealth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DadosContactoFamiliar(
        @NotBlank String nome,
        @NotBlank String telefone,
        @Email String email,
        @NotBlank String parentesco) {
}
