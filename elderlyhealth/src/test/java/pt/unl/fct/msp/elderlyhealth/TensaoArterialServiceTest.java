package pt.unl.fct.msp.elderlyhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.model.ClassificacaoTensao;
import pt.unl.fct.msp.elderlyhealth.model.Destinatario;
import pt.unl.fct.msp.elderlyhealth.model.MedicaoTensao;
import pt.unl.fct.msp.elderlyhealth.service.TensaoArterialService;

class TensaoArterialServiceTest {

    @Test
    void classificacao() {
        assertEquals(ClassificacaoTensao.NORMAL, TensaoArterialService.classificar(120, 80));
        assertEquals(ClassificacaoTensao.ALERTA_NAO_GRAVE, TensaoArterialService.classificar(150, 95));
        assertEquals(ClassificacaoTensao.ALERTA_NAO_GRAVE, TensaoArterialService.classificar(85, 55));
        assertEquals(ClassificacaoTensao.ALERTA_GRAVE, TensaoArterialService.classificar(185, 100));
        assertEquals(ClassificacaoTensao.ALERTA_GRAVE, TensaoArterialService.classificar(170, 125));
        assertEquals(ClassificacaoTensao.ALERTA_GRAVE, TensaoArterialService.classificar(75, 50));
    }

    @Test
    void normalNotificaApenasIdoso() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        MedicaoTensao m = f.tensao.registarMedicao(id, "tensiometro-1", 120, 80, 70);
        assertEquals(ClassificacaoTensao.NORMAL, m.getClassificacao());
        assertEquals(1, f.notificacoes.listar(Destinatario.IDOSO, id).size());
        assertTrue(f.notificacoes.listar(Destinatario.ENFERMEIRO, id).isEmpty());
    }

    @Test
    void naoGraveGeraRelatorioDeCuidados() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        MedicaoTensao m = f.tensao.registarMedicao(id, null, 150, 95, null);
        assertEquals(ClassificacaoTensao.ALERTA_NAO_GRAVE, m.getClassificacao());
        assertTrue(!m.getRecomendacoes().isEmpty());
    }

    @Test
    void graveAlertaEnfermeiro() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        f.tensao.registarMedicao(id, null, 190, 125, 110);
        assertEquals(1, f.notificacoes.listar(Destinatario.ENFERMEIRO, id).size());
        assertEquals(1, f.tensao.historico(id).size());
    }

    @Test
    void sistolicaMenorQueDiastolicaEInvalida() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        assertThrows(PedidoInvalidoException.class, () -> f.tensao.registarMedicao(id, null, 80, 90, null));
    }
}
