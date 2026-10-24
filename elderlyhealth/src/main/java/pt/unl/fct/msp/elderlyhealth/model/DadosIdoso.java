package pt.unl.fct.msp.elderlyhealth.model;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

public record DadosIdoso(
        @NotBlank String nome,
        @NotBlank String nif,
        @NotNull @Past LocalDate dataNascimento,
        String informacaoMedica,
        String quarto) {
}
