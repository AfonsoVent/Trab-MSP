package pt.unl.fct.msp.elderlyhealth.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import pt.unl.fct.msp.elderlyhealth.config.AuthInterceptor;
import pt.unl.fct.msp.elderlyhealth.model.CategoriaDocumento;
import pt.unl.fct.msp.elderlyhealth.model.Documento;
import pt.unl.fct.msp.elderlyhealth.model.Utilizador;
import pt.unl.fct.msp.elderlyhealth.service.AuthService;
import pt.unl.fct.msp.elderlyhealth.service.DocumentoService;

/** Pasta de documentos de cada idoso (histórico clínico, exames, receitas, relatórios, outros). */
@RestController
@RequestMapping("/api/idosos/{idosoId}/documentos")
public class DocumentoController {

    private final DocumentoService documentos;

    public DocumentoController(DocumentoService documentos) {
        this.documentos = documentos;
    }

    @GetMapping
    public List<Documento> listar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u,
                                  @PathVariable Long idosoId,
                                  @RequestParam(required = false) CategoriaDocumento categoria) {
        AuthService.exigirAcessoIdoso(u, idosoId);
        return documentos.listar(idosoId, categoria);
    }

    /** multipart/form-data com os campos "ficheiro" e "categoria". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Documento carregar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u,
                              @PathVariable Long idosoId,
                              @RequestParam("categoria") CategoriaDocumento categoria,
                              @RequestParam("ficheiro") MultipartFile ficheiro) throws IOException {
        AuthService.exigirAcessoIdoso(u, idosoId);
        try (InputStream in = ficheiro.getInputStream()) {
            return documentos.guardar(idosoId, categoria, ficheiro.getOriginalFilename(), ficheiro.getSize(), in);
        }
    }

    /** Descarregar (ou ver no browser com ?inline=true). */
    @GetMapping("/{categoria}/{nome}")
    public ResponseEntity<Resource> descarregar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u,
                                                @PathVariable Long idosoId,
                                                @PathVariable CategoriaDocumento categoria,
                                                @PathVariable String nome,
                                                @RequestParam(defaultValue = "false") boolean inline) throws IOException {
        AuthService.exigirAcessoIdoso(u, idosoId);
        Path ficheiro = documentos.caminho(idosoId, categoria, nome);
        String nomeCodificado = URLEncoder.encode(ficheiro.getFileName().toString(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (inline ? "inline" : "attachment") + "; filename*=UTF-8''" + nomeCodificado)
                .contentType(MediaType.parseMediaType(DocumentoService.tipoDe(ficheiro.getFileName().toString())))
                .contentLength(Files.size(ficheiro))
                .body(new FileSystemResource(ficheiro));
    }

    @DeleteMapping("/{categoria}/{nome}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@RequestAttribute(AuthInterceptor.ATRIBUTO_UTILIZADOR) Utilizador u,
                       @PathVariable Long idosoId,
                       @PathVariable CategoriaDocumento categoria,
                       @PathVariable String nome) {
        AuthService.exigirAcessoIdoso(u, idosoId);
        documentos.apagar(idosoId, categoria, nome);
    }
}
