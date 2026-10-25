package pt.unl.fct.msp.elderlyhealth.dto;

import jakarta.validation.constraints.NotNull;

public record RespostaIdosoRequest(@NotNull Boolean precisaAjuda) {
}
