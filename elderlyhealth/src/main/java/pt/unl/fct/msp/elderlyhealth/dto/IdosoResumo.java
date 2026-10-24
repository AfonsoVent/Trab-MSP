package pt.unl.fct.msp.elderlyhealth.dto;

import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.model.MedicaoTensao;

/** Idoso + estado atual, para o painel (lista de idosos e faixa de estado). */
public record IdosoResumo(Idoso idoso, MedicaoTensao ultimaMedicao, long quedasAtivas, long contactosFamiliares) {
}
