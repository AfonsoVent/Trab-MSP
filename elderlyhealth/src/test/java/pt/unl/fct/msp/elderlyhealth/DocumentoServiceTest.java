package pt.unl.fct.msp.elderlyhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.RecursoNaoEncontradoException;
import pt.unl.fct.msp.elderlyhealth.model.CategoriaDocumento;
import pt.unl.fct.msp.elderlyhealth.model.Documento;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;

class DocumentoServiceTest {

    private static Documento enviar(Fixtures f, Long id, CategoriaDocumento c, String nome, String conteudo) {
        byte[] b = conteudo.getBytes(StandardCharsets.UTF_8);
        return f.documentos.guardar(id, c, nome, b.length, new ByteArrayInputStream(b));
    }

    @Test
    void cadaIdosoTemASuaPastaComCategorias() throws Exception {
        Fixtures f = new Fixtures();
        Idoso maria = f.criarIdoso();
        enviar(f, maria.getId(), CategoriaDocumento.EXAMES, "Análises Março.pdf", "%PDF-1.4");

        assertTrue(Files.isDirectory(f.pastaStorage.resolve("idosos/idoso-234567899/historico-clinico")));
        assertTrue(Files.isRegularFile(f.pastaStorage.resolve("idosos/idoso-234567899/exames/Analises Marco.pdf")));
        assertEquals(1, f.documentos.listar(maria.getId(), null).size());
        assertEquals(0, f.documentos.listar(maria.getId(), CategoriaDocumento.RECEITAS).size());
    }

    @Test
    void nomesRepetidosNaoSubstituem() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        enviar(f, id, CategoriaDocumento.RECEITAS, "receita.pdf", "a");
        Documento d = enviar(f, id, CategoriaDocumento.RECEITAS, "receita.pdf", "b");
        assertEquals("receita (2).pdf", d.nome());
    }

    @Test
    void bloqueiaExtensoesEPathTraversal() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        assertThrows(PedidoInvalidoException.class, () -> enviar(f, id, CategoriaDocumento.OUTROS, "virus.exe", "x"));
        Documento d = enviar(f, id, CategoriaDocumento.OUTROS, "../../../etc/notas.txt", "x");
        assertEquals("notas.txt", d.nome());
        assertThrows(RecursoNaoEncontradoException.class,
                () -> f.documentos.caminho(id, CategoriaDocumento.OUTROS, "../exames/notas.txt"));
    }

    @Test
    void apagarDocumento() {
        Fixtures f = new Fixtures();
        Long id = f.criarIdoso().getId();
        enviar(f, id, CategoriaDocumento.OUTROS, "nota.txt", "x");
        f.documentos.apagar(id, CategoriaDocumento.OUTROS, "nota.txt");
        assertEquals(0, f.documentos.listar(id, null).size());
    }
}
