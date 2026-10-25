package pt.unl.fct.msp.elderlyhealth.dto;

import jakarta.validation.constraints.NotBlank;

public record RelatorioAtendimentoRequest(@NotBlank String enfermeiro, @NotBlank String relatorio) {
}
