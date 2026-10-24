package pt.unl.fct.msp.elderlyhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import pt.unl.fct.msp.elderlyhealth.dto.SessaoIniciada;
import pt.unl.fct.msp.elderlyhealth.exception.AcessoNegadoException;
import pt.unl.fct.msp.elderlyhealth.exception.NaoAutenticadoException;
import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.model.Papel;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.PasswordHasher;

class AuthServiceTest {

    @Test
    void passwordNuncaFicaEmTextoSimples() {
        String h = PasswordHasher.hash("segredo123");
        assertFalse(h.contains("segredo123"));
        assertTrue(PasswordHasher.verificar("segredo123", h));
        assertFalse(PasswordHasher.verificar("segredo124", h));
        // salt aleatório: o mesmo password dá hashes diferentes
        assertFalse(h.equals(PasswordHasher.hash("segredo123")));
    }

    @Test
    void criarContaEEntrar() {
        Fixtures f = new Fixtures();
        f.auth.registar("Ana Costa", "Ana@Mail.pt", "ana12345", Papel.FAMILIAR);
        SessaoIniciada s = f.auth.login("ana@mail.pt", "ana12345");
        assertNotNull(s.token());
        assertEquals("ana@mail.pt", s.utilizador().email());
        assertEquals("Ana Costa", f.auth.autenticar(s.token()).getNome());
    }

    @Test
    void validacoesDaConta() {
        Fixtures f = new Fixtures();
        assertThrows(PedidoInvalidoException.class, () -> f.auth.registar("Ana", "ana-mail", "ana12345", null));
        assertThrows(PedidoInvalidoException.class, () -> f.auth.registar("Ana", "ana@mail.pt", "curta1", null));
        assertThrows(PedidoInvalidoException.class, () -> f.auth.registar("Ana", "ana@mail.pt", "semnumeros", null));
        f.auth.registar("Ana", "ana@mail.pt", "ana12345", null);
        assertThrows(PedidoInvalidoException.class, () -> f.auth.registar("Ana 2", "ANA@mail.pt", "ana12345", null));
        assertThrows(AcessoNegadoException.class, () -> f.auth.registar("X", "x@mail.pt", "xpto1234", Papel.ADMIN));
    }

    @Test
    void credenciaisErradasEBloqueio() {
        Fixtures f = new Fixtures();
        f.auth.registar("Ana", "ana@mail.pt", "ana12345", null);
        for (int i = 0; i < 5; i++) {
            assertThrows(NaoAutenticadoException.class, () -> f.auth.login("ana@mail.pt", "errada999"));
        }
        // bloqueada mesmo com a password certa
        assertThrows(NaoAutenticadoException.class, () -> f.auth.login("ana@mail.pt", "ana12345"));
        f.clock.avancar(Duration.ofMinutes(6));
        assertNotNull(f.auth.login("ana@mail.pt", "ana12345").token());
    }

    @Test
    void sessaoExpiraELogout() {
        Fixtures f = new Fixtures();
        f.auth.registar("Ana", "ana@mail.pt", "ana12345", null);
        String t1 = f.auth.login("ana@mail.pt", "ana12345").token();
        f.auth.logout(t1);
        assertThrows(NaoAutenticadoException.class, () -> f.auth.autenticar(t1));
        String t2 = f.auth.login("ana@mail.pt", "ana12345").token();
        f.clock.avancar(Duration.ofHours(9));
        assertThrows(NaoAutenticadoException.class, () -> f.auth.autenticar(t2));
    }

    @Test
    void familiarSoVeOsSeusIdosos() {
        Fixtures f = new Fixtures();
        Long maria = f.criarIdoso().getId();
        Utilizador fam = f.auth.registar("Ana", "ana@mail.pt", "ana12345", null);
        Utilizador enf = f.auth.criarConta("Rita", "rita@hosp.pt", "rita12345", Papel.ENFERMEIRO);
        assertThrows(AcessoNegadoException.class, () -> AuthService.exigirAcessoIdoso(fam, maria));
        AuthService.exigirAcessoIdoso(enf, maria);
        fam.getIdosoIds().add(maria);
        AuthService.exigirAcessoIdoso(fam, maria);
        assertThrows(AcessoNegadoException.class, () -> AuthService.exigirEquipa(fam));
    }
}
