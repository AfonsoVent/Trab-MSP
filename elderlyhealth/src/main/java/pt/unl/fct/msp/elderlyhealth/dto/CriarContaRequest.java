package pt.unl.fct.msp.elderlyhealth.dto;

import jakarta.validation.constraints.NotBlank;
import pt.unl.fct.msp.elderlyhealth.model.Papel;

public record CriarContaRequest(@NotBlank String nome, @NotBlank String email, @NotBlank String password, Papel papel) {
}
