package pt.unl.fct.msp.elderlyhealth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Enviado pelo dispositivo de medição da tensão arterial. */
public record MedicaoTensaoRequest(
        @NotNull Long idosoId,
        String dispositivoId,
        @NotNull @Min(40) @Max(300) Integer sistolica,
        @NotNull @Min(20) @Max(200) Integer diastolica,
        @Min(20) @Max(250) Integer pulso) {
}
