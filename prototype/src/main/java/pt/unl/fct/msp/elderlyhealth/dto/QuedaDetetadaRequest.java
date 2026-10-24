package pt.unl.fct.msp.elderlyhealth.dto;

import jakarta.validation.constraints.NotNull;

/** Enviado pelo sensor / wearable quando deteta uma queda. */
public record QuedaDetetadaRequest(@NotNull Long idosoId, String sensorId) {
}
