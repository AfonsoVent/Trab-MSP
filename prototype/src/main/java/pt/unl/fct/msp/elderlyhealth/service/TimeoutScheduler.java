package pt.unl.fct.msp.elderlyhealth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Dispara os timers dos BPMN (20 min registo, 1 min confirmação de contacto, 1 min resposta à queda). */
@Component
public class TimeoutScheduler {

    private final RegistoContaService registos;
    private final ContactoFamiliarService contactos;
    private final DetecaoQuedaService quedas;

    public TimeoutScheduler(RegistoContaService registos, ContactoFamiliarService contactos,
                            DetecaoQuedaService quedas) {
        this.registos = registos;
        this.contactos = contactos;
        this.quedas = quedas;
    }

    @Scheduled(fixedDelayString = "${elderlyhealth.scheduler.intervalo-ms:5000}")
    public void verificarTimers() {
        registos.expirarPendentes();
        contactos.expirarPendentes();
        quedas.escalarSemResposta();
    }
}
