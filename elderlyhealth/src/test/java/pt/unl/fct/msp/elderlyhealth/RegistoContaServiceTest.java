package pt.unl.fct.msp.elderlyhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import pt.unl.fct.msp.elderlyhealth.exception.EstadoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.PedidoExpiradoException;
import pt.unl.fct.msp.elderlyhealth.model.CanalRegisto;
import pt.unl.fct.msp.elderlyhealth.model.ContactoEmergencia;
import pt.unl.fct.msp.elderlyhealth.model.DadosIdoso;
import pt.unl.fct.msp.elderlyhealth.model.EstadoRegisto;
import pt.unl.fct.msp.elderlyhealth.model.PedidoRegisto;
import pt.unl.fct.msp.elderlyhealth.model.Preferencias;

class RegistoContaServiceTest {

    private static DadosIdoso dados(String nif) {
        return new DadosIdoso("António Costa", nif, LocalDate.of(1940, 1, 1), "Diabetes", "Q101");
    }

    @Test
    void registoCompletoCriaIdoso() {
        Fixtures f = new Fixtures();
        PedidoRegisto p = f.registos.iniciar(CanalRegisto.RECEPCIONISTA);
        assertEquals(EstadoRegisto.AGUARDA_DADOS, p.getEstado());

        f.registos.submeterDados(p.getId(), dados("123456789"));
        assertEquals(EstadoRegisto.AGUARDA_PREFERENCIAS_E_CONTACTO, p.getEstado());

        // ramos paralelos podem chegar por qualquer ordem
        f.registos.submeterContactoEmergencia(p.getId(), new ContactoEmergencia("Ana Costa", "+351 961 234 567", "Filha"));
        assertEquals(EstadoRegisto.AGUARDA_PREFERENCIAS_E_CONTACTO, p.getEstado());
        f.registos.submeterPreferencias(p.getId(), new Preferencias("Sem açúcar", List.of("Lactose"), null));

        assertEquals(EstadoRegisto.REGISTADO, p.getEstado());
        assertNotNull(p.getIdosoId());
        assertEquals("961234567", f.idosos.obter(p.getIdosoId()).getContactoEmergencia().telefone());
    }

    @Test
    void registoDaContaFamiliarDaAcessoAoIdoso() {
        Fixtures f = new Fixtures();
        var ana = f.auth.registar("Ana Costa", "ana@mail.pt", "ana12345", null);
        PedidoRegisto p = f.registos.iniciar(CanalRegisto.FAMILIAR, ana.getId());
        f.registos.submeterDados(p.getId(), dados("123456789"));
        f.registos.submeterPreferencias(p.getId(), new Preferencias(null, null, null));
        f.registos.submeterContactoEmergencia(p.getId(), new ContactoEmergencia("Ana Costa", "961234567", "Filha"));
        assertEquals(EstadoRegisto.REGISTADO, p.getEstado());
        assertEquals(true, ana.getIdosoIds().contains(p.getIdosoId()));
        assertEquals(true, java.nio.file.Files.isDirectory(f.pastaStorage.resolve("idosos/idoso-123456789/exames")));
    }

    @Test
    void nifInvalidoNegaConta() {
        Fixtures f = new Fixtures();
        PedidoRegisto p = f.registos.iniciar(null);
        f.registos.submeterDados(p.getId(), dados("123456780"));
        assertEquals(EstadoRegisto.NEGADO, p.getEstado());
        assertEquals("NIF inválido", p.getMotivo());
    }

    @Test
    void nifDuplicadoNegaConta() {
        Fixtures f = new Fixtures();
        f.criarIdoso(); // NIF 234567899
        PedidoRegisto p = f.registos.iniciar(null);
        f.registos.submeterDados(p.getId(), dados("234567899"));
        assertEquals(EstadoRegisto.NEGADO, p.getEstado());
    }

    @Test
    void contactoEmergenciaInvalidoNegaConta() {
        Fixtures f = new Fixtures();
        PedidoRegisto p = f.registos.iniciar(null);
        f.registos.submeterDados(p.getId(), dados("123456789"));
        f.registos.submeterContactoEmergencia(p.getId(), new ContactoEmergencia("Ana", "123", "Filha"));
        assertEquals(EstadoRegisto.NEGADO, p.getEstado());
    }

    @Test
    void passoForaDeOrdemDaConflito() {
        Fixtures f = new Fixtures();
        PedidoRegisto p = f.registos.iniciar(null);
        assertThrows(EstadoInvalidoException.class,
                () -> f.registos.submeterPreferencias(p.getId(), new Preferencias(null, null, null)));
    }

    @Test
    void timerDe20MinutosExpiraORegisto() {
        Fixtures f = new Fixtures();
        PedidoRegisto p = f.registos.iniciar(null);
        f.clock.avancar(Duration.ofMinutes(21));
        assertThrows(PedidoExpiradoException.class, () -> f.registos.submeterDados(p.getId(), dados("123456789")));
        assertEquals(EstadoRegisto.EXPIRADO, f.registos.obter(p.getId()).getEstado());
    }
}
