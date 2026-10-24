package pt.unl.fct.msp.elderlyhealth.config;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import pt.unl.fct.msp.elderlyhealth.model.ContactoEmergencia;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.model.Preferencias;
import pt.unl.fct.msp.elderlyhealth.repository.IdosoRepository;

/** Idoso de demonstração (id 1) para testar contactos, quedas e tensão sem ter de registar primeiro. */
@Component
@ConditionalOnProperty(name = "elderlyhealth.dados-demo", havingValue = "true", matchIfMissing = true)
public class DadosIniciais implements CommandLineRunner {

    private final IdosoRepository idosos;
    private final Clock clock;

    public DadosIniciais(IdosoRepository idosos, Clock clock) {
        this.idosos = idosos;
        this.clock = clock;
    }

    @Override
    public void run(String... args) {
        if (!idosos.todos().isEmpty()) {
            return;
        }
        Idoso maria = new Idoso();
        maria.setNome("Maria Silva");
        maria.setNif("234567899");
        maria.setDataNascimento(LocalDate.of(1942, 5, 17));
        maria.setInformacaoMedica("Hipertensão; dieta hipossódica");
        maria.setQuarto("Q204");
        maria.setPreferencias(new Preferencias("Hipossódica", List.of("Penicilina"), "Prefere visitas à tarde"));
        maria.setContactoEmergencia(new ContactoEmergencia("João Silva", "912345678", "Filho"));
        maria.setRegistadoEm(clock.instant());
        idosos.guardar(maria);
    }
}
