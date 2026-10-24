package pt.unl.fct.msp.elderlyhealth.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pt.unl.fct.msp.elderlyhealth.exception.PedidoInvalidoException;
import pt.unl.fct.msp.elderlyhealth.exception.RecursoNaoEncontradoException;
import pt.unl.fct.msp.elderlyhealth.model.CategoriaDocumento;
import pt.unl.fct.msp.elderlyhealth.model.Documento;
import pt.unl.fct.msp.elderlyhealth.model.Idoso;

/**
 * Pasta de ficheiros de cada idoso:
 * <pre>
 * storage/idosos/idoso-{NIF}/historico-clinico/...
 *                            /exames/...
 *                            /receitas/...
 *                            /relatorios/...   (relatórios de incidentes gerados pelo CPS)
 *                            /outros/...
 * </pre>
 * A pasta usa o NIF (e não o id interno) para continuar associada ao mesmo idoso
 * mesmo que a aplicação seja reiniciada.
 */
@Service
public class DocumentoService {

    public static final long TAMANHO_MAXIMO = 10L * 1024 * 1024;
    private static final Set<String> EXTENSOES = Set.of("pdf", "png", "jpg", "jpeg", "txt", "doc", "docx",
            "xls", "xlsx", "csv", "odt");

    private final IdosoService idosos;
    private final Path raiz;

    public DocumentoService(IdosoService idosos, @Value("${elderlyhealth.storage.dir:storage}") String diretorio) {
        this.idosos = idosos;
        this.raiz = Paths.get(diretorio).toAbsolutePath().normalize().resolve("idosos");
    }

    /** Cria a pasta do idoso e as subpastas de cada categoria (chamado no fim do registo). */
    public Path criarPastas(Idoso idoso) {
        Path pasta = pastaDo(idoso);
        try {
            for (CategoriaDocumento c : CategoriaDocumento.values()) {
                Files.createDirectories(pasta.resolve(c.getPasta()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta do idoso", e);
        }
        return pasta;
    }

    public List<Documento> listar(Long idosoId, CategoriaDocumento categoria) {
        Idoso idoso = idosos.obter(idosoId);
        Path pasta = criarPastas(idoso);
        List<Documento> resultado = new ArrayList<>();
        for (CategoriaDocumento c : CategoriaDocumento.values()) {
            if (categoria != null && c != categoria) {
                continue;
            }
            try (Stream<Path> ficheiros = Files.list(pasta.resolve(c.getPasta()))) {
                ficheiros.filter(Files::isRegularFile).forEach(f -> resultado.add(descrever(idosoId, c, f)));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        resultado.sort(Comparator.comparing(Documento::carregadoEm).reversed());
        return resultado;
    }

    public Documento guardar(Long idosoId, CategoriaDocumento categoria, String nomeOriginal,
                             long tamanho, InputStream conteudo) {
        if (categoria == null) {
            throw new PedidoInvalidoException("Escolha a categoria do documento");
        }
        if (tamanho <= 0) {
            throw new PedidoInvalidoException("O ficheiro está vazio");
        }
        if (tamanho > TAMANHO_MAXIMO) {
            throw new PedidoInvalidoException("O ficheiro é demasiado grande (máximo 10 MB)");
        }
        String nome = limparNome(nomeOriginal);
        String extensao = extensao(nome);
        if (!EXTENSOES.contains(extensao)) {
            throw new PedidoInvalidoException("Tipo de ficheiro não permitido (." + extensao
                    + "). Aceites: " + String.join(", ", EXTENSOES.stream().sorted().toList()));
        }
        Idoso idoso = idosos.obter(idosoId);
        Path destino = nomeLivre(criarPastas(idoso).resolve(categoria.getPasta()), nome);
        try {
            Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível guardar o ficheiro", e);
        }
        return descrever(idosoId, categoria, destino);
    }

    /** Usado pelo CPS para arquivar relatórios gerados automaticamente (ex.: incidente de queda). */
    public Documento guardarTexto(Long idosoId, CategoriaDocumento categoria, String nome, String texto) {
        byte[] bytes = texto.getBytes(StandardCharsets.UTF_8);
        return guardar(idosoId, categoria, nome, bytes.length, new java.io.ByteArrayInputStream(bytes));
    }

    public Path caminho(Long idosoId, CategoriaDocumento categoria, String nome) {
        Idoso idoso = idosos.obter(idosoId);
        Path pastaCategoria = pastaDo(idoso).resolve(categoria.getPasta()).normalize();
        Path ficheiro = pastaCategoria.resolve(nome).normalize();
        // Proteção contra path traversal (ex.: nome = "../../outro-idoso/ficheiro.pdf")
        if (!ficheiro.getParent().equals(pastaCategoria) || !Files.isRegularFile(ficheiro)) {
            throw new RecursoNaoEncontradoException("Documento não encontrado");
        }
        return ficheiro;
    }

    public Documento descrever(Long idosoId, CategoriaDocumento categoria, String nome) {
        return descrever(idosoId, categoria, caminho(idosoId, categoria, nome));
    }

    public void apagar(Long idosoId, CategoriaDocumento categoria, String nome) {
        try {
            Files.delete(caminho(idosoId, categoria, nome));
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível apagar o ficheiro", e);
        }
    }

    Path pastaDo(Idoso idoso) {
        return raiz.resolve("idoso-" + idoso.getNif());
    }

    private Documento descrever(Long idosoId, CategoriaDocumento categoria, Path f) {
        try {
            return new Documento(idosoId, categoria, f.getFileName().toString(), Files.size(f),
                    tipoDe(f.getFileName().toString()), Files.getLastModifiedTime(f).toInstant());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Remove caminhos, acentos e caracteres especiais do nome enviado pelo browser. */
    static String limparNome(String original) {
        if (original == null || original.isBlank()) {
            throw new PedidoInvalidoException("O ficheiro não tem nome");
        }
        String base = original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        base = base.replaceAll("[^A-Za-z0-9._ -]", "_").replaceAll("\\s+", " ").trim();
        while (base.startsWith(".")) {
            base = base.substring(1);
        }
        if (base.isBlank() || !base.contains(".")) {
            throw new PedidoInvalidoException("Nome de ficheiro inválido");
        }
        return base.length() > 120 ? base.substring(base.length() - 120) : base;
    }

    private static String extensao(String nome) {
        return nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static Path nomeLivre(Path pasta, String nome) {
        Path destino = pasta.resolve(nome);
        int ponto = nome.lastIndexOf('.');
        String base = nome.substring(0, ponto);
        String ext = nome.substring(ponto);
        for (int i = 2; Files.exists(destino); i++) {
            destino = pasta.resolve(base + " (" + i + ")" + ext);
        }
        return destino;
    }

    public static String tipoDe(String nome) {
        return switch (extensao(nome)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "txt" -> "text/plain;charset=UTF-8";
            case "csv" -> "text/csv;charset=UTF-8";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            default -> "application/octet-stream";
        };
    }
}
