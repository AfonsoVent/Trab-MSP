package pt.unl.fct.msp.elderlyhealth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio controlado pelos testes para simular os timers dos BPMN. */
public class MutableClock extends Clock {

    private Instant agora = Instant.parse("2026-10-25T10:00:00Z");

    public void avancar(Duration d) {
        agora = agora.plus(d);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
