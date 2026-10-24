package pt.unl.fct.msp.elderlyhealth;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;

import pt.unl.fct.msp.elderlyhealth.model.Idoso;
import pt.unl.fct.msp.elderlyhealth.repository.AlertaQuedaRepository;
import pt.unl.fct.msp.elderlyhealth.repository.ContactoFamiliarRepository;
import pt.unl.fct.msp.elderlyhealth.repository.IdosoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.MedicaoTensaoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.NotificacaoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.PedidoContactoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.PedidoRegistoRepository;
import pt.unl.fct.msp.elderlyhealth.repository.UtilizadorRepository;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.DocumentoService;
import pt.unl.fct.msp.elderlyhealth.service.ContactoFamiliarService;
import pt.unl.fct.msp.elderlyhealth.service.DetecaoQuedaService;
import pt.unl.fct.msp.elderlyhealth.service.IdosoService;
import pt.unl.fct.msp.elderlyhealth.service.NotificacaoService;
import pt.unl.fct.msp.elderlyhealth.service.RegistoContaService;
import pt.unl.fct.msp.elderlyhealth.service.TensaoArterialService;

/** Monta os serviços à mão (sem Spring) para testes unitários rápidos. */
public class Fixtures {

    public final MutableClock clock = new MutableClock();
    public final IdosoRepository idosoRepo = new IdosoRepository();
    public final NotificacaoService notificacoes = new NotificacaoService(new NotificacaoRepository(), clock);
    public final IdosoService idosos = new IdosoService(idosoRepo);
    public final UtilizadorRepository utilizadorRepo = new UtilizadorRepository();
    public final AuthService auth = new AuthService(utilizadorRepo, clock, Duration.ofHours(8));
    public final Path pastaStorage = tempDir();
    public final DocumentoService documentos = new DocumentoService(idosos, pastaStorage.toString());
    public final RegistoContaService registos = new RegistoContaService(new PedidoRegistoRepository(), idosoRepo,
            utilizadorRepo, documentos, notificacoes, clock, Duration.ofMinutes(20));
    public final ContactoFamiliarService contactos = new ContactoFamiliarService(new PedidoContactoRepository(),
            new ContactoFamiliarRepository(), idosos, notificacoes, clock, Duration.ofMinutes(1));
    public final DetecaoQuedaService quedas = new DetecaoQuedaService(new AlertaQuedaRepository(), idosos,
            notificacoes, documentos, clock, Duration.ofMinutes(1));
    public final TensaoArterialService tensao = new TensaoArterialService(new MedicaoTensaoRepository(), idosos,
            notificacoes, clock);

    private static Path tempDir() {
        try {
            Path p = Files.createTempDirectory("elderlyhealth-test");
            p.toFile().deleteOnExit();
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Idoso criarIdoso() {
        Idoso i = new Idoso();
        i.setNome("Maria Silva");
        i.setNif("234567899");
        i.setDataNascimento(LocalDate.of(1942, 5, 17));
        i.setInformacaoMedica("Hipertensão");
        i.setQuarto("Q204");
        i.setRegistadoEm(clock.instant());
        return idosoRepo.guardar(i);
    }
}
