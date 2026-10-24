package pt.unl.fct.msp.elderlyhealth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pt.unl.fct.msp.elderlyhealth.service.ValidadorDados;

class ValidadorDadosTest {

    @Test
    void nifValidoComDigitoDeControlo() {
        assertTrue(ValidadorDados.nifValido("123456789"));
        assertTrue(ValidadorDados.nifValido("234 567 899"));
    }

    @Test
    void nifInvalido() {
        assertFalse(ValidadorDados.nifValido("123456780"));
        assertFalse(ValidadorDados.nifValido("12345"));
        assertFalse(ValidadorDados.nifValido("abcdefghi"));
        assertFalse(ValidadorDados.nifValido(null));
    }

    @Test
    void telefonesPortugueses() {
        assertTrue(ValidadorDados.telefoneValido("912345678"));
        assertTrue(ValidadorDados.telefoneValido("+351 912 345 678"));
        assertTrue(ValidadorDados.telefoneValido("213456789"));
        assertFalse(ValidadorDados.telefoneValido("812345678"));
        assertFalse(ValidadorDados.telefoneValido("9123"));
    }
}
