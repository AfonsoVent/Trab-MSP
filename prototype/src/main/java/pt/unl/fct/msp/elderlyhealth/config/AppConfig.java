package pt.unl.fct.msp.elderlyhealth.config;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
public class AppConfig {

    /** Relógio injetável — permite testar os timers (timeouts) sem esperar tempo real. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Liga o agendador que dispara os timers dos BPMN (pode ser desligado nos testes). */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "elderlyhealth.scheduler.ativo", havingValue = "true", matchIfMissing = true)
    static class SchedulingConfig {
    }
}
