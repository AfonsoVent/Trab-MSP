package pt.unl.fct.msp.elderlyhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import pt.unl.fct.msp.elderlyhealth.exception.PedidoExpiradoException;
import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.RecursoNaoEncontradoException;
import pt.unl.fct.msp.elderlyhealth.model.DadosContactoFamiliar;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.EstadoPedidoContacto;
import pt.unl.fct.msp.elderlyhealth.model.PedidoContactoFamiliar;

class ContactoFamiliarServiceTest {

    private static final DadosContactoFamiliar JOAO =
            new DadosContactoFamiliar("João Silva", "912 345 678", "joao@mail.pt", "Filho");

    @Test
    void contactoConfirmadoFicaGuardado() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        PedidoContactoFamiliar p = f.contactos.pedirAdicao(id, JOAO);
        assertEquals(EstadoPedidoContacto.AGUARDA_CONFIRMACAO, p.getEstado());

        f.contactos.confirmar(id, p.getId(), true);
        assertEquals(EstadoPedidoContacto.CONFIRMADO, p.getEstado());
        assertEquals(1, f.contactos.listar(id).size());
        assertEquals("912345678", f.contactos.listar(id).get(0).getTelefone());
        assertTrue(!f.notificacoes.listar(Destinatario.FAMILIA, id).isEmpty());
    }

    @Test
    void utilizadorRejeita() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        PedidoContactoFamiliar p = f.contactos.pedirAdicao(id, JOAO);
        f.contactos.confirmar(id, p.getId(), false);
        assertEquals(EstadoPedidoContacto.REJEITADO, p.getEstado());
        assertEquals(0, f.contactos.listar(id).size());
    }

    @Test
    void telefoneInvalidoOuDuplicado() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        assertThrows(PedidoInvalidoException.class, () -> f.contactos.pedirAdicao(id,
                new DadosContactoFamiliar("X", "000", null, "Neto")));

        PedidoContactoFamiliar p = f.contactos.pedirAdicao(id, JOAO);
        f.contactos.confirmar(id, p.getId(), true);
        assertThrows(PedidoInvalidoException.class, () -> f.contactos.pedirAdicao(id, JOAO));
    }

    @Test
    void semConfirmacaoEm1MinutoExpira() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        PedidoContactoFamiliar p = f.contactos.pedirAdicao(id, JOAO);
        f.clock.avancar(Duration.ofSeconds(61));
        f.contactos.expirarPendentes();
        assertEquals(EstadoPedidoContacto.EXPIRADO, p.getEstado());
        assertThrows(PedidoExpiradoException.class, () -> f.contactos.confirmar(id, p.getId(), true));
    }

    @Test
    void idosoInexistente() {
        Fixtures f = new Fixtures();
        assertThrows(RecursoNaoEncontradoException.class, () -> f.contactos.pedirAdicao(99L, JOAO));
    }
}
