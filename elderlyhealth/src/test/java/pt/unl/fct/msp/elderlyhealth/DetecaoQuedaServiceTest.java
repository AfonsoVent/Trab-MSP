package pt.unl.fct.msp.elderlyhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import pt.unl.fct.msp.elderlyhealth.exception.EstadoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.model.AlertaQueda;
import pt.unl.fct.msp.elderlyhealth.model.CategoriaDocumento;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.EstadoQueda;

class DetecaoQuedaServiceTest {

    @Test
    void idosoDizQueEstaBemCancelaAlerta() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        AlertaQueda a = f.quedas.registarQueda(id, "wearable-7");
        assertEquals(EstadoQueda.AGUARDA_RESPOSTA_IDOSO, a.getEstado());
        f.quedas.responder(a.getId(), false);
        assertEquals(EstadoQueda.CANCELADO, a.getEstado());
        assertTrue(f.notificacoes.listar(Destinatario.ENFERMEIRO, id).isEmpty());
    }

    @Test
    void idosoPedeAjudaAlertaEnfermeiro() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        AlertaQueda a = f.quedas.registarQueda(id, "wearable-7");
        f.quedas.responder(a.getId(), true);
        assertEquals(EstadoQueda.ENFERMEIRO_ALERTADO, a.getEstado());
        assertFalse(a.isEscaladoPorTimeout());
        assertEquals("Q204", a.getQuarto());
        assertEquals(1, f.notificacoes.listar(Destinatario.ENFERMEIRO, id).size());
    }

    @Test
    void semRespostaEm1MinutoEscalaEDepoisRegistaIncidente() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        AlertaQueda a = f.quedas.registarQueda(id, "wearable-7");
        f.clock.avancar(Duration.ofSeconds(61));
        f.quedas.escalarSemResposta();
        assertEquals(EstadoQueda.ENFERMEIRO_ALERTADO, a.getEstado());
        assertTrue(a.isEscaladoPorTimeout());

        // resposta tardia já não é aceite
        assertThrows(EstadoInvalidoException.class, () -> f.quedas.responder(a.getId(), false));

        f.quedas.registarRelatorio(a.getId(), "Enf. Rita", "Idoso no chão, sem ferimentos. Levantado e observado.");
        assertEquals(EstadoQueda.INCIDENTE_REGISTADO, a.getEstado());
        // o relatório fica guardado na pasta do idoso
        assertEquals("incidente-queda-" + a.getId() + ".txt",
                f.documentos.listar(id, CategoriaDocumento.RELATORIOS).get(0).nome());
    }

    @Test
    void relatorioAntesDoAlertaDaConflito() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        AlertaQueda a = f.quedas.registarQueda(id, null);
        assertThrows(EstadoInvalidoException.class, () -> f.quedas.registarRelatorio(a.getId(), "Enf. Rita", "x"));
    }
}
