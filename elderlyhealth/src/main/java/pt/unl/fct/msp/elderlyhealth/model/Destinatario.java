package pt.unl.fct.msp.elderlyhealth.model;

/** Participantes "black box" dos BPMN: só recebem/enviam informação pedida pelo CPS. */
public enum Destinatario {
    UTILIZADOR,   // quem está a fazer o registo / pedido (idoso, familiar ou recepcionista)
    IDOSO,
    FAMILIA,
    ENFERMEIRO
}
